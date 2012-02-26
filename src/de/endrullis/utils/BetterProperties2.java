package de.endrullis.utils;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * BetterProperties2 is an improved implementation of {@link de.endrullis.utils.BetterProperties} and
 * {@link java.util.Properties}.
 *
 * It allows you to
 * <ul>
 * <li>define the order of properties used when writing the properties file</li>
 * <li>define several comments in the properties file</li>
 * <li>set defaults for property values</li>
 * <li>specify value ranges for properties</li>
 * <li>access boolean, integer, long, double and float values by using same-named methods</li>
 * </ul>
 *
 * @author Stefan Endrullis
 */
public class BetterProperties2 extends Properties {
  /** Default values. */
  private ArrayList<Entry> entries = new ArrayList<Entry>();
	private HashMap<String,Def> defMap = new HashMap<String, Def>();
	private boolean ensureValidRanges = true;

	public BetterProperties2() {
    super();
  }

  public BetterProperties2(Properties properties) {
    super(properties);
  }

  /**
   * Adds a comment or a default key-value pair to this object.
   *
   * @param entry comment of default to add
   */
  public void addEntry(Entry entry) {
    entries.add(entry);
	  if (entry instanceof Def) {
			Def def = (Def) entry;
			defMap.put(def.key, def);
		}
  }

	public ArrayList<Entry> getEntries() {
		return entries;
	}

	public HashMap<String, Def> getDefMap() {
		return defMap;
	}

	@Override
  public String setProperty(String key, String value) {
		if (value == null) {
			remove(key);
			return null;
		} else {
      return (String) put(key, value);
		}
  }

  @Override
  public void load(Reader reader) throws IOException {
    // initialize map with defaults
    loadDefaults();

    super.load(reader);
  }

  public void loadDefaults() {
	  for (Def def : defMap.values()) {
		  if (def.getValue() != null) {
		    put(def.getKey(), def.getValue());
		  }
	  }
  }

	private void checkRanges() {
		if (ensureValidRanges) {
			for (Def def : defMap.values()) {
				// if user value is not valid -> reset to default
				if (!def.range.isValid(getString(def.key))) {
					put(def.getKey(), def.getValue());
				}
			}
		}
	}

  @Override
  public void load(InputStream inputStream) throws IOException {
    // initialize map with defaults
    loadDefaults();

    super.load(inputStream);

	  checkRanges();
  }

	@Override
  public void store(Writer writer, String comments) throws IOException {
    store0((writer instanceof BufferedWriter) ? (BufferedWriter) writer : new BufferedWriter(writer),
        comments, false);
  }

  @Override
  public void store(OutputStream out, String comments) throws IOException {
    store0(new BufferedWriter(new OutputStreamWriter(out, "8859_1")), comments, true);
  }

  @Override
  public void loadFromXML(InputStream inputStream) throws IOException, InvalidPropertiesFormatException {
    // initialize map with defaults
    loadDefaults();

    super.loadFromXML(inputStream);

	  checkRanges();
  }

  @Override
  public void storeToXML(OutputStream outputStream, String s) throws IOException {
    throw new UnsupportedOperationException("not implemented jet");

    // TODO
    //super.storeToXML(outputStream, s);
  }

  @Override
  public void storeToXML(OutputStream outputStream, String s, String s1) throws IOException {
    throw new UnsupportedOperationException("not implemented jet");

    // TODO
    //super.storeToXML(outputStream, s, s1);
  }

  @Override
  public String getProperty(String s) {
    return super.getProperty(s);
  }

  @Override
  public String getProperty(String s, String s1) {
    return super.getProperty(s, s1);
  }

  @Override
  public Enumeration<?> propertyNames() {
    return super.propertyNames();
  }

  @Override
  public Set<String> stringPropertyNames() {
    return super.stringPropertyNames();
  }

  @Override
  public void list(PrintStream printStream) {
    super.list(printStream);
  }

  @Override
  public void list(PrintWriter printWriter) {
    super.list(printWriter);
  }

  @Override
  @Deprecated
  public void save(OutputStream outputStream, String s) {
    throw new RuntimeException("this deprecated method has been removed");
  }

