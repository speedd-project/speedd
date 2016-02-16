#!/bin/bash
source ./setup-env-mesos
cd ~/kafka

./kafka-mesos.sh stop "*"

./kafka-mesos.sh  update "*" --options log.retention.hours=0

./kafka-mesos.sh start "*"

./kafka-mesos.sh status

./kafka-mesos.sh stop "*"

./kafka-mesos.sh  update "*" --options log.retention.hours=2

./kafka-mesos.sh start "*"

./kafka-mesos.sh status

