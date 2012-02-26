package de.endrullis.utils;

import java.io.*;

/**
 * StreamUtils
 *
 * @author Jörg Endrullis und Stefan Endrullis
 * @version 1.2
 */
public class StreamUtils{
  /** Size of the temporary buffer. */
  private static int TEMPORARY_BUFFER_SIZE = 10000;

  /**
   * Reads from the input stream and returns the content as byte array.
   *
   * @param stream input stream
   * @return content of the stream
   * @exception IOException if an I/O error occurs
   */
  public static byte[] readBytesFromInputStream(InputStream stream) throws IOException {
    BufferedInputStream bufferedStream = new BufferedInputStream(stream, TEMPORARY_BUFFER_SIZE);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // create the temporary input stream
    byte temporary_buffer[] = new byte[TEMPORARY_BUFFER_SIZE];

    // read the content in blocks of size TEMPORARY_BUFFER_SIZE
    int bytes_read = 0;
    while((bytes_read = bufferedStream.read(temporary_buffer)) != -1){
      if(bytes_read != 0) outputStream.write(temporary_buffer, 0, bytes_read);
    }

    return outputStream.toByteArray();
  }
  
  /**
   * Copy the content of one stream to another.
   *
   * @param in input stream
   * @param out output stream
   * @throws IOException thrown if reading or writing has failed
   */
  public static void copyStream(InputStream in, OutputStream out) throws IOException {
    byte[] bytes = new byte[1024];
    int len;
    while((len = in.read(bytes)) != -1) out.write(bytes, 0, len);
  }

	/**
	 * Copies the directory structure with all the included files to a new directory.
	 * The files are processed linewise, i.e., the OS specific newline character will be used
	 * to break lines.
	 *
	 * @param inDir directory that shall be copied
	 * @param outDir directory the files and directories shall be copied to
	 * @throws IOException thrown if reading or writing has failed
	 */
	public static void copyFileRecursivlyLinewise(File inDir, File outDir) throws IOException {
		File[] inputFiles = inDir.listFiles();
		for (File inputFile : inputFiles) {
			File outputFile = new File(outDir, inputFile.getName());
			// copy program files line-wise to convert use OS specific newlines
			if (inputFile.isFile()) {
				copyFileLinewise(inputFile, outputFile);
			} else {
				File newOutDir = new File(outDir, inputFile.getName());
				newOutDir.mkdir();
				copyFileRecursivlyLinewise(inputFile, newOutDir);
			}
		}
	}

	/**
	 * Copies one file linewise to another file.
	 *
	 * @param inputFile input file (that shall be copied)
	 * @param outputFile destination file
	 * @throws IOException thrown if reading or writing has failed
	 */
	public static void copyFileLinewise(File inputFile, File outputFile) throws IOException {
		try {
			BufferedReader r = new BufferedReader(new FileReader(inputFile));
			BufferedWriter w = new BufferedWriter(new FileWriter(outputFile));

			String line;
			while ((line = r.readLine()) != null) {
				w.write(line);
				w.newLine();
			}

			w.close();
			r.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

  /**
   * Reads the specified InputStream and returns it as String in an Encoding
   * found in the first 100 bytes defined by encoding="...".
   * If no encoding definition is found the default UTF-8 is used.
   *
   * @param stream input stream.
   * @return decoded xml string
   * @exception IOException if an I/O error occurs
   */
  public static String readXmlStream(InputStream stream) throws IOException {
    byte[] bytes = readBytesFromInputStream(stream);

    // try to guess the encoding  todo POST header zusätzlich auswerten
    String encoding = "UTF-8";
    int length = Math.min(bytes.length, 100);
    String beginning = new String(bytes, 0, length);
    if(beginning.indexOf("encoding=\"") != -1){
      int encodingStart = beginning.indexOf("encoding=\"") + 10;
      int encodingEnd = beginning.indexOf('"', encodingStart);
      if(encodingStart < encodingEnd) encoding = beginning.substring(encodingStart, encodingEnd);
    }

    if (encoding != null) {
      try {
        return new String(bytes,encoding);
      } catch (UnsupportedEncodingException e) {
        System.err.println("Error in StreamUtils.readStringFromInputStream during encoding.");
        e.printStackTrace();
        return null;
      }
    } else {
      return new String(bytes);
    }
  }

	/**
	 * Reads the contents of the file.
	 *
	 * @param filename the filename
	 * @return the content of the file
	 */
	public static String readFile(String filename) throws IOException {
	  InputStream inputStream = getInputStream(filename);

	  byte data[] = readBytesFromInputStream(inputStream);
	  if(data == null) return null;

	  return new String(data);
	}

	/**
	 * Opens an InputStream for the given resource name.
	 *
	 * @param fileName resource name / file name (does not have to start with a "/")
	 * @return InputStream for the given resource
	 */
	public static InputStream getInputStream(String fileName) throws FileNotFoundException {
		// try to find the resource in local class resources
		InputStream localStream = StreamUtils.class.getResourceAsStream("/" + fileName);
		if(localStream != null) return new BufferedInputStream(localStream);

		//find file in local data directory
		//if (new java.io.File(fileName).exists()) {
		return new FileInputStream(fileName);
		//}

		// load the resource from web
		//return new URL(DATA_DIRECTORY_CLIENT + fileName).openStream();
	}
}
