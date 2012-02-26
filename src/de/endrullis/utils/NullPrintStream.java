package de.endrullis.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Alias /dev/null.
 */
public class NullPrintStream extends PrintStream {
  public NullPrintStream() {
    super(new PrintStream(new NullOutputStream()));
  }

  public static class NullOutputStream extends OutputStream {
    public void write(int b) throws IOException {
    }
  }
}
