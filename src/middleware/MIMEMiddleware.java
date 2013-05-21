package middleware;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;

import webserver.Request;
import webserver.Response;

public class MIMEMiddleware implements Middleware {
	public static final HashMap<String, String> mimeTypes = new HashMap<String, String>();

	static {
		try {
			Scanner sc = new Scanner(new File("MIMEtypes.txt"));
			while (sc.hasNextLine()) {
				Scanner line = new Scanner(sc.nextLine());
				if (line.hasNext()) {
					String mimeType = line.next();
					while (line.hasNext()) {
						if (!mimeType.startsWith("#")) {
							String extensions = line.next();
							mimeTypes.put(extensions, mimeType);
						} else
							line.nextLine();
					}
				}
				line.close();
			}
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void execute(Request request, Response response)
			throws MiddlewareException {
		// request.path: /bajs.html [html]
		int index = request.path.lastIndexOf(".");
		int p = Math.max(request.path.lastIndexOf('/'),
				request.path.lastIndexOf('\\'));
		if (index > p) {
			String extension = request.path.substring(index + 1);
			String mimeType = mimeTypes.get(extension);
			response.headers.put("Content-Type", mimeType);
		}
		else {
			response.code = Response.STATUS_404;
			throw new MiddlewareException("MIME-type not found.");
		}
	}
}
