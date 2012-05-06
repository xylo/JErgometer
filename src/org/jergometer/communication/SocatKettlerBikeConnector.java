package org.jergometer.communication;

import gnu.io.UnsupportedCommOperationException;
import org.jergometer.model.DataRecord;

import javax.swing.*;
import java.io.IOException;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class SocatKettlerBikeConnector implements BikeConnector {

// dynamic

	private Process process;
	private KettlerBikeReader reader = null;
	private KettlerBikeWriter writer = null;
	private int power = 0;

	@Override
	public void connect(String serialName, BikeListener listener) throws BikeException, UnsupportedCommOperationException, IOException {
		process = Runtime.getRuntime().exec(new String[]{
			"/usr/bin/socat", "-", serialName + ",b9600,min=1,time=1,brkint=0,icrnl=0,ixoff=1,imaxbel=0,opost=0,isig=0,icanon=0,iexten=0,echo=0,echoe=0,echok=0,crnl"
		});
		writer = new KettlerBikeWriter(false, process.getOutputStream());
		reader = new KettlerBikeReader(process.getInputStream());
		reader.addBikeReaderListener(listener);
		reader.start();
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
		reader.close();
		process.destroy();
	}

	@Override
	public String getName() {
		return  "Kettler-socat";
	}

	@Override
	public String toString() {
		return getName();
	}
}
