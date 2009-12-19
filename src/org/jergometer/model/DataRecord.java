package org.jergometer.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Bike data record.
 */
public class DataRecord {
	protected int pulse;
	protected int pedalRpm;
	protected int speed;
	protected int distance;
	protected int destPower;
	protected int energy;
	protected String time;
	protected int realPower;

	public DataRecord(int pulse, int pedalRpm, int speed, int distance, int destPower, int energy, String time, int realPower) {
		this.pulse = pulse;
		this.pedalRpm = pedalRpm;
		this.speed = speed;
		this.distance = distance;
		this.destPower = destPower;
		this.energy = energy;
		this.time = time;
		this.realPower = realPower;
	}

	public DataRecord(String dataString) {
		String[] parts = dataString.split("\\t");
		pulse = Integer.parseInt(parts[0]);
		pedalRpm = Integer.parseInt(parts[1]);
		speed = Integer.parseInt(parts[2]);
		distance = Integer.parseInt(parts[3]);
		destPower = Integer.parseInt(parts[4]);
		energy = Integer.parseInt(parts[5]);
		time = parts[6];
		realPower = Integer.parseInt(parts[7]);
	}

	public DataRecord(DataInputStream in) throws IOException {
		fromStream(in);
	}

	public String toString() {
		return new StringBuilder().append("pulse: ").append(pulse).append(",\tpedal rpm: ").append(pedalRpm).append(",\tspeed: ").append(speed).append(",\tdistance: ").append(distance).append(",\tdest. power: ").append(destPower).append(",\tenergy: ").append(energy).append(",\ttime: ").append(time).append(",\treal power: ").append(realPower).toString();
	}

	public void toStream(DataOutputStream out) throws IOException {
		out.writeInt(pulse);
		out.writeInt(pedalRpm);
		out.writeInt(speed);
		out.writeInt(distance);
		out.writeInt(destPower);
		out.writeInt(energy);
		out.writeUTF(time);
		out.writeInt(realPower);
	}

	public void fromStream(DataInputStream in) throws IOException {
		pulse = in.readInt();
		pedalRpm = in.readInt();
		speed = in.readInt();
		distance = in.readInt();
		destPower = in.readInt();
		energy = in.readInt();
		time = in.readUTF();
		realPower = in.readInt();
	}


	// getters and setters

	public int getPulse() {
		return pulse;
	}

	public int getPedalRpm() {
		return pedalRpm;
	}

	public int getSpeed() {
		return speed;
	}

	public int getDistance() {
		return distance;
	}

	public int getDestPower() {
		return destPower;
	}

	public int getEnergy() {
		return energy;
	}

	public String getTime() {
		return time;
	}

	public int getRealPower() {
		return realPower;
	}
}
