/**
 * ****************************************************************************
 * Copyright (C) 2024 Stefan Sedelmaier
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * ****************************************************************************
 */
package org.opensbpm.engine.e2e;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.LogManager;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.opensbpm.engine.api.ProcessNotFoundException;
import org.opensbpm.engine.api.UserNotFoundException;
import org.opensbpm.engine.api.instance.*;
import org.opensbpm.engine.api.model.ProcessModelInfo;
import org.opensbpm.engine.client.Credentials;
import org.opensbpm.engine.client.EngineServiceClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@SpringBootApplication
public class Main implements CommandLineRunner {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws GeneralSecurityException, IOException {
        SpringApplication.run(Main.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
            execute(Configuration.parseArgs(args));
        } catch (ParseException ex) {
            //ParseException is already dumped to System.out, log here for debugging purpose
            LOGGER.log(Level.FINEST, ex.getMessage(), ex);
            System.exit(1);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
            System.exit(1);
        } catch (ProcessNotFoundException | UserNotFoundException
                 | IOException | SecurityException
                 | ExecutionException | GeneralSecurityException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            System.exit(1);
        }
    }

    public void execute(Configuration configuration) throws UserNotFoundException, ProcessNotFoundException,
            IOException, InterruptedException, ExecutionException, GeneralSecurityException {

        try {
            EngineServiceClient adminClient = configuration.createEngineServiceClient(Credentials.of("admin", "admin".toCharArray()));
            InputStream modelResource = Main.class.getResourceAsStream("/models/" + "dienstreiseantrag_extended.xml");
            ProcessModelInfo processModelInfo = adminClient.getProcessModelResource().create(modelResource);
            LOGGER.info("ProcessModel " + processModelInfo.getName()+" uploaded");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        List<Credentials> allCredentials = asList(
                Credentials.of("alice", "alice".toCharArray()),
                Credentials.of("jdoe", "jdoe".toCharArray()),
                //Credentials.of("jodoe", "jodoe".toCharArray()),
                Credentials.of("miriam", "miriam".toCharArray())
        );

        if (configuration.isIndexed()) {
            Integer index = Integer.valueOf(System.getenv("JOB_COMPLETION_INDEX"));
            if (index == 0) {
                LOGGER.log(Level.INFO, "Running as Controller");
                //send info when all started processed are finished
            } else {
                Credentials credentials = allCredentials.get(index - 1);
                LOGGER.log(Level.INFO, "Running as User " + credentials.getUserName());
                UserClient userClient = UserClient.of(configuration, credentials);

                ExecutorService executorService = Executors.newSingleThreadExecutor();
                executorService.submit(userClient::start);
                executorService.shutdown();

                //send info about started processed

                //retrieve info when to stop


            }
        } else {
            executeSinglePod(configuration, allCredentials);
        }
    }

    private void executeSinglePod(Configuration configuration, List<Credentials> allCredentials) {
        ExecutorService taskExecutorService = Executors.newWorkStealingPool();

        ExecutorService executorService = Executors.newFixedThreadPool(allCredentials.size());
        List<UserClient> userClients = allCredentials.stream()
                .map(credentials -> UserClient.of(configuration,taskExecutorService, credentials))
                .collect(Collectors.toList());

        userClients.forEach(client->{
                    executorService.submit(() -> client.start());
                });
        executorService.shutdown();
        try {
            executorService.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }

        boolean allFinished = false;
        while (!allFinished) {
            allFinished = userClients.stream()
                    .mapToLong(userClient -> userClient.getActiveProcesses().size())
                    .sum() == 0;

            if(LOGGER.isLoggable(Level.FINEST)) {
                for (UserClient userClient : userClients) {
                    List<ProcessInfo> activeProcesses = userClient.getActiveProcesses();
                    if (!activeProcesses.isEmpty()) {
                        LOGGER.finest("User[" + userClient.getUserToken().getName() + "] has active processes " +
                                activeProcesses.stream()
                                        .map(processInfo -> asString(processInfo))
                                        .collect(Collectors.joining(","))
                        );
                    }
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
                // Restore interrupted state...
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.info("All started processes finished");
        taskExecutorService.shutdown();
        for (UserClient client : userClients) {
            client.stop();
        }
        try {
            executorService.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
        }
        LOGGER.info("Everything done");

        StringBuilder builder = new StringBuilder("start,end,duration,taskcount\n");
        String data = userClients.stream()
                .flatMap(UserClient::getStatistics)
                .map(Statistics::toString)
        .collect(Collectors.joining("\n"));
        builder.append(data).append("\n");

        LOGGER.info("statistics: \n" + builder.toString());

        try {
            Files.writeString(
                    Path.of("/var/run/e2e-client/statistics.csv"),builder.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String asString(ProcessInfo processInfo) {
        return format("%s started at %s by %s in state %s",
                processInfo.getProcessModelInfo().getName(),
                processInfo.getStartTime(),
                processInfo.getOwner().getName(),
                processInfo.getSubjects().stream()
                        .map(ProcessInfo.SubjectStateInfo::getStateName)
                        .collect(Collectors.joining(","))
        );
    }
}
