package middleware;

import webserver.Request;
import webserver.Response;

public class ConnectionMiddleware implements Middleware {

	@Override
	public void execute(Request request, Response response)
			throws MiddlewareException {
		
		response.headers.put("Connection","Close");

	}

}
