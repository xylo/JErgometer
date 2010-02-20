package org.jergometer.gui;

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
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Enumeration;
import java.io.IOException;
import java.io.File;

import de.endrullis.utils.SystemUtils;
import de.endrullis.utils.StreamUtils;

/**
 * Main window.
 */
public class MainWindow extends JFrame implements ActionListener, TreeSelectionListener, ListSelectionListener, KeyListener {
	private JPanel mainPanel;
	private JLabel pulseLabel;
	private JLabel speedLabel;
	private JLabel distanceLabel;
	private JLabel destPowerLabel;
	private JLabel pedalRpmLabel;
	private JLabel energyLabel;
	private JLabel timeLabel;
	private JLabel realPowerLabel;
	private Diagram diagram;
	private JTable sessionTable;
	private JTree programTree;
	private JLabel welcomeLabel;
	private JButton recordButton;
	private JButton stopButton;

	// menu items
	private JMenuItem newUserMenuItem;
	private JMenuItem settingsMenuItem;
	private JMenuItem quitMenuItem;
	private JMenuItem aboutMenuItem;
	private JRadioButtonMenuItem diagramAverageValuesMenuItem;
	private JRadioButtonMenuItem diagramProgressionMenuItem;
	private JCheckBoxMenuItem showOnlyCompletedSessionsMenuItem;
	private JMenu userMenu;
	private ButtonGroup userButtonGroup;
	private ArrayList<JMenuItem> userMenuItems = new ArrayList<JMenuItem>();
	private JMenuItem insertProgramMenuItem;

	// popup menus
	private JPopupMenu sessionTablePopup;
	private JPopupMenu programTreePopup;

