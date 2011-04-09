package org.jergometer.communication;

import org.jergometer.translation.I18n;

import javax.swing.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.awt.*;
import java.util.Enumeration;

import gnu.io.SerialPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

/**
 * Tests the connection to the ergometer.
 */
public class BikeConnectionTester extends Thread {
	private enum State { init, openConnection, doReset, wait, retrieveId, finished, canceled }

	private Component owner;
	private String serialName;
	private SerialPort serialPort;
	private BufferedReader reader = null;
	private KettlerBikeWriter writer = null;

	private State state = State.init;
	private ProgressMonitor pm;
	private String id;

	private final Object sync = new Object();

	public BikeConnectionTester(Component owner, String serialName) {
		this.owner = owner;
		this.serialName = serialName;
	}

	public void connect(String serialName) {
		this.serialName = serialName;

		Enumeration portList = CommPortIdentifier.getPortIdentifiers();

		while (portList.hasMoreElements()) {
			CommPortIdentifier portId = (CommPortIdentifier) portList.nextElement();

			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				if(portId.getName().equals(serialName)) {
					try {
						if(portId.isCurrentlyOwned()) {
							System.out.println("Serial port is currently owned by another application!");
						}
						serialPort = (SerialPort) portId.open("BikeConnector", 2000);

						connect(serialPort);
					} catch (PortInUseException e) {
						e.printStackTrace();
					} catch (UnsupportedCommOperationException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void connect(SerialPort serialPort) throws UnsupportedCommOperationException, IOException {
		serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8,
																	 SerialPort.STOPBITS_1,
																	 SerialPort.PARITY_NONE);

		// set reader and writer
		writer = new KettlerBikeWriter(serialPort.getOutputStream());
		RXTXReader rxtxReader = new RXTXReader(serialPort);
		reader = new BufferedReader(new InputStreamReader(rxtxReader));
	}

	@Override
	public void run() {
		try {
			notifyParent(State.openConnection);
			connect(serialName);
			if (isCanceled()) return;

			notifyParent(State.doReset);
			writer.sendReset();
			if (isCanceled()) return;

			notifyParent(State.wait);
			if (isCanceled()) return;
			Thread.sleep(5000);

			// clear input buffer
			while (reader.ready()) reader.read();

			notifyParent(State.retrieveId);
			writer.sendGetId();
			if (isCanceled()) return;
			id = reader.readLine();

			notifyParent(State.finished);
		} catch (InterruptedException ignored) {
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally {
			try {
				close();
			} catch (IOException ignored) {
			}
		}
	}

	private boolean isCanceled() {
		if (interrupted() || pm.isCanceled()) {
			notifyParent(State.canceled);

			return true;
		}

		return false;
	}

	private void notifyParent(State state) {
		synchronized (sync) {
			this.state = state;
			sync.notify();
		}
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

	/**
	 * Tests the connection and returns the ergometer id in case of success.
	 *
	 * @return ergometer id in case of success
	 */
	public String test() {
		// start connection
		this.start();

		pm = new ProgressMonitor(owner, I18n.getString("connection_tester.testing_connection_to_ergometer"), "", 0, 100);
		pm.setMillisToPopup(0);
		pm.setMillisToDecideToPopup(0);
		
		try {
			boolean abort = false;

			// final state machine
			while (!abort) {
				switch (state) {

					case init:
						break;

					case openConnection:
						pm.setProgress(10);
						pm.setNote(I18n.getString("connection_tester.open_connection"));
						waitForChild(2000);
						if (state == State.openConnection) abort = true;
						break;

					case doReset:
						pm.setProgress(20);
						pm.setNote(I18n.getString("connection_tester.send_reset"));
						waitForChild(1000);
						if (state == State.doReset) abort = true;
						break;

					case wait:
						pm.setProgress(30);
			  		pm.setNote(I18n.getString("connection_tester.wait_for_reset_finished"));
						waitForChild(6000);
						if (state == State.wait) abort = true;
						break;

					case retrieveId:
						pm.setProgress(80);
						pm.setNote(I18n.getString("connection_tester.retrieving_ergometer_id"));
						waitForChild(5000);
						if (state == State.retrieveId) abort = true;
						break;

					case finished:
						pm.setProgress(100);
						pm.setNote(I18n.getString("connection_tester.connection_successful", id));
						abort = true;
						break;

					case canceled:
						abort = true;
						break;
				}
			}

			if (state != State.finished) {
				System.err.println("Connection to ergometer failed at state \"" + state + "\"");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			pm.close();
			if (isAlive()) {
				interrupt();
			}
		}

		return id;
	}

	private void waitForChild(int timeout) throws InterruptedException {
		synchronized (sync) {
			sync.wait(timeout);
		}
	}
}
