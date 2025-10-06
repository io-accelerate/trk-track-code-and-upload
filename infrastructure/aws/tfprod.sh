#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <terraform-command> [args...]" >&2
  exit 1
fi

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

GRADLE_VERSION=$(grep '^version=' ../../gradle.properties | cut -d'=' -f2)
export TF_VAR_lambda_version="$GRADLE_VERSION"
export TF_VAR_environment="prod"

export TF_CLI_ARGS_plan="-var-file=vars/prod.tfvars"
export TF_CLI_ARGS_apply="-var-file=vars/prod.tfvars"
export TF_CLI_ARGS_destroy="-var-file=vars/prod.tfvars"
export TF_CLI_ARGS_import="-var-file=vars/prod.tfvars"

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
    -backend-config="key=trk-track-code-and-upload/prod/terraform.tfstate" \
    -backend-config="region=eu-west-2" \
    -backend-config="encrypt=true" \
    "${@:2}"
else
  terraform "$@"
fi
