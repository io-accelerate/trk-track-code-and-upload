variable "environment" {
  description = "Deployment environment name (e.g. dev or prod)."
  type        = string
  validation {
    condition     = contains(["dev", "prod"], var.environment)
    error_message = "Environment must be either 'dev' or 'prod'."
  }
}

variable "upload_bucket_name" {
  description = "S3 bucket name for storing uploaded files."
  type        = string
  validation {
    condition     = length(trimspace(var.upload_bucket_name)) > 0
    error_message = "upload_bucket_name cannot be empty."
  }
}

locals {
  project_name = "trk-track-code-and-upload"
  aws_region   = "eu-west-2"
  tags = {
    Project     = local.project_name
    Environment = var.environment
  }
}
