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


docker run --rm --name stresstest-jdoe opensbpm/stresstest \
  --opensbpm.username=jdoe --opensbpm.password=jdoe &
docker run --rm --name stresstest-miriam opensbpm/stresstest \
  --opensbpm.username=miriam --opensbpm.password=miriam &

docker run --rm --name stresstest-alice opensbpm/stresstest \
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
        echo "Running stresstest command with $process_count (repetition $repetitions)"
        docker run --rm --name stresstest-alice opensbpm/stresstest \
          --opensbpm.username=alice --opensbpm.password=alice \
          --opensbpm.starter=true \
          --opensbpm.statistics.nodes=$NODES \
          --opensbpm.statistics.pods=$PODS \
          --opensbpm.statistics.processes=$process_count
    done
done

docker stop stresstest-miriam stresstest-jdoe
