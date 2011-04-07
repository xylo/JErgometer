package org.jergometer.model;

import de.endrullis.utils.StreamUtils;
import de.endrullis.xml.XMLDocument;
import de.endrullis.xml.XMLParser;
import org.jergometer.JergometerSettings;
import org.jergometer.translation.I18n;
import org.jergometer.control.BikeProgram;
import sun.security.action.GetPropertyAction;

import javax.swing.text.MutableAttributeSet;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileFilter;
import java.util.*;

/**
 * Bike program tree as on file system.
 * User objects in this tree can be of type {@link BikeProgramDir} or {@link BikeProgram}.
 */
public class BikeProgramTree extends DefaultTreeModel {
	/** Root element of the tree. */
	private DefaultMutableTreeNode root;
	private HashMap<String,DefaultMutableTreeNode> allProgramNodes = new HashMap<String, DefaultMutableTreeNode>();
	private static final FileFilter FILE_FILTER = new FileFilter() {
		public boolean accept(File file) {
			return !file.isHidden() && (file.isDirectory() || file.getName().endsWith(".xml"));
		}
	};
	private int charsToRemoveFromAbsolutePath;

	public BikeProgramTree() {
		super(null);
		load();
	}

	private void load() {
		File programsDir = new File(JergometerSettings.jergometerProgramsDirName);
		root = new DefaultMutableTreeNode(new BikeProgramDir(programsDir, I18n.getString("label.all_programs"), "."), true);
		charsToRemoveFromAbsolutePath = programsDir.getAbsolutePath().length() + 1;
		updateNode(root);
		setRoot(root);
	}

	/**
	 * Returns the tree node for a given program file or directory.
	 *
	 * @param file program file or directory
	 * @return tree node
	 */
	private DefaultMutableTreeNode getTreeNode(File file) {
		String relativePath = file.getPath().substring(charsToRemoveFromAbsolutePath);
		DefaultMutableTreeNode newNode;
		if (file.isDirectory()) {
			newNode = new DefaultMutableTreeNode(new BikeProgramDir(file, file.getName(), relativePath), true);
			updateNode(newNode);
		}
		else {
			newNode = new DefaultMutableTreeNode(file.getName().substring(0, file.getName().length() - 4), false);

			// load program
			XMLParser parser = new XMLParser();
			try {
				XMLDocument doc = parser.parse(StreamUtils.readXmlStream(new FileInputStream(file)));
				BikeProgram program = new BikeProgram(file, relativePath, new BikeProgramData(doc.getRootElement()));
				newNode.setUserObject(program);
				allProgramNodes.put(program.getProgramName(), newNode);
			} catch (Exception e) {
				System.err.println("Error parsing bike program.");
			}
		}
		return newNode;
	}

