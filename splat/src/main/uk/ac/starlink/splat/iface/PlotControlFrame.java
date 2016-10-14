/*
 * Copyright (C) 2000-2005 Central Laboratory of the Research Councils
 * Copyright (C) 2007-2009 Science and Technology Facilities Council
 *
 *  History:
 *     29-SEP-2000 (Peter W. Draper):
 *        Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import uk.ac.starlink.ast.gui.AstFigureStore;
import uk.ac.starlink.ast.gui.AstAxes;
import uk.ac.starlink.ast.gui.AxesControls;
import uk.ac.starlink.ast.gui.AstPlotSource;
import uk.ac.starlink.ast.gui.ComponentColourControls;
import uk.ac.starlink.ast.gui.GraphicsEdgesControls;
import uk.ac.starlink.ast.gui.GraphicsHintsControls;
import uk.ac.starlink.ast.gui.PlotConfigurator;
import uk.ac.starlink.diva.DrawActions;
import uk.ac.starlink.diva.DrawGraphicsMenu;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.GraphicFileUtilities;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.vo.SLAPBrowser;
import uk.ac.starlink.util.gui.BasicFileChooser;

/**
 * PlotControlFrame provides a top-level wrapper for a PlotControl
 * object.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PlotControlFrame
    extends JFrame
    implements ItemListener, ActionListener
{
    /**
     *  PlotControl object for displaying the spectra.
     */
    protected PlotControl plot;

    /**
     * PlotConfigurator window for changing the Plot configuration
     * values (created when required).
     */
    protected PlotConfigurator configFrame = null;

    /**
     * PolyFitFrame window for fitting a polynomial to the spectrum
     * (created when required).
     */
    protected PolyFitFrame polyFitFrame = null;

    /**
     * Window used to generate spectra from hand drawn interpolated
     * lines.
     */
    protected GenerateFromInterpFrame interpFrame = null;

    /**
     * Window used to deblend spectra.
     */
    protected DeblendFrame deblendFrame = null;

    /**
     * LineFitFrame window for measuring to the spectral line properties.
     * (created when required).
     */
    protected LineFitFrame lineFitFrame = null;

    /**
     * The panner window.
     */
    protected PlotPannerFrame pannerFrame = null;

    /**
     * The cutter window.
     */
    protected SpecCutterFrame cutterFrame = null;

    /**
     * The filter window.
     */
    protected SpecFilterFrame filterFrame = null;

    /**
     * The units window.
     */
    protected PlotUnitsFrame unitsFrame = null;

    /**
     * The flip and translate frame.
     */
    protected FlipFrame flipFrame = null;

    /**
     * The statistics frame.
     */
    protected StatsFrame statsFrame = null;

    /**
     * The spectra stacker frame.
     */
    protected PlotStackerFrame stackerFrame = null;

    /**
     *  The global list of spectra and plots.
     */
    private static GlobalSpecPlotList globalList =
        GlobalSpecPlotList.getInstance();

    /**
     * UI preferences.
     */
    protected static Preferences prefs =
        Preferences.userNodeForPackage( PlotControlFrame.class );

    /**
     *  Main menubar and various menus.
     */
    protected JCheckBoxMenuItem autoFitPercentiles = null;
    protected JCheckBoxMenuItem baseSystemMatching = null;
    protected JCheckBoxMenuItem coordinateMatching = null;
    protected JCheckBoxMenuItem dataUnitsMatching = null;
    protected JCheckBoxMenuItem displayErrorsAsData = null;
    protected JCheckBoxMenuItem doubleDSBLineIDs = null;
    protected JCheckBoxMenuItem errorbarAutoRanging = null;
    protected JCheckBoxMenuItem horizontalLineIDs = null;
    protected JCheckBoxMenuItem offsetMatching = null;
    protected JCheckBoxMenuItem prefixLineIDs = null;
    protected JCheckBoxMenuItem shortNameLineIDs = null;
    protected JCheckBoxMenuItem showShortNames = null;
    protected JCheckBoxMenuItem showSynopsis = null;
    protected JCheckBoxMenuItem showLegend = null;
    protected JCheckBoxMenuItem showVerticalMarks = null;
    protected JCheckBoxMenuItem showVisibleOnly = null;
    protected JCheckBoxMenuItem sidebandMatching = null;
    protected JCheckBoxMenuItem suffixLineIDs = null;
    protected JCheckBoxMenuItem trackerLineIDs = null;
    protected JMenu analysisMenu = new JMenu();
    protected JMenu editMenu = new JMenu();
    protected JMenu fileMenu = new JMenu();
    protected JMenu helpMenu = new JMenu();
    protected JMenu lineOptionsMenu = new JMenu();
    protected JMenu optionsMenu = new JMenu();
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenuItem drawMenu = new JMenuItem();
    protected JMenuItem openSlapBrowser = null;
    protected JMenuItem loadAllLineIDs = null;
    protected JMenuItem loadLoadedLineIDs = null;
    protected JMenuItem removeCurrent = null;
    protected JMenuItem unloadLineIDs = null;
    protected PlotGraphicsClipMenuItem clipGraphics = null;

    /**
     *  Toolbar.
     */
    protected ToolButtonBar toolBar = null;

    /**
     * File chooser used for postscript files.
     */
    protected BasicFileChooser postscriptChooser = null;

    /**
     * Checkbox controlling the type of postscript saved.
     */
    protected JCheckBox epsBox = null;

    /**
     * Show deblend tools, removed once development is complete.
     */
    private boolean showDeblend = false;

    private SLAPBrowser slapBrowser;

    /**
     *  Create an instance using an existing SpecDataComp.
     *
     *  @param specDataComp Active SpecDataComp reference.
     *
     */
    public PlotControlFrame( SpecDataComp specDataComp )
        throws SplatException
    {
        this( "PlotControlFrame", specDataComp, -1 );
    }

    /**
     *  Create an instance using an existing SpecDataComp and new PlotControl
     *  with a given plot index.
     *
     *  @param specDataComp Active SpecDataComp reference.
     *  @param id plot identifier (-1 for automatic, otherwise must not be in
     *            use).
     *
     */
    public PlotControlFrame( SpecDataComp specDataComp, int id )
        throws SplatException
    {
        this( "PlotControlFrame", specDataComp, id );
    }

    /**
     *  Create instance using an existing PlotControl.
     *
     *  @param plotControl a PlotControl instance.
     *
     */
    public PlotControlFrame( PlotControl plotControl )
        throws SplatException
    {
        this( "PlotControlFrame", plotControl );
    }

    /**
     *  Create the main window. Creates default PlotControls and SpecDataComp
     *  instances.
     */
    public PlotControlFrame( String title )
        throws SplatException
    {
        this( title, (SpecDataComp) null, -1 );
    }

    /**
     *  Create an instance with a given title and SpecDataComp.
     *  A new default PlotControl instance will be created.
     *
     *  @param specDataComp active SpecDataComp reference.
     *
     */
    public PlotControlFrame( String title, SpecDataComp specDataComp )
        throws SplatException
    {
        this( title, specDataComp, -1 );
    }

    /**
     *  Create an instance with a given title and SpecDataComp.
     *  A new default PlotControl instance will be created.
     *
     *  @param title for window
     *  @param specDataComp active SpecDataComp reference.
     *  @param id plot identifier (-1 for automatic, otherwise must not be in
     *            use).
     *
     */
    public PlotControlFrame( String title, SpecDataComp specDataComp, int id )
        throws SplatException
    {
        this( title, new PlotControl( specDataComp, id ) );
    }

    /**
     *  Plot a spectrum.
     *
     *  @param file  name of file containing spectrum.
     *
     */
    public PlotControlFrame( String title, String file )
        throws SplatException
    {
        this( title, new PlotControl( file ) );
    }

    /**
     *  Create an instance with a given title and PlotControl.
     *
     *  @param specDataComp Active SpecDataComp reference.
     *
     */
    public PlotControlFrame( String title, PlotControl plotControl )
        throws SplatException
    {
        //  Development properties.
        java.util.Properties props = System.getProperties();
        String isDevelop = props.getProperty( "splat.development" );
        if (  isDevelop != null && isDevelop.equals( "1" )  ) {
            showDeblend = true;
        }
        else {
            showDeblend = false;
        }

        this.plot = plotControl;
        initUI( title );
    }

    /**
     *  Return a reference to the PlotControl object.
     */
    public PlotControl getPlot()
    {
        return plot;
    }

    /**
     *  Make the frame visible and set the default action for when we
     *  are closed.
     */
    protected void initUI( String title )
    {
        setTitle( Utilities.getTitle( title ) );
        setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        getContentPane().add( plot, BorderLayout.CENTER );
        configureMenus();

        Utilities.setFrameSize( (JFrame) this, 0, 0, prefs,
                                "PlotControlFrame" );
        Utilities.setComponentSize( plot.getPlot(), 0, 0, prefs, "DivaPlot" );

        //  Listen for resize events so we can update the preferences.
        addComponentListener( new ComponentAdapter() {
                public void componentResized( ComponentEvent e )
                {
                    recordWindowSize();
                }
            });

        plot.getPlot().setBaseScale();

        setVisible( true );

        //  Attempt to focus in the plot.
        try {
            plot.getPlot().requestFocus();
        }
        catch (Exception e) {
            //  Don't care, so print this for information only.
            e.printStackTrace();
        }

    }

    /**
     *  Configure the menu and toolbar.
     */
    protected void configureMenus()
    {
        //  Add the menuBar.
        setJMenuBar( menuBar );

        //  Add the toolbar to a container.
        toolBar = new ToolButtonBar( getContentPane() );

        //  Add the File menu.
        setupFileMenu();

        //  Set up the Analysis menu.
        setupAnalysisMenu();

        //  Set up the Edit ment.
        setupEditMenu();

        //  Set up the Options menu.
        setupOptionsMenu();

        //  Set up the Graphics menu.
        setupGraphicsMenu();

        //  Set up the help menu.
        setupHelpMenu();
    }

    /**
     *  Configure the File menu.
     */
    protected void setupFileMenu()
    {
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        ImageIcon printImage = new ImageIcon(
            ImageHolder.class.getResource( "print.gif" ) );
        ImageIcon printPostscriptImage = new ImageIcon(
            ImageHolder.class.getResource( "postscriptprint.gif" ) );
        ImageIcon printJPEGImage = new ImageIcon(
            ImageHolder.class.getResource( "jpegpng.gif" ) );
        ImageIcon closeImage = new ImageIcon(
            ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon fitWidthImage = new ImageIcon(
            ImageHolder.class.getResource( "fitwidth.gif" ) );
        ImageIcon fitHeightImage = new ImageIcon(
            ImageHolder.class.getResource( "fitheight.gif" ) );
        ImageIcon configImage = new ImageIcon(
            ImageHolder.class.getResource( "config.gif" ) );
        ImageIcon pannerImage = new ImageIcon(
            ImageHolder.class.getResource( "panner.gif" ) );

        //  Add action to print figure.
        PrintAction printAction  =
            new PrintAction( "Print", printImage,
                             "Print display to local printer or file" );
        fileMenu.add( printAction ).setMnemonic( KeyEvent.VK_P );
        toolBar.add( printAction );

        //  Add action to print figure to postscript file.
        PrintPostscriptAction printPostscriptAction  =
            new PrintPostscriptAction( "Print to postscript",
                                       printPostscriptImage,
                                       "Print display to a postscript file" );
        fileMenu.add( printPostscriptAction ).setMnemonic( KeyEvent.VK_T );
        toolBar.add( printPostscriptAction );

        //  Add action to print figure to a JPEG or PNG.
        PrintJPEGAction printJPEGAction  =
            new PrintJPEGAction( "Print to JPEG/PNG", printJPEGImage,
                                 "Print display to a JPEG or a PNG file" );
        fileMenu.add( printJPEGAction ).setMnemonic( KeyEvent.VK_J );
        toolBar.add( printJPEGAction );

        //  Add action to fit plot to window width.
        FitWidthAction fitWidthAction  =
            new FitWidthAction( "Fit width",
                                fitWidthImage,
                                "Scale spectrum to fit visible width" );
        fileMenu.add( fitWidthAction ).setMnemonic( KeyEvent.VK_W );
        toolBar.add( fitWidthAction );

        //  Add action to fit plot to window height.
        FitHeightAction fitHeightAction =
            new FitHeightAction( "Fit height",
                                 fitHeightImage,
                                 "Scale spectrum to fit visible height" );
        fileMenu.add( fitHeightAction ).setMnemonic( KeyEvent.VK_H );
        toolBar.add( fitHeightAction );

        //  Add action to enable the panner.
        PannerAction pannerAction  =
            new PannerAction( "Show panner",
                              pannerImage,
                "Show panner window for controlling scroll position " );
        fileMenu.add( pannerAction ).setMnemonic( KeyEvent.VK_S );
        toolBar.add( pannerAction );

        //  Add action to configure plot.
        ConfigAction configAction  =
            new ConfigAction( "Configure",
                              configImage,
                              "Configure plot presentation attributes" );
        fileMenu.add( configAction ).setMnemonic( KeyEvent.VK_O );
        toolBar.add( configAction );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage,
                                                   "Close window" );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );
    }

    /**
     * Configure the analysis menu.
     */
    protected void setupAnalysisMenu()
    {
        analysisMenu.setText( "Analysis" );
        analysisMenu.setMnemonic( KeyEvent.VK_A );
        menuBar.add( analysisMenu );

        ImageIcon backImage =
            new ImageIcon( ImageHolder.class.getResource( "fitback.gif" ) );
        ImageIcon interpImage =
            new ImageIcon( ImageHolder.class.getResource("interpolate.gif") );
        ImageIcon deblendImage =
            new ImageIcon( ImageHolder.class.getResource( "deblend.gif" ) );
        ImageIcon lineImage =
            new ImageIcon( ImageHolder.class.getResource( "fitline.gif" ) );
        ImageIcon cutterImage =
            new ImageIcon( ImageHolder.class.getResource( "cutter.gif" ) );
        ImageIcon regionCutterImage =
            new ImageIcon( ImageHolder.class.getResource("regioncutter.gif") );
        ImageIcon filterImage =
            new ImageIcon( ImageHolder.class.getResource( "filter.gif" ) );
        ImageIcon unitsImage =
            new ImageIcon( ImageHolder.class.getResource( "units.gif" ) );
        ImageIcon flipImage =
            new ImageIcon( ImageHolder.class.getResource( "flip.gif" ) );
        ImageIcon statsImage =
            new ImageIcon( ImageHolder.class.getResource( "sigma.gif" ) );

        //  Add action to enable to cut out the current view of
        //  current spectrum.
        ViewCutterAction viewCutterAction =
            new ViewCutterAction( "Cut out view", cutterImage,
                "Cut out what you can see of the current spectrum" );
        analysisMenu.add( viewCutterAction ).setMnemonic( KeyEvent.VK_V );
        toolBar.add( viewCutterAction );

        //  Add action start the cutter tool.
        RegionCutterAction regionCutterAction =
            new RegionCutterAction( "Cut regions from spectrum",
                                    regionCutterImage,
                "Cut out selected regions of the current spectrum" );
        analysisMenu.add( regionCutterAction ).setMnemonic( KeyEvent.VK_R );
        toolBar.add( regionCutterAction );

        //  Add the fit polynomial to background item.
        PolyFitAction polyFitAction =
            new PolyFitAction( "Fit polynomial",
                               backImage,
                               "Fit parts of spectrum using a polynomial" );
        analysisMenu.add( polyFitAction ).setMnemonic( KeyEvent.VK_Y );
        toolBar.add( polyFitAction );

        //  Add the generate from interpolated line item.
        GenFromInterpAction interpAction =
            new GenFromInterpAction( "Spectrum from interpolation",
                                     interpImage,
                "Generate a spectrum from an interpolated line" );
        analysisMenu.add( interpAction ).setMnemonic( KeyEvent.VK_P );
        toolBar.add( interpAction );

        //  Add the deblend lines action.
        if ( showDeblend ) {
            DeblendAction deblendAction =
                new DeblendAction( "Deblend lines", deblendImage,
                                   "Fit components to a blend of lines" );
            analysisMenu.add( deblendAction ).setMnemonic( KeyEvent.VK_D );
            toolBar.add( deblendAction );
        }

        //  Add the measure and fit spectral lines action.
        LineFitAction lineFitAction =
            new LineFitAction( "Fit lines",
                               lineImage,
                "Fit spectral lines using a variety of functions" );
        analysisMenu.add( lineFitAction ).setMnemonic( KeyEvent.VK_L );
        toolBar.add( lineFitAction );

        FilterAction filterAction =
            new FilterAction( "Filter spectrum", filterImage,
                              "Apply a filter to the current spectrum" );
        analysisMenu.add( filterAction ).setMnemonic( KeyEvent.VK_F );
        toolBar.add( filterAction );

        UnitsAction unitsAction =
            new UnitsAction( "Change units", unitsImage,
                             "Change the units of the current spectrum" );
        analysisMenu.add( unitsAction ).setMnemonic( KeyEvent.VK_U );
        toolBar.add( unitsAction );

        FlipAction flipAction =
            new FlipAction( "Flip compare", flipImage,
                            "Flip and/or translate current spectrum" );
        analysisMenu.add( flipAction ).setMnemonic( KeyEvent.VK_I );
        toolBar.add( flipAction );

        StatsAction statsAction =
            new StatsAction( "Region statistics", statsImage,
                             "Get statistics on regions of spectrum" );
        analysisMenu.add( statsAction ).setMnemonic( KeyEvent.VK_S );
        toolBar.add( statsAction );
    }

    /**
     * Configure the edit menu.
     */
    protected void setupEditMenu()
    {
        editMenu.setText( "Edit" );
        editMenu.setMnemonic( KeyEvent.VK_E );
        menuBar.add( editMenu );

        //  Remove the current spectrum.
        removeCurrent = new JMenuItem( "Remove current spectrum" );
        editMenu.add( removeCurrent );
        removeCurrent.addActionListener( this );

        //  Stack the spectra.
        ImageIcon stackerImage =
            new ImageIcon( ImageHolder.class.getResource( "stacker.gif" ) );
        StackerAction stackerAction =
            new StackerAction( "Stack display spectra", stackerImage,
                    "Display spectra in an ordered stack arrangement" );
        editMenu.add( stackerAction );
        toolBar.add( stackerAction );
    }

    /**
     * Configure the options menu.
     */
    protected void setupOptionsMenu()
    {
        optionsMenu.setText( "Options" );
        optionsMenu.setMnemonic( KeyEvent.VK_O );
        menuBar.add( optionsMenu );

        //  Arrange to carefully align coordinates when asked (expensive
        //  otherwise).
        coordinateMatching =
            new JCheckBoxMenuItem( "Match coordinates and/or fluxes" );
        optionsMenu.add( coordinateMatching );
        coordinateMatching.addItemListener( this );

        dataUnitsMatching =
            new JCheckBoxMenuItem( "Match non-flux data units" );
        optionsMenu.add( dataUnitsMatching );
        dataUnitsMatching.addItemListener( this );

        sidebandMatching =
            new JCheckBoxMenuItem( "Match sidebands" );
        optionsMenu.add( sidebandMatching );
        sidebandMatching.addItemListener( this );

        offsetMatching =
            new JCheckBoxMenuItem( "Match origins" );
        optionsMenu.add( offsetMatching );
        offsetMatching.addItemListener( this );

        baseSystemMatching =
            new JCheckBoxMenuItem( "Match using base system" );
        optionsMenu.add( baseSystemMatching );
        baseSystemMatching.addItemListener( this );

        boolean state1 =
            prefs.getBoolean( "PlotControlFrame_coordinatematch", true );
        boolean state2 =
            prefs.getBoolean( "PlotControlFrame_dataunitsmatch", false );
        boolean state3 =
            prefs.getBoolean( "PlotControlFrame_sidebandmatch", false );
        boolean state4 =
            prefs.getBoolean( "PlotControlFrame_offsetmatch", false );
        boolean state5 =
            prefs.getBoolean( "PlotControlFrame_basesystemmatch", true );
        coordinateMatching.setSelected( state1 );
        dataUnitsMatching.setSelected( state2 );
        sidebandMatching.setSelected( state3 );
        offsetMatching.setSelected( state4 );
        baseSystemMatching.setSelected( state5 );

        //  Whether to just draw the grid in the visible region. Can be
        //  expensive so off by default.
        showVisibleOnly =
            new JCheckBoxMenuItem( "Only display grid axes in visible area" );
        optionsMenu.add( showVisibleOnly );
        showVisibleOnly.addItemListener( this );

        state1 = prefs.getBoolean( "PlotControlFrame_showvisibleonly", false );
        showVisibleOnly.setSelected( state1 );

        //  Whether to clip spectrum graphics to lie with axes border.
        clipGraphics =
            new PlotGraphicsClipMenuItem( plot.getPlot(),
                                          "Only display spectra within axes" );
        optionsMenu.add( clipGraphics );
        clipGraphics.addItemListener( this );

        state1 = prefs.getBoolean( "PlotControlFrame_clipgraphics", false );
        clipGraphics.setSelected( state1 );

        setupLineOptionsMenu();

        //  Include spacing for error bars in the auto ranging.
        errorbarAutoRanging = new JCheckBoxMenuItem("Error bar auto-ranging");
        optionsMenu.add( errorbarAutoRanging );
        errorbarAutoRanging.addItemListener( this );
        state1 = prefs.getBoolean( "PlotControlFrame_errorbarautoranging",
                                   false );
        errorbarAutoRanging.setSelected( state1 );

        //  Display errors as the spectrum. Note not session persistent.
        displayErrorsAsData = new JCheckBoxMenuItem("Draw errors as spectrum");
        optionsMenu.add( displayErrorsAsData );
        displayErrorsAsData.addItemListener( this );
        displayErrorsAsData.setSelected( false );

        //  Autofit to Y when selecting a percentile cut.
        autoFitPercentiles =
            new JCheckBoxMenuItem( "Auto fit percentiles in Y" );
        optionsMenu.add( autoFitPercentiles );
        autoFitPercentiles.addItemListener( this );
        state1 =
            prefs.getBoolean( "PlotControlFrame_autofitpercentiles", false );
        autoFitPercentiles.setSelected( state1 );

        //  Show short names in drop down menu.
        showShortNames =
            new JCheckBoxMenuItem( "Short names in menu lists" );
        optionsMenu.add( showShortNames );
        showShortNames.addItemListener( this );
        state1 = prefs.getBoolean( "PlotControlFrame_showshortnames",
                                   LineRenderer.isShowShortNames() );
        showShortNames.setSelected( state1 );
        LineRenderer.setShowShortNames( state1 );

        //  Display the current spectrum synopsis.
        showSynopsis = new JCheckBoxMenuItem( "Display synopsis" );
        optionsMenu.add( showSynopsis );
        showSynopsis.addItemListener( this );
        state1 = prefs.getBoolean( "PlotControlFrame_showsynopsis", false );
        showSynopsis.setSelected( state1 );
        plot.setShowSynopsis( state1 );

        //  Display the legend. XXX not finished.
        //showLegend = new JCheckBoxMenuItem( "Display legend" );
        //optionsMenu.add( showLegend );
        //showLegend.addItemListener( this );
        //state1 = prefs.getBoolean( "PlotControlFrame_showlegend", false );
        //showLegend.setSelected( state1 );
        //plot.setShowLegend( state1 );
    }

    /**
     * Set up the line identifier options menu.
     */
    protected void setupLineOptionsMenu()
    {
        lineOptionsMenu.setText( "Line identifiers" );
        lineOptionsMenu.setMnemonic( KeyEvent.VK_L );
        optionsMenu.add( lineOptionsMenu );

        openSlapBrowser = new JMenuItem("SLAP Browser");
        
      
        SLAPAction slapAction =
                new SLAPAction( "SLAP Browser", 
                        "Open Simple Line Access Protocol browser" );
        lineOptionsMenu.add(slapAction);
     
        //openSlapBrowser.addActionListener( this );
    
        //  Load line identifiers into the plot. This comes in two flavours
        //  load all line identifiers and only those that are already
        //  available in the global list.
        loadAllLineIDs = new JMenuItem( "Load all matching line identifiers" );
        lineOptionsMenu.add( loadAllLineIDs );
        loadAllLineIDs.addActionListener( this );

        loadLoadedLineIDs = new JMenuItem
            ( "Load all matching pre-loaded line identifiers" );
        lineOptionsMenu.add( loadLoadedLineIDs );
        loadLoadedLineIDs.addActionListener( this );

        //  Unload all line identifiers.
        unloadLineIDs = new JMenuItem( "Unload all line identifiers" );
        lineOptionsMenu.add( unloadLineIDs );
        unloadLineIDs.addActionListener( this );

        //  Make labels track the position of the current spectrum.
        trackerLineIDs =
            new JCheckBoxMenuItem( "Positions track current spectrum" );
        lineOptionsMenu.add( trackerLineIDs );
        trackerLineIDs.addItemListener( this );
        boolean state = prefs.getBoolean( "PlotControlFrame_trackerlineids",
                                          false );
        trackerLineIDs.setSelected( state );

        //  Show the vertical marks as well.
        showVerticalMarks = new JCheckBoxMenuItem( "Show vertical lines" );
        lineOptionsMenu.add( showVerticalMarks );
        showVerticalMarks.addItemListener( this );
        state = prefs.getBoolean( "PlotControlFrame_showverticalmarks", true );
        showVerticalMarks.setSelected( state );

        //  Prefix labels with the short name.
        prefixLineIDs = new JCheckBoxMenuItem( "Prefix name to labels" );
        lineOptionsMenu.add( prefixLineIDs );
        prefixLineIDs.addItemListener( this );
        boolean state1 =
            prefs.getBoolean( "PlotControlFrame_prefixlineids", false );

        //  Or Suffix labels with the short name.
        suffixLineIDs = new JCheckBoxMenuItem( "Suffix name to labels" );
        lineOptionsMenu.add( suffixLineIDs );
        suffixLineIDs.addItemListener( this );
        boolean state2 =
            prefs.getBoolean( "PlotControlFrame_suffixlineids", false );

        //  Or show only short name (useful when labels are very long).
        shortNameLineIDs = new JCheckBoxMenuItem("Short name only as labels");
        lineOptionsMenu.add( shortNameLineIDs );
        shortNameLineIDs.addItemListener( this );
        boolean state3 =
            prefs.getBoolean( "PlotControlFrame_shortnamelineids", false );

        //  Interdependent, so initialise once all are realised.
        prefixLineIDs.setSelected( state1 );
        suffixLineIDs.setSelected( state2 );
        shortNameLineIDs.setSelected( state3 );

        //  Draw labels horizontally
        horizontalLineIDs = new JCheckBoxMenuItem( "Draw horizontal labels" );
        lineOptionsMenu.add( horizontalLineIDs );
        horizontalLineIDs.addItemListener( this );
        state = prefs.getBoolean("PlotControlFrame_horizontallineids", false);
        horizontalLineIDs.setSelected( state );

        //  Display labels for both axes of a DSB spectrum.
        doubleDSBLineIDs = new JCheckBoxMenuItem("Show dual sideband labels");
        lineOptionsMenu.add( doubleDSBLineIDs );
        doubleDSBLineIDs.setSelected( true );
        doubleDSBLineIDs.addItemListener( this );
        state = prefs.getBoolean( "PlotControlFrame_doubledsblineids", true );
        doubleDSBLineIDs.setSelected( state );
    }

    /**
     * Configure the Graphics menu.
     */
    protected void setupGraphicsMenu()
    {
        DrawActions drawActions = plot.getPlot().getDrawActions();

        //  Set the figure store for saving figures to a backing file.
        AstFigureStore store = new AstFigureStore
            ( (AstPlotSource) plot.getPlot(), Utilities.getApplicationName(),
              "FigureStore.xml", "drawnfigures" );
        drawActions.setFigureStore( store );

        //  Keyboard shortcuts to some actions.
        Action action = drawActions.getDeleteSelectedAction();
        action.putValue( Action.ACCELERATOR_KEY,
                         KeyStroke.getKeyStroke( "DELETE" ) );

        action = drawActions.getClearAction();
        action.putValue( Action.ACCELERATOR_KEY,
                         KeyStroke.getKeyStroke( "shift DELETE" ) );

        action = drawActions.getRaiseSelectedAction();
        action.putValue( Action.ACCELERATOR_KEY,
                         KeyStroke.getKeyStroke( "R" ) );

        action = drawActions.getLowerSelectedAction();
        action.putValue( Action.ACCELERATOR_KEY,
                         KeyStroke.getKeyStroke( "L" ) );

        action = drawActions.getHideAction();
        action.putValue( Action.ACCELERATOR_KEY,
                         KeyStroke.getKeyStroke( "H" ) );

        action = drawActions.getHideAction();
        action.putValue( Action.ACCELERATOR_KEY,
                         KeyStroke.getKeyStroke( "H" ) );

        for ( int i = 2; i < DrawActions.NUM_DRAWING_MODES; i++ ) {
            action = drawActions.getDrawingModeAction( i );
            action.putValue( Action.ACCELERATOR_KEY,
                             KeyStroke.getKeyStroke( "control "+ ( i - 1 ) ) );
        }

        //  Finally create menu and add to menubar.
        DrawGraphicsMenu graphicsMenu = new DrawGraphicsMenu( drawActions );
        graphicsMenu.setMnemonic( KeyEvent.VK_G );
        menuBar.add( graphicsMenu );
    }

    /**
     * Configure the help menu.
     */
    protected void setupHelpMenu()
    {
        HelpFrame.createButtonHelpMenu( "plot-window", "Help on window",
                                        menuBar, toolBar );
    }

    /**
     *  Print the current display to a printer.
     */
    protected void printDisplay()
    {
        try {
            plot.print();
        }
        catch (SplatException e) {
            JOptionPane.showMessageDialog ( this, e.getMessage(),
                                            "Printer warning",
                                            JOptionPane.ERROR_MESSAGE );
        }
    }

    /**
     *  Print the current display to a postscript file.
     */
    protected void printPostscriptDisplay()
    {
        if ( postscriptChooser == null ) {
            postscriptChooser = new BasicFileChooser( false );
            postscriptChooser.setSelectedFile( new File( "out.ps" ) );

            JPanel extraControls = new JPanel();
            epsBox = new JCheckBox( "Encapsulated" );
            epsBox.setToolTipText("Select to save as encapsulated postscript");
            extraControls.add( epsBox );
            postscriptChooser.setAccessory( extraControls );
        }
        int result = postscriptChooser.showSaveDialog( this );
        if ( result == postscriptChooser.APPROVE_OPTION ) {
            File file = postscriptChooser.getSelectedFile();
            try {
                plot.printPostscript( epsBox.isSelected(), file.getPath() );
            }
            catch (SplatException e) {
                JOptionPane.showMessageDialog ( this, e.getMessage(),
                                                "Printer warning",
                                                JOptionPane.ERROR_MESSAGE );
            }
        }
    }

    /**
     *  Print the current display to a JPEG or PNG file.
     */
    protected void printJPEGDisplay()
    {
        GraphicFileUtilities.showGraphicChooser( plot.getPlot() );
    }

    /**
     *  Make spectrum fit width.
     */
    public void fitToWidth()
    {
        plot.fitToWidth();
    }

    /**
     *  Make spectrum fit height.
     */
    public void fitToHeight()
    {
        plot.fitToHeight();
    }

    /**
     *  Activate the pop-up window for configuring the Plot.
     */
    public void configPlot()
    {
        if ( configFrame == null ) {
            configFrame = new PlotConfigurator( "Plot configurator window",
                                                plot,
                                                plot.getPlotConfiguration(),
                                                Utilities.getApplicationName(),
                                                "PlotConfigs.xml" );

            //  Add the SPLAT on-line help.
            HelpFrame.createHelpMenu( "config-window", "Help on window",
                                      configFrame.getJMenuBar(), null );


            //  Add controls for the extra facilities provided by the
            //  DivaPlot.
            DivaPlot divaPlot = plot.getPlot();
            try {
                AstAxes astAxes = (AstAxes) configFrame.getConfiguration()
                    .getControlsModel( AxesControls.getControlsModelClass() );
                DataLimitControls dlc =
                    new DataLimitControls( divaPlot.getDataLimits(), plot,
                                           astAxes );
                configFrame.addExtraControls( dlc, false );
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            try {
                GraphicsHintsControls ghc =
                    new GraphicsHintsControls( divaPlot.getGraphicsHints() );
                configFrame.addExtraControls( ghc, true);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            try {
                GraphicsEdgesControls gec =
                    new GraphicsEdgesControls( divaPlot.getGraphicsEdges() );
                configFrame.addExtraControls( gec, true );
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            try {
                ComponentColourControls ccc =
                    new ComponentColourControls
                    ( plot, divaPlot.getBackgroundColourStore(),
                      "Plot Background", "Background", "Colour:" );
                configFrame.addExtraControls( ccc, true );
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            //  We'd like to know if the window is closed.
            configFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        configClosed();
                    }
                });

            //  Autodate is switched on by default.
            configFrame.setAutoDrawOption( true );

            //  Show first page (data limits, not title page).
            configFrame.reveal( 0 );
        }
        else {
            Utilities.raiseFrame( configFrame );
        }
    }

    /**
     *  Configuration window is closed.
     */
    protected void configClosed()
    {
        // Nullify if method for closing switches to dispose.
        // configFrame = null;
    }

    /**
     *  Close the config window.
     */
    protected void closeConfigFrame()
    {
        if ( configFrame != null ) {
            configFrame.dispose();
            configFrame = null;
        }
    }

    /**
     *  Activate the pop-up window for fitting a polynomial to the
     *  current spectrum.
     */
    public void polyFit()
    {
        if ( polyFitFrame == null ) {
            polyFitFrame = new PolyFitFrame( this );
            //  We'd like to know if the window is closed.
            polyFitFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        polyFitClosed();
                    }
                });
        }
        else {
            Utilities.raiseFrame( polyFitFrame );
        }
    }

    /**
     *  Polynomial fitting window is closed.
     */
    protected void polyFitClosed()
    {
        // Nullify if method for closing switches to dispose.
        // fitPolynomialWindow = null;
    }

    /**
     *  Close the polynomial fitting window.
     */
    protected void closePolyFitFrame()
    {
        if ( polyFitFrame != null ) {
            polyFitFrame.dispose();
            polyFitFrame = null;
        }
    }

    /**
     *  Activate the pop-up window for generating an interpolated
     *  spectrum.
     */
    public void interpolate()
    {
        if ( interpFrame == null ) {
            interpFrame = new GenerateFromInterpFrame( this );
            //  We'd like to know if the window is closed.
            interpFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        interpClosed();
                    }
                });
        }
        else {
            Utilities.raiseFrame( interpFrame );
        }
    }

    /**
     *  Interpolation window is closed.
     */
    protected void interpClosed()
    {
        // Nullify if method for closing switches to dispose.
        // interpFrame = null;
    }

    /**
     *  Close the interpolation window.
     */
    protected void closeInterpFrame()
    {
        if ( interpFrame != null ) {
            interpFrame.dispose();
            interpFrame = null;
        }
    }

    /**
     *  Activate the pop-up window for deblending spectral lines.
     */
    public void deblend()
    {
        if ( deblendFrame == null ) {
            deblendFrame = new DeblendFrame( this );
            //  We'd like to know if the window is closed.
            deblendFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        deblendClosed();
                    }
                });
        }
        else {
            Utilities.raiseFrame( deblendFrame );
        }
    }

    /**
     *  Deblend window is closed.
     */
    protected void deblendClosed()
    {
        // Nullify if method for closing switches to dispose.
        // deblendFrame = null;
    }

    /**
     *  Close the deblending window.
     */
    protected void closeDeblendFrame()
    {
        if ( deblendFrame != null ) {
            deblendFrame.dispose();
            deblendFrame = null;
        }
    }

    /**
     *  Activate the pop-up window for fitting lines of the
     *  current spectrum.
     */
    public void lineFit()
    {
        if ( lineFitFrame == null ) {
            lineFitFrame = new LineFitFrame( this );
            //  We'd like to know if the window is closed.
            lineFitFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        lineFitClosed();
                    }
                });
        } else {
            Utilities.raiseFrame( lineFitFrame );
        }
    }

    /**
     * Line fitting window is closed.
     */
    protected void lineFitClosed()
    {
        // Nullify if method for closing switches to dispose.
        // lineFitFrame = null;
    }

    /**
     *  Close the line fitting window.
     */
    protected void closeLineFitFrame()
    {
        if ( lineFitFrame != null ) {
            lineFitFrame.dispose();
            lineFitFrame = null;
        }
    }

    /**
     *  Activate the panner window for panning a zoomed spectrum.
     */
    public void showPanner()
    {
        if ( pannerFrame == null ) {
            pannerFrame = new PlotPannerFrame( getPlot() );
            //  We'd like to know if the window is closed.
            pannerFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        pannerClosed();
                    }
                });
        } else {
            Utilities.raiseFrame( pannerFrame );
        }
    }

    /**
     * Panner window is closed.
     */
    protected void pannerClosed()
    {
        // Nullify if method for closing switches to dispose.
        // lineFitWindow = null;
    }

    /**
     *  Close the panner window.
     */
    protected void closePanner()
    {
        if ( pannerFrame != null ) {
            pannerFrame.dispose();
            pannerFrame = null;
        }
    }

    /**
     *  Activate the cutter window.
     */
    public void showCutter()
    {
        if ( cutterFrame == null ) {
            cutterFrame = new SpecCutterFrame( getPlot() );
            //  We'd like to know if the window is closed.
            cutterFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        cutterClosed();
                    }
                });
        } else {
            Utilities.raiseFrame( cutterFrame );
        }
    }

    /**
     * Cutter window is closed.
     */
    protected void cutterClosed()
    {
        // Nullify if method for closing switches to dispose.
        // cutterFrame = null;
    }

    /**
     *  Close the cutter window.
     */
    protected void closeCutter()
    {
        if ( cutterFrame != null ) {
            cutterFrame.dispose();
            cutterFrame = null;
        }
    }

    /**
     * Cut out the current view of the current spectrum and add it to
     * the global list.
     */
    public void cutView()
    {
        SpecCutter.getInstance().cutView( plot.getCurrentSpectrum(),
                                          getPlot() );
    }

    /**
     *  Activate the filter window.
     */
    public void showFilter()
    {
        if ( filterFrame == null ) {
            filterFrame = new SpecFilterFrame( getPlot() );
            //  We'd like to know if the window is closed.
            filterFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        filterClosed();
                    }
                });
        }
	else {
            Utilities.raiseFrame( filterFrame );
        }
    }

    /**
     * Filter window is closed.
     */
    protected void filterClosed()
    {
        // Nullify if method for closing switches to dispose.
        // filterFrame = null;
    }

    /**
     *  Close the filter window.
     */
    protected void closeFilter()
    {
        if ( filterFrame != null ) {
            filterFrame.dispose();
            filterFrame = null;
        }
    }

    /**
     *  Activate the units window.
     */
    public void showUnits()
    {
        if ( unitsFrame == null ) {
            unitsFrame = new PlotUnitsFrame( getPlot() );
            //  We'd like to know if the window is closed.
            unitsFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        unitsClosed();
                    }
                });
        }
	else {
            Utilities.raiseFrame( unitsFrame );
        }
    }

    /**
     * Units window is closed.
     */
    protected void unitsClosed()
    {
        // Nullify if method for closing switches to dispose.
        // unitsFrame = null;
    }

    /**
     *  Close the units window.
     */
    protected void closeUnits()
    {
        if ( unitsFrame != null ) {
            unitsFrame.dispose();
            unitsFrame = null;
        }
    }

    /**
     *  Activate the flip window.
     */
    public void showFlip()
    {
        if ( flipFrame == null ) {
            flipFrame = new FlipFrame( getPlot() );
            //  We'd like to know if the window is closed.
            flipFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        flipClosed();
                    }
                });
        }
	else {
            Utilities.raiseFrame( flipFrame );
        }
    }

    /**
     * Flip window is closed.
     */
    protected void flipClosed()
    {
        // Nullify if method for closing switches to dispose.
        // flipFrame = null;
    }

    /**
     *  Close the flip window.
     */
    protected void closeFlip()
    {
        if ( flipFrame != null ) {
            flipFrame.dispose();
            flipFrame = null;
        }
    }

    /**
     *  Activate the stats window.
     */
    public void showStats()
    {
        if ( statsFrame == null ) {
            statsFrame = new StatsFrame( getPlot() );
            //  We'd like to know if the window is closed.
            statsFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        statsClosed();
                    }
                });
        }
        else {
            Utilities.raiseFrame( statsFrame );
        }
    }

    /**
     * Stats window is closed.
     */
    protected void statsClosed()
    {
        // Nullify if method for closing switches to dispose.
        // statsFrame = null;
    }

    /**
     *  Close the stats window.
     */
    protected void closeStats()
    {
        if ( statsFrame != null ) {
            statsFrame.dispose();
            statsFrame = null;
        }
    }

    /**
     *  Activate the stacker window.
     */
    public void showStacker()
    {
        if ( stackerFrame == null ) {
            stackerFrame = new PlotStackerFrame( getPlot() );
            //  We'd like to know if the window is closed.
            stackerFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        stackerClosed();
                    }
                });
        }
        else {
            Utilities.raiseFrame( stackerFrame );
        }
    }

    /**
     * Stacker window is closed.
     */
    protected void stackerClosed()
    {
        // Nullify if method for closing switches to dispose.
        // stackerFrame = null;
    }

    /**
     *  Close the stacker window.
     */
    protected void closeStacker()
    {
        if ( stackerFrame != null ) {
            stackerFrame.dispose();
            stackerFrame = null;
        }
    }
    
    /**
     *  Activate the SLAP Browser window.
     */
    public void showSlapBrowser()
    {
        coordinateMatching.setSelected( true );
        if ( slapBrowser == null ) {
            slapBrowser = new SLAPBrowser( getPlot() );
            //  We'd like to know if the window is closed.
 /*           slapBrowser.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        slapBrowserClosed();
                    }
                });
 */
        } else {
            Utilities.raiseFrame( slapBrowser );
        }
    }


    /**
     * Set the main cursor to indicate waiting for some action to
     * complete and lock the interface by trapping all mouse events.
     */
    protected void setWaitCursor()
    {
        Cursor wait = Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR );
        Component glassPane = getGlassPane();
        glassPane.setCursor( wait );
        glassPane.setVisible( true );
        glassPane.addMouseListener( new MouseAdapter() {} );
    }

    /**
     * Undo the action of the setWaitCursor method.
     */
    protected void resetWaitCursor()
    {
        getGlassPane().setCursor( null );
        getGlassPane().setVisible( false );
    }

    /**
     *  Close all associated tool windows.
     */
    public void closeToolWindows()
    {
        closeConfigFrame();
        closePolyFitFrame();
        closeInterpFrame();
        closeDeblendFrame();
        closeLineFitFrame();
        closePanner();
        closeCutter();
        closeFilter();
        closeUnits();
        closeFlip();
        closeStats();
        closeStacker();
        plot.release();
    }

    /**
     *  Clear resources when finalized.
     */
    protected void finalize() throws Throwable
    {
        closeToolWindows();
        super.finalize();
    }

    /**
     *  When window is closed remove configuration windows too.
     */
    protected void processWindowEvent( WindowEvent e )
    {
        super.processWindowEvent( e );
        if ( e.getID() == WindowEvent.WINDOW_CLOSING ) {
            recordWindowSize();
            closeToolWindows();
        }
    }

    /**
     *  Close the main window, removing all the tool windows too.
     */
    public void closeWindow()
    {
        recordWindowSize();
        dispose();
        closeToolWindows();
    }

    /**
     *  Remove the current spectrum.
     */
    public void removeCurrentSpectrum()
    {
        globalList
            .removeSpectrum(plot, plot.getSpecDataComp().getCurrentSpectrum());
    }

    /**
     * Record the window size into the preferences database.
     */
    protected void recordWindowSize()
    {
        Utilities.saveFrameSize( this, prefs, "PlotControlFrame" );
        Utilities.saveComponentSize( plot.getViewport(), prefs, "DivaPlot" );
    }

