resource "aws_lambda_layer_version" "aws_opentelemetry_javaagent" {
  layer_name = "aws-opentelemetry-javaagent"
  filename = "../../lambda-api/build/distributions/aws-opentelemetry-agent-layer.zip"
  compatible_runtimes = ["java8", "java8.al2", "java11"]
  license_info = "Apache-2.0"
  source_code_hash = filebase64sha256("../../lambda-api/build/distributions/aws-opentelemetry-agent-layer.zip")
}
