# X-Ray Instrumentation Playground

This repository contains a toy application which exhibits a simple yet somewhat representative trace,
including AWS SDK calls, a frontend and backend, and HTTP and gRPC.

The same application is instrumented in several ways, allowing us to compare the experience when viewing
traces for the different types of instrumentation.

Current instrumentation includes
- OpenTelemetry Auto Instrumentation + OpenTelemetry Collector
- X-Ray SDK Instrumentation + X-Ray Daemon
  - Does not instrument many libraries like gRPC and Lettuce

## Setting up AWS resources

The playground access various endpoints hosted on AWS. Feel free to skip this section to just see traces with local endpoints.

To set up AWS resources you will need Terraform, available [here](https://www.terraform.io/downloads.html).

First, make sure your have configured AWS credentials using the AWS CLI as described [here](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html).

Then, navigate to the scripts/terraform directory and run

```
$ terraform init
$ terraform apply
```

This will take some time as it provisions resources. Note that this also generates `terraform.tfstate` files in the
same directory. DO NOT LOSE THESE - without these files, Terraform will not be able to cleanup after you are done with the
resources.

After it completes, three output values will be printed. Open `docker-compose.yml` and find the four values under `#AWS Provisioned resources`
Change the values so

- `API_GATEWAY_ENDPOINT` is set to the output value `lambda_api_gateway_url`
- `ECS_ENDPOINT` is set to the output value `ecs_url`
- `EKS_ENDPOINT` is set to the output value `eks_fargate_url`
- `OTEL_ENDPOINT_PEER_SERVICE_MAPPING` - replace the keys for hello-lambda-api, ecs-backend, eks-backend to the domains for these three values

## Running

Make sure Docker is installed and run

`$ docker-compose up`

Access `http://localhost:9080/`.

Then visit the X-Ray console, for example [here](https://ap-northeast-1.console.aws.amazon.com/xray/home?region=ap-northeast-1#/traces)
and you should see multiple traces corresponding to the request you made.

The app uses normal [AWS credentials](https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/setup-credentials.html).
If you have trouble running after using the CLI to run `aws configure`, try setting the environment variables as described
on that page, in particular `AWS_REGION`.

Note that the `dynamodb-table` is only to create the table once, so it is normal for it to exist after creating the table.

If you see excessive deadline exceeded errors or the page doesn't respond properly, your Docker configuration may not have enough RAM.
We recommend setting Docker to 4GB of RAM for a smooth experience.

## Cleaning up

If you provisioned AWS resources above, run `terraform deploy` to clean them up.

## How it works

The playground is composed of two observability components in addition to the business logic actually being monitored.

- [OpenTelemetry Java Agent](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [OpenTelemetry Collector](https://github.com/open-telemetry/opentelemetry-collector-contrib)

The recommended way to get started for your app is to run the Docker image for the collector from [here](https://hub.docker.com/r/otel/opentelemetry-collector-contrib-dev).
The collector listens on port 55680 for telemetry.

You will need to provide a path to a configuration file with the `--config` parameter when running. This basic configuration
will work for X-Ray.

```yaml
receivers:
  otlp:
    protocols:
      grpc:
exporters:
  logging:
    loglevel: info
  awsxray:
    local_mode: true
processors:
  memory_limiter:
    limit_mib: 100
    check_interval: 5s
service:
  pipelines:
    traces:
      processors:
      - memory_limiter
      receivers:
      - otlp
      exporters:
      - logging
      - awsxray
      # Feel free to add more exporters if you use e.g., Zipkin, Jaeger
```

If you have AWS credentials configured and both apps running on localhost, you will see traces in X-Ray if you issue any
requests. If the collector cannot be accessed via localhost (e.g., in docker-compose), you may need to set the endpoint when
starting your Java application using the `OTEL_OTLP_ENDPOINT` environment variable.

# License

This project is licensed under the MIT No Attribution License.
