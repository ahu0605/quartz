#!/bin/sh
pwd=$(pwd)
echo $pwd
java -jar -Dres=$pwd/target/config/ -Dlogback.configurationFile=$pwd/target/config/logback.xml target/sks-0.0.1-SNAPSHOT.jar & >start.log
