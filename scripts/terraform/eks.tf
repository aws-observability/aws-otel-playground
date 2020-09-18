data "aws_iam_policy_document" "eks_assume_role" {
  statement {
    actions = [
      "sts:AssumeRole"
    ]

    principals {
      type = "Service"
      identifiers = [
        "eks.amazonaws.com"
      ]
    }
  }
}

data "aws_iam_policy_document" "eks_fargate_assume_role" {
  statement {
    actions = [
      "sts:AssumeRole"
    ]

    principals {
      type = "Service"
      identifiers = [
        "eks-fargate-pods.amazonaws.com"
      ]
    }
  }
}

resource "aws_iam_role" "eks_cluster_role" {
  name = "eksClusterRole"
  assume_role_policy = data.aws_iam_policy_document.eks_assume_role.json
}

resource "aws_iam_role_policy_attachment" "eks_cluster_role-AmazonEKSClusterPolicy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSClusterPolicy"
  role = aws_iam_role.eks_cluster_role.name
}

resource "aws_iam_role" "eks_fargate_role" {
  name = "eksFargateRole"
  assume_role_policy = data.aws_iam_policy_document.eks_fargate_assume_role.json
}

resource "aws_iam_role_policy_attachment" "eks_fargate_role-AmazonEKSFargatePodExecutionRolePolicy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSFargatePodExecutionRolePolicy"
  role = aws_iam_role.eks_fargate_role.name
}

resource "aws_iam_role_policy_attachment" "eks_fargate_role_xray" {
  role = aws_iam_role.eks_fargate_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
}

resource "aws_eks_cluster" "otel_playground" {
  name = "opentelemetry-playground"
  role_arn = aws_iam_role.eks_cluster_role.arn

  vpc_config {
    subnet_ids = concat(module.vpc.public_subnets, module.vpc.private_subnets)
  }

  enabled_cluster_log_types = [
    "api",
    "controllerManager",
    "scheduler"
  ]

  # Ensure that IAM Role permissions are created before and deleted after EKS Cluster handling.
  # Otherwise, EKS will not be able to properly delete EKS managed EC2 infrastructure such as Security Groups.
  depends_on = [
    aws_iam_role_policy_attachment.eks_cluster_role-AmazonEKSClusterPolicy
  ]
}

resource "aws_eks_fargate_profile" "otel_playground_fargate" {
  cluster_name = aws_eks_cluster.otel_playground.name
  fargate_profile_name = "example"
  pod_execution_role_arn = aws_iam_role.eks_fargate_role.arn
  subnet_ids = module.vpc.private_subnets

  selector {
    namespace = "fargate"
  }
}

provider "kubernetes" {
  host = data.aws_eks_cluster.otel_playground.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.otel_playground.certificate_authority[0].data)
  token = data.aws_eks_cluster_auth.otel_playground.token
  load_config_file = false
  version = "~> 1.13"
}

data "aws_eks_cluster" "otel_playground" {
  name = aws_eks_cluster.otel_playground.id
}

data "aws_eks_cluster_auth" "otel_playground" {
  name = aws_eks_cluster.otel_playground.id
}

resource "kubernetes_namespace" "fargate" {
  metadata {
    name = "fargate"
  }
}

resource "kubernetes_config_map" "fargate_otel_config" {
  metadata {
    name = "otel-config"
    namespace = "fargate"
  }

  data = {
    "otel_config.yml" = file("../../otel/collector-config.yml")
  }
}

resource "kubernetes_deployment" "fargate_backend" {
  metadata {
    name = "backend"
    namespace = "fargate"
    labels = {
      app = "backend"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "backend"
      }
    }

    template {
      metadata {
        labels = {
          app = "backend"
        }
      }

      spec {
        container {
          name = "backend"
          image = "ghcr.io/anuraaga/otel-playground-backend"

          readiness_probe {
            http_get {
              path = "/hellospark"
              port = 8083
            }

            initial_delay_seconds = 10
            period_seconds = 5
          }

          resources {
            requests {
              cpu = "0.2"
              memory = "384Mi"
            }
          }
        }
        container {
          name = "otel-collector"
          image = "otel/opentelemetry-collector-contrib-dev"
          args = ["--config", "/otel/otel_config.yml", "--log-level", "debug"]

          env {
            name = "AWS_REGION"
            value = "us-east-1"
          }

          env {
            name = "HTTPS_PROXY"
            value = "https://${data.aws_nat_gateway.vpc.private_ip}/"
          }

          resources {
            requests {
              cpu = "0.05"
              memory = "128Mi"
            }
          }

          volume_mount {
            mount_path = "/otel"
            name = "otel-config"
          }
        }

        volume {
          name = "otel-config"
          config_map {
            name = "otel-config"
          }
        }
      }
    }
  }
  depends_on = [
    kubernetes_config_map.fargate_otel_config
  ]
}
