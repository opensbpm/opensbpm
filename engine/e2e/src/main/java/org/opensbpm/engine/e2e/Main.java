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

import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.LogManager;

import org.apache.commons.cli.ParseException;
import org.opensbpm.engine.api.ProcessNotFoundException;
import org.opensbpm.engine.api.UserNotFoundException;
import org.opensbpm.engine.api.instance.*;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Arrays.asList;


public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
            new Main(Configuration.parseArgs(args)).execute();
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

    private final Configuration configuration;

    private Main(Configuration configuration) {
        this.configuration = configuration;
    }

    public void execute() throws UserNotFoundException, ProcessNotFoundException,
            IOException, InterruptedException, ExecutionException, GeneralSecurityException {

        List<UserClient> userClients = asList(
                UserClient.of(configuration, "alice", "alice"),
                UserClient.of(configuration, "jdoe", "jdoe"),
                UserClient.of(configuration, "miriam", "miriam")
        );
        ExecutorService executorService = Executors.newFixedThreadPool(userClients.size());
        for (UserClient client : userClients) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    client.start();
                }
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

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
        for (UserClient client : userClients) {
            client.stop();
        }
        LOGGER.info("Everything done");
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
