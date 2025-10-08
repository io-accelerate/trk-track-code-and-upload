#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <terraform-command> [args...]" >&2
  exit 1
fi

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

# Use an environment-specific data dir to keep local state separate
export TF_DATA_DIR="$DIR/.terraform-dev"
mkdir -p "$TF_DATA_DIR"

GRADLE_VERSION=$(grep '^version=' ../../gradle.properties | cut -d'=' -f2)
export TF_VAR_lambda_version="$GRADLE_VERSION"
export TF_VAR_environment="dev"

export TF_CLI_ARGS_plan="-var-file=vars/dev.tfvars"
export TF_CLI_ARGS_apply="-var-file=vars/dev.tfvars"
export TF_CLI_ARGS_destroy="-var-file=vars/dev.tfvars"
export TF_CLI_ARGS_import="-var-file=vars/dev.tfvars"

if [[ "$1" == "apply" ]]; then
  for arg in "${@:2}"; do
    if [[ "$arg" == *.tfplan ]]; then
      export TF_CLI_ARGS_apply=""
      break
    fi
  done
fi

if [[ "$1" == "init" ]]; then
  terraform init \
    -backend-config="bucket=tdl-artifacts" \
    -backend-config="key=trk-track-code-and-upload/dev/terraform.tfstate" \
    -backend-config="region=eu-west-2" \
    -backend-config="encrypt=true" \
    "${@:2}"
else
  terraform "$@"
fi
