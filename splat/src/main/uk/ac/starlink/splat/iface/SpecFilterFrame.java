/*
 * Copyright (C) 2003-2004 Central Laboratory of the Research Councils
 * Copyright (C) 2009 Science and Technology Facilities Council
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
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import uk.ac.starlink.ast.gui.DecimalField;
import uk.ac.starlink.ast.gui.ScientificFormat;
import uk.ac.starlink.ast.gui.ScientificSpinner;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.util.WaveletFilter;
import uk.ac.starlink.util.gui.GridBagLayouter;

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
    /** UI preferences. */
    protected static Preferences prefs =
        Preferences.userNodeForPackage( SpecFilterFrame.class );

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
    protected JMenu rangeMenu = new JMenu();
    protected JMenuItem closeFileMenu = new JMenuItem();

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
     * The rebin filter width.
     */
    protected DecimalField rebinWidth = null;

    /**
     * The median filter width.
     */
    protected DecimalField medianWidth = null;

    /**
     *  Reference to GlobalSpecPlotList object.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * Global list of spectral view (used for selecting a spectrum as
     * a kernel).
     */
    protected JList globalView = null;

    /**
     * Wavelet chioce.
     */
    protected JComboBox waveletBox = null;

    /**
     * Wavelet percent
     */
    protected ScientificSpinner waveletPercent = null;

    /**
     * Replaced spectrum.
     */
    protected SpecData removedCurrentSpectrum = null;

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
        ImageIcon resetImage = new ImageIcon(
            ImageHolder.class.getResource( "reset.gif" ) );
        ImageIcon filterImage = new ImageIcon(
            ImageHolder.class.getResource( "filter.gif" ) );

        //  Create the File menu.
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Add action to filter all regions.
        FilterAction filterAction =
            new FilterAction( "Filter", filterImage );
        fileMenu.add( filterAction ).setMnemonic( KeyEvent.VK_F );

        JButton filterButton = new JButton( filterAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( filterButton );
        filterButton.setToolTipText
            ( "Apply the filter to the current spectrum" );

        //  Add action to filter all regions and replace current spectrum.
        FilterReplaceAction filterReplaceAction =
            new FilterReplaceAction( "Filter (Replace)", filterImage );
        fileMenu.add( filterReplaceAction ).setMnemonic( KeyEvent.VK_L );

        JButton filterReplaceButton = new JButton( filterReplaceAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( filterReplaceButton );
        filterButton.setToolTipText
            ( "Apply the filter to the current spectrum and replace it" );

        //  Add action to reset all values.
        ResetAction resetAction = new ResetAction( "Reset", resetImage );
        fileMenu.add( resetAction ).setMnemonic( KeyEvent.VK_R );

        JButton resetButton = new JButton( resetAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( resetButton );
        resetButton.setToolTipText( "Clear all filtered spectra and ranges" );

        //  Add action to reset a filter replace.
        ResetReplaceAction resetReplaceAction = 
            new ResetReplaceAction( "Reset (Replace)", resetImage );
        fileMenu.add( resetReplaceAction ).setMnemonic( KeyEvent.VK_P );

        JButton resetReplaceButton = new JButton( resetReplaceAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( resetReplaceButton );
        resetReplaceButton.setToolTipText("Reset changes of Filter (Replace)");

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        actionBar.add( Box.createGlue() );

        // Now add the Ranges menu.
        rangeMenu.setText( "Ranges" );
        rangeMenu.setMnemonic( KeyEvent.VK_R );
        menuBar.add( rangeMenu );

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

        initBinUI();
        initAverageUI();
        initMedianUI();
        initProfilesUI();
        initWaveletUI();
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
        ScientificFormat scientificFormat = new ScientificFormat();
        int value = prefs.getInt( "SpecFilterFrame_averagewindow", 5 );
        averageWidth = new DecimalField( value, 5, scientificFormat );
        averageWidth.setToolTipText( "Width of region to average over" );

        panel.add( widthLabel );
        panel.add( averageWidth );

        tabbedPane.addTab( "Average", panel );
    }

    /**
     * Add controls for the rebin filter.
     */
    protected void initBinUI()
    {
        JPanel panel = new JPanel( new FlowLayout( FlowLayout.LEFT ) );

        //  Just need to get the rebin width size.
        JLabel widthLabel = new JLabel( "Width:   " );
        ScientificFormat scientificFormat = new ScientificFormat();
        int value = prefs.getInt( "SpecFilterFrame_rebinwidth", 5 );
        rebinWidth = new DecimalField( value, 5, scientificFormat );
        rebinWidth.setToolTipText( "Number of values per new value" );

        panel.add( widthLabel );
        panel.add( rebinWidth );

        tabbedPane.addTab( "Rebin", panel );
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
        ScientificFormat scientificFormat = new ScientificFormat();
        int value = prefs.getInt( "SpecFilterFrame_medianwidth", 5 );
        medianWidth = new DecimalField( value, 5, scientificFormat );
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
    protected JCheckBox hanningProfile = null;
    protected JCheckBox hammingProfile = null;
    protected JCheckBox welchProfile = null;
    protected JCheckBox bartlettProfile = null;
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
        GridBagLayouter gbl = new GridBagLayouter( panel );

        //  Need a profile type and some parameters. Gaussian needs a
        //  width as does Lorentz. Voigt needs two widths.
        //  Hanning, Hamming. Welch and Barlett need neither.

        JLabel typeLabel = new JLabel( "Type of profile:" );

        gaussProfile = new JCheckBox( "Gaussian" );
        gaussProfile.setToolTipText( "Smooth using a Gaussian profile" );

        lorentzProfile = new JCheckBox( "Lorentzian" );
        lorentzProfile.setToolTipText( "Smooth using a Lorentzian profile" );

        voigtProfile = new JCheckBox( "Voigt" );
        voigtProfile.setToolTipText( "Smooth using a Voigt profile" );

        hanningProfile = new JCheckBox( "Hanning" );
        hanningProfile.setToolTipText( "Smooth using a Hanning filter" );

        hammingProfile = new JCheckBox( "Hamming" );
        hammingProfile.setToolTipText( "Smooth using a Hamming filter" );

        welchProfile = new JCheckBox( "Welch" );
        welchProfile.setToolTipText( "Smooth using a Welch filter" );

        bartlettProfile = new JCheckBox( "Barlett" );
        bartlettProfile.setToolTipText( "Smooth using a Barlett filter" );

        gbl.add( typeLabel, false );
        gbl.add( gaussProfile, false );
        gbl.add( lorentzProfile, false );
        gbl.add( voigtProfile, true );

        gbl.add( Box.createHorizontalBox(), false );
        gbl.add( hanningProfile, false );
        gbl.add( hammingProfile, true );

        gbl.add( Box.createHorizontalBox(), false );
        gbl.add( welchProfile, false );
        gbl.add( bartlettProfile, true );

        ButtonGroup useGroup = new ButtonGroup();
        useGroup.add( gaussProfile );
        useGroup.add( lorentzProfile );
        useGroup.add( voigtProfile );
        useGroup.add( hanningProfile );
        useGroup.add( hammingProfile );
        useGroup.add( welchProfile );
        useGroup.add( bartlettProfile );
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

        hanningProfile.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    toggleProfileWidths();
                }
            });

        hammingProfile.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    toggleProfileWidths();
                }
            });

        welchProfile.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    toggleProfileWidths();
                }
            });

        bartlettProfile.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e ) {
                    toggleProfileWidths();
                }
            });


        JLabel widthLabel = new JLabel( "Profile evaluation width:   " );
        ScientificFormat scientificFormat = new ScientificFormat();
        double value = prefs.getDouble( "SpecFilterFrame_profilewidth", 50.0 );
        profileWidth = new DecimalField( value, 5, scientificFormat );
        profileWidth.setToolTipText( "Width used to evaluate profile " +
                                     "(should be at least several widths)" );

        gbl.add( widthLabel, false );
        gbl.add( profileWidth, true );

        gWidthLabel = new JLabel( "Gaussian FWHM/width:   " );
        scientificFormat = new ScientificFormat();
        value = prefs.getDouble( "SpecFilterFrame_gausswidth", 5.0 );
        gWidth = new DecimalField( value, 5, scientificFormat );
        gWidth.setToolTipText( "FWHM of gaussian or gaussian width" );

        gbl.add( gWidthLabel, false );
        gbl.add( gWidth, true );

        lWidthLabel = new JLabel( "Lorentzian width:   " );
        scientificFormat = new ScientificFormat();
        value = prefs.getDouble( "SpecFilterFrame_lorentzwidth", 5.0 );
        lWidth = new DecimalField( value, 5, scientificFormat );
        lWidth.setToolTipText( "The Lorentzian width" );

        gbl.add( lWidthLabel, false );
        gbl.add( lWidth, true );
        gbl.eatSpare();
        toggleProfileWidths();

        tabbedPane.addTab( "Profile", panel );
    }

    /**
     * Add controls for the wavelet smoothing option.
     */
    protected void initWaveletUI()
    {
        JPanel panel = new JPanel();
        GridBagLayouter gbl = new GridBagLayouter( panel );

        //  Need two parameters, the wavelet to use and the fraction
        //  of coefficients to zero.
        JLabel waveletLabel = new JLabel( "Wavelet:   " );
        waveletBox = new JComboBox( WaveletFilter.WAVELETS );
        waveletBox.setToolTipText( "Wavelet to use when generating" +
                                   " coefficients");

        JLabel percentLabel = new JLabel( "Threshold (percent):   " );
        double value = prefs.getDouble("SpecFilterFrame_waveletpercent", 50.0);
        waveletPercent = new ScientificSpinner
            ( new SpinnerNumberModel( value, 0.0, 100.0, 1.0 ) );
        waveletPercent.setToolTipText( "Percentage of signal to remove" );

        gbl.add( waveletLabel, false );
        gbl.add( waveletBox, true );
        gbl.add( percentLabel, false );
        gbl.add( waveletPercent, true );
        gbl.eatSpare();

        tabbedPane.addTab( "Wavelet", panel );
    }

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

        rangeList = new XGraphicsRangesView( plot.getPlot(), rangeMenu );
        panel.add( rangeList, BorderLayout.CENTER );

        //  Add action to do read a list of ranges from disk file.
        Icon readImage = 
            new ImageIcon( ImageHolder.class.getResource( "read.gif" ) );
        Action readAction = rangeList.getReadAction("Read ranges", readImage);
        JMenuItem readItem = new JMenuItem( readAction );
        fileMenu.add( readItem, 0 );

        return panel;
    }

    /**
     *  Apply the filter, to either the full spectrum, all the ranges
     *  or just the selected ones. Replace the current spectrum with the
     *  result, if requested.
     *
     *  @param selected true if we should just filter the selected ranges.
     *  @param replace if true remove current spectrum
     */
    public void filter( boolean selected, boolean replace )
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
        SpecFilter filter = SpecFilter.getInstance();
        boolean useHistogram = false;
        switch ( tabbedPane.getSelectedIndex() ) {
            case 0: {
                // Rebin.
                int value = rebinWidth.getIntValue();
                prefs.putInt( "SpecFilterFrame_rebinwidth", value );
                newSpec = filter.rebinFilter( currentSpectrum, value );
                useHistogram = true;
            }
            break;
            case 1: {
                // Average.
                int value = averageWidth.getIntValue();
                prefs.putInt( "SpecFilterFrame_averagewindow", value );
                newSpec = filter.averageFilter( currentSpectrum, value,
                                                ranges, include );
            }
            break;
            case 2: {
                // Median
                int value = medianWidth.getIntValue();
                prefs.putInt( "SpecFilterFrame_medianwidth", value );
                newSpec = filter.medianFilter( currentSpectrum, value,
                                               ranges, include );
            }
            break;
            case 3: {
                // Profile
                newSpec = applyCurrentProfile( currentSpectrum, filter,
                                               ranges, include );
            }
            break;
            case 4: {
                // Wavelet.
                double percent =
                    ((Double)waveletPercent.getValue()).doubleValue();
                prefs.putDouble( "SpecFilterFrame_waveletpercent", percent );
                newSpec =
                    filter.waveletFilter( currentSpectrum,
                                          (String)waveletBox.getSelectedItem(),
                                          percent, ranges, include );
                useHistogram = true;
            }
            break;
            case 5: {
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
            if ( useHistogram ) {
                newSpec.setPlotStyle( SpecData.HISTOGRAM );
            }
            globalList.addSpectrum( plot, newSpec );
            globalList.setKnownNumberProperty( newSpec, SpecData.LINE_COLOUR,
                                               new Integer(Color.red.getRGB()));
            if ( replace ) {
                plot.removeSpectrum( currentSpectrum );

                //  The removed current spectrum remains the same until 
                //  the next reset (multiple filters want to return to raw
                //  data).
                if ( removedCurrentSpectrum == null ) {
                    removedCurrentSpectrum = currentSpectrum;
                }
            }
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
        prefs.putInt( "SpecFilterFrame_profilewidth", 
                      profileWidth.getIntValue() );
        prefs.putDouble( "SpecFilterFrame_gausswidth", 
                         gWidth.getDoubleValue() );
        prefs.putDouble( "SpecFilterFrame_lorentzwidth", 
                         lWidth.getDoubleValue() );

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
        if ( voigtProfile.isSelected() ) {
            return filter.voigtFilter( currentSpectrum,
                                       profileWidth.getIntValue(),
                                       gWidth.getDoubleValue(),
                                       lWidth.getDoubleValue(),
                                       ranges, include );
        }
        if ( hanningProfile.isSelected() ) {
            return filter.hanningFilter( currentSpectrum,
                                         profileWidth.getIntValue(),
                                         ranges, include );
        }
        if ( hammingProfile.isSelected() ) {
            return filter.hammingFilter( currentSpectrum,
                                         profileWidth.getIntValue(),
                                         ranges, include );
        }
        if ( welchProfile.isSelected() ) {
            return filter.welchFilter( currentSpectrum,
                                       profileWidth.getIntValue(),
                                       ranges, include );
        }
        return filter.bartlettFilter( currentSpectrum,
                                      profileWidth.getIntValue(),
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
            if ( hanningProfile.isSelected() ||
                 hammingProfile.isSelected() ||
                 welchProfile.isSelected() ||
                 bartlettProfile.isSelected() ) {
                gWidth.setEnabled( false );
                gWidthLabel.setEnabled( false );
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
     * Reset a filter replace. That's get rid of the filtered spectra and
     * replace the current one.
     */
    protected void resetReplaceActionEvent()
    {
        //  Remove any spectra.
        deleteSpectra();

        //  Restore the current spectrum, if needed.
        if ( removedCurrentSpectrum != null ) {
            if ( ! plot.isDisplayed( removedCurrentSpectrum )  ) {
                try {
                    plot.addSpectrum( removedCurrentSpectrum );
                    removedCurrentSpectrum = null;
                }
                catch (SplatException e) {
                    //  Do nothing, not important.
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Filter action.
     */
    protected class FilterAction
        extends AbstractAction
    {
        public FilterAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control F" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            filter( false, false );
        }
    }

    /**
     * Filter action, replacing current spectrum with result.
     */
    protected class FilterReplaceAction
        extends AbstractAction
    {
        public FilterReplaceAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control L" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            filter( false, true );
        }
    }

    /**
     * Inner class defining Action for closing window and keeping the
     * results.
     */
    protected class CloseAction
        extends AbstractAction
    {
        public CloseAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control W" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            closeWindowEvent();
        }
    }

    /**
     * Inner class defining action for resetting a filter replace.
     */
    protected class ResetReplaceAction
        extends AbstractAction
    {
        public ResetReplaceAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control P" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            resetReplaceActionEvent();
        }
    }

    /**
     * Inner class defining action for resetting all values.
     */
    protected class ResetAction
        extends AbstractAction
    {
        public ResetAction( String name, Icon icon )
        {
            super( name, icon );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control R" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            resetActionEvent();
        }
    }
}
