/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 * Copyright (C) 2007-2009 Science and Technology Facilities Council
 *
 *  History:
 *     10-JAN-2005 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Iterator;
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
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Triple;
import uk.ac.starlink.splat.util.UnitUtilities;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * Window for choosing to set or modify the spectral data units. The data
 * units will be used for alignment, either dimensionally or by flux.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SpecDataUnitsFrame
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
        Preferences.userNodeForPackage( SpecDataUnitsFrame.class );

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

    /**
     * Control for displaying/changing the units.
     */
    private JComboBox unitsBox = null;

    /**
     * Initialization of useful AST units information.
     */
    private static Vector unitsVector = null;
    static {
        unitsVector = new Vector();
        Frame testFrame = new Frame( 1 );
        unitsVector.add( new Triple( "Jansky", "Jy", "Jy" ) );
        unitsVector.add( new Triple( "milliJansky", "mJy", "mJy" ) );
        String[] canonicalUnits = { "W/m^2/Hz",
                                    "W/m^2/Angstrom",
                                    "W/cm^2/um",
                                    "erg/cm^2/s/Hz",
                                    "erg/cm^2/s/Angstrom"
        };

        for ( int i = 0; i < canonicalUnits.length; i++ ) {
            testFrame.setUnit( 1, canonicalUnits[i] );
            unitsVector.add( new Triple( canonicalUnits[i],
                                         canonicalUnits[i],
                                         testFrame.getC( "normunit" ) ) );
        }
    };

    /**
     * Create an instance.
     *
     * @param selectionSource if not null then this defines another
     *                        JList those selection is to be copied to
     *                        our JList.
     */
    public SpecDataUnitsFrame( JList selectionSource )
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

        //  Create the Help menu.
        HelpFrame.createHelpMenu( "dataunits-window", "Help on window",
                                  menuBar, null );
        initSpecListArea();
        initControlArea();
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
     * Create the main controls for displaying and changing the data units.
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

        // Add an action to convert from the existing data units to some new
        // data units. Tricky as involves modification of underlying data
        // values.
        ConvertAction convertAction = new ConvertAction();
        fileMenu.add( convertAction ).setMnemonic( KeyEvent.VK_N );

        JButton convertButton = new JButton( convertAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( convertButton );
        actionBar.add( Box.createGlue() );

        // Add an action to set the data units.
        SetAction setAction = new SetAction();
        fileMenu.add( setAction ).setMnemonic( KeyEvent.VK_S );

        JButton setButton = new JButton( setAction );
        actionBar.add( Box.createGlue() );
        actionBar.add( setButton );
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
        // the data units of the selected spectra.
        JPanel specFramePanel = initControls();
        specFramePanel.setBorder
            ( BorderFactory.createTitledBorder( "Data units" ) );
        panel.add( specFramePanel, BorderLayout.CENTER );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    protected void initFrame()
    {
        setTitle( Utilities.getTitle( "Data units" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        Utilities.setFrameLocation( this, null, prefs, "SpecDataUnitsFrame" );
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

        unitsBox = new JComboBox( unitsVector );
        unitsBox.setEditable( true );
        unitsBox.addActionListener( this );
        JLabel label = new JLabel( "Units: " );
        gbl.add( label, false );
        gbl.add( unitsBox, true );
        unitsBox.setToolTipText( "Units that describe the data values" );

        gbl.eatSpare();

        return panel;
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
     * Make the currently selected spectra have the current data units.
     * This can be a "set" or "convert" operation. If "convert" then the
     * spectra must already have FluxFrame and the new units should be
     * matchable.
     */
    public void matchUnitsType( boolean set )
    {
        //  Get a list of the selected spectra.
        int[] indices = specList.getSelectedIndices();
        if ( indices.length != 0 ) {

            //  Get the current units.
            String units = getUnits();

            //  And apply to all selected spectra.
            for ( int i = 0; i < indices.length; i++ ) {
                SpecData spec = globalList.getSpectrum( indices[i] );
                try {
                    if ( set ) {
                        setToUnits( spec, units);
                    }
                    else {
                        convertToUnits( spec, units );
                    }
                }
                catch (SplatException e ) {
                    ErrorDialog.showError( this, e );
                }
            }
        }
    }

    /**
     * Get units from the interface.
     */
    protected String getUnits()
    {
        Object value = unitsBox.getEditor().getItem();
        String units = null;
        if ( value instanceof String ) {
            units = (String) value;
        }
        else {
            units = ((Triple) value).gets2();
        }
        if ( isValid( units ) ) {
            return UnitUtilities.fixUpUnits( units );
        }
        return "";
    }

    /** Test if result String is set and valid */
    private boolean isValid( String value )
    {
        return ( value != null &&
                 ! "".equals( value ) &&
                 ! "Unknown".equals( value ) );
    }

    /**
     * Convert the data units of a spectrum. Changes the actual data values
     * and the units as presented by the SpecData instance, but the underlying
     * implementation is not modified.
     *
     * @param spec the SpecData instance.
     * @param units the new units.
     * @throws SplatException when an error occurs
     */
    public static void convertToUnits( SpecData spec, String units )
        throws SplatException
    {
        //  To convert the data units of a spectrum we need to set the
        //  apparent units of the underlying SpecData object. We need to do it
        //  this way, rather than say by just modifying the FluxFrame units,
        //  as the data values themselves may be immutable and without being
        //  able to modify the data values any changes we make will be lost
        //  when the plot FrameSet is re-generated.
        spec.setApparentDataUnits( units );

        // Do a full update to get the changes propagated throughout.
        try {
            spec.initialiseAst();

            //  Check if the change has succeeded.
            if ( spec.getApparentDataUnits() == null ) {
                throw new SplatException( "Cannot convert to new data units" );
            }
            globalList.notifySpecListenersModified( spec );
        }
        catch (SplatException e) {
            throw new SplatException("Failed to convert to new data units", e);
        }
    }

    /**
     * Set data units of a spectrum.
     *
     * @param spec the SpecData instance.
     * @param units the new units.
     */
    public static void setToUnits( SpecData spec, String units )
        throws SplatException
    {
        // Do a full update to get the changes propagated throughout. This
        // creates a FluxFrame, if it can.
        spec.setDataUnits( units );
        spec.initialiseAst();
        globalList.notifySpecListenersModified( spec );
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
     * Update all values to reflect those of the selected spectra.
     */
    public void update()
    {
        int[] indices = specList.getSelectedIndices();
        if ( indices.length != 0 ) {
            SpecData spec = globalList.getSpectrum( indices[0] );
            if ( spec == null ) return;
            FrameSet frameSet = spec.getAst().getRef();
            String units = frameSet.getC( "Unit(2)" );

            // Set the values.
            setInterface( units );
        }
    }

    /**
     * Set the interface to show a full set of values. The Strings are
     * the AST attributes values (not the full expanded names shown in
     * the interface, but these do have to match the ones in the
     * various Maps defined in this class).
     */
    public void setInterface( String units )
    {
        if ( units != null ) {
            unitsBox.setSelectedItem( units );
        }
    }

    /**
     * Close the window.
     */
    protected void closeWindowEvent()
    {
        Utilities.saveFrameLocation( this, prefs, "SpecDataUnitsFrame" );
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

    private final static ImageIcon convertImage =
        new ImageIcon( ImageHolder.class.getResource( "modify.gif" ) );

    /**
     * Inner class defining Action for converting the AST framesets to
     * the current set of attributes.
     */
    protected class ConvertAction
        extends AbstractAction
    {
        public ConvertAction()
        {
            super( "Convert", convertImage );
            putValue( SHORT_DESCRIPTION, "Convert data units from old to " +
                      "new type" );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control N" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            matchUnitsType( false );
        }
    }

    private final static ImageIcon setImage =
        new ImageIcon( ImageHolder.class.getResource( "set.gif" ) );

    /**
     * Inner class defining Action for setting the AST framesets to
     * the current set of attributes.
     */
    protected class SetAction
        extends AbstractAction
    {
        public SetAction()
        {
            super( "Set", setImage );
            putValue( SHORT_DESCRIPTION, "Set data units" );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( "control S" ) );
        }
        public void actionPerformed( ActionEvent ae )
        {
            matchUnitsType( true );
        }
    }

    //
    // Implement ActionListener interface.
    //
    public void actionPerformed( ActionEvent e )
    {
        //  Handle case when a String has been entered by the user.
        //  Want to see if the form matches the canonical ones and if
        //  so display that instead.
        Object source = e.getSource();
        if ( source instanceof JComboBox ) {
            JComboBox jb = (JComboBox) source;
            if ( jb.equals( unitsBox ) ) {
                Object selected = jb.getSelectedItem();

                //  If source is String assume user value.
                if ( selected instanceof String ) {

                    Frame f = new Frame( 1 );
                    f.setUnit( 1, (String) selected );
                    String normunit = f.getC( "normunit" );

                    Iterator i = unitsVector.iterator();
                    Triple t;
                    while ( i.hasNext() ) {
                        t = (Triple) i.next();
                        if ( t.gets3().equals( normunit ) ) {
                            unitsBox.setSelectedItem( t );
                            break;
                        }
                    }
                }
            }
        }
    }
}
