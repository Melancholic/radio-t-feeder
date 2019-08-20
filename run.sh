#!/bin/bash

source ./ENV_INIT.sh

/usr/bin/java -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Xmx128m -Xss512k -XX:MetaspaceSize=100m -jar ./*radiot2telegram-*.jar