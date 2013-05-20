package webserver;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Response {
	public ByteBuffer body;
	public Map<String,String> headers;
	
	public static final int BUFFER_SIZE = 8192; //8Kb
	
	private boolean headersSent;
	private Client client;
	
	public Response(Client client){
		body = ByteBuffer.allocate(BUFFER_SIZE);
		headers = new HashMap<String,String>();
		headersSent = false;
		this.client = client;
	}

	public void sendHeaders(CompletionHandler<Integer, Object> completionHandler) {
		//TODO: serialize headers to string.
		headersSent = true;
		headers.put("Content-Type", "text/xml");
		StringBuilder hs = new StringBuilder("HTTP/1.1 200 OK\r\n");
		
		for(Entry<String, String> header: headers.entrySet()){
			hs.append(header.getKey() + ": " + header.getValue());
		}
		
		client.ch.write(ByteBuffer.wrap(hs.toString().getBytes()),null,null);
	}
		
	public void sendFile(FileChannel file){
		//file.transferTo(0, 0, client.ch); //TODO
		end();
	}

	public void end() {
		client.requestFinished();
	}
}
