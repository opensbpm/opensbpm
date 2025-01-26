#!/bin/bash

# Show help and end programm
show_help() { echo "Usage: $0 --config <name>"; exit 1; }

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --config) CONFIG="$2"; shift 2 ;;
        *) show_help ;;
    esac
done

# Validate parameters
[[ "$CONFIG" =~ ^[a-zA-Z0-9]+$ ]] || { echo "Error: --config must be alphanumeric."; show_help; }


docker run --rm --detach --name userbot-jdoe opensbpm/userbot \
  --opensbpm.username=jdoe --opensbpm.password=jdoe
docker run --rm --detach --name userbot-miriam opensbpm/userbot \
  --opensbpm.username=miriam --opensbpm.password=miriam

# run Warmup
docker run --rm --name userbot-alice opensbpm/userbot \
  --opensbpm.username=alice --opensbpm.password=alice \
  --opensbpm.starter=true \
  --opensbpm.statistics.config=$CONFIG \
  --opensbpm.statistics.processes=1


# Define step count
steps=12

for ((i=1; i<=steps; i++)); do
    # Calculate the processcount
    process_count=$((2 ** i))

    repetitions=1
    if ((process_count < 64)); then
      # If there are less than 60 processes startet, run the the test more often
      repetitions=8
    fi

    for ((j=1; j<=repetitions; j++)); do
        echo "Running userbot with $process_count processes (repetition $j)"
        docker run --rm --name userbot-alice opensbpm/userbot \
          --opensbpm.username=alice --opensbpm.password=alice \
          --opensbpm.starter=true \
          --opensbpm.statistics.config=$CONFIG \
          --opensbpm.statistics.processes=$process_count
    done
done

docker stop userbot-miriam userbot-jdoe
