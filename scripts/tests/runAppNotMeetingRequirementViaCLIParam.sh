#!/bin/bash

set -e
set -u
set -o pipefail

SCRIPT_CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source ${SCRIPT_CURRENT_DIR}/common-functions.sh

mkdir -p localstore

OSARCH=$(getOSArch)

echo "Detected OS: ${OSARCH}"

MINIMUM_REQUIRED_DISKSPACE=100
exitCodeFile=$(mktemp)
results=$( (java -jar ${PROJECT_ROOT_FOLDER}/build/libs/record-and-upload-${OSARCH}-*all.jar --minimum-required-diskspace-gb ${MINIMUM_REQUIRED_DISKSPACE} --config ${PROJECT_ROOT_FOLDER}/.private/aws-test-secrets --store ${PROJECT_ROOT_FOLDER}/build/play && true); echo $? > "${exitCodeFile}" )

exitCode=$(cat ${exitCodeFile})
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" 1>&2
echo " Running test on record and upload app to verify the minimum disk space requirement check (by passing --minimum-required-diskspace-gb ${MINIMUM_REQUIRED_DISKSPACE})"  1>&2
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" 1>&2

if [[ ${exitCode} -ne 0 ]]; then
  if [[ $(echo ${results} | grep "Sorry, you need at least ${MINIMUM_REQUIRED_DISKSPACE}GB of free disk space on this volume (or drive)" ) ]]; then
  	echo "Test PASSED"          1>&2
  else
    testFailedDueToOtherReasonsNotification ${results}
  fi
else
  testFailedDueToExitCodeNotification "0" ${results}
fi
