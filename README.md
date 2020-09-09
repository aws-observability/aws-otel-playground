# X-Ray Instrumentation Playground

This repository contains a toy application which exhibits a simple yet somewhat representative trace,
including AWS SDK calls, a frontend and backend, and HTTP and gRPC.

The same application is instrumented in several ways, allowing us to compare the experience when viewing
traces for the different types of instrumentation.

Current instrumentation includes
- OpenTelemetry Auto Instrumentation + OpenTelemetry Collector
- X-Ray SDK Instrumentation + X-Ray Daemon
  - Does not instrument many libraries like gRPC and Lettuce

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
