package org.jergometer.gui;

import de.endrullis.utils.VelocityUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.jergometer.Jergometer;
import org.jergometer.control.BikeProgram;
import org.jergometer.model.*;
import org.jergometer.translation.I18n;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.IOException;
import java.io.File;
import java.util.List;

import de.endrullis.utils.SystemUtils;
import de.endrullis.utils.StreamUtils;

/**
 * Main window.
 */
public class MainWindow extends JFrame implements ActionListener, TreeSelectionListener, ListSelectionListener, KeyListener {
	private static double kcalFactor = 0.239005736;

	private Properties iconMap = new Properties();
	{
		try {
			iconMap.load(StreamUtils.getInputStream("org/jergometer/images/icon_map.properties"));
		} catch (IOException ignored) {
		}
	}

	private JPanel mainPanel;
	private Diagram diagram;
	private JTable sessionTable;
	private JTree programTree;
	private JLabel welcomeLabel;
	private JButton recordButton;
	private JButton stopButton;
	private BikeInfoPane bikeInfoPane;

	// menu items
	private JMenuItem newUserMenuItem;
	private JMenuItem settingsMenuItem;
	private JMenuItem quitMenuItem;
	private JMenuItem aboutMenuItem;
	private JRadioButtonMenuItem diagramAverageValuesMenuItem;
	private JRadioButtonMenuItem diagramProgressionMenuItem;
	private JCheckBoxMenuItem showOnlyCompletedSessionsMenuItem;
	private JCheckBoxMenuItem showFullSessionLength;
	private JMenuItemSet editProgramMenuItem;
	private JMenuItemSet renameProgramMenuItem;
	private JMenuItemSet createNewProgramDirectory;
	private JMenuItemSet copyProgramMenuItem;
	private JMenuItemSet cutProgramDataMenuItem;
	private JMenuItemSet deleteProgramMenuItem;
	private JMenuItemSet updateProgramMenuItem;
	private JMenu userMenu;
	private ButtonGroup userButtonGroup;
	private ArrayList<JMenuItem> userMenuItems = new ArrayList<JMenuItem>();
	private JMenuItemSet insertProgramMenuItem;

	// popup menus
	private JPopupMenu sessionTablePopup;
	private JPopupMenu programTreePopup;

	private DefaultMutableTreeNode copiedProgramNode = null;
	private boolean movePrograms = false;
	private DataRecord lastDataRecord;
	private boolean kcal = false;

	/**
	 * Main class.
	 */
	Jergometer jergometer;
	private static final String AC_SELECT_ALL_SESSIONS = "select all sessions";
	private static final String AC_DELETE_SELECTED_SESSIONS = "delete selected sessions";
	private static final String AC_REPARSE_USER_DATA = "reparse user data";
	private static final String AC_EDIT_PROGRAM = "edit program";
	private static final String AC_RENAME_PROGRAM = "rename program";
	private static final String AC_CREATE_NEW_PROGRAM_DIRECTORY = "create new program directory";
	private static final String AC_COPY_PROGRAM = "copy program";
	private static final String AC_CUT_PROGRAM = "cut program";
	private static final String AC_INSERT_PROGRAM = "insert program";
	private static final String AC_DELETE_PROGRAM = "delete program";
	private static final String AC_UPDATE_PROGRAM = "update program";

