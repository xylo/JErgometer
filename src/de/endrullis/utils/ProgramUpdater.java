package de.endrullis.utils;

import de.endrullis.xml.XMLDocument;
import de.endrullis.xml.XMLElement;
import de.endrullis.xml.XMLException;
import de.endrullis.xml.XMLParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a simple and universal program updater.
 *
 * @author Stefan Endrullis
 */
public class ProgramUpdater extends JFrame implements ActionListener {
	public static final String VERSIONS_FILE_NAME = "versions.xml";
	public static final String UPDATE_DIR = "update";

	private JProgressBar progressBar = new JProgressBar();
	private JButton abortButton = new JButton("Abort");

	private String urlPrefix;
	private HashMap<String, XMLElement> files = new HashMap<String,XMLElement>();
	private ArrayList<String> files2download = new ArrayList<String>();
	private int totalSize = 0;
	private int currentSize = 0;

	private InputStream in = null;
	private OutputStream out = null;
	private boolean abort = false;

	/**
	 * Creates an update program (window and download thread).
	 * @param title title of the updater
	 * @param urlPrefix URL prefix where the update files are located
	 */
	public ProgramUpdater(String title, String urlPrefix) throws HeadlessException {
		super(title);
		this.urlPrefix = urlPrefix;

	  setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

	  Container cp = getContentPane();
	  GridBagLayout layout = new GridBagLayout();
	  GridBagConstraints constraints = new GridBagConstraints();
	  cp.setLayout(layout);

	  abortButton.addActionListener(this);

	  constraints.fill = GridBagConstraints.HORIZONTAL;
	  constraints.insets = new Insets(5, 5, 5, 5);

	  constraints.anchor = GridBagConstraints.CENTER;
	  constraints.gridy = 0;
	  constraints.gridx = 0; cp.add(progressBar, constraints);
	  constraints.gridx = 1; cp.add(abortButton, constraints);
	  abortButton.setVisible(true);

	  pack();
	  setLocationRelativeTo(null);
	}

