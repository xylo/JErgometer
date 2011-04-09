package org.jergometer.communication;

import gnu.io.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.jergometer.translation.I18n;

import javax.swing.*;

/**
 * BikeConnectorSerial connects to the bike via serial port (e.g. RS232 or USB).
 * It is used to receive data from the bike and to control it.
 */
public class BikeConnectorSerial implements BikeConnector {

// static

	static {
		String driverName = "gnu.io.RXTXCommDriver";
		try {
			CommDriver driver = (CommDriver) Class.forName(driverName).newInstance();
			driver.initialize();
		} catch (Exception e) {
			System.err.println("Could not load serial port driver \"" + driverName + "\".");
			e.printStackTrace();
		}
	}

	public static ArrayList<String> getPortNames() {
		ArrayList<String> portNames = new ArrayList<String>();

		Enumeration portList = CommPortIdentifier.getPortIdentifiers();

		while (portList.hasMoreElements()) {
			CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();

			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				portNames.add(portId.getName()); // + " (owner: " + portId.getCurrentOwner() + ")"
			}
		}

		return portNames;
	}


// dynamic

	private String serialName;
	private SerialPort serialPort;
	private BikeReader reader = null;
	private BikeWriter writer = null;

	public BikeConnectorSerial(String serialName) throws BikeException, UnsupportedCommOperationException, IOException {
		connect(serialName);
	}

	public void connect(String serialName) throws BikeException, UnsupportedCommOperationException, IOException {
		this.serialName = serialName;

		Enumeration portList = CommPortIdentifier.getPortIdentifiers();

		while (portList.hasMoreElements()) {
			CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();

			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if(portId.getName().equals(serialName)) {
					if(portId.isCurrentlyOwned()) {
						throw new BikeException(I18n.getString("msg.com_port_used_by_the_following_application", portId.getCurrentOwner()));
					}
					try {
						serialPort = (SerialPort) portId.open("JErgometer", 2000);
					} catch (PortInUseException e) {
						throw new BikeException(I18n.getString("msg.com_port_used_by_the_following_application", portId.getCurrentOwner()));
					}

					connect(serialPort);
				}
			}
		}
	}

	public void connect(SerialPort serialPort) throws UnsupportedCommOperationException, IOException {
		serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
																	 SerialPort.STOPBITS_1,
																	 SerialPort.PARITY_NONE);

		// set reader and writer
		writer = new BikeWriter(serialPort.getOutputStream());
		RXTXReader rxtxReader = new RXTXReader(serialPort);
		reader = new BikeReader(rxtxReader);
	}

	public void close() throws IOException {
		// stop reader and writer
		if(reader != null) {
			reader.close();
			reader = null;
		}
		if(writer != null) {
			writer = null;
		}

		// close streams and socket
		if(serialPort != null) {
			serialPort.getOutputStream().close();
			serialPort.getInputStream().close();
			serialPort.removeEventListener();
			serialPort.close();
		}
	}


// getters and setters
	public BikeReader getReader() {
		return reader;
	}

	public BikeWriter getWriter() {
		return writer;
	}
}
