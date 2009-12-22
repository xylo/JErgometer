package org.jergometer;

import de.endrullis.utils.ParamsExt;
import de.endrullis.utils.ProgramUpdater;
import de.endrullis.utils.ShellPrintStream;
import de.endrullis.utils.StreamUtils;
import org.jergometer.communication.*;
import org.jergometer.control.BikeProgram;
import org.jergometer.diagram.BikeProgramVisualizer;
import org.jergometer.diagram.BikeSessionVisualizer;
import org.jergometer.diagram.DiagramVisualizer;
import org.jergometer.diagram.ProgressionVisualizer;
import org.jergometer.gui.ChooseNewProgramDialog;
import org.jergometer.gui.Diagram;
import org.jergometer.gui.MainWindow;
import org.jergometer.model.*;
import org.jergometer.translation.I18n;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import gnu.io.UnsupportedCommOperationException;
import gnu.io.PortInUseException;

/**
 * Main class of JErgometer.
 */
public class Jergometer implements BikeReaderListener, ActionListener, WindowListener {

// static

	public static String version = "*Bleeding Edge*";
	public static boolean devVersion = true;
	static {
		try {
			version = StreamUtils.readFile("version.txt");
			devVersion = false;
		} catch (IOException ignored) {}
	}

	public enum State { notConnected, connected, reset, hello }
	public enum SessionsVis { average, progression }

	private final ProgramUpdater updater = new ProgramUpdater("JErgometer update", "http://common.jergometer.org/update/");

	public static void main(String[] args) {
		ShellPrintStream.replaceSystemOut(args);

		ParamsExt.Option[] options = new ParamsExt.Option[]{
				new ParamsExt.Option("help",     'h', I18n.getString("args.show_help")),
				new ParamsExt.Option("nogui",    'G', I18n.getString("args.disable_gui")),
				new ParamsExt.Option("color",    'c', I18n.getString("args.color", bold("on"), bold("off"))),
				new ParamsExt.Option("version",  'v', I18n.getString("args.show_version"))
		};

		// parse parameters
		ParamsExt params = new ParamsExt(options, args);
		params.setSyntax(bold("java test.endrullis.jergometer.Jergometer") + " [" +
				underline(I18n.getString("args.options")) + "]"
		);

		// begin of the real program

		if(params.isOptionAvailable("help")) {
			params.printHelp();
			System.exit(0);
		}

		if(params.isOptionAvailable("version")) {
			System.out.println("JErgometer " + version);
			System.exit(0);
		}

		new Jergometer(true);
	}

	private static String bold(String text) {
		return ShellPrintStream.out.style(text, ShellPrintStream.STYLE_BOLD);
	}

	private static String underline(String text) {
		return ShellPrintStream.out.style(text, ShellPrintStream.STYLE_UNDERLINE);
	}


// dynamic

	private JergometerSettings jergometerSettings;
	private UserSettings userSettings = null;
	private UserData userData = null;
	private State state = State.notConnected;
	private BikeConnector bikeConnector;
	private MainWindow mainWindow;
	private boolean gui;
	private Timer communicationTimer = null;
	private boolean recording = false;
	private int power;
	private BikeProgramTree programTree;
	private BikeProgram program;
	private DiagramVisualizer diagramVisualizer = new BikeProgramVisualizer(null);
	private BikeSessionFilter sessionFilter = new BikeSessionFilter();
	private SessionsVis sessionsVis = SessionsVis.average;
	private ArrayList<BikeSession> selectedSessions = new ArrayList<BikeSession>();