	/**
	 * Checks for new versions.
	 *
	 * @return true if a new version is available
	 */
	public boolean isNewVersionAvailable() {
		try {
			compareVersions();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return totalSize > 0;
	}

	/**
	 * Performs the update process and returns true if it was successful.
	 *
	 * @param confirmation if true a dialog is shown if the update process was successful
	 * @return true if the update was successful
	 */
	public boolean performUpdate(boolean confirmation) {
		setAlwaysOnTop(true);
		setVisible(true);

	  try {
	    compareVersions();
	    downloadFiles();
	    writeUpdateVersionsFile();

	    // if windows we have to and here and go on with call of AfterUpdate
	    if(SystemUtils.isWinOS()) {
	      System.out.println("Bad OS");
	      moveAfterUpdate();
		    // TODO
	      //FootWizard.startApplication("AfterUpdate", new String[]{AFTER_UPDATE_JAR_NAME}, "");
	    } else {
		    moveFiles();
	    }

	    if(totalSize > 0) {
	      if(confirmation) {
	        JOptionPane.showMessageDialog(this, "Update was successful. The program will be restarted.");
	      }
		    setVisible(false);
		    dispose();
		    return true;
	    } else {
	      if(confirmation) {
	        JOptionPane.showMessageDialog(this, "The software is already up-to-date.");
	      }
		    setVisible(false);
		    dispose();
		    return false;
	    }
	  } catch (Exception e) {
	    JOptionPane.showMessageDialog(this, "Update failed.", "Update", JOptionPane.WARNING_MESSAGE);

	    setVisible(false);
	    dispose();
	    return false;
	  }
	}

	/**
	 * Compares the versions of the local files with the versions of the web files and
	 * adds all files needed to download to the list <code>files2download</code>.
	 */
	private void compareVersions() throws XMLException, IOException {
	  XMLParser parser = new XMLParser();

		totalSize = 0;
		files.clear();
		files2download.clear();

	  // open local versions.xml
	  try {
	    XMLDocument doc = parser.parse(StreamUtils.readXmlStream(new FileInputStream(VERSIONS_FILE_NAME)));
	    for (XMLElement fileXml : doc.getRootElement().getChildElements()) {
	      files.put(fileXml.getAttribute("name"), fileXml);
	    }
	  } catch (Exception ignored) {}

	  // load versions.xml from the web
	  XMLDocument doc = parser.parse(StreamUtils.readXmlStream(getInputStream(VERSIONS_FILE_NAME)));

	  for (XMLElement fileXml : doc.getRootElement().getChildElements()) {
	    String name = fileXml.getAttribute("name");
	    String webVersion = fileXml.getAttribute("version");
	    if (files.containsKey(name)) {
	      // compare versions
	      String localVersion = files.get(name).getAttribute("version");
	      if (!localVersion.equals(webVersion)) {
	        //System.out.println("Want to update " + name + " from version " + localVersion + " to version " + webVersion);
	        files.put(name, fileXml);
	        files2download.add(name);
	        totalSize += Integer.parseInt(fileXml.getAttribute("length"));
	      }
	    } else {
	      String os = fileXml.getAttribute("os");
	      if(os == null || os.indexOf(SystemUtils.getSimpleOSType()) != -1) {
	        //System.out.println("Want to download " + name + " version " + webVersion);
	        // download the file
	        files.put(name, fileXml);
	        files2download.add(name);
	        totalSize += Integer.parseInt(fileXml.getAttribute("length"));
	      }
	    }
	  }
	}

	/**
	 * Loads the files from the web.
	 */
	private void downloadFiles() throws IOException, InterruptedException {
	  // create update dir if not exists
	  File updateDir = new File(UPDATE_DIR);
	  if(!updateDir.exists()) updateDir.mkdir();

	  for (String filename : files2download) {
		  File outFile = new File(UPDATE_DIR + "/" + filename);
		  outFile.getParentFile().mkdirs();
	    out = new FileOutputStream(outFile);
	    in = getInputStream(filename);

	    // read and write
	    byte[] bytes = new byte[1024];
	    int count;
	    int percCount = 0;
	    int percUpdate = (totalSize/200/bytes.length) + 1;
	    updatePercentage();
	    while((count = in.read(bytes)) != -1){
	      out.write(bytes, 0, count);
	      currentSize += count;

	      percCount++;
	      if(percCount == percUpdate){
	        updatePercentage();
	        percCount = 0;
	      }

	      if(abort) {
	        throw new InterruptedException();
	      }
	    }
	    in.close();
	    out.close();
	  }
	}

	/**
	 * Overwrites the old after_update.jar with the new one (from update dir).
	 */
	private void moveAfterUpdate() {
		/* TODO
	  File afterUpdateJar = new File(UPDATE_DIR + "/" + AFTER_UPDATE_JAR_NAME);
	  if (afterUpdateJar.exists()) {
	    new File(AFTER_UPDATE_JAR_NAME).delete();
	    afterUpdateJar.renameTo(new File(AFTER_UPDATE_JAR_NAME));
	  }
	  */
	}

	/**
	 * Overwrites the old files with the new one (from update dir).
	 */
	public static void moveFiles() {
	  File updateDir = new File(UPDATE_DIR);
	  if (!updateDir.isDirectory()) return;

	  // move files in update dir to .
	  File[] files2move = updateDir.listFiles();
		assert files2move != null;
		for (File file : files2move) {
			File destFile = new File(file.getName());
			file.renameTo(destFile);
			if (file.getName().endsWith(".sh") && !SystemUtils.isWinOS()) {
				try {
					SystemUtils.exec(new String[] {"chmod", "uog+x", destFile.getAbsolutePath()});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
	  }

	  // remove the update dir
	  updateDir.delete();
	}

	/**
	 * Writes the new version file.
	 * For the time being the file is located in update dir and will be moved
	 * later (with AfterUpdate if windows) to . dir.
	 */
	private void writeUpdateVersionsFile() throws IOException {
	  XMLDocument doc = new XMLDocument();
	  XMLElement filesXml = new XMLElement("files");
	  for (XMLElement fileElement : files.values()) {
	    filesXml.addChildElement(fileElement);
	  }
	  doc.setRootElement(filesXml);

	  FileWriter fileWriter = new FileWriter(UPDATE_DIR + "/" + VERSIONS_FILE_NAME);
	  fileWriter.write(doc.toString());
	  fileWriter.close();
	}

	/**
	 * Updates the percentage.
	 */
	private void updatePercentage() {
	  progressBar.setValue((100*currentSize/totalSize));
		progressBar.repaint();
	}

	private InputStream getInputStream(String fileName) throws IOException {
	  return new URL(urlPrefix + fileName).openStream();
	}

	public void actionPerformed(ActionEvent e) {
	  if(e.getSource() == abortButton) {
	    abort();
	  }
	}

	private void abort() {
	  try {
	    abort = true;
	    if(in != null) in.close();
	    if(out != null) out.close();
	  } catch (IOException ignored) {}
	}
}
