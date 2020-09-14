data "aws_iam_policy_document" "lambda_assume_role" {
  statement {
    actions = [
      "sts:AssumeRole"
    ]

    principals {
      type = "Service"
      identifiers = [
        "lambda.amazonaws.com"
      ]
    }
  }
}

resource "aws_iam_role" "lambda_execution_role" {
  name = "lambdaExecutionRole"
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role.json
}

resource "aws_iam_role_policy_attachment" "lambdaExecutionRole_lambdaPolicy" {
  role = aws_iam_role.lambda_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "lambdaExecutionRole_xray" {
  role = aws_iam_role.lambda_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

resource "aws_lambda_function" "lambda_api" {
  function_name = "hello-lambda-api"
  handler = "io.awsobservability.opentelemetry.playground.lambdaapi.HelloLambdaHandler"
  role = aws_iam_role.lambda_execution_role.arn
  runtime = "java11"
  filename = "../../lambda-api/build/libs/lambda-api-all.jar"
  source_code_hash = filebase64sha256("../../lambda-api/build/libs/lambda-api-all.jar")
  tracing_config {
    mode = "Active"
  }
}

resource "aws_api_gateway_rest_api" "lambda_api" {
  name = "hello-lambda-api"
}

resource "aws_api_gateway_resource" "lambda_api_proxy" {
  rest_api_id = aws_api_gateway_rest_api.lambda_api.id
  parent_id = aws_api_gateway_rest_api.lambda_api.root_resource_id
  path_part = "{proxy+}"
}

resource "aws_api_gateway_method" "lambda_api_proxy" {
  rest_api_id = aws_api_gateway_rest_api.lambda_api.id
  resource_id = aws_api_gateway_resource.lambda_api_proxy.id
  http_method = "ANY"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "lambda_api" {
  rest_api_id = aws_api_gateway_rest_api.lambda_api.id
  resource_id = aws_api_gateway_method.lambda_api_proxy.resource_id
  http_method = aws_api_gateway_method.lambda_api_proxy.http_method

  integration_http_method = "POST"
  type = "AWS_PROXY"
  uri = aws_lambda_function.lambda_api.invoke_arn
}

resource "aws_api_gateway_method" "lambda_api_proxy_root" {
  rest_api_id = aws_api_gateway_rest_api.lambda_api.id
  resource_id = aws_api_gateway_rest_api.lambda_api.root_resource_id
  http_method = "ANY"
  authorization = "NONE"
}

resource "aws_api_gateway_integration" "lambda_api_root" {
  rest_api_id = aws_api_gateway_rest_api.lambda_api.id
  resource_id = aws_api_gateway_method.lambda_api_proxy_root.resource_id
  http_method = aws_api_gateway_method.lambda_api_proxy_root.http_method

  integration_http_method = "POST"
  type = "AWS_PROXY"
  uri = aws_lambda_function.lambda_api.invoke_arn
}

resource "aws_api_gateway_deployment" "lambda_api" {
  depends_on = [
    aws_api_gateway_integration.lambda_api,
    aws_api_gateway_integration.lambda_api_root,
  ]

  rest_api_id = aws_api_gateway_rest_api.lambda_api.id
}

resource "aws_api_gateway_stage" "test" {
  stage_name = "default"
  rest_api_id = aws_api_gateway_rest_api.lambda_api.id
  deployment_id = aws_api_gateway_deployment.lambda_api.id
  xray_tracing_enabled = true
}

resource "aws_lambda_permission" "lambda_api_allow_gateway" {
  action = "lambda:InvokeFunction"
  function_name = aws_lambda_function.lambda_api.arn
  principal = "apigateway.amazonaws.com"
  source_arn = "${aws_api_gateway_rest_api.lambda_api.execution_arn}/*/*"
}
