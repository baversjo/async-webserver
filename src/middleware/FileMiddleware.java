package middleware;

import http_parser.HTTPMethod;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

				Path absolute_path = Paths.get(document_root, path);
				System.out.println("absolute:" + absolute_path.toString());
				try {
					String ifModifiedSince = request.headers
							.get("If-Modified-Since");
					Date modifiedOnClient = StaticHeadersMiddleware.rfc1123Format
							.parse(ifModifiedSince);
					Date modifiedOnServer = new Date(new File(
							absolute_path.toString()).lastModified());
					if (modifiedOnServer.compareTo(modifiedOnClient) > 0) {
						final FileChannel fileChannel = FileChannel
								.open(absolute_path);
						long size = fileChannel.size();
						response.headers.put("Content-Length",
								String.valueOf(fileChannel.size()));
						response.code = Response.STATUS_200;
						if (request.httpMethod == HTTPMethod.HTTP_GET) {
							response.sendFile(fileChannel, size);// will send
							// headers and
							// end
						} else {
							response.sendHeaders();
							response.end();
						}
					}

					else {
						response.code = Response.STATUS_304;
						response.headers.remove("Content-Type");
						response.sendHeaders();
						response.end();
						//TODO: Send header
					}
				} catch (ParseException e) {
					// TODO: Throw exception or system out print?
					e.printStackTrace();
				}

			} catch (IOException e) {
				response.code = Response.STATUS_404;
				response.sendHeaders();
				response.end();
				throw new MiddlewareException("File not found");
			}
		}
	}
}