#!/bin/bash

docker run --rm --name stresstest-jdoe opensbpm/stresstest \
  --opensbpm.username=jdoe --opensbpm.password=jdoe &
docker run --rm --name stresstest-miriam opensbpm/stresstest \
  --opensbpm.username=miriam --opensbpm.password=miriam &

docker run --rm --name stresstest-alice opensbpm/stresstest \
  --opensbpm.username=alice --opensbpm.password=alice \
  --opensbpm.starter=true \
  --opensbpm.statistics.nodes=1 \
  --opensbpm.statistics.pods=1 \
  --opensbpm.statistics.processes=1


# Define the starting value and step size
steps=12

for ((i=1; i<=steps; i++)); do
  # Calculate the processcount and repetitions
  process_count=$((2 ** (i)))
  #repetitions=$((2 ** (i - 1)))
  echo "Running stresstest command with $process_count processes"

  docker run --rm --name stresstest-alice opensbpm/stresstest \
    --opensbpm.username=alice --opensbpm.password=alice \
    --opensbpm.starter=true \
    --opensbpm.statistics.nodes=1 \
    --opensbpm.statistics.pods=1 \
    --opensbpm.statistics.processes=$process_count

done
