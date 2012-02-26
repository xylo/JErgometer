package de.endrullis.utils;

import java.util.HashMap;

/**
 * Manage command line parameters in the form "key" or "key=value".
 *
 * @author Stefan Endrullis
 * @version 1.0
 */
public class Params {
  /** Arguments in the form key value. */
  protected HashMap<String,String> optionsHash = new HashMap<String,String>();

  public Params(String[] args) {
    for(String argument : args){
      int eqIndex = argument.indexOf('=');
      if(eqIndex != -1){
        optionsHash.put(argument.substring(0, eqIndex), argument.substring(eqIndex + 1));
      }else{
        optionsHash.put(argument, "");
      }
    }
  }

  /**
   * Returns true if the option is in the param list.
   *
   * @param option option
   * @return true if the option is in the param list
   */
  public boolean isOptionAvailable(String option) {
    return optionsHash.containsKey(option);
  }

  /**
   * Returns the value for the option.
   *
   * @param option option
   * @return value for option
   */
  public String getValueFor(String option) {
    return optionsHash.get(option);
  }
}
