package de.endrullis.utils;

import java.io.PrintStream;

/**
 * Output.
 */
public class Debug {
  public static PrintStream out;

  public static void setOutput(PrintStream out) {
    Debug.out = out;
  }
}
