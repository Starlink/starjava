/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     17-JUN-2005 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.Statistics;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.gui.GridBagLayouter;


/**
 * Interactively display the statistics of a region of the current
 * spectrum. The statistics are, sum, mean, standard deviation, median, mode,
 * min, max and number of pixels used.
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public class StatsFrame
    extends JFrame
{
    /** Content pane of frame */
    protected JPanel contentPane = null;

    /** The PlotControl that is displaying the current spectrum */
    protected PlotControl control = null;

    /** The global list of spectra and plots */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /** Text area for results of all statistics fits. */
    protected JTextArea statsResults = null;

    /** Ranges of data. */
    protected StatsRangesView rangeList = null;

    /** Level of full stats reported */
    protected JCheckBoxMenuItem fullStatsBox = null;

    /**
     * Create an instance.
     */
    public StatsFrame( PlotControl control )
    {
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout( new BorderLayout() );
        setPlotControl( control );
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
        return control;
    }

    /**
     * Set the PlotControlFrame that has the spectrum that we are to
     * process.
     *
     * @param control the PlotControl reference.
     */
    public void setPlotControl( PlotControl control )
    {
        this.control = control;
    }

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {

        //  Central region.
        JPanel centre = new JPanel();
        GridBagLayouter layouter =
            new GridBagLayouter( centre, GridBagLayouter.SCHEME4 );
        contentPane.add( centre, BorderLayout.CENTER );

        //  The ranges.
        StatsRangesModel model = new StatsRangesModel( control );
        rangeList = new StatsRangesView( control, model );
        layouter.add( rangeList, true );

        //  Text pane to show report on statistics. Use this so that 
        //  previous reports can be reviewed.
        JPanel statsPanel = new JPanel();
        statsPanel.setBorder
            ( BorderFactory.createTitledBorder( "Full stats log:" ) );
        GridBagLayouter gbl = 
            new GridBagLayouter( statsPanel, GridBagLayouter.SCHEME4 );
        
        statsResults = new JTextArea();
        JScrollPane scrollPane = new JScrollPane( statsResults );
        gbl.add( scrollPane, true );

        //  Button for saving to the log file.
        LocalAction saveAction = new LocalAction
            ( LocalAction.LOGSTATS, "Save to log file",
              "Append log window contents to SPLATStats.log file" );
        JButton saveButton = new JButton( saveAction );
        gbl.add( saveButton, false );

        //  Button for clearing log area.
        LocalAction clearAction = 
            new LocalAction( LocalAction.CLEARSTATS, "Clear log",
                             "Clear log window of all content" );
        JButton clearButton = new JButton( clearAction );
        gbl.add( clearButton, false );
        gbl.eatLine();

        layouter.add( statsPanel, true );

        //  Menubar and toolbars.
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar( menuBar );
        
        //  File menu.
        JMenu fileMenu = new JMenu( "File" );
        menuBar.add( fileMenu );

        //  Options.
        JMenu optionsMenu = new JMenu( "Options" );
        menuBar.add( optionsMenu );

        //  Action bar for buttons.
        JPanel actionBar = new JPanel();

        //  Images.
        ImageIcon closeImage =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon statsImage =
            new ImageIcon( ImageHolder.class.getResource( "sigma.gif" ) );
        ImageIcon readImage =
            new ImageIcon( ImageHolder.class.getResource( "read.gif" ) );
        ImageIcon saveImage =
            new ImageIcon( ImageHolder.class.getResource( "save.gif" ) );

        //  Statistics on the selected ranges.
        LocalAction selectedAction = 
            new LocalAction( LocalAction.SELECTEDSTATS, 
                             "Selected stats", statsImage,
                             "Statistics for selected ranges" );
        fileMenu.add( selectedAction );
        JButton selectedButton = new JButton( selectedAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( selectedButton );

        //  Statistics on all ranges.
        LocalAction allAction = 
            new LocalAction( LocalAction.ALLSTATS, 
                             "All stats", statsImage,
                             "Statistics for all ranges" );
        fileMenu.add( allAction );
        JButton allButton = new JButton( allAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( allButton );

        //  Statistics on the full current spectrum.
        LocalAction wholeAction = 
            new LocalAction( LocalAction.WHOLESTATS, "Whole stats", statsImage,
                             "Statistics for whole spectrum" );
        fileMenu.add( wholeAction );
        JButton wholeButton = new JButton( wholeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( wholeButton );
        
        actionBar.add( Box.createGlue() );
        contentPane.add( actionBar, BorderLayout.SOUTH );

        //  Read and write ranges to disk file.
        Action readAction = rangeList.getReadAction( "Read ranges", 
                                                     readImage );
        fileMenu.add( readAction );
        Action writeAction = rangeList.getWriteAction( "Save ranges", 
                                                       saveImage );
        fileMenu.add( writeAction );


        //  Add an action to close the window.
        LocalAction closeAction = new LocalAction( LocalAction.CLOSE, 
                                                   "Close", closeImage,
                                                   "Close window" );
        fileMenu.add( closeAction );
        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );

        //  Option to control quantity of stats shown.
        fullStatsBox = new JCheckBoxMenuItem( "Show extra stats" );
        optionsMenu.add( fullStatsBox );

        //  Add the help menu.
        HelpFrame.createHelpMenu( "stats-window", "Help on window",
                                  menuBar, null );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Region statistics" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 650, 500 ) );
        setVisible( true );
    }

    /**
     * Calculate statistics and update interface.
     */
    public void calcStats( int type )
    {
        SpecData currentSpectrum = control.getCurrentSpectrum();
        double[] yData = currentSpectrum.getYData();
        if ( type == LocalAction.SELECTEDSTATS || 
             type == LocalAction.ALLSTATS ) {

            double[] xData = currentSpectrum.getXData();
            int[] ranges = 
                rangeList.extractRanges( type == LocalAction.SELECTEDSTATS, 
                                         true, xData );
            if ( ranges == null || ranges.length == 0 ) {
                //  No ranges... nothing to do.
                return;
            }
            
            //  Test for presence of BAD values in the data.
            int count = 0;
            for ( int i = 0; i < ranges.length; i += 2 ) {
                int low = ranges[i];
                int high = Math.min( ranges[i+1], yData.length - 1 );
                for ( int j = low; j <= high; j++ ) {
                    if ( yData[j] != SpecData.BAD ) count++;
                }
            }
            
            //  Now allocate the necessary memory and copy in the data.
            double[] cleanData = new double[count];
            count = 0;
            for ( int i = 0; i < ranges.length; i += 2 ) {
                int low = ranges[i];
                int high = Math.min( ranges[i+1], yData.length - 1 );
                for ( int j = low; j <= high; j++ ) {
                    if ( yData[j] != SpecData.BAD ) {
                        cleanData[count++] = yData[j];
                    }
                }
            }

            Statistics stats = new Statistics( cleanData );
            StringBuffer buffer = new StringBuffer();
            buffer.append( "Statistics of " + currentSpectrum.getShortName() +
                           " over ranges: \n" );
            double[] coordRanges = 
                rangeList.getRanges( type == LocalAction.SELECTEDSTATS );
            for ( int i = 0; i < coordRanges.length; i += 2 ) {
                buffer.append( "  -->" + coordRanges[i] + 
                               " : " + coordRanges[i+1] + "\n" );
            }
            buffer.append( "\n" );
            reportStats( buffer.toString(), stats );
        }
        else if ( type == LocalAction.WHOLESTATS ) {
            //  Remove all BAD values, if needed.
            int count = 0;
            for ( int i = 0; i < yData.length; i++ ) {
                if ( yData[i] != SpecData.BAD ) count++;
            }

            //  Don't make a clean copy if there are no BAD values.
            if ( count != yData.length ) {
                double[] cleanData = new double[count];
                count = 0;
                for ( int i = 0; i < yData.length; i++ ) {
                    if ( yData[i] != SpecData.BAD ) {
                        cleanData[count++] = yData[i];
                    }
                }
                yData = cleanData;
            }
            Statistics stats = new Statistics( yData );
            String desc = 
                "Statistics of " + currentSpectrum.getShortName() + ":\n";
            reportStats( desc, stats );
        }
    }

    /**
     * Add a named report to the text area.
     */
    public void reportStats( String description, Statistics stats )
    {
        statsResults.append( description );
        statsResults.append( stats.getStats( fullStatsBox.isSelected() ) );
    }

    /**
     * Save the contents of the stats log to a disk file.
     */
    protected void saveStats()
    {
        BufferedWriter writer = null;
        try {
            writer = 
                new BufferedWriter( new FileWriter( "SPLATstats.log", true ) );
            statsResults.write( writer );
            writer.write( "\n" );
            writer.close();
        }
        catch (IOException e) {
            JOptionPane.showMessageDialog( this, e.getMessage(),
                                           "Failed writing SPEFO log",
                                           JOptionPane.ERROR_MESSAGE );
        }

    }

    /**
     * Clear the stats log region.
     */
    protected void clearStats()
    {
        statsResults.selectAll();
        statsResults.cut();
    }

    /**
     * Close the window. Delete any local resources.
     */
    protected void closeWindowEvent()
    {
        rangeList.deleteAllRanges();
        this.dispose();
    }

    /**
     * Inner class defining all local Actions.
     */
    protected class LocalAction
        extends AbstractAction
    {
        
        //  Types of action.
        public static final int CLOSE = 0;
        public static final int WHOLESTATS = 1;
        public static final int SELECTEDSTATS = 2;
        public static final int ALLSTATS = 3;
        public static final int LOGSTATS = 4;
        public static final int CLEARSTATS = 5;

        //  The type of this instance.
        private int actionType = CLOSE;

        public LocalAction( int actionType, String name, Icon icon )
        {
            super( name, icon );
            this.actionType = actionType;
        }

        public LocalAction( int actionType, String name, String help )
        {
            super( name );
            this.actionType = actionType;
            putValue( SHORT_DESCRIPTION, help );
        }

        public LocalAction( int actionType, String name, Icon icon, 
                            String help )
        {
            this( actionType, name, icon );
            putValue( SHORT_DESCRIPTION, help );
        }

        public void actionPerformed( ActionEvent ae )
        {
            switch ( actionType )
            {
               case CLOSE: {
                   closeWindowEvent();
                   break;
               }
               case WHOLESTATS: {
                   calcStats( WHOLESTATS );
                   break;
               }
               case SELECTEDSTATS: {
                   calcStats( SELECTEDSTATS );
                   break;
               }
               case ALLSTATS: {
                   calcStats( ALLSTATS );
                   break;
               }
               case LOGSTATS: {
                   saveStats();
                   break;
               }
               case CLEARSTATS: {
                   clearStats();
                   break;
               }
            }
        }
    }
}
