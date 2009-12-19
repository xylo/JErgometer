package org.jergometer.gui;

import org.jergometer.model.HoldsFile;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.io.File;

/**
 * Better TreeCellRenderer.
 * This TreeCellRenderer displays a node as directory if it allows children.
 */
public class BetterTreeCellRenderer extends DefaultTreeCellRenderer {
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		if (value instanceof DefaultMutableTreeNode) {
		  DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
			leaf = !treeNode.getAllowsChildren();
		}

		return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
	}
}
