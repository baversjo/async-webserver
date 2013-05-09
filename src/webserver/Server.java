package webserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class Server {
	public static final int BUFFER_SIZE = 10000;
	
	public static final CharsetEncoder utf8Encoder = Charset.forName("UTF-8").newEncoder();
	public static final CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder();
	
	private int port;
	
	public static void main(String args[]){
        int port = 8060;
        new Server(port);
	}
	
	public Server(int port){
		this.port = port;
		AsynchronousChannelGroup group;
		
        try {
        	
        	group = AsynchronousChannelGroup.withThreadPool(Executors
                    .newSingleThreadExecutor());
        	
            final AsynchronousServerSocketChannel serverSocket = AsynchronousServerSocketChannel.open(group);
            serverSocket.bind(new InetSocketAddress(this.port));


            serverSocket.accept("Client connection", 
                    new CompletionHandler<AsynchronousSocketChannel, Object>() {
                public void completed(AsynchronousSocketChannel ch, Object att) {
                    System.out.println("Accepted a connection");

                    // accept the next connection
                    serverSocket.accept("Client connection", this);

                    // handle this connection
                    Client client = new Client(ch);
                }

                public void failed(Throwable exc, Object att) {
                    System.out.println("Failed to accept connection");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        
        System.out.println("Server accepting conenctions on port " + port);
        
        
        // wait until group.shutdown()/shutdownNow(), or the thread is interrupted:
        try {
			group.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			System.out.println("Shutting down");
			return;
		}

	}
	
	/*
	protected void handleConnection(final AsynchronousSocketChannel ch){
		
		
		
		String response = "Buy more ฿itcoins!\n";
		
		CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
		ByteBuffer buf;
		try {
			buf = encoder.encode(CharBuffer.wrap(response));
		} catch (CharacterCodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		ch.write(buf);
		try {
			ch.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	*/
}
