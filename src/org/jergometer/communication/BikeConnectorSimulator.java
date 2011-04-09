package org.jergometer.communication;

import de.endrullis.utils.StreamUtils;
import org.jergometer.model.DataRecord;

import java.io.*;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class BikeConnectorSimulator implements BikeConnector {
	public static final String SIMULATOR_SESSION = "simulator.session";

	private BikeReader reader = null;
	private BikeWriter writer = null;
	private BikeReaderListener listener = null;
	private DataInputStream sessionInputStream = new DataInputStream(StreamUtils.getInputStream(SIMULATOR_SESSION));

	public BikeConnectorSimulator() throws IOException {
		reader = new BikeReader() {
			@Override
			public void close() {
			}
			@Override
			public void addBikeReaderListener(BikeReaderListener listener) {
				BikeConnectorSimulator.this.listener = listener;
			}
			@Override
			public void removeBikeReaderListener(BikeReaderListener listener) {
			}
			@Override
			public void start() {
			}
		};
		writer = new BikeWriter() {
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
			public void sendGetData(int power) throws IOException {
				try {
					listener.bikeData(new DataRecord(sessionInputStream));
				} catch (EOFException ignored) {}
			}
		};
	}

	@Override
	public BikeReader getReader() {
		return reader;
	}

	@Override
	public BikeWriter getWriter() {
		return writer;
	}

	@Override
	public void close() throws IOException {
	}
}
