/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

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
