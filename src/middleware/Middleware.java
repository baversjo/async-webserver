package middleware;

import webserver.Request;
import webserver.Response;

public interface Middleware {
	public void execute(Request request, Response response) throws MiddlewareException;
}
