#!/bin/bash

# Show help and end programm
show_help() { echo "Usage: $0 --interval <int> --processes <int> --config <name>"; exit 1; }

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --interval) INTERVAL="$2"; shift 2 ;;
        --processes) PROCESSES="$2"; shift 2 ;;
        --config) CONFIG="$2"; shift 2 ;;
        *) show_help ;;
    esac
done

# Validate parameters
[[ "$INTERVAL" =~ ^[0-9]+$ && "$PROCESSES" =~ ^[0-9]+$ ]] || { echo "Error: Both --interval and --processes must be integers."; show_help; }
[[ "$CONFIG" =~ ^[a-zA-Z0-9]+$ ]] || { echo "Error: --config must be alphanumeric."; show_help; }


# Run userbot of users
docker run --rm --detach --name userbot-jdoe opensbpm/userbot \
  --opensbpm.username=jdoe --opensbpm.password=jdoe
docker run --rm --detach --name userbot-miriam opensbpm/userbot \
  --opensbpm.username=miriam --opensbpm.password=miriam

# Run userbot of main user
docker run --rm --name userbot-alice opensbpm/userbot \
  --opensbpm.username=alice --opensbpm.password=alice \
  --opensbpm.starter=true \
  --opensbpm.statistics.interval=$INTERVAL \
  --opensbpm.statistics.processes=$PROCESSES \
  --opensbpm.statistics.config=$CONFIG

docker stop userbot-miriam userbot-jdoe