  private void store0(BufferedWriter bw, String comment, boolean escUnicode) throws IOException {
    writeComments(bw, comment);
    synchronized (this) {
      for (Entry entry : entries) {
        if (entry instanceof Comment) {
          Comment c = (Comment) entry;
          writeComments(bw, c.getComment());
        } else if (entry instanceof Def) {
          Def def = (Def) entry;
          String key = def.getKey();
          String currValue = getProperty(key);
          String defValue = def.getValue();

          key = saveConvert(key, true, escUnicode);

          if (currValue == null) {
            bw.write('#');
            bw.write(key + "=" + def.getAltValue());
          } else {
            // No need to escape embedded and trailing spaces for value, hence
            // pass false to flag.
            String val = saveConvert(currValue, false, escUnicode);
            if (currValue.equals(defValue)) {
              bw.write('#');
            }
            bw.write(key + "=" + val);
          }
          bw.newLine();
        }
      }
    }
    bw.flush();
  }

  /**
   * Converts unicodes to encoded &#92;uxxxx and escapes
   * special characters with a preceding slash.
   */
  private String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode) {
    int len = theString.length();
    int bufLen = len * 2;
    if (bufLen < 0) {
      bufLen = Integer.MAX_VALUE;
    }
    StringBuffer outBuffer = new StringBuffer(bufLen);

    for (int x = 0; x < len; x++) {
      char aChar = theString.charAt(x);
      // Handle common case first, selecting largest block that
      // avoids the specials below
      if ((aChar > 61) && (aChar < 127)) {
        if (aChar == '\\') {
          outBuffer.append('\\');
          outBuffer.append('\\');
          continue;
        }
        outBuffer.append(aChar);
        continue;
      }
      switch (aChar) {
        case ' ':
          if (x == 0 || escapeSpace)
            outBuffer.append('\\');
          outBuffer.append(' ');
          break;
        case '\t':
          outBuffer.append('\\');
          outBuffer.append('t');
          break;
        case '\n':
          outBuffer.append('\\');
          outBuffer.append('n');
          break;
        case '\r':
          outBuffer.append('\\');
          outBuffer.append('r');
          break;
        case '\f':
          outBuffer.append('\\');
          outBuffer.append('f');
          break;
        case '=': // Fall through
        case ':': // Fall through
        case '#': // Fall through
        case '!':
          outBuffer.append('\\');
          outBuffer.append(aChar);
          break;
        default:
          if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
            outBuffer.append('\\');
            outBuffer.append('u');
            outBuffer.append(getHexDigit(aChar >> 12));
            outBuffer.append(getHexDigit(aChar >> 8));
            outBuffer.append(getHexDigit(aChar >> 4));
            outBuffer.append(getHexDigit(aChar));
          } else {
            outBuffer.append(aChar);
          }
      }
    }
    return outBuffer.toString();
  }

  protected static void writeComments(BufferedWriter bw, String comments)
      throws IOException {
    int len = comments.length();
    int current = 0;
    int last = 0;
    char[] uu = new char[6];
    uu[0] = '\\';
    uu[1] = 'u';

    boolean firstCharLineBreak = false;
    if (len > current && comments.charAt(current) == '\r') {
      firstCharLineBreak = true;
      current++;
    }
    if (len > current && comments.charAt(current) == '\n') {
      firstCharLineBreak = true;
      current++;
    }
    if (firstCharLineBreak) bw.newLine();
    last = current;

    bw.write("#");

    while (current < len) {
      char c = comments.charAt(current);
      if (c > '\u00ff' || c == '\n' || c == '\r') {
        if (last != current)
          bw.write(comments.substring(last, current));
        if (c > '\u00ff') {
          uu[2] = getHexDigit(c >> 12);
          uu[3] = getHexDigit(c >> 8);
          uu[4] = getHexDigit(c >> 4);
          uu[5] = getHexDigit(c & 0xf);
        } else {
          bw.newLine();
          if (c == '\r' &&
              current != len - 1 &&
              comments.charAt(current + 1) == '\n') {
            current++;
          }
          if (current != len - 1 &&
              (comments.charAt(current + 1) != '#' &&
                  comments.charAt(current + 1) != '!'))
            bw.write("#");
        }
        last = current + 1;
      }
      current++;
    }
    if (last != current)
      bw.write(comments.substring(last, current));
    bw.newLine();
  }

  /**
   * Convert a nibble to a hex character
   *
   * @param nibble the nibble to convert.
   * @return returns the hex digit
   */
  protected static char getHexDigit(int nibble) {
    return hexDigit[(nibble & 0xF)];
  }

  /**
   * A table of hex digits
   */
  protected static final char[] hexDigit = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
  };



  /*********************
   * Typed get methods *
   *********************/

  /**
   * Returns the property value as string.
   *
   * @param key name of the property
   * @return property value
   */
  public String getString(String key) {
    return getProperty(key);
  }

  /**
   * Returns the property value as int.
   *
   * @param key name of the property
   * @return property value
   */
  public int getInt(String key) {
    return Integer.parseInt(getProperty(key));
  }

  /**
   * Returns the property value as long.
   *
   * @param key name of the property
   * @return property value
   */
  public long getLong(String key) {
    return Long.parseLong(getProperty(key));
  }

  /**
   * Returns the property value as double.
   *
   * @param key name of the property
   * @return property value
   */
  public double getDouble(String key) {
    return Double.parseDouble(getProperty(key));
  }

  /**
   * Returns the property value as float.
   *
   * @param key name of the property
   * @return property value
   */
  public float getFloat(String key) {
    return Float.parseFloat(getProperty(key));
  }

  /**
   * Returns the property value as boolean.
   *
   * @param key name of the property
   * @return property value
   */
  public boolean getBoolean(String key) {
    return Boolean.parseBoolean(getProperty(key));
  }

	public void setString(String key, String value) {
		setProperty(key, value);
	}

	public void setBoolean(String key, boolean value) {
		setProperty(key, "" + value);
	}

	public void setInt(String key, int value) {
		setProperty(key, "" + value);
	}

	public void setLong(String key, long value) {
		setProperty(key, "" + value);
	}

	public void setFloat(String key, float value) {
		setProperty(key, "" + value);
	}

	public void setDouble(String key, double value) {
		setProperty(key, "" + value);
	}



  /*****************
   * inner classes *
   *****************/
   
  public static interface Entry {
  }

  public static class Comment implements Entry {
    private String comment;

    public Comment(String comment) {
      this.comment = comment;
    }

    public String getComment() {
      return comment;
    }
  }

  public static class Def implements Entry {
    private String key;
    private Range range;
    private String value;
    private String altValue;

    public Def(String key, Range range, String value) {
      this.key = key;
	    this.range = range;
      this.value = value;
    }

    public Def(String key, Range range, String value, String altValue) {
      this.key = key;
	    this.range = range;
      this.value = value;
      this.altValue = altValue;
    }

    public String getKey() {
      return key;
    }

	  public Range getRange() {
		  return range;
	  }

	  public String getValue() {
      return value;
    }

    public String getAltValue() {
      return altValue;
    }
  }

	public static interface Range {
		public boolean isValid(String s);
		public String description();
	}
	
	public static class PInt implements Range {
		private int min = Integer.MIN_VALUE;
		private int max = Integer.MAX_VALUE;

		public PInt() {
		}
		public PInt(int min) {
			this.min = min;
		}
		public PInt(int min, int max) {
			this.min = min;
			this.max = max;
		}

		@Override
		public boolean isValid(String s) {
			try {
				int value = Integer.parseInt(s);
				return value >= min && value <= max;
			} catch (Exception e) {
				return false;
			}
		}
		@Override
		public String description() {
			return "integer value between " + min + " and " + max;
		}
	}

	public static class PDouble implements Range {
		private double min = Double.MIN_VALUE;
		private double max = Double.MAX_VALUE;

		public PDouble() {
		}
		public PDouble(double min) {
			this.min = min;
		}
		public PDouble(double min, double max) {
			this.min = min;
			this.max = max;
		}

		@Override
		public boolean isValid(String s) {
			try {
				double value = Double.parseDouble(s);
				return value >= min && value <= max;
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public String description() {
			return "double value between " + min + " and " + max;
		}
	}

	public static class PSet implements Range {
		public LinkedHashSet<String> content = new LinkedHashSet<String>();
		public PSet (String... items) {
			for (String item : items) {
				content.add(item);
			}
		}
		@Override
		public boolean isValid(String s) {
			return content.contains(s);
		}

		@Override
		public String description() {
			return "values out of (" + CollectionUtils.join(content, ", ") + ")";
		}
	}

	public static class PBoolean extends PSet {
		public PBoolean () {
			super("true", "false");
		}
	}

	public static class PString implements Range {
		@Override
		public boolean isValid(String s) {
			return true;
		}

		@Override
		public String description() {
			return "arbitrary string";
		}
	}

	public static class PShortcut implements Range {
		@Override
		public boolean isValid(String s) {
			try {
				return KeyStroke.getKeyStroke(s).getKeyCode() >= 0;
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		public String description() {
			return "Java shortcut";
		}
	}

	public static final Range INT           = new PInt();
	public static final Range INT_GT_0      = new PInt(0);
	public static final Range DOUBLE        = new PDouble();
	public static final Range DOUBLE_GT_0   = new PDouble(0);
	public static final Range DOUBLE_0_TO_1 = new PDouble(0, 1);
	public static final Range BOOLEAN       = new PBoolean();
	public static final Range STRING        = new PString();
	public static final Range SHORTCUT      = new PShortcut();
}
