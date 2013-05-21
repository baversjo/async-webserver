package webserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Response {
	public Map<String,String> headers;
	
	public static final int BUFFER_SIZE = 8192; //8Kb
	
	static final byte[] NEW_LINE = {13,10};
	static final byte[] HTTP_1_OK = "HTTP/1.1 200 OK\r\n".getBytes();
	
	private boolean headersSent;
	private Client client;
	
	public Response(Client client){
		headers = new HashMap<String,String>();
		headersSent = false;
		this.client = client;
	}

	public void sendHeaders() {
		if(!headersSent){
			headersSent = true;
			headers.put("Content-Type", "text/xml");
			
			ByteBuffer[] bb = new ByteBuffer[headers.size()+2];
			
			bb[0] = ByteBuffer.wrap(HTTP_1_OK);
			
			int i = 1;
			for(Entry<String, String> header: headers.entrySet()){
				bb[i] =ByteBuffer.wrap((header.getKey() + ": " + header.getValue()).getBytes());
				i++;
			}
			
			bb[i] = ByteBuffer.wrap(NEW_LINE);
			try {
				client.ch.write(bb); //TODO: wait for key.isWritable?
			} catch (IOException e) {
				System.err.println("Could not write to client");
				e.printStackTrace();
				client.close();
			}
		}
	}
		
	public void sendFile(FileChannel file){
		try {
			sendHeaders();
			file.transferTo(0, Long.MAX_VALUE, client.ch); //TODO while loop
		} catch (IOException e) {
			System.err.println("could not send file");
			e.printStackTrace();
			client.close();
		}
		end();
	}

	public void end() {
		client.requestFinished();
	}
}
