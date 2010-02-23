package org.jergometer.model;

import de.endrullis.utils.StreamUtils;
import de.endrullis.xml.XMLDocument;
import de.endrullis.xml.XMLElement;
import de.endrullis.xml.XMLParser;
import org.jergometer.JergometerSettings;
import org.jergometer.control.BikeProgram;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

/**
 * User data.
 */
public class UserData {
	private String userName;
	private ArrayList<BikeSession> sessions = new ArrayList<BikeSession>();
	private ProgressMonitor progressMonitor;

	public UserData(String userName, BikeProgramTree programTree) {
		this.userName = userName;

		// load sessions
		load(programTree);
	}

	public void generate() throws IOException {
		sessions.clear();

		File[] sessionFiles = getSessionFiles();
		Arrays.sort(sessionFiles);
		if (progressMonitor != null)
			progressMonitor.setMaximum(sessionFiles.length);

		int nr = 0;
		for (File file : sessionFiles) {
			sessions.add(new BikeSession(file));
			nr++;
			if (progressMonitor != null) {
				progressMonitor.setProgress(nr);
				if (progressMonitor.isCanceled()) {
					break;
				}
			}
		}
	}

	public void load(BikeProgramTree programTree) {
		sessions.clear();

		String sessionsDirName = JergometerSettings.jergometerUsersDirName + "/" + userName + "/sessions/";
		File sessionsFile = new File(JergometerSettings.jergometerUsersDirName + "/" + userName + "/sessions.xml");
		if (sessionsFile.exists()) {
			XMLParser parser = new XMLParser();
			try {
				XMLDocument doc = parser.parse(StreamUtils.readXmlStream(new FileInputStream(sessionsFile)));
				XMLElement root = doc.getRootElement();

				XMLElement sessionsXml = root.getChildElement("sessions");

				for (XMLElement sessionXml : sessionsXml.getChildElements()) {
					long time = Long.parseLong(sessionXml.getAttribute("date"));
					String programName = sessionXml.getAttribute("programName");
					String programdurationString = sessionXml.getAttribute("programduration");
					int programduration;
					if (programdurationString != null) {
						programduration = Integer.parseInt(programdurationString);
					} else {
						programduration = -1;
					}
					if (programduration == -1) {
						// handle old session format 1 -> try to determine the program duration
						BikeProgram program = programTree.getProgram(programName);
						if (program != null) {
							programduration = programTree.getProgram(programName).getProgramData().getDuration();
						}
					}

					StatsRecord statsRegular, statsTotal;
					XMLElement statsRegularXml = sessionXml.getChildElement("statsRegular");
					if (statsRegularXml != null) {
						statsRegular = new StatsRecord(statsRegularXml);
					} else {
						int duration = Integer.parseInt(sessionXml.getAttribute("duration"));
						int sumPulse = Integer.parseInt(sessionXml.getAttribute("sumPulse"));
						int sumPower = Integer.parseInt(sessionXml.getAttribute("sumPower"));
						int sumPedalRpm = Integer.parseInt(sessionXml.getAttribute("sumPedalRpm"));
						int pulseCount = Integer.parseInt(sessionXml.getAttribute("pulseCount"));
						statsRegular = new StatsRecord(sumPulse, sumPower, sumPedalRpm, duration, pulseCount);
					}

					XMLElement statsTotalXml = sessionXml.getChildElement("statsTotal");
					if (statsTotalXml != null) {
						statsTotal = new StatsRecord(statsTotalXml);
					} else {
						statsTotal = statsRegular;
					}

					sessions.add(new BikeSession(sessionsDirName, new Date(time), programName, programduration,
							statsRegular, statsTotal));
				}

			} catch (Exception ignored) {
			}
		} else {
			try {
				generate();
				save();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private File[] getSessionFiles() {
		File sessionsDir = new File(JergometerSettings.jergometerUsersDirName + "/" + userName + "/sessions");
		if (sessionsDir.exists() && sessionsDir.isDirectory()) {
			File[] files = sessionsDir.listFiles(new FileFilter(){
				public boolean accept(File pathname) {
					return pathname.getName().endsWith(".dat");
				}
			});
			return files;
		}
		else {
			return new File[]{};
		}
	}

	public void save() {
		XMLElement root = new XMLElement("sessions");
		root.setAttribute("version", "3");

		{
			XMLElement sessionsXml = new XMLElement("sessions");
			root.addChildElement(sessionsXml);

			for (BikeSession session : sessions) {
				XMLElement sessionXml = new XMLElement("session");
				sessionXml.setAttribute("date", "" + session.getStartTime().getTime());
				sessionXml.setAttribute("programName", "" + session.getProgramName());
				sessionXml.setAttribute("programDuration", "" + session.getProgramDuration());
				sessionXml.addChildElement(session.getStatsRegular().toXml("statsRegular"));
				sessionXml.addChildElement(session.getStatsTotal().toXml("statsTotal"));

				sessionsXml.addChildElement(sessionXml);
			}
			/*
			if (lastProgram != null) {
				programs.setAttribute("lastProgram", lastProgram);
			}
			*/
		}

		// write the document
		XMLDocument doc = new XMLDocument();
		doc.setRootElement(root);
		try {
			FileWriter writer = new FileWriter(JergometerSettings.jergometerUsersDirName + "/" + userName + "/sessions.xml");
			writer.write(doc.toString());
			writer.close();
		} catch (IOException ignored) {
		}
	}


	// getters and setters

	public String getUserName() {
		return userName;
	}

	public ArrayList<BikeSession> getSessions() {
		return sessions;
	}

	public ProgressMonitor getProgressMonitor() {
		return progressMonitor;
	}

	public void setProgressMonitor(ProgressMonitor progressMonitor) {
		this.progressMonitor = progressMonitor;
	}
}
