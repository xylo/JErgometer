package de.endrullis.utils;

import java.io.*;

/**
 * Utilities for files.
 */
public class FileUtils {
  /**
   * Reads the content of a text file and returns the it as string.
   *
   * @param file text file
   * @return content of the file as string
   * @throws IOException if an I/O error occurs 
   */
  public static String readFileAsString(File file) throws IOException {
    return new String(StreamUtils.readBytesFromInputStream(new FileInputStream(file)));
  }

  /**
   * Writes a string into a file.
   *
   * @param file text file
   * @param text text
   * @throws FileNotFoundException If the given file object does not denote an existing, writable regular file and a new regular file of that name cannot be created, or if some other error occurs while opening or creating the file
   */
  public static void writeStringToFile(File file, String text) throws FileNotFoundException {
    PrintStream out = new PrintStream(file);
    out.print(text);
    out.close();
  }
}
