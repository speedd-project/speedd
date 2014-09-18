#!/bin/sh
echo "\n Setting up UI server"
export PROJECT_HOME=/home/$USER/speedd/speedd-ui
cd "$PROJECT_HOME"
node app.js
