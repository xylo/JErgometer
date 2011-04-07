package org.jergometer;

import de.endrullis.utils.StreamUtils;
import de.endrullis.xml.XMLDocument;
import de.endrullis.xml.XMLElement;
import de.endrullis.xml.XMLParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Manages settings of an user.
 */
public class UserSettings {
	private String userName;
	private String lastProgram;

	public UserSettings(String userName) {
		this.userName = userName;

		// load settings
		load();
	}

	private void load() {
		File settingsFile = new File(JergometerSettings.jergometerUsersDirName + "/" + userName + "/settings.xml");
		if (settingsFile.exists()) {
			// default settings
			XMLParser parser = new XMLParser();
			try {
				XMLDocument doc = parser.parse(StreamUtils.readXmlStream(new FileInputStream(settingsFile)));
				XMLElement root = doc.getRootElement();

				XMLElement users = root.getChildElement("programs");
				lastProgram = users.getAttribute("lastprogram");
			} catch (Exception ignored) {
			}
		}
	}

	public void save() {
		XMLElement root = new XMLElement("settings");
		root.setAttribute("version", "1");

		{
			XMLElement programs = new XMLElement("programs");
			root.addChildElement(programs);
			if (lastProgram != null) {
				programs.setAttribute("lastProgram", lastProgram);
			}
		}

		// write the document
		XMLDocument doc = new XMLDocument();
		doc.setRootElement(root);
		try {
			FileWriter writer = new FileWriter(JergometerSettings.jergometerUsersDirName + "/" + userName + "/settings.xml");
			writer.write(doc.toString());
			writer.close();
		} catch (IOException ignored) {
		}
	}

	// getters and setters
	public String getLastProgram() {
		return lastProgram;
	}

	public void setLastProgram(String lastProgram) {
		this.lastProgram = lastProgram;
	}
}
