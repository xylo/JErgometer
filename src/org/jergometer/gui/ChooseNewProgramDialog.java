package org.jergometer.gui;

import org.jergometer.control.BikeProgram;
import org.jergometer.model.BikeProgramTree;
import org.jergometer.translation.I18n;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog to assign a new bike program to some sessions.
 */
public class ChooseNewProgramDialog extends JDialog {
// dynamic

	public enum Result { assign, skip, abort }

	private ChooseNewProgramWindow tmpWindow;
	private Result result = Result.abort;
	private BikeProgram selectedBikeProgram;

	public ChooseNewProgramDialog(BikeProgramTree programTree) throws HeadlessException {
		this(null, programTree);
	}

	public ChooseNewProgramDialog(Frame owner, BikeProgramTree programTree) throws HeadlessException {
		super(owner, I18n.getString("choose_new_program_dialog.title"), true);

		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

		// set content pane
		tmpWindow = new ChooseNewProgramWindow();
		tmpWindow.getExplanation().setText(I18n.getString("choose_new_program_dialog.explanation"));
		tmpWindow.getAssignButton().setText(I18n.getString("label.assign"));
		tmpWindow.getSkipButton().setText(I18n.getString("label.skip"));
		setContentPane(tmpWindow.$$$getRootComponent$$$());
		tmpWindow.getBikeProgramTree().setModel(programTree);

		// add listeners
		tmpWindow.getAssignButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				result = Result.assign;
				setVisible(false);
			}
		});
		tmpWindow.getSkipButton().addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				result = Result.skip;
				setVisible(false);
			}
		});
		tmpWindow.getBikeProgramTree().addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				JTree programTree = tmpWindow.getBikeProgramTree();
				if (programTree.getSelectionCount() == 1) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) programTree.getSelectionPath().getLastPathComponent();
					if (node.getUserObject() instanceof BikeProgram) {
						selectedBikeProgram = ((BikeProgram) node.getUserObject());
					}
				}
			}
		});

		// set size and show dialog
		setSize(500, 400);
		setLocationRelativeTo(owner);
	}

	public void openDialog(String oldProgramName) {
		result = Result.abort;
		selectedBikeProgram = null;
		tmpWindow.getHeadline().setText(I18n.getString("choose_new_program_dialog.sessions_uses_unknown_program", oldProgramName));
		tmpWindow.getBikeProgramTree().getSelectionModel().clearSelection();
		setVisible(true);
	}

// getters and setters

	public Result getResult() {
		return result;
	}

	public BikeProgram getSelectedBikeProgram() {
		return selectedBikeProgram;
	}
}
