package org.jergometer;

import org.jergometer.model.DataRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

/**
 * Console handler for JErgometer.
 *
 * @author Stefan Endrullis &lt;stefan@endrullis.de&gt;
 */
public class JergometerConsole extends Thread {
	private Jergometer jergometer;
	private Random random = new Random(0);
	private int pulse = 80, pedalRpm = 70, speed = 50, distance = 0, destPower = 100, energy = 0, realPower = 100, time = 0, lastTime = 0;

	public JergometerConsole(Jergometer jergometer) {
		this.jergometer = jergometer;
	}

	@Override
	public void run() {
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(System.in));

			String line;
			while ((line = r.readLine()) != null) {
				if (line.equals("train")) {
					// jergometer.getProgram().getProgramData().getDuration() +
					for (; time < 100; time++) {
						pulse     += random.nextInt(3)-1;
						pedalRpm  += random.nextInt(3)-1;
						speed     += random.nextInt(3)-1;
						distance  += 1;
						destPower += random.nextInt(3)-1;
						energy    += random.nextInt(2);
						realPower += random.nextInt(3)-1;
						jergometer.bikeData(new DataRecord(pulse, pedalRpm, speed, distance, destPower, energy, "" + time, realPower));
						sleep(10);
					}
					lastTime = time;
//				} else
//				if (line.equals("idle")) {
					for (int idleTime = 0; idleTime < 20; idleTime++) {
						pulse     += random.nextInt(2)-1;
						jergometer.bikeData(new DataRecord(pulse, 0, 0, distance, destPower, energy, "" + time, realPower));
						sleep(10);
					}
					for (; time < 150; time++) {
						pulse     += random.nextInt(3)-1;
						pedalRpm  += random.nextInt(3)-1;
						speed     += random.nextInt(3)-1;
						distance  += 1;
						destPower += random.nextInt(3)-1;
						energy    += random.nextInt(2);
						realPower += random.nextInt(3)-1;
						jergometer.bikeData(new DataRecord(pulse, pedalRpm, speed, distance, destPower, energy, "" + time, realPower));
						sleep(10);
					}
					lastTime = time;
					for (int idleTime = 0; idleTime < 20; idleTime++) {
						pulse     += random.nextInt(2)-1;
						jergometer.bikeData(new DataRecord(pulse, 0, 0, distance, destPower, energy, "" + time, realPower));
						sleep(10);
					}
				} else
				if (line.equals("quit")) {
					System.exit(0);
				}
			}
		} catch (IOException ignored) {
		} catch (InterruptedException ignored) {
		}

		System.out.println("quitting console");
	}
}
