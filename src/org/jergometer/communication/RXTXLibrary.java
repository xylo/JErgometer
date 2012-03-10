package org.jergometer.communication;

import gnu.io.CommDriver;
import gnu.io.CommPortIdentifier;

import java.util.ArrayList;
import java.util.Enumeration;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class RXTXLibrary {
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

	public static void init() {}

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
}
