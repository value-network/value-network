#!/bin/bash
PID=$(ps -ef|grep valume.jar| grep -v grep|awk '{printf $2}')

if [ "$PID" != "" ]; then
    kill -9 ${PID}
fi

echo "Start valume.jar..."
#nohup java -cp valume.jar:conf val.ValNetwork  > nohup.out  2>&1 &
java -cp valume.jar:conf val.ValNetwork
