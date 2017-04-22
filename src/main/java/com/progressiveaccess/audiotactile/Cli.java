// Copyright 2015 Volker Sorge
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @file   Cli.java
 * @author Volker Sorge
 *         <a href="mailto:V.Sorge@progressiveaccess.com">Volker Sorge</a>
 * @date   Sat Feb 14 12:06:05 2015
 *
 * @brief  Command line interface.
 *
 *
 */

//

package com.progressiveaccess.audiotactile;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line interface for enricher app.
 */
public final class Cli {

  private static CommandLine cl;
  private static List<String> files = new ArrayList<String>();


  /** Dummy constructor. */
  private Cli() {
    throw new AssertionError("Instantiating utility class...");
  }


  /**
   * Initialises the command line interpreter.
   *
   * @param args
   *          The command line argument list.
   */
  public static void init(final String[] args) {
    Cli.parse(args);
  }


  /**
   * Parses the command line arguments.
   *
   * @param args
   *          The command line argument list.
   */
  private static void parse(final String[] args) {
    final Options options = new Options();
    // Basic Options
    options.addOption("help", false, "Print this message");
    options.addOption("d", "debug", false, "Debug mode");
    options.addOption("v", "verbose", false, "Verbose mode");
    // File Handling
    options.addOption("o", "output", true, "Output file");
    // Processing Options
    // Not sure if we need this!
    // options.addOption("ath", "tactile_hydrogens", false,
    //     "Audio tactile option: include explicit hydrogens in atom sets.");
    options.addOption("p", "polygons", false,
        "Use polygons instead of bounding boxes.");
    // // Iveo
    options.addOption("i", "iveo", false,
        "Audio tactile option: create SVG suitable for IVEO.");
    options.addOption("ivb", "iveo_buttons", false,
        "Iveo option: Add exploration buttons.");
    options.addOption("ivc", "iveo_collision", false,
        "Iveo option: Move buttons by avoiding collisions.");
    // Internationalisation
    options.addOption("l", "language", true, "Which language to be chosen.");

    final CommandLineParser parser = new DefaultParser();
    try {
      Cli.cl = parser.parse(options, args);
    } catch (final ParseException e) {
      usage(options, 1);
    }
    if (Cli.cl.hasOption("help")) {
      usage(options, 0);
    }

    for (int i = 0; i < Cli.cl.getArgList().size(); i++) {
      final String fileName = Cli.cl.getArgList().get(i).toString();
      final File file = new File(fileName);
      if (file.exists() && !file.isDirectory()) {
        Cli.files.add(fileName);
      } else {
        Cli.warning(fileName);
      }
    }

  }


  /**
   * Prints the usage information.
   *
   * @param options
   *          The options of the cli.
   * @param exitValue
   *          The exit value.
   */
  private static void usage(final Options options, final int exitValue) {

    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("atDiagram.sh", options);
    System.exit(exitValue);
  }


  /**
   * Prints warnings for missing file names.
   *
   * @param fileName
   *          The filename.
   */
  private static void warning(final String fileName) {
    System.err.println("Warning: File " + fileName
        + " does not exist. Ignored!");
  }


  /**
   * Checks if the command line has a particular option.
   *
   * @param option
   *          The option in question.
   *
   * @return True if the option is available.
   */
  public static boolean hasOption(final String option) {
    return Cli.cl.hasOption(option);
  }


  /**
   * Retrieves the value for a particular option.
   *
   * @param option
   *          The option in question.
   *
   * @return The value of that option.
   */
  public static String getOptionValue(final String option) {
    return Cli.cl.getOptionValue(option);
  }


  /**
   * @return The file list given on the command line.
   */
  public static List<String> getFiles() {
    return Cli.files;
  }

}
