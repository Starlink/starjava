package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.AsciiFileParser;
import uk.ac.starlink.splat.util.Utilities;

/**
 * Provides a toolbox with number of ways to cut out parts of a
 * spectrum. The cut becomes a new memory spectrum on the global list.
 *
 * @since $Date$
 * @since 15-JUN-2001
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2001 Central Laboratory of the Research Councils
 * @see SpecCutter
 */
public class SpecCutterFrame extends JFrame
{
    /**
     * List of spectra that we have created.
     */
    protected ArrayList localList = new ArrayList( 5 );

    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     * Action buttons container.
     */
    protected JPanel actionBar = new JPanel();

    /**
     *  Menubar and various menus and items that it contains.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu fileMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();
    protected JMenu helpMenu = new JMenu();
    protected JMenuItem helpMenuAbout = new JMenuItem();

    /**
     *  The PlotControl that is displaying the current spectrum.
     */
    protected PlotControl plot = null;

    /**
     *  Ranges of the spectrum.
     */
    protected XGraphicsRangesView rangeList = null;

    /**
     *  Reference to GlobalSpecPlotList object.
     */
    protected GlobalSpecPlotList globalList =
        GlobalSpecPlotList.getReference();

    /**
     * File chooser used for reading ranges from text files.
     */
    protected JFileChooser fileChooser = null;

    /**
     * Create an instance.
     */
    public SpecCutterFrame( PlotControl plot )
    {
        contentPane = (JPanel) getContentPane();
        setPlot( plot );
        initUI();
        initFrame();
    }

    /**
     * Get the PlotControl that we are using.
     *
     * @return the PlotControl
     */
    public PlotControl getPlot()
    {
        return plot;
    }

