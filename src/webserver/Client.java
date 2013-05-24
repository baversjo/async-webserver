package webserver;

import http_parser.HTTPCallback;
import http_parser.HTTPDataCallback;
import http_parser.HTTPParser;
import http_parser.ParserSettings;
import http_parser.ParserType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import middleware.Middleware;
import middleware.MiddlewareException;


public class Client implements Comparable<Client>{
	protected final SocketChannel ch;
	private Request request;
	private ParserSettings settings;
	private HTTPParser parser;
	private String lastHeader;
	private boolean returnVal;
	protected long lastCommunication;
	protected SelectionKey key;
	private WorkerThread worker;
	
	private Queue<Response> responses;
	private boolean gotOneFullRequest;
	private boolean open;

	public Client(final SocketChannel ch, WorkerThread worker){
		this.ch = ch;
		this.worker = worker;
		updateLastCommunication();
		open = true;
		
		responses = new LinkedBlockingQueue<Response>();
		
		parser = new HTTPParser(ParserType.HTTP_REQUEST);
		
		settings = new ParserSettings();
		
		settings.on_message_begin = new HTTPCallback() {
			
			@Override
			public int cb(HTTPParser parser) {
				request = new Request(parser.getHTTPMethod());
				try {
					request.ip = ch.getRemoteAddress();
				} catch (IOException e) {
				}
				return 0;
			}
		};
		
		settings.on_path = new HTTPDataCallback(){
			@Override
			public int cb(HTTPParser p, byte[] by, int pos, int len) {
				String path = new String(by);
				if(path.equals( "/")){
					path = "/index.html";
				}
				request.path = path;
				return 0;
				
			}
		};
		
		settings.on_header_field = new HTTPDataCallback(){
			@Override
			public int cb(HTTPParser p, byte[] by, int pos, int len) {
				lastHeader = new String(by).toLowerCase();
				
				if(request.httpMajor == 0){
					request.httpMajor = p.http_major;
					request.httpMinor = p.http_minor;
				}
				return 0;
				
			}
		};
		
		settings.on_header_value = new HTTPDataCallback(){
			@Override
			public int cb(HTTPParser p, byte[] by, int pos, int len) {
				request.headers.put(lastHeader,new String(by));
				return 0;
				
			}
		};
		
		settings.on_message_complete = new HTTPCallback() {
			
			@Override
			public int cb(HTTPParser parser) {
				request.completed = true;
				gotOneFullRequest = true;
				sendResponse();
				return 0;
			}
		};
		
		
	}
	public void requestFinished() {
		request = null;
	}

	public void close() {
		returnVal = false;
		if(open){
			try {
				ch.close();
				key.cancel();
				lastCommunication = 0;
				for(Response response : responses){
					if(response.fileToSend != null){
						response.fileToSend.close();
					}
				}
			} catch (IOException e) {}
			open = false;
		}
	}
	
	//returns false if connection should be closed.
	public boolean doRead(ByteBuffer buff) {
		returnVal = true;
		gotOneFullRequest = false;
		try {
			int res = ch.read(buff);
			if(res == -1){
				returnVal = false; //close
			}
		} catch (IOException e) {
			System.err.println("Socket read failed");
			returnVal = false;
			e.printStackTrace();
		}
		buff.flip();
		parser.execute(settings,buff);
		buff.clear();
		
		if(gotOneFullRequest){
			key.interestOps(SelectionKey.OP_WRITE);
		}
		return returnVal;
	}
	
	public boolean doWrite(){
		returnVal = true;
		Response response;
		while(true){
			response = responses.peek();
			if(response == null){ //no more buffered responses, start reading again.
				if(key.isValid()){
					key.interestOps(SelectionKey.OP_READ);
				}
				break;
			}else{
				if(response.write()){ //wrote entire response, continue
					responses.remove();
					
				}else{ //response not finished
					break;
				}
			}
		}
		return returnVal;
	}
	
	
	private void sendResponse() {
		Response response = new Response(this);
		for(Middleware middleware: worker.middlewares){
			try{
				middleware.execute(request, response);
			}catch(MiddlewareException ex){
				break;
			}
		}
		//TODO: optimizaiton: try writing response, and only buffer if not everything was written.
		responses.add(response);
	}
	
	public void updateLastCommunication() {
		this.lastCommunication = System.currentTimeMillis();
		
	}

	@Override
	public int compareTo(Client o) {
		return (int)((this.lastCommunication - o.lastCommunication)/1000);
	}
	public boolean requestIsEmpty(){
		return (request == null);
	}
	
}