//
//  Inner classes.
//
    /**
     *  Inner class defining Action for closing window.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control W" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            closeWindow();
        }
    }

    /**
     *  Inner class defining Action for printing.
     */
    protected class PrintAction extends AbstractAction
    {
        public PrintAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control P" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            printDisplay();
        }
    }

    /**
     *  Inner class defining Action for printing to postscript.
     */
    protected class PrintPostscriptAction extends AbstractAction
    {
        public PrintPostscriptAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control T" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            printPostscriptDisplay();
        }
    }

    /**
     *  Inner class defining Action for printing to a JPEG or PNG file.
     */
    protected class PrintJPEGAction extends AbstractAction
    {
        public PrintJPEGAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control J" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            printJPEGDisplay();
        }
    }

    /**
     *  Inner class defining Action for fitting spectrum to width.
     */
    protected class FitWidthAction extends AbstractAction
    {
        public FitWidthAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control B" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            fitToWidth();
        }
    }

    /**
     *  Inner class defining Action for fitting spectrum to height.
     */
    protected class FitHeightAction extends AbstractAction
    {
        public FitHeightAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control H" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            fitToHeight();
        }
    }

    /**
     *  Inner class defining Action for configuring plot.
     */
    protected class ConfigAction extends AbstractAction
    {
        public ConfigAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control O" ) );
        }
        public void actionPerformed( ActionEvent ae ) {
            configPlot();
        }
    }

    /**
     *  Inner class defining Action for showing panner.
     */
    protected class PannerAction extends AbstractAction
    {
        public PannerAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control A" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            showPanner();
        }
    }

    /**
     *  Inner class defining Action for cutting out view of spectrum.
     */
    protected class ViewCutterAction extends AbstractAction
    {
        public ViewCutterAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control V" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            cutView();
        }
    }

    /**
     *  Inner class defining Action for cutting out regions of spectrum.
     */
    protected class RegionCutterAction extends AbstractAction
    {
        public RegionCutterAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control R" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            showCutter();
        }
    }

    /**
     *  Inner class defining Action for filtering spectrum.
     */
    protected class FilterAction extends AbstractAction
    {
        public FilterAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control F" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            showFilter();
        }
    }

    /**
     *  Inner class defining Action for changing units.
     */
    protected class UnitsAction extends AbstractAction
    {
        public UnitsAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control U" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            showUnits();
        }
    }

    /**
     *  Inner class defining Action for flipping spectrum.
     */
    protected class FlipAction extends AbstractAction
    {
        public FlipAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control I" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            showFlip();
        }
    }

    /**
     *  Inner class defining stats action.
     */
    protected class StatsAction extends AbstractAction
    {
        public StatsAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control S" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            showStats();
        }
    }

    /**
     *  Inner class defining plot stacker action.
     */
    protected class StackerAction extends AbstractAction
    {
        public StackerAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );
            //putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control S" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            showStacker();
        }
    }

    /**
     *  Inner class defining Action for fitting a polynomial to a spectrum.
     */
    protected class PolyFitAction extends AbstractAction
    {
        public PolyFitAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control Y" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            polyFit();
        }
    }

    /**
     *  Inner class defining Action for creating an interpolated background.
     */
    protected class GenFromInterpAction extends AbstractAction
    {
        public GenFromInterpAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control G" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            interpolate();
        }
    }

    /**
     *  Inner class defining Action for deblending lines.
     */
    protected class DeblendAction extends AbstractAction
    {
        public DeblendAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control D" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            deblend();
        }
    }

    /**
     *  Inner class defining Action for fitting spectral lines.
     */
    protected class LineFitAction extends AbstractAction
    {
        public LineFitAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );

            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control L" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            lineFit();
        }
    }
    
    /**
     *  Inner class defining Action for SLAP browsing and adding spectral lines.
     */
    protected class SLAPAction extends AbstractAction
    {
        public SLAPAction( String name, String help )
        {
            super( name);
            putValue( SHORT_DESCRIPTION, help );

            //putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control V" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            showSlapBrowser();
        }
    }

    //
    // Implement ItemListener interface. This is used for menus items
    // that do not require the full capabilities of an Action
    // (i.e. don't need an icon and don't also appear in the toolbar).
    //
    public void itemStateChanged( ItemEvent e )
    {
        Object source = e.getSource();

        if ( source.equals( coordinateMatching ) ||
             source.equals( dataUnitsMatching ) ||
             source.equals( sidebandMatching ) ||
             source.equals( offsetMatching ) ||
             source.equals( baseSystemMatching ) ) {

            boolean state1 = coordinateMatching.isSelected();
            boolean state2 = dataUnitsMatching.isSelected();
            boolean state3 = sidebandMatching.isSelected();
            boolean state4 = offsetMatching.isSelected();
            boolean state5 = baseSystemMatching.isSelected();

            if ( source.equals( dataUnitsMatching ) ) {
                // Need coordinateMatching when matching dataUnits. Coordinate
                // matching cannot be switched off (SpecFrame active units).
                if ( state2 ) {
                    coordinateMatching.setSelected( true );
                    state1 = true;
                }
            }
            else {
                if ( ! state1 ) {
                    dataUnitsMatching.setSelected( false );
                    state2 = false;
                }
            }

            plot.getSpecDataComp().setCoordinateMatching( state1 );
            plot.getSpecDataComp().setDataUnitsMatching( state2 );
            plot.getSpecDataComp().setSideBandMatching( state3 );
            plot.getSpecDataComp().setOffsetMatching( state4 );
            plot.getSpecDataComp().setBaseSystemMatching( state5 );

            prefs.putBoolean( "PlotControlFrame_coordinatematch", state1 );
            prefs.putBoolean( "PlotControlFrame_dataunitsmatch", state2 );
            prefs.putBoolean( "PlotControlFrame_sidebandmatch", state3 );
            prefs.putBoolean( "PlotControlFrame_offsetmatch", state4 );
            prefs.putBoolean( "PlotControlFrame_basesystemmatch", state5 );
           try {
                plot.updateThePlot( null );
            }
            catch (SplatException se) {

                // Matching has failed. Need to make this clear and then
                // rectify the situation by switching it off.
                if ( state1 || state2 ) {
                    JOptionPane.showMessageDialog
                        ( this, se.getMessage() +
                          "\n Matching will be switched off",
                          "Matching failed",
                          JOptionPane.ERROR_MESSAGE );

                    //  Trigger rematch?
                    coordinateMatching.setSelected( false );
                    dataUnitsMatching.setSelected( false );
                    sidebandMatching.setSelected( false );
                    offsetMatching.setSelected( false );
                    baseSystemMatching.setSelected( true );
                }
            }
            return;
        }

        if ( source.equals( showVisibleOnly ) ) {
            boolean state = showVisibleOnly.isSelected();
            plot.getPlot().setVisibleOnly( state );
            prefs.putBoolean( "PlotControlFrame_showvisibleonly", state );
            plot.updatePlot();
            return;
        }

        if ( source.equals( clipGraphics ) ) {
            boolean state = clipGraphics.isSelected();
            plot.getPlot().getGraphicsEdges().setClipped( state );
            prefs.putBoolean( "PlotControlFrame_clipgraphics", state );
            plot.updatePlot();
            return;
        }

        if ( source.equals( errorbarAutoRanging ) ) {
            boolean state = errorbarAutoRanging.isSelected();
            plot.getSpecDataComp().setErrorbarAutoRanging( state );
            prefs.putBoolean( "PlotControlFrame_errorbarautoranging", state );
            plot.updatePlot();
            return;
        }

        if ( source.equals( displayErrorsAsData ) ) {
            boolean state = displayErrorsAsData.isSelected();
            plot.getSpecDataComp().setPlotErrorsAsData( state );
            plot.updatePlot();
            return;
        }

        if ( source.equals( autoFitPercentiles ) ) {
            boolean state = autoFitPercentiles.isSelected();
            prefs.putBoolean( "PlotControlFrame_autofitpercentiles", state );
            plot.setAutoFitPercentiles( state );
            return;
        }

        if ( source.equals( showShortNames ) ) {
            boolean state = showShortNames.isSelected();
            prefs.putBoolean( "PlotControlFrame_showshortnames", state );
            LineRenderer.setShowShortNames( state );
            return;
        }

        if ( source.equals( showSynopsis ) ) {
            boolean state = showSynopsis.isSelected();
            prefs.putBoolean( "PlotControlFrame_showsynopsis", state );
            plot.setShowSynopsis( state );
            return;
        }

        if ( source.equals( showLegend ) ) {
            boolean state = showLegend.isSelected();
            prefs.putBoolean( "PlotControlFrame_showlegend", state );
            plot.setShowLegend( state );
            return;
        }

        if ( source.equals( trackerLineIDs ) ) {
            boolean state = trackerLineIDs.isSelected();
            prefs.putBoolean( "PlotControlFrame_trackerlineids", state );
            plot.getSpecDataComp().setTrackerLineIDs( state );
            plot.updatePlot();
            return;
        }

        if ( source.equals( showVerticalMarks ) ) {
            boolean state = showVerticalMarks.isSelected();
            prefs.putBoolean( "PlotControlFrame_showverticalmarks", state );
            plot.getSpecDataComp().setShowVerticalMarks( state );
            plot.updatePlot();
            return;
        }

        if ( source.equals( prefixLineIDs ) ) {
            boolean state = prefixLineIDs.isSelected();
            prefs.putBoolean( "PlotControlFrame_prefixlineids", state );
            plot.getSpecDataComp().setPrefixLineIDs( state );
            if ( state ) {
                shortNameLineIDs.setSelected( false );
            }
            plot.updatePlot();
            return;
        }

        if ( source.equals( suffixLineIDs ) ) {
            boolean state = suffixLineIDs.isSelected();
            prefs.putBoolean( "PlotControlFrame_suffixlineids", state );
            plot.getSpecDataComp().setSuffixLineIDs( state );
            if ( state ) {
                shortNameLineIDs.setSelected( false );
            }
            plot.updatePlot();
            return;
        }

        if ( source.equals( shortNameLineIDs ) ) {
            boolean state = shortNameLineIDs.isSelected();
            prefs.putBoolean( "PlotControlFrame_shortnamelineids", state );
            plot.getSpecDataComp().setShortNameLineIDs( state );
            if ( state ) {
                prefixLineIDs.setSelected( false );
                suffixLineIDs.setSelected( false );
            }
            plot.updatePlot();
            return;
        }

        if ( source.equals( horizontalLineIDs ) ) {
            boolean state = horizontalLineIDs.isSelected();
            prefs.putBoolean( "PlotControlFrame_horizontallineids", state );
            plot.getSpecDataComp().setDrawHorizontalLineIDs( state );
            plot.updatePlot();
            return;
        }

        if ( source.equals( doubleDSBLineIDs ) ) {
            boolean state = doubleDSBLineIDs.isSelected();
            prefs.putBoolean( "PlotControlFrame_doubledsblineids", state );
            plot.getPlot().setDoubleDSBLineIds( state );
            plot.updatePlot();
            return;
        }
    }

    //
    // ActionListener interface for menubuttons that do not require a full
    // action.
    //
    public void actionPerformed( ActionEvent e )
    {
        Object source = e.getSource();

        if ( source.equals( removeCurrent ) ) {
            removeCurrentSpectrum();
            return;
        }

      /*  if ( source.equals( openSlapBrowser ) ) {
            slapBrowser = new SLAPBrowser( plot);
            return;
        }*/
        
        if ( source.equals( loadAllLineIDs ) ) {
            plot.loadLineIDs( true, doubleDSBLineIDs.isSelected(),
                              LocalLineIDManager.getInstance() );
            return;
        }

        if ( source.equals( loadLoadedLineIDs ) ) {
            plot.loadLineIDs( false, doubleDSBLineIDs.isSelected(),
                              LocalLineIDManager.getInstance() );
            return;
        }

        if ( source.equals( unloadLineIDs ) ) {
            plot.unloadLineIDs();
            return;
        }
    }
    
    //  add listener to slap browser -> load spectral line frames
}
