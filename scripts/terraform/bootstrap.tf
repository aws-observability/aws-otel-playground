provider "aws" {
  version = "~> 3.0"
  region = "us-east-1"
}

provider "tls" {
  version = "~> 2.2.0"
}

data "aws_region" "current" {}
