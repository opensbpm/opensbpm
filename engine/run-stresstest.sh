
#docker run --rm --name stresstest-jdoe opensbpm/stresstest-worker:latest --opensbpm.username=jdoe --opensbpm.password=jdoe &
#docker run --rm --name stresstest-miriam opensbpm/stresstest-worker:latest --opensbpm.username=miriam --opensbpm.password=miriam &

mvn -pl engine/stresstestworker spring-boot:run -Dspring-boot.run.arguments="--opensbpm.username=jdoe --opensbpm.password=jdoe" &
mvn -pl engine/stresstestworker spring-boot:run -Dspring-boot.run.arguments="--opensbpm.username=miriam --opensbpm.password=miriam" &
