package org.jergometer.control;

import org.jergometer.model.BikeSession;
import org.jergometer.model.DataRecord;
import org.jergometer.model.MiniDataRecord;

import java.util.ArrayList;
import java.io.IOException;

/**
 * Sub program of bike program.
 *
 * @see org.jergometer.control.SubProgram
 */
abstract public class SubProgram {
	protected BikeSession session;
	protected int power;

	public SubProgram(BikeSession session, int power) {
		this.session = session;
		this.power = power;
	}

	public abstract void update(DataRecord dataRecord);

	public int getPower() {
		return power;
	}

	public void setPower(int power) {
		this.power = power;

		if(this.power < 25)  this.power = 25;
		if(this.power > 400) this.power = 400;
	}


// sub classes
	public static class Power extends SubProgram {
		public Power(BikeSession session, int power) {
			super(session, power);
		}

		public void update(DataRecord dataRecord) {
		}
	}


	public static class Pulse extends SubProgram {
		private int destPulse;
		private int last10 = 0, last20 = 0;
		private int last10Count = 0, last20Count = 0;
		private int switcher = 0;

		public Pulse(BikeSession session, int power, int pulse) {
			super(session, power);
			destPulse = pulse;

			ArrayList<MiniDataRecord> data = null;
			try {
				data = session.getData();
			} catch (IOException e) {
				// this should never happen
				data = new ArrayList<MiniDataRecord>();
			}

			// calculate the pulse sum vom -20 to -10
			int value;
			for(int i = Math.max(0, data.size() - 20); i < data.size() - 10; i++) {
				value = data.get(i).getPulse();
				if (value > 0) {
					last20 += value;
					last20Count++;
				}
			}

			// calculate the pulse sum vom -10 to 0
			for(int i = Math.max(0, data.size() - 10); i < data.size(); i++) {
				value = data.get(i).getPulse();
				if (value > 0) {
					last10 += value;
					last10Count++;
				}
			}
		}

		public void update(DataRecord dataRecord) {
			ArrayList<MiniDataRecord> data = null;
			try {
				data = session.getData();
			} catch (IOException e) {
				throw new RuntimeException("This should never happen!");
			}
			int value;

			if (data.size() >= 21) {
				value = data.get(data.size() - 21).getPulse();
				if (value > 0) {
					last20 -= value;
					last20Count--;
				}
			}
			if (data.size() >= 11) {
				value = data.get(data.size() - 11).getPulse();
				if (value > 0) {
					last20 += value;
					last20Count++;
					last10 -= value;
					last10Count--;
				}
			}
			if (data.size() >= 1) {
				value = data.get(data.size() - 1).getPulse();
				if (value > 0) {
					last10 += value;
					last10Count++;
				}
			}

			if (last10Count >= 5 && last20Count >= 5) {
				switcher += destPulse - ((0.5 + (double) (last10/last10Count - last20/last20Count)*10/3 + dataRecord.getPulse()));

				if (switcher >= 100) {
					setPower(power + 5);
					switcher = 0;
				}

				if (switcher <= -100) {
					setPower(power - 5);
					switcher = 0;
				}
			}
		}
	}
}
