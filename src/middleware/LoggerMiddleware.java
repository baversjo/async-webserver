package middleware;

import java.text.SimpleDateFormat;
import java.util.Date;

import webserver.Request;
import webserver.Response;

public class LoggerMiddleware implements Middleware {

	@Override
	public void execute(Request request, Response response)
			throws MiddlewareException {
		Date d = new Date();
		SimpleDateFormat format = new SimpleDateFormat();
		StringBuilder sb = new StringBuilder(format.format(d));
		sb.append(" [");
		sb.append(Thread.currentThread().getName());
		sb.append("] ");
		sb.append(request.httpMethod.name());
		sb.append(" ");
		sb.append(request.path);
		sb.append(" ");
		sb.append(request.ip);
		String ua = request.headers.get("user-agent");
		if(ua != null){
			sb.append(" ");
			sb.append(ua);	
		}
		System.out.println(sb.toString());

	}

}
