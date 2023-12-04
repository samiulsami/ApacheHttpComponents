package Main;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) {
        try{
            UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();
            mapper.register("/pushListener", new pushListener());
            HttpServer server = ServerBootstrap.bootstrap()
                    .setListenerPort(2450)
                    .setHandlerMapper(mapper).create();

            server.start();
            System.out.println("Server started");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static class pushListener implements HttpRequestHandler {

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context)throws IOException{
            String responseBody = "it worked";
            StringEntity entity = new StringEntity(responseBody, ContentType.TEXT_PLAIN);
            response.setStatusCode(HttpStatus.SC_OK);
            response.setEntity(entity);
        }
    }
}