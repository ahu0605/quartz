#!/bin/sh
pro=`ps -ef|grep java|grep quartz|awk '{print $2}'`
echo $pro is killed
kill -9 $pro
