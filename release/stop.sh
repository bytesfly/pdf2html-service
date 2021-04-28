#!/bin/sh

DIR=$(cd "$(dirname "$0")" && pwd)

APP="pdf2html-service"

echo "${APP} is stopping..."

PID_FILE="${DIR}/${APP}.pid"

if [ -e "$PID_FILE" ]; then
      kill $(cat $PID_FILE)
      rm -f ${PID_FILE}
else
      echo "${APP}.pid not found !"
fi

sleep 1

jps | grep ${APP}