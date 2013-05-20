package middleware;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
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
			
			final FileChannel fileChannel = FileChannel.open(absolute_path);
			response.sendFile(fileChannel);
			
			
		} catch (IOException e) {
			//TODO: 404
			System.err.println("TODO: handle 404");
		}
		
	}

}
