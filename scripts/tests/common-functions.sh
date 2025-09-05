#!/bin/bash

set -e
set -u
set -o pipefail

export PROJECT_ROOT_FOLDER=$(realpath ${SCRIPT_CURRENT_DIR}/../../)

export RED='\033[0;31m'
export NC='\033[0m' # No Color

export ONE_GB=$((1024 * 1024))

export MINIMUM_REQUIRED_DISKSPACE_HUMAN_READABLE=1
export MINIMUM_REQUIRED_DISKSPACE=$((${MINIMUM_REQUIRED_DISKSPACE_HUMAN_READABLE} * ${ONE_GB}))
export AVAILABLE_DISKSPACE=$(df --output=avail $(pwd) | tail -n 1 | awk '{print $1}')   ### Should work on both Linux and MacOS
export AVAILABLE_DISKSPACE_HUMAN_READABLE=$((${AVAILABLE_DISKSPACE} / ${ONE_GB}))
echo "Available disk space on '$(pwd)': ${AVAILABLE_DISKSPACE_HUMAN_READABLE}GB"
echo ""

function getOSArch() {
    OSARCH="linux"
    case "$(uname)" in
      CYGWIN* )
        OSARCH="windows"
        ;;

      Darwin* )
        OSARCH="macos"
        ;;

      MINGW* )
        OSARCH="windows"
        ;;
    esac

    echo ${OSARCH}
}

function testFailedDueToOtherReasonsNotification() {
  results=$1

  echo "Test FAILED"
  echo "  App failed due to other reasons than disk space requirements." 1>&2

  echo "App execution logs:"  1>&2
  echo ${results}             1>&2

  exit -1
}

function testFailedDueToExitCodeNotification() {
  exitCodeType=$1
  results=$2

  echo "Test FAILED"
  echo "Test should NOT have failed with a ${exitCodeType} exit code" 1>&2

  echo "Please check if the app is run under the expected conditions" 1>&2

  echo "App execution logs:"  1>&2
  echo ${results}             1>&2
  exit -1
}