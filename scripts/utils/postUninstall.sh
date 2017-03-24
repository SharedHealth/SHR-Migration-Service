#!/bin/sh

rm -f /etc/init.d/shr-migration-service
rm -f /etc/default/shr-migration-service
rm -f /var/run/shr-migration-service

#Remove shr-migration-service from chkconfig
chkconfig --del shr-migration-service || true
