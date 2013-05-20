package sendfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import com.sun.org.apache.bcel.internal.generic.DSTORE;
import com.sun.org.apache.bcel.internal.generic.NEW;

public class TransferToClient {
	
	public static void main(String[] args) throws IOException{
		TransferToClient sfc = new TransferToClient();
		sfc.testSendfile();
	}
	public void testSendfile() throws IOException {
	    String host = "localhost";
	    int port = 9026;
	    
	    ServerSocketChannel server = ServerSocketChannel.open();
	    ServerSocket ss = server.socket();
	    ss.bind(new InetSocketAddress(port));
	    server.configureBlocking(false);
	    
	    SocketChannel channels[] = new SocketChannel[2];
	    long nr[] = new long[2];
	    SelectionKey keys[] = new SelectionKey[2];

	    Selector selector = Selector.open();
	    server.register(selector,SelectionKey.OP_ACCEPT);
	    
	    while (true) {

			selector.select();
	
			Set ready = selector.selectedKeys();
			Iterator iterator = ready.iterator();
	
			while (iterator.hasNext()) {
				SelectionKey key = (SelectionKey) iterator.next();
				
				System.out.println("Channel event:" + key.isWritable() + " " + key.isAcceptable() + key.isReadable() + " " + key.isConnectable() + " " + key.isValid());
				
			    iterator.remove();
			    if (key.isAcceptable()) {
					SocketChannel channel = server.accept();
					System.out.println("Accept");
				    channels[0] = channel;
				    channel.configureBlocking(false);
				    keys[0] = channel.register(selector,SelectionKey.OP_READ);
				    nr[0] = 0;
				    
				    String fname = "webroot/hej.html";
				    long fsize = 183678375L;
				    
				    FileChannel fc = new FileInputStream(fname).getChannel();
				    long start = System.currentTimeMillis();
				    
				    channel.write(ByteBuffer.wrap("HEADERS!!!\r\n".getBytes()));

				    long curnset = fc.transferTo(0, fsize, channel);
				    System.out.println("total bytes transferred--"+curnset+" and time taken in MS--"+(System.currentTimeMillis() - start));
			    }
			    if(key.isReadable()){
			    		System.out.println("readable");
			    		SocketChannel channel = (SocketChannel)key.channel();
			    		ByteBuffer bff = ByteBuffer.allocate(1000);
			    		int read = channel.read(bff);
			    		byte[] chars = new byte[1000];
			    		bff.get(chars);
			    		String s = new String(chars);
			    		System.out.println(s);
			    		channel.close();
			    }
			    /*if(key.isWritable()){
			    		SocketChannel channel = (SocketChannel)key.channel();
			    		
			    }*/
			}
	    }
	  }


}
