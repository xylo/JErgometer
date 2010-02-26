package org.jergometer.model;

import java.io.*;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Collection of the necessary input data of a bike session.
 */
public class BikeSession {
	private Date startTime;
	private String programName;
	private int programDuration;
	private ArrayList<MiniDataRecord> data;
	private ArrayList<Integer> pulseAfterSession = new ArrayList<Integer>();
	private StatsRecord statsRegular = new StatsRecord(0, 0, 0, 0, 0);
	private StatsRecord statsTotal = statsRegular;
	private StatsRecord currentStats = statsRegular;
	private DataRecord lastRecordRegular = new DataRecord(0,0,0,0,0,0,"",0);
	private DataRecord lastRecordTotal = new DataRecord(0,0,0,0,0,0,"",0);
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
	 * @param statsRegular statistics in the regular training time
	 * @param statsTotal statistics in the real total training time
	 */
	public BikeSession(String parentDir, Date startTime, String programName, int programDuration,
	                   StatsRecord statsRegular, StatsRecord statsTotal) {
		this(new File(getFileName(parentDir, startTime)), startTime, programName, programDuration, statsRegular, statsTotal);
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
	 * @param statsRegular statistics in the regular training time
	 * @param statsTotal statistics in the real total training time
	 */
	public BikeSession(File file, Date startTime, String programName, int programDuration,
	                   StatsRecord statsRegular, StatsRecord statsTotal) {
		this.onlyMiniInfo = true;
		this.file = file;
		this.startTime = startTime;
		this.programName = programName;
		this.programDuration = programDuration;
		this.statsRegular = statsRegular;
		this.statsTotal = statsTotal;
	}

	public void initialVirtualBikeSession() {
		data = new ArrayList<MiniDataRecord>();
		statsTotal.duration = programDuration;
	}

	public boolean update(DataRecord record) {
		if(!record.time.equals(lastRecordTotal.time)) {
			// ergometer is not paused -> add the data record to my list
			data.add(new MiniDataRecord(record.pulse, record.realPower, record.pedalRpm));

			// update statistics
			currentStats.duration++;
			if (record.pulse > 0) {
				currentStats.pulseCount++;
				currentStats.pulseSum += record.pulse;
			}
			currentStats.powerSum += record.realPower;
			currentStats.pedalRpmSum += record.pedalRpm;

			if (currentStats == statsRegular) {
				lastRecordRegular = record;
			}
			lastRecordTotal = record;
			
			// switch from regular to total stats end of program reached
			if (currentStats.duration == programDuration) {
				statsTotal = statsRegular.clone();
				currentStats = statsTotal;
			}

			pulseAfterSession.clear();

			return true;
		} else {
			pulseAfterSession.add(record.pulse);

			return false;
		}
	}

	public void recalculateMiniInfo() {
		currentStats = null;
		int size = data.size();

		statsRegular = new StatsRecord(0, 0, 0, 0, 0);
		for (int i = 0; i < size && i < programDuration; i++) {
			MiniDataRecord record = data.get(i);

			statsRegular.duration++;
			if (record.pulse > 0) {
				statsRegular.pulseCount++;
				statsRegular.pulseSum += record.pulse;
			}
			statsRegular.powerSum += record.power;
			statsRegular.pedalRpmSum += record.pedalRpm;
		}

		statsTotal = statsRegular.clone();
		for (int i = statsRegular.duration; i < size; i++) {
			MiniDataRecord record = data.get(i);

			statsTotal.duration++;
			if (record.pulse > 0) {
				statsTotal.pulseCount++;
				statsTotal.pulseSum += record.pulse;
			}
			statsTotal.powerSum += record.power;
			statsTotal.pedalRpmSum += record.pedalRpm;
		}

		needToBeSaved = true;
	}

	public void save(String dir) throws IOException {
		new File(dir).mkdirs();

		file = new File(getFileName(dir + "/", startTime));
		DataOutputStream out = new DataOutputStream(new FileOutputStream(file));

		toStream(out);
		out.close();

		needToBeSaved = false;
	}

	private void toStream(DataOutputStream out) throws IOException {
		out.writeUTF("jergometer session");
		out.writeUTF("3");
		out.writeLong(startTime.getTime());
		out.writeUTF(programName);
		out.writeInt(programDuration);
		lastRecordRegular.toStream(out);
		lastRecordTotal.toStream(out);
		statsRegular.toStream(out);
		statsTotal.toStream(out);
		out.writeInt(data.size());
		for (MiniDataRecord miniDataRecord : data) {
			miniDataRecord.toStream(out);
		}
	}

	private void fromStream(DataInputStream in) throws IOException {
		String type = in.readUTF();
		if (type.equals("jergometer session")) {
			int format = Integer.parseInt(in.readUTF());

			if (format <= 2) {
				startTime = new Time(in.readLong());
				programName = in.readUTF();
				MiniDataRecord regularSum = new MiniDataRecord(in);
				int pulseCount = in.readInt();
				lastRecordRegular = new DataRecord(in);
				if (format >= 2) {
					programDuration = in.readInt();
				}
				int duration = in.readInt();
				data = new ArrayList<MiniDataRecord>(duration);
				for (int i = 0; i < duration; i++) {
					data.add(new MiniDataRecord(in));
				}
				statsRegular = new StatsRecord(regularSum.getPulse(), regularSum.getPower(), regularSum.getPedalRpm(), duration, pulseCount);
				statsTotal = statsRegular;
				currentStats = null;
			} else
			if (format == 3) {
				startTime = new Time(in.readLong());
				programName = in.readUTF();
				programDuration = in.readInt();
				lastRecordRegular = new DataRecord(in);
				lastRecordTotal = new DataRecord(in);
				statsRegular = new StatsRecord(in);
				statsTotal = new StatsRecord(in);
				int size = in.readInt();
				data = new ArrayList<MiniDataRecord>(size);
				for (int i = 0; i < size; i++) {
					data.add(new MiniDataRecord(in));
				}
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
		return statsTotal.duration;
	}

	public ArrayList<MiniDataRecord> getData() throws IOException {
		loadFull();
		return data;
	}

	public StatsRecord getStatsRegular() {
		return statsRegular;
	}

	public StatsRecord getStatsTotal() {
		return statsTotal;
	}

	public DataRecord getLastRecordRegular() throws IOException {
		loadFull();
		return lastRecordRegular;
	}

	public String getProgramName() {
		return programName;
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
		return statsTotal.duration >= programDuration;
	}

	public File getFile() {
		return file;
	}

	public ArrayList<Integer> getPulseAfterSession() {
		return pulseAfterSession;
	}
	
	public int getDurationPulse() {
		return statsTotal.duration + (pulseAfterSession.size() + 1) / 2;
	}
}
