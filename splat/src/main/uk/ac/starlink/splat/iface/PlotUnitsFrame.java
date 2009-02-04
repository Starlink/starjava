/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 * Copyright (C) 2007-2009 Science and Technology Facilties Council
 *
 *  History:
 *     18-APR-2005 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.splat.ast.ASTJ;
import uk.ac.starlink.splat.data.LineIDSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.plot.PlotClickedListener;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Triple;
import uk.ac.starlink.splat.util.UnitUtilities;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * Window for choosing a pre-defined set of spectral coordinates and data
 * units for application to the current spectrum of a PlotControl instance.
 * <p>
 * These controls are a convenience and do not replace the fullers versions
 * of {@link SpecDataUnitsFrame} and {@link SpecCoordinatesFrame}.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PlotUnitsFrame
    extends JFrame
    implements ItemListener, PlotClickedListener
{
    /**
     * Reference to global list of spectra and plots.
     */
    private GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     * UI preferences.
     */
    private static Preferences prefs =
        Preferences.userNodeForPackage( PlotUnitsFrame.class );

    /**
     * The PlotControl we're associated with.
     */
    private PlotControl control = null;

    /**
     * Content pane of frame.
     */
    private JPanel contentPane = null;

    /**
     * Action buttons container.
     */
    private JPanel actionBar = new JPanel();

    /**
     * Menubar and various menus and items that it contains.
     */
    private JMenuBar menuBar = new JMenuBar();
    private JMenu fileMenu = new JMenu();

    /**
     * Control for displaying/changing the data units.
     */
    private JComboBox dataUnitsBox = null;

    /**
     * Control for displaying/changing the coordinate system.
     */
    private JComboBox coordinatesBox = null;

    /**
     * Control for selecting the DSB sideband.
     */
    private JComboBox sideBandBox = null;

    /**
     * Control for selecting spectral origin.
     */
    private JComboBox originBox = null;

    /**
     * Control for selecting standard of rest.
     */
    private JComboBox stdOfRestBox = null;

    /**
     * Text field for displaying the restfrequency.
     */
    private JTextField restFrequencyField = null;

    /**
     * How a new rest frequency pick should be handled. If true then
     * a search for the nearest line identifier position is made.
     */
    private boolean pickLineIdentifier = true;

    /**
     * Unknown units, could also be unrecognised.
     */
    private static final String UNKNOWN = "Unknown";
    private static final String UNKNOWN_LABEL = "Not set or unrecognised";

    /**
     * No units (redshift, beta factor).
     */
    private static final String NONE = "None";

    /**
     * List of the possible data units. Use canonical strings for
     * presentation and AST normalised forms for comparisons.
     */
    private static Vector dataUnitsVector = null;
    static {
        dataUnitsVector = new Vector();
        Frame testFrame = new Frame( 1 );
        dataUnitsVector.add( new Triple( UNKNOWN_LABEL, UNKNOWN, UNKNOWN ) );
        dataUnitsVector.add( new Triple( "Jansky", "Jy", "Jy" ) );
        dataUnitsVector.add( new Triple( "milliJansky", "mJy", "mJy" ) );
        String[] canonicalUnits = { "W/m^2/Hz",
                                    "W/m^2/Angstrom",
                                    "W/cm^2/um",
                                    "erg/cm^2/s/Hz",
                                    "erg/cm^2/s/Angstrom"
        };

        for ( int i = 0; i < canonicalUnits.length; i++ ) {
            testFrame.setUnit( 1, canonicalUnits[i] );
            dataUnitsVector.add( new Triple( canonicalUnits[i],
                                             canonicalUnits[i],
                                             testFrame.getC( "normunit" ) ) );
        }
    };

    /**
     * List of the possible spectral coordinates. These are presented to the
     * user as units only. The system is assumed.
     */
    private static Vector coordinateSystems = null;
    static {
        coordinateSystems = new Vector();
        coordinateSystems.add( new Triple( UNKNOWN_LABEL, UNKNOWN, UNKNOWN ) );
        coordinateSystems.add( new Triple( "Angstroms", "Angstrom", "WAVE" ) );
        coordinateSystems.add( new Triple( "Nanometres", "nm", "WAVE" ) );
        coordinateSystems.add( new Triple( "Micrometres", "um", "WAVE" ) );
        coordinateSystems.add( new Triple( "Millimetres", "mm", "WAVE" ) );
        coordinateSystems.add( new Triple( "Metres", "m", "WAVE" ) );
        coordinateSystems.add( new Triple( "Terahertz", "THz", "FREQ" ) );
        coordinateSystems.add( new Triple( "Gigahertz", "GHz", "FREQ" ) );
        coordinateSystems.add( new Triple( "Megahertz", "MHz", "FREQ" ) );
        coordinateSystems.add( new Triple( "Kilohertz", "kHz", "FREQ" ) );
        coordinateSystems.add( new Triple( "Hertz", "Hz", "FREQ" ) );
        coordinateSystems.add( new Triple( "Joules", "J", "ENER" ) );
        coordinateSystems.add( new Triple( "Ergs", "erg", "ENER" ) );
        coordinateSystems.add( new Triple( "Electron-volts", "eV", "ENER" ) );
        coordinateSystems.add( new Triple( "Kilo-electron-volts", "keV",
                                         "ENER" ) );
        coordinateSystems.add( new Triple( "Metres-per-sec (radio)", "m/s",
                                         "VRAD" ) );
        coordinateSystems.add( new Triple( "Kilometres-per-sec (radio)", "km/s",
                                         "VRAD" ) );
        coordinateSystems.add( new Triple( "Redshift", NONE, "ZOPT" ) );
        coordinateSystems.add( new Triple( "Kilometres-per-sec (rela)", "km/s",
                                         "VELO" ) );
        coordinateSystems.add( new Triple( "Kilometres-per-sec (opt)", "km/s",
                                         "VOPT" ) );
        coordinateSystems.add( new Triple( "Per-metre", "1/m", "WAVN" ) );
    };

    /**
     * List of the possible sidebands for DSBSpecFrames. Only two, but
     * this is confused by the fact that LSB and USB can also be known
     * as the observed and image sidebands (observed includes the central
     * frequency, the choice is determined by the value of IF, if > 0 then
     * LSB=observed and USB=image, otherwise LSB=image and USB=observed).
     */
    private Map sideBandMap = ifPosSideBandMap;
    private DefaultComboBoxModel ifPosSideBandModel = null;
    private static Map ifPosSideBandMap = null;
    static {
        ifPosSideBandMap = new LinkedHashMap();
        ifPosSideBandMap.put( UNKNOWN_LABEL, UNKNOWN );
        ifPosSideBandMap.put( "Lower (observed)", "LSB" );
        ifPosSideBandMap.put( "Upper (image)", "USB" );
        ifPosSideBandMap.put( "Offset from LO", "LO" );
    };

    private DefaultComboBoxModel ifNegSideBandModel = null;
    private static Map ifNegSideBandMap = null;
    static {
        ifNegSideBandMap = new LinkedHashMap();
        ifNegSideBandMap.put( UNKNOWN_LABEL, UNKNOWN );
        ifNegSideBandMap.put( "Lower (image)", "LSB" );
        ifNegSideBandMap.put( "Upper (observed)", "USB" );
        ifNegSideBandMap.put( "Offset from LO", "LO" );
    };

    /** Default spectral origin */
    private double originDefault = 0.0;

    /** Default spectral units, need a Frame for complete coverage */
    private Frame originDefaultFrame = null;

    /**
     * List of the possible origins.
     */
    private static Map originMap = null;
    static {
        originMap = new LinkedHashMap();
        originMap.put( "Default", "Default" );
        originMap.put( "Rest Frequency", "RestFreq" );
        originMap.put( "None", "None" );
    };

    /**
     * List of the possible standards of rest.
     */
    private static Map stdOfRestMap = null;
    static {
        stdOfRestMap = new LinkedHashMap();
        stdOfRestMap.put( UNKNOWN_LABEL, UNKNOWN );
        stdOfRestMap.put( "Observer", "Topocentric" );
        stdOfRestMap.put( "Centre of Earth", "Geocentric" );
        stdOfRestMap.put( "Solar system barycentre", "Barycentric" );
        stdOfRestMap.put( "Centre of Sun", "Heliocentric" );
        stdOfRestMap.put( "Kinematical local standard of rest", "LSRK" );
        stdOfRestMap.put( "Dynamical local standard of rest", "LSRD" );
        stdOfRestMap.put( "Galactic centre", "Galactic" );
        stdOfRestMap.put( "Local group", "Local_group" );
        stdOfRestMap.put( "Source", "Source" );
    };

    /**
     * Create an instance.
     *
     * @plot a PlotControl instance that specifies the current spectrum.
     */
    public PlotUnitsFrame( PlotControl control )
    {
        contentPane = (JPanel) getContentPane();
        this.control = control;
        initUI();
        initFrame();

        //  Listen for changes to the current spectrum. When this happens
        //  update the displayed units.
        control.addItemListener( this );
        matchCurrentSpectrum();
    }

    /**
     * Initialise the main part of the user interface.
     */
    protected void initUI()
    {
        contentPane.setLayout( new BorderLayout() );

        // Add the menuBar.
        setJMenuBar( menuBar );

        // Create the File menu.
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Create the Help menu.
        HelpFrame.createHelpMenu( "simpleunits-window", "Help on window",
                                  menuBar, null );
        initControlArea();
    }

    /**
     * Create the main controls.
     */
    protected void initControlArea()
    {
        JPanel panel = new JPanel();
        contentPane.add( panel, BorderLayout.CENTER );

        // Action bar uses a BoxLayout and is placed at the south.
        JPanel controlsPanel = new JPanel();
        contentPane.add( controlsPanel, BorderLayout.SOUTH );

        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        controlsPanel.add( actionBar, BorderLayout.SOUTH );

        // Add an action to convert from the existing coordinates and data
        // units to some new values.
        ApplyAction applyAction = new ApplyAction();
        fileMenu.add( applyAction ).setMnemonic( KeyEvent.VK_A );

        JButton applyButton = new JButton( applyAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( applyButton );
        actionBar.add( Box.createGlue() );

        // Add an action to close the window (appears in File menu and action
        // bar).
        CloseAction closeAction = new CloseAction();
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        actionBar.add( Box.createGlue() );

        // The control panel holds the interesting components that describe
        // the coordinates and data units of the selected spectra.
        JPanel specFramePanel = initControls();
        panel.add( specFramePanel, BorderLayout.CENTER );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Current Spectrum Units" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        Utilities.setFrameLocation( this, null, prefs, "PlotUnitsFrame" );
        pack();
        setVisible( true );
    }

    /**
     * Create the controls for displaying/changing the units.
     */
    protected JPanel initControls()
    {
        JPanel panel = new JPanel();
        GridBagLayouter gbl = new GridBagLayouter( panel,
                                                   GridBagLayouter.SCHEME2 );

        //  Spectral coordinates
        coordinatesBox = new JComboBox( coordinateSystems );
        JLabel label = new JLabel( "Coordinates: " );
        gbl.add( label, false );
        gbl.add( coordinatesBox, false );
        coordinatesBox.setToolTipText( "Units of the spectral coordinates" );

        //  DSB side band, start with positive IF assumption.
        ifNegSideBandModel =
            new DefaultComboBoxModel( ifNegSideBandMap.keySet().toArray() );
        ifPosSideBandModel =
            new DefaultComboBoxModel( ifPosSideBandMap.keySet().toArray() );
        sideBandBox = new JComboBox( ifPosSideBandModel );
        label = new JLabel( "SideBand: " );
        gbl.add( label, false );
        gbl.add( sideBandBox, false );
        gbl.eatLine();
        sideBandBox.setToolTipText
            ( "Current sideband when display dual sideband data" );

        //  Data units
        dataUnitsBox = new JComboBox( dataUnitsVector );
        label = new JLabel( "Data units: " );
        gbl.add( label, false );
        gbl.add( dataUnitsBox, false );
        dataUnitsBox.setToolTipText( "Units of the data values" );

        //  Spectral origin
        originBox = new JComboBox( originMap.keySet().toArray() );
        label = new JLabel( "Origin: " );
        gbl.add( label, false );
        gbl.add( originBox, false );
        gbl.eatLine();
        originBox.setToolTipText( "Origin of spectral coordinates" );

        //  Standard of rest
        stdOfRestBox = new JComboBox( stdOfRestMap.keySet().toArray() );
        label = new JLabel( "Standard of rest: " );
        gbl.add( label, false );
        gbl.add( stdOfRestBox, false );
        stdOfRestBox.setToolTipText( "Standard of rest for coordinates" );


        //  Text field for displaying the rest frequency (cannot be
        //  changed by editting.
        label = new JLabel( "Rest Frequency: " );
        gbl.add( label, false );
        restFrequencyField = new JTextField();
        restFrequencyField.setEditable( false );
        gbl.add( restFrequencyField, false );
        restFrequencyField.setToolTipText( "Current rest frequency in GHz" );
        gbl.eatLine();

        //  Add buttons to pick the a line identifier position as the
        //  rest frequency, or just a position.
        gbl.add( Box.createGlue(), false );
        gbl.add( Box.createGlue(), false );
        JButton restFrequencyIdButton = new JButton( "Pick ID" );
        restFrequencyIdButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e )
                {
                    pickRestFrequency( true );
                }
            });
        gbl.add( restFrequencyIdButton, false );
        restFrequencyIdButton.setToolTipText
            ( "Pick line identifier as the rest frequency" );

        JButton restFrequencyPosButton = new JButton( "Pick pos" );
        restFrequencyPosButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e )
                {
                    pickRestFrequency( false );
                }
            });
        gbl.add( restFrequencyPosButton, false );
        restFrequencyPosButton.setToolTipText
            ( "Pick plot position for the rest frequency" );
        gbl.eatLine();
        gbl.eatSpare();

        return panel;
    }

    /**
     * Match the UI to the properties of the current spectrum. If the
     * units cannot be matched against the list of possibles then
     * {@link UNKNOWN} is shown.
     */
    protected void matchCurrentSpectrum()
    {
        SpecData spectrum = control.getCurrentSpectrum();
        ASTJ astJ = spectrum.getAst();
        FrameSet frameSet = astJ.getRef();

        boolean isDSB = astJ.isFirstAxisDSBSpecFrame();
        String sideband = UNKNOWN;
        double interFreq = 1.0;
        if ( isDSB ) {
            sideband = frameSet.getC( "SideBand" );
            interFreq = frameSet.getD( "IF" );
        }

        String stdOfRest = UNKNOWN;
        if ( astJ.isFirstAxisSpecFrame() ) {
            stdOfRest = frameSet.getC( "StdOfRest" );
        }

        String coordUnits = frameSet.getC( "unit(1)" );
        String dataUnits = frameSet.getC( "normunit(2)" );
        try {
            originDefault = frameSet.getD( "SpecOrigin" );
            originDefaultFrame = (Frame)
                frameSet.getFrame( FrameSet.AST__CURRENT ).copy();

            //  Needs a SpecFrame too.
            restFrequencyField.setText( frameSet.getC( "RestFreq" ) );
        }
        catch (AstException e) {
            //  Not a SpecFrame, no SpecOrigin.
            originDefault = 0.0;
            originDefaultFrame = null;
        }

        //  Need to transform these into local strings. We use the normalised
        //  forms so that comparison of dimensionally similar units succeeds.
        Iterator i = dataUnitsVector.iterator();
        dataUnitsBox.setSelectedIndex( 0 ); //  Unknown
        Triple triple = null;
        while ( i.hasNext() ) {
            triple = (Triple) i.next();
            if ( triple.gets3().equals( dataUnits ) ) {
                dataUnitsBox.setSelectedItem( triple );
                break;
            }
        }

        i = coordinateSystems.iterator();
        coordinatesBox.setSelectedIndex( 0 ); // Unknown
        String units;
        while ( i.hasNext() ) {
            triple = (Triple) i.next();
            units = triple.gets2();
            if ( ! units.equals( NONE ) && units.equals( coordUnits ) ) {
                coordinatesBox.setSelectedItem( triple );
                break;
            }
        }

        if ( interFreq > 0 ) {
            sideBandMap = ifPosSideBandMap;
            sideBandBox.setModel( ifPosSideBandModel );
        }
        else {
            sideBandMap = ifNegSideBandMap;
            sideBandBox.setModel( ifNegSideBandModel );
        }
        Set entrySet = sideBandMap.entrySet();
        i = entrySet.iterator();
        sideBandBox.setSelectedIndex( 0 ); //  Unknown
        Map.Entry entry = null;
        while ( i.hasNext() ) {
            entry = (Map.Entry) i.next();
            if ( entry.getValue().equals( sideband ) ) {
                sideBandBox.setSelectedItem( (String) entry.getKey() );
                break;
            }
        }

        sideBandBox.setEnabled( isDSB );

        entrySet = stdOfRestMap.entrySet();
        i = entrySet.iterator();
        stdOfRestBox.setSelectedIndex( 0 ); //  Unknown
        while ( i.hasNext() ) {
            entry = (Map.Entry) i.next();
            if ( entry.getValue().equals( stdOfRest ) ) {
                stdOfRestBox.setSelectedItem( (String) entry.getKey() );
                break;
            }
        }
    }

    /**
     * Apply the current units as selected in the UI controls to
     * the current spectrum.
     */
    protected void matchUIUnits()
    {
        //  Get data units in canonical form.
        Triple triple = (Triple) dataUnitsBox.getSelectedItem();
        String dataUnits = (String) triple.gets2();

        triple = (Triple) coordinatesBox.getSelectedItem();
        String coordUnits = triple.gets2();
        if ( coordUnits.equals( NONE ) ) {
            coordUnits = "";
        }
        String coordSystem = triple.gets3();

        String sideBand = (String) sideBandBox.getSelectedItem();
        sideBand = (String) sideBandMap.get( sideBand );
        if ( ! sideBandBox.isEnabled() ) {
            sideBand = UNKNOWN;
        }
        if ( dataUnits.equals( UNKNOWN ) &&
             coordUnits.equals( UNKNOWN ) ) {
            new SplatException
                ( this, "Cannot convert when current units are unknown",
                  "Unknown data units", JOptionPane.WARNING_MESSAGE );
            return;
        }

        //  See if we need to set the spectral origin. Three states, default,
        //  unset and set to the rest frequency.
        String origin = (String) originBox.getSelectedItem();
        origin = (String) originMap.get( origin );

        //  See if we need to set the standard of rest.
        String restframe = (String) stdOfRestBox.getSelectedItem();
        restframe = (String) stdOfRestMap.get( restframe );

        //  And apply to current spectrum,
        SpecData spec = control.getCurrentSpectrum();
        convertToUnits( spec, dataUnits, coordUnits, coordSystem, sideBand,
                        origin, restframe );
    }

    /**
     * Convert current units of a given spectrum to some new values.
     * Units that are {@link UNKNOWN} are skipped and remain at their
     * current values.
     */
    protected void convertToUnits( SpecData spec, String dataUnits,
                                   String coordUnits, String coordSystem,
                                   String sideBand, String origin,
                                   String stdOfRest )
    {
        if ( ! dataUnits.equals( UNKNOWN ) ) {
            try {
                SpecDataUnitsFrame.convertToUnits( spec, dataUnits );
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }

        if ( ! coordUnits.equals( UNKNOWN ) ) {
            try {
                int iaxis = spec.getMostSignificantAxis();
                String attributes =
                    "System=" + coordSystem + ",Unit("+iaxis+")=" + coordUnits;
                if ( ! sideBand.equals( UNKNOWN ) ) {
                    attributes = attributes + ",SideBand=" + sideBand;
                }
                if ( ! stdOfRest.equals( UNKNOWN ) ) {
                    attributes = attributes + ",StdOfRest=" + stdOfRest;
                }
                SpecCoordinatesFrame.convertToAttributes( spec, attributes,
                                                          iaxis, false );
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }

        //  Now that the transformations are complete deal with the origin
        //  transformation.
        if ( originDefaultFrame != null ) {

            // Note here we use the plot 2D frameset for conversions etc.
            // (this will be updated above), but apply the changes to the
            // underlying frameset, so the changes are used when the plot
            // frameset is regenerated.
            FrameSet plotFrameSet = spec.getAst().getRef();
            if ( "Default".equals( origin ) ) {
                if ( originDefault != 0.0 ) {
                    //  Transform to the new coordinates from the ones that
                    //  the default is in.
                    plotFrameSet.clear( "SpecOrigin" );
                    double newOrigin =
                        UnitUtilities.convert( plotFrameSet, 1,
                                               originDefaultFrame, 1,
                                               true, originDefault );
                    spec.getFrameSet().setD( "SpecOrigin", newOrigin );
                }
                else {
                    //  Default is 0.0 so clear.
                    spec.getFrameSet().clear( "SpecOrigin" );
                }
            }
            else if ( "RestFreq".equals( origin ) ) {
                //  Get the rest frequency, that's always in GHz.
                double restfreq = plotFrameSet.getD( "RestFreq" );
                //  Transform to the new coordinates from the ones that
                //  the default is in.
                plotFrameSet.clear( "SpecOrigin" );
                double newOrigin =
                    UnitUtilities.convert( plotFrameSet, 1,
                                           "System=FREQ,Unit=GHz",
                                           true, false, restfreq );
                spec.getFrameSet().setD( "SpecOrigin", newOrigin );
            }
            else {
                spec.getFrameSet().clear( "SpecOrigin" );
            }

            // Do a full update to get the changes propagated throughout.
            try {
                spec.initialiseAst();
                globalList.notifySpecListenersModified( spec );
            }
            catch (SplatException e) {
                //  Not fatal so just trap.
            }
        }
    }

    /**
     * Pick a new rest frequency. If id is true then an attempt to select
     * a line identifier position is made, otherwise the point clicked
     * on the plot is used.
     */
    protected void pickRestFrequency( boolean id )
    {
        //  Pass id value to plotClicked method.
        pickLineIdentifier = id;

        //  Register as a listener for clicks on the plot.
        DivaPlot divaPlot = control.getPlot();
        divaPlot.addPlotClickedListener( this );

        //  Raise the plot to indicate that an interaction should begin.
        SwingUtilities.getWindowAncestor( divaPlot ).toFront();
    }

    /**
     * Use a picked position to set the rest frequency. If pickLineIdentifier
     * is set true then the strategy is to search the plot for a line
     * identifier position that is nearest to the clicked position, otherwise
     * the coordinates of the clicked point are used.
     */
    public void plotClicked( MouseEvent e )
    {
        DivaPlot divaPlot = control.getPlot();

        //  Must be a click of mouse button 1.
        if ( e.getButton()  != MouseEvent.BUTTON1 ) {
            return;
        }

        //  Clicked position.
        int xg = e.getX();

        SpecData currentSpectrum = control.getCurrentSpectrum();

        //  Transform this into world coordinates of the current spectrum.
        SpecDataComp specDataComp = divaPlot.getSpecDataComp();
        double[] mainCoords =
            specDataComp.lookup( xg, (Plot) divaPlot.getMapping() );

        //  Picked coordinate and index of associated spectrum (zero is
        //  current).
        double bestcoord = -1.0;
        if ( pickLineIdentifier ) {
            //  Iterate over all plot spectra looking for line identifiers
            //  when located get the nearest position and the offset.
            double[] lineCoords = null;
            double[] localCoords = null;
            double diff = Double.MAX_VALUE;
            SpecData[] specData =
                control.getPlot().getSpecDataComp().get();
            int bestindex = -1;
            for ( int i = 0; i < specData.length; i++ ) {
                if ( specData[i] instanceof LineIDSpecData ) {
                    lineCoords = specDataComp.transformCoords( specData[i],
                                                               mainCoords,
                                                               false );
                    //  Look for nearest position in this spectrum.
                    lineCoords = specData[i].nearest( lineCoords[0] );

                    //  Back to coordinates of the main spectrum.
                    localCoords = specDataComp.transformCoords( specData[i],
                                                                lineCoords,
                                                                true );

                    //  If this is nearer then save index and coordinate.
                    if ( Math.abs( localCoords[0] - mainCoords[0] ) < diff ) {
                        bestindex = i;
                        bestcoord = lineCoords[0];

                        //  Nearest diff.
                        diff = Math.abs( localCoords[0] - mainCoords[0] );
                    }
                }
            }
            if ( bestindex != -1 ) {
                int index = specData[bestindex].nearestIndex( bestcoord );
                bestcoord =
                    ((LineIDSpecData)specData[bestindex]).getFrequency(index);
            }
        }
        else {
            //  The rest frequency can be supplied with various units
            //  but not any velocity ones (which are the most useful for this
            //  feature), so need to transform to GHz.
            FrameSet frameSet = currentSpectrum.getAst().getRef();
            try {
                bestcoord = UnitUtilities.convert( frameSet, 1,
                                                   "System=FREQ,Unit=GHz",
                                                   true, true, mainCoords[0] );
            }
            catch (AstException ae) {
                //  Doing nothing, probably not a SpecFrame.
                bestcoord = -1.0;
            }
        }
        if ( bestcoord > -1.0 ) {
            String restfreq = "RestFreq=" + bestcoord + "GHz";
            try {
                SpecCoordinatesFrame.convertToAttributes
                    ( currentSpectrum, restfreq,
                      currentSpectrum.getMostSignificantAxis(), false );
                restFrequencyField.setText( Double.toString( bestcoord ) );
            }
            catch (SplatException se) {
                ErrorDialog.showError( this, se );
            }
        }

        //  Remove the listener, once only interaction.
        divaPlot.removePlotClickedListener( this );
    }

    /**
     * Close the window.
     */
    protected void closeWindowEvent()
    {
        Utilities.saveFrameLocation( this, prefs, "PlotUnitsFrame" );

        //  Make sure we are removed as a PlotClickedListener.
        control.getPlot().removePlotClickedListener( this );
        dispose();
    }

    private final static ImageIcon closeImage =
        new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction
        extends AbstractAction
    {
        public CloseAction()
        {
            super( "Close", closeImage );
            putValue( SHORT_DESCRIPTION, "Close window" );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control W" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            closeWindowEvent();
        }
    }

    private final static ImageIcon modifyImage =
        new ImageIcon( ImageHolder.class.getResource( "modify.gif" ) );

    /**
     * Inner class defining Action for converting the AST framesets to
     * the current set of attributes.
     */
    protected class ApplyAction
        extends AbstractAction
    {
        public ApplyAction()
        {
            super( "Apply", modifyImage );
            putValue( SHORT_DESCRIPTION,
                      "Apply units change to current spectrum" );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control A" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            matchUIUnits();
        }
    }

    //
    // ItemListener interface. Respond to changes of the plot current spectrum.
    //
    public void itemStateChanged( ItemEvent e )
    {
        if ( e.getStateChange() == ItemEvent.SELECTED ) {
            matchCurrentSpectrum();
        }
    }
}
