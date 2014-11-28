#!/bin/sh
echo "INSTALLING Prerequisites"

sudo apt-get install python-software-properties
sudo apt-add-repository ppa:chris-lea/node.js
sudo apt-get update

sudo apt-get install nodejs
sudo apt-get install npm
sudo npm install -g express
sudo npm install -g kafka-node
sudo npm install -g kafka
sudo npm install -g socket.io
