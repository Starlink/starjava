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

/**
 *   JSpec -- an application for displaying and manipulating
 *   astronomical spectra. 
 *
 * @params args command-line list of spectra to display
 *
 * @author Peter W. Draper
 * @version 0.0
 * @since 14-SEP-1999
 */
public class JSpec_with_tabs extends JFrame {
    
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
     *  Reference to save as menu item.
     */
    private JMenuItem saveMenu;

    /**
     *  Reference to exit menu item.
     */
    private JMenuItem exitMenu;

    /**
     *  Reference to tabbed pane of spectrums.
     */
    private JTabbedPane tabbedPane;

    /**
     *  Reference to the file chooser.
     */
    protected JFileChooser fileChooser = null;
    
    /**
     *  Reference to list of all spectra.
     */
    protected Vector spectra = new Vector();

    /**
     *  Load the shareable library that contains the NDF and AST
     *  code and all their dependencies.
     */
    static {
        System.loadLibrary( "ndfstar" );
    }

    /**
     *  Application JSpec starts here.
     *
     *  @params args command-line initialisations.
     *
     */
    public static void main( String[] args) {
        new JSpec_with_tabs ( args ).show();
    }

    /**
     *  Default constructor.
     */
    public JSpec_with_tabs() {
        
        //  Create the static interface components.
        initComponents ();
        pack ();
    }
    
    /**
     *  Create instance initialising spectra list from an arrays of
     *  names.
     *
     *  @param names initial list of spectra
     */
    public JSpec_with_tabs( String[] names ) {
        
        //  Add spectra to lists.
        if ( names.length > 0 ) {
            for ( int i = 0; i < names.length; i++ ) {
                addSpectrum( names[i], false );
            }
        }

        //  Create the static interface components.
        initComponents ();

        //  Display any spectra that are available.
        displaySpectra();

        //  Make interface visible.
        pack();
    }

    /**
     *  Create the static interface.
     */
    protected void initComponents () {

        //  Set main application title.
        setTitle ("JSpec; astronomical spectra plotting tool");

        //  Application exits when this window is closed.
        addWindowListener ( new WindowAdapter () {
            public void windowClosing ( WindowEvent evt ) {
                exitWindowEvent( evt );
            }
        });

        //  Use a border layout with menubar at the top, action
        //  buttons at the bottom and a tabbed pane in the middle.
        getContentPane().setLayout ( new BorderLayout () );
        
        //  Create the menubar.
        menuBar = new JMenuBar ();

        //  Create and set up the File menu.
        setupFileMenu();

        //  Create a tabbed pane to display all the spectra.
        tabbedPane = new JTabbedPane();
        tabbedPane.setPreferredSize( new Dimension(600, 200) );
        tabbedPane.setMinimumSize( new Dimension(100, 50) );
        tabbedPane.setName( "Spectra display region" );
        tabbedPane.setBorder( new LineBorder( Color.black ) );
        tabbedPane.setToolTipText(
	    "Tabbed pane of spectra. Select using side tabs");
        
        //  Add the tabbed pane.
        getContentPane().add( tabbedPane, "Center" );
        
        //  Add panel to contain the action buttons.
        buttonPanel = new JPanel( new FlowLayout() );
        
        //  Add "exit" button.
        exitButton = (JButton) buttonPanel.add( new JButton( "Exit" ) );
        exitButton.setToolTipText( "Exit the application" );
        exitButton.addActionListener (new ActionListener () {
            public void actionPerformed (ActionEvent evt) {
                exitActionEvent (evt);
            }
        } );

	//  Add button pane to basic frame.
        getContentPane().add( buttonPanel, "South" );
    }
    
    /**
     *  Add a "File" menu to the menubar. Also adds the standard 
     *  items.
     */
    protected void setupFileMenu() {

        //  Create the File menu button.
        fileMenu = (JMenu) menuBar.add( new JMenu( "File") );
        fileMenu.setMnemonic( 'F' );
        fileMenu.setToolTipText( "File and application controls" );

        //  Add the "open" item.
        openMenu = (JMenuItem) fileMenu.add( new JMenuItem( "Open" ) );
        openMenu.setToolTipText( "Open a file to obtain a spectrum" );
        openMenu.setMnemonic( 'O' );
        openMenu.addActionListener( new ActionListener () {
            public void actionPerformed( ActionEvent evt ) {
                openActionEvent( evt );
            }
        });

        //  Add the "save as" item.
        saveMenu = (JMenuItem) fileMenu.add( new JMenuItem( "Save As" ) );
        saveMenu.setToolTipText( "Save the current spectrum to a file" );
        saveMenu.setMnemonic( 'S' );
        saveMenu.addActionListener( new ActionListener () {
            public void actionPerformed( ActionEvent evt ) {
                saveActionEvent( evt );
            }
        });

        //  Add the "exit" item.
        exitMenu = (JMenuItem) fileMenu.add( new JMenuItem( "Exit" ) );
        exitMenu.setToolTipText( "Exit the application" );
        exitMenu.setMnemonic( 'X' );
        exitMenu.addActionListener( new ActionListener () {
            public void actionPerformed( ActionEvent evt ) {
                exitActionEvent( evt );
            }
        });

        //  Register menubar with main JFrame.
        setJMenuBar(menuBar);
    }


    /**
     *  Create the spectra display widgets. A current list of these is 
     *  stored in the Vector spectra.
     */
    protected void displaySpectra() {
        if ( spectra.size() > 0 ) {
            
            //  Add an PlotControl object.
	    for ( int i = 0; i < spectra.size(); i++ ) {
                PlotControl plot = 
                    new PlotControl( (String) spectra.get( i ) );
		tabbedPane.addTab( String.valueOf( i ), null, plot );
	    }
        }
    }

    /**
     *  Update the display of all spectra. Only need to display new ones.
     */
    protected void updateSpectra() {
        displaySpectra(); // for now.
    }

    /**
     *  Add a spectrum to the list of all spectra.
     *
     *  @params name name of spectrum (NDF specification, FITS file etc).
     */
    public void addSpectrum( String name, boolean update ) {
        spectra.add( name );
	if ( update ) {
	  updateSpectra();
	}
    }
    
    /**
     *  Request to exit the application. 
     */
    private void exitActionEvent(ActionEvent evt) {
        System.exit( 0 );
    }
    
    /**
     *  Request to save the current spectrum. 
     */
    private void saveActionEvent(ActionEvent evt) {
        System.out.println( "Not implemented yet " );
    }
    
    /**
     *  Request to exit the application. 
     */
    private void exitWindowEvent(WindowEvent evt) {
        System.exit( 0 );
    }
    
    /**
     *  The open file menu item has been selected.
     */
    private void openActionEvent (ActionEvent evt) {
        
        // Create file chooser to open files.
        if ( fileChooser == null ) {
            fileChooser = new JFileChooser( System.getProperty("user.dir") );
        }
        if ( fileChooser.showOpenDialog( this )  ==
             fileChooser.APPROVE_OPTION ) {
            File f = fileChooser.getSelectedFile();
            addSpectrum( f.toString(), true );
        }
    }
    
}
