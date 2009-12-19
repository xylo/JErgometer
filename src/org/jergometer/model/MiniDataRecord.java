package org.jergometer.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Data record for the important input data.
 */
public class MiniDataRecord {
	protected int pulse;
	protected int power;
	protected int pedalRpm;

	public MiniDataRecord(int pulse, int power, int pedalRpm) {
		this.pulse = pulse;
		this.power = power;
		this.pedalRpm = pedalRpm;
	}

	public MiniDataRecord(DataInputStream in) throws IOException {
		fromStream(in);
	}

	public void toStream(DataOutputStream out) throws IOException {
		out.writeInt(pulse);
		out.writeInt(power);
		out.writeInt(pedalRpm);
	}

	public void fromStream(DataInputStream in) throws IOException {
		pulse = in.readInt();
		power = in.readInt();
		pedalRpm = in.readInt();
	}

	// getters
	public int getPulse() {
		return pulse;
	}

	public int getPower() {
		return power;
	}

	public int getPedalRpm() {
		return pedalRpm;
	}
}
