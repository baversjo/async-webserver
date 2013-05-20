package webserver;

import java.util.HashMap;
import java.util.Map;


public class Request {
	public Map<String,String> headers;
	public String httpVersion;
	public String path;
	public String httpMethod;
	protected boolean completed;
	
	public Request(){
		headers = new HashMap<String,String>();
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