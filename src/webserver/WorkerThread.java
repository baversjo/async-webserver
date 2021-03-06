package webserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import middleware.ConnectionMiddleware;
import middleware.FileMiddleware;
import middleware.HTTPVersionMiddleware;
import middleware.LoggerMiddleware;
import middleware.MIMEMiddleware;
import middleware.Middleware;
import middleware.StaticHeadersMiddleware;

public class WorkerThread extends Thread {

	private PriorityQueue<Client> clients;
	private ConcurrentLinkedQueue<Client> newClients;
	
	protected Selector selector;
	private volatile int newConnectionsSinceVacuum;
	private int threadId;
	public volatile boolean block;
	private int max_clients;
	
	public Middleware[] middlewares;

	public WorkerThread(int i, int max_clients) {
		super("Worker " + i);
		this.threadId = i;
		
		middlewares = new Middleware[6];
		middlewares[0] = new LoggerMiddleware();
		middlewares[1] = new HTTPVersionMiddleware();
		middlewares[2] = new StaticHeadersMiddleware();
		middlewares[3] = new ConnectionMiddleware();
		middlewares[4] = new MIMEMiddleware();
		middlewares[5] = new FileMiddleware();
		
		clients = new PriorityQueue<Client>(
				Server.VACUUM_TRIGGER);
		
		newClients = new ConcurrentLinkedQueue<Client>();
		newConnectionsSinceVacuum = 0;
		block = false;
		this.max_clients = max_clients;

		try {
			selector = Selector.open();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public void delegateClient(Client client) {
		newClients.offer(client);
		newConnectionsSinceVacuum++;
	}

	public synchronized void run() {
		System.out.println("Worker thread " + threadId + " started");
		
		ByteBuffer readBuffer = ByteBuffer.allocate(Server.BUFFER_SIZE);
		
		while (true) {

			if (block) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
				block = false;
			}
			try {
				selector.select(Server.VACUUM_LIMIT / 4);
			} catch (IOException e) {
				System.err.println("Select failed");
				e.printStackTrace();
			}

			Set<SelectionKey> selectorKeys = selector.selectedKeys();
			Iterator<SelectionKey> keyIterator = selectorKeys.iterator();
			
			//add new clients
			while(!newClients.isEmpty()){
				clients.offer(newClients.poll());
			}

			while (keyIterator.hasNext()) {
				SelectionKey key = keyIterator.next();

				keyIterator.remove();
				if (key.isValid() && key.isReadable()) {
					Client client = (Client) key.attachment();
					if (client.requestIsEmpty()) {
						clients.remove(client);
						if (!client.doRead(readBuffer)) {
							client.close();
						}else{
							clients.offer(client);
						}
					}
				}
				if(key.isValid() && key.isWritable()){
					Client client = (Client) key.attachment();
					if(!client.doWrite()){
						clients.remove(client);
						client.close();
					}
				}
			}
			
			if (newConnectionsSinceVacuum > Server.VACUUM_TRIGGER) {
				System.out.println("VACUUM "+threadId+" ==================================");
				long now = System.currentTimeMillis();
				int nbrOfClients = clients.size();
				while (nbrOfClients > max_clients ||
						(nbrOfClients > 0 && 
						clients.peek().lastCommunication + Server.VACUUM_LIMIT < now)
				) {
					System.out.println("	vacuum close");
					Client client = clients.poll();
					client.close();
					nbrOfClients--;
				}
				newConnectionsSinceVacuum = 0;
				System.out.println("END "+threadId+" ==================================");
			}
		}
	}

	public synchronized void stopBlocking() {
		notify();
	}

}