	/**
	 * Creates an JErgometer instance.
	 *
	 * @param gui true if you want to have a gui
	 */
	public Jergometer(boolean gui) {
		this.gui = gui;

		jergometerSettings = new JergometerSettings();

		// search for updates in the background
		if (!devVersion) {
			new Thread(){
				public void run() {
					checkForUpdates(true);
				}
			}.start();
		}

		programTree = new BikeProgramTree();

		mainWindow = new MainWindow(I18n.getString("main_window.title", version), this);
		mainWindow.getProgramTree().setModel(programTree);
		// maximize the main window
		mainWindow.pack();
		mainWindow.setExtendedState(mainWindow.getExtendedState()|JFrame.MAXIMIZED_BOTH);
		mainWindow.addWindowListener(this);
		mainWindow.setVisible(true);

		setShowOnlyCompletedSessions(true);
		setSessionsVis(SessionsVis.progression);

		switchToUser(jergometerSettings.getLastUserName());

		/*
		int u = 130;
		int v = 80;
		int w = 100;
		for(int i = 0; i < 3600; i++) {
			u += Math.random() *3 - 1;
			if(u < 30) u = 30;
			if(u > 210) u = 210;
			mainWindow.getDiagram().addValue("pulse", i, u);
			v += Math.random() *3 - 1;
			if(v < 30) v = 30;
			if(v > 210) v = 210;
			mainWindow.getDiagram().addValue("pedalRPM", i, v);
			w += Math.random() *3 - 1;
			if(w < 30) w = 30;
			if(w > 210) w = 210;
			mainWindow.getDiagram().addValue("power", i, w);
		}
		*/
	}

