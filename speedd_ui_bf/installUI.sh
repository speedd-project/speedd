#!/bin/sh

export SCRIPTS_HOME=/home/$USER/speedd/speedd_ui_bf
# makes scripts executable
chmod +x "$SCRIPTS_HOME"/bin/*.sh
echo "$SCRIPTS_HOME"

cd "$SCRIPTS_HOME"
"$SCRIPTS_HOME"/bin/prereq.sh $*
"$SCRIPTS_HOME"/bin/run.sh $*

