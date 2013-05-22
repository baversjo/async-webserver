package middleware;

import webserver.Request;
import webserver.Response;

public class ConnectionMiddleware implements Middleware {
	public static final String CONN_CLOSE = "Close";
	public static final String CONN_KEEP_ALIVE = "Keep-Alive";
	
	public static final String CONN_CLOSE_L = CONN_CLOSE.toLowerCase();
	public static final String CONN_KEEP_ALIVE_L = CONN_KEEP_ALIVE.toLowerCase();
	
	@Override
	public void execute(Request request, Response response)
			throws MiddlewareException {

		response.headers.put("Connection",CONN_CLOSE);
		String connHeader = request.headers.get("Connection");
		if(connHeader != null){
			connHeader = connHeader.toLowerCase();
		}
		String connRes = CONN_CLOSE;
		if(request.httpMinor == 0){
			
			if(connHeader != null && connHeader.equals(CONN_KEEP_ALIVE_L)){
				connRes = CONN_KEEP_ALIVE;
			}else{
				connRes = CONN_CLOSE;
			}
		}else if(request.httpMinor == 1){
			if(connHeader != null && connHeader.equals(CONN_CLOSE_L)){
				connRes = CONN_CLOSE;
			}else{
				connRes = CONN_KEEP_ALIVE;
			}
		}else{
			response.code = Response.STATUS_500;
			throw new MiddlewareException("Should not get here. We only support 1.0 and 1.1");
		}
		
		response.headers.put("Connection",connRes);
		
		if(connRes == CONN_KEEP_ALIVE){
			response.closeAfterEnd = false;
		}else{
			response.closeAfterEnd = true;
		}
	}

}
