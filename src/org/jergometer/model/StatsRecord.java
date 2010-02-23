package org.jergometer.model;

import de.endrullis.xml.XMLElement;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class StatsRecord implements Cloneable {
	protected long pulseSum;
	protected long powerSum;
	protected long pedalRpmSum;
	protected int duration;
	protected int pulseCount;

	public StatsRecord(long pulseSum, long powerSum, long pedalRpmSum, int duration, int pulseCount) {
		this.pulseSum = pulseSum;
		this.powerSum = powerSum;
		this.pedalRpmSum = pedalRpmSum;
		this.duration = duration;
		this.pulseCount = pulseCount;
	}

	public StatsRecord(DataInputStream in) throws IOException {
		fromStream(in);
	}

	public StatsRecord(XMLElement xml) {
		fromXml(xml);
	}

	public void fromStream(DataInputStream in) throws IOException {
		pulseSum = in.readLong();
		powerSum = in.readLong();
		pedalRpmSum = in.readLong();
		duration = in.readInt();
		pulseCount = in.readInt();
	}

	public void toStream(DataOutputStream out) throws IOException {
		out.writeLong(pulseSum);
		out.writeLong(powerSum);
		out.writeLong(pedalRpmSum);
		out.writeInt(duration);
		out.writeInt(pulseCount);
	}

	public void fromXml(XMLElement xml) {
		pulseSum = Long.parseLong(xml.getAttribute("pulseSum"));
		powerSum = Long.parseLong(xml.getAttribute("powerSum"));
		pedalRpmSum = Long.parseLong(xml.getAttribute("pedalRpmSum"));
		duration = Integer.parseInt(xml.getAttribute("duration"));
		pulseCount = Integer.parseInt(xml.getAttribute("pulseCount"));
	}

	public XMLElement toXml(String elementName) {
		XMLElement xml = new XMLElement(elementName);

		xml.setAttribute("pulseSum", "" + pulseSum);
		xml.setAttribute("powerSum", "" + powerSum);
		xml.setAttribute("pedalRpmSum", "" + pedalRpmSum);
		xml.setAttribute("duration", "" + duration);
		xml.setAttribute("pulseCount", "" + pulseCount);

		return xml;
	}

	@Override
	protected StatsRecord clone() {
		return new StatsRecord(pulseSum, powerSum, pedalRpmSum, duration, pulseCount);
	}

	public double getAveragePulse() {
		return (double) pulseSum/pulseCount;
	}

	public double getAveragePower() {
		return (double) powerSum/duration;
	}

	public double getAveragePedalRPM() {
		return (double) pedalRpmSum/duration;
	}
	
	public long getPulseSum() {
		return pulseSum;
	}

	public long getPowerSum() {
		return powerSum;
	}

	public long getPedalRpmSum() {
		return pedalRpmSum;
	}

	public int getDuration() {
		return duration;
	}

	public int getPulseCount() {
		return pulseCount;
	}
}