	private DefaultMutableTreeNode copiedProgramNode = null;
	private boolean movePrograms = false;

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
		setContentPane(mainPanel);
		setJMenuBar(createMenuBar());
		createPopups();

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
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					int row = programTree.getRowForLocation(e.getX(), e.getY());
					programTree.setSelectionRow(row);
					programTreePopup.show(programTree, e.getX(), e.getY());
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
		pulseLabel.setText(dataRecord.getPulse() + "");
		pedalRpmLabel.setText(dataRecord.getPedalRpm() + "");
		speedLabel.setText(dataRecord.getSpeed() + "");
		distanceLabel.setText(dataRecord.getDistance() + "");
		destPowerLabel.setText(dataRecord.getDestPower() + "");
		energyLabel.setText(dataRecord.getEnergy() + "");
		timeLabel.setText(dataRecord.getTime());
		realPowerLabel.setText(dataRecord.getRealPower() + "");
	}

	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		// File
		{
			JMenu menu = new JMenu(I18n.getString("menu.file"));
			menu.setMnemonic(I18n.getMnemonic("menu.file_mn"));
			menuBar.add(menu);
			newUserMenuItem = new JMenuItem(I18n.getString("menu.file.new_user"));
			newUserMenuItem.setMnemonic(I18n.getMnemonic("menu.file.new_user_mn"));
			newUserMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.file.new_user_ks")));
			newUserMenuItem.addActionListener(this);
			menu.add(newUserMenuItem);
			settingsMenuItem = new JMenuItem(I18n.getString("menu.file.settings"));
			settingsMenuItem.setMnemonic(I18n.getMnemonic("menu.file.settings_mn"));
			settingsMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.file.settings_ks")));
			settingsMenuItem.addActionListener(this);
			menu.add(settingsMenuItem);
			quitMenuItem = new JMenuItem(I18n.getString("menu.file.quit"));
			quitMenuItem.setMnemonic(I18n.getMnemonic("menu.file.quit_mn"));
			quitMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.file.quit_ks")));
			quitMenuItem.addActionListener(this);
			menu.add(quitMenuItem);
		}

		// User
		{
			userMenu = new JMenu(I18n.getString("menu.user"));
			userMenu.setMnemonic(I18n.getMnemonic("menu.user_mn"));
			menuBar.add(userMenu);
		}

		// Sessions
		{
			JMenu sessionsMenu = new JMenu(I18n.getString("menu.sessions"));
			sessionsMenu.setMnemonic(I18n.getMnemonic("menu.sessions_mn"));
			menuBar.add(sessionsMenu);
			ButtonGroup group = new ButtonGroup();
			diagramAverageValuesMenuItem = new JRadioButtonMenuItem(I18n.getString("menu.sessions.diagram_average_values"));
			diagramAverageValuesMenuItem.setMnemonic(I18n.getMnemonic("menu.sessions.diagram_average_values_mn"));
			diagramAverageValuesMenuItem.setSelected(true);
			diagramAverageValuesMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.sessions.diagram_average_values_ks")));
			diagramAverageValuesMenuItem.addActionListener(this);
			group.add(diagramAverageValuesMenuItem);
			sessionsMenu.add(diagramAverageValuesMenuItem);
			diagramProgressionMenuItem = new JRadioButtonMenuItem(I18n.getString("menu.sessions.diagram_progression"));
			diagramProgressionMenuItem.setMnemonic(I18n.getMnemonic("menu.sessions.diagram_progression_mn"));
			diagramProgressionMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.sessions.diagram_progression_ks")));
			diagramProgressionMenuItem.addActionListener(this);
			group.add(diagramProgressionMenuItem);
			sessionsMenu.add(diagramProgressionMenuItem);
			sessionsMenu.addSeparator();
			showOnlyCompletedSessionsMenuItem = new JCheckBoxMenuItem(I18n.getString("menu.sessions.show_only_completed"));
			showOnlyCompletedSessionsMenuItem.setMnemonic(I18n.getMnemonic("menu.sessions.show_only_completed_mn"));
			showOnlyCompletedSessionsMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.sessions.show_only_completed_ks")));
			showOnlyCompletedSessionsMenuItem.addActionListener(this);
			sessionsMenu.add(showOnlyCompletedSessionsMenuItem);
			JMenuItem selectAllSessionsMenuItem = new JMenuItem(I18n.getString("menu.sessions.select_all"));
			selectAllSessionsMenuItem.setActionCommand(AC_SELECT_ALL_SESSIONS);
			selectAllSessionsMenuItem.setMnemonic(I18n.getMnemonic("menu.sessions.select_all_mn"));
			selectAllSessionsMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.sessions.select_all_ks")));
			selectAllSessionsMenuItem.addActionListener(this);
			sessionsMenu.add(selectAllSessionsMenuItem);
			JMenuItem deleteSelectedSessionsMenuItem = new JMenuItem(I18n.getString("menu.sessions.delete_selected"));
			deleteSelectedSessionsMenuItem.setActionCommand(AC_DELETE_SELECTED_SESSIONS);
			deleteSelectedSessionsMenuItem.setMnemonic(I18n.getMnemonic("menu.sessions.delete_selected_mn"));
//			deleteSelectedSessionsMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.sessions.delete_selected_all_ks")));
			deleteSelectedSessionsMenuItem.addActionListener(this);
			sessionsMenu.add(deleteSelectedSessionsMenuItem);
			JMenuItem reparseUserDataMenuItem = new JMenuItem(I18n.getString("menu.sessions.reparse_user_data"));
			reparseUserDataMenuItem.setActionCommand(AC_REPARSE_USER_DATA);
			reparseUserDataMenuItem.setMnemonic(I18n.getMnemonic("menu.sessions.reparse_user_data_mn"));
			reparseUserDataMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.sessions.reparse_user_data_ks")));
			reparseUserDataMenuItem.addActionListener(this);
			sessionsMenu.add(reparseUserDataMenuItem);
		}

		// Help
		{
			JMenu menu = new JMenu(I18n.getString("menu.help"));
			menu.setMnemonic(I18n.getMnemonic("menu.help_mn"));
			menuBar.add(menu);
			aboutMenuItem = new JMenuItem(I18n.getString("menu.help.about"));
			aboutMenuItem.setMnemonic(I18n.getMnemonic("menu.help.about_mn"));
			aboutMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.help.about_ks")));
			aboutMenuItem.addActionListener(this);
			menu.add(aboutMenuItem);
		}

		return menuBar;
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
			JMenuItem editProgramMenuItem = new JMenuItem(I18n.getString("menu.programs.edit"));
			editProgramMenuItem.setActionCommand(AC_EDIT_PROGRAM);
			editProgramMenuItem.setMnemonic(I18n.getMnemonic("menu.programs.edit_mn"));
			editProgramMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.programs.edit_ks")));
			editProgramMenuItem.addActionListener(this);
			programTreePopup.add(editProgramMenuItem);
			JMenuItem renameProgramMenuItem = new JMenuItem(I18n.getString("menu.programs.rename"));
			renameProgramMenuItem.setActionCommand(AC_RENAME_PROGRAM);
			renameProgramMenuItem.setMnemonic(I18n.getMnemonic("menu.programs.rename_mn"));
			renameProgramMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.programs.rename_ks")));
			renameProgramMenuItem.addActionListener(this);
			programTreePopup.add(renameProgramMenuItem);
			JMenuItem createNewProgramDirectory = new JMenuItem(I18n.getString("menu.programs.create_new_directory"));
			createNewProgramDirectory.setActionCommand(AC_CREATE_NEW_PROGRAM_DIRECTORY);
			createNewProgramDirectory.setMnemonic(I18n.getMnemonic("menu.programs.create_new_directory_mn"));
			createNewProgramDirectory.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.programs.create_new_directory_ks")));
			createNewProgramDirectory.addActionListener(this);
			programTreePopup.add(createNewProgramDirectory);
			JMenuItem copyProgramMenuItem = new JMenuItem(I18n.getString("menu.programs.copy"));
			copyProgramMenuItem.setActionCommand(AC_COPY_PROGRAM);
			copyProgramMenuItem.setMnemonic(I18n.getMnemonic("menu.programs.copy_mn"));
			copyProgramMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.programs.copy_ks")));
			copyProgramMenuItem.addActionListener(this);
			programTreePopup.add(copyProgramMenuItem);
			JMenuItem cutProgramDataMenuItem = new JMenuItem(I18n.getString("menu.programs.cut"));
			cutProgramDataMenuItem.setActionCommand(AC_CUT_PROGRAM);
			cutProgramDataMenuItem.setMnemonic(I18n.getMnemonic("menu.programs.cut_mn"));
			cutProgramDataMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.programs.cut_ks")));
			cutProgramDataMenuItem.addActionListener(this);
			programTreePopup.add(cutProgramDataMenuItem);
			insertProgramMenuItem = new JMenuItem(I18n.getString("menu.programs.insert"));
			insertProgramMenuItem.setActionCommand(AC_INSERT_PROGRAM);
			insertProgramMenuItem.setMnemonic(I18n.getMnemonic("menu.programs.insert_mn"));
			insertProgramMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.programs.insert_ks")));
			insertProgramMenuItem.setEnabled(false);
			insertProgramMenuItem.addActionListener(this);
			programTreePopup.add(insertProgramMenuItem);
			JMenuItem deleteProgramMenuItem = new JMenuItem(I18n.getString("menu.programs.delete"));
			deleteProgramMenuItem.setActionCommand(AC_DELETE_PROGRAM);
			deleteProgramMenuItem.setMnemonic(I18n.getMnemonic("menu.programs.delete_mn"));
