/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 * Copyright (C) 2007 Particle Physics and Astronomy Research Council
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     07-APR-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.SpecFrame;
import uk.ac.starlink.pal.Pal;
import uk.ac.starlink.pal.Observatory;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * Window for choosing to set or modify the properties of the X axis
 * coordinates of a set of selected spectra. This allows the correct
 * coordinate system, units etc. to the applied to a spectrum, or for
 * it to be transformed into another system, units, etc.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SpecCoordinatesFrame
    extends JFrame
    implements ActionListener
{
    /**
     * Reference to global list of spectra and plots.
     */
    private static GlobalSpecPlotList globalList =
        GlobalSpecPlotList.getInstance();

    /**
     * UI preferences.
     */
    private static Preferences prefs =
        Preferences.userNodeForPackage( SpecCoordinatesFrame.class );

    /**
     * A related JList that is displaying the global list of spectra.
     */
    private JList specList = null;

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
    private JMenu optionsMenu = new JMenu();

    /**
     * Control for displaying/changing the system.
     */
    private JComboBox systemBox = null;

    /**
     * Control for displaying/changing the system units.
     */
    private JComboBox systemUnitsBox = null;

    /**
     * Control for selecting the standard of rest.
     */
    private JComboBox stdOfRestBox = null;

    /**
     * Control for observer longitude
     */
    private JTextField obsLong = null;

    /**
     * Control for observer latitude
     */
    private JTextField obsLat = null;

    /**
     * List of known observatories (used to get lat and long)
     */
    private JComboBox observatoryBox = null;

    /**
     * Control for source RA
     */
    private JTextField sourceRA = null;

    /**
     * Control for source Dec
     */
    private JTextField sourceDec = null;

    /**
     * Rest frequency
     */
    private JTextField restFrequency = null;

    /**
     * Rest frequency units
     */
    private JComboBox restFrequencyUnits = null;

    /**
     * Spectral origin. Special attribute. Uses units of spectrum and is
     * transformed with the spectrum. A value of 0 is taken to mean clear
     * (quite different from setting to 0). Cannot apply from the UI unless it
     * is changed as the units are not matched.
     */
    private JTextField specOrigin = null;
    private String specOriginDefault = null;

    /**
     * Control for source velocity.
     */
    private JTextField sourceVel = null;

    /**
     * Control for selecting the source standard of rest;
     */
    private JComboBox sourceStdOfRestBox = null;

    /**
     * Control for the source velocity spectral system.
     */
    private JComboBox sourceSystemBox = null;

    /**
     * Control for date-obs.
     */
    private JTextField dateObs = null;

    /**
     * AST names and descriptions for the various spectral systems
     */
    protected static Map astSystems = null;

    /**
     * Inverse of AST names and descriptions for the various spectral systems
     */
    protected static Map astSystemsInverse = null;

    /**
     * Default units for each system. Possible values are described in
     * SUN/211, "The Unit Attribute". The complexities of this mean
     * that a string must be used, rather than a preset list of units.
     *
     */
    protected static Map systemUnits = null;

    /**
     * List of useful units.
     */
    protected static Map unitsMap = null;

    /**
     * Possible standards of rest.
     */
    protected static Map stdOfRest = null;

    /**
     * Possible standards of rest for the source (minus Source).
     */
    protected static Map sourceStdOfRest = null;

    /**
     * Inverse of possible standards of rest mappings.
     */
    protected static Map stdOfRestInverse = null;

    /**
     * Possible systems for supplying a source velocity.
     */
    protected static Map sourceSystems = null;

    /**
     * Inverse of possible systems for supplying a source velocity.
     */
    protected static Map sourceSystemsInverse = null;

    /**
     * Known observatories and their identifications.
     */
    protected static Map observatories = null;

    /**
     * Postional astronomy object.
     */
    protected static Pal palReference = new Pal();

    /**
     * AST SpecFrame, used for formatting values.
     */
    private static SpecFrame aSpecFrame = new SpecFrame();

    /**
     * Dependencies of standard of rest.
     */
    //protected static Map stdDependOn = null;

    /**
     * Initialization of AST system and units information.
     */
    static {
        //  Note use LinkedHashMap so that iteration order is same as
        //  insertion.
        astSystems = new LinkedHashMap( 11 );
        astSystems.put( "Unknown", "" );
        astSystems.put( "Frequency", "FREQ" );
        astSystems.put( "Energy", "ENER" );
        astSystems.put( "Wave number", "WAVN" );
        astSystems.put( "Wave-length in vacuum", "WAVE" );
        astSystems.put( "Wave-length in air", "AWAV" );
        astSystems.put( "Radio velocity", "VRAD" );
        astSystems.put( "Optical velocity", "VOPT" );
        astSystems.put( "Redshift", "ZOPT" );
        astSystems.put( "Beta factor", "BETA" );
        astSystems.put( "Relativistic velocity", "VELO" );

        // XXX must be a better way to do this!
        astSystemsInverse = new LinkedHashMap( 11 );
        astSystemsInverse.put( "", "Unknown" );
        astSystemsInverse.put( "FREQ", "Frequency" );
        astSystemsInverse.put( "ENER", "Energy" );
        astSystemsInverse.put( "WAVN", "Wave number" );
        astSystemsInverse.put( "WAVE", "Wave-length in vacuum" );
        astSystemsInverse.put( "AWAV", "Wave-length in air" );
        astSystemsInverse.put( "VRAD", "Radio velocity" );
        astSystemsInverse.put( "VOPT", "Optical velocity" );
        astSystemsInverse.put( "ZOPT", "Redshift" );
        astSystemsInverse.put( "BETA", "Beta factor" );
        astSystemsInverse.put( "VELO", "Relativistic velocity" );

        systemUnits = new LinkedHashMap( 11 );
        systemUnits.put( "Unknown", "" );
        systemUnits.put( "Frequency", "GHz" );
        systemUnits.put( "Energy", "J" );
        systemUnits.put( "Wave number", "1/m" );
        systemUnits.put( "Wave-length in vacuum", "Angstrom" );
        systemUnits.put( "Wave-length in air", "Angstrom" );
        systemUnits.put( "Radio velocity", "km/s" );
        systemUnits.put( "Optical velocity", "km/s" );
        systemUnits.put( "Redshift", "" );
        systemUnits.put( "Beta factor", "" );
        systemUnits.put( "Relativistic velocity", "km/s" );

        unitsMap = new LinkedHashMap( 15 );
        unitsMap.put( "", "" );
        unitsMap.put( "Angstroms", "Angstrom" );
        unitsMap.put( "Nanometres", "nm" );
        unitsMap.put( "Millimetres", "mm" );
        unitsMap.put( "Micrometres", "um" );
        unitsMap.put( "Metres", "m" );
        unitsMap.put( "Terahertz", "THz" );
        unitsMap.put( "Gigahertz", "GHz" );
        unitsMap.put( "Megahertz", "MHz" );
        unitsMap.put( "Kilohertz", "kHz" );
        unitsMap.put( "Hertz", "Hz" );
        unitsMap.put( "Joules", "J" );
        unitsMap.put( "Ergs", "erg" );
        unitsMap.put( "Electron-volts", "eV" );
        unitsMap.put( "Kilo-electron-volts", "keV" );
        unitsMap.put( "Metres-per-sec", "m/s" );
        unitsMap.put( "Kilometres-per-sec", "km/s" );
        unitsMap.put( "Per-metre", "1/m" );

        stdOfRest = new LinkedHashMap( 10 );
        stdOfRest.put( "Observer", "Topocentric" );
        stdOfRest.put( "Centre of Earth", "Geocentric" );
        stdOfRest.put( "Solar system barycentre", "Barycentric" );
        stdOfRest.put( "Centre of Sun", "Heliocentric" );
        stdOfRest.put( "Kinematical local standard of rest", "LSRK" );
        stdOfRest.put( "Dynamical local standard of rest", "LSRD" );
        stdOfRest.put( "Galactic centre", "Galactic" );
        stdOfRest.put( "Local group", "Local_group" );
        stdOfRest.put( "Source", "Source" );

        stdOfRestInverse = new LinkedHashMap( 10 );
        stdOfRestInverse.put( "Topocentric", "Observer" );
        stdOfRestInverse.put( "Geocentric", "Centre of Earth" );
        stdOfRestInverse.put( "Barycentric", "Solar system barycentre" );
        stdOfRestInverse.put( "Heliocentric", "Centre of Sun"  );
        stdOfRestInverse.put( "LSRK", "Kinematical local standard of rest" );
        stdOfRestInverse.put( "LSRD", "Dynamical local standard of rest" );
        stdOfRestInverse.put( "Galactic", "Galactic centre" );
        stdOfRestInverse.put( "Local_group", "Local group" );
        stdOfRestInverse.put( "Source", "Source" );

        sourceStdOfRest = new LinkedHashMap( 9 );
        sourceStdOfRest.put( "Observer", "Topocentric" );
        sourceStdOfRest.put( "Centre of Earth", "Geocentric" );
        sourceStdOfRest.put( "Solar system barycentre", "Barycentric" );
        sourceStdOfRest.put( "Centre of Sun", "Heliocentric" );
        sourceStdOfRest.put( "Kinematical local standard of rest", "LSRK" );
        sourceStdOfRest.put( "Dynamical local standard of rest", "LSRD" );
        sourceStdOfRest.put( "Galactic centre", "Galactic" );
        sourceStdOfRest.put( "Local group", "Local_group" );

        sourceSystems = new LinkedHashMap();
        sourceSystems.put( "Relativistic velocity", "VELO" );
        sourceSystems.put( "Radio velocity", "VRAD" );
        sourceSystems.put( "Optical velocity", "VOPT" );
        sourceSystems.put( "Redshift", "ZOPT" );
        sourceSystems.put( "Beta factor", "BETA" );

        sourceSystemsInverse = new LinkedHashMap();
        sourceSystemsInverse.put( "VELO", "Relativistic velocity" );
        sourceSystemsInverse.put( "VRAD", "Radio velocity" );
        sourceSystemsInverse.put( "VOPT", "Optical velocity" );
        sourceSystemsInverse.put( "ZOPT", "Redshift" );
        sourceSystemsInverse.put( "BETA", "Beta factor" );

        //stdDependOn = new LinkedHashMap( 8 );
        //stdDependOn.put( "Topocentric", "Epoch ObsLat ObsLon RefRA RefDec" );
        //stdDependOn.put( "Geocentric", "Epoch RefRA RefDec" );
        //stdDependOn.put( "Barycentric", "Epoch RefRA RefDec" );
        //stdDependOn.put( "Heliocentric", "RefRA RefDec" );
        //stdDependOn.put( "LSRK", "RefRA RefDec" );
        //stdDependOn.put( "LSRD", "RefRA RefDec" );
        //stdDependOn.put( "Galactic", "RefRA RefDec" );
        //stdDependOn.put( "Local_group", "RefRA RefDec" );
        //stdDependOn.put( "Source", "RefRA RefDec SourceVel" );

        // Map of observatories - full descriptions versus identifiers.
        int obsCount = Observatory.getObservatoryCount();
        observatories = new LinkedHashMap( obsCount + 1 );
        observatories.put( "", "NONE" );
        for ( int i = 0; i < obsCount; i++ ) {
            observatories.put( Observatory.getObservatoryName( i ),
                               Observatory.getObservatoryID( i ) );
        }
    };

    /**
     * Create an instance.
     *
     * @param selectionSource if not null then this defines another
     *                        JList those selection is to be copied to
     *                        our JList.
     */
    public SpecCoordinatesFrame( JList selectionSource )
    {
        contentPane = (JPanel) getContentPane();
        initUI();
        initFrame();

        if ( selectionSource != null ) {
            setSelectionFrom( selectionSource );
        }
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

        // Create the options menu.
        optionsMenu.setText( "Options" );
        optionsMenu.setMnemonic( KeyEvent.VK_O );
        menuBar.add( optionsMenu );

        //  Create the Help menu.
        HelpFrame.createHelpMenu( "specxcoord-window", "Help on window",
                                  menuBar, null );
        initSpecListArea();
        initControlArea();
        initOptions();
    }

    /**
     * Create a list to display the global list of spectra.
     */
    protected void initSpecListArea()
    {
        specList = new JList();
        JScrollPane scroller = new JScrollPane( specList );
        TitledBorder listTitle =
            BorderFactory.createTitledBorder( "Global list of spectra:" );
        scroller.setBorder( listTitle );
        contentPane.add( scroller, BorderLayout.CENTER );

        // The JList model is the global list of spectra.
        specList.setModel( new SpecListModel( specList.getSelectionModel() ) );

        // Listen for updates to the list selection.
        specList.addListSelectionListener( new ListSelectionListener()  {
                public void valueChanged( ListSelectionEvent e ) {
                    update( e );
                }
            });
    }

    /**
     * Create the main controls for displaying and changing the
     * selected spectral attributes.
     */
    protected void initControlArea()
    {
        JPanel panel = new JPanel( new BorderLayout() );
        contentPane.add( panel, BorderLayout.SOUTH );

        // Action bar uses a BoxLayout and is placed at the south.
        JPanel controlsPanel = new JPanel( new BorderLayout() );
        panel.add( controlsPanel, BorderLayout.SOUTH );

        actionBar.setLayout( new BoxLayout( actionBar, BoxLayout.X_AXIS ) );
        actionBar.setBorder( BorderFactory.createEmptyBorder( 3, 3, 3, 3 ) );
        controlsPanel.add( actionBar, BorderLayout.SOUTH );

        // Add an action to convert the AST framesets of the current
        // spectra.
        ConvertAction convertAction = new ConvertAction();
        fileMenu.add( convertAction ).setMnemonic( KeyEvent.VK_N );

        JButton convertButton = new JButton( convertAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( convertButton );
        actionBar.add( Box.createGlue() );

        // Add an action to set the AST framesets of the current
        // spectra.
        SetAction setAction = new SetAction();
        fileMenu.add( setAction ).setMnemonic( KeyEvent.VK_S );

        JButton setButton = new JButton( setAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( setButton );
        actionBar.add( Box.createGlue() );

        // Add an action to close the window (appears in File menu
        // and action bar).
        CloseAction closeAction = new CloseAction();
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( closeButton );
        actionBar.add( Box.createGlue() );

        // The control panel holds the interesting components that
        // describe the state of the selected spectra SpecFrames (if
        // any) and provide for the modification of SpecFrame
        // attributes.
        JPanel specFramePanel = initControls();
        specFramePanel.setBorder
            (BorderFactory.createTitledBorder("Spectral attribute controls"));
        panel.add( specFramePanel, BorderLayout.CENTER );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Coordinate system attributes" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        Utilities.setFrameLocation( this, null, prefs, "SpecCoordinatesFrame" );
        pack();
        setVisible( true );
    }

    /**
     * Create the controls for displaying/changing the values.
     */
    protected JPanel initControls()
    {
        JPanel panel = new JPanel();
        GridBagLayouter gbl = new GridBagLayouter( panel,
                                                   GridBagLayouter.SCHEME3 );

        // System
        systemBox = new JComboBox( astSystems.keySet().toArray() );
        systemBox.addActionListener( this );
        JLabel label = new JLabel( "Coordinate system: " );

        gbl.add( label, false );
        gbl.add( systemBox, true );

        // System units
        systemUnitsBox = new JComboBox( unitsMap.keySet().toArray() );
        systemUnitsBox.setEditable( true );
        systemUnitsBox.addActionListener( this );
        label = new JLabel( "Units: " );
        gbl.add( label, false );
        gbl.add( systemUnitsBox, true );
        systemUnitsBox.setToolTipText( "Units for the spectral " +
                                       "coordinate system" );

        // Standard of rest.
        stdOfRestBox = new JComboBox( stdOfRest.keySet().toArray() );
        //stdOfRestBox.addActionListener( this );
        label = new JLabel( "Standard of rest: " );
        gbl.add( label, false );
        gbl.add( stdOfRestBox, true );
        stdOfRestBox.setToolTipText( "Standard of rest for the " +
                                     "spectrum coordinates" );

        // Date of the observation.
        label = new JLabel( "Date of observation:" );
        dateObs = new JTextField();
        gbl.add( label, false );
        gbl.add( dateObs, true );
        dateObs.setToolTipText( "Epoch of the observation " +
                                "(J<decimal_year>, B<decimal_year>, " +
                                "MJD, JD, yyyy-mmm-dd.dd etc." );

        //  Observers longitude and latitude. These are slightly
        //  different from others as we offer the values for standard
        //  observatories.
        label = new JLabel( "Observatory:" );
        observatoryBox = new JComboBox( observatories.keySet().toArray() );
        observatoryBox.addActionListener( this );
        gbl.add( label, false );
        gbl.add( observatoryBox, true );
        observatoryBox.setToolTipText( "Choose longitude and" +
                                       " latitude from a list" +
                                       " of known observatories" );

        label = new JLabel( "Longitude of observer:" );
        obsLong = new JTextField();
        gbl.add( label, false );
        gbl.add( obsLong, true );
        String unitsHelp = "(degrees, +/-dd:mm:ss.ss, [N|S|W|E]dd:mm:ss.ss)";

        obsLong.setToolTipText( "The geodetic longitude of the " +
                                "observer " + unitsHelp );

        label = new JLabel( "Latitude of observer:" );
        obsLat = new JTextField();
        gbl.add( label, false );
        gbl.add( obsLat, true );
        obsLat.setToolTipText( "The geodetic latitude of the observer " +
                               unitsHelp );

        //  RA and Dec of source FK5 J2000
        label = new JLabel( "RA of source:" );
        sourceRA = new JTextField();
        gbl.add( label, false );
        gbl.add( sourceRA, true );
        unitsHelp = "(FK5 J2000.0, hh/dd:mm:ss.sss)";
        sourceRA.setToolTipText( "The right ascension of the " +
                                 "source" + unitsHelp );

        label = new JLabel( "Dec of source:" );
        sourceDec = new JTextField();
        gbl.add( label, false );
        gbl.add( sourceDec, true );
        sourceDec.setToolTipText( "The declination of the source " +
                                  unitsHelp );

        //  Rest frequency, can be qualified by a units string
        //  (wavelength or energy).
        label = new JLabel( "Rest frequency:" );
        JPanel restpanel = new JPanel();
        GridBagLayouter gbl2 = new GridBagLayouter( restpanel,
                                                    GridBagLayouter.SCHEME2 );
        gbl2.setInsets( new Insets( 0, 0, 0, 0 ) );
        restFrequency = new JTextField();
        restFrequencyUnits = new JComboBox( unitsMap.keySet().toArray() );
        restFrequencyUnits.setEditable( true );
        restFrequencyUnits.addActionListener( this );
        restFrequencyUnits.setSelectedItem( "GHz" );
        gbl2.add( restFrequency, false );
        gbl2.add( restFrequencyUnits, true );
        gbl.add( label, false );
        gbl.add( restpanel, true );
        restFrequency.setToolTipText( "Rest frequency/wavelength/energy" +
                                      " of zero velocity ");
        restFrequencyUnits.setToolTipText( "Rest frequency/wavelength/energy" +
                                           " value units");
        //  Spectral origin, can be qualified by a units string
        //  (wavelength or energy).
        label = new JLabel( "Spectral origin:" );
        JPanel origpanel = new JPanel();
        GridBagLayouter gbl3 = new GridBagLayouter( origpanel,
                                                    GridBagLayouter.SCHEME2 );
        gbl3.setInsets( new Insets( 0, 0, 0, 0 ) );
        specOrigin = new JTextField();
        gbl3.add( specOrigin, false );
        gbl.add( label, false );
        gbl.add( origpanel, true );
        specOrigin.setToolTipText( "Origin for spectral values (displays" +
                                   " offset values from this position)," +
                                   " same units as spectrum (0 to reset)" );

        //  Source rest frame.
        label = new JLabel( "Source rest frame:" );
        sourceStdOfRestBox =
            new JComboBox( sourceStdOfRest.keySet().toArray() );
        gbl.add( label, false );
        gbl.add( sourceStdOfRestBox, true );
        sourceStdOfRestBox.setToolTipText( "Standard of rest for the" +
                                           " source velocity" );

        //  Source velocity system.
        label = new JLabel( "Source system:" );
        sourceSystemBox =
            new JComboBox( sourceSystems.keySet().toArray() );
        gbl.add( label, false );
        gbl.add( sourceSystemBox, true );
        sourceStdOfRestBox.setToolTipText
            ( "Coordinate system used for specifying the source velocity" );

        //  Source velocity.
        label = new JLabel( "Source velocity:" );
        sourceVel = new JTextField();
        gbl.add( label, false );
        gbl.add( sourceVel, true );
        sourceVel.setToolTipText
            ( "The velocity of source in current source system units" );
        return panel;
    }

    /**
     * Add various options to the Options menu.
     */
    protected void initOptions()
    {
        JMenuItem lineIdValues = new JMenuItem( "Line ID defaults" );
        lineIdValues.addActionListener( this );
        lineIdValues.setActionCommand( "set1" );
        optionsMenu.add( lineIdValues );
    }

    /**
     * Set the selection of the JList to the same as another list
     * (which is presumably showing the global list).
     */
    public void setSelectionFrom( JList fromList )
    {
        specList.setSelectedIndices( fromList.getSelectedIndices() );
    }

    /**
     * Set the observers longitude and latitude to those of a known
     * observatory.
     */
    protected void setObservatory()
    {
        String selected = (String) observatoryBox.getSelectedItem();
        if ( selected != null && ! "".equals( selected ) ) {
            String id = (String ) observatories.get( selected );
            try {
                Observatory obs = palReference.Obs( id );
                // Longitude is west-positive, should be east-positive
                // for AST. Note we use AST to format the value.
                aSpecFrame.setD( "ObsLon", -1.0 * obs.getLong() * Pal.R2D );
                aSpecFrame.setD( "ObsLat", obs.getLat() * Pal.R2D );
                obsLong.setText( aSpecFrame.getC( "ObsLon" ) );
                obsLat.setText( aSpecFrame.getC( "ObsLat" ) );
            }
            catch (Exception e) {
                obsLong.setText( "" );
                obsLat.setText( "" );
            }
        }
        else {
            obsLong.setText( "" );
            obsLat.setText( "" );
        }
    }

    /**
     * Make the currently selected spectra have the attributes that
     * are currently described by the user interface. If asked this
     * can be a "set" or "convert" operation. If "convert" then the
     * spectra must already have a SpecFrame.
     */
    public void matchCoordType( boolean set )
    {
        //  Get a list of the selected spectra.
        int[] indices = specList.getSelectedIndices();
        if ( indices.length != 0 ) {

            //  Gather the current state of the interface as an AST attributes
            //  string.
            String attributes = gatherAttributes();

            //  Only set spectral origin when it has changed. When it is set
            //  to zero we really need to clear the value instead.
            String value = specOrigin.getText();
            boolean clearOrigin = false;
            if ( isValid( value ) ) { 

                //  Check for zero, means clear this attribute.
                try {
                    double origin = Double.parseDouble( value );
                    if ( origin == 0.0 ) {
                        clearOrigin = true;
                    }
                }
                catch (NumberFormatException e) {
                    //  Do nothing, just this for information.
                    e.printStackTrace();
                }
                if ( ! clearOrigin && 
                     ( set || ! value.equals( specOriginDefault ) ) ) {
                    attributes = attributes + ",SpecOrigin=" + value;
                }
            }

            //  And apply to all selected spectra.
            for ( int i = 0; i < indices.length; i++ ) {
                SpecData spec = globalList.getSpectrum( indices[i] );
                String fullats = attributes;

                // Get Units value. This is special as it can be axis
                // dependent when the underlying spectrum has redundant axes.
                int iaxis = spec.getMostSignificantAxis();
                String units = (String) systemUnitsBox.getEditor().getItem();
                if ( isValid( units ) ) {
                    fullats = attributes +  ",Unit(" + iaxis + ")=" + units;
                }
                try {
                    if ( set ) {
                        setToAttributes( spec, fullats, iaxis, clearOrigin );
                    }
                    else {
                        convertToAttributes( spec, fullats, iaxis, 
                                             clearOrigin );
                    }
                }
                catch (SplatException e) {
                    ErrorDialog.showError( this, e );
                }
            }

            //  Make interface match spectra. Need to do that as the spectral
            //  origin is transformed to the new coordinate system.
            update();
        }
    }

    /**
     * Gather information from interface and create a single String
     * suitable for applying to a FrameSet. Does not include Unit as this is
     * axis dependent.
     */
    protected String gatherAttributes()
    {
        StringBuffer buffer = new StringBuffer();

        String selected = (String) systemBox.getSelectedItem();
        String value = (String) astSystems.get( selected );
        if ( isValid( value ) ) {
            buffer.append( "System=" + value );
        }

        selected = (String) stdOfRestBox.getSelectedItem();
        value = (String) stdOfRest.get( selected );
        if ( isValid( value ) ) {
            buffer.append( ",StdOfRest=" + value );
        }

        value = obsLong.getText();
        if ( isValid( value ) ) {
            buffer.append( ",ObsLon=" + value );
        }

        value = obsLat.getText();
        if ( isValid( value ) ) {
            buffer.append( ",ObsLat=" + value );
        }

        value = sourceRA.getText();
        if ( isValid( value ) ) {
            buffer.append( ",RefRA=" + value );
        }

        value = sourceDec.getText();
        if ( isValid( value ) ) {
            buffer.append( ",RefDec=" + value );
        }

        value = restFrequency.getText();
        if ( isValid( value ) ) {
            String units = (String) restFrequencyUnits.getEditor().getItem();
            buffer.append( ",RestFreq=" + value + units );
        }

        selected = (String) sourceStdOfRestBox.getSelectedItem();
        value = (String) stdOfRest.get( selected );
        if ( isValid( value ) ) {
            buffer.append( ",SourceVRF=" + value );
        }

        //  Support old-style "z" indicating a redshift. In that case the
        //  system must be "REDSHIFT". Need to check this before setting the
        //  source system.
        String velocity = sourceVel.getText();
        if ( isValid( velocity ) ) {
            int index = velocity.indexOf( 'z' );
            if ( index != -1 ) {
                velocity = velocity.substring( 0, index );
                sourceSystemBox.setSelectedItem( "Redshift" );
            }
        }

        //  Source system.
        selected = (String) sourceSystemBox.getSelectedItem();
        value = (String) sourceSystems.get( selected );
        if ( isValid( value ) ) {
            buffer.append( ",SourceSys=" + value );
        }

        //  Velocity must follow source system.
        if ( isValid( velocity ) ) {
            buffer.append( ",SourceVel=" + velocity );
        }

        value = dateObs.getText();
        if ( isValid( value ) ) {
            buffer.append( ",Epoch=" + value );
        }
        return buffer.toString();
    }

    /** Test if result String is set and valid */
    private boolean isValid( String value )
    {
        return ( value != null &&
                 ! "".equals( value ) &&
                 ! "Unknown".equals( value ) );
    }

    /**
     * Convert a spectrum to a different coordinate system. The new system is
     * specified as a set of AST attributes.
     *
     * @param spec the spectrum to convert
     * @param attributes the AST attributes describing the new system
     *                   (for example: "system=WAVE,unit(1)=Angstrom").
     * @param iaxis the axis that of spectral FrameSet that is the SpecFrame.
     * @param clearOrigin iff true clear the SpecOrigin attribute first.
     *
     * @throws SplatException when an error occurs
     */
    public static void convertToAttributes( SpecData spec, String attributes,
                                            int iaxis, boolean clearOrigin )
        throws SplatException
    {
        //  This modifies the underlying FrameSet. Not the plot version.
        //  The makes sure the changes are propagated to any new plot
        //  FrameSets.
        FrameSet frameSet = spec.getFrameSet();

        //  Pick out the significant axis from FrameSet, this should be a
        //  SpecFrame or subclass.
        int iaxes[] = new int[1];
        iaxes[0] = iaxis;
        Frame picked = frameSet.pickAxes( 1, iaxes, null );

        // Now set the values. Note we write to FrameSet not Frame as this
        // makes the system remap. Try values out on a copy first as this
        // protects against making the FrameSet bad by establishing a partial
        // set of attributes.
        if ( picked instanceof SpecFrame ) {
            try {
                FrameSet localCopy = (FrameSet) frameSet.copy();
                if ( clearOrigin ) {
                    localCopy.clear( "SpecOrigin" );
                }
                localCopy.set( attributes );

                if ( clearOrigin ) {
                    frameSet.clear( "SpecOrigin" );
                }
                frameSet.set( attributes );

                // Do a full update to get the changes propagated throughout.
                spec.initialiseAst();
                globalList.notifySpecListenersModified( spec );
            }
            catch (AstException e) {
                throw new SplatException( "Failed to convert to new " +
                                          "coordinate system", e );
            }
        }
        else {
            throw new SplatException( "Cannot convert coordinate type " +
                                      "as the spectrum '" +
                                      spec.getShortName() +
                                      "' does not have an existing " +
                                      "spectral coordinate system" );
        }
    }

    /**
     * Set the coordinate system attributes of a spectrum. The system is
     * specified as a set of AST attributes.
     *
     * @param spec the spectrum to set
     * @param attributes the AST attributes describing the system
     *                   (for example: "system=WAVE,unit=Angstrom").
     * @param iaxis the axis that of spectral FrameSet that is the SpecFrame.
     * @param clearOrigin iff true clear the SpecOrigin attribute first.
     * @throws SplatException when an error occurs.
     */
    public static void setToAttributes( SpecData spec, String attributes, 
                                        int iaxis, boolean clearOrigin )
        throws SplatException
    {
        //  This sets the underlying FrameSet. Not the plot version.  The
        //  makes sure the changes are propagated to any new plot FrameSets,
        //  and saved with the spectrum.
        FrameSet frameSet = spec.getFrameSet();

        // If current frame doesn't contain a SpecFrame then we need to
        // generate one.
        Frame current = frameSet.getFrame( FrameSet.AST__CURRENT );
        int iaxes[] = new int[1];
        iaxes[0] = iaxis;
        Frame picked = frameSet.pickAxes( 1, iaxes, null );
        if ( ! ( picked instanceof SpecFrame ) ) {

            // Simplest way is to get SpecData to do this work. What we use
            // here doesn't matter as it will be overwritten by later calls.
            frameSet.set( "Label(" + iaxis + ")=Wavelength," + 
                          "Unit(" + iaxis + ")=Angstrom" );
            spec.initialiseAst();
        }

        // Now set the values. Note we write to Frame not FrameSet as this
        // just sets the attributes, rather than have them remapped. Use a
        // copy of the FrameSet so we can back out when errors happen (these
        // can leave the FrameSet quite mixed up).

        FrameSet localCopy = (FrameSet) frameSet.copy();
        Frame currentCopy = localCopy.getFrame( FrameSet.AST__CURRENT );
        try {
            if ( clearOrigin ) {
                currentCopy.clear( "SpecOrigin" );
            }
            currentCopy.set( attributes );

            current = frameSet.getFrame( FrameSet.AST__CURRENT );
            if ( clearOrigin ) {
                current.clear( "SpecOrigin" );
            }
            current.set( attributes );

            // Do a full update to get the changes propagated throughout.
            spec.initialiseAst();
            globalList.notifySpecListenersModified( spec );
        }
        catch (AstException e) {
            throw new SplatException("Failed to set new coordinate system", e);
        }
    }

    /**
     *  Update the value of all components to reflect the values of
     *  the selected spectra.
     */
    protected void update( ListSelectionEvent e )
    {
        if ( ! e.getValueIsAdjusting() ) {
            update();
        }
    }

    /**
     * Update all values to reflect those of the selected
     * spectra.
     */
    public void update()
    {
        int[] indices = specList.getSelectedIndices();
        if ( indices.length != 0 ) {
            SpecData spec = globalList.getSpectrum( indices[0] );
            if ( spec == null ) return;

            //  Use the Plot FrameSet. SpecFrame on axis 1.
            FrameSet frameSet = spec.getAst().getRef();

            String system = frameSet.getC( "System" );
            String unit = frameSet.getC( "Unit(1)" );
            String stdofrest = "";
            String obslon = "";
            String obslat = "";
            String refra = "";
            String refdec = "";
            String restfreq = "";
            String specorigin = "0";
            String sourcevel = "";
            String sourcevrf = "";
            String sourcesys = "VELO";
            String epoch = "";

            //  As using Plot FrameSet we need to look at the first axis.
            int iaxes[] = { 1 };
            Frame picked = frameSet.pickAxes( 1, iaxes, null );
            if ( picked instanceof SpecFrame ) {
                system = picked.getC( "System" );
                unit = picked.getC( "Unit" );
                stdofrest = picked.getC( "StdOfRest" );
                obslon = picked.getC( "ObsLon" );
                obslat = picked.getC( "ObsLat" );
                refra = picked.getC( "RefRA" );
                refdec = picked.getC( "RefDec" );
                restfreq = picked.getC( "RestFreq" );
                specorigin = picked.getC( "SpecOrigin" );
                sourcevel = picked.getC( "SourceVel" );
                sourcesys = picked.getC( "SourceSys" );
                sourcevrf = picked.getC( "SourceVRF" );
                epoch = picked.getC( "Epoch" );

                for ( int i = 1; i < indices.length; i++ ) {
                    spec = globalList.getSpectrum( indices[i] );
                    if ( spec == null ) continue;
                    frameSet = spec.getAst().getRef();
                    picked = frameSet.pickAxes( 1, iaxes, null );
                    if ( picked instanceof SpecFrame ) {
                        system = picked.getC( "System" );
                        unit = picked.getC( "Unit" );
                        stdofrest = checkAttr( picked, stdofrest,
                                               "StdOfRest" );
                        obslon = checkAttr( picked, obslon, "ObsLon" );
                        obslat = checkAttr( picked, obslat, "ObsLat" );
                        refra = checkAttr( picked, refra, "RefRA" );
                        refdec = checkAttr( picked, refdec, "RefDec" );
                        restfreq = checkAttr( picked, restfreq, "RestFreq" );
                        specorigin = checkAttr( picked, specorigin, 
                                                "SpecOrigin" );
                        sourcevel = checkAttr( picked, sourcevel,
                                               "SourceVel" );
                        sourcesys = checkAttr( picked, sourcesys,
                                               "SourceSys" );
                        sourcevrf = checkAttr( picked, sourcevrf,
                                               "SourceVRF" );
                        epoch = checkAttr( picked, epoch, "Epoch" );
                    }
                    else {
                        // break as nothing can be set correctly.
                        system = "Unknown";
                        unit = "";
                        stdofrest = "";
                        obslon = "";
                        obslat = "";
                        refra = "";
                        refdec = "";
                        restfreq = "";
                        specorigin = "0";
                        sourcevel = "";
                        sourcesys = "VELO";
                        sourcevrf = "";
                        epoch = "";
                    }
                }
            }
            else {
                system = "Unknown";
                unit = "";
            }

            // Set the values.
            setInterface( system, unit, stdofrest, obslon, obslat, refra,
                          refdec, restfreq, specorigin, sourcevrf, sourcevel,
                          sourcesys, epoch );
        }
    }

    /**
     * Set the interface to show a full set of values. The Strings are
     * the AST attributes values (not the full expanded names shown in
     * the interface, but these do have to match the ones in the
     * various Maps defined in this class). The rest frequency must be
     * in GHz. If any values are to be left upset they should be set
     * to null. To reset values set them to "", except for system
     * which should be set to "Unknown".
     */
    public void setInterface( String system, String unit,
                              String stdofrest, String obslon,
                              String obslat, String refra, String refdec,
                              String restfreq, String specorigin,
                              String sourcevrf, String sourcevel, 
                              String sourcesys, String epoch )
    {
        String value = null;

        if ( system != null ) {
            value = (String) astSystemsInverse.get( system );
            systemBox.setSelectedItem( value );
        }
        if ( unit != null ) {
            systemUnitsBox.setSelectedItem( unit );
        }

        if ( stdofrest != null ) {
            value = (String) stdOfRestInverse.get( stdofrest );
            stdOfRestBox.setSelectedItem( value );
        }

        if ( obslon != null ) {
            obsLong.setText( obslon );
        }
        if ( obslat != null ) {
            obsLat.setText( obslat );
        }

        if ( refra != null ) {
            sourceRA.setText( refra );
        }

        if ( refdec != null ) {
            sourceDec.setText( refdec );
        }

        if ( restfreq != null ) {
            restFrequency.setText( restfreq );
            restFrequencyUnits.setSelectedItem( "GHz" );
        }

        if ( specorigin != null ) {
            specOrigin.setText( specorigin );
            specOriginDefault = specorigin;
        }

        if ( sourcevel != null ) {
            sourceVel.setText( sourcevel );
        }

        if ( sourcevrf != null ) {
            value = (String) stdOfRestInverse.get( sourcevrf );
            sourceStdOfRestBox.setSelectedItem( value );
        }

        if ( sourcesys != null ) {
            value = (String) sourceSystemsInverse.get( sourcesys );
            sourceSystemBox.setSelectedItem( value );
        }

        if ( epoch != null ) {
            dateObs.setText( epoch );
        }
    }

    private String checkAttr( Frame specFrame, String value, String attr )
    {
        if ( value != null && ! value.equals( specFrame.getC( attr ) ) ) {
            return null;
        }
        return value;
    }

    /**
     * Close the window.
     */
    protected void closeWindowEvent()
    {
        Utilities.saveFrameLocation( this, prefs, "SpecCoordinatesFrame" );
        dispose();
    }

    private final static ImageIcon closeImage =
        new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction extends AbstractAction
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

    private final static ImageIcon convertImage =
        new ImageIcon( ImageHolder.class.getResource( "modify.gif" ) );

    /**
     * Inner class defining Action for converting the AST framesets to
     * the current set of attributes.
     */
    protected class ConvertAction extends AbstractAction
    {
        public ConvertAction()
        {
            super( "Convert", convertImage );
            putValue( SHORT_DESCRIPTION, "Convert spectral coordinates" +
                      "from old to new type" );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control N" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            matchCoordType( false );
        }
    }

    private final static ImageIcon setImage =
        new ImageIcon( ImageHolder.class.getResource( "set.gif" ) );

    /**
     * Inner class defining Action for setting the AST framesets to
     * the current set of attributes.
     */
    protected class SetAction extends AbstractAction
    {
        public SetAction()
        {
            super( "Set", setImage );
            putValue( SHORT_DESCRIPTION, "Set spectral coordinate type" );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control S" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            matchCoordType( true );
        }
    }

    //
    // Implement ActionListener interface.
    //
    public void actionPerformed( ActionEvent e )
    {
        Object source = e.getSource();
        if ( source instanceof JComboBox ) {
            JComboBox jb = (JComboBox) source;
            String name = (String) jb.getSelectedItem();
            if ( jb.equals( systemBox ) ) {
                // Set related default units.
                String units = (String) systemUnits.get( name );
                if ( units != null ) {
                    systemUnitsBox.setSelectedItem( units );
                }
            }
            else if ( jb.equals( systemUnitsBox ) ) {
                // Convert to real string from symbolic.
                String units = (String) unitsMap.get( name );
                if ( units != null ) {
                    systemUnitsBox.setSelectedItem( units );
                }
            }
            else if ( jb.equals( stdOfRestBox ) ) {
                String restFrame = (String) stdOfRest.get( name );
                //  TODO: toggle according to stuff...
            }
            else if ( jb.equals( observatoryBox ) ) {
                setObservatory();
            }
            else if ( jb.equals( restFrequencyUnits ) ) {
                String units = (String) unitsMap.get( name );
                if ( units != null ) {
                    restFrequencyUnits.setSelectedItem( units );
                }
            }
        }
        else if ( source instanceof JMenuItem ) {
            String cmd = e.getActionCommand();
            if ( "set1".equals( cmd ) ) {
                setInterface( "WAVE", "Angstrom", "Source", "", "", "", "",
                              "", "", "Topocentric", "0.0", "VELO", "" );
            }
        }
    }
}
