#!/bin/bash
source ./setup-env

service storm-supervisor stop
service storm-nimbus stop
service kafka-server stop
service zookeeper-server stop

echo Wait for all services to stop...
sleep 5

echo Cleaning data logs...

rm -rf /var/lib/zookeeper/version-2
rm -rf /tmp/kafka-logs
rm -rf /storm-local/*

echo Init zookeeper data log
service zookeeper-server init

service zookeeper-server start
service kafka-server start
service storm-nimbus start
service storm-supervisor start

echo Wait for all services to start...
sleep 5

echo Creating topics...
./create-topics

echo Done.
