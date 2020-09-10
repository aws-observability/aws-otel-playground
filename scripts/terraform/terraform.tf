provider "aws" {
  version = "~> 3.0"
  region = "us-west-2"
}

terraform {
  backend "s3" {
    bucket = "opentelemetry-playground-terraform-state"
    key    = "terraform.state"
    region = "us-west-2"
    encrypt = true
    skip_metadata_api_check = true
  }
}

resource "aws_s3_bucket" "terraform_state" {
  bucket = "opentelemetry-playground-terraform-state"

  versioning {
    enabled = true
  }
}

resource "aws_dynamodb_table" "terraform_state_locks" {
  name = "opentelemetry-playground-terraform-state-locks"
  hash_key = "LockID"
  billing_mode = "PAY_PER_REQUEST"

  attribute {
    name = "LockID"
    type = "S"
  }
}
