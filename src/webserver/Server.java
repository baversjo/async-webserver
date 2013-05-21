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
import java.util.PriorityQueue;
import java.util.Set;

import middleware.ConnectionMiddleware;
import middleware.FileMiddleware;
import middleware.HTTPVersionMiddleware;
import middleware.MIMEMiddleware;
import middleware.Middleware;
import middleware.StaticHeadersMiddleware;


public class Server {
	public static final int BUFFER_SIZE = 8192; //8kb
	public static final int VACUUM_TRIGGER = 100; //num connections before vacuum
	public static final int VACUUM_LIMIT = 35*1000; //35 seconds
	
	public static final CharsetEncoder utf8Encoder = Charset.forName("UTF-8").newEncoder();
	public static final CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder();
	
	public static final LinkedList<Middleware> middlewares = new LinkedList<Middleware>();
	public static final String VERSION = "AWEB 0.1 (Java)"; 
	
	
	private Map<SocketChannel, Client> connectedClients;
	private PriorityQueue<Client> connectedClientsSorted;
	
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
        
        middlewares.add(new HTTPVersionMiddleware());
        middlewares.add(new StaticHeadersMiddleware());
        middlewares.add(new ConnectionMiddleware());
        middlewares.add(new MIMEMiddleware());
        middlewares.add(new FileMiddleware());
        
        new Server(port);
	}
	
	public Server(int port){
		
		connectedClients = new HashMap<SocketChannel, Client>();
		
		connectedClientsSorted = new PriorityQueue<Client>(VACUUM_TRIGGER);
		
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
		int newConnectionsSinceVacuum = 0; 
	    while (true) {
	    		try{
	    			selector.select(VACUUM_LIMIT/4);
	    		}catch(IOException e){
	    			System.err.println("Select failed");
	    			e.printStackTrace();
	    		}
	
			Set<SelectionKey> ready = selector.selectedKeys();
			Iterator<SelectionKey> iterator = ready.iterator();
	
			while (iterator.hasNext()) {
				SelectionKey key = iterator.next();
				System.out.println("event" + " " + key.isReadable() + 
						" " + key.isWritable() + 
						" " + key.isAcceptable() +
						" " + key.isConnectable() +
						" " + key.isValid());
				iterator.remove();
				if (key.isAcceptable()) {
					System.out.println("acceptable");
					SocketChannel channel;
					try {
						channel = server.accept();
						System.out.println("Accept new connection.");
						channel.configureBlocking(false);
						
						SelectionKey newKey = channel.register(selector,SelectionKey.OP_READ);
					    Client client = new Client(channel,newKey);
					    connectedClients.put(channel, client);
					    connectedClientsSorted.add(client);
					    newConnectionsSinceVacuum++;
					} catch (IOException e) {
						System.err.println("Failed accepting connection");
						e.printStackTrace();
					}

				}
				if(key.isReadable()){
					SocketChannel channel = (SocketChannel)key.channel();
					Client client = connectedClients.get(channel);
					if(!client.doRead()){
						System.out.println("Closing connection");
						closeClient(client);
					}
				}
			}
			if(newConnectionsSinceVacuum > VACUUM_TRIGGER){
				System.out.println("VACUUM");
				long now = System.currentTimeMillis();
				while(
						connectedClientsSorted.size() > 0 &&
						connectedClientsSorted.peek().lastCommunication + VACUUM_LIMIT < now
				){
					Client client = connectedClientsSorted.poll();
					closeClient(client);
				}
				newConnectionsSinceVacuum = 0;
			}
	    }
	}
	
	private void closeClient(Client client){
		SocketChannel channel = client.ch;
		connectedClients.remove(channel);
		try { channel.close(); } catch (IOException e) {}
		client.key.cancel();
		client.lastCommunication = 0;
		System.out.println("client close.");
	}
}
