
# AWS X-Ray Sampling Specification

To ensure efficient tracing and provide a representative sample of the requests that your application serves, the X-Ray SDK applies a **sampling** algorithm to determine which requests get traced.  

## Workflow

In a distributed application, upon a client request arrives, the first traced service (entry service) is responsible for deciding whether the request will be traced or not. The decision will be propagated to all downstream services that participates in this distributed call. 

## Sampling Rule

Sampling rules tell the X-Ray SDK how many requests to record for a set of criteria.  

### Sampling Rate

By default, the X-Ray SDK records the first request each second, and five percent of any additional requests. One request per second is the **_reservoir (or fixed_target)_**. This ensures that at least one trace is recorded each second as long as the service is serving requests. Five percent is the **_rate_** at which additional requests beyond the reservoir size are sampled.  

### Sampling Rule Options

X-Ray supports key-based sampling which applies a sampling rule only when the request attributes matches the rule’s criteria. While a request may match multiple sampling rules, only the rule with highest priority is honoured.  
  
The following options are available for each rule. String values can use wildcards to match a single character (`?`) or zero or more characters (`*`).  

-   **Rule name (or description)** (string) – A unique name for the rule.
-   **Priority** (integer between 1 and 9999) – The priority of the sampling rule. Services evaluate rules in ascending order of priority, and make a sampling decision with the first rule that matches.
-   **Reservoir** (non-negative integer) – A fixed number of matching requests to instrument per second, before applying the fixed rate. The reservoir is not used directly by services, but applies to all services using the rule collectively.
-   **Rate** (number between 0 and 100) – The percentage of matching requests to instrument, after the reservoir is exhausted. The rate may be an integer or a float.
-   **Service name** (string) – The name of the instrumented service, as it appears in the service map. In X-Ray SDK, it is the service name that you configure on the recorder.
-   **Service type** (string) – The service type, as it appears in the service map. For the X-Ray SDK, set the service type by applying the appropriate plugin:
	-   `AWS::ElasticBeanstalk::Environment` – An AWS Elastic Beanstalk environment (plugin).
	-   `AWS::EC2::Instance` – An Amazon EC2 instance (plugin).
	-   `AWS::ECS::Container` – An Amazon ECS container (plugin).
	-   `AWS::APIGateway::Stage` – An Amazon API Gateway stage.
	-   `AWS::AppSync::GraphQLAPI` – An AWS AppSync API request.
-   **Host** (string) – The hostname from the HTTP host header.
-   **HTTP method** (string) – The method of the HTTP request.
-   **URL path** (string) – The path portion of the HTTP request URL.
-   **Resource ARN** (string) – The ARN of the AWS resource running the service.
	-   X-Ray SDK – Not supported. The SDK can only use rules with **Resource ARN** set to `*`.
	-   Amazon API Gateway – The stage ARN.

### Examples

**Example – Default rule with no reservoir and a low rate**  
You can modify the default rule's reservoir and rate. The default rule applies to requests that don't match any other rule.  

-   **Reservoir** – `0`
-   **Rate** – `0.005` (0.5 percent)

**Example – Higher minimum rate for POSTs**  

-   **Rule name** – `POST minimum`
-   **Priority** – `100`
-   **Reservoir** – `10`
-   **Rate** – `0.10`
-   **Service name** – `*`
-   **Service type** – `*`
-   **Host** – `*`
-   **HTTP method** – `POST`
-   **URL path** – `*`
-   **Resource ARN** – `*`

## Work Modes

### Dynamic (Centralized) Sampling

The default and recommended sampling work mode is Dynamic Sampling. In this mode, you can define sampling rules in the X-Ray console, then X-Ray SDK will periodically read the latest sampling rules from the X-Ray service. The service manages the reservoir for each rule, and assigns quotas to each instance of your service to distribute the reservoir evenly, based on the number of instances that are running. The reservoir limit is calculated according to the rules you set. And because the rules are configured in the service, you can manage rules without making additional deployments.

### Static (Local) Sampling

Alternatively, you can configure the X-Ray SDK to read sampling rules from a local JSON document that you include with your code. However, when you run multiple instances of your service, each instance performs sampling independently. This causes the overall percentage of requests sampled to increase because the reservoirs of all of the instances are effectively added together. Additionally, to update local sampling rules, you need to redeploy your code.  
  
Below is an example local sampling `sampling-rule.json` file:  

```
{
  "version": 2,
  "default": {
    "fixed_target": 1,
    "rate": 0.05
  },
  "rules": [
    {
      "description": "GetBookCalls",
      "host ": "",
       "http_method": "GET",
      "service_name": "BookService",
      "url_path": "/api/book/",
       "fixed_target": 1,
       "rate": 0.10
    },
    {
      "description": "HealthCheck",
       "host ": "",
      "http_method": "*",
      "url_path": "/api/healthcheck",
      "fixed_target": 0,
      "rate": 0.00
    }
  ]
}
```

## Propagating Sampling Decision

The sampling decision and trace ID are added to HTTP requests in **tracing headers** named `X-Amzn-Trace-Id`. The first X-Ray-integrated service that the request hits adds a tracing header, which is read by the X-Ray SDK and included in the response.

**Example Tracing header with root trace ID and sampling decision**

`X-Amzn-Trace-Id: Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=1`
