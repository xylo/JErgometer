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
public class KettlerBikeReader extends Thread {

// static

	public static enum PrintAvailable { none, characters, decimals, hexadecimal }

	/** Client commands. */
	public static final String CMD_ACK      = "ACK";
	public static final String CMD_ERROR    = "ERROR";
	public static final String CMD_RUN      = "RUN";

// dynamic

	/** Input stream. */
	private InputStream inStream;
	private boolean closed = false;
	private BufferedReader in;
	/** BikeReaderListeners. */
	private ArrayList<BikeListener> bikeListeners = new ArrayList<BikeListener>();
	/** Print available bytes (for debugging). */
	private PrintAvailable printAvailable = PrintAvailable.none;
	private int lastJergometerDesPower = 0;
	private int jergometerDestPower = 0;

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
						for (BikeListener listener : bikeListeners) {
							listener.bikeAck();
						}
					}
					else if (dataString.equals(CMD_ERROR)) {
						for (BikeListener listener : bikeListeners) {
							listener.bikeError();
						}
					}
					else if (dataString.equals(CMD_RUN)) {
						for (BikeListener listener : bikeListeners) {
							listener.bikeAck();
						}
					}
					else {
						DataRecord data = new DataRecord(dataString);

						if (lastJergometerDesPower == 0) {
							lastJergometerDesPower = data.getDestPower();
						}

						for (BikeListener listener : bikeListeners) {
							if (data.getDestPower() != lastJergometerDesPower && data.getDestPower() != jergometerDestPower) {
								listener.bikeDestPowerChanged((data.getDestPower() - jergometerDestPower)/5);
							}
							listener.bikeData(data);
						}
						lastJergometerDesPower = data.getDestPower();
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

	public void addBikeReaderListener(BikeListener listener) {
		bikeListeners.add(listener);
	}

	public void removeBikeReaderListener(BikeListener listener) {
		bikeListeners.remove(listener);
	}

	public void removeAllBikeReaderListeners() {
		bikeListeners.clear();
	}

	public void setJErgometerDestPower(int jergometerDestPower) {
		this.jergometerDestPower = jergometerDestPower;
	}
}
