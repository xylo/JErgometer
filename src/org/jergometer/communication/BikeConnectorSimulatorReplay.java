package org.jergometer.communication;

import de.endrullis.utils.StreamUtils;
import gnu.io.UnsupportedCommOperationException;
import org.jergometer.model.DataRecord;

import java.io.*;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class BikeConnectorSimulatorReplay implements BikeConnector {
	public static final String SIMULATOR_SESSION = "simulator.session";

	private BikeListener listener = null;
	private DataInputStream sessionInputStream;

	@Override
	public void connect(String serialName, BikeListener listener) throws BikeException, UnsupportedCommOperationException, IOException {
		this.listener = listener;
		sessionInputStream = new DataInputStream(StreamUtils.getInputStream(SIMULATOR_SESSION));
	}

	@Override
	public void sendHello() throws IOException {
		listener.bikeAck();
	}
	@Override
	public void sendReset() throws IOException {
		listener.bikeAck();
	}
	@Override
	public void sendGetId() throws IOException {
	}
	@Override
	public void sendGetData() throws IOException {
		try {
			listener.bikeData(new DataRecord(sessionInputStream));
		} catch (EOFException ignored) {}
	}
	@Override
	public void sendSetPower(int power) throws IOException {
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public String getName() {
		return "simulator-replay";
	}

	@Override
	public String toString() {
		return getName();
	}
}
