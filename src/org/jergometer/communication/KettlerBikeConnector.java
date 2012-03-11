package org.jergometer.communication;

import gnu.io.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import org.jergometer.translation.I18n;

/**
 * KettlerBikeConnector connects to the bike via serial port (e.g. RS232 or USB).
 * It is used to receive data from the bike and to control it.
 */
public class KettlerBikeConnector implements BikeConnector {

// dynamic

	private SerialPort serialPort;
	public KettlerBikeReader reader = null;
	public KettlerBikeWriter writer = null;
	private int power;

	public void connect(String serialName, BikeListener listener) throws BikeException, UnsupportedCommOperationException, IOException {
		RXTXLibrary.init();

		Enumeration portList = CommPortIdentifier.getPortIdentifiers();

		while (portList.hasMoreElements()) {
			CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();

			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if(portId.getName().equals(serialName)) {
					if(portId.isCurrentlyOwned()) {
						throw new BikeException(I18n.getString("msg.serial_port_used_by_the_following_application", portId.getCurrentOwner()));
					}
					try {
						serialPort = (SerialPort) portId.open("JErgometer", 2000);
					} catch (PortInUseException e) {
						throw new BikeException(I18n.getString("msg.serial_port_used_by_the_following_application", portId.getCurrentOwner()));
					}

					connect(serialPort);
				}
			}
		}
		reader.addBikeReaderListener(listener);
		reader.start();
	}

	public void connect(SerialPort serialPort) throws UnsupportedCommOperationException, IOException {
		serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
																	 SerialPort.STOPBITS_1,
																	 SerialPort.PARITY_NONE);

		// set reader and writer
		writer = new KettlerBikeWriter(true, serialPort.getOutputStream());
		RXTXReader rxtxReader = new RXTXReader(serialPort);
		reader = new KettlerBikeReader(rxtxReader);
	}

	@Override
	public void sendHello() throws IOException {
		writer.sendHello();
	}

	@Override
	public void sendReset() throws IOException {
		writer.sendReset();
	}

	@Override
	public void sendGetId() throws IOException {
		writer.sendGetId();
	}

	@Override
	public void sendGetData() throws IOException {
		if (power != 0) {
			writer.sendSetPower(power);
			power = 0;
		} else {
			writer.sendGetData();
		}
	}

	@Override
	public void sendSetPower(int power) throws IOException {
		reader.setJErgometerDestPower(power);
		this.power = power;
	}

	@Override
	public void close() throws IOException {
		// stop reader and writer
		if(reader != null) {
			reader.removeAllBikeReaderListeners();
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

	@Override
	public String getName() {
		return "Kettler-RXTX";
	}

	@Override
	public String toString() {
		return getName();
	}
}
