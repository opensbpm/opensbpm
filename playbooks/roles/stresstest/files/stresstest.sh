#!/bin/bash

# show help and end programm
show_help() { echo "Usage: $0 --nodes <count> --pods <count>"; exit 1; }

# Argumente parsen
while [[ $# -gt 0 ]]; do
    case "$1" in
        --nodes) NODES="$2"; shift 2 ;;
        --pods)  PODS="$2"; shift 2 ;;
        *) show_help ;;
    esac
done

# validate parameters
[[ "$NODES" =~ ^[0-9]+$ && "$PODS" =~ ^[0-9]+$ ]] || { echo "Error: Both --nodes and --pods must be integers."; show_help; }


docker run --rm --detach --name userbot-jdoe opensbpm/userbot \
  --opensbpm.username=jdoe --opensbpm.password=jdoe
docker run --rm --detach --name userbot-miriam opensbpm/userbot \
  --opensbpm.username=miriam --opensbpm.password=miriam

docker run --rm --name userbot-alice opensbpm/userbot \
  --opensbpm.username=alice --opensbpm.password=alice \
  --opensbpm.starter=true \
  --opensbpm.statistics.nodes=$NODES \
  --opensbpm.statistics.pods=$PODS \
  --opensbpm.statistics.processes=1


# Define the starting value and step size
steps=12

for ((i=1; i<=steps; i++)); do
    # Calculate the processcount and repetitions
    process_count=$((2 ** i))

    repetitions=1
    if ((process_count <64 )); then
      repetitions=8
    fi
    for ((j=1; j<=repetitions; j++)); do
        echo "Running userbot-alice with $process_count processes (repetition $j)"
        docker run --rm --name userbot-alice opensbpm/userbot \
          --opensbpm.username=alice --opensbpm.password=alice \
          --opensbpm.starter=true \
          --opensbpm.statistics.nodes=$NODES \
          --opensbpm.statistics.pods=$PODS \
          --opensbpm.statistics.processes=$process_count
    done
done

docker stop userbot-miriam userbot-jdoe
