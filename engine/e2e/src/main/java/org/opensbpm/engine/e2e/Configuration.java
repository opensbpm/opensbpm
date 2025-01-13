/*******************************************************************************
 * Copyright (C) 2024 Stefan Sedelmaier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.opensbpm.engine.e2e;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.opensbpm.engine.client.Credentials;
import org.opensbpm.engine.client.EngineServiceClient;

public class Configuration {

    private static final Option URL_OPTION = Option.builder("u")
            .longOpt("url")
            .hasArg(true)
            .argName("url")
            .required(true)
            .desc("url of OpenSBPM-Engine")
            .build();
    private static final Option AUTHURL_OPTION = Option.builder("a")
            .longOpt("authurl")
            .hasArg(true)
            .argName("authurl")
            .desc("Use different url for authentication")
            .build();
    private static final Option INDEXED_OPTION = Option.builder("i")
            .longOpt("indexed")
            .argName("indexed")
            .desc("Run in indexed mode using env variable JOB_COMPLETION_INDEX")
            .build();

    private static final Option NODES_OPTION = Option.builder("n")
            .longOpt("nodes")
            .hasArg(true)
            .argName("nodes")
            .required(true)
            .desc("Number of nodes running")
            .build();

    private static final Option PODS_OPTION = Option.builder("p")
            .longOpt("pods")
            .hasArg(true)
            .argName("pods")
            .required(true)
            .desc("Number of pods running")
            .build();

    private static final Option PROCESSES_OPTION = Option.builder("c")
            .longOpt("processes")
            .hasArg(true)
            .argName("processes")
            .required(true)
            .desc("Number of processes to run")
            .build();

    public static Configuration parseArgs(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption(URL_OPTION);
        options.addOption(AUTHURL_OPTION);
        options.addOption(INDEXED_OPTION);
        options.addOption(NODES_OPTION);
        options.addOption(PODS_OPTION);
        options.addOption(PROCESSES_OPTION);
        try {
            CommandLine cmd = new DefaultParser().parse(options, args, true);
            return new Configuration(cmd);
        } catch (ParseException ex) {
            System.out.println("" + ex.getLocalizedMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.setOptionComparator(null);
            formatter.printHelp("e2e", options);
            throw ex;
        }
    }

    private final CommandLine cmd;

    private Configuration(CommandLine cmd) {
        this.cmd = cmd;
    }

    public String getUrl() {
        return cmd.getOptionValue(URL_OPTION.getOpt());
    }

    public boolean hasAuthUrl() {
        return cmd.hasOption(AUTHURL_OPTION.getOpt());
    }

    public String getAuthUrl() {
        return cmd.getOptionValue(AUTHURL_OPTION.getOpt());
    }

    public boolean isIndexed() {
        return cmd.hasOption(AUTHURL_OPTION.getOpt());
    }

    public Integer getNodeCount() {
        return Integer.valueOf(cmd.getOptionValue(NODES_OPTION.getOpt()));
    }

    public Integer getPodCount() {
        return Integer.valueOf(cmd.getOptionValue(PODS_OPTION.getOpt()));
    }

    public Integer getProcessCount() {
        return Integer.valueOf(cmd.getOptionValue(PROCESSES_OPTION.getOpt()));
    }

    public EngineServiceClient createEngineServiceClient(Credentials credentials) {
        final EngineServiceClient engineServiceClient;
        if (hasAuthUrl()) {
            engineServiceClient = EngineServiceClient.create(getAuthUrl(), getUrl(), credentials);
        } else {
            engineServiceClient = EngineServiceClient.create(getUrl(), credentials);
        }
        return engineServiceClient;
    }


}
