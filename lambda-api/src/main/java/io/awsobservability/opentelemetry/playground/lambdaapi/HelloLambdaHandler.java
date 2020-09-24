package io.awsobservability.opentelemetry.playground.lambdaapi;

import static java.util.stream.Collectors.toMap;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class HelloLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        var response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setHeaders(request.getHeaders().entrySet()
                                   .stream()
                                   .map(e -> Map.entry("received-" + e.getKey(), e.getValue()))
                                   .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

        HttpClient httpclient = HttpClients.createDefault();
        for(int i = 0; i < 3; i++) {
            try {
                HttpResponse httpResponse = httpclient.execute(new HttpGet("http://httpbin.org/"));
                httpResponse.getEntity().getContent().readAllBytes();
            } catch (Exception e) {
            }
        }

        Throwable t = new Throwable();
        StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));
        response.setBody("I'm lambda!\n" + writer.toString());
        return response;
    }
}
