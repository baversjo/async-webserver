package middleware;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import webserver.Request;
import webserver.Response;
import webserver.Server;

public class StaticHeadersMiddleware implements Middleware {
	
	public final static DateFormat rfc1123Format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	
	static {
        rfc1123Format.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

	@Override
	public void execute(Request request, Response response) throws MiddlewareException{
		response.headers.put("Date", rfc1123Format.format(new Date()));
		response.headers.put("Server", Server.VERSION);
	}

}
