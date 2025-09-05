#!/bin/bash

set -e
set -u
set -o pipefail

SCRIPT_CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source ${SCRIPT_CURRENT_DIR}/common-functions.sh

if [[ "${AVAILABLE_DISKSPACE}" -lt "${MINIMUM_REQUIRED_DISKSPACE}" ]]; then
   echo "${RED}Sorry, you need ${MINIMUM_REQUIRED_DISKSPACE_HUMAN_READABLE}GB of free disk space on this drive, in order for this test to work."
   echo ""
   echo "Please make sure the expected environment is setup on '$(pwd)' and try running the test again.${NC}"
   exit -1
fi

mkdir -p localstore

OSARCH=$(getOSArch)

echo "Detected OS: ${OSARCH}"

exitCodeFile=$(mktemp)
results=$( (java -jar ${PROJECT_ROOT_FOLDER}/build/libs/record-and-upload-${OSARCH}-*all.jar --config ${PROJECT_ROOT_FOLDER}/.private/aws-test-secrets --store ${PROJECT_ROOT_FOLDER}/build/play && true); echo $? > "${exitCodeFile}" )

exitCode=$(cat ${exitCodeFile})
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" 1>&2
echo " Running test on record and upload app to verify happy path when diskspace is available and the minimum required diskspace param is not passed)"  1>&2
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" 1>&2

if [[ ${exitCode} -eq 0 ]]; then
  if [[ $(echo ${results} | grep "Start S3 Sync session" ) ]]; then
  	echo "Test PASSED"          1>&2
  else
    testFailedDueToOtherReasonsNotification ${results}
  fi
else
  testFailedDueToExitCodeNotification "non-zero" ${results}
fi
