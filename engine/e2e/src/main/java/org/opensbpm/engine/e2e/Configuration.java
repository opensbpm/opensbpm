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

public class Configuration {

    private static final Option URLOPTION = Option.builder("u")
            .longOpt("url")
            .hasArg(true)
            .argName("url")
            .desc("url of OpenSBPM-Engine")
            .required(true)
            .build();
    private static final Option AUTHURLOPTION = Option.builder("a")
            .longOpt("authurl")
            .hasArg(true)
            .argName("authurl")
            .desc("Use different url for authentication")
            .build();
    public static Configuration parseArgs(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption(URLOPTION);
        options.addOption(AUTHURLOPTION);
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
        return cmd.getOptionValue(URLOPTION.getOpt());
    }

    public boolean hasAuthUrl(){
        return cmd.hasOption(AUTHURLOPTION.getOpt());
    }

    public String getAuthUrl() {
        return cmd.getOptionValue(AUTHURLOPTION.getOpt());
    }

}
