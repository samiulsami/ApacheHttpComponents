package Main;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.HashMap;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        try{
            UriHttpRequestHandlerMapper mapper = new UriHttpRequestHandlerMapper();
            mapper.register("/pushListener", new pushListener());
            HttpServer server = ServerBootstrap.bootstrap()
                    .setListenerPort(3332)
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
                    int responseCode = HttpStatus.SC_OK;
                    //System.out.println(requestBody);
                    try{
                        ObjectMapper objectMapper = new ObjectMapper();
                        var mp = objectMapper.readValue(requestBody, new TypeReference<HashMap<String, String>>() {});
                        String pushSubscriptionId = mp.get("pushSubscriptionId");
                        String verificationCode = mp.get("verificationCode");
                       // System.out.println(pushSubscriptionId + " " + verificationCode);
                        updatePushSubscription(pushSubscriptionId, verificationCode);
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
        private static void updatePushSubscription(String pushSubscriptionId, String verificationCode)throws Exception{
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

            String json = null;
            json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            System.out.println(json);
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