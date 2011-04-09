package org.jergometer;

import de.endrullis.utils.BetterProperties2;
import de.endrullis.utils.StreamUtils;
import de.endrullis.xml.XMLDocument;
import de.endrullis.xml.XMLElement;
import de.endrullis.xml.XMLParser;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;

/**
 * Jergometer settings.
 */
@SuppressWarnings({"ResultOfMethodCallIgnored"})
public class JergometerSettings {

// static

	// directories
	public static final String jergometerDirName = System.getProperty("user.home") + "/.jergometer";
	public static final String jergometerUsersDirName = jergometerDirName + "/users";
	public static final String jergometerProgramsDirName = jergometerDirName + "/programs";
	public static final String jergometerExampleProgramsDirName = "programs";
	// files
	public static final File oldSettingsFile = new File(jergometerDirName + "/settings.xml");
	public static final File settingsFile = new File(jergometerDirName + "/settings.properties");

	// BetterProperties2 constants
	public static final BetterProperties2.Range INT           = BetterProperties2.INT;
	public static final BetterProperties2.Range INT_GT_0      = BetterProperties2.INT_GT_0;
	public static final BetterProperties2.Range DOUBLE        = BetterProperties2.DOUBLE;
	public static final BetterProperties2.Range DOUBLE_GT_0   = BetterProperties2.DOUBLE_GT_0;
	public static final BetterProperties2.Range DOUBLE_0_TO_1 = BetterProperties2.DOUBLE_0_TO_1;
	public static final BetterProperties2.Range BOOLEAN       = BetterProperties2.BOOLEAN;
	public static final BetterProperties2.Range STRING        = BetterProperties2.STRING;
	public static final BetterProperties2.Range SHORTCUT      = BetterProperties2.SHORTCUT;


// dynamic

	private BetterProperties2 properties = new BetterProperties2();
	private boolean checkForUpdatesOnStart = true;
	private Rectangle mainWindowBounds;
	private int mainWindowMaximizedState;
	private ArrayList<String> userNames = new ArrayList<String>();
	private String lastUserName;
	private String serialPort;
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

		defineProperties();

		// load settings
		load();
	}

	private void defineProperties() {
		// set default for the properties file
		properties.addEntry(new BetterProperties2.Comment("\n## General properties"));
		//properties.addEntry(new BetterProperties2.Comment(" Check for updates"));
		properties.addEntry(new BetterProperties2.Def("check_for_updates", BOOLEAN, "true"));
		properties.addEntry(new BetterProperties2.Def("last_user", STRING, null));
		properties.addEntry(new BetterProperties2.Def("comport", STRING, null));
		properties.addEntry(new BetterProperties2.Def("xml_editor", STRING, null));

		properties.addEntry(new BetterProperties2.Comment("\n## Window properties"));
		properties.addEntry(new BetterProperties2.Comment(" Position, width, and height of the main window"));
		properties.addEntry(new BetterProperties2.Def("main_window.x", INT_GT_0, "0"));
		properties.addEntry(new BetterProperties2.Def("main_window.y", INT_GT_0, "0"));
		properties.addEntry(new BetterProperties2.Def("main_window.width", INT_GT_0, "700"));
		properties.addEntry(new BetterProperties2.Def("main_window.height", INT_GT_0, "500"));
		properties.addEntry(new BetterProperties2.Def("main_window.maximized", INT_GT_0, "" + JFrame.MAXIMIZED_BOTH));
		/*
		properties.addEntry(new BetterProperties2.Comment(" Width of the symbols panel as part of the main window"));
		properties.addEntry(new BetterProperties2.Def("symbols_panel.width", DOUBLE_0_TO_1, "0.25"));
		properties.addEntry(new BetterProperties2.Comment(" Height of the tools panel as part of the main window"));
		properties.addEntry(new BetterProperties2.Def("tools_panel.height", DOUBLE_0_TO_1, "0.15"));
    */

		properties.addEntry(new BetterProperties2.Comment("\n## Shortcuts"));
		properties.addEntry(new BetterProperties2.Comment(" File menu"));
		properties.addEntry(new BetterProperties2.Def("shortcut.new", SHORTCUT, "control N"));
		properties.addEntry(new BetterProperties2.Def("shortcut.open", SHORTCUT, "control O"));
		properties.addEntry(new BetterProperties2.Def("shortcut.save", SHORTCUT, "control S"));
		properties.addEntry(new BetterProperties2.Def("shortcut.close", SHORTCUT, "control W"));
		properties.addEntry(new BetterProperties2.Def("shortcut.exit", SHORTCUT, ""));

	}

	public void load() {
		if (settingsFile.exists()) {
			try {
				properties.load(new FileReader(settingsFile));
			} catch (IOException e) {
				properties.loadDefaults();
				e.printStackTrace();
			}
		} else {
			properties.loadDefaults();
		}

		// extract variables
		checkForUpdatesOnStart = properties.getBoolean("check_for_updates");
		mainWindowBounds = new Rectangle(
				properties.getInt("main_window.x"),
				properties.getInt("main_window.y"),
				properties.getInt("main_window.width"),
				properties.getInt("main_window.height")
		);
		mainWindowMaximizedState = properties.getInt("main_window.maximized");

		lastUserName = properties.getString("last_user");
		serialPort = properties.getString("comport");
		xmlEditor = properties.getString("xml_editor");

		if (oldSettingsFile.exists()) {
			XMLParser parser = new XMLParser();
			try {
				XMLDocument doc = parser.parse(StreamUtils.readXmlStream(new FileInputStream(oldSettingsFile)));
				XMLElement root = doc.getRootElement();

				XMLElement update = root.getChildElement("update");
				if (update != null) checkForUpdatesOnStart = update.getAttribute("checkOnStart").equals("true");
				XMLElement users = root.getChildElement("users");
				if (users != null) lastUserName = users.getAttribute("lastUser");
				XMLElement comport = root.getChildElement("comport");
				if (comport != null) serialPort = comport.getAttribute("name");
				XMLElement xmlEditor = root.getChildElement("xmlEditor");
				if (xmlEditor != null) this.xmlEditor = xmlEditor.getAttribute("name");
			} catch (Exception ignored) {
			}
			save();
			oldSettingsFile.delete();
		}
	}

	/*
	*/

	public void save() {
		properties.setBoolean("check_for_updates", checkForUpdatesOnStart);
		properties.setInt("main_window.x", mainWindowBounds.x);
		properties.setInt("main_window.y", mainWindowBounds.y);
		properties.setInt("main_window.width", mainWindowBounds.width);
		properties.setInt("main_window.height", mainWindowBounds.height);
		properties.setInt("main_window.maximized", mainWindowMaximizedState);

		properties.setString("last_user", lastUserName);
		properties.setString("comport", serialPort);
		properties.setString("xml_editor", xmlEditor);

		settingsFile.getParentFile().mkdirs();
		try {
			properties.store(new FileOutputStream(settingsFile),
					" JErgometer properties\n" +
					" Default values will be automatically commented out.\n");
		} catch (Exception e) {
			e.printStackTrace();
		}

		/*
		// write the document
		XMLDocument doc = new XMLDocument();
		doc.setRootElement(root);
		try {
			FileWriter writer = new FileWriter(settingsFileName);
			writer.write(doc.toString());
			writer.close();
		} catch (IOException ignored) {
		}
		*/
	}

// getters and setters

	public boolean isCheckForUpdatesOnStart() {
		return checkForUpdatesOnStart;
	}

	public void setCheckForUpdatesOnStart(boolean checkForUpdatesOnStart) {
		this.checkForUpdatesOnStart = checkForUpdatesOnStart;
	}

	public Rectangle getMainWindowBounds() {
		return mainWindowBounds;
	}

	public void setMainWindowBounds(Rectangle mainWindowBounds) {
		this.mainWindowBounds = mainWindowBounds;
	}

	public int getMainWindowMaximizedState() {
		return mainWindowMaximizedState;
	}

	public void setMainWindowMaximizedState(int mainWindowMaximizedState) {
		this.mainWindowMaximizedState = mainWindowMaximizedState;
	}

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

	public String getSerialPort() {
		return serialPort;
	}

	public void setSerialPort(String serialPort) {
		this.serialPort = serialPort;
	}

	public String getXmlEditor() {
		return xmlEditor;
	}

	public void setXmlEditor(String xmlEditor) {
		this.xmlEditor = xmlEditor;
	}
}