//			deleteProgramMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.programs.delete_ks")));
			deleteProgramMenuItem.addActionListener(this);
			programTreePopup.add(deleteProgramMenuItem);
			JMenuItem updateProgramMenuItem = new JMenuItem(I18n.getString("menu.programs.update"));
			updateProgramMenuItem.setActionCommand(AC_UPDATE_PROGRAM);
			updateProgramMenuItem.setMnemonic(I18n.getMnemonic("menu.programs.update_mn"));
			updateProgramMenuItem.setAccelerator(KeyStroke.getKeyStroke(I18n.getString("menu.programs.update_ks")));
			updateProgramMenuItem.addActionListener(this);
			programTreePopup.add(updateProgramMenuItem);
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
			System.out.println(showOnlyCompletedSessionsMenuItem.isSelected());
			jergometer.setShowOnlyCompletedSessions(showOnlyCompletedSessionsMenuItem.isSelected());
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
				} else if (node.getUserObject() instanceof BikeProgramDir) {
					jergometer.selectBikeProgramDirectory((BikeProgramDir) node.getUserObject());
				} else if (node.getUserObject() instanceof BikeProgram) {
					jergometer.selectBikeProgram((BikeProgram) node.getUserObject());
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
			}
		} else if (src == programTree) {
			if (e.getKeyCode() == KeyEvent.VK_F4) {
				editProgram();
			} else if (e.getKeyCode() == KeyEvent.VK_F2) {
				renameProgram();
			} else if (e.getKeyCode() == KeyEvent.VK_C && e.getModifiers() == KeyEvent.CTRL_MASK) {
				copyProgram();
			} else if (e.getKeyCode() == KeyEvent.VK_X && e.getModifiers() == KeyEvent.CTRL_MASK) {
				cutProgram();
			} else if (e.getKeyCode() == KeyEvent.VK_V && e.getModifiers() == KeyEvent.CTRL_MASK) {
				insertProgram();
			} else if (e.getKeyCode() == KeyEvent.VK_DELETE && e.getModifiers() == 0) {
				deleteProgram();
			} else if (e.getKeyCode() == KeyEvent.VK_F5) {
				updateProgram();
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

	{
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer
	 * >>> IMPORTANT!! <<<
	 * DO NOT edit this method OR call it in your code!
	 *
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
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
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.gridheight = 3;
		gbc.insets = new Insets(0, 0, 0, 10);
		panel1.add(panel4, gbc);
		final JLabel label1 = new JLabel();
		this.$$$loadLabelText$$$(label1, ResourceBundle.getBundle("org/jergometer/translation/jergometer").getString("property.pulse"));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(label1, gbc);
		final JLabel label2 = new JLabel();
		this.$$$loadLabelText$$$(label2, ResourceBundle.getBundle("org/jergometer/translation/jergometer").getString("property.pedal_rpm"));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(label2, gbc);
		final JLabel label3 = new JLabel();
		this.$$$loadLabelText$$$(label3, ResourceBundle.getBundle("org/jergometer/translation/jergometer").getString("property.speed"));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(label3, gbc);
		final JLabel label4 = new JLabel();
		this.$$$loadLabelText$$$(label4, ResourceBundle.getBundle("org/jergometer/translation/jergometer").getString("property.distance"));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(label4, gbc);
		final JLabel label5 = new JLabel();
		this.$$$loadLabelText$$$(label5, ResourceBundle.getBundle("org/jergometer/translation/jergometer").getString("property.dest_power"));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(label5, gbc);
		final JLabel label6 = new JLabel();
		this.$$$loadLabelText$$$(label6, ResourceBundle.getBundle("org/jergometer/translation/jergometer").getString("property.energy"));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 5;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(label6, gbc);
		final JLabel label7 = new JLabel();
		this.$$$loadLabelText$$$(label7, ResourceBundle.getBundle("org/jergometer/translation/jergometer").getString("property.time"));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 6;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(label7, gbc);
		final JLabel label8 = new JLabel();
		this.$$$loadLabelText$$$(label8, ResourceBundle.getBundle("org/jergometer/translation/jergometer").getString("property.real_power"));
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 7;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(label8, gbc);
		destPowerLabel = new JLabel();
		destPowerLabel.setText("-");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 4;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(destPowerLabel, gbc);
		pulseLabel = new JLabel();
		pulseLabel.setText("-");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(pulseLabel, gbc);
		pedalRpmLabel = new JLabel();
		pedalRpmLabel.setText("-");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(pedalRpmLabel, gbc);
		speedLabel = new JLabel();
		speedLabel.setText("-");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(speedLabel, gbc);
		distanceLabel = new JLabel();
		distanceLabel.setText("-");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(distanceLabel, gbc);
		energyLabel = new JLabel();
		energyLabel.setText("-");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 5;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(energyLabel, gbc);
		timeLabel = new JLabel();
		timeLabel.setText("-");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 6;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(timeLabel, gbc);
		realPowerLabel = new JLabel();
		realPowerLabel.setText("-");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 7;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 5, 5, 5);
		panel4.add(realPowerLabel, gbc);
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
}
