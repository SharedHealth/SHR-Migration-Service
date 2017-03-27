#!/bin/sh
nohup java -jar /opt/shr-migration-service/lib/shr-migration-service.jar >  /dev/null 2>&1 &
echo $! > /var/run/shr-migration-service/shr-migration-service.pid