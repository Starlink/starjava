/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     18-APR-2005 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.ExceptionDialog;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
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
    implements ItemListener
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
     * Unknown units, could also be unrecognised.
     */
    private static final String UNKNOWN = "Unknown";

    /**
     * List of the possible data units.
     */
    private static Map dataUnitsMap = null;
    static {
        dataUnitsMap = new LinkedHashMap(); 
        dataUnitsMap.put( UNKNOWN, UNKNOWN);
        dataUnitsMap.put( "Jansky", "Jy");
        dataUnitsMap.put( "W/m^2/Hz", "W/m^2/Hz" );
        dataUnitsMap.put( "W/m^2/Angstrom", "W/m^2/Angstrom" );
        dataUnitsMap.put( "W/cm^2/um", "W/cm^2/um" );
        dataUnitsMap.put( "erg/cm^2/s/Hz", "erg/cm^2/s/Hz" );
        dataUnitsMap.put( "erg/cm^2/s/Angstrom", "erg/cm^2/s/Angstrom" ); 
    };

    /**
     * List of the possible spectral coordinates. These are presented to the
     * user as units only. The system is assumed.
     */
    private static Vector coordinateSystems = null;
    static {
        coordinateSystems = new Vector(); 
        coordinateSystems.add( new Cdus( UNKNOWN, UNKNOWN, UNKNOWN ) );
        coordinateSystems.add( new Cdus( "Angstroms", "Angstrom", "WAVE" ) );
        coordinateSystems.add( new Cdus( "Nanometres", "nm", "WAVE" ) );
        coordinateSystems.add( new Cdus( "Millimetres", "mm", "WAVE" ) );
        coordinateSystems.add( new Cdus( "Micrometres", "um", "WAVE" ) );
        coordinateSystems.add( new Cdus( "Gigahertz", "GHz", "FREQ" ) );
        coordinateSystems.add( new Cdus( "Megahertz", "MHz", "FREQ" ) );
        coordinateSystems.add( new Cdus( "Terahertz", "THz", "FREQ" ) );
        coordinateSystems.add( new Cdus( "Kilohertz", "kHz", "FREQ" ) );
        coordinateSystems.add( new Cdus( "Joules", "J", "ENER" ) );
        coordinateSystems.add( new Cdus( "Ergs", "erg", "ENER" ) );
        coordinateSystems.add( new Cdus( "Electron-volts", "eV", "ENER" ) );
        coordinateSystems.add( new Cdus( "Kilo-electron-volts", "keV", 
                                         "ENER" ) );
        coordinateSystems.add( new Cdus( "Metres-per-sec (radio)", "m/s",
                                         "VRAD" ) );
        coordinateSystems.add( new Cdus( "Kilometres-per-sec (radio)", "km/s",
                                         "VRAD" ) );
        coordinateSystems.add( new Cdus( "Per-metre", "1/m", "WAVN" ) );
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
        fileMenu.add( applyAction );
        JButton applyButton = new JButton( applyAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( applyButton );
        actionBar.add( Box.createGlue() );

        // Add an action to close the window (appears in File menu and action
        // bar).
        CloseAction closeAction = new CloseAction();
        fileMenu.add( closeAction );
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
        setTitle( Utilities.getTitle( "Plot Units" ) );
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
                                                   GridBagLayouter.SCHEME3 );

        coordinatesBox = new JComboBox( coordinateSystems );
        JLabel label = new JLabel( "Coordinates: " );
        gbl.add( label, false );
        gbl.add( coordinatesBox, true );
        coordinatesBox.setToolTipText( "Units of the spectral coordinates" );

        dataUnitsBox = new JComboBox( dataUnitsMap.keySet().toArray() );
        label = new JLabel( "Data units: " );
        gbl.add( label, false );
        gbl.add( dataUnitsBox, true );
        dataUnitsBox.setToolTipText( "Units of the data values" );

        gbl.eatSpare();

        return panel;
    }

    /**
     * Match the UI to the properties of the current spectrum. If the
     * units cannot be matched against the list of possibles then 
     * "Unknown" is shown.
     */
    protected void matchCurrentSpectrum()
    {
        SpecData spectrum = control.getCurrentSpectrum();
        FrameSet frameSet = spectrum.getAst().getRef();
        String coordUnits = frameSet.getC( "unit(1)" );
        String dataUnits = frameSet.getC( "unit(2)" );

        //  Need to transform these into local strings.
        Set entrySet = dataUnitsMap.entrySet();
        Iterator i = entrySet.iterator();
        Map.Entry entry;
        String value;
        dataUnitsBox.setSelectedIndex( 0 ); //  Unknown
        while ( i.hasNext() ) {
            entry = (Map.Entry) i.next();
            if ( entry.getValue().equals( dataUnits ) ) {
                dataUnitsBox.setSelectedItem( (String) entry.getKey() );
                break;
            }
        }

        i = coordinateSystems.iterator();
        coordinatesBox.setSelectedIndex( 0 ); // Unknown
        Cdus cdus = null;
        while ( i.hasNext() ) {
            cdus = (Cdus) i.next();
            if ( cdus.getUnits().equals( coordUnits ) ) {
                coordinatesBox.setSelectedItem( cdus );
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
        String dataUnits = (String) dataUnitsBox.getSelectedItem();
        dataUnits = (String) dataUnitsMap.get( dataUnits );

        Cdus cdus = (Cdus) coordinatesBox.getSelectedItem();
        String coordUnits = cdus.getUnits();
        String coordSystem = cdus.getSystem();

        if ( dataUnits.equals( UNKNOWN ) && 
             coordUnits.equals( UNKNOWN ) ) {
            new SplatException
                ( this, "Cannot convert when current units are unknown",
                  "Unknown data units", JOptionPane.WARNING_MESSAGE );
            return;
        }

        //  And apply to current spectrum,
        SpecData spec = control.getCurrentSpectrum();
        convertToUnits( spec, dataUnits, coordUnits, coordSystem );
    }

    /**
     * Convert current units of a given spectrum to some new values.
     * Units that are "Unknown" are skipped and remain at there current
     * values.
     */
    protected void convertToUnits( SpecData spec, String dataUnits,
                                   String coordUnits, String coordSystem )
    {
        if ( ! dataUnits.equals( UNKNOWN ) ) {
            try {
                SpecDataUnitsFrame.convertToUnits( spec, dataUnits );
            }
            catch (SplatException e) {
                new ExceptionDialog( this, e );
            }
        }
        
        if ( ! coordUnits.equals( UNKNOWN ) ) {
            try {
                int iaxis = spec.getMostSignificantAxis();
                String attributes = 
                    "System=" + coordSystem + ",Unit("+iaxis+")=" + coordUnits;
                SpecCoordinatesFrame.convertToAttributes( spec, attributes, 
                                                          iaxis );
            }
            catch (SplatException e) {
                new ExceptionDialog( this, e );
            }
        }
    }

    /**
     * Close the window.
     */
    protected void closeWindowEvent()
    {
        Utilities.saveFrameLocation( this, prefs, "PlotUnitsFrame" );
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
        }
        public void actionPerformed( ActionEvent ae )
        {
            matchUIUnits();
        }
    }

    /**
     * Inner class for containing the description of a known coordinate
     * system.
     */
    protected static class Cdus
    {
        private String description;
        private String units;
        private String system;

        public Cdus( String description, String units, String system )
        {
            this.description = description;
            this.units = units;
            this.system = system;
        }
        public String toString()
        {
            return description;
        }
        public String getDescription()
        {
            return description;
        }
        public String getUnits()
        {
            return units;
        }
        public String getSystem()
        {
            return system;
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
