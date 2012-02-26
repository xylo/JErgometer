package de.endrullis.utils;

import org.jetbrains.annotations.NonNls;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Extended command line parameter manager.
 * Manages command line parameters in the form "-k", "--key",
 * "-k [value1 [value2 [...]]]" or "--key[=value1[:value2[:...]]]".
 *
 * @author Stefan Endrullis
 * @version 1.0
 */
public class ParamsExt {
  /** Syntax. */
  protected String syntax = null;
  /** List of options in the form (full name, one character, description). */
  protected Option[] options;
  /** Maximal length of option names. */
  protected int maxOptionNameLength = 0;
  /** Arguments in the form key value. */
  protected HashMap<String,String[]> optionsHash = new HashMap<String,String[]>();
  /** Command line arguments which are not bound to an option name. */
  protected ArrayList<String> unboundArguments = new ArrayList<String>();

  /**
   * Creates an extended command line parameter manager.
   *
   * @param options list of options in the form (full name, one character, description)
   * @param args list of command line parameters
   */
  public ParamsExt(Option[] options, String[] args) {
    this.options = options;

    // get maximal length of option names
    for (Option option : options) {
      maxOptionNameLength = Math.max(maxOptionNameLength, option.name.length());
    }

    // parse options
    for (int i = 0; i < args.length; i++) {
      String argument = args[i];

      if(argument.startsWith("--")) {
        int eqIndex = argument.indexOf('=');
        if(eqIndex != -1){
          String[] values = argument.substring(eqIndex + 1).split(":");
          optionsHash.put(argument.substring(2, eqIndex), values);
        }else{
          optionsHash.put(argument.substring(2), null);
        }
      }
      else if(argument.startsWith("-")) {
        Option option = getOption(argument.charAt(1));

        if(option.paramsCount == 0) {
          optionsHash.put(argument.substring(2), null);
        } else {
          String values[] = new String[option.paramsCount];
          try{
            System.arraycopy(args, i + 1, values, 0, values.length);
            i += values.length;
          } catch(ArrayIndexOutOfBoundsException e) {
            System.err.println("option -" + option.mnemonic + " requires more arguments");
            System.exit(1);
          }
          optionsHash.put(option.name, values);
        }
      }
      else {
        unboundArguments.add(argument);
      }
    }
  }

  private Option getOption(Character c) {
    for (Option option : options) {
      if(option.mnemonic == c) {
        return option;
      }
    }

    return null;
  }

  private Option getOption(String optionName) {
    for (Option option : options) {
      if(option.name.equals(optionName)) {
        return option;
      }
    }

    return null;
  }

  public void printHelp() {
    if(ShellPrintStream.out != null) {
      printHelp(ShellPrintStream.out);
    } else {
      printHelp(System.out);
    }
  }

  public void printHelp(PrintStream ps) {
    ShellPrintStream out;

    if(ps instanceof ShellPrintStream) {
      out = (ShellPrintStream) ps;
    } else {
      out = new ShellPrintStream(ps, ShellPrintStream.Colored.off);
    }

    if(syntax != null) {
      out.println("SYNTAX", out.STYLE_BOLD);
      out.println("  " + syntax);
      out.println();
    }

    out.println("OPTIONS", out.STYLE_BOLD);
    for (Option option : options) {
      StringBuilder sb = new StringBuilder();
      if(option.mnemonic == null) {
        sb.append("     ");
      } else {
        sb.append(out.style("  -" + option.mnemonic, out.STYLE_BOLD)).append(",");
      }
      sb.append(out.style("--" + option.name, out.STYLE_BOLD));
      appendSpaces(sb, 3 + maxOptionNameLength - option.name.length());
      sb.append(option.description);

      out.println(sb.toString());
    }
  }

  private void appendSpaces(StringBuilder sb, int count) {
    for(int i = 0; i < count; i++) {
      sb.append(' ');
    }
  }

  /**
   * Returns true if the option is in the param list.
   *
   * @param option option
   * @return true if the option is in the param list
   */
  public boolean isOptionAvailable(@NonNls String option) {
    return optionsHash.containsKey(option);
  }

  /**
   * Returns the value for the option.
   *
   * @param option option
   * @return value for option
   */
  public String getValueFor(String option) {
    String[] values = getValuesFor(option);

    if(values == null) {
      return null;
    } else {
      // concat the values with ':' to one string
      StringBuilder sb = new StringBuilder();
      sb.append(values[0]);
      for (int i = 1; i < values.length; i++) {
        sb.append(':').append(values[i]);
      }
      return sb.toString();
    }
  }

  /**
   * Returns the value for the option.
   *
   * @param option option
   * @return value for option
   */
  public String[] getValuesFor(String option) {
    return optionsHash.get(option);
  }


// getters and setters

  public String getSyntax() {
    return syntax;
  }

  public void setSyntax(String syntax) {
    this.syntax = syntax;
  }

  public ArrayList<String> getUnboundArguments() {
    return unboundArguments;
  }

  public static Option getColorOption() {
    return getColorOption(ShellPrintStream.out);
  }

  public static Option getColorOption(ShellPrintStream out) {
    return new ParamsExt.Option("color", 'c', "use styled output (if " + out.style("on",
        out.STYLE_BOLD) + ") or not (if " + out.style("off", out.STYLE_BOLD) + ")");
  }

// subclasses

  /**
   * This is the option structure without logic.
   */
  public static class Option {
    public String name;
    public Character mnemonic;
    public String description;
    public int paramsCount = 0;

    public Option(@NonNls String name, Character mnemonic, String description) {
      this.name = name;
      this.mnemonic = mnemonic;
      this.description = description;
    }

    public Option(String name, Character mnemonic, String description, int paramsCount) {
      this.name = name;
      this.mnemonic = mnemonic;
      this.description = description;
      this.paramsCount = paramsCount;
    }
  }
}
