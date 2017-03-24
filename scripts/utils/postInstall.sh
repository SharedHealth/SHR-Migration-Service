#!/bin/sh

ln -s /opt/shr-migration-service/bin/shr-migration-service /etc/init.d/shr-migration-service
ln -s /opt/shr-migration-service/etc/shr-migration-service /etc/default/shr-migration-service
ln -s /opt/shr-migration-service/var /var/run/shr-migration-service

if [ ! -e /var/log/shr-migration-service ]; then
    mkdir /var/log/shr-migration-service
fi

# Add shr-migration-service service to chkconfig
chkconfig --add shr-migration-service