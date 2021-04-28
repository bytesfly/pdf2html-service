#!/bin/sh

DIR=$(cd "$(dirname "$0")" && pwd)

APP="pdf2html-service"
APP_JAR="${DIR}/${APP}-*.jar"

if [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA=$(which java)
fi

if [ ! -x "$JAVA" ]; then
    echo "Please Install Java Or Set JAVA_HOME"
    exit 1
fi

HEAP_OPTS="-Xmx3072m -Xms3072m -Xmn2048m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=128m"
HEAP_DUMP_OPTS="-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${DIR}/${APP}.hprof"
JMX_OPTS="-Dcom.sun.management.jmxremote.port=10007 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false"
FILE_ENCODING="-Dfile.encoding=UTF-8"
LOGGING_CONFIG="-Dlogging.config=${DIR}/logback-spring.xml"
USER_TIMEZONE="-Duser.timezone=GMT+08"

JAVA_OPTS="${HEAP_OPTS} ${HEAP_DUMP_OPTS} ${FILE_ENCODING} ${USER_TIMEZONE} ${LOGGING_CONFIG} ${APP_JAR}"

if [ ! -d "${DIR}/logs" ]; then
  mkdir -p ${DIR}/logs
fi

if [ ! -f "${DIR}/logs/start.out" ]; then
  touch "${DIR}/logs/start.out"
fi

# ----- start -----
echo "${APP} is starting..."

echo "${JAVA_OPTS}" > ${DIR}/logs/start.out 2>&1 &

nohup ${JAVA} -jar ${JAVA_OPTS} >> /dev/null 2>&1 &

# write pid file
echo $! > ${DIR}/${APP}.pid

sleep 1

jps | grep "${APP}"
cat ${DIR}/${APP}.pid