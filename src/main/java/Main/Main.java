package Main;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.*;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.UriHttpRequestHandlerMapper;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Main {

    private static final String JMAPServerURL = "http://localhost:80/jmap";
    private static final int pushServerPort = 3333;

    private static final String username = "testuser@mydomain";
    private static final String password = "password";

    public static void main(String[] args) {
        try{
            UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();
            mapper.register("/pushListener", new pushListener());
            HttpServer server = ServerBootstrap.bootstrap()
                    .setListenerPort(pushServerPort)
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
                    String responseBody = "";
                    String requestBody = InputStreamToString(((HttpEntityEnclosingRequest) request).getEntity().getContent());
                    int responseCode = 201;
                    System.out.println(requestBody);
                    try{
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    try{
                        ObjectMapper objectMapper = new ObjectMapper();
                        var mp = objectMapper.readValue(requestBody, new TypeReference<HashMap<String, String>>() {});
                        String pushSubscriptionId = mp.get("pushSubscriptionId");
                        String verificationCode = mp.get("verificationCode");
                       // System.out.println(pushSubscriptionId + " " + verificationCode);
                        String FormattedRequestBody = FormatRequestBody(pushSubscriptionId, verificationCode);
                        updatePushSubscription(JMAPServerURL, FormattedRequestBody);
                        try{
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        responseCode = HttpStatus.SC_UNPROCESSABLE_ENTITY;
                    }
                    //responseBody = requestBody;
                    StringEntity entity = new StringEntity(responseBody, ContentType.TEXT_PLAIN);
                    response.setStatusCode(responseCode);
                    response.setEntity(entity);
                    break;
                }
                default:
                    response.setStatusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
            }
        }


        private static void updatePushSubscription(String url, String requestBody) throws Exception{

            try(DefaultHttpClient httpClient = new DefaultHttpClient()){
                HttpPost postRequest = new HttpPost(url);
                postRequest.addHeader("Content-Type", "application/json; charset=utf-8");
                postRequest.addHeader("Authorization", "Basic dGVzdHVzZXJAbXlkb21haW46cGFzc3dvcmQ=");
                postRequest.setEntity(new StringEntity(requestBody));
                HttpResponse response = httpClient.execute(postRequest);

                {
                    HttpEntity entity = response.getEntity();
                    String responseString = EntityUtils.toString(entity, "UTF-8");
                    System.out.println(responseString);
                }

                int statusCode = response.getStatusLine().getStatusCode();
                if(statusCode != 200){
                    throw new RuntimeException("Failed with HTTP error code: " + statusCode);
                }
            }
            catch (Exception e){
                System.out.println("Error connecting to server\n");

                e.printStackTrace();
            }
        }

        /*
        {
              "using": [ "urn:ietf:params:jmap:core", "urn:ietf:params:jmap:mail"],
              "methodCalls": [
                [ "PushSubscription/set", {
                        "update": {
                            "3b1249a4-e800-4dba-97e1-6ba60be1ce9b": {
                                 "verificationCode":"6e579a03-7e15-4af1-925e-04494312248a"
                                }
                        }
                    }, "0" ]
              ]
            }
         */

        private static String FormatRequestBody(String pushSubscriptionId, String verificationCode)throws Exception{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.createObjectNode();

            ObjectNode verificationCodeNode = mapper.createObjectNode();
            verificationCodeNode.put("verificationCode", verificationCode);

            ObjectNode pushSubscriptionIdNode = mapper.createObjectNode();
            pushSubscriptionIdNode.set(pushSubscriptionId, verificationCodeNode);

            ObjectNode updateNode = mapper.createObjectNode();
            updateNode.set("update", pushSubscriptionIdNode);

            ArrayNode methodCallsNode = mapper.createArrayNode();
            methodCallsNode.add("PushSubscription/set");
            methodCallsNode.add(updateNode);
            methodCallsNode.add("0");

            ArrayNode methodCallsArrayNode = mapper.createArrayNode();
            methodCallsArrayNode.add(methodCallsNode);

            //ObjectNode methodCallsObjectNode = mapper.createObjectNode();
            //methodCallsObjectNode.set("methodCalls", methodCallsArrayNode);

            ArrayNode usingArrayNode = mapper.createArrayNode();
            usingArrayNode.add("urn:ietf:params:jmap:core");
            usingArrayNode.add("urn:ietf:params:jmap:mail");

            //ObjectNode usingObjectNode = mapper.createObjectNode();
           // usingObjectNode.set("using", usingNode);

            ((ObjectNode)root).set("using",usingArrayNode);
            ((ObjectNode)root).set("methodCalls",methodCallsArrayNode);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
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