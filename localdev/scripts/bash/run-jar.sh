#!/usr/bin/env bash

#
# Copied from https://github.com/BlueWizardHat/skeleton-docker-webapp
# Copyright belong to the original author
#

JAVA_RUN_DIR="/var/run/java"
SCRIPT_DIR="/var/run/bash"

mkdir -p "${SCRIPT_DIR}"

jar_file="$1"
shift

#echo "JAVA_DEBUG=${JAVA_DEBUG}"
if [ ! -z "${JAVA_DEBUG}" ]; then
    DEBUGGING_FLAGS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=4000 "
fi

function wait_for_tcp() {
    echo "Waiting for tcp: '$1'"
    if [ ! -f "${SCRIPT_DIR}/wait-for-it.sh" ]; then
        cp /setup/wait-for-it.sh "${SCRIPT_DIR}/wait-for-it.sh"
        chmod +x "${SCRIPT_DIR}/wait-for-it.sh"
    fi
    "${SCRIPT_DIR}/wait-for-it.sh" -t 180 "$1"
}

function wait_seconds() {
    echo "Sleeping for additional $1 second(s)"
    sleep "$1"
}

while [[ $# > 0 ]]; do
    wait_for="$1"
    shift

    if [[ "${wait_for}" =~ ^[0-9]+$ ]]; then
        wait_seconds "${wait_for}"
    else
        wait_for_tcp "${wait_for}"
    fi

done


if [ ! -d "${JAVA_RUN_DIR}" ]; then
    mkdir -p "${JAVA_RUN_DIR}"
fi

cd /code
cp "/code/${jar_file}" "${JAVA_RUN_DIR}"
COMMAND="java ${DEBUGGING_FLAGS}-Duser.timezone=UTC -jar ${JAVA_RUN_DIR}/${jar_file}"

echo ""
java -version
echo ""
echo "Running jar file ${jar_file}:"
echo ""
echo ">   ${COMMAND}"
echo ""
${COMMAND}
