/*
 * Copyright (C) 2001-2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     15-JUN-2001 (Peter W. Draper):
 *       Original version.
 *     28-JAN-2003 (Peter W. Draper):
 *       Added remove facilities.
 */
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
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.AsciiFileParser;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;

/**
 * Provides a toolbox with number of ways to cut out or remove parts
 * of a spectrum. The result becomes a new memory spectrum on the global
 * list.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecCutter
 */
public class SpecCutterFrame 
    extends JFrame
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
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * File chooser used for reading ranges from text files.
     */
    protected BasicFileChooser fileChooser = null;

    /**
     * The SpecCutter instance.
     */
    protected SpecCutter specCutter = SpecCutter.getInstance();

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
     * process.
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

        //  Action bars use BoxLayout and are placed at the south.
        JPanel actionBar = new JPanel( new BorderLayout() );
        JPanel actionBar1 = new JPanel();
        JPanel actionBar2 = new JPanel();
        actionBar.add( actionBar1, BorderLayout.NORTH );
        actionBar.add( actionBar2, BorderLayout.SOUTH );
        actionBar1.setLayout( new BoxLayout( actionBar1, BoxLayout.X_AXIS ) );
        actionBar2.setLayout( new BoxLayout( actionBar2, BoxLayout.X_AXIS ) );
        actionBar1.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        actionBar2.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        contentPane.add( actionBar, BorderLayout.SOUTH );

        //  Get icons.
        ImageIcon closeImage = new ImageIcon(
            ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon readImage = new ImageIcon(
            ImageHolder.class.getResource( "read.gif" ) );
        ImageIcon resetImage = new ImageIcon(
            ImageHolder.class.getResource( "reset.gif" ) );
        ImageIcon cutterImage = new ImageIcon(
            ImageHolder.class.getResource( "cutter.gif" ) );
        ImageIcon removeImage = new ImageIcon(
            ImageHolder.class.getResource( "erase.gif" ) );
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
        actionBar1.add( Box.createGlue() );
        actionBar1.add( cutButton );
        cutButton.setToolTipText
            ( "Cut out all regions from the current spectrum" );

        //  Add action to cut out the selected regions.
        CutSelectedAction cutSelectedAction =
            new CutSelectedAction( "Cut Selected", cutterImage );
        fileMenu.add( cutSelectedAction );
        JButton cutSelectedButton = new JButton( cutSelectedAction );
        actionBar1.add( Box.createGlue() );
        actionBar1.add( cutSelectedButton );
        cutSelectedButton.setToolTipText
            ( "Cut out the selected regions from the current spectrum" );

        //  Add action to remove all regions.
        RemoveAction removeAction =
            new RemoveAction( "Remove", removeImage );
        fileMenu.add( removeAction );
        JButton removeButton = new JButton( removeAction );
        actionBar1.add( Box.createGlue() );
        actionBar1.add( removeButton );
        removeButton.setToolTipText
            ( "Remove all regions from the current spectrum" );

        //  Add action to just remove the selected regions.
        RemoveSelectedAction removeSelectedAction =
            new RemoveSelectedAction( "Remove Selected", removeImage );
        fileMenu.add( removeSelectedAction );
        JButton removeSelectedButton = new JButton( removeSelectedAction );
        actionBar1.add( Box.createGlue() );
        actionBar1.add( removeSelectedButton );
        removeSelectedButton.setToolTipText
            ( "Remove the selected regions from the current spectrum" );

        actionBar1.add( Box.createGlue() );

        //  Add action to reset all values.
        ResetAction resetAction = new ResetAction( "Reset", resetImage );
        fileMenu.add( resetAction );
        JButton resetButton = new JButton( resetAction );
        actionBar2.add( Box.createGlue() );
        actionBar2.add( resetButton );
        resetButton.setToolTipText( "Clear all produced spectra and ranges" );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction );
        JButton closeButton = new JButton( closeAction );
        actionBar2.add( Box.createGlue() );
        actionBar2.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        actionBar2.add( Box.createGlue() );

        //  Add the help menu.
        HelpFrame.createHelpMenu( "cutter-window", "Help on window",
                                  menuBar, null );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Cut/Remove regions from a spectrum" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 550, 300 ) );
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
            specCutter.cutRanges( currentSpectrum, ranges );
        localList.add( newSpec );
    }

    /**
     *  Do the remove, to either all the ranges or just the selected
     *  ones.
     *
     *  @param selected true if we should just remove selected ranges.
     */
    public void remove( boolean selected )
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
            specCutter.deleteRanges( currentSpectrum, ranges );
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
     *  Delete all known cuts and deletes.
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
            fileChooser = new BasicFileChooser( false );
            fileChooser.setMultiSelectionEnabled( false );

            //  Add a filter for text files.
            BasicFileFilter textFileFilter =
                new BasicFileFilter( "txt", "TEXT files" );
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
     * Remove action. Removes all ranges.
     */
    protected class RemoveAction extends AbstractAction
    {
        public RemoveAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            remove( false );
        }
    }

    /**
     * Remove selected action. Performs remove of selected ranges.
     */
    protected class RemoveSelectedAction extends AbstractAction
    {
        public RemoveSelectedAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            remove( true );
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
