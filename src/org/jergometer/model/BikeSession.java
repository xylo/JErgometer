package org.jergometer.model;

import java.io.*;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Collection of the nessessary input data of a bike session.
 */
public class BikeSession {
	private Date startTime;
	private String programName;
	private int programDuration;
	private int duration;
	private ArrayList<MiniDataRecord> data;
	private MiniDataRecord sum = new MiniDataRecord(0, 0, 0);
	private int pulseCount = 0;
	private DataRecord lastRecord = new DataRecord(0,0,0,0,0,0,"",0);
	private File file;
	private boolean onlyMiniInfo = false;
	private boolean needToBeSaved = false;

	public BikeSession(String programName, int programDuration) {
		this.programName = programName.replaceAll("\\\\", "/");
		this.programDuration = programDuration;
		startTime = GregorianCalendar.getInstance().getTime();
		data = new ArrayList<MiniDataRecord>(programDuration);
	}

	/**
	 * Loads a bike session from a file.
	 *
	 * @param file bike session file
	 * @throws IOException
	 */
	public BikeSession(File file) throws IOException {
		this.file = file;
		loadFromFile(file);
	}

	private void loadFromFile(File file) throws IOException {
		fromStream(new DataInputStream(new FileInputStream(file)));
		onlyMiniInfo = false;
	}

	/**
	 * Creates a bike session from a "mini info".
	 *
	 * @param parentDir directory where the session files are stored
	 * @param startTime date and time of this session
	 * @param programName name of the program used for this session
	 * @param programDuration duration of the program
	 * @param duration duration of this session
	 * @param sumPulse pulse sum
	 * @param sumPower power sum
	 * @param sumPedalRpm pedal RPM sum
	 * @param pulseCount number of totalized pulse values
	 */
	public BikeSession(String parentDir, Date startTime, String programName, int programDuration,
										 int duration, int sumPulse, int sumPower, int sumPedalRpm, int pulseCount) {
		this(new File(getFileName(parentDir, startTime)), startTime, programName, programDuration, duration, sumPulse, sumPower, sumPedalRpm, pulseCount);
	}

	public static String getFileName(String parentDir, Date startTime) {
		return parentDir + String.format("%1$tY-%1$tm-%1$td_%1$tH-%1$tM.dat", startTime);
	}

	/**
	 * Creates a bike session from a "mini info".
	 *
	 * @param file session file used to load the complete session
	 * @param startTime date and time of this session
	 * @param programName name of the program used for this session
	 * @param programDuration duration of the program
	 * @param duration duration of this session
	 * @param sumPulse pulse sum
	 * @param sumPower power sum
	 * @param sumPedalRpm pedal RPM sum
	 * @param pulseCount number of totalized pulse values
	 */
	public BikeSession(File file, Date startTime, String programName, int programDuration,
										 int duration, int sumPulse, int sumPower, int sumPedalRpm, int pulseCount) {
		this.onlyMiniInfo = true;
		this.file = file;
		this.startTime = startTime;
		this.programName = programName;
		this.duration = duration;
		this.programDuration = programDuration;
		this.sum = new MiniDataRecord(sumPulse, sumPower, sumPedalRpm);
		this.pulseCount = pulseCount;
	}

	public void initialVirtualBikeSession() {
		data = new ArrayList<MiniDataRecord>();
		duration = programDuration;
	}

	public boolean update(DataRecord record) {
		if(!record.time.equals(lastRecord.time)) {
			// ergometer is not paused -> add the data record to my list
			data.add(new MiniDataRecord(record.pulse, record.realPower, record.pedalRpm));
			duration++;
			// update statistics
			if (data.size() < programDuration) {
				if (record.pulse > 0) {
					pulseCount++;
					sum.pulse += record.pulse;
				}
				sum.power += record.realPower;
				sum.pedalRpm += record.pedalRpm;
			}
			lastRecord = record;

			return true;
		}

		return false;
	}

	public void recalculateMiniInfo() {
		sum = new MiniDataRecord(0, 0, 0);
		pulseCount = 0;
		for (int i = 0; i < duration && i < programDuration; i++) {
			MiniDataRecord record = data.get(i);
			if (record.pulse > 0) {
				pulseCount++;
				sum.pulse += record.pulse;
			}
			sum.power += record.power;
			sum.pedalRpm += record.pedalRpm;
		}
		needToBeSaved = true;
	}

	public void save(String dir) throws IOException {
		new File(dir).mkdirs();

		String filename = getFileName(dir + "/", startTime);
		DataOutputStream out = new DataOutputStream(new FileOutputStream(filename));

		toStream(out);

		out.close();

		needToBeSaved = false;
	}

	private void toStream(DataOutputStream out) throws IOException {
		out.writeUTF("jergometer session");
		out.writeUTF("2");
		out.writeLong(startTime.getTime());
		out.writeUTF(programName);
		sum.toStream(out);
		out.writeInt(pulseCount);
		lastRecord.toStream(out);
		out.writeInt(programDuration);
		out.writeInt(data.size());
		for (MiniDataRecord miniDataRecord : data) {
			miniDataRecord.toStream(out);
		}
	}

	private void fromStream(DataInputStream in) throws IOException {
		String type = in.readUTF();
		if (type.equals("jergometer session")) {
			int format = Integer.parseInt(in.readUTF());
			startTime = new Time(in.readLong());
			programName = in.readUTF();
			sum = new MiniDataRecord(in);
			pulseCount = in.readInt();
			lastRecord = new DataRecord(in);
			if (format >= 2) {
				programDuration = in.readInt();
			}
			duration = in.readInt();
			data = new ArrayList<MiniDataRecord>(duration);
			for (int i = 0; i < duration; i++) {
				data.add(new MiniDataRecord(in));
			}
		}
		else {
			throw new IOException("File \"" + file.getName() + "\" is not a valid session file.");
		}
	}

	private void loadFull() throws IOException {
		if (onlyMiniInfo) {
			loadFromFile(file);
		}
	}



	public Date getStartTime() {
		return startTime;
	}

	public int getProgramDuration() {
		return programDuration;
	}

	public int getDuration() {
		return duration;
	}

	public ArrayList<MiniDataRecord> getData() throws IOException {
		loadFull();
		return data;
	}

	public MiniDataRecord getSum() {
		return sum;
	}

	public DataRecord getLastRecord() throws IOException {
		loadFull();
		return lastRecord;
	}

	public String getProgramName() {
		return programName;
	}

	public double getAveragePulse() {
		return (double) sum.getPulse()/pulseCount;
	}

	public double getAveragePower() {
		return (double) sum.getPower()/duration;
	}

	public double getAveragePedalRPM() {
		return (double) sum.getPedalRpm()/duration;
	}

	public int getPulseCount() {
		return pulseCount;
	}

	public void setProgramDuration(int programDuration) {
		this.programDuration = programDuration;
		needToBeSaved = true;
	}

	public void setProgramName(String programName) {
		this.programName = programName.replaceAll("\\\\", "/");
		needToBeSaved = true;
	}

	public boolean isNeedToBeSaved() {
		return needToBeSaved;
	}

	public boolean isCompleted() {
		return duration >= programDuration;
	}

	public File getFile() {
		return file;
	}
}
