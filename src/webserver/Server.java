package webserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import middleware.FileMiddleware;
import middleware.Middleware;


public class Server {
	public static final int BUFFER_SIZE = 8192; //8kb
	
	public static final CharsetEncoder utf8Encoder = Charset.forName("UTF-8").newEncoder();
	public static final CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder();
	
	public static final LinkedList<Middleware> middlewares = new LinkedList<Middleware>(); 
	
	
	private Map<SocketChannel, Client> connectedClients;
	
	public static void main(String args[]){
		String portStr = System.getenv("AWEB_PORT");
		int port = 0;
		if(portStr == null){
			throw new RuntimeException("Please specify the environment variable AWEB_PORT");
		}else{
			port = Integer.parseInt(portStr);
		}
        if(port <= 0){
        		throw new RuntimeException("Invalid port specified (" + port +")");
        }
        
        middlewares.add(new FileMiddleware());
        
        new Server(port);
	}
	
	public Server(int port){
		
		connectedClients = new HashMap<SocketChannel, Client>();
		
	    ServerSocketChannel server;
	    Selector selector;
		try {
			
			server = ServerSocketChannel.open();
		    ServerSocket ss = server.socket();
		    ss.bind(new InetSocketAddress(port));
		    server.configureBlocking(false);
		    
		    selector = Selector.open();
		    server.register(selector,SelectionKey.OP_ACCEPT);
		    
		} catch (IOException e) {
			System.err.println("Failed starting server.");
			e.printStackTrace();
			return;
		}
		
		System.out.println("Server accepting connections on port " + port);
		
	    while (true) {
	    		try{
	    			selector.select();
	    		}catch(IOException e){
	    			System.err.println("Select failed");
	    			e.printStackTrace();
	    		}
	
			Set<SelectionKey> ready = selector.selectedKeys();
			Iterator<SelectionKey> iterator = ready.iterator();
	
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				iterator.remove();
				if (key.isAcceptable()) {
					System.out.println("acceptable");
					SocketChannel channel;
					try {
						channel = server.accept();
						System.out.println("Accept new connection.");
						channel.configureBlocking(false);
						
					    channel.register(selector,SelectionKey.OP_READ);
					    Client client = new Client(channel);
					    connectedClients.put(channel, client);
					} catch (IOException e) {
						System.err.println("Failed accepting connection");
						e.printStackTrace();
					}

				}
				if(key.isReadable()){
					System.out.println("readable");
					SocketChannel channel = (SocketChannel)key.channel();
					Client client = connectedClients.get(channel);
					if(!client.doRead()){
						connectedClients.remove(channel);
						try { channel.close(); } catch (IOException e) {}
						key.cancel();
					}
				}
			}
	    }
	}
}
