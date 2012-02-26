package de.endrullis.utils;

import java.io.*;

/**
 * This is a PrintStream with colored/styled output.
 */
public class ShellPrintStream extends PrintStream {

// static

  public enum Colored {on, auto, off};

  // variables for the style of the text
  public final static int STYLE_NORMAL      = 0;
  public final static int STYLE_BOLD        = 1;
  public final static int STYLE_LIGHT = 1;
  public final static int STYLE_UNDERLINE   = 4;
  public final static int STYLE_FLASHING    = 5;
  public final static int STYLE_INVERT      = 7;

  // variables for color
  public final static int COLOR_BLACK       = 30;
  public final static int COLOR_RED         = 31;
  public final static int COLOR_GREEN       = 32;
  public final static int COLOR_YELLOW      = 33;
  public final static int COLOR_BLUE        = 34;
  public final static int COLOR_VIOLETT     = 35;
  public final static int COLOR_CYAN        = 36;
  public final static int COLOR_WHITE       = 37;

  // variables for color
  public final static int BGCOLOR_BLACK     = 40;
  public final static int BGCOLOR_RED       = 41;
  public final static int BGCOLOR_GREEN     = 42;
  public final static int BGCOLOR_YELLOW    = 43;
  public final static int BGCOLOR_BLUE      = 44;
  public final static int BGCOLOR_VIOLETT   = 45;
  public final static int BGCOLOR_CYAN      = 46;
  public final static int BGCOLOR_WHITE     = 47;

  /** Global ShellPrintStream replacing the System.out. */
  public static ShellPrintStream out = null;

  /**
   * Tests if the terminal supports colored and styled output and replaces the
   * System.out with an instance of ShellPrintStream.
   *
   * @return styled PrintStream
   */
  public static ShellPrintStream replaceSystemOut() {
    out = new ShellPrintStream(System.out, Colored.auto);

    return out;
  }

  /**
   * Tests if the terminal supports colored styled output and checks if the user
   * has set the command line argument "--color", and replaces the System.out
   * with an instance of ShellPrintStream.
   *
   * @param args command line arguments
   * @return styled PrintStream
   */
  public static ShellPrintStream replaceSystemOut(String args[]) {
    // default
    Colored colored = Colored.auto;

    // check if "--color" is specified
    for (String s : args) {
      if(s.startsWith("--color=")) {
        String valueString = s.substring("--color=".length());
        try {
          colored = Colored.valueOf(valueString);
        } catch(IllegalArgumentException e) { }
        break;
      }
    }

    out = new ShellPrintStream(System.out, colored);

    return out;
  }


// dynamic

  protected boolean colored = false;


// constructors
  public ShellPrintStream(OutputStream out, Colored c) {
    super(out);
    setColoredOrNot(c);
  }
  public ShellPrintStream(OutputStream out, boolean autoFlush, Colored c) {
    super(out, autoFlush);
    setColoredOrNot(c);
  }
  public ShellPrintStream(String fileName, Colored c) throws FileNotFoundException {
    super(fileName);
    setColoredOrNot(c);
  }
  public ShellPrintStream(String fileName, String csn, Colored c) throws FileNotFoundException, UnsupportedEncodingException {
    super(fileName, csn);
    setColoredOrNot(c);
  }
  public ShellPrintStream(File file, Colored c) throws FileNotFoundException {
    super(file);
    setColoredOrNot(c);
  }
  public ShellPrintStream(File file, String csn, Colored c) throws FileNotFoundException, UnsupportedEncodingException {
    super(file, csn);
    setColoredOrNot(c);
  }


// methods

  public void setColoredOrNot(Colored c) {
    switch(c) {

      case on:
        colored = true;
        break;

      case off:
        colored = false;
        break;

      case auto:
        String term = System.getenv("TERM");
        if(term != null) {
          if(term.startsWith("xterm") || term.startsWith("rxvt")) {
            colored = true;
          }
        }
        break;
    }
  }

  public boolean isColored() {
    return colored;
  }

  /**
   * Print a styled string. If the argument is null then the
   * string "null" is printed. Otherwise, the string's characters are
   * converted into bytes according to the platform's default
   * character encoding, and these bytes are written in exactly
   * the manner of the write(int) method.
   *
   * @param s string
   * @param styles styles
   */
  public void print(String s, int... styles) {
    setStyle(styles);
    super.print(s);
    resetStyle();
  }

  /**
   * Print a styled string and then terminate the line.
   * This method behaves though it invokes <code>{@link #print(String)}</code>
   * and then <code>{@link #println()}</code>.
   *
   * @param x string
   * @param styles styles
   */
  public void println(String x, int... styles) {
    setStyle(styles);
    super.print(x);
    resetStyle();
    super.println();
  }

  /**
   * Sets the style and color of the ouput.
   *
   * @param styles
   */
  public void setStyle(int... styles) {
    if(!colored) return;

    String styleString = "" + styles[0];
    for (int i = 1; i < styles.length; i++) {
      styleString += ";" + styles[i];
    }

    print('\033'+"[" + styleString + "m");
  }

  /**
   * Returns the styled/colored text.
   *
   * @param text input text
   * @param styles styles
   * @return styled/colored text
   */
  public String style(String text, int... styles) {
    if(!colored) return text;

    String styleString = "" + styles[0];
    for (int i = 1; i < styles.length; i++) {
      styleString += ";" + styles[i];
    }

    return '\033'+"[" + styleString + "m" + text + '\033' + "[0m";
  }

  /**
   * Resets the style to normal output.
   */
  public void resetStyle() {
    if(!colored) return;

    print('\033' + "[0m");
  }

  public void clearScreen() {
    if(!colored) return;

    print('\033' + "[2J");
  }

  public void clearToEndOfLine() {
    if(!colored) return;

    print('\033' + "[K");
  }
}
