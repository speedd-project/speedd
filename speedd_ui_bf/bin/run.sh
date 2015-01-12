#!/bin/sh
echo "\n Setting up UI server"
export PROJECT_HOME=/home/$USER/speedd/speedd_ui_bf
cd "$PROJECT_HOME"
node app.js
