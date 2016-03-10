#!/usr/bin/env bash
# NOTE: This script requires `spark-submit` command declared in the PATH

# Get the location for this script; handles symlinks
function get_script_path {
  local source="${BASH_SOURCE[0]}"
  while [ -h "$source" ] ; do
    local linked="$(readlink "$source")"
    local dir="$(cd -P $(dirname "$source") && cd -P $(dirname "$linked") && pwd)"
    source="$dir/$(basename "$linked")"
  done
  echo ${source}
}

# script details
declare -r script_path=$(get_script_path)
declare -r script_name=$(basename "$script_path")
declare -r script_dir="$(cd -P "$(dirname "$script_path")" && pwd)"

# directories
declare -r base_dir="$(cd "$script_dir/.." && pwd)"
declare -r lib_dir="$base_dir/lib"

# Execute using spark-submit
spark-submit \
    --properties-file $base_dir/etc/spark-defaults.conf \
    --class org.speedd.ml.app.WeightLearnerCLI \
    $lib_dir/NCSR_SPEEDD-ML.jar $@