    /**
     * Set the PlotControlFrame that has the spectrum that we are to
     * cut.
     *
     * @param plot the PlotControl reference.
     */
    public void setPlot( PlotControl plot )
    {
        this.plot = plot;
    }

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {
        //  The layout is a BorderLayout with the list of regions in
        //  the center and the toolbox below this.
        contentPane.setLayout( new BorderLayout() );

        //  Add the list of regions.
        rangeList = new XGraphicsRangesView( plot.getPlot() );
        contentPane.add( rangeList, BorderLayout.CENTER );

        //  Add the menuBar.
        setJMenuBar( menuBar );

        //  Action bar uses a BoxLayout and is placed at the south.
        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        contentPane.add( actionBar, BorderLayout.SOUTH );

        //  Get icons.
        ImageIcon closeImage = new ImageIcon(
            ImageHolder.class.getResource( "exit.gif" ) );
        ImageIcon readImage = new ImageIcon(
            ImageHolder.class.getResource( "read.gif" ) );
        ImageIcon resetImage = new ImageIcon(
            ImageHolder.class.getResource( "reset.gif" ) );
        ImageIcon cutterImage = new ImageIcon(
            ImageHolder.class.getResource( "cutter.gif" ) );
        ImageIcon helpImage = new ImageIcon(
            ImageHolder.class.getResource( "help.gif" ) );

        //  Create the File menu.
        fileMenu.setText( "File" );
        menuBar.add( fileMenu );

        //  Add action to do read a list of ranges from disk file.
        ReadAction readAction = new ReadAction( "Read ranges", readImage );
        fileMenu.add( readAction );

        //  Add action to cut out all regions.
        CutAction cutAction =
            new CutAction( "Cut", cutterImage );
        fileMenu.add( cutAction );
        JButton cutButton = new JButton( cutAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( cutButton );
        cutButton.setToolTipText( "Cut out all regions" );

        //  Add action to cut out the selected regions.
        CutSelectedAction cutSelectedAction =
            new CutSelectedAction( "Cut Selected", cutterImage );
        fileMenu.add( cutSelectedAction );
        JButton cutSelectedButton = new JButton( cutSelectedAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( cutSelectedButton );
        cutSelectedButton.setToolTipText( "Cut out the selected regions" );

        //  Add action to reset all values.
        ResetAction resetAction = new ResetAction( "Reset", resetImage );
        fileMenu.add( resetAction );
        JButton resetButton = new JButton( resetAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( resetButton );
        resetButton.setToolTipText( "Clear all associated cuts and ranges" );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction );
        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        actionBar.add( Box.createGlue() );

        //  Add the help menu.
        HelpFrame.createHelpMenu( "cutter-window", "Help on window",
                                  menuBar, null );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Cut regions from a spectrum" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 500, 300 ) );
        setVisible( true );
    }

    /**
     *  Do the cut, to either all the ranges or just the selected
     *  ones.
     *
     *  @param selected true if we should just cut to selected ranges.
     */
    public void cut( boolean selected )
    {
        //  Extract all ranges and obtain current spectrum.
        SpecData currentSpectrum = plot.getCurrentSpectrum();
        if ( currentSpectrum == null ) {
            return; // No spectrum available, so do nothing.
        }

        double[] ranges = rangeList.getRanges( selected );
        if ( ranges.length == 0 ) {
            return; // No ranges, so nothing to do.
        }

        //  Perform the cut operation and add the spectrum to the
        //  global list.
        SpecData newSpec = 
            SpecCutter.getReference().cutView( currentSpectrum, ranges );
        localList.add( newSpec );
    }

    /**
     *  Close the window. Delete any ranges that are shown.
     */
    protected void closeWindowEvent()
    {
        rangeList.deleteAllRanges();
        this.dispose();
    }

    /**
     *  Delete all known cuts.
     */
    protected void deleteCuts()
    {
        for ( int i = 0; i < localList.size(); i++ ) {
            globalList.removeSpectrum( (SpecData)localList.get( i ) );
        }
        localList.clear();
    }

    /**
     * Reset all controls and dispose of all associated cuts.
     */
    protected void resetActionEvent()
    {
        //  Remove any spectra.
        deleteCuts();

        //  Remove any graphics and ranges.
        rangeList.deleteAllRanges();
    }

    /**
     * Initiate a file selection dialog and choose a file that
     * contains a list of ranges.
     */
    public void getRangesFromFile()
    {
        initFileChooser();
        int result = fileChooser.showOpenDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            readRangesFromFile( file );
        }
    }

    /**
     * Initialise the file chooser to have the necessary filters.
     */
    protected void initFileChooser()
    {
        if ( fileChooser == null ) {
            fileChooser = new JFileChooser( System.getProperty( "user.dir" ) );
            fileChooser.setMultiSelectionEnabled( false );

            //  Add a filter for text files.
            SpectralFileFilter textFileFilter =
                new SpectralFileFilter( "txt", "TEXT files" );
            fileChooser.addChoosableFileFilter( textFileFilter );

            //  But allow all files as well.
            fileChooser.addChoosableFileFilter
                ( fileChooser.getAcceptAllFileFilter() );
        }
    }

    /**
     * Read a set of ranges from a file. These are added to the
     * existing ranges. The file should be simple and have two
     * fields, separated by whitespace or commas. Comments are
     * indicated by lines starting with a hash (#) and are ignored.
     *
     * @param file reference to the file.
     */
    public void readRangesFromFile( File file )
    {
        //  Check file exists.
        if ( ! file.exists() && file.canRead() && file.isFile() ) {
            return;
        }
        AsciiFileParser parser = new AsciiFileParser( file );
        if ( parser.getNFields() != 2 ) {
            JOptionPane.showMessageDialog( this,
               "The format of ranges file requires just two fields + (" +
               parser.getNFields() +" were found)",
               "Error reading " + file.getName(),
               JOptionPane.ERROR_MESSAGE);
        }

        int nrows = parser.getNRows();
        double[] range = new double[2];
        for( int i = 0; i < nrows; i++ ) {
            for ( int j = 0; j < 2; j++ ) {
                range[j] = parser.getDoubleField( i, j );
            }

            //  Create the new range.
            rangeList.createRange( range );
        }
    }

    /**
     * Cut action. Cuts out all ranges.
     */
    protected class CutAction extends AbstractAction
    {
        public CutAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            cut( false );
        }
    }

    /**
     * Cut selected action. Performs cut of only selected ranges.
     */
    protected class CutSelectedAction extends AbstractAction
    {
        public CutSelectedAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            cut( true );
        }
    }

    /**
     * Inner class defining Action for closing window and keeping the
     * cuts.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            closeWindowEvent();
        }
    }

    /**
     * Inner class defining action for resetting all values.
     */
    protected class ResetAction extends AbstractAction
    {
        public ResetAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            resetActionEvent();
        }
    }

    /**
     * Read ranges from file action.
     */
    protected class ReadAction extends AbstractAction
    {
        public ReadAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            getRangesFromFile();
        }
    }

}
