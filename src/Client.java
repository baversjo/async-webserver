import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.CharacterCodingException;


public class Client {
	private final AsynchronousSocketChannel ch;
	private final ByteBuffer buffer;
	private final StringBuilder request; 

	public Client(AsynchronousSocketChannel ch){
		this.ch = ch;
		
		this.request = new StringBuilder(); 
		
		buffer = ByteBuffer.allocate(Server.BUFFER_SIZE);
		
		ClientReader reader = new ClientReader();
		ch.read(buffer, buffer, reader);
		
		
	}
	
	private class ClientReader implements CompletionHandler<Integer, ByteBuffer>{

		@Override
		public void completed(Integer result, ByteBuffer attachment) {
        	buffer.flip();
        	
        	
        	try {
				request.append(Server.utf8Decoder.decode(buffer));
			} catch (CharacterCodingException e) {
				e.printStackTrace();
			}
        	
        	buffer.clear();
        	
        	ch.read(buffer, buffer, this);
        	
        	System.out.println("GOT DATA:" + request.toString());
		}

		@Override
		public void failed(Throwable exc, ByteBuffer attachment) {
			System.err.println("ERROR: could not read data from client");
		}
		
	}
}
