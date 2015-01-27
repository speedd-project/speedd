#!/bin/bash
source ../setup-env

service storm-supervisor stop
service storm-nimbus stop
service kafka-server stop
service zookeeper-server stop

rm -rf /var/lib/zookeeper/version-2
rm -rf /tmp/kafka-logs
rm -rf /storm-local/*
service zookeeper-server init

service zookeeper-server start
service kafka-server start
service storm-nimbus start
service storm-supervisor start

./create-topics
