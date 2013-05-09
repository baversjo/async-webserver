package webserver;

import java.nio.ByteBuffer;

import http_parser.HTTPCallback;
import http_parser.HTTPDataCallback;
import http_parser.HTTPParser;
import http_parser.ParserSettings;
import http_parser.ParserType;


public class ParserTest { //TODO: catch HTTPException
	public static void main(String args[]){
		HTTPParser parser = new HTTPParser(ParserType.HTTP_REQUEST);
		
		ParserSettings settings = new ParserSettings();
		
		settings.on_path = new HTTPDataCallback(){
			@Override
			public int cb(HTTPParser p, byte[] by, int pos, int len) {
				System.out.println("PATH:" + new String(by));
				return 0;
				
			}
		};
		
		settings.on_header_field = new HTTPDataCallback(){
			@Override
			public int cb(HTTPParser p, byte[] by, int pos, int len) {
				System.out.print( new String(by) + "=");
				return 0;
				
			}
		};
		
		settings.on_header_value = new HTTPDataCallback(){
			@Override
			public int cb(HTTPParser p, byte[] by, int pos, int len) {
				System.out.println( new String(by));
				return 0;
				
			}
		};
		
		String reqStr = "GET / HTTP/1.1\r\n";
		
		parser.execute(settings,ByteBuffer.wrap(reqStr.getBytes()));
		
		reqStr = "User-Agent: curl/7.24.0 (x86_64-apple-darwin12.0) libcurl/7.24.0 OpenSSL/0.9.8r zlib/1.2.5\r\n" + 
				"Ho";
		
		parser.execute(settings,ByteBuffer.wrap(reqStr.getBytes()));
		
		reqStr = "st: google.se\r\n" + 
				"Accept: */*\r\n\r\n";
		
		parser.execute(settings,ByteBuffer.wrap(reqStr.getBytes()));
		
		System.out.println("returning from main");
	}
}

class OurDataCallback extends HTTPDataCallback{

	@Override
	public int cb(HTTPParser p, byte[] by, int pos, int len) {
		System.out.println("GOT data:" + new String(by));
		return 0;
	}
	
}


class OurHttpCallback extends HTTPCallback{

	@Override
	public int cb(HTTPParser parser) {
		System.out.println("message done");
		return 0;
	}
}