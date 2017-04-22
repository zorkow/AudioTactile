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
 * @file   Logger.java
 * @author Volker Sorge
 *         <a href="mailto:V.Sorge@progressiveaccess.com">Volker Sorge</a>
 * @date   Sat Feb 14 12:06:23 2015
 *
 * @brief  Logger facilities for logging and error output.
 *
 *
 */

//

package com.progressiveaccess.audiotactile;

import java.io.PrintWriter;

/**
 * Logger facilities:
 *
 * <p>
 * Error logging is either to file or stderr. Message logging is either to file
 * or stdout.
 * </p>
 *
 */
public final class Logger {
  private static Boolean debug = false;
  private static Boolean verbose = false;
  private static PrintWriter logFile = new PrintWriter(System.out);
  private static PrintWriter errFile = new PrintWriter(System.err);


  /** Dummy constructor. */
  private Logger() {
    throw new AssertionError("Instantiating utility class...");
  }


  /** Starts logging facilities. */
  public static void start() {
    Logger.debug = Cli.hasOption("d");
    Logger.verbose = Cli.hasOption("v");
  }


  /**
   * Prints debug information if option is set.
   *
   * @param str
   *          The information to print.
   */
  public static void error(final Object str) {
    if (Logger.debug) {
      Logger.errFile.print(str);
    }
  }


  /**
   * Prints verbose information if option is set.
   *
   * @param str
   *          The information to print.
   */
  public static void logging(final Object str) {
    if (Logger.verbose) {
      Logger.logFile.print(str);
    }
  }
}
