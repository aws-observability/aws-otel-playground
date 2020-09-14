resource "aws_default_vpc" "default" {
}

resource "aws_default_subnet" "a" {
  availability_zone = "us-east-1a"
}

resource "aws_default_subnet" "b" {
  availability_zone = "us-east-1b"
}

resource "aws_default_subnet" "c" {
  availability_zone = "us-east-1c"
}

resource "aws_default_subnet" "d" {
  availability_zone = "us-east-1d"
}

resource "aws_default_subnet" "e" {
  availability_zone = "us-east-1e"
}

resource "aws_default_subnet" "f" {
  availability_zone = "us-east-1f"
}

resource "aws_security_group" "webservers" {
  name = "webservers"
  vpc_id = aws_default_vpc.default.id

  ingress {
    description = "plain text"
    from_port = 80
    protocol = "tcp"
    to_port = 80
    cidr_blocks = [
      "0.0.0.0/0"
    ]
  }

  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = [
      "0.0.0.0/0"
    ]
  }
}

resource "aws_security_group" "webbackends" {
  name = "webbackends"
  vpc_id = aws_default_vpc.default.id

  ingress {
    from_port = 0
    protocol = -1
    to_port = 0
    security_groups = [
      aws_security_group.webservers.id
    ]
  }

  egress {
    from_port = 0
    to_port = 0
    protocol = "-1"
    cidr_blocks = [
      "0.0.0.0/0"
    ]
  }
}