	public MainWindow(String title, final Jergometer jergometer) throws HeadlessException {
		super(title);
		this.jergometer = jergometer;
		$$$setupUI$$$();
		setContentPane(mainPanel);
		createPopups();
		setJMenuBar(createMenuBar());

		// set icon
		try {
			setIconImage(new ImageIcon(StreamUtils.readBytesFromInputStream(StreamUtils.getInputStream("org/jergometer/images/icon_32.png"))).getImage());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// single selection for the program tree
		programTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		// remove keystroke "control A" from program tree
		programTree.getInputMap().getParent().remove(KeyStroke.getKeyStroke("control A"));
		programTree.setCellRenderer(new BetterTreeCellRenderer());

		// reset bikeInfoPane
		bikeInfoPane.resetValues();

		// add listener
		recordButton.addActionListener(this);
		stopButton.addActionListener(this);
		sessionTable.getSelectionModel().addListSelectionListener(this);
		sessionTable.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		sessionTable.addKeyListener(this);
		sessionTable.setComponentPopupMenu(sessionTablePopup);
		sessionTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					sessionTablePopup.show(sessionTable, e.getX(), e.getY());
				}
			}
		});
		programTree.addTreeSelectionListener(this);
		//programTree.setComponentPopupMenu(programTreePopup);
		programTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					System.out.println(1);
					int row = programTree.getRowForLocation(e.getX(), e.getY());
					programTree.setSelectionRow(row);
					programTreePopup.show(programTree, e.getX(), e.getY());
					System.out.println(2);
				}
			}
		});
		programTree.addKeyListener(this);
	}

	public void init() {
		// create a user if not already done
		while (jergometer.getSettings().getLastUserName() == null) {
			createNewUser();
		}
	}

	public void setData(DataRecord dataRecord) {
		lastDataRecord = dataRecord;

		updateBikeInfoPane(dataRecord);
	}

	public void updateBikeInfoPane(DataRecord dataRecord) {
		if (dataRecord == null) return;

		VelocityContext context = new VelocityContext();
		context.put("pulse", dataRecord.getPulse());
		context.put("pulseString", dataRecord.getPulse() == 0 ? "?" : "" + dataRecord.getPulse());
		context.put("pedalRpm", dataRecord.getPedalRpm());
		context.put("pedalRpmString", dataRecord.getPedalRpm() + "&nbsp;rpm");
		context.put("speed", dataRecord.getSpeed());
		context.put("speedString", String.format("%.1f&nbsp;km/h", (double) dataRecord.getSpeed() / 10));
		context.put("distance", dataRecord.getDistance());
		context.put("distanceString", dataRecord.getDistance() + "&nbsp;km");
		context.put("destPower", dataRecord.getDestPower());
		context.put("destPowerString", dataRecord.getDestPower() + "&nbsp;W");
		context.put("actPower", dataRecord.getRealPower());
		context.put("actPowerString", dataRecord.getRealPower() + "&nbsp;W");
		context.put("energy", dataRecord.getEnergy());
		String energyString = dataRecord.getEnergy() + "&nbsp;kJ";
		if (kcal) {
			energyString = ((int) (dataRecord.getEnergy() * kcalFactor)) + "&nbsp;kcal";
		}
		context.put("energyString", energyString);
		context.put("timeString", dataRecord.getTime());

		bikeInfoPane.setContext(context);
	}

	public void bikeInfoPaneAction(String action) {
		if (action.equals("energy")) {
			kcal = !kcal;
			updateBikeInfoPane(lastDataRecord);
		}
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		// File
		{
			JMenu menu = new JMenu(I18n.getString("menu.file"));
			menu.setMnemonic(I18n.getMnemonic("menu.file_mn"));
			menuBar.add(menu);
			menu.add(newUserMenuItem = createMenuItem("menu.file.new_user"));
			menu.add(settingsMenuItem = createMenuItem("menu.file.settings"));
			menu.add(quitMenuItem = createMenuItem("menu.file.quit"));
		}

		// User
		{
			userMenu = new JMenu(I18n.getString("menu.user"));
			userMenu.setMnemonic(I18n.getMnemonic("menu.user_mn"));
			menuBar.add(userMenu);
		}

		// Programs
		{
			JMenu programsMenu = new JMenu(I18n.getString("menu.programs"));
			programsMenu.setMnemonic(I18n.getMnemonic("menu.programs_mn"));
			menuBar.add(programsMenu);
			programsMenu.add(editProgramMenuItem.next());
			programsMenu.add(renameProgramMenuItem.next());
			programsMenu.add(createNewProgramDirectory.next());
			programsMenu.add(copyProgramMenuItem.next());
			programsMenu.add(cutProgramDataMenuItem.next());
			programsMenu.add(insertProgramMenuItem.next());
			programsMenu.add(deleteProgramMenuItem.next());
			programsMenu.add(updateProgramMenuItem.next());
		}

		// Sessions
		{
			JMenu sessionsMenu = new JMenu(I18n.getString("menu.sessions"));
			sessionsMenu.setMnemonic(I18n.getMnemonic("menu.sessions_mn"));
			menuBar.add(sessionsMenu);

			ButtonGroup group = new ButtonGroup();
			sessionsMenu.add(diagramAverageValuesMenuItem = createRadioButtonMenuItem("menu.sessions.diagram_average_values", group));
			diagramAverageValuesMenuItem.setSelected(true);
			sessionsMenu.add(diagramProgressionMenuItem = createRadioButtonMenuItem("menu.sessions.diagram_progression", group));
			sessionsMenu.addSeparator();
			sessionsMenu.add(showOnlyCompletedSessionsMenuItem = createCheckBoxMenuItem("menu.sessions.show_only_completed"));
			sessionsMenu.add(showFullSessionLength = createCheckBoxMenuItem("menu.sessions.show_full_length"));
			sessionsMenu.addSeparator();
			JMenuItem selectAllSessionsMenuItem = createMenuItem("menu.sessions.select_all");
			selectAllSessionsMenuItem.setActionCommand(AC_SELECT_ALL_SESSIONS);
			sessionsMenu.add(selectAllSessionsMenuItem);
			JMenuItem deleteSelectedSessionsMenuItem = createMenuItem("menu.sessions.delete_selected");
			deleteSelectedSessionsMenuItem.setActionCommand(AC_DELETE_SELECTED_SESSIONS);
			sessionsMenu.add(deleteSelectedSessionsMenuItem);
			JMenuItem reparseUserDataMenuItem = createMenuItem("menu.sessions.reparse_user_data");
			reparseUserDataMenuItem.setActionCommand(AC_REPARSE_USER_DATA);
			sessionsMenu.add(reparseUserDataMenuItem);
		}

		// Help
		{
			JMenu menu = new JMenu(I18n.getString("menu.help"));
			menu.setMnemonic(I18n.getMnemonic("menu.help_mn"));
			menuBar.add(menu);
			menu.add(aboutMenuItem = createMenuItem("menu.help.about"));
		}

		return menuBar;
	}

	private JMenuItem createMenuItem(String command) {
		return assignProperties(new JMenuItem(I18n.getString(command)), command);
	}

	private JCheckBoxMenuItem createCheckBoxMenuItem(String command) {
		return (JCheckBoxMenuItem) assignProperties(new JCheckBoxMenuItem(I18n.getString(command)), command);
	}

	private JRadioButtonMenuItem createRadioButtonMenuItem(String command, ButtonGroup buttonGroup) {
		JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(I18n.getString(command));
		assignProperties(menuItem, command);
		buttonGroup.add(menuItem);
		return menuItem;
	}

	private JMenuItem assignProperties(JMenuItem menuItem, String command) {
		menuItem.setActionCommand(command);
		// set mnemonic
		try {
			char mnemonic = I18n.getMnemonic(command + "_mn");
			if (mnemonic != '!') {
				menuItem.setMnemonic(mnemonic);
			}
		} catch (MissingResourceException ignored) {
		}
		// set shortcut
		try {
			String shorcutString = I18n.getString(command + "_ks");
			if (shorcutString != null && !shorcutString.equals("")) {
				menuItem.setAccelerator(KeyStroke.getKeyStroke(shorcutString));
			}
		} catch (MissingResourceException ignored) {
		}
		// set icon
		try {
			String filename = iconMap.getProperty(command);
			menuItem.setIcon(new ImageIcon(StreamUtils.readBytesFromInputStream(StreamUtils.getInputStream(filename))));
		} catch (Exception ignored) {
		}
		menuItem.addActionListener(this);

		return menuItem;
	}

	private void createPopups() {
		// session table popup
		{
			sessionTablePopup = new JPopupMenu();
			JMenuItem selectAllSessionsMenuItem = new JMenuItem(I18n.getString("menu.sessions.select_all"));
			selectAllSessionsMenuItem.setActionCommand(AC_SELECT_ALL_SESSIONS);
			selectAllSessionsMenuItem.setMnemonic(I18n.getMnemonic("menu.sessions.select_all_mn"));
			selectAllSessionsMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.sessions.select_all_ks")));
			selectAllSessionsMenuItem.addActionListener(this);
			sessionTablePopup.add(selectAllSessionsMenuItem);
			JMenuItem deleteSelectedSessionsMenuItem = new JMenuItem(I18n.getString("menu.sessions.delete_selected"));
			deleteSelectedSessionsMenuItem.setActionCommand(AC_DELETE_SELECTED_SESSIONS);
			deleteSelectedSessionsMenuItem.setMnemonic(I18n.getMnemonic("menu.sessions.delete_selected_mn"));
//			deleteSelectedSessionsMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.sessions.delete_selected_all_ks")));
			deleteSelectedSessionsMenuItem.addActionListener(this);
			sessionTablePopup.add(deleteSelectedSessionsMenuItem);
			JMenuItem reparseUserDataMenuItem = new JMenuItem(I18n.getString("menu.sessions.reparse_user_data"));
			reparseUserDataMenuItem.setActionCommand(AC_REPARSE_USER_DATA);
			reparseUserDataMenuItem.setMnemonic(I18n.getMnemonic("menu.sessions.reparse_user_data_mn"));
			reparseUserDataMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.sessions.reparse_user_data_ks")));
			reparseUserDataMenuItem.addActionListener(this);
			sessionTablePopup.add(reparseUserDataMenuItem);
		}

		// program tree popup
		{
			programTreePopup = new JPopupMenu();
			editProgramMenuItem = new JMenuItemSet("menu.programs.edit", AC_EDIT_PROGRAM);
			programTreePopup.add(editProgramMenuItem.next());
			renameProgramMenuItem = new JMenuItemSet("menu.programs.rename", AC_RENAME_PROGRAM);
			programTreePopup.add(renameProgramMenuItem.next());
			createNewProgramDirectory = new JMenuItemSet("menu.programs.create_new_directory", AC_CREATE_NEW_PROGRAM_DIRECTORY);
			programTreePopup.add(createNewProgramDirectory.next());
			copyProgramMenuItem = new JMenuItemSet("menu.programs.copy", AC_COPY_PROGRAM);
			programTreePopup.add(copyProgramMenuItem.next());
			cutProgramDataMenuItem = new JMenuItemSet("menu.programs.cut", AC_CUT_PROGRAM);
			programTreePopup.add(cutProgramDataMenuItem.next());
			insertProgramMenuItem = new JMenuItemSet("menu.programs.insert", AC_INSERT_PROGRAM);
			insertProgramMenuItem.setEnabled(false);
			programTreePopup.add(insertProgramMenuItem.next());
			deleteProgramMenuItem = new JMenuItemSet("menu.programs.delete", AC_DELETE_PROGRAM);
			programTreePopup.add(deleteProgramMenuItem.next());
			updateProgramMenuItem = new JMenuItemSet("menu.programs.update", AC_UPDATE_PROGRAM);
			programTreePopup.add(updateProgramMenuItem.next());
		}
	}

	public void setUserList(String[] userNames, String selectedUser) {
		userMenu.removeAll();
		userMenuItems.clear();
		userButtonGroup = new ButtonGroup();
		for (String userName : userNames) {
			JMenuItem menuItem = new JRadioButtonMenuItem(userName);
			menuItem.addActionListener(this);
			userMenuItems.add(menuItem);
			userMenu.add(menuItem);
			userButtonGroup.add(menuItem);
			if (selectedUser != null && selectedUser.equals(userName)) {
				menuItem.setSelected(true);
			}
		}
		if (selectedUser != null) {
			welcomeLabel.setText(I18n.getString("label.welcome_user", selectedUser));
		}
	}

	public void setRecordState(boolean recording) {
		if (recording) {
			recordButton.setEnabled(false);
			stopButton.setEnabled(true);
		} else {
			recordButton.setEnabled(true);
			stopButton.setEnabled(false);
		}
	}

	// getters

	public Diagram getDiagram() {
		return diagram;
	}

	public JTree getProgramTree() {
		return programTree;
	}

	public JTable getSessionTable() {
		return sessionTable;
	}

	// ActionListener by menu items

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == recordButton) {
			jergometer.startRecording();
		} else if (e.getSource() == stopButton) {
			jergometer.stopRecording();
		} else if (e.getSource() == newUserMenuItem) {
			createNewUser();
		} else if (e.getSource() == settingsMenuItem) {
			openSettingsWindow();
		} else if (e.getSource() == quitMenuItem) {
			System.exit(0);
		} else if (e.getSource() == aboutMenuItem) {
			new AboutDialog().showMe();
		} else if (e.getSource() == diagramAverageValuesMenuItem) {
			jergometer.setSessionsVis(Jergometer.SessionsVis.average);
		} else if (e.getSource() == diagramProgressionMenuItem) {
			jergometer.setSessionsVis(Jergometer.SessionsVis.progression);
		} else if (e.getSource() == showOnlyCompletedSessionsMenuItem) {
			jergometer.setShowOnlyCompletedSessions(showOnlyCompletedSessionsMenuItem.isSelected());
		} else if (e.getSource() == showFullSessionLength) {
			jergometer.setShowFullSessionLength(showFullSessionLength.isSelected());
		} else if (e.getActionCommand() == AC_SELECT_ALL_SESSIONS) {
			sessionTable.selectAll();
		} else if (e.getActionCommand() == AC_DELETE_SELECTED_SESSIONS) {
			deleteSelectedSessions();
		} else if (e.getActionCommand() == AC_REPARSE_USER_DATA) {
			jergometer.reparseUserData();
		} else if (e.getActionCommand() == AC_EDIT_PROGRAM) {
			editProgram();
		} else if (e.getActionCommand() == AC_RENAME_PROGRAM) {
			renameProgram();
		} else if (e.getActionCommand() == AC_CREATE_NEW_PROGRAM_DIRECTORY) {
			createNewProgramDirectory();
		} else if (e.getActionCommand() == AC_COPY_PROGRAM) {
			copyProgram();
		} else if (e.getActionCommand() == AC_CUT_PROGRAM) {
			cutProgram();
		} else if (e.getActionCommand() == AC_INSERT_PROGRAM) {
			insertProgram();
		} else if (e.getActionCommand() == AC_DELETE_PROGRAM) {
			deleteProgram();
		} else if (e.getActionCommand() == AC_UPDATE_PROGRAM) {
			updateProgram();
		} else {
			String userName = null;
			for (JMenuItem userMenuItem : userMenuItems) {
				if (e.getSource() == userMenuItem) {
					userName = userMenuItem.getText();
				}
			}
			if (userName != null) {
				jergometer.switchToUser(userName);
			}
		}
	}

	private void createNewUser() {
		String userName = JOptionPane.showInputDialog(this, I18n.getString("msg.enter_username"), I18n.getString("label.create_new_user"), JOptionPane.QUESTION_MESSAGE);
		if (userName != null && !userName.trim().equals("")) {
			jergometer.newUser(userName);
		}
	}

	private void editProgram() {
		if (programTree.getSelectionCount() == 1) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) programTree.getSelectionPath().getLastPathComponent();
			if (node.getUserObject() instanceof BikeProgram) {
				final BikeProgram bikeProgram = (BikeProgram) node.getUserObject();

				final String xmlEditor = jergometer.getSettings().getXmlEditor();
				if (xmlEditor == null && !Desktop.isDesktopSupported()) {
					JOptionPane.showMessageDialog(this, I18n.getString("msg.configure_xml_editor_first"));
					openSettingsWindow();
					return;
				}

				final MainWindow mainWindow = this;
				Thread thread = new Thread() {
					@Override
					public void run() {
						if (xmlEditor == null) {
							try {
								Desktop.getDesktop().edit(bikeProgram.getFile());
							} catch (Exception e) {
								JOptionPane.showMessageDialog(mainWindow, I18n.getString("msg.could_not_open_file.please_configure_editor"),
									I18n.getString("msg.could_not_open_xml_editor"), JOptionPane.ERROR_MESSAGE);
								openSettingsWindow();
							}
						} else {
							String command = xmlEditor + " " + bikeProgram.getFile().getAbsolutePath();
							try {
								SystemUtils.exec(command);
							} catch (IOException e) {
								JOptionPane.showMessageDialog(mainWindow, I18n.getString("msg.error_during_execution_of", command),
									I18n.getString("msg.could_not_open_xml_editor"), JOptionPane.ERROR_MESSAGE);
							} catch (InterruptedException ignored) {
							}
						}
					}
				};
				thread.start();
			}
		}
	}

	private void renameProgram() {
		if (programTree.getSelectionCount() == 1) {
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) programTree.getSelectionPath().getLastPathComponent();

			// ensure source node is not root
			if (selectedNode == programTree.getModel().getRoot()) return;

			Object userObject = selectedNode.getUserObject();

			if (userObject instanceof HoldsFile) {
				HoldsFile holdsFile = (HoldsFile) userObject;

				// ask for renaming the file
				File oldFile = holdsFile.getFile();
				String newFileName = JOptionPane.showInputDialog(this, I18n.getString("enter_new_file_or_directory_name"), oldFile.getName());
				if (newFileName != null) {
					oldFile.renameTo(new File(oldFile.getParent(), newFileName));
					jergometer.getProgramTree().updateNode((DefaultMutableTreeNode) selectedNode.getParent());
				}
			}
		}
	}

	private void createNewProgramDirectory() {
		if (programTree.getSelectionCount() == 1) {
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) programTree.getSelectionPath().getLastPathComponent();

			// ensure source node is a program directory
			if (selectedNode.getUserObject() instanceof BikeProgram) {
				selectedNode = (DefaultMutableTreeNode) selectedNode.getParent();
			}

			Object node = selectedNode.getUserObject();
			if (node instanceof BikeProgramDir) {
				BikeProgramDir bikeProgramDir = (BikeProgramDir) node;
				File dir = bikeProgramDir.getFile();

				// ask for renaming the file
				String newFileName = JOptionPane.showInputDialog(this, I18n.getString("enter_new_file_or_directory_name"), "");
				if (newFileName != null && !newFileName.trim().equals("")) {
					new File(dir, newFileName).mkdir();
					jergometer.getProgramTree().updateNode(selectedNode);
				}
			}

			copiedProgramNode = selectedNode;
			insertProgramMenuItem.setEnabled(true);
			movePrograms = false;
		}
	}

	private void copyProgram() {
		if (programTree.getSelectionCount() == 1) {
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) programTree.getSelectionPath().getLastPathComponent();

			// ensure source node is not root
			if (selectedNode == programTree.getModel().getRoot()) return;

			copiedProgramNode = selectedNode;
			insertProgramMenuItem.setEnabled(true);
			movePrograms = false;
		}
	}

	private void cutProgram() {
		if (programTree.getSelectionCount() == 1) {
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) programTree.getSelectionPath().getLastPathComponent();

			// ensure source node is not root
			if (selectedNode == programTree.getModel().getRoot()) return;

			copiedProgramNode = (DefaultMutableTreeNode) programTree.getSelectionPath().getLastPathComponent();
			insertProgramMenuItem.setEnabled(true);
			movePrograms = true;
		}
	}

	private void insertProgram() {
		if (copiedProgramNode != null && programTree.getSelectionCount() == 1) {
			if (movePrograms) {
				// cannot move node A into A's own subtree
				boolean ok = true;
				for (Object node : programTree.getSelectionPath().getPath()) {
					if (copiedProgramNode == node) {
						ok = false;
						break;
					}
				}
				if (!ok) return;
			}

			DefaultMutableTreeNode srcNode = copiedProgramNode;
			DefaultMutableTreeNode destNode = (DefaultMutableTreeNode) programTree.getSelectionPath().getLastPathComponent();

			// ensure destination is a directory
			if (!(destNode.getUserObject() instanceof BikeProgramDir)) {
				JOptionPane.showMessageDialog(this, I18n.getString("msg.select_a_destination_directory"));
				return;
			}

			File destDir = ((BikeProgramDir) destNode.getUserObject()).getFile();
			File srcFile = ((HoldsFile) srcNode.getUserObject()).getFile();

			// check if a file/dir with the same name already exists in the destination directory
			File destFile = new File(destDir, srcFile.getName());
			while (destFile.exists()) {
				String fileName = JOptionPane.showInputDialog(this, I18n.getString("msg.file_or_directory_already_exists.please_choose_another_name"), destFile.getName());
				if (fileName == null) return;
				destFile = new File(destDir, fileName);
			}

			try {
				// copy srcNode recursively to destFile
				copyFilesRecursivelyToDir(srcNode, destFile);

				// delete srcNode and corresponding files if action is "cut"
				if (movePrograms) {
					jergometer.getProgramTree().deletePrograms(srcNode);
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(this, I18n.getString("msg.failed_to_copy_the_files"));
			}

			jergometer.getProgramTree().updateNode(destNode);
		}
	}

	/**
	 * Copies all files and directories corresponding to the elements of this subtree to the given destination file/directory.
	 *
	 * @param srcNode  root node of the subtree to copy
	 * @param destFile destination file or directory
	 * @throws java.io.IOException thrown if reading or writing has failed
	 */
	private void copyFilesRecursivelyToDir(DefaultMutableTreeNode srcNode, File destFile) throws IOException {
		File srcFile = ((HoldsFile) srcNode.getUserObject()).getFile();
		if (srcFile.isFile()) {
			StreamUtils.copyFileLinewise(srcFile, destFile);
		} else {
			destFile.mkdir();
			Enumeration children = srcNode.children();
			while (children.hasMoreElements()) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) children.nextElement();
				File childFile = ((HoldsFile) childNode.getUserObject()).getFile();
				copyFilesRecursivelyToDir(childNode, new File(destFile, childFile.getName()));
			}
		}
	}

	private void deleteProgram() {
		if (programTree.getSelectionCount() == 1) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) programTree.getSelectionPath().getLastPathComponent();
			if (node == programTree.getModel().getRoot()) {
				if (JOptionPane.showConfirmDialog(this, I18n.getString("msg.really_delete_all_programs")) != JOptionPane.YES_OPTION)
					return;
			} else if (node.getUserObject() instanceof BikeProgramDir) {
				if (JOptionPane.showConfirmDialog(this, I18n.getString("msg.really_delete_selected_program_dir")) != JOptionPane.YES_OPTION)
					return;
			} else if (node.getUserObject() instanceof BikeProgram) {
				if (JOptionPane.showConfirmDialog(this, I18n.getString("msg.really_delete_selected_program")) != JOptionPane.YES_OPTION)
					return;
			} else {
				return;
			}

			int row = programTree.getSelectionModel().getLeadSelectionRow();

			// remove the node from the programTree
			jergometer.getProgramTree().deletePrograms(node);

			// select program over deleted program
			programTree.setSelectionRow(Math.max(0, row - 1));
		}
	}

	private void updateProgram() {
		if (programTree.getSelectionCount() == 1) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) programTree.getSelectionPath().getLastPathComponent();
			jergometer.getProgramTree().updateNode(node);
			// is selected node is bike program -> update the diagram
			if (node.getUserObject() instanceof BikeProgram) {
				BikeProgram bikeProgram = (BikeProgram) node.getUserObject();
				jergometer.selectBikeProgram(bikeProgram);
			}
		}
	}

	private void deleteSelectedSessions() {
		if (jergometer.getSelectedBikeSessions().size() > 0) {
			if (JOptionPane.showConfirmDialog(this, I18n.getString("msg.really_delete_selected_sessions")) == JOptionPane.YES_OPTION) {
				jergometer.deleteSelectedBikeSessions();
			}
		}
	}

	public void openSettingsWindow() {
		SettingsWindow settingsWindow = new SettingsWindow(this);
		if (settingsWindow.showDialog(jergometer.getSettings()) == SettingsWindow.ReturnCode.save) {
			settingsWindow.saveSettings(jergometer.getSettings());
		}
	}

	// TreeSelectionListener by programTree

	public void valueChanged(TreeSelectionEvent e) {
		if (e.getSource() == programTree) {
			if (programTree.getSelectionCount() == 1) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) programTree.getSelectionPath().getLastPathComponent();
				if (node == programTree.getModel().getRoot()) {
					jergometer.selectBikeProgramRoot();
					editProgramMenuItem.setEnabled(false);
				} else if (node.getUserObject() instanceof BikeProgramDir) {
					jergometer.selectBikeProgramDirectory((BikeProgramDir) node.getUserObject());
					editProgramMenuItem.setEnabled(false);
				} else if (node.getUserObject() instanceof BikeProgram) {
					jergometer.selectBikeProgram((BikeProgram) node.getUserObject());
					editProgramMenuItem.setEnabled(true);
				}
			}
		}
	}

	// ListSelectionListener by sessionTable

	public void valueChanged(ListSelectionEvent e) {
		if (sessionTable.getSelectionModel().isSelectionEmpty()) return;

		ArrayList<BikeSession> selectedSessions = new ArrayList<BikeSession>();

		SessionTableModel sessionTableModel = (SessionTableModel) sessionTable.getModel();
		ListSelectionModel selectionModel = sessionTable.getSelectionModel();

		for (int i = 0; i < sessionTableModel.getRowCount(); i++) {
			if (selectionModel.isSelectedIndex(i)) {
				BikeSession bikeSession = sessionTableModel.getSessionAtRow(i);
				selectedSessions.add(bikeSession);
			}
		}

		if (selectedSessions.size() == 1) {
			jergometer.selectBikeSession(selectedSessions.get(0));
		} else {
			jergometer.selectBikeSessions(selectedSessions);
		}
	}

	// KeyListener by sessionTable and programTree

	public void keyTyped(KeyEvent e) {
	}

	public void keyPressed(KeyEvent e) {
		Object src = e.getSource();
		if (src == sessionTable) {
			if (e.getKeyCode() == KeyEvent.VK_DELETE && e.getModifiers() == 0) {
				deleteSelectedSessions();
				e.consume();
			}
		} else if (src == programTree) {
			if (e.getKeyCode() == KeyEvent.VK_DELETE && e.getModifiers() == 0) {
				deleteProgram();
				e.consume();
			} else if (e.getKeyCode() == KeyEvent.VK_F2) {
				renameProgram();
				e.consume();
			} else if (e.getKeyCode() == KeyEvent.VK_C && e.getModifiers() == KeyEvent.CTRL_MASK) {
				copyProgram();
				e.consume();
			} else if (e.getKeyCode() == KeyEvent.VK_X && e.getModifiers() == KeyEvent.CTRL_MASK) {
				cutProgram();
				e.consume();
			} else if (e.getKeyCode() == KeyEvent.VK_V && e.getModifiers() == KeyEvent.CTRL_MASK) {
				insertProgram();
				e.consume();
			}
		}
	}

	public void keyReleased(KeyEvent e) {
	}

	public void setSessionsVis(Jergometer.SessionsVis sessionsVis) {
		switch (sessionsVis) {
			case average:
				diagramAverageValuesMenuItem.setSelected(true);
				break;
			case progression:
				diagramProgressionMenuItem.setSelected(true);
				break;
		}
	}

	public void setShowOnlyCompletedSessions(boolean value) {
		showOnlyCompletedSessionsMenuItem.setSelected(value);
	}

	public void setShowFullSessionLength(boolean value) {
		showFullSessionLength.setSelected(value);
	}

	public boolean isShowFullSessionLength() {
		return showFullSessionLength.isSelected();
	}

	public void createUIComponents() {
		try {
			bikeInfoPane = new BikeInfoPane(this, getLangTemplate("org/jergometer/gui/templates/default"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Template getLangTemplate(String templateName) throws Exception {
		String cc = Locale.getDefault().getCountry().toLowerCase();
		try {
			return VelocityUtils.getTemplate(templateName + "_" + cc + ".vm");
		} catch (ResourceNotFoundException e) {
			return VelocityUtils.getTemplate(templateName + ".vm");
		}
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		createUIComponents();
		mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());
		final JSplitPane splitPane1 = new JSplitPane();
		splitPane1.setContinuousLayout(false);
		splitPane1.setOneTouchExpandable(true);
		splitPane1.setOrientation(0);
		GridBagConstraints gbc;
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		mainPanel.add(splitPane1, gbc);
		diagram = new Diagram();
		splitPane1.setRightComponent(diagram);
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridBagLayout());
		splitPane1.setLeftComponent(panel1);
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridheight = 2;
		gbc.weightx = 0.5;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 10, 10, 10);
		panel1.add(panel2, gbc);
		panel2.setBorder(BorderFactory.createTitledBorder(ResourceBundle.getBundle("org/jergometer/translation/jergometer").getString("label.training_program")));
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0.5;
		gbc.insets = new Insets(0, 0, 5, 5);
		panel2.add(panel3, gbc);
		recordButton = new JButton();
		recordButton.setIcon(new ImageIcon(getClass().getResource("/org/jergometer/images/record.png")));
		recordButton.setText("");
		recordButton.setToolTipText("start recording training session");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(0, 0, 0, 5);
		panel3.add(recordButton, gbc);
		stopButton = new JButton();
		stopButton.setEnabled(false);
		stopButton.setIcon(new ImageIcon(getClass().getResource("/org/jergometer/images/stop.png")));
		stopButton.setText("");
		stopButton.setToolTipText("stop recording training session");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 5, 0, 0);
		panel3.add(stopButton, gbc);
		final JScrollPane scrollPane1 = new JScrollPane();
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.5;
		gbc.weighty = 0.5;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 5, 5, 5);
		panel2.add(scrollPane1, gbc);
		programTree = new JTree();
		programTree.setRootVisible(true);
		programTree.setShowsRootHandles(false);
		scrollPane1.setViewportView(programTree);
		final JPanel panel4 = new JPanel();
		panel4.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 3;
		gbc.gridy = 0;
		gbc.gridheight = 3;
		gbc.insets = new Insets(0, 0, 0, 10);
		panel1.add(panel4, gbc);
		welcomeLabel = new JLabel();
		welcomeLabel.setFont(new Font("Bitstream Vera Serif", Font.BOLD, 16));
		this.$$$loadLabelText$$$(welcomeLabel, ResourceBundle.getBundle("org/jergometer/translation/jergometer").getString("label.welcome"));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(10, 10, 10, 10);
		panel1.add(welcomeLabel, gbc);
		final JPanel panel5 = new JPanel();
		panel5.setLayout(new GridBagLayout());
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridheight = 3;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(10, 0, 10, 10);
		panel1.add(panel5, gbc);
		panel5.setBorder(BorderFactory.createTitledBorder(ResourceBundle.getBundle("org/jergometer/translation/jergometer").getString("label.training_data")));
		final JScrollPane scrollPane2 = new JScrollPane();
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 0.5;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(0, 5, 5, 5);
		panel5.add(scrollPane2, gbc);
		sessionTable = new JTable();
		scrollPane2.setViewportView(sessionTable);
		bikeInfoPane.setBackground(UIManager.getColor("Label.background"));
		bikeInfoPane.setContentType("text/html");
		bikeInfoPane.setEditable(false);
		bikeInfoPane.setEnabled(true);
		bikeInfoPane.setText("<html>\n  <head>\n\n  </head>\n  <body>\n    <p style=\"margin-top: 0\">\n      \n    </p>\n  </body>\n</html>\n");
		bikeInfoPane.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.gridheight = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel1.add(bikeInfoPane, gbc);
	}

	/**
	 * @noinspection ALL
	 */
	private void $$$loadLabelText$$$(JLabel component, String text) {
		StringBuffer result = new StringBuffer();
		boolean haveMnemonic = false;
		char mnemonic = '\0';
		int mnemonicIndex = -1;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '&') {
				i++;
				if (i == text.length()) break;
				if (!haveMnemonic && text.charAt(i) != '&') {
					haveMnemonic = true;
					mnemonic = text.charAt(i);
					mnemonicIndex = result.length();
				}
			}
			result.append(text.charAt(i));
		}
		component.setText(result.toString());
		if (haveMnemonic) {
			component.setDisplayedMnemonic(mnemonic);
			component.setDisplayedMnemonicIndex(mnemonicIndex);
		}
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return mainPanel;
	}

	class JMenuItemSet {
		private String key;
		private String command;
		private List<JMenuItem> menuItems = new LinkedList<JMenuItem>();

		JMenuItemSet(String key, String command) {
			this.key = key;
			this.command = command;
		}

		public JMenuItem next() {
			JMenuItem menuItem = createMenuItem(key);
			menuItem.setActionCommand(command);
			menuItems.add(menuItem);
			return menuItem;
		}

		public void setEnabled(boolean b) {
			for (JMenuItem menuItem : menuItems) {
				menuItem.setEnabled(b);
			}
		}
	}
}
