package star.jspec;
/**
 *   JSpec, an application for displaying and manipulating
 *   astronomical spectra. This version create independent top-level
 *   windows for each plot.
 *
 * @params args command-line list of spectra to display
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since 14-SEP-1999
 */

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

public class JSpecWindows extends JFrame {

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
     *  References to various PlotControlFrames.
     */
    private ArrayList plots = new ArrayList();

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

        //try {
        //    UIManager.setLookAndFeel(
        //        "com.sun.java.swing.plaf.motif.MotifLookAndFeel");
        //} catch (Exception e) {
        //    System.out.println( "Bad look and feel" );
        //}
        new JSpecWindows( args ).setVisible( true );
    }

    /**
     *  Default constructor.
     */
    public JSpecWindows() {

        //  Create the static interface components.
        initComponents();
        pack();
    }

    /**
     *  Create instance initialising spectra list from an arrays of
     *  names.
     *
     *  @param names initial list of spectra
     */
    public JSpecWindows( String[] names ) {

        //  Add spectra to lists.
        if ( names.length > 0 ) {
            for ( int i = 0; i < names.length; i++ ) {
                addSpectrum( names[i], false );
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
     *  Create the static interface.
     */
    protected void initComponents() {

        //  Set main application title.
        setTitle( "JSpec; astronomical spectra plotting tool" );

        //  Application exits when this window is closed.
        addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent evt ) {
                exitWindowEvent( evt );
            }
        });

        //  Use a border layout with menubar at the top, action
        //  buttons at the bottom and a tabbed pane in the middle.
        getContentPane().setLayout( new BorderLayout() );

        //  Create the menubar.
        menuBar = new JMenuBar();

        //  Create and set up the File menu.
        setupFileMenu();

        //  Add panel to contain the action buttons.
        buttonPanel = new JPanel( new FlowLayout() );

        //  Add "exit" button.
        exitButton = (JButton) buttonPanel.add( new JButton( "Exit" ) );
        exitButton.setToolTipText( "Exit the application" );
        exitButton.addActionListener( new ActionListener() {
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
        openMenu.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                openActionEvent( evt );
            }
        });

        //  Add the "save as" item.
        saveMenu = (JMenuItem) fileMenu.add( new JMenuItem( "Save As" ) );
        saveMenu.setToolTipText( "Save the current spectrum to a file" );
        saveMenu.setMnemonic( 'S' );
        saveMenu.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                saveActionEvent( evt );
            }
        });

        //  Add the "print" item.
        printMenu = (JMenuItem) fileMenu.add( new JMenuItem( "Print" ) );
        printMenu.setToolTipText( "Print" );
        printMenu.setMnemonic( 'P' );
        printMenu.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                printActionEvent( evt );
            }
        });

        //  Add the "exit" item.
        exitMenu = (JMenuItem) fileMenu.add( new JMenuItem( "Exit" ) );
        exitMenu.setToolTipText( "Exit the application" );
        exitMenu.setMnemonic( 'X' );
        exitMenu.addActionListener( new ActionListener() {
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
             
             //  Display a spectra in its own window.
             int offset = 50;
             Point lastLocation = new Point( offset, offset );
             for ( int i = 0; i < spectra.size(); i++ ) {
                 
                 // Create spectral display widget.
                 PlotControlFrame plot = null;
                 try {
                     plot = new PlotControlFrame( (String) spectra.get( i ), 
                                                  (String) spectra.get( i ) );
                 } catch (JSpecException e) {
                     
                     //  Exception should generally be reported in a
                     //  dialog. So remove this spectrum from the list 
                     //  and pass on to next.
                     spectra.remove( i );
                     break;
                 }
                 
                 //  Displace Plot slightly so that windows do not
                 //  totally obscure each other.
                 lastLocation.translate( offset, offset );
                 plot.setLocation( lastLocation );

                 //  Add plot to list available.
                 plots.add( plot );

                 //  We'd like to know if the plot window is closed.
                 plot.addWindowListener( new WindowAdapter() {
                    public void windowClosing( WindowEvent evt ) {
                        removePlot( evt.getWindow() );
                    }
                 });
                 lastLocation = plot.getLocation();
             }
         }
     }

    /**
     *  Plot window is closed. Remove this plot as a spectral view.
     */
    protected void removePlot( Window w ) {
        PlotControlFrame deadPlot = (PlotControlFrame) w;
        System.out.println( "Plot id dead: " + deadPlot );
        plots.remove( plots.indexOf( deadPlot ) );
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
    private void exitActionEvent( ActionEvent evt ) {
        System.exit( 0 );
    }

    /**
     *  Request to save the current spectrum.
     */
    private void saveActionEvent( ActionEvent evt ) {
        System.out.println( "Not implemented yet " );
    }

    /**
     *  Request to exit the application.
     */
    private void exitWindowEvent( WindowEvent evt ) {
        System.exit( 0 );
    }

    /**
     *  The open file menu item has been selected.
     */
    private void openActionEvent( ActionEvent evt ) {

        // Create file chooser to open files.
        if ( fileChooser == null ) {
            fileChooser = new JFileChooser(
                             System.getProperty( "user.dir" ) );
        }
        int result = fileChooser.showOpenDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File f = fileChooser.getSelectedFile();
            addSpectrum( f.toString(), true );
        }
    }

    /**
     *  The print menu item has been selected. So select the "current
     *  plot"? and ask it to print a copy of itself.
     */
    private void printActionEvent( ActionEvent evt ) {
        PlotControl p = (PlotControl) ((PlotControlFrame)plots.get(0)).getPlot();
        p.print();
    }
}
