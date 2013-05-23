package webserver;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import middleware.ConnectionMiddleware;
import middleware.FileMiddleware;
import middleware.HTTPVersionMiddleware;
import middleware.LoggerMiddleware;
import middleware.MIMEMiddleware;
import middleware.Middleware;
import middleware.StaticHeadersMiddleware;

public class WorkerThread extends Thread {

	private volatile Map<SocketChannel, Client> connectedClients;
	private volatile PriorityQueue<Client> connectedClientsSorted;
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
		
		connectedClients = new HashMap<SocketChannel, Client>();
		connectedClientsSorted = new PriorityQueue<Client>(
				Server.VACUUM_TRIGGER);
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
		connectedClients.put(client.ch, client);
		connectedClientsSorted.add(client);
		newConnectionsSinceVacuum++;
	}

	public synchronized void run() {
		System.out.println("Worker thread " + threadId + " started");
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

			while (keyIterator.hasNext()) {
				SelectionKey key = keyIterator.next();

				keyIterator.remove();
				if (key.isReadable()) {
					SocketChannel channel = (SocketChannel) key.channel();
					Client client = connectedClients.get(channel);
					if (client.requestIsEmpty()) {
						if (!client.doRead()) {
							closeClient(client);
						}
					}
				
				}
			}

			if (newConnectionsSinceVacuum > Server.VACUUM_TRIGGER) {
				System.out.println("VACUUM "+threadId+" ==================================");
				long now = System.currentTimeMillis();
				int nbrOfClients = connectedClientsSorted.size();
				while (nbrOfClients > max_clients ||
						(nbrOfClients > 0 && 
						connectedClientsSorted.peek().lastCommunication + Server.VACUUM_LIMIT < now)
				) {
					System.out.println("	vacuum close");
					Client client = connectedClientsSorted.poll();
					closeClient(client);
					nbrOfClients--;
				}
				newConnectionsSinceVacuum = 0;
				System.out.println("END "+threadId+" ==================================");
			}
		}
	}

	private void closeClient(Client client) {
		SocketChannel channel = client.ch;
		connectedClients.remove(channel);
		try {
			channel.close();
		} catch (IOException e) {
		}
		client.key.cancel();
		client.lastCommunication = 0;
		//System.out.println("client close.");
	}

	public synchronized void stopBlocking() {
		notify();
	}

}
