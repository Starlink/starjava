/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
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
import uk.ac.starlink.splat.util.ExceptionDialog;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
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
public class SpecXCoordTypeFrame
    extends JFrame
    implements ActionListener
{
    /**
     * Reference to global list of spectra and plots.
     */
    private GlobalSpecPlotList
        globalList = GlobalSpecPlotList.getReference();

    /**
     * UI preferences.
     */
    private static Preferences prefs =
        Preferences.userNodeForPackage( SpecXCoordTypeFrame.class );

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
     * Control for source velocity.
     */
    private JTextField sourceVel = null;

    /**
     * Control for selecting the source standard of rest;
     */
    private JComboBox sourceStdOfRestBox = null;

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

    /** Switch for inhibiting the propagation of changes */
    protected boolean inhibitChanges = false;

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
        unitsMap.put( "Gigahertz", "GHz" );
        unitsMap.put( "Megahertz", "MHz" );
        unitsMap.put( "Terahertz", "THz" );
        unitsMap.put( "Kilohertz", "kHz" );
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

        //stdDependOn = new LinkedHashMap( 8 );
        //stdDependOn.put( "Topocentric", "Epoch GeoLat GeoLon RefRA RefDec" );
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
    public SpecXCoordTypeFrame( JList selectionSource )
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
        menuBar.add( fileMenu );

        // Create the options menu.
        optionsMenu.setText( "Options" );
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
        fileMenu.add( convertAction );
        JButton convertButton = new JButton( convertAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( convertButton );
        actionBar.add( Box.createGlue() );

        // Add an action to set the AST framesets of the current
        // spectra.
        SetAction setAction = new SetAction();
        fileMenu.add( setAction );
        JButton setButton = new JButton( setAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( setButton );
        actionBar.add( Box.createGlue() );

        // Add an action to close the window (appears in File menu
        // and action bar).
        CloseAction closeAction = new CloseAction();
        fileMenu.add( closeAction );
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
        Utilities.setFrameLocation( this, null, prefs, "SpecXCoordTypeFrame" );
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
        systemUnitsBox.setToolTipText( "Coordinate system for the " +
                                       "spectrum coordinates" );

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

        label = new JLabel( "Source rest frame:" );
        sourceStdOfRestBox =
            new JComboBox( sourceStdOfRest.keySet().toArray() );
        gbl.add( label, false );
        gbl.add( sourceStdOfRestBox, true );
        sourceStdOfRestBox.setToolTipText( "Standard of rest for the" +
                                           " source velocity" );

        label = new JLabel( "Source velocity:" );
        sourceVel = new JTextField();
        gbl.add( label, false );
        gbl.add( sourceVel, true );
        sourceVel.setToolTipText( "The velocity of source in the" +
                                  " source rest frame (km/s)" );

        gbl.eatSpare();

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
                aSpecFrame.setD( "GeoLon", -1.0 * obs.getLong() * Pal.R2D );
                aSpecFrame.setD( "GeoLat", obs.getLat() * Pal.R2D );
                obsLong.setText( aSpecFrame.getC( "GeoLon") );
                obsLat.setText( aSpecFrame.getC( "GeoLat") );
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

            //  Gather the current state of the interface.
            String attributes = gatherAttributes();

            //  And apply to all selected spectra.
            for ( int i = 0; i < indices.length; i++ ) {
                SpecData spec = globalList.getSpectrum( indices[i] );
                FrameSet frameSet = spec.getFrameSet();
                if ( set ) {
                    setToAttributes( spec, frameSet, attributes );
                }
                else {
                    convertToAttributes( spec, frameSet, attributes );
                }
            }
        }
    }

    /**
     * Gather information from interface and create a single String
     * suitable for applying to a FrameSet.
     */
    protected String gatherAttributes()
    {
        StringBuffer buffer = new StringBuffer();

        String selected = (String) systemBox.getSelectedItem();
        String value = (String) astSystems.get( selected );
        if ( isValid( value ) ) {
            buffer.append( "System=" + value );
        }

        value = (String) systemUnitsBox.getSelectedItem();
        if ( isValid( value ) ) {
            buffer.append( ",Unit(1)=" + value );
        }

        selected = (String) stdOfRestBox.getSelectedItem();
        value = (String) stdOfRest.get( selected );
        if ( isValid( value ) ) {
            buffer.append( ",StdOfRest=" + value );
        }

        value = obsLong.getText();
        if ( isValid( value ) ) {
            buffer.append( ",GeoLon=" + value );
        }

        value = obsLat.getText();
        if ( isValid( value ) ) {
            buffer.append( ",GeoLat=" + value );
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
            String units = (String) restFrequencyUnits.getSelectedItem();
            buffer.append( ",RestFreq=" + value + units );
        }

        selected = (String) sourceStdOfRestBox.getSelectedItem();
        value = (String) stdOfRest.get( selected );
        if ( isValid( value ) ) {
            buffer.append( ",SourceVRF=" + value );
        }

        value = sourceVel.getText();
        if ( isValid( value ) ) {
            buffer.append( ",SourceVel=" + value );
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
     *
     */
    protected void convertToAttributes( SpecData spec, FrameSet frameSet,
                                        String attributes )
    {
        //  Pick out first axis from FrameSet, this should be a
        //  SpecFrame.
        int iaxes[] = { 1 };
        Frame picked = frameSet.pickAxes( 1, iaxes, null );

        // Now set the values. Note we write to FrameSet not Frame as
        // this makes the system remap.
        if ( picked instanceof SpecFrame ) {
            try {
                frameSet.set( attributes );

                // Do a full update to get the changes
                // propagated throughout.
                try {
                    spec.initialiseAst();
                    globalList.notifySpecListeners( spec );
                }
                catch (SplatException e) {
                    new ExceptionDialog( this, e );
                }
            }
            catch (AstException e) {
                new ExceptionDialog( this,
                                     "Failed to convert to new " +
                                     "coordinate system", e );
            }
        }
        else {
            SplatException e =
                new SplatException( "Cannot convert coordinate type " +
                                    "as the spectrum '" +
                                    spec.getShortName() +
                                    "' does not already have a " +
                                    "spectral coordinate system" );
            new ExceptionDialog( this, e );
        }
    }

    /**
     *
     */
    protected void setToAttributes( SpecData spec, FrameSet frameSet,
                                    String attributes )
    {
        // If current frame doesn't contain a SpecFrame then we need
        // to generate one.
        Frame current = frameSet.getFrame( FrameSet.AST__CURRENT );
        int iaxes[] = { 1 };
        Frame picked = current.pickAxes( 1, iaxes, null );
        if ( ! ( picked instanceof SpecFrame ) ) {
            // Simplest way is to get SpecData to do this work. What
            // we use here doesn't matter as it will be overwritten by
            // later calls.
            frameSet.set( "Label(1)=Wavelength, Unit(1)=Angstrom" );
            try {
                spec.initialiseAst();
            }
            catch (SplatException e) {
                new ExceptionDialog( this, e );
                return;
            }
        }

        // Now set the values. Note we write to Frame not FrameSet as
        // this just reset attributes, rather than have them remapped.
        current = frameSet.getFrame( FrameSet.AST__CURRENT );
        try {
            current.set( attributes );

            // Do a full update to get the changes propagated throughout.
            try {
                spec.initialiseAst();
                globalList.notifySpecListeners( spec );
            }
            catch (SplatException e) {
                new ExceptionDialog( this, e );
            }
        }
        catch (AstException e) {
            new ExceptionDialog( this,
                                 "Failed to set new coordinate system", e );
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
            FrameSet frameSet = spec.getFrameSet();

            String system = frameSet.getC( "System" );
            String unit = frameSet.getC( "Unit(1)" );
            String stdofrest = "";
            String geolon = "";
            String geolat = "";
            String refra = "";
            String refdec = "";
            String restfreq = "";
            String sourcevel = "";
            String sourcevrf = "";
            String epoch = "";

            int iaxes[] = { 1 };
            Frame picked = frameSet.pickAxes( 1, iaxes, null );
            if ( picked instanceof SpecFrame ) {
                stdofrest = frameSet.getC( "StdOfRest" );
                geolon = frameSet.getC( "GeoLon" );
                geolat = frameSet.getC( "GeoLat" );
                refra = frameSet.getC( "RefRA" );
                refdec = frameSet.getC( "RefDec" );
                restfreq = frameSet.getC( "RestFreq" );
                sourcevel = frameSet.getC( "SourceVel" );
                sourcevrf = frameSet.getC( "SourceVRF" );
                epoch = frameSet.getC( "Epoch" );

                for ( int i = 1; i < indices.length; i++ ) {
                    spec = globalList.getSpectrum( indices[i] );
                    frameSet = spec.getFrameSet();
                    picked = frameSet.pickAxes( 1, iaxes, null );
                    if ( picked instanceof SpecFrame ) {
                        system = frameSet.getC( "System" );
                        unit = frameSet.getC( "Unit(1)" );
                        stdofrest = checkAttr( frameSet, stdofrest,
                                               "StdOfRest" );
                        geolon = checkAttr( frameSet, geolon, "GeoLon" );
                        geolat = checkAttr( frameSet, geolat, "GeoLat" );
                        refra = checkAttr( frameSet, refra, "RefRA" );
                        refdec = checkAttr( frameSet, refdec, "RefDec" );
                        restfreq = checkAttr( frameSet, restfreq, "RestFreq" );
                        sourcevel = checkAttr( frameSet, sourcevel,
                                               "SourceVel" );
                        sourcevrf = checkAttr( frameSet, sourcevrf,
                                               "SourceVRF" );
                        epoch = checkAttr( frameSet, epoch, "Epoch" );
                    }
                    else {
                        // break as nothing can be set correctly.
                        system = "Unknown";
                        unit = "";
                        stdofrest = "";
                        geolon = "";
                        geolat = "";
                        refra = "";
                        refdec = "";
                        restfreq = "";
                        sourcevel = "";
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
            setInterface( system, unit, stdofrest, geolon, geolat, refra,
                          refdec, restfreq, sourcevrf, sourcevel,
                          epoch );
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
                              String stdofrest, String geolon, 
                              String geolat, String refra, String refdec,
                              String restfreq, String sourcevrf,
                              String sourcevel, String epoch )
    {
        inhibitChanges = true;
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

        if ( geolon != null ) {
            obsLong.setText( geolon );
        }
        if ( geolat != null ) {
            obsLat.setText( geolat );
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

        if ( sourcevel != null ) {
            sourceVel.setText( sourcevel );
        }

        if ( sourcevrf != null ) {
            value = (String) stdOfRestInverse.get( sourcevrf );
            sourceStdOfRestBox.setSelectedItem( value );
        }

        if ( epoch != null ) {
            dateObs.setText( epoch );
        }
        inhibitChanges = false;
    }

    private String checkAttr( FrameSet frameSet, String value, String attr )
    {
        if ( value != null && ! value.equals( frameSet.getC( attr ) ) ) {
            return null;
        }
        return value;
    }

    /**
     * Close the window.
     */
    protected void closeWindowEvent()
    {
        Utilities.saveFrameLocation( this, prefs, "SpecXCoordTypeFrame" );
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
            if ( jb == systemBox ) {
                // Set related default units.
                String units = (String) systemUnits.get( name );
                if ( units != null ) {
                    systemUnitsBox.setSelectedItem( units );
                }
            }
            else if ( jb == systemUnitsBox ) {
                // Convert to real string from symbolic.
                String units = (String) unitsMap.get( name );
                if ( units != null ) {
                    systemUnitsBox.setSelectedItem( units );
                }
            }
            else if ( jb == stdOfRestBox ) {
                String restFrame = (String) stdOfRest.get( name );
                //  TODO: toggle according to stuff...
            }
            else if ( jb == observatoryBox ) {
                setObservatory();
            }
            else if ( jb == restFrequencyUnits ) {
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
                              "", "Topocentric", "0.0", "" );
            }
        }
    }
}
