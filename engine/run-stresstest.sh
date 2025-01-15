#!/bin/bash

#docker run --rm --name stresstest-jdoe opensbpm/stresstest-worker --opensbpm.username=jdoe --opensbpm.password=jdoe &
#docker run --rm --name stresstest-miriam opensbpm/stresstest-worker --opensbpm.username=miriam --opensbpm.password=miriam &

#mvn -pl engine/stresstestworker spring-boot:run -Dspring-boot.run.arguments="--opensbpm.username=jdoe --opensbpm.password=jdoe" &
#mvn -pl engine/stresstestworker spring-boot:run -Dspring-boot.run.arguments="--opensbpm.username=miriam --opensbpm.password=miriam" &

# Define the starting value and step size
start_value=640
steps=7

# Loop through 10 steps
for ((i=1; i<=steps; i++)); do
  # Calculate the processcount and repetitions
  process_count=$((start_value / (2 ** (i - 1))))
  repetitions=$((2 ** (i - 1)))

  echo "Running stresstest command $repetitions times for processcount $process_count"

  # Run the Docker command the calculated number of times
  for ((j=1; j<=repetitions; j++)); do
    #echo "  Iteration $j for value $value"
    docker run --rm --name stresstest-alice opensbpm/stresstest-worker \
      --opensbpm.username=alice --opensbpm.password=alice \
      --opensbpm.starter=true \
      --opensbpm.statistics.nodes=1 \
      --opensbpm.statistics.pods=1 \
      --opensbpm.statistics.processes=$process_count
  done
done
