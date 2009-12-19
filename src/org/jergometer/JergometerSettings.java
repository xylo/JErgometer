package org.jergometer;

import de.endrullis.utils.StreamUtils;
import de.endrullis.xml.XMLDocument;
import de.endrullis.xml.XMLElement;
import de.endrullis.xml.XMLParser;

import java.io.*;
import java.util.ArrayList;

/**
 * Jergometer settings.
 */
public class JergometerSettings {

// static

	// directories
	public static final String jergometerDirName = System.getProperty("user.home") + "/.jergometer";
	public static final String jergometerUsersDirName = jergometerDirName + "/users";
	public static final String jergometerProgramsDirName = jergometerDirName + "/programs";
	public static final String jergometerExampleProgramsDirName = "programs";
	// files
	public static final String settingsFileName = jergometerDirName + "/settings.xml";


// dynamic

	private ArrayList<String> userNames = new ArrayList<String>();
	private String lastUserName;
	private String comPort;
	private String xmlEditor;

	public JergometerSettings() {
		// create all directories
		new File(jergometerDirName).mkdirs();
		new File(jergometerUsersDirName).mkdirs();
		File programsDir = new File(jergometerProgramsDirName);
		programsDir.mkdirs();

		// determine user list
		File[] userDirs = new File(jergometerUsersDirName).listFiles();
		for (File userDir : userDirs) {
			if (userDir.isDirectory() && !userDir.isHidden()) {
				userNames.add(userDir.getName());
			}
		}

		// if no programs in programDir -> copy example programs into programsDir
		if (programsDir.list().length == 0) {
			File exampleProgramsDir = new File(jergometerExampleProgramsDirName);

			try {
				StreamUtils.copyFileRecursivlyLinewise(exampleProgramsDir, programsDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// load settings
		load();
	}

	public void load() {
		File settingsFile = new File(settingsFileName);
		if (settingsFile.exists()) {
			XMLParser parser = new XMLParser();
			try {
				XMLDocument doc = parser.parse(StreamUtils.readXmlStream(new FileInputStream(settingsFile)));
				XMLElement root = doc.getRootElement();

				XMLElement users = root.getChildElement("users");
				if (users != null) lastUserName = users.getAttribute("lastuser");
				XMLElement comport = root.getChildElement("comport");
				if (comport != null) comPort = comport.getAttribute("name");
				XMLElement xmleditor = root.getChildElement("xmleditor");
				if (xmleditor != null) xmlEditor = xmleditor.getAttribute("name");
			} catch (Exception ignored) {
			}
		}
	}

	public void save() {
		XMLElement root = new XMLElement("settings");
		root.setAttribute("version", "1");

		{
			XMLElement users = new XMLElement("users");
			root.addChildElement(users);
			if (lastUserName != null) {
				users.setAttribute("lastuser", lastUserName);
			}
			XMLElement comport = new XMLElement("comport");
			root.addChildElement(comport);
			if (comPort != null) {
				comport.setAttribute("name", comPort);
			}
			XMLElement xmleditor = new XMLElement("xmleditor");
			root.addChildElement(xmleditor);
			if (xmlEditor != null) {
				xmleditor.setAttribute("name", xmlEditor);
			}
		}

		// write the document
		XMLDocument doc = new XMLDocument();
		doc.setRootElement(root);
		try {
			FileWriter writer = new FileWriter(settingsFileName);
			writer.write(doc.toString());
			writer.close();
		} catch (IOException ignored) {
		}
	}

// getters and setters

	public ArrayList<String> getUserNames() {
		return userNames;
	}

	public void setUserNames(ArrayList<String> userNames) {
		this.userNames = userNames;
	}

	public String getLastUserName() {
		return lastUserName;
	}

	public void setLastUserName(String lastUserName) {
		this.lastUserName = lastUserName;
	}

	public String getComPort() {
		return comPort;
	}

	public void setComPort(String comPort) {
		this.comPort = comPort;
	}

	public String getXmlEditor() {
		return xmlEditor;
	}

	public void setXmlEditor(String xmlEditor) {
		this.xmlEditor = xmlEditor;
	}
}
