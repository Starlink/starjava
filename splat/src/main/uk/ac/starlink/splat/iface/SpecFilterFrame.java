/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     1-JAN-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.text.DecimalFormat;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.AsciiFileParser;
import uk.ac.starlink.splat.util.GridBagLayouter;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.ast.gui.DecimalField;

/**
 * Provides a toolbox with number of ways to filter a spectrum.
 * The result becomes a new memory spectrum on the global list.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see SpecFilter
 */
public class SpecFilterFrame
    extends JFrame
{
    /**
     * List of spectra that we have created.
     */
    protected ArrayList localList = new ArrayList();

    /**
     * Content pane of frame.
     */
    protected JPanel contentPane = null;

    /**
     * Action buttons container.
     */
    protected JPanel actionBar = new JPanel();

    /**
     * Selection for filtering inside ranges or outside.
     */
    protected JCheckBox includeRanges = null;

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
     *  Ranges of the spectrum to process. If not set whole spectrum
     *  is used.
     */
    protected XGraphicsRangesView rangeList = null;

    /**
     * The JTabbedPane for filters.
     */
    protected JTabbedPane tabbedPane = null;

    /**
     * The average filter width.
     */
    protected DecimalField averageWidth = null;

    /**
     * The median filter width.
     */
    protected DecimalField medianWidth = null;

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
    public SpecFilterFrame( PlotControl plot )
    {
        contentPane = (JPanel) getContentPane();
        setPlot( plot );
        initBasicUI();
        initControlArea();
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
     * filter.
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
    protected void initBasicUI()
    {
        //  The layout is a BorderLayout with the complex controls
        //  being held in a split pane area in the centre and the action
        //  bar held in the south area. The split pane holds the
        //  regions and filter choices.
        contentPane.setLayout( new BorderLayout() );

        //  Add the menuBar.
        setJMenuBar( menuBar );

        //  Action bar uses a BoxLayout and is placed at the south.
        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        contentPane.add( actionBar, BorderLayout.SOUTH );

        //  Get icons.
        ImageIcon closeImage = new ImageIcon(
            ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon readImage = new ImageIcon(
            ImageHolder.class.getResource( "read.gif" ) );
        ImageIcon resetImage = new ImageIcon(
            ImageHolder.class.getResource( "reset.gif" ) );
        ImageIcon filterImage = new ImageIcon(
            ImageHolder.class.getResource( "filter.gif" ) );
        ImageIcon helpImage = new ImageIcon(
            ImageHolder.class.getResource( "help.gif" ) );

        //  Create the File menu.
        fileMenu.setText( "File" );
        menuBar.add( fileMenu );

        //  Add action to do read a list of ranges from disk file.
        ReadAction readAction = new ReadAction( "Read ranges", readImage );
        fileMenu.add( readAction );

        //  Add action to filter all regions.
        FilterAction filterAction =
            new FilterAction( "Filter", filterImage );
        fileMenu.add( filterAction );
        JButton filterButton = new JButton( filterAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( filterButton );
        filterButton.setToolTipText
            ( "Apply the filter to the current spectrum" );

        //  Add action to reset all values.
        ResetAction resetAction = new ResetAction( "Reset", resetImage );
        fileMenu.add( resetAction );
        JButton resetButton = new JButton( resetAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( resetButton );
        resetButton.setToolTipText( "Clear all filtered spectra and ranges" );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction );
        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        actionBar.add( Box.createGlue() );

        //  Add the help menu.
        HelpFrame.createHelpMenu( "filter-window", "Help on window",
                                  menuBar, null );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Filter regions of a spectrum" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 750, 300 ) );
        setVisible( true );
    }

    /**
     * Initialise the control area. This contains a split pane with
     * the filters on the left and ranges on the right.
     */
    protected void initControlArea()
    {
        JSplitPane splitPane = new JSplitPane();
        splitPane.setOneTouchExpandable( true );

        tabbedPane = new JTabbedPane();
        splitPane.setLeftComponent( tabbedPane );
        splitPane.setRightComponent( initRegionUI() );

        initAverageUI();
        initMedianUI();
        initProfilesUI();
        initSpectrumUI();

        contentPane.add( splitPane, BorderLayout.CENTER );
    }

    /**
     * Add controls for the windowed average filter.
     */
    protected void initAverageUI()
    {
        JPanel panel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );

        //  Just need to get the window size and whether to use any
        //  regions or not.
        JLabel widthLabel = new JLabel( "Window width:   " );
        DecimalFormat decimalFormat = new DecimalFormat();
        averageWidth = new DecimalField( 5, 5, decimalFormat );
        averageWidth.setToolTipText( "Width of region to average over" );

        panel.add( widthLabel );
        panel.add( averageWidth );

        tabbedPane.addTab( "Average", panel );
    }

    /**
     * Add controls for the windowed median filter.
     */
    protected void initMedianUI()
    {
        JPanel panel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );

        //  Just need to get the window size and whether to use any
        //  regions or not.
        JLabel widthLabel = new JLabel( "Window width:   " );
        DecimalFormat decimalFormat = new DecimalFormat();
        medianWidth = new DecimalField( 5, 5, decimalFormat );
        medianWidth.setToolTipText( "Width of region to median over" );

        panel.add( widthLabel );
        panel.add( medianWidth );

        tabbedPane.addTab( "Median", panel );
    }

    //  Controls needed for setting and getting the state of the
    //  profiles pane.
    protected JCheckBox gaussProfile = null;
    protected JCheckBox lorentzProfile = null;
    protected JCheckBox voigtProfile = null;
    protected DecimalField profileWidth = null;
    protected DecimalField gWidth = null;
    protected DecimalField lWidth = null;
    protected JLabel gWidthLabel = null;
    protected JLabel lWidthLabel = null;

    /**
     * Add controls for filtering using one of the standard profiles.
     */
    protected void initProfilesUI()
    {
        JPanel panel = new JPanel();
        GridBagLayouter layouter = new GridBagLayouter( panel );

        //  Need a profile type and some parameters. Gaussian needs a
        //  width as does Lorentz. Voigt needs two widths.

        JLabel typeLabel = new JLabel( "Type of profile:" );
        gaussProfile = new JCheckBox( "Gaussian" );
        gaussProfile.setToolTipText( "Smooth using a Gaussian profile" );
        lorentzProfile = new JCheckBox( "Lorentzian" );
        lorentzProfile.setToolTipText( "Smooth using a Lorentzian profile" );
        voigtProfile = new JCheckBox( "Voigt" );
        voigtProfile.setToolTipText( "Smooth using a Voigt profile" );

        layouter.add( typeLabel, false );
        layouter.add( gaussProfile, false );
        layouter.add( lorentzProfile, false );
        layouter.add( voigtProfile, true );

        ButtonGroup useGroup = new ButtonGroup();
        useGroup.add( gaussProfile );
        useGroup.add( lorentzProfile );
        useGroup.add( voigtProfile );
        gaussProfile.setSelected( true );

        // Arrange to toggle entry fields.
        gaussProfile.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    toggleProfileWidths();
                }
            });
        lorentzProfile.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    toggleProfileWidths();
                }
            });
        voigtProfile.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    toggleProfileWidths();
                }
            });

        JLabel widthLabel = new JLabel( "Profile evaluation width:   " );
        DecimalFormat decimalFormat = new DecimalFormat();
        profileWidth = new DecimalField( 50.0, 5, decimalFormat );
        profileWidth.setToolTipText( "Width used to evaluate profile " +
                                     "(should be at least several widths)" );

        layouter.add( widthLabel, false );
        layouter.add( profileWidth, true );

        gWidthLabel = new JLabel( "Gaussian FWHM/width:   " );
        decimalFormat = new DecimalFormat();
        gWidth = new DecimalField( 5, 5, decimalFormat );
        gWidth.setToolTipText( "FWHM of gaussian or gaussian width" );

        layouter.add( gWidthLabel, false );
        layouter.add( gWidth, true );

        lWidthLabel = new JLabel( "Lorentzian width:   " );
        decimalFormat = new DecimalFormat();
        lWidth = new DecimalField( 5, 5, decimalFormat );
        lWidth.setToolTipText( "The Lorentzian width" );

        layouter.add( lWidthLabel, false );
        layouter.add( lWidth, true );
        layouter.eatSpare();
        toggleProfileWidths();

        tabbedPane.addTab( "Profile", panel );
    }

    // Controls needed for selecting a spectrum.
    protected JList globalView = null;

    /**
     * Add controls for filtering using another spectrum.
     */
    protected void initSpectrumUI()
    {
        JPanel panel = new JPanel( new BorderLayout() );

        //  Just a need a view of the global list of spectra.
        globalView = new JList();
        JScrollPane globalScroller = new JScrollPane( globalView );
        TitledBorder globalViewTitle =
            BorderFactory.createTitledBorder( "Global list of spectra:" );
        globalScroller.setBorder( globalViewTitle );
        panel.add( globalScroller, BorderLayout.CENTER );

        //  Set the JList model to show the spectra (SplatListModel
        //  interacts with the global list).
        globalView.setModel
            ( new SpecListModel( globalView.getSelectionModel() ) );

        //  Only allow the selection of one item at a time.
        globalView.setSelectionMode
            ( ListSelectionModel.SINGLE_INTERVAL_SELECTION );

        tabbedPane.addTab( "Spectrum", panel );
    }


    /**
     * Add controls for selecting regions, if required.
     */
    protected JPanel initRegionUI()
    {
        JPanel panel = new JPanel( new BorderLayout() );

        // Are ranges include or exclude?
        includeRanges = new JCheckBox( "Filter inside ranges" );
        includeRanges.setSelected( false );
        includeRanges.setToolTipText
            ( "Apply filter to interior of ranges, otherwise exclude" +
              " ranges from filtering" );
        panel.add( includeRanges, BorderLayout.NORTH );

        rangeList = new XGraphicsRangesView( plot.getPlot() );
        panel.add( rangeList, BorderLayout.CENTER );
        return panel;
    }

    /**
     *  Apply the filter, to either the full spectrum, all the ranges
     *  or just the selected ones.
     *
     *  @param selected true if we should just filter the selected ranges.
     */
    public void filter( boolean selected )
    {
        //  Extract all ranges and obtain current spectrum.
        SpecData currentSpectrum = plot.getCurrentSpectrum();
        if ( currentSpectrum == null ) {
            return; // No spectrum available, so do nothing.
        }
        double[] ranges = rangeList.getRanges( selected );
        boolean include = includeRanges.isSelected();

        //  Perform the filter operation and add the spectrum to the
        //  global list.
        SpecData newSpec = null;
        SpecFilter filter = SpecFilter.getReference();
        switch ( tabbedPane.getSelectedIndex() ) {
            case 0: {
                // Average.
                newSpec = filter.averageFilter( currentSpectrum,
                                                averageWidth.getIntValue(),
                                                ranges, include );
            }
            break;
            case 1: {
                // Median
                newSpec = filter.medianFilter( currentSpectrum,
                                               medianWidth.getIntValue(),
                                               ranges, include );
            }
            break;
            case 2: {
                // Profile
                newSpec = applyCurrentProfile( currentSpectrum, filter,
                                               ranges, include );
            }
            break;
            case 3: {
                // Another spectrum.
                newSpec = applySpectrumProfile( currentSpectrum, filter, 
                                                ranges, include );
            }
            break;
            default: {
               // No filter type selected.
                JOptionPane.showMessageDialog
                    ( this, "Select a filter type pane", "No type selected",
                      JOptionPane.ERROR_MESSAGE );
                return;
            }
        }
        localList.add( newSpec );

        // And plot the filter.
        try {
            globalList.addSpectrum( plot, newSpec );
            globalList.setKnownNumberProperty( newSpec,
                                               SpecData.LINE_COLOUR,
                                               new Integer(Color.red.getRGB()) );
        }
        catch (SplatException e) {
            e.printStackTrace();
        }
    }

    /**
     * Use the current profiles choices to filter a given spectrum.
     */
    protected SpecData applyCurrentProfile( SpecData currentSpectrum,
                                            SpecFilter filter,
                                            double[] ranges, 
                                            boolean include  )
    {
        if ( gaussProfile.isSelected() ) {
            return filter.gaussianFilter( currentSpectrum,
                                          profileWidth.getIntValue(),
                                          gWidth.getDoubleValue(),
                                          ranges, include );
        }
        if ( lorentzProfile.isSelected() ) {
            return filter.lorentzFilter( currentSpectrum,
                                         profileWidth.getIntValue(),
                                         lWidth.getDoubleValue(),
                                         ranges, include );
        }
        return filter.voigtFilter( currentSpectrum,
                                   profileWidth.getIntValue(),
                                   gWidth.getDoubleValue(),
                                   lWidth.getDoubleValue(),
                                   ranges, include );
    }

    /**
     * Use another spectrum to filter the current spectrum.
     */
    protected SpecData applySpectrumProfile( SpecData currentSpectrum,
                                             SpecFilter filter,
                                             double[] ranges,
                                             boolean include )
    {
        int index = globalView.getSelectedIndex();
        if ( index > -1 ) {
            return filter.specKernelFilter( currentSpectrum,  
                                            globalList.getSpectrum(index ),
                                            ranges, include );
        }
        JOptionPane.showMessageDialog
            ( this, "You need to select a spectrum", "No spectrum selected",
              JOptionPane.ERROR_MESSAGE );
        return null;
    }

    /**
     * Toggle the width entry fields for profiles according the currently
     * selected profile.
     */
    protected void toggleProfileWidths()
    {
        if ( gaussProfile.isSelected() ) {
            gWidth.setEnabled( true );
            gWidthLabel.setText( "Gaussian FWHM:    " );
            gWidthLabel.setEnabled( true );
            lWidth.setEnabled( false );
            lWidthLabel.setEnabled( false );
        }
        else {
            lWidth.setEnabled( true );
            lWidthLabel.setEnabled( true );
            gWidthLabel.setText( "Gaussian width:    " );
            if ( lorentzProfile.isSelected() ) {
                gWidth.setEnabled( false );
                gWidthLabel.setEnabled( false );
            }
            else {
                gWidth.setEnabled( true );
                gWidthLabel.setEnabled( true );
            }
        }
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
     *  Delete all known spectra.
     */
    protected void deleteSpectra()
    {
        for ( int i = 0; i < localList.size(); i++ ) {
            globalList.removeSpectrum( (SpecData)localList.get( i ) );
        }
        localList.clear();
    }

    /**
     * Reset all controls and dispose of all associated filtered spectra.
     */
    protected void resetActionEvent()
    {
        //  Remove any spectra.
        deleteSpectra();

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
     * Filter action.
     */
    protected class FilterAction extends AbstractAction
    {
        public FilterAction( String name, Icon icon ) {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae ) {
            filter( false );
        }
    }

    /**
     * Inner class defining Action for closing window and keeping the
     * results.
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
