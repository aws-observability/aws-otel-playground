terraform {
  backend "s3" {
    bucket = "opentelemetry-playground-terraform-state"
    key    = "terraform.state"
    region = "us-west-2"
    encrypt = true
    skip_metadata_api_check = true
  }
}
