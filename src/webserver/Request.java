package webserver;

import http_parser.HTTPMethod;

import java.util.HashMap;
import java.util.Map;


public class Request {
	public Map<String,String> headers;
	public int httpMajor;
	public int httpMinor;
	//public String method;
	public String path;
	public HTTPMethod httpMethod;
	protected boolean completed;
	
	public Request(HTTPMethod httpMethod){
		headers = new HashMap<String,String>();
		this.httpMethod = httpMethod; 
		completed = false;
	}
	
	
	public String toString(){
		String str = "Request object: " + path + "\n";
		for(String key: headers.keySet()){
			str += (key + "=" + headers.get(key) + "\n");
		}
		return str;
	}
}
