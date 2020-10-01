data "aws_iam_policy_document" "ecs_assume_role" {
  statement {
    actions = [
      "sts:AssumeRole"
    ]

    principals {
      type = "Service"
      identifiers = [
        "ecs-tasks.amazonaws.com"
      ]
    }
  }
}

resource "aws_iam_role" "ecs_task_execution_role" {
  name = "ecsTaskExecutionRole"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

resource "aws_iam_role_policy_attachment" "ecsTaskExecutionRole_policy" {
  role = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_ecs_cluster" "otel_playground" {
  name = "opentelemetry-playground"
  capacity_providers = [
    "FARGATE",
    "FARGATE_SPOT"
  ]
  setting {
    name = "containerInsights"
    value = "enabled"
  }

  depends_on = [
    aws_iam_role_policy_attachment.ecsTaskExecutionRole_policy
  ]
}

resource "aws_iam_role" "ecs_backend" {
  name = "ECSTaskBackend"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

resource "aws_iam_role_policy_attachment" "ecs_backend_policy_xray" {
  role = aws_iam_role.ecs_backend.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

resource "aws_ecs_task_definition" "otel_backend" {
  family = "otel_backend"
  container_definitions = <<TASK_DEFINITION
  [
    {
        "cpu": 200,
        "image": "ghcr.io/anuraaga/otel-playground-backend",
        "memory": 384,
        "name": "backend",
        "portMappings": [
            {
                "containerPort": 8081
            },
            {
                "containerPort": 8083
            }
        ],
        "logConfiguration": {
            "logDriver": "awslogs",
            "options": {
                "awslogs-group": "${aws_cloudwatch_log_group.otel-playground.name}",
                "awslogs-region": "${data.aws_region.current.name}",
                "awslogs-stream-prefix": "backend"
            }
        }
    },
    {
        "cpu": 55,
        "image": "otel/opentelemetry-collector-contrib-dev",
        "memory": 128,
        "name": "otel-collector",
        "command": ["--config", "/otel/config.yml"],
        "dependsOn": [
            {
                "containerName": "otel-collector-init",
                "condition": "COMPLETE"
            }
         ],
        "portMappings": [
            {
                "containerPort": 55680
            }
        ],
        "logConfiguration": {
            "logDriver": "awslogs",
            "options": {
                "awslogs-group": "${aws_cloudwatch_log_group.otel-playground.name}",
                "awslogs-region": "${data.aws_region.current.name}",
                "awslogs-stream-prefix": "backend"
            }
        },
        "mountPoints": [
            {
                "sourceVolume": "otel-config",
                "containerPath": "/otel",
                "readOnly": true
            }
        ]
    },
    {
        "cpu": 1,
        "image": "busybox",
        "memory": 128,
        "name": "otel-collector-init",
        "essential": false,
        "entrypoint": ["/bin/sh"],
        "command": ["-c", "echo ${base64encode(file("../../otel/collector-config.yml"))} | base64 -d - > /otel/config.yml"],
        "logConfiguration": {
            "logDriver": "awslogs",
            "options": {
                "awslogs-group": "${aws_cloudwatch_log_group.otel-playground.name}",
                "awslogs-region": "${data.aws_region.current.name}",
                "awslogs-stream-prefix": "backend"
            }
        },
        "mountPoints": [
            {
                "sourceVolume": "otel-config",
                "containerPath": "/otel"
            }
        ]
    }
  ]
  TASK_DEFINITION
  volume {
    name = "otel-config"
  }
  requires_compatibilities = [
    "FARGATE"
  ]
  cpu = "256"
  memory = "512"
  network_mode = "awsvpc"
  task_role_arn = aws_iam_role.ecs_backend.arn
  execution_role_arn = aws_iam_role.ecs_task_execution_role.arn
}

resource "aws_lb" "ecs_backend" {
  name = "ecs-backend"
  security_groups = [
    aws_security_group.webservers.id
  ]
  subnets = [
    aws_default_subnet.a.id,
    aws_default_subnet.b.id
  ]
}

resource "aws_lb_target_group" "ecs_backend_spark" {
  name = "ecs-backend-spark"
  port = 8083
  protocol = "HTTP"
  target_type = "ip"
  vpc_id = aws_default_vpc.default.id
  health_check {
    path = "/hellospark"
  }
  depends_on = [
    aws_lb.ecs_backend
  ]
}

resource "aws_lb_listener" "ecs_backend_spark" {
  load_balancer_arn = aws_lb.ecs_backend.arn
  port = 80
  protocol = "HTTP"

  default_action {
    type = "forward"
    target_group_arn = aws_lb_target_group.ecs_backend_spark.arn
  }
}

resource "aws_ecs_service" "otel_backend" {
  name = "backend"
  cluster = aws_ecs_cluster.otel_playground.arn
  task_definition = aws_ecs_task_definition.otel_backend.arn
  desired_count = 1
  launch_type = "FARGATE"
  health_check_grace_period_seconds = 3600
  load_balancer {
    target_group_arn = aws_lb_target_group.ecs_backend_spark.arn
    container_name = "backend"
    container_port = 8083
  }
  network_configuration {
    subnets = [
      aws_default_subnet.a.id,
      aws_default_subnet.b.id
    ]
    security_groups = [
      aws_security_group.webbackends.id
    ]
    assign_public_ip = true
  }
}

output "ecs_url" {
  value = aws_lb.ecs_backend.dns_name
}