	/**
	 * Checks for JErgometer updates.
	 *
	 * @param start true if you want to check for updates without notifications in case there are no new versions
	 */
	private void checkForUpdates(boolean start) {
		// check for new version
		if (updater.isNewVersionAvailable()) {
			// ask user if (s)he wants to update
			if (JOptionPane.showConfirmDialog(mainWindow, "A new version of JErgometer is available. Do you want to update?",
					"JErgometer - Updater", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {

				// perform update
				if (updater.performUpdate(true)) {
					// restart the editor
					System.exit(255);
				}
			}
		} else {
			if (!start) {
				JOptionPane.showMessageDialog(mainWindow, "JErgometer is up-to-date.", "JErgometer - Updater", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	/**
	 * Connects to the serial port.
	 */
	private void connectToSerialPort() throws BikeException, UnsupportedCommOperationException, IOException, UnconfiguredComPortException {
		String comPort = jergometerSettings.getComPort();
		if (comPort == null) {
			throw new UnconfiguredComPortException();
		}
		bikeConnector = new BikeConnectorCOM(comPort);

		BikeReader bikeReader = bikeConnector.getReader();
		bikeReader.addBikeReaderListener(this);
		bikeReader.start();
	}

	/**
	 * Saves all settings and sessions.
	 */
	private void save() {
		stopRecording();
		jergometerSettings.save();
		userSettings.save();
	}


	/**
	 * Adds a new user.
	 *
	 * @param userName user name
	 */
	public void newUser(String userName) {
		if (!jergometerSettings.getUserNames().contains(userName)) {
			jergometerSettings.getUserNames().add(userName);
			switchToUser(userName);
		}
	}

	/**
	 * Sets the user to the given one.
	 *
	 * @param userName user name
	 */
	public void switchToUser(String userName) {
		jergometerSettings.setLastUserName(userName);
		userSettings = new UserSettings(jergometerSettings.getLastUserName());
		userData = new UserData(jergometerSettings.getLastUserName(), programTree);
		String[] userNames = jergometerSettings.getUserNames().toArray(new String[jergometerSettings.getUserNames().size()]);
		mainWindow.setUserList(userNames, jergometerSettings.getLastUserName());
		filterSessions();
	}

	private void filterSessions() {
		if (!gui || userData == null) return;

		ArrayList<BikeSession> filteredList = new ArrayList<BikeSession>();
		for (BikeSession bikeSession : userData.getSessions()) {
			if (sessionFilter.match(bikeSession)) {
				filteredList.add(bikeSession);
			}
		}
		mainWindow.getSessionTable().setModel(new SessionTableModel(filteredList));
	}

	/**
	 * Forces a reparse of the sessions files of the current user.
	 */
	public void reparseUserData() {
		try {
			ProgressMonitor pm = new ProgressMonitor(mainWindow, I18n.getString("msg.loading_user_sessions"), null, 0, 0);
//			pm.setMillisToDecideToPopup(200);
			pm.setMillisToPopup(0);
			userData.setProgressMonitor(pm);

			// parse all session files
			userData.generate();

			// check if there are old/unknown bike programs in the sessions
			// and put them into unknownBikeProgram2Sessions
			HashMap<String, ArrayList<BikeSession>> unknownBikeProgram2Sessions = new HashMap<String, ArrayList<BikeSession>>();
			for (BikeSession bikeSession : userData.getSessions()) {
				BikeProgram bikeProgram = programTree.getProgram(bikeSession.getProgramName());
				if (bikeProgram == null) {
					ArrayList<BikeSession> affectedBikeSessions = unknownBikeProgram2Sessions.get(bikeSession.getProgramName());
					if (affectedBikeSessions == null) {
						affectedBikeSessions = new ArrayList<BikeSession>(1);
						unknownBikeProgram2Sessions.put(bikeSession.getProgramName(), affectedBikeSessions);
					}
					affectedBikeSessions.add(bikeSession);
				}
			}
			if (!unknownBikeProgram2Sessions.isEmpty()) {
				ChooseNewProgramDialog dialog = new ChooseNewProgramDialog(mainWindow, programTree);
				for (String oldProgramName : unknownBikeProgram2Sessions.keySet()) {
					dialog.openDialog(oldProgramName);
					ChooseNewProgramDialog.Result result = dialog.getResult();
					switch (result) {
						case assign:
							BikeProgram bikeProgram = dialog.getSelectedBikeProgram();
							if (bikeProgram != null) {
								String newProgramName = bikeProgram.getProgramName();
								// save corresponding sessions with new program name
								for (BikeSession bikeSession : unknownBikeProgram2Sessions.get(oldProgramName)) {
									bikeSession.setProgramName(newProgramName);
								}
							}
							break;
						case skip:
							continue;
						case abort:
							break;
					}
				}
			}

			// check session consistency, correct them and save them if needed
			for (BikeSession bikeSession : userData.getSessions()) {
				BikeProgram bikeProgram = programTree.getProgram(bikeSession.getProgramName());
				if (bikeProgram.getProgramData().getDuration() != bikeSession.getProgramDuration()) {
					bikeSession.setProgramDuration(bikeProgram.getProgramData().getDuration());
					bikeSession.recalculateMiniInfo();
				}
				if (bikeSession.isNeedToBeSaved()) {
					bikeSession.save(getCurrentSessionDir());
				}
			}

			// save the sessions.xml
			userData.save();
			pm.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] userNames = jergometerSettings.getUserNames().toArray(new String[jergometerSettings.getUserNames().size()]);
		mainWindow.setUserList(userNames, jergometerSettings.getLastUserName());
		filterSessions();
	}

	/**
	 * Starts the recording of a bike session.
	 */
	public void startRecording() {
		if (!recording) {
			// clear diagram and draw the bike program
			selectBikeProgram(program);

			Diagram diagram = mainWindow.getDiagram();
//			diagram.clearGraphs();
			diagram.addGraph("pulse", "Pulse", new Color(128,0,0), Diagram.Side.left);
			diagram.addGraph("pedalRPM", "Pedal RPM", new Color(0,128,0), Diagram.Side.left);
			diagram.addGraph("power", "Power", new Color(0,0,128), Diagram.Side.left);

			try {
				connectToSerialPort();
			} catch (BikeException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(mainWindow, e.getMessage(), I18n.getString("error_dialog.title"), JOptionPane.ERROR_MESSAGE);
				return;
			} catch (UnconfiguredComPortException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(mainWindow, I18n.getString("msg.configure_comport_first"));
				mainWindow.openSettingsWindow();
			} catch (UnsatisfiedLinkError e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(mainWindow, I18n.getString("msg.unsatisfied_link_error"), I18n.getString("error_dialog.title"), JOptionPane.ERROR_MESSAGE);
				return;
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(mainWindow, I18n.getString("msg.connection_failed"), I18n.getString("error_dialog.title"), JOptionPane.ERROR_MESSAGE);
				return;
			}

			if (program == null) {
				JOptionPane.showMessageDialog(mainWindow, I18n.getString("msg.choose_a_program"));
				return;
			}

			mainWindow.setRecordState(true);
			recording = true;

			program.newSession();

			communicationTimer = new Timer(500, this);
			communicationTimer.start();
		}
	}

	/**
	 * Stops the recording and saves the session to a file.
	 */
	public void stopRecording() {
		if (recording) {
			if (communicationTimer != null) {
				communicationTimer.stop();
			}
			try {
				if (bikeConnector != null) {
					bikeConnector.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if (program != null && program.getSession() != null) {
				try {
					// save session file
					program.getSession().save(getCurrentSessionDir());
					// add session to session table
					userData.getSessions().add(program.getSession());
					userData.save();
					// update session table
					filterSessions();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			mainWindow.setRecordState(false);
			recording = false;
		}
	}

	private String getCurrentSessionDir() {
		return JergometerSettings.jergometerUsersDirName + "/" + jergometerSettings.getLastUserName() + "/sessions";
	}

	public void selectBikeProgram(BikeProgram bikeProgram) {
		selectedSessions.clear();
		program = bikeProgram;
		sessionFilter.setProgramFilter(bikeProgram);
		diagramVisualizer.stopVisualization();
		filterSessions();
		visualizeBikeProgram(bikeProgram);
	}

	public void selectBikeProgramDirectory(BikeProgramDir bikeProgramDir) {
		sessionFilter.setProgramDirFilter(bikeProgramDir);
		filterSessions();
	}

	public void selectBikeProgramRoot() {
		sessionFilter.deactivateFilter();
		filterSessions();
	}

	private void visualizeBikeProgram(BikeProgram bikeProgram) {
		BikeProgramVisualizer bikeProgramVisualizer = new BikeProgramVisualizer(mainWindow.getDiagram());
		diagramVisualizer = bikeProgramVisualizer;
		bikeProgramVisualizer.visualize(bikeProgram);
	}

	public void selectBikeSession(BikeSession bikeSession) {
		selectedSessions.clear();
		selectedSessions.add(bikeSession);
		try {
			visualizeBikeSession(bikeSession);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(mainWindow, e.getMessage(), I18n.getString("error_dialog.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	public void selectBikeSessions(ArrayList<BikeSession> bikeSessions) {
		selectedSessions = bikeSessions;
		try {
			visualizeBikeSessions(bikeSessions);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(mainWindow, e.getMessage(), I18n.getString("error_dialog.title"), JOptionPane.ERROR_MESSAGE);
		}
	}

	public void deleteSelectedBikeSessions() {
		for (BikeSession selectedSession : selectedSessions) {
			this.userData.getSessions().remove(selectedSession);
			selectedSession.getFile().delete();
		}
		userData.save();
		filterSessions();
	}

	/**
	 * Shows a bike session in the diagram.
	 *
	 * @param bikeSession bike session
	 */
	private void visualizeBikeSession(BikeSession bikeSession) throws IOException {
		diagramVisualizer.stopVisualization();

		// draw the program
		BikeProgram bikeProgram = programTree.getProgram(bikeSession.getProgramName());
		boolean programFound = bikeProgram != null;
		if (programFound) {
			visualizeBikeProgram(bikeProgram);
		}

		// draw the session
		BikeSessionVisualizer bikeSessionVisualizer = new BikeSessionVisualizer(mainWindow.getDiagram());
		diagramVisualizer = bikeSessionVisualizer;
		bikeSessionVisualizer.visualize(bikeSession, !programFound);
	}

	private void visualizeBikeSessions(ArrayList<BikeSession> bikeSessions) throws IOException {
		diagramVisualizer.stopVisualization();

		switch (sessionsVis) {
			case average:
				int duration = Integer.MAX_VALUE;
				for (BikeSession bikeSession : bikeSessions) {
					duration = Math.min(duration,bikeSession.getProgramDuration());
				}

				BikeSession virtualBikeSession = new BikeSession(bikeSessions.get(0).getProgramName(), duration);
				virtualBikeSession.initialVirtualBikeSession();

				int sessionCount = bikeSessions.size();

				int pulse, pulseCount, finalPulse, pedalRpm, power;
				MiniDataRecord data;

				for (int i = 0; i < duration; i++) {
					// reset values
					pulse = 0; pulseCount = 0; pedalRpm = 0; power = 0;

					for (BikeSession bikeSession : bikeSessions) {
						data = bikeSession.getData().get(i);
						if (data.getPulse() > 0) {
							pulse += data.getPulse();
							pulseCount++;
						}
						pedalRpm += data.getPedalRpm();
						power += data.getPower();
					}
					finalPulse = pulseCount == 0 ? 0 : pulse/pulseCount;

					virtualBikeSession.getData().add(new MiniDataRecord(finalPulse, power/sessionCount, pedalRpm/sessionCount));
				}
				visualizeBikeSession(virtualBikeSession);
				break;
			case progression:
				ProgressionVisualizer progressionVisualizer = new ProgressionVisualizer(mainWindow.getDiagram());
				diagramVisualizer = progressionVisualizer;
				progressionVisualizer.visualize(bikeSessions);
				break;
		}
	}

	public SessionsVis getSessionsVis() {
		return sessionsVis;
	}

	public void setSessionsVis(SessionsVis sessionsVis) {
		mainWindow.setSessionsVis(sessionsVis);
		this.sessionsVis = sessionsVis;

		if (!selectedSessions.isEmpty()) {
			try {
				visualizeBikeSessions(selectedSessions);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(mainWindow, e.getMessage(), I18n.getString("error_dialog.title"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	public void setShowOnlyCompletedSessions(boolean value) {
		mainWindow.setShowOnlyCompletedSessions(value);
		sessionFilter.setOnlyCompletedSessions(value);

		filterSessions();
	}

	public ArrayList<BikeSession> getSelectedBikeSessions() {
		return selectedSessions;
	}

	public JergometerSettings getSettings() {
		return jergometerSettings;
	}

	public BikeProgramTree getProgramTree() {
		return programTree;
	}

	/**
	 * Leaves the program.
	 */
	public void quit() {
		save();
		mainWindow.dispose();

		System.exit(0);
	}


// BikeReaderListener by BikeReader
	public void bikeAck() {
		switch(state) {
			case hello:
				state = State.connected;
				break;
			case reset:
				state = State.hello;
				break;
		}
	}

	public void bikeData(DataRecord data) {
		mainWindow.setData(data);
		if (program.getSession().getDuration() < program.getSession().getProgramDuration()) {
			mainWindow.getDiagram().addValue("pulse", program.getSession().getDuration(), data.getPulse());
			mainWindow.getDiagram().addValue("pedalRPM", program.getSession().getDuration(), data.getPedalRpm());
			mainWindow.getDiagram().addValue("power", program.getSession().getDuration(), data.getRealPower());
		}

		program.update(data);
		power = program.getPower();
	}

	public void bikeError() {
		System.err.println("Bike: ERROR");
	}

// ActionListener by communicationTimer
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == communicationTimer) {
			try {
				// FSM
				switch(state) {
					case notConnected:
						state = State.reset;
						bikeConnector.getWriter().sendReset();
						break;
					case hello:
						bikeConnector.getWriter().sendHello();
						break;
					case reset:
						bikeConnector.getWriter().sendReset();
						break;
					case connected:
						bikeConnector.getWriter().sendGetData(power);
						break;
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

// WindowListener by mainWindow
	public void windowOpened(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
		quit();
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowActivated(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}
}
