package star.jspec;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import star.jspec.imagedata.*;
import star.jspec.plot.*;
import star.jspec.util.*;

/**
 *  Description of the Class
 *
 *@author     pdraper
 *@created    April 2, 2001
 */
public class JSpec extends JFrame {

	/**
	 *  Reference to the file chooser.
	 */
	protected JFileChooser fileChooser = null;

	/**
	 *  Reference to list of all spectra.
	 */
	protected Vector spectra = new Vector();

	// Variable declarations

	/**
	 *  Reference to action button panel.
	 */
	private JPanel buttonPanel;

	/**
	 *  Reference to exit button.
	 */
	private JButton exitButton;

	/**
	 *  Reference to menubar.
	 */
	private JMenuBar menuBar;

	/**
	 *  Reference to file menu button.
	 */
	private JMenu fileMenu;

	/**
	 *  Reference to open file menu item.
	 */
	private JMenuItem openMenu;

	/**
	 *  Reference to the print menu item.
	 */
	private JMenuItem printMenu;

	/**
	 *  Reference to save as menu item.
	 */
	private JMenuItem saveMenu;

	/**
	 *  Reference to exit menu item.
	 */
	private JMenuItem exitMenu;

	/**
	 *  Reference to pane of spectrums.
	 */
	private JDesktopPane mainPane;


	/**
	 *  Default constructor.
	 */
	public JSpec() {

		//  Create the static interface components.
		initComponents();
		pack();
	}


	/**
	 *  Create instance initialising spectra list from an arrays of names.
	 *
	 *@param  names  initial list of spectra
	 */
	public JSpec(String[] names) {

		//  Add spectra to lists.
		if (names.length > 0) {
			for (int i = 0; i < names.length; i++) {
				addSpectrum(names[i], false);
			}
		}

		//  Create the static interface components.
		initComponents();

		//  Make interface visible.
		pack();

		//  Display any spectra that are available.
		displaySpectra();
	}


	/**
	 *  Add a spectrum to the list of all spectra.
	 *
	 *@param  name    The feature to be added to the Spectrum attribute
	 *@param  update  The feature to be added to the Spectrum attribute
	 *@params         name name of spectrum (NDF specification, FITS file etc).
	 */
	public void addSpectrum(String name, boolean update) {
		spectra.add(name);
		if (update) {
			updateSpectra();
		}
	}


	/**
	 *  Add a "File" menu to the menubar. Also adds the standard items.
	 */
	protected void setupFileMenu() {

		//  Create the File menu button.
		fileMenu = (JMenu) menuBar.add(new JMenu("File"));
		fileMenu.setMnemonic('F');
		fileMenu.setToolTipText("File and application controls");

		//  Add the "open" item.
		openMenu = (JMenuItem) fileMenu.add(new JMenuItem("Open"));
		openMenu.setToolTipText("Open a file to obtain a spectrum");
		openMenu.setMnemonic('O');
		openMenu.addActionListener(
			new ActionListener() {
				/**
				 *  Description of the Method
				 *
				 *@param  evt  Description of Parameter
				 */
				public void actionPerformed(ActionEvent evt) {
					openActionEvent(evt);
				}
			});

		//  Add the "save as" item.
		saveMenu = (JMenuItem) fileMenu.add(new JMenuItem("Save As"));
		saveMenu.setToolTipText("Save the current spectrum to a file");
		saveMenu.setMnemonic('S');
		saveMenu.addActionListener(
			new ActionListener() {
				/**
				 *  Description of the Method
				 *
				 *@param  evt  Description of Parameter
				 */
				public void actionPerformed(ActionEvent evt) {
					saveActionEvent(evt);
				}
			});

		//  Add the "print" item.
		printMenu = (JMenuItem) fileMenu.add(new JMenuItem("Print"));
		printMenu.setToolTipText("Print");
		printMenu.setMnemonic('P');
		printMenu.addActionListener(
			new ActionListener() {
				/**
				 *  Description of the Method
				 *
				 *@param  evt  Description of Parameter
				 */
				public void actionPerformed(ActionEvent evt) {
					printActionEvent(evt);
				}
			});

		//  Add the "exit" item.
		exitMenu = (JMenuItem) fileMenu.add(new JMenuItem("Exit"));
		exitMenu.setToolTipText("Exit the application");
		exitMenu.setMnemonic('X');
		exitMenu.addActionListener(
			new ActionListener() {
				/**
				 *  Description of the Method
				 *
				 *@param  evt  Description of Parameter
				 */
				public void actionPerformed(ActionEvent evt) {
					exitActionEvent(evt);
				}
			});

		//  Register menubar with main JFrame.
		setJMenuBar(menuBar);
	}