	/**
	 * Updates the directories and files related to this node.
	 *
	 * @param rootNode node
	 */
	public void updateNode(DefaultMutableTreeNode rootNode) {
		Object userObject = rootNode.getUserObject();

		if (userObject instanceof BikeProgramDir) {
			BikeProgramDir bikeProgramDir = (BikeProgramDir) userObject;
			File[] files = bikeProgramDir.getFile().listFiles(FILE_FILTER);
			// sort files by file name
			Arrays.sort(files, new Comparator<File>() {
				public int compare(File o1, File o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			// go through both lists like a zipper and equalize them (remove/add entries from/to node)
			for (int nodeIndex=0, fileIndex=0; nodeIndex < rootNode.getChildCount() || fileIndex < files.length;) {
				DefaultMutableTreeNode node = null;
				String nodeFileName = null;
				try {
					node = (DefaultMutableTreeNode) rootNode.getChildAt(nodeIndex);
					nodeFileName = ((HoldsFile) node.getUserObject()).getFile().getName();
				} catch (ArrayIndexOutOfBoundsException ignored)  {}

				File file = fileIndex < files.length ? files[fileIndex] : null;

				int compare;
				if (nodeFileName == null) {
					compare = 1;
				} else
				if (file == null) {
					compare = -1;
				} else {
					compare = nodeFileName.compareTo(file.getName());
				}

				if (compare == 0) {
					// equality -> update next level
					updateNode(node);
					// go to next node and next file
					nodeIndex++;
					fileIndex++;
				} else
				if (compare < 0) {
					// node string > file name -> we could not find corresponding file -> remove the node
					rootNode.remove(nodeIndex);
					// inform listeners about removed nodes
					nodesWereRemoved(rootNode, new int[]{nodeIndex}, new Object[]{node});
				} else
				if (compare > 0) {
					// node string < file name -> we found a new file (one that's not in the node list) -> add it
					rootNode.insert(getTreeNode(file), nodeIndex);
					// inform listeners about inserted nodes
					nodesWereInserted(rootNode, new int[]{nodeIndex});
					// go to next node and next file
					nodeIndex++;
					fileIndex++;
				}
			}
		} else
		if (userObject instanceof BikeProgram) {
		  BikeProgram bikeProgram = (BikeProgram) userObject;
		  DefaultMutableTreeNode newNode = getTreeNode(bikeProgram.getFile());
			rootNode.setUserObject(newNode.getUserObject());
			nodesChanged(rootNode.getParent(), new int[]{rootNode.getParent().getIndex(rootNode)});
		}
	}

	public DefaultMutableTreeNode getProgramNode(String name) {
		return allProgramNodes.get(name);
	}

	/**
	 * Returns the bike program by name.
	 *
	 * @param name name of the bike program
	 * @return bike program
	 */
	public BikeProgram getProgram(String name) {
		return (BikeProgram) allProgramNodes.get(name).getUserObject();
	}

	/**
	 * Deletes the program from file system and from this data structure.
	 * 
	 * @param node program
	 */
	public void deletePrograms(DefaultMutableTreeNode node) {
		DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();

		if (node == root) {
			// convert enum to list
			Enumeration childEnum = node.children();
			ArrayList<DefaultMutableTreeNode> childNodes = new ArrayList<DefaultMutableTreeNode>();
			while (childEnum.hasMoreElements()) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) childEnum.nextElement();
				childNodes.add(childNode);
			}

			// remove nodes recursively
			for (DefaultMutableTreeNode childNode : childNodes) {
				deleteProgramsRecursively(childNode);
			}

			// build index array and object array
			int[] nodeIndices = new int[childNodes.size()];
			for (int i = 0; i < nodeIndices.length; i++) {
				nodeIndices[i] = i;
			}
			Object[] objects = new Object[childNodes.size()];
			objects = childNodes.toArray(objects);

			// inform listeners about removed nodes
			nodesWereRemoved(node, nodeIndices, objects);
		} else

		if (node.getUserObject() instanceof BikeProgram || node.getUserObject() instanceof BikeProgramDir) {
			// remove node recursively
			int nodeIndex = deleteProgramsRecursively(node);

			// inform listeners about removed nodes
			nodesWereRemoved(parentNode, new int[]{nodeIndex}, new Object[]{node});
		}
	}

	private int deleteProgramsRecursively(DefaultMutableTreeNode node) {
		DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
		int nodeIndex = parentNode.getIndex(node);
		parentNode.remove(nodeIndex);

		if (node.getUserObject() instanceof BikeProgram) {
			BikeProgram bikeProgram = (BikeProgram) node.getUserObject();

			allProgramNodes.remove(bikeProgram.getProgramName());

			// delete the program file
			bikeProgram.getFile().delete();
		} else if (node.getUserObject() instanceof BikeProgramDir) {
			BikeProgramDir bikeProgramDir = (BikeProgramDir) node.getUserObject();

			// convert enum to list
			Enumeration childEnum = node.children();
			ArrayList<DefaultMutableTreeNode> childNodes = new ArrayList<DefaultMutableTreeNode>();
			while (childEnum.hasMoreElements()) {
				DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) childEnum.nextElement();
				childNodes.add(childNode);
			}

			// remove nodes recursively
			for (DefaultMutableTreeNode childNode : childNodes) {
				deleteProgramsRecursively(childNode);
			}

			// delete the directory
			bikeProgramDir.getFile().delete();
		}

		return nodeIndex;
	}

	public void copyPrograms(DefaultMutableTreeNode srcNode, DefaultMutableTreeNode destNode) {
		// TODO
		/*
		// ensure destination is a directory
		if (!(destNode.getUserObject() instanceof BikeProgramDir)) return;

		File destDir = ((BikeProgramDir) destNode.getUserObject()).getFile();

		if (srcNode.getUserObject() instanceof BikeProgram) {
			StreamUtils
		} else if (srcNode.getUserObject() instanceof BikeProgramDir) {
		}
		*/
	}

	// getters and setters
	public DefaultMutableTreeNode getRoot() {
		return root;
	}
}
