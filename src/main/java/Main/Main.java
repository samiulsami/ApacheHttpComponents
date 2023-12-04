package Main;

import org.apache.http.HttpEntityEnclosingRequest;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        try{
            UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();
            mapper.register("/pushListener", new pushListener());
            HttpServer server = ServerBootstrap.bootstrap()
                    .setListenerPort(3323)
                    .setHandlerMapper(mapper).create();

            server.start();
            System.out.println("Server started");
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    static class pushListener implements HttpRequestHandler {

        @Override
        public void handle(HttpRequest request, HttpResponse response, HttpContext context)throws IOException{
            {
                String path = request.getRequestLine().getUri();
                System.out.println("Request received\npath: " + path + "\n--------------------\n");
            }

            switch(request.getRequestLine().getMethod()) {
                case "POST": {
                    String requestBody = ((HttpEntityEnclosingRequest)request).getEntity().toString();
                    String responseBody = "test";
                    responseBody = InputStreamToString(((HttpEntityEnclosingRequest) request).getEntity().getContent());
                    StringEntity entity = new StringEntity(responseBody, ContentType.TEXT_PLAIN);
                    response.setStatusCode(HttpStatus.SC_OK);
                    response.setEntity(entity);
                    break;
                }
                default:
                    response.setStatusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
            }
        }
    }

    private static String InputStreamToString(InputStream is){
        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is))){
            return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
        catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

}