package de.endrullis.utils;

import java.util.ArrayList;

/**
 * Utilities for Strings.
 *
 * @author Jörg Endrullis und Stefan Endrullis
 * @version 1.1
 */
public class StringUtils{
  public static String stringBefore(String inStr, String delimiterStr){
    int pos = inStr.indexOf(delimiterStr);
    if(pos == -1)
      return null;
    else
      return inStr.substring(0, pos);
  }

  /**
   * Returns the string before a delimiter string.
   *
   * @param inStr input string
   * @param delimiterStr delimiter
   * @param nr 'f' for first, 'l' for last
   * @return string before a delimiter string
   */
  public static String stringBefore(String inStr, String delimiterStr, char nr){
    if(nr == 'f') {
      return stringBefore(inStr, delimiterStr);
    }
    else {
      int pos = inStr.lastIndexOf(delimiterStr);
      if(pos == -1)
        return null;
      else
        return inStr.substring(0, pos);
    }
  }

  public static String stringAfter(String inStr, String delimiterStr){
    int pos = inStr.indexOf(delimiterStr);
    if(pos == -1)
      return null;
    else
      return inStr.substring(pos + delimiterStr.length(), inStr.length());
  }

  /**
   * Returns the string after a delimiter string.
   *
   * @param inStr input string
   * @param delimiterStr delimiter
   * @param nr 'f' for first, 'l' for last
   * @return string after a delimiter string
   */
  public static String stringAfter(String inStr, String delimiterStr, char nr){
    if(nr == 'f') {
      return stringAfter(inStr, delimiterStr);
    }
    else {
      int pos = inStr.lastIndexOf(delimiterStr);
      if(pos == -1)
        return null;
      else
        return inStr.substring(pos + delimiterStr.length(), inStr.length());
    }
  }

  public static String[] stringSplitter(String inStr, String delimiterStr){
    ArrayList<String> stringVector = new ArrayList<String>();
    String beforeStr;
    while((beforeStr = stringBefore(inStr, delimiterStr)) != null){
      stringVector.add(beforeStr);
      inStr = stringAfter(inStr, delimiterStr);
    }
    stringVector.add(inStr);

    return stringVector.toArray(new String[]{});
  }
}
