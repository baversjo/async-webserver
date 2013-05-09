package webserver;

import http_parser.HTTPCallback;
import http_parser.HTTPDataCallback;
import http_parser.HTTPParser;
import http_parser.ParserSettings;
import http_parser.ParserType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.CharacterCodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


public class Client {
	private final AsynchronousSocketChannel ch;
	private final ByteBuffer buffer;
	private Request request;
	private ParserSettings settings;
	private HTTPParser parser;
	private String lastHeader;

	public Client(AsynchronousSocketChannel ch){
		this.ch = ch;
		
		
		buffer = ByteBuffer.allocate(Server.BUFFER_SIZE);
		
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
				return 0;
			}
		};
		
		ClientReader reader = new ClientReader();
		ch.read(buffer, buffer, reader);
		
	}
	
	private class ClientReader implements CompletionHandler<Integer, ByteBuffer>{

		@Override
		public void completed(Integer result, ByteBuffer attachment) {
        	buffer.flip();
        	
        	parser.execute(settings,buffer);
        	
        	buffer.clear();
        	if(ch.isOpen()){
        		ch.read(buffer, buffer, this);
        	}
        	
		}

		@Override
		public void failed(Throwable exc, ByteBuffer attachment) {
			System.err.println("ERROR: could not read data from client");
		}
		
	}
}
