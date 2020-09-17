provider "aws" {
  version = "~> 3.0"
  region = "us-east-1"
}

provider "aws" {
  alias  = "west"
  region = "us-west-2"
}

data "aws_region" "current" {}

resource "aws_s3_bucket" "terraform_state" {
  provider = aws.west
  bucket = "opentelemetry-playground-terraform-state"

  versioning {
    enabled = true
  }
}

resource "aws_dynamodb_table" "terraform_state_locks" {
  provider = aws.west
  name = "opentelemetry-playground-terraform-state-locks"
  hash_key = "LockID"
  billing_mode = "PAY_PER_REQUEST"

  attribute {
    name = "LockID"
    type = "S"
  }
}
