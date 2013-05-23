package middleware;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import webserver.Request;
import webserver.Response;
import webserver.Server;

public class StaticHeadersMiddleware implements Middleware {
	
	
    public static final ThreadLocal<SimpleDateFormat> rfc1123Format =
        new ThreadLocal<SimpleDateFormat>() {
            @Override protected SimpleDateFormat initialValue() {
            	SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            	format.setTimeZone(TimeZone.getTimeZone("GMT"));
            	return format;
            }
        };


	@Override
	public void execute(Request request, Response response) throws MiddlewareException{
		response.headers.put("Date", rfc1123Format.get().format(new Date()));
		response.headers.put("Server", Server.VERSION);
	}

}
