package org.jergometer;

import de.endrullis.utils.ParamsExt;
import de.endrullis.utils.ProgramUpdater;
import de.endrullis.utils.ShellPrintStream;
import de.endrullis.utils.StreamUtils;
import org.jergometer.communication.*;
import org.jergometer.control.BikeProgram;
import org.jergometer.diagram.*;
import org.jergometer.gui.ChooseNewProgramDialog;
import org.jergometer.gui.Diagram;
import org.jergometer.gui.MainWindow;
import org.jergometer.model.*;
import org.jergometer.translation.I18n;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import gnu.io.UnsupportedCommOperationException;

/**
 * Main class of JErgometer.
 */
public class Jergometer implements BikeReaderListener, ActionListener, WindowListener {

// static

	public static String version = "*Bleeding Edge*";
	public static boolean devVersion = true;
	public static boolean updatable = false;
	static {
		try {
			version = StreamUtils.readFile("version.txt");
			devVersion = false;
			updatable = true;
			updatable = Boolean.parseBoolean(StreamUtils.readFile("updatable"));
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
				new ParamsExt.Option("bleeding-edge", null, I18n.getString("args.bleeding_edge")),
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

		if (params.isOptionAvailable("bleeding-edge")) {
			version = "*Bleeding Edge*";
			devVersion = true;
			updatable = false;
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
	private Diagram.Marker sessionEndMarker = null;

	/**
	 * Creates an JErgometer instance.
	 *
	 * @param gui true if you want to have a gui
	 */
	public Jergometer(boolean gui) {
		this.gui = gui;

		jergometerSettings = new JergometerSettings();

		// search for updates in the background
		if (updatable) {
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
		mainWindow.setBounds(jergometerSettings.getMainWindowBounds());
		mainWindow.setExtendedState(mainWindow.getExtendedState() | jergometerSettings.getMainWindowMaximizedState());
		mainWindow.addWindowListener(this);
		mainWindow.setVisible(true);
		mainWindow.init();

		setShowOnlyCompletedSessions(true);
		setSessionsVis(SessionsVis.progression);

		switchToUser(jergometerSettings.getLastUserName());

		new JergometerConsole(this).start();

		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				quit();
			}
		});
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
			if (JOptionPane.showConfirmDialog(mainWindow, I18n.getString("msg.new_version_available.want_to_update"),
					I18n.getString("msg.updater.title"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {

				// perform update
				if (updater.performUpdate(true)) {
					// restart the editor
					System.exit(255);
				}
			}
		} else {
			if (!start) {
				JOptionPane.showMessageDialog(mainWindow, I18n.getString("msg.jergometer_is_up-to-date"), I18n.getString("msg.updater.title"), JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	/**
	 * Connects to the serial port.
	 *
	 * @throws gnu.io.UnsupportedCommOperationException if communication operation is not supported
	 * @throws java.io.IOException if an I/O error occurs
	 * @throws org.jergometer.communication.BikeException if the bike communication fails
	 * @throws org.jergometer.communication.UnconfiguredSerialPortException if the serial port is not configured yet
	 */
	private void connectToSerialPort() throws BikeException, UnsupportedCommOperationException, IOException, UnconfiguredSerialPortException {
		String comPort = jergometerSettings.getSerialPort();
		if (comPort == null) {
			throw new UnconfiguredSerialPortException();
		} else
		if (comPort.equals("replay")) {
			bikeConnector = new BikeConnectorSimulator();
		} else
		if (comPort.startsWith("record:")) {
			comPort = comPort.substring("record:".length());
			bikeConnector = new KetterBikeConnector(comPort);
			bikeConnector.getReader().addBikeReaderListener(new FileRecorder(BikeConnectorSimulator.SIMULATOR_SESSION));
		} else {
			bikeConnector = new KetterBikeConnector(comPort);
		}

		BikeReader bikeReader = bikeConnector.getReader();
		bikeReader.addBikeReaderListener(this);
		bikeReader.start();
	}

	/**
	 * Saves all settings and sessions.
	 */
	private void save() {
		stopRecording();
		jergometerSettings.setMainWindowBounds(mainWindow.getBounds());
		jergometerSettings.setMainWindowMaximizedState(mainWindow.getExtendedState() & JFrame.MAXIMIZED_BOTH);
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
		if (userSettings.getLastProgram() != null) {
			DefaultMutableTreeNode programNode = programTree.getProgramNode(userSettings.getLastProgram());
			final TreePath path = new TreePath(programNode.getPath());
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					mainWindow.getProgramTree().expandPath(path);
					mainWindow.getProgramTree().getSelectionModel().setSelectionPath(path);
					mainWindow.getProgramTree().scrollPathToVisible(path);
				}
			});
		}
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
			if (program == null) {
				JOptionPane.showMessageDialog(mainWindow, I18n.getString("msg.choose_a_program"));
				return;
			}

			// clear diagram and draw the bike program
			selectBikeProgram(program, true);

			Diagram diagram = mainWindow.getDiagram();
//			diagram.clearGraphs();
			BikeDiagram.createLegend(diagram, false, false, program.getProgramData().getDuration());

			try {
				connectToSerialPort();
			} catch (BikeException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(mainWindow, e.getMessage(), I18n.getString("error_dialog.title"), JOptionPane.ERROR_MESSAGE);
				return;
			} catch (UnconfiguredSerialPortException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(mainWindow, I18n.getString("msg.configure_comport_first"));
				mainWindow.openSettingsWindow();
			} catch (NoClassDefFoundError e) {
				JOptionPane.showMessageDialog(mainWindow, I18n.getString("msg.unsatisfied_link_error"), I18n.getString("error_dialog.title"), JOptionPane.ERROR_MESSAGE);
				return;
			} catch (UnsatisfiedLinkError e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(mainWindow, I18n.getString("msg.unsatisfied_link_error"), I18n.getString("error_dialog.title"), JOptionPane.ERROR_MESSAGE);
				return;
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(mainWindow, I18n.getString("msg.connection_failed"), I18n.getString("error_dialog.title"), JOptionPane.ERROR_MESSAGE);
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
		selectBikeProgram(bikeProgram, false);
	}

	private void selectBikeProgram(BikeProgram bikeProgram, boolean bright) {
		selectedSessions.clear();
		program = bikeProgram;
		sessionFilter.setProgramFilter(bikeProgram);
		diagramVisualizer.stopVisualization();
		filterSessions();
		visualizeBikeProgram(bikeProgram, bright, -1);

		if (userSettings != null) {
			userSettings.setLastProgram(bikeProgram.getProgramName());
		}
	}

	public void selectBikeProgramDirectory(BikeProgramDir bikeProgramDir) {
		sessionFilter.setProgramDirFilter(bikeProgramDir);
		filterSessions();
	}

	public void selectBikeProgramRoot() {
		sessionFilter.deactivateFilter();
		filterSessions();
	}

	private void visualizeBikeProgram(BikeProgram bikeProgram, boolean bright, long duration) {
		BikeProgramVisualizer bikeProgramVisualizer = new BikeProgramVisualizer(mainWindow.getDiagram());
		diagramVisualizer = bikeProgramVisualizer;
		bikeProgramVisualizer.visualize(bikeProgram, bright, duration);
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
			//noinspection ResultOfMethodCallIgnored
			selectedSession.getFile().delete();
		}
		userData.save();
		filterSessions();
	}

	/**
	 * Shows a bike session in the diagram.
	 *
	 * @param bikeSession bike session
	 * @throws java.io.IOException if an I/O error occurs
	 */
	private void visualizeBikeSession(BikeSession bikeSession) throws IOException {
		diagramVisualizer.stopVisualization();

		int duration = mainWindow.isShowFullSessionLength() ? bikeSession.getStatsTotal().getDuration() : bikeSession.getProgramDuration();

		// draw the program
		BikeProgram bikeProgram = programTree.getProgram(bikeSession.getProgramName());
		boolean programFound = bikeProgram != null;
		if (programFound) {
			visualizeBikeProgram(bikeProgram, true, duration);
		}

		// draw the session
		BikeSessionVisualizer bikeSessionVisualizer = new BikeSessionVisualizer(mainWindow.getDiagram());
		diagramVisualizer = bikeSessionVisualizer;
		bikeSessionVisualizer.visualize(bikeSession, !programFound, mainWindow.isShowFullSessionLength());
	}

	private void visualizeBikeSessions(ArrayList<BikeSession> bikeSessions) throws IOException {
		diagramVisualizer.stopVisualization();

		switch (sessionsVis) {
			case average:
				boolean fullLength = mainWindow.isShowFullSessionLength();

				// calculate minimal duration
				int duration = Integer.MAX_VALUE;
				for (BikeSession bikeSession : bikeSessions) {
					int thisDuration = fullLength ? bikeSession.getStatsTotal().getDuration() : bikeSession.getProgramDuration();
					duration = Math.min(duration, thisDuration);
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

	public void setShowFullSessionLength(boolean value) {
		mainWindow.setShowFullSessionLength(value);

		if (selectedSessions.size() == 1) {
			selectBikeSession(selectedSessions.get(0));
		} else
		if (selectedSessions.size() > 1) {
			selectBikeSessions(selectedSessions);
		}
	}

	public BikeProgram getProgram() {
		return program;
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
		//mainWindow.dispose();
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

		Diagram diagram = mainWindow.getDiagram();

		// extends time range when we are at the end
		if (program.getSession().getDurationPulse() >= diagram.getTimeRange().max) {
			long newMax = diagram.getTimeRange().max + (program.getProgramData().getDuration() / 2);
			diagram.setTimeRange(new Diagram.Range(0, newMax));
			diagram.redrawImage();
		}

		/*
		if (program.getSession().getDuration() < diagram.getTimeRange().max) {
			int time = program.getSession().getDuration();
			diagram.addValue("pulse", time, data.getPulse());
			diagram.addValue("pedalRPM", time, data.getPedalRpm());
			diagram.addValue("power", time, data.getRealPower());
		}
		*/

		// add new data record to session
		switch (program.update(data)) {
			case cycle:
				// user is cycling -> remove session end marker if added
				if (sessionEndMarker != null) {
					diagram.removeVerticalMarker(sessionEndMarker);
					diagram.clearGraph("pulse-end");
					diagram.redrawImage();
					diagram.repaint();
					sessionEndMarker = null;
				}

				int time = program.getSession().getDuration();
				diagram.addValue("pulse", time, data.getPulse());
				diagram.addValue("pedalRPM", time, data.getPedalRpm());
				diagram.addValue("power", time, data.getRealPower());

				power = program.getPower();
				break;

			case pulse:
				// user is not cycling -> add session end marker if not already added
				if (sessionEndMarker == null) {
					int endTime = program.getSession().getStatsTotal().getDuration();
					sessionEndMarker = new Diagram.Marker(endTime, new Color(196, 196, 0), new BasicStroke(), "session end");
					diagram.addVerticalMarker(sessionEndMarker);
					diagram.repaint();
					
					time = program.getSession().getDuration();
					for (Integer pulse : program.getSession().getPulseAfterSession()) {
						diagram.addValue("pulse-end", ++time, pulse);
					}
				} else {
					time = program.getSession().getDurationPulse();
					diagram.addValue("pulse-end", time, data.getPulse());
				}
				break;
		}
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
		System.exit(0);
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
