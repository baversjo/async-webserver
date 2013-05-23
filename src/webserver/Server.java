package webserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import middleware.ConnectionMiddleware;
import middleware.FileMiddleware;
import middleware.HTTPVersionMiddleware;
import middleware.MIMEMiddleware;
import middleware.Middleware;
import middleware.StaticHeadersMiddleware;

public class Server {
	public static final int BUFFER_SIZE = 8192; // 8kb
	public static final int VACUUM_TRIGGER = 100; // num connections before
													// vacuum
	public static final int VACUUM_LIMIT = 35 * 1000; // 35 seconds

	public static final LinkedList<Middleware> middlewares = new LinkedList<Middleware>();
	public static final String VERSION = "WIKING 0.2 (Java)";
	private static final int MAX_CLIENTS = 400; //1024 is default in ubuntu

	private WorkerThread[] workers;
	private int lastWorker;
	private int cores;
	private int port;

	public static void main(String args[]) {
		String portStr = System.getenv("PORT");
		int port = 0;
		if (portStr == null) {
			throw new RuntimeException(
					"Please specify the environment variable PORT");
		} else {
			port = Integer.parseInt(portStr);
		}
		if (port <= 0) {
			throw new RuntimeException("Invalid port specified (" + port + ")");
		}

		middlewares.add(new HTTPVersionMiddleware());
		middlewares.add(new StaticHeadersMiddleware());
		middlewares.add(new ConnectionMiddleware());
		middlewares.add(new MIMEMiddleware());
		middlewares.add(new FileMiddleware());

		new Server(port);
	}

	public Server(int port) {
		lastWorker = 0;
		cores = Runtime.getRuntime().availableProcessors();
		workers = new WorkerThread[cores];
		this.port = port;
		startServer();
	}

	private void startServer() {

		for (int i = 0; i < cores; i++) {
			WorkerThread worker = new WorkerThread(i,MAX_CLIENTS/cores);
			workers[i] = worker;
			worker.start();
		}

		ServerSocketChannel socketAccepter;
		Selector selector;
		try {

			socketAccepter = ServerSocketChannel.open();
			ServerSocket ss = socketAccepter.socket();
			ss.bind(new InetSocketAddress(port));
			socketAccepter.configureBlocking(false);

			selector = Selector.open();
			socketAccepter.register(selector, SelectionKey.OP_ACCEPT);

		} catch (IOException e) {
			System.err.println("Failed starting server.");
			e.printStackTrace();
			return;
		}

		System.out.println("Server accepting connections on port " + port);
		while (true) {
			try {
				selector.select(VACUUM_LIMIT / 4);
			} catch (IOException e) {
				System.err.println("Select failed");
				e.printStackTrace();
			}

			Set<SelectionKey> selectorKeys = selector.selectedKeys();
			Iterator<SelectionKey> keyIterator = selectorKeys.iterator();

			while (keyIterator.hasNext()) {
				SelectionKey key = keyIterator.next();
				keyIterator.remove();
				if (key.isAcceptable()) {
					SocketChannel clientChannel;
					try {
						clientChannel = socketAccepter.accept();
						clientChannel.configureBlocking(false);

						WorkerThread worker = workers[lastWorker];
						lastWorker++;
						if (lastWorker == cores) {
							lastWorker = 0;
						}

						Client client = new Client(clientChannel);
						worker.delegateClient(client);
						worker.block = true;
						worker.selector.wakeup();
						SelectionKey newKey = clientChannel.register(
								worker.selector, SelectionKey.OP_READ);
						client.key = newKey;
						worker.stopBlocking();

					} catch (IOException e) {
						System.err.println("Failed accepting connection");
						e.printStackTrace();
					}
				}
			}
		}
	}
}
