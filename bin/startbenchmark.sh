#!/bin/bash

# check and set the PADRES_HOME environment value
if [ -z "$PADRES_HOME" ]
then
    #PADRES_HOME="$(cd $(dirname "$0")/.. && pwd)"
    PADRES_HOME=$(dirname $(dirname $(readlink -f $0)/))
    export PADRES_HOME
fi

# if the JAVA_HOME is not set, set to the home directory of the jvm
if [ -z $JAVA_HOME ]
then
    # jvm is in $JAVA_HOME/jre/bin/; therefore three 'dirname'
    JAVA_HOME=$(dirname $(dirname $(dirname $(readlink -f '/usr/bin/java'))))
fi

# get the command line arguments
ARGS=$*

# path separater for java class path values
PATHSEP=':'

# default values
MEMORY_MAX=6
CLIENT="benchmark.Benchmark"
CLIENT_PATH=""
CLIENT_ID=""
CLIENT_IP=""
BROKER_URI=""

CLASSPATH="${PADRES_HOME}/build/${PATHSEP}${CLIENT_PATH}"
for LIB in `ls ${PADRES_HOME}/lib/*.jar`
do
	CLASSPATH="${CLASSPATH}${PATHSEP}${LIB}"
done

JVM_ARGS="-J-Xmx${MEMORY_MAX}g \
         -cp target/padres-broker-jar-with-dependencies.jar\
         -Djava.security.policy=${PADRES_HOME}/etc/java.policy"

if [ ! -z $CLIENT_IP ]; then
    JVM_ARGS="$JVM_ARGS -Djava.rmi.server.hostname=$CLIENT_IP"
fi

# start the client
CMD="scala $JVM_ARGS $CLIENT ${ARGS[@]}"
$CMD
