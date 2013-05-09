import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.CharacterCodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Client {
	private final AsynchronousSocketChannel ch;
	private final ByteBuffer buffer;
	private final StringBuilder request;

	public Client(AsynchronousSocketChannel ch) {
		this.ch = ch;

		this.request = new StringBuilder();

		buffer = ByteBuffer.allocate(Server.BUFFER_SIZE);

		ClientReader reader = new ClientReader();
		ch.read(buffer, buffer, reader);

	}

	private void handleRequestIfComplete() {
		String strRequest = request.toString();
		System.out.println("Bytefuffer to string:" + strRequest);
		strRequest.indexOf("13101310");
		Scanner sc = new Scanner(strRequest);
		List<String> headers = new LinkedList<String>();

		Scanner httpCommand = new Scanner(sc.nextLine());

		String header = "";

		while (true) {
			header = sc.nextLine();
			if (header.equals("")) {
				break;
			}
			headers.add(header);
		}

		String type = httpCommand.next();

		if (type.toUpperCase().equals("GET")) {
			String url = httpCommand.next();
			System.out.println("GET REQUEST for URL:" + url);
			try {
				ch.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.err.println("Invalid HTTP request");
		}

	}

	private class ClientReader implements
			CompletionHandler<Integer, ByteBuffer> {

		//When the bytebuffer is successfully read?
		@Override
		public void completed(Integer result, ByteBuffer attachment) {
			buffer.flip();

			try {
				request.append(Server.utf8Decoder.decode(buffer));
			} catch (CharacterCodingException e) {
				e.printStackTrace();
			}

			handleRequestIfComplete();

			buffer.clear();
			if (ch.isOpen()) {
				ch.read(buffer, buffer, this);
			}

			// System.out.println("GOT DATA:" + request.toString());
		}

		@Override
		public void failed(Throwable exc, ByteBuffer attachment) {
			System.err.println("ERROR: could not read data from client");
		}

	}
}
