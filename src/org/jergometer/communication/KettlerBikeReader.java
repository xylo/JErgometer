package org.jergometer.communication;

import org.jergometer.model.DataRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * It reads incoming messages from the bike.
 */
public class KettlerBikeReader extends Thread implements BikeReader {

// static

	public static enum PrintAvailable { none, characters, decimals, hexadecimal }

	/** Client commands. */
	public static final String CMD_ACK      = "ACK";
	public static final String CMD_ERROR    = "ERROR";
	public static final String CMD_RUN      = "RUN";

	/** Input stream. */
	private InputStream inStream;
	private boolean closed = false;
	private BufferedReader in;
	/** BikeReaderListeners. */
	private ArrayList<BikeReaderListener> bikeReaderListeners = new ArrayList<BikeReaderListener>();
	/** Print available bytes (for debugging). */
	private PrintAvailable printAvailable = PrintAvailable.none;

	/**
	 * Creates the reader for the incoming messages of the bike.
	 *
	 * @param in input stream
	 */
	public KettlerBikeReader(InputStream in) {
		this.inStream = in;
		this.in = new BufferedReader(new InputStreamReader(inStream), 1);
	}

	public void run() {
		while(!isInterrupted()) {
			try {
				String dataString = in.readLine();
				if (dataString == null || closed) {
					return;
				}

				if(printAvailable == PrintAvailable.none) {
					if (dataString.contains(CMD_ACK)) {
						for (BikeReaderListener listener : bikeReaderListeners) {
							listener.bikeAck();
						}
					}
					else if (dataString.equals(CMD_ERROR)) {
						for (BikeReaderListener listener : bikeReaderListeners) {
							listener.bikeError();
						}
					}
					else if (dataString.equals(CMD_RUN)) {
						for (BikeReaderListener listener : bikeReaderListeners) {
							listener.bikeAck();
						}
					}
					else {
						for (BikeReaderListener listener : bikeReaderListeners) {
							listener.bikeData(new DataRecord(dataString));
						}
					}
				}
				else {
					// for debugging
					byte[] bytes = dataString.getBytes();

					if(printAvailable == PrintAvailable.characters) {
						System.err.print(dataString);
					}
					else if(printAvailable == PrintAvailable.decimals) {
						for (byte aByte : bytes) {
							System.out.print("," + (aByte & 0xFF));
						}
					}
					else if(printAvailable == PrintAvailable.hexadecimal) {
						for (byte aByte : bytes) {
							System.out.format(",%X", (aByte & 0xFF));
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void close() {
		closed = true;
		try {
			inStream.close();
			this.interrupt();
		} catch (IOException ignored) {}
		super.interrupt();
	}

	public PrintAvailable getPrintAvailable() {
		return printAvailable;
	}

	public void setPrintAvailable(PrintAvailable printAvailable) {
		this.printAvailable = printAvailable;
	}

	public void addBikeReaderListener(BikeReaderListener listener) {
		bikeReaderListeners.add(listener);
	}

	public void removeBikeReaderListener(BikeReaderListener listener) {
		bikeReaderListeners.remove(listener);
	}
}
