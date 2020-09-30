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

resource "aws_iam_role" "eks_node_role" {
  name = "eksNodeRole"

  assume_role_policy = jsonencode({
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
    Version = "2012-10-17"
  })
}

resource "aws_iam_role_policy_attachment" "eksNodeRole-AmazonEKSWorkerNodePolicy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy"
  role       = aws_iam_role.eks_node_role.name
}

resource "aws_iam_role_policy_attachment" "eksNodeRole-AmazonEKS_CNI_Policy" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
  role       = aws_iam_role.eks_node_role.name
}

resource "aws_iam_role_policy_attachment" "eksNodeRole-AmazonEC2ContainerRegistryReadOnly" {
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
  role       = aws_iam_role.eks_node_role.name
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

resource "aws_eks_node_group" "otel_playground" {
  cluster_name = aws_eks_cluster.otel_playground.name
  node_group_name = "default"
  node_role_arn = aws_iam_role.eks_node_role.arn
  subnet_ids = module.vpc.public_subnets

  instance_types = [
    "t3.small"
  ]

  scaling_config {
    desired_size = 1
    max_size = 1
    min_size = 1
  }

  depends_on = [
    aws_iam_role_policy_attachment.eksNodeRole-AmazonEC2ContainerRegistryReadOnly,
    aws_iam_role_policy_attachment.eksNodeRole-AmazonEKS_CNI_Policy,
    aws_iam_role_policy_attachment.eksNodeRole-AmazonEKSWorkerNodePolicy
  ]
}

data "tls_certificate" "eks_otel_playground_oidc" {
  url = aws_eks_cluster.otel_playground.identity[0].oidc[0].issuer
}

resource "aws_iam_openid_connect_provider" "eks_otel_playground" {
  client_id_list = [
    "sts.amazonaws.com"
  ]
  thumbprint_list = [
    data.tls_certificate.eks_otel_playground_oidc.certificates[0].sha1_fingerprint
  ]
  url = aws_eks_cluster.otel_playground.identity[0].oidc[0].issuer
}

data "aws_iam_policy_document" "eks_pod_policy" {
  statement {
    actions = [
      "sts:AssumeRoleWithWebIdentity"
    ]
    effect = "Allow"

    condition {
      test = "StringLike"
      variable = "${replace(aws_iam_openid_connect_provider.eks_otel_playground.url, "https://", "")}:sub"
      values = [
        "system:serviceaccount:fargate:*"
      ]
    }

    principals {
      identifiers = [
        aws_iam_openid_connect_provider.eks_otel_playground.arn
      ]
      type = "Federated"
    }
  }
}

resource "aws_iam_role" "eks_pod_monitoring_role" {
  assume_role_policy = data.aws_iam_policy_document.eks_pod_policy.json
  name = "eks-opentelemetry-playground"
}

resource "aws_iam_role_policy_attachment" "eks_opentelemetry_playground_xray" {
  role = aws_iam_role.eks_pod_monitoring_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess"
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

resource "kubernetes_default_service_account" "fargate" {
  metadata {
    namespace = "fargate"
    annotations = {
      "eks.amazonaws.com/role-arn" = aws_iam_role.eks_pod_monitoring_role.arn
    }
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
    namespace = kubernetes_namespace.fargate.metadata[0].name
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
          image_pull_policy = "Always"

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
          image_pull_policy = "Always"
          args = [
            "--config",
            "/otel/otel_config.yml",
            "--log-level",
            "debug"
          ]

          env {
            name = "AWS_REGION"
            value = "us-east-1"
          }

          env {
            name = "AWS_STS_REGIONAL_ENDPOINTS"
            value = "regional"
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

resource "kubernetes_service" "fargate_backend" {
  metadata {
    name = "backend"
    namespace = kubernetes_namespace.fargate.metadata[0].name
  }

  spec {
    selector = {
      app = kubernetes_deployment.fargate_backend.metadata[0].labels.app
    }

    type = "NodePort"

    port {
      name = "spark"
      port = 8083
    }
  }
}

module "alb_ingress_controller" {
  source  = "iplabs/alb-ingress-controller/kubernetes"
  version = "3.4.0"

  k8s_cluster_type = "eks"
  k8s_namespace    = "kube-system"

  aws_region_name  = data.aws_region.current.name
  k8s_cluster_name = data.aws_eks_cluster.otel_playground.name
}

resource "kubernetes_ingress" "fargate_ingress" {
  metadata {
    name = "backend"
    namespace = kubernetes_namespace.fargate.metadata[0].name

    annotations = {
      "kubernetes.io/ingress.class": "alb"
      "alb.ingress.kubernetes.io/target-type": "ip"
      "alb.ingress.kubernetes.io/scheme": "internet-facing"
      "alb.ingress.kubernetes.io/healthcheck-path": "/hellospark"
    }
  }

  spec {
    rule {
      http {
        path {
          path = "/hellospark"

          backend {
            service_name = kubernetes_service.fargate_backend.metadata[0].name
            service_port = kubernetes_service.fargate_backend.spec[0].port[0].port
          }
        }
      }
    }
  }

  depends_on = [
    module.alb_ingress_controller
  ]
}
