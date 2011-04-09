package org.jergometer;

import org.jergometer.communication.*;
import org.jergometer.model.DataRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import gnu.io.UnsupportedCommOperationException;

/**
 * Test console class of jergometer.
 */
public class JergometerTestConsole implements BikeReaderListener {

// static

	public static void main(String[] args) throws BikeException, UnsupportedCommOperationException {
		JergometerTestConsole jergometerTestConsole = new JergometerTestConsole();
	}

// dynamic  

	private KetterBikeConnector bikeConnector;

	public JergometerTestConsole() throws UnsupportedCommOperationException, BikeException {
		try {
			String osName = System.getProperty("os.name");

			if(osName.toLowerCase().startsWith("windows")) {
				bikeConnector = new KetterBikeConnector("COM1");
			} else {
				bikeConnector = new KetterBikeConnector("/dev/ttyUSB0");
//				bikeConnector = new KetterBikeConnector("/dev/ttyS0");
			}

			KettlerBikeWriter bikeWriter = bikeConnector.getWriter();
			KettlerBikeReader bikeReader = bikeConnector.getReader();
			bikeReader.addBikeReaderListener(this);
			bikeReader.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("* commands: ");
			System.out.println("*   0 - hello");
			System.out.println("*   1 - getData");
			System.out.println("*   2 - reset");
			System.out.println("*   5 - search for commands (brute force)");
			System.out.println("*   8 - raw data");
			System.out.println("*   9 - close connection");
			System.out.println("*  11 - print available input data");
			System.out.println("*  12 - print available input data as decimals");


			while(true) {
				System.out.print("* cmd? ");
				int sendCmd;
				try {
					sendCmd = Integer.parseInt(reader.readLine());
				} catch(NumberFormatException e) {
					sendCmd = -1;
				}

				String data;

				switch(sendCmd) {

					case 0:
						bikeWriter.sendHello();
						break;

					case 1:
						bikeWriter.sendGetData(80);
						break;

					case 2:
						bikeWriter.sendReset();
						break;

					case 5:
						while (true) {
							System.out.print("* enter staring command [AA]: ");
							data = reader.readLine();
							if (data.equals(""))  {
								data = "AA";
								break;
							} else
							if (data.length() == 2) {
								data = data.toUpperCase();
								break;
							}
						}

						char fst = data.charAt(0);
						char snd = data.charAt(1);

						for (;fst <= 'Z'; fst = (char) (fst+1)) {
							for (;snd <= 'Z'; snd = (char) (snd+1)) {
								data = "" + fst + snd;
								System.out.println("testing " + data);
								data += "\r\n";
								bikeWriter.writeRawBytes(data.getBytes());
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							snd = 'A';
						}

					case 8:
						System.out.print("* enter raw data: ");
						data = reader.readLine();
						data += "\r\n";
						bikeWriter.writeRawBytes(data.getBytes());
						break;

					case 9:
						bikeConnector.close();
						return;

					case 11:
						bikeReader.setPrintAvailable(KettlerBikeReader.PrintAvailable.characters);
						break;

					case 12:
						bikeReader.setPrintAvailable(KettlerBikeReader.PrintAvailable.decimals);
						break;

					default:
						System.out.println("* error: unknown command");
				}
			}


		} catch (IOException e) {
			System.err.println("* Cannot connect to bike.");
			e.printStackTrace();
		}
	}

	public void bikeAck() {
		System.err.println("* bikeAck received.");
	}

	public void bikeData(DataRecord data) {
		System.err.println("* bikeData received:\n" + data);
	}

	public void bikeError() {
		System.err.println("Bike: ERROR");
	}
}
