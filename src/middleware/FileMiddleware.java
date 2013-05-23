package middleware;

import http_parser.HTTPMethod;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Date;

import webserver.Request;
import webserver.Response;

public class FileMiddleware implements Middleware {
	private String document_root;

	public FileMiddleware() {
		document_root = System.getenv("AWEB_DOCUMENT_ROOT");
		if (document_root == null) {
			throw new RuntimeException(
					"Please specify the environment variable AWEB_DOCUMENT_ROOT");
		} else {
			File f = new File(document_root);
			if (!f.canRead()) {
				throw new RuntimeException(
						"Please make sure the server can access the directory '"
								+ document_root + "'");
			}
		}
	}

	@Override
	public void execute(final Request request, final Response response)
			throws MiddlewareException {
		if (request.httpMethod == HTTPMethod.HTTP_GET
				|| request.httpMethod == HTTPMethod.HTTP_HEAD) {
			try {
				/*
				 * /hello.html TODO: http://google.se/hello.html
				 */
				String path = request.path;

				Path absolute_path = Paths.get(document_root, path).normalize();
				
				
				
				if(absolute_path.toString().indexOf(document_root) < 0){
					error(response,"Tried to access file outside of document root", Response.STATUS_404);
				}
				
				boolean sendFile = true;
				String ifModifiedSince = request.headers
						.get("if-modified-since");
				
				File f = new File(absolute_path.toString());
				Date modifiedOnServer = new Date(f.lastModified());
				
				if(ifModifiedSince != null){
					try {
						Date modifiedOnClient =  StaticHeadersMiddleware.rfc1123Format
								.get().parse(ifModifiedSince);
						
						if (modifiedOnServer.getTime() <= modifiedOnClient.getTime()) {
							sendFile = false;
						}
					}
					catch (ParseException e) {}
					catch (NumberFormatException e) {}
				}
				
				if(sendFile){
					FileChannel fileChannel = FileChannel
							.open(absolute_path);
					
					long size = fileChannel.size();
					response.headers.put("Content-Length",
							String.valueOf(fileChannel.size()));
					
					response.headers.put("Last-Modified", 
							StaticHeadersMiddleware.rfc1123Format
								.get().format(modifiedOnServer));
					
					response.code = Response.STATUS_200;
					if (request.httpMethod == HTTPMethod.HTTP_GET) {
						response.sendFile(fileChannel, size);// will send
					}
					fileChannel.close();
				}
				else {
					response.code = Response.STATUS_304;
					response.headers.remove("Content-Type");
				}

			} catch (IOException e) {
				error(response,"File not found", Response.STATUS_404);
			}
		}
	}
	
	private void error(Response response, String message, byte[] responseCode) throws MiddlewareException{
		response.code = responseCode;
		response.headers.remove("Content-Type");
		response.headers.put("Content-Length","0");
		throw new MiddlewareException(message);
	}
}
