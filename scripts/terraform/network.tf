resource "aws_default_vpc" "default" {
}

resource "aws_default_subnet" "a" {
  availability_zone = "us-west-2a"
}

resource "aws_default_subnet" "b" {
  availability_zone = "us-west-2b"
}

resource "aws_default_subnet" "c" {
  availability_zone = "us-west-2c"
}

resource "aws_default_subnet" "d" {
  availability_zone = "us-west-2d"
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
