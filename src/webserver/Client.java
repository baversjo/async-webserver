package webserver;

import http_parser.HTTPCallback;
import http_parser.HTTPDataCallback;
import http_parser.HTTPParser;
import http_parser.ParserSettings;
import http_parser.ParserType;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import middleware.Middleware;


public class Client {
	protected final SocketChannel ch;
	private final ByteBuffer buffer;
	private Request request;
	private ParserSettings settings;
	private HTTPParser parser;
	private String lastHeader;
	private boolean returnVal;

	public Client(SocketChannel ch){
		this.ch = ch;
		
		
		buffer = ByteBuffer.allocate(Server.BUFFER_SIZE);//TODO: what happens if request is really big?
		
		parser = new HTTPParser(ParserType.HTTP_REQUEST);
		
		settings = new ParserSettings();
		
		settings.on_message_begin = new HTTPCallback() {
			
			@Override
			public int cb(HTTPParser parser) {
				request = new Request();
				return 0;
			}
		};
		
		settings.on_path = new HTTPDataCallback(){
			@Override
			public int cb(HTTPParser p, byte[] by, int pos, int len) {
				request.path = new String(by);
				return 0;
				
			}
		};
		
		settings.on_header_field = new HTTPDataCallback(){
			@Override
			public int cb(HTTPParser p, byte[] by, int pos, int len) {
				lastHeader = new String(by);
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
				System.out.println(request.toString());
				request.completed = true;
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
	}
	
	//returns false if connection should be closed.
	public boolean doRead() {
		returnVal = true;
		
		buffer.flip();
		parser.execute(settings,buffer);
		buffer.clear();

		return returnVal;
	}
	
	
	private void sendResponse() {
		Response response = new Response(this);
		for(Middleware middleware: Server.middlewares){
			middleware.execute(request, response);
		}
	}
}
