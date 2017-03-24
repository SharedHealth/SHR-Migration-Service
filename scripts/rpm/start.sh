#!/bin/sh
nohup java -jar /opt/mci-background-jobs/lib/mci-background-jobs.jar >  /dev/null 2>&1 &
echo $! > /var/run/mci-background-jobs/mci-background-jobs.pid