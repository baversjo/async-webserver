package middleware;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;

import webserver.Request;
import webserver.Response;

public class FileMiddleware implements Middleware{
	private String document_root = System.getenv("AWEB_DOCUMENT_ROOT");
	@Override
	public void execute(final Request request, final Response response) {
		System.out.println(document_root);
		try {
			/*
			 *    /hello.html
			 *    TODO: http://google.se/hello.html
			 */
			String path = request.path;
			
			Path absolute_path = Paths.get(document_root,path);
			System.out.println("absolute:" + absolute_path.toString());
			
			final AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(absolute_path);
			CompletionHandler<Integer, Object> handler =
			    new CompletionHandler<Integer, Object>() {
			    @Override
			    public void completed(final Integer position, Object attachment) {
			        System.out.println(attachment + " completed with " + position + " bytes read");

		        	//TODO: forward data to response body
		        	CompletionHandler<Integer, Object> handler = new CompletionHandler<Integer, Object>() {
						@Override
						public void completed(Integer result,Object attachment) {
							response.body.clear();
				        	response.body.flip();
				        	
					        if(position == -1){
					        	response.end(); 
					        }else{
					        	fileChannel.read(response.body, position);
					        }
							
						}

						@Override
						public void failed(Throwable exc, Object attachment) {
							//TODO: close connection
							System.err.println("Error writing to connection");
						}
		        	};
			        
			    }
			    @Override
			    public void failed(Throwable e, Object attachment) {
			        System.err.println(attachment + " failed with:");
			        e.printStackTrace();
			        //TODO: 404
			        System.err.println("TODO: handle 404");
			    }
			};
			fileChannel.read(response.body, 0, null, handler);
			
		} catch (IOException e) {
			//TODO: 404
			System.err.println("TODO: handle 404");
		}
		
	}

}
