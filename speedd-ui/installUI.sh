#!/bin/sh

export SCRIPTS_HOME=/home/$USER/speedd/speedd-ui/bin
# makes scripts executable
chmod +x "$SCRIPTS_HOME"/*.sh
echo "$SCRIPTS_HOME"

cd "$SCRIPTS_HOME"
"$SCRIPTS_HOME"/prereq.sh $*
"$SCRIPTS_HOME"/run.sh $*