	/**
	 *  Create the spectra display widgets. A current list of these is stored in
	 *  the Vector spectra.
	 */
	protected void displaySpectra() {
		if (spectra.size() > 0) {

			//  Display a spectra in its own window.
			int offset = 0;
			for (int i = 0; i < spectra.size(); i++) {

				// Create spectral display widget.
				PlotControl plot = null;
				try {
					plot = new PlotControl((String) spectra.get(i));
				}
				catch (JSpecException e) {

					//  Exception should generally be reported in a
					//  dialog. So remove this spectrum from the list
					//  and pass on to next.
					spectra.remove(i);
					break;
				}

				// Create internal frame as container.
				JInternalFrame jif = new JInternalFrame(String.valueOf(i),
						true, true, true, true);

				//  Set preferred size so that complete spectrum is
				//  generally visible, regardless of the spectrum length.
				plot.setPreferredSize(new Dimension(600, 400));

				// Add spectrum to internal frame.
				jif.getContentPane().add(plot);

				// Add internal frame to displaypane.
				mainPane.add(jif, JDesktopPane.DEFAULT_LAYER);

				//  Displace it slightly so that windows do not
				//  totally obscure each other.
				jif.setLocation(offset, offset);
				offset += 30;

				//  Show the internal frame.
				jif.pack();
				jif.show();
			}
		}
	}


	/**
	 *  Update the display of all spectra. Only need to display new ones.
	 */
	protected void updateSpectra() {
		displaySpectra();
		// for now.
	}


	/**
	 *  Request to exit the application.
	 *
	 *@param  evt  Description of Parameter
	 */
	private void exitActionEvent(ActionEvent evt) {
		System.exit(0);
	}


	/**
	 *  Request to save the current spectrum.
	 *
	 *@param  evt  Description of Parameter
	 */
	private void saveActionEvent(ActionEvent evt) {
		System.out.println("Not implemented yet ");
	}


	/**
	 *  Request to exit the application.
	 *
	 *@param  evt  Description of Parameter
	 */
	private void exitWindowEvent(WindowEvent evt) {
		System.exit(0);
	}


	/**
	 *  The open file menu item has been selected.
	 *
	 *@param  evt  Description of Parameter
	 */
	private void openActionEvent(ActionEvent evt) {

		// Create file chooser to open files.
		if (fileChooser == null) {
			fileChooser = new JFileChooser(
					System.getProperty("user.dir"));
		}
		int result = fileChooser.showOpenDialog(this);
		if (result == fileChooser.APPROVE_OPTION) {
			File f = fileChooser.getSelectedFile();
			addSpectrum(f.toString(), true);
		}
	}


	/**
	 *  The print menu item has been selected. So select the current plot and ask
	 *  it to print a copy of itself.
	 *
	 *@param  evt  Description of Parameter
	 */
	private void printActionEvent(ActionEvent evt) {
		JInternalFrame[] frames = mainPane.getAllFrames();
		for (int i = 0; i < frames.length; i++) {
			System.out.println("Selected:" + frames[i].IS_SELECTED_PROPERTY);
			if (frames[i].IS_SELECTED_PROPERTY == "selected") {
				JComponent c = (JComponent) frames[i].getContentPane();
				PlotControl p = (PlotControl) c.getComponent(0);
				p.print();
			}
		}
	}


	/**
	 *  Application JSpec starts here.
	 *
	 *@param  args  The command line arguments
	 *@params       args command-line initialisations.
	 */
	public static void main(String[] args) {

		//try {
		//    UIManager.setLookAndFeel(
		//        "com.sun.java.swing.plaf.motif.MotifLookAndFeel");
		//} catch (Exception e) {
		//    System.out.println( "Bad look and feel" );
		//}
		new JSpec(args).setVisible(true);
	}

	static {
		System.loadLibrary("ndfstar");
	}
}
