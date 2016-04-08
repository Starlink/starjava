/*
 * Copyright (C) 2000-2005 Central Laboratory of the Research Councils
 * Copyright (C) 2007 Particle Physics and Astronomy Research Council
 * Copyright (C) 2007-2009 Science and Technology Facilities Council
 *
 *  History:
 *     25-SEP-2000 (Peter W. Draper):
 *       Original version.
 *     2012 (Margarida Castro Neves)
 *      added getData support
 *     2013
 *      added DataLink support 
 *     JUL-2015
 *      removed getData support
 */

//  XXX Need to use SpectrumIO consistently for opening all spectra
//  from all sources and all the public methods that support this
//  need to be made package private so we can remove or change any
//  methods side-stepping SpectrumIO (in case it is forgotten NDFs must
//  all be opened in the same thread, this keeps the NDF library state
//  consistent, especially for any WCS components).

package uk.ac.starlink.splat.iface;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

//import org.astrogrid.acr.InvalidArgumentException;



import uk.ac.starlink.ast.gui.ScientificFormat;
import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.NameParser;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.data.SpecList;
import uk.ac.starlink.splat.iface.SpectrumIO.SourceType;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.RemoteServer;
import uk.ac.starlink.splat.util.SEDSplatException;
import uk.ac.starlink.splat.util.SampCommunicator;
import uk.ac.starlink.splat.util.SplatCommunicator;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.SplatSOAPServer;
import uk.ac.starlink.splat.util.MathUtils;
import uk.ac.starlink.splat.util.Transmitter;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.URLDataSource;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;
import uk.ac.starlink.util.gui.FileNameListCellRenderer;
import uk.ac.starlink.util.gui.GridBagLayouter;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.votable.VOTableBuilder;

import uk.ac.starlink.splat.vo.DataLinkParams;
import uk.ac.starlink.splat.vo.SSAQueryBrowser;
import uk.ac.starlink.splat.vo.SSAServerList;
import uk.ac.starlink.splat.vo.SSAPAuthenticator;
import uk.ac.starlink.splat.vo.ObsCorePanel;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableFormatException;

/**
 * This is the main class for the SPLAT program. It creates the
 * browser interface that displays and controls the global lists of
 * the currently available spectra and plots.
 * <p>
 * Using the menus and controls of this interface, spectra can be
 * opened, removed and copied into the global list. Plots can created,
 * have spectra added and be closed.
 * <p>
 * Groups of selected spectra can have their display inspected and
 * changed using the controls of the related #SplatSelectedProperties
 * object.
 * <p>
 * There are also a series of global, rather than spectra, specific
 * tools that are made available via a toolbar. These include an
 * animator tool and tools for performing simple spectral arithmetic,
 * plus more trivial options, like choosing the look and feel.
 * <p>
 * The actual display and interactive analysis of spectra takes place
 * in the plots (see {@link PlotControlFrame}, {@link PlotControl}
 * and {@link DivaPlot}).
 *
 * @author Peter W. Draper
 * @author Mark Taylor
 * @version $Id$
 *
 * @see GlobalSpecPlotList
 * @see SplatSelectedProperties
 * @see SplatPlotTable
 * @see PlotControlFrame
 * @see PlotControl
 * @see DivaPlot
 */
public class SplatBrowser
    extends JFrame
    implements ItemListener, ActionListener
{
    //  Logger.
    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.splat.iface.SplatBrowser" );

    /**
     *  The global list of spectra and plots.
     */
    protected GlobalSpecPlotList globalList = GlobalSpecPlotList.getInstance();

    /**
     *  Factory methods for creating SpecData instances.
     */
    protected SpecDataFactory specDataFactory = SpecDataFactory.getInstance();

    /**
     * UI preferences.
     */
    protected static Preferences prefs =
        Preferences.userNodeForPackage( SplatBrowser.class );

    /**
     * Default location of window.
     */
    private static final Rectangle defaultWindowLocation =
        new Rectangle( 10, 10, 700, 550 );

    /**
     *  Content pane of JFrame.
     */
    protected JPanel contentPane;

    /**
     *  Main menubar and various menus.
     */
    protected JMenuBar menuBar = new JMenuBar();

    /**
     *  Toolbar.
     */
    protected ToolButtonBar toolBar = null;

    /**
     *  Spectral list and related widgets.
     */
    protected JSplitPane splitPane = new JSplitPane();
    protected JList specList = new JList();
    protected JScrollPane specListScroller = new JScrollPane();
    protected TitledBorder specListTitle =
        BorderFactory.createTitledBorder( "Global list of spectra:" );

    /**
     *  Control and current properties region. This needs two JPanels
     *  so that the controls can be adjusted to fit top-right of the
     *  area. The first fills whole of the main BorderLayout.CENTER
     *  region, the second fits in the BorderLayout.NORTH region of
     *  this, thus being at the top and not expanding vertically. The
     *  second JPanel has a GridBagLayout for positioning the controls.
     */
    protected JPanel controlArea = new JPanel();
    protected BorderLayout controlAreaLayout = new BorderLayout();
    protected SplatSelectedProperties selectedProperties =
        new SplatSelectedProperties( specList );
    protected JScrollPane controlScroller = new JScrollPane();
    protected SplatPlotTable plotTable =  new SplatPlotTable( specList );
    protected TitledBorder selectedPropertiesTitle =
        BorderFactory.createTitledBorder( "Properties of current spectra:" );

    /**
     * Orientation of split
     */
    protected JCheckBoxMenuItem splitOrientation = null;

    /**
     * Whether to automatically choose a colour for each spectrum as loaded.
     */
    protected JCheckBoxMenuItem colourAsLoadedItem = null;
    protected boolean colourAsLoaded = true;

    /**
     * Whether to show short or full names in the global list.
     */
    protected JCheckBoxMenuItem showShortNamesItem = null;

    /**
     * Whether short names are simplified.
     */
    protected JCheckBoxMenuItem showSimpleShortNamesItem = null;

    /**
     *  Open or save file chooser.
     */
    protected BasicFileChooser fileChooser = null;

    /**
     *  Names of files for loading.
     */
    protected String[] newFiles = null;

    /**
     *  Whether files loaded should also be displayed (in a single new plot).
     */
    protected boolean displayNewFiles = false;

    /**
     *  Location chooser.
     */
    protected HistoryStringDialog locationChooser = null;

    /**
     *  SSAP browser.
     */
    protected SSAQueryBrowser ssapBrowser = null;
   
    /**
     * OBSCore browser.
     */
    protected ObsCorePanel obscorePanel = null; 

    /**
     *  Stack open or save chooser.
     */
    protected BasicFileChooser stackChooser = null;

    /**
     *  SplatNodeChooser for using DataNode explorer.
     */
    protected SplatNodeChooser splatNodeChooser = null;

    /**
     * SpecAnimatorFrame window for displaying a series of spectra,
     * one after the other.
     */
    protected SpecAnimatorFrame animatorFrame = null;

    /**
     * Frame with binary maths operator controls.
     */
    protected SimpleBinaryMaths binaryMathsFrame = null;

    /**
     * Frame with unary maths operator controls.
     */
    protected SimpleUnaryMaths unaryMathsFrame = null;

    /**
     * Spectrum viewer frames.
     */
    protected ArrayList specViewerFrames = null;

    /**
     * FITS header viewer frames.
     */
    protected ArrayList fitsViewerFrames = null;

    /**
     * Spectral coordinates viewer frame.
     */
    protected SpecCoordinatesFrame coordinatesFrame = null;

    /**
     * Data units viewer frame.
     */
    protected SpecDataUnitsFrame dataUnitsFrame = null;

    /**
     * Whether the application is embedded. In this case application
     * exit is assumed controlled by the embedding app.
     */
    protected boolean embedded = false;

    /**
     * The type of data that spectra are in by default. This is the value of
     * one of the SpecDataFactory constants.
     */
    protected int openUsertypeIndex = SpecDataFactory.DEFAULT;

    /**
     * The type of data that spectra are saved in by default. This is the
     * value of one of the SpecDataFactory constants. Usually only set by the
     * save spectrum dialog.
     */
    protected int saveUsertypeIndex = SpecDataFactory.DEFAULT;

    /**
     * The type of table that spectra are saved in by default. Usually only
     * set by the save spectrum dialog. The formats available are not known
     * until runtime.
     */
    protected int saveTabletypeIndex = 0;

    /**
     * The action to take with 2 and 3D data. Can be COLLAPSE, EXTRACT or
     * VECTORIZE.
     */
    protected int ndAction = SpecDataFactory.COLLAPSE;

    /**
     * Full descriptions of COLLAPSE, EXTRACT and VECTORIZE methods.
     */
    private static String collapseDescription = "collapse";
    private static String extractDescription = "extract all spectra";
    private static String vectorizeDescription = "open whole as 1D";

    /**
     * The dispersion axis of any 2/3D data encountered. By default SPLAT will
     * choose this axis, but it can be specified.
     */
    protected Integer dispAxis = null;

    /**
     * The select axis of any 3D data encountered, this is the axis that is
     * stepped along first, when either collapsing or extracting. By default
     * SPLAT will choose this axis, but it can be specified.
     */
    protected Integer selectAxis = null;

    /**
     * Whether to purge extracted spectra with bad limits. Make's sure
     * the spectra can be displayed.
     */
    protected JCheckBoxMenuItem purgeBadDataLimitsItem = null;
    protected boolean purgeBadDataLimits = true;

    /**
     * Whether to search spectra for a spectral coordinate system.
     */
    protected JCheckBoxMenuItem searchCoordsItem = null;

    /**
     * Whether to plot the spectra to the same window
     */
    protected JCheckBoxMenuItem plotSampSpectraToSameWindowItem = null;
    protected boolean plotSampSpectraToSameWindow = false;

    /**
     * Controls communications for SAMP interoperability.
     */
    protected SplatCommunicator communicator = null;

    /**
     * A long filename to use for setting the default size of a cell
     * in the spectral list.
     */
    protected static String LONG_FILE_NAME = "12345678901234567890" +
        "123456789012345678901234567890123456789012345678901234567890";

    /** Authenticator**/
    private SSAPAuthenticator authenticator;
    
    /**
     *  Create a browser with no existing spectra.
     */
    public SplatBrowser()
    {
        this( null, false, null, null, null, null, null );
    }

    /**
     *  Create a browser with no existing spectra that may be suitable
     *  for embedding (not remote control, exit disabled).
     */
    public SplatBrowser( boolean embedded )
    {
        this( null, embedded, null, null, null, null, null );
    }

    /**
     * Constructor, with list of spectra to initialise. All spectra
     * given this way are displayed in a single plot.
     *
     *  @param inspec list of spectra to add. If null then none are
     *                added.
     */
    public SplatBrowser( String[] inspec )
    {
        this( inspec, false, null, null, null, null, null );
    }

    /**
     * Constructor, with list of spectra to initialise. All spectra
     * given this way are displayed in a single plot.
     *
     *  @param inspec list of spectra to add. If null then none are
     *                added.
     *  @param embedded whether the application is embedded.
     */
    public SplatBrowser( String[] inspec, boolean embedded )
    {
        this( inspec, embedded, null, null, null, null, null );
    }

    /**
     * Constructor, with list of spectra to initialise. All spectra
     * given this way are displayed in a single plot.
     *
     *  @param inspec list of spectra to add. If null then none are
     *                added.
     *  @param embedded whether the application is embedded.
     *  @param type the type of spectra to be opened, if null then the SPLAT
     *              defaults based on the file extensions is used.
     *  @param ndAction the action to take when 2 or 3D data are encountered.
     *                  This can be one of the strings "collapse", "extract" or
     *                  "vectorize" or a unique contraction.
     *  @param dispAxis the dispersion axis to use during collapse/extract
     *                  if any of the spectra are 2/3D. If null then an
     *                  axis will be selected automatically.
     *  @param selectAxis the axis to step along during collapse/extract,
     *                    if any of the spectra are 3D. If null then an axis
     *                    will be selected automatically.
     *  @param communicator object which provides inter-client SAMP 
     *                      communications, null for none.
     */
    public SplatBrowser( String[] inspec, boolean embedded, String type,
                         String ndAction, Integer dispAxis,
                         Integer selectAxis, SplatCommunicator communicator )
    {
        //  Webstart bug: http://developer.java.sun.com/developer/bugParade/bugs/4665132.html
        //  Don't know where to put this though.
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        
        setEmbedded( embedded );
        enableEvents( AWTEvent.WINDOW_EVENT_MASK );
        try {
            if ( communicator != null ) {
                communicator.setBrowser( this );
            }
            this.communicator = communicator;
            initComponents();
        }
        catch ( Exception e ) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            return;
        }

        //  If a type has been given then attempt to match the string to the
        //  known types. A match is declared when the string know to
        //  SpecDataFactory starts with the same sequence of lower case
        //  characters.
        openUsertypeIndex = SpecDataFactory.DEFAULT;
        if ( type != null ) {
            String caselessType = type.toLowerCase();
            int nnames = SpecDataFactory.shortNames.length;
            for ( int i = 0; i < nnames; i++ ) {
                if ( SpecDataFactory.shortNames[i].startsWith(caselessType) ) {
                    openUsertypeIndex = i;
                    break;
                }
            }
        }

        //  If an action for dealing with 2/3D data has been given then
        //  convert this into an appropriate action.
        setNDAction( ndAction );

        //  If axis for dealing with 2 and 3D data have been given then we
        //  need to make sure these are passed on to the SpecDataFactory when
        //  necessary.
        this.dispAxis = dispAxis;
        this.selectAxis = selectAxis;

        //  Now add any command-line spectra. Do this after the interface is
        //  visible and in a separate thread from the GUI and event
        //  queue. Note this may cause the GUI to be realized, so any
        //  additional work must be done on the event queue.
        if ( inspec != null ) {
            newFiles = new String[inspec.length];
            for ( int i = 0; i < inspec.length; i++ ) {
                newFiles[i] = inspec[i];
            }
            displayNewFiles = true;
            SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        threadLoadChosenSpectra();
                        threadInitRemoteServices();
                    }
                });
        }
        else {

            // Make sure we start the remote services, but avoid contention
            // with image loading by also doing this as above when there are
            // files to be loaded.
            SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        threadInitRemoteServices();
                    }
                });
        }
    }

    /**
     * Set the ndAction value to match a string description. Strings are
     * case-insensitive versions of "collapse", "extract" and "vectorize"
     * or the fuller descriptions given by the collapseDescription,
     * extractDescription or vectorizeDescription members.
     * These can all be truncated, as long as they remain unique.
     */
    private void setNDAction( String ndAction )
    {
        this.ndAction = SpecDataFactory.COLLAPSE;
        if ( ndAction != null ) {
            String caselessAction = ndAction.toLowerCase();
            if ( "collapse".startsWith( caselessAction ) ||
                 collapseDescription.startsWith( caselessAction ) ) {
                this.ndAction = SpecDataFactory.COLLAPSE;
            }
            else if ( "extract".startsWith( caselessAction ) ||
                      extractDescription.startsWith( caselessAction ) ) {
                this.ndAction = SpecDataFactory.EXTRACT;
            }
            else if ( "vectorize".startsWith( caselessAction ) ||
                      vectorizeDescription.startsWith( caselessAction ) ) {
                this.ndAction = SpecDataFactory.VECTORIZE;
            }
        }
    }

    /**
     * Set the value of a class boolean preference.
     */
    public static void setPreference( String what, boolean value )
    {
        prefs.putBoolean( what, value );
    }

    /**
     * Get the value of a class boolean preference.
     */
    public static boolean getPreference( String what, boolean defaultValue )
    {
        return prefs.getBoolean( what, defaultValue );
    }

    /**
     *  Initialise all visual components.
     */
    private void initComponents()
    {
        //  Set up the content pane and window size.
        contentPane = (JPanel) getContentPane();

        contentPane.setLayout( new BorderLayout() );

        Utilities.setFrameLocation( (JFrame) this, defaultWindowLocation,
                                    prefs, "SplatBrowser" );
        setTitle( Utilities.getFullDescription() );

        //  Set up menus and toolbar.
        setupMenusAndToolbar();

        //  Add the list of spectra to its scroller.
        specListScroller.getViewport().add( specList, null );

        //  Add titles and help to main components.
        specListScroller.setBorder( specListTitle );
        specList.setToolTipText( "Listing of all short names for spectra,"+
                                 " double click on one to display" );
        controlScroller.setBorder( selectedPropertiesTitle );
        selectedProperties.setToolTipText( "Properties of selected spectra,"+
                               " change to modify views in all plots" );

        //  Set the ListModel of the list of spectra.
        specList.setModel(new SpecListModel(specList.getSelectionModel()));
        specList.setSelectionMode(
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );

        /* 
         * Double click on item(s) to display new plots.
         * Single click on item(s) to highlight them in plot windows
         */
        specList.addMouseListener( new MouseAdapter() {
                public void mouseClicked( MouseEvent e ) {
                    if ( e.getClickCount() >= 2 ) {
                        displaySelectedSpectra();
                    }
                    if ( e.getClickCount() == 1 ) {
                        fireCurrentSpectrumChanged();
                    }
                }
            });

        //  Drag to a plot to display?
        specList.setDragEnabled( true );
        specList.setTransferHandler( new SpecTransferHandler() );

        // Scroll to the current spectra when selection changed
        ((SpecListModel)specList.getModel()).addSelectionChangeListener(new SpecListModelSelectionListener() {
			
			@Override
			public void selectionChanged(SpecListModelSelectionEvent e) {
				if (e.getIndex() != null && e.getIndex().length > 0)
					specList.ensureIndexIsVisible(e.getIndex()[0]);
			}
		});
        
        //  Short or full names.
        setShowShortNames( true );
        setShowSimpleShortNames( false );

        //  Purge bad data limits spectra from 2/2D reprocessing.
        setPurgeBadDataLimits( true );

        //  Searching for spectral coordinates.
        setSearchCoords( true );

        //  Set up the control area.
        controlArea.setLayout( controlAreaLayout );
        controlArea.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );
        controlScroller.getViewport().add( controlArea, null );

        // showing spectra from SAMP in the same window
        setPlotSampSpectraToSameWindow( true );

        
        //  Set up split pane.
        splitPane.setOneTouchExpandable( true );
        setSplitOrientation( true );
        //specList.setSize( new Dimension( 190, 0 ) );

        //  Size a cell in the JList in an attempt to stop the random
        //  size bug (must be a timing issue, sometimes the height/width
        //  are clearly requested when not initialised).
        specList.setPrototypeCellValue( LONG_FILE_NAME );
        specList.setCellRenderer( new FileNameListCellRenderer() );
        splitPane.setDividerLocation( 200 );

        //  Finally add the main components to the content and split
        //  panes. Also positions the toolbar.
        splitPane.setLeftComponent( specListScroller );
        splitPane.setRightComponent( controlScroller );

        controlArea.add( plotTable, BorderLayout.WEST );
        controlArea.add( selectedProperties, BorderLayout.NORTH );

        contentPane.add( splitPane, BorderLayout.CENTER );
    }

    /**
     *  Setup the menus and toolbar.
     */
    private void setupMenusAndToolbar()
    {
        //  Add the menuBar.
        this.setJMenuBar( menuBar );

        //  Create the toolbar. This type manages lack of size.
        toolBar = new ToolButtonBar( contentPane );

        //  Create the File menu.
        createFileMenu();

        //  Edit menu.
        createEditMenu();

        //  View menu.
        createViewMenu();

        //  Options menu.
        createOptionsMenu();

        //  Operations menu.
        createOperationsMenu();

        //  Interop menu.
        createInteropMenu();

        //  Help menu.
        createHelpMenu();
    }

    /**
     * Create the File menu and populate it with appropriate actions.
     */
    private void createFileMenu()
    {
        JMenu fileMenu = new JMenu( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Add action to open a list of spectrum stored in files.
        ImageIcon openImage =
            new ImageIcon( ImageHolder.class.getResource( "openfile.gif" ) );
        LocalAction openAction  = new LocalAction( LocalAction.OPEN,
                                                   "Open", openImage,
                                                   "Open spectra",
                                                   "control O" );
        fileMenu.add( openAction ).setMnemonic( KeyEvent.VK_O );

        toolBar.add( openAction );

        //  Add action to open a spectrum using a typed in location (URL).
        ImageIcon locationImage =
            new ImageIcon( ImageHolder.class.getResource( "location.gif" ) );
        LocalAction locationAction  = new LocalAction( LocalAction.LOCATION,
                                                       "Location",
                                                       locationImage,
                                                       "Open location",
                                                       "control L" );
        fileMenu.add( locationAction ).setMnemonic( KeyEvent.VK_L );

        toolBar.add( locationAction );

        //  Add action to do a search of any SSAP servers.
        ImageIcon ssapImage =
            new ImageIcon( ImageHolder.class.getResource( "ssap.gif" ) );
        LocalAction ssapAction  = new LocalAction( LocalAction.SSAP,
                                                   "SSAP",
                                                   ssapImage,
                                                   "Search SSAP servers" );
        fileMenu.add( ssapAction ).setMnemonic( KeyEvent.VK_P );
        toolBar.add( ssapAction );

       
        // Add acion to go to use OBSCORE
        ImageIcon obscoreImage =
                new ImageIcon( ImageHolder.class.getResource( "obscore.gif" ) );
        LocalAction obsCoreAction = new LocalAction( LocalAction.OBSCORE,
                                                    "ObsCore", 
                                                     obscoreImage, 
                                                     "Query VO using ObsCore TAP" );
        fileMenu.add(obsCoreAction);
        toolBar.add( obsCoreAction );

        //  Add action to browse the local file system and look for tables
        //  etc. in sub-components.
        ImageIcon browseImage =
            new ImageIcon( ImageHolder.class.getResource( "browse.gif" ) );
            javax.swing.plaf.metal.MetalIconFactory.getTreeControlIcon(true);

        LocalAction browseAction  = new LocalAction( LocalAction.BROWSE,
                                                     "Browse", browseImage,
                                                     "Browse for spectra" );
        fileMenu.add( browseAction ).setMnemonic( KeyEvent.VK_B );
        toolBar.add( browseAction );

        //  Add action to re-open a list of spectra if possible.
        ImageIcon reOpenImage =
            new ImageIcon( ImageHolder.class.getResource( "reopen.gif" ) );
        LocalAction reOpenAction  =
            new LocalAction( LocalAction.REOPEN, "Re-Open", reOpenImage,
                             "Re-Open selected spectra", "control R" );
        fileMenu.add( reOpenAction ).setMnemonic( KeyEvent.VK_R );

        toolBar.add( reOpenAction );

        //  Add action to save a spectrum
        ImageIcon saveImage =
            new ImageIcon( ImageHolder.class.getResource( "savefile.gif" ) );
        LocalAction saveAction  =
            new LocalAction( LocalAction.SAVE, "Save", saveImage,
                             "Save a spectrum to disk file", "Control S" );
        fileMenu.add( saveAction ).setMnemonic( KeyEvent.VK_S );

        toolBar.add( saveAction );

        //  Add an action to read in a stack of spectra.
        ImageIcon readStackImage =
            new ImageIcon( ImageHolder.class.getResource( "readstack.gif" ) );
        LocalAction readStackAction =
            new LocalAction( LocalAction.READ_STACK, "Read stack",
                             readStackImage,
                             "Read back spectra stored in a disk file");
        fileMenu.add( readStackAction ).setMnemonic( KeyEvent.VK_T );;

        //  Add an action to save the stack of spectra.
        ImageIcon saveStackImage =
            new ImageIcon( ImageHolder.class.getResource( "savestack.gif" ) );
        LocalAction saveStackAction =
            new LocalAction( LocalAction.SAVE_STACK, "Save stack",
                             saveStackImage, "Save all spectra to disk file" );
        fileMenu.add( saveStackAction ).setMnemonic( KeyEvent.VK_V );

        //  Add an action to exit application.
        ImageIcon exitImage =
            new ImageIcon( ImageHolder.class.getResource( "exit.gif" ) );
        LocalAction exitAction = new LocalAction( LocalAction.EXIT,
                                                  "Exit", exitImage,
                                                  "Exit program",
                                                  "control W" );
        fileMenu.add( exitAction ).setMnemonic( KeyEvent.VK_X );
    }

    /**
     * Create the Edit menu and populate it with appropriate actions.
     */
    private void createEditMenu()
    {
        JMenu editMenu = new JMenu( "Edit" );
        editMenu.setMnemonic( KeyEvent.VK_E );
        menuBar.add( editMenu );

        //  Add an action to remove the selected spectra.
        ImageIcon removeImage =
            new ImageIcon( ImageHolder.class.getResource( "remove.gif" ) );
        LocalAction removeSpectraAction =
            new LocalAction( LocalAction.REMOVE_SPECTRA,
                             "Remove selected spectra", removeImage,
                             "Close any spectra selected in global list",
                             "DELETE" );
        editMenu.add( removeSpectraAction ).setMnemonic( KeyEvent.VK_R );

        toolBar.add( removeSpectraAction );

        //  Add an action to select all spectra.
        LocalAction selectSpectraAction =
            new LocalAction( LocalAction.SELECT_SPECTRA,
                             "Select all spectra", null,
                             "Select all spectra in list",
                             "control A" );
        editMenu.add( selectSpectraAction ).setMnemonic( KeyEvent.VK_S );

        //  Add an action to deselect all spectra.
        LocalAction deSelectSpectraAction =
            new LocalAction( LocalAction.DESELECT_SPECTRA,
                             "Deselect all spectra", null,
                             "Deselect any spectra selected in list" );
        editMenu.add( deSelectSpectraAction ).setMnemonic( KeyEvent.VK_D );

        //  Add an action to remove the selected plots.
        ImageIcon removePlotImage =
            new ImageIcon( ImageHolder.class.getResource( "removeplot.gif" ) );
        LocalAction removePlotAction =
            new LocalAction( LocalAction.REMOVE_PLOTS,
                             "Remove selected plots", removePlotImage,
                             "Close any plots selected in views list" );
        editMenu.add( removePlotAction ).setMnemonic( KeyEvent.VK_O );

        //  Add an action to select all plots.
        LocalAction selectPlotAction =
            new LocalAction( LocalAction.SELECT_PLOTS,
                             "Select all plots", null,
                             "Select all plots in list" );
        editMenu.add( selectPlotAction ).setMnemonic( KeyEvent.VK_E );

        //  Add an action to deselect all plots.
        LocalAction deSelectPlotAction =
            new LocalAction( LocalAction.DESELECT_PLOTS,
                             "Deselect all plots", null,
                             "Deselect any plots selected in list" );
        editMenu.add( deSelectPlotAction ).setMnemonic( KeyEvent.VK_L );

        //  Add an action to copy all selected spectra. This makes
        //  memory copies.
        LocalAction copySelectedSpectraAction =
            new LocalAction( LocalAction.COPY_SPECTRA,
                             "Copy selected spectra", null,
                             "Make memory copies of all selected spectra" );
        editMenu.add( copySelectedSpectraAction ).setMnemonic( KeyEvent.VK_C );

        //  Add an action to copy and sort all selected spectra. This makes
        //  memory copies.
        LocalAction copySortSelectedSpectraAction =
            new LocalAction( LocalAction.COPYSORT_SPECTRA,
                             "Copy and sort selected spectra", null,
                             "Make memory copies of all selected spectra " +
                             "and sort their coordinates if necessary" );
        editMenu.add( copySortSelectedSpectraAction ).
            setMnemonic( KeyEvent.VK_Y );

        //  Add an action to create a new spectrum. The size is
        //  obtained from a dialog.
        LocalAction createSpectrumAction =
            new LocalAction( LocalAction.CREATE_SPECTRUM,
                             "Create new spectrum", null,
                             "Create a new spectrum with unset elements" );
        editMenu.add( createSpectrumAction ).setMnemonic( KeyEvent.VK_T );

        //  Remove any spectra that have no data or invalid coordinates.
        LocalAction purgeSpectraAction =
            new LocalAction( LocalAction.PURGE_SPECTRA,
                             "Purge empty spectra", null,
                             "Remove spectra that cannot be autoranged from" +
                             " global list" );
        editMenu.add( purgeSpectraAction ).setMnemonic( KeyEvent.VK_P );
    }

    /**
     * Create the View menu and populate it with appropriate actions.
     */
    private void createViewMenu()
    {
        JMenu viewMenu = new JMenu( "View" );
        viewMenu.setMnemonic( KeyEvent.VK_V );
        menuBar.add( viewMenu );

        //  Add an action to display the selected spectra.
        ImageIcon displayImage =
            new ImageIcon(ImageHolder.class.getResource("display.gif"));
        LocalAction displayAction =
            new LocalAction( LocalAction.SINGLE_DISPLAY,
                             "Display in new plots",
                             displayImage,
                             "Display selected spectra in separate windows",
                             "control D" );
        viewMenu.add( displayAction ).setMnemonic( KeyEvent.VK_D );

        toolBar.add( displayAction );

        //  Add an action to display the selected spectra into the
        //  selected plots.
        ImageIcon multiDisplayImage =
            new ImageIcon(ImageHolder.class.getResource("multidisplay.gif"));
        LocalAction multiDisplayAction =
            new LocalAction( LocalAction.MULTI_DISPLAY,
                             "Display/add to plot", multiDisplayImage,
                             "Display selected spectra in one plot or add to"+
                             " selected plots", "control I" );
        viewMenu.add( multiDisplayAction ).setMnemonic( KeyEvent.VK_I );

        toolBar.add( multiDisplayAction );

        //  Add an action to display the selected spectra in a single
        //  plot, one at a time.
        ImageIcon animateImage =
            new ImageIcon(ImageHolder.class.getResource("animate.gif"));
        LocalAction animateAction =
            new LocalAction( LocalAction.ANIMATE_DISPLAY,
                             "Animate spectra", animateImage,
                "Animate selected spectra by displaying one at a time" );
        viewMenu.add( animateAction ).setMnemonic( KeyEvent.VK_A );

        toolBar.add( animateAction );

        //  Add an action to view the values of the spectra.
        ImageIcon viewerImage =
            new ImageIcon(ImageHolder.class.getResource("table.gif"));
        LocalAction viewerAction =
            new LocalAction( LocalAction.SPEC_VIEWER,
                             "View/modify spectra values", viewerImage,
                             "View/modify the values of the selected spectra");
        viewMenu.add( viewerAction ).setMnemonic( KeyEvent.VK_V );
        toolBar.add( viewerAction );

        //  Add an action to set the spectral coordinates (x units).
        ImageIcon specCoordsImage =
            new ImageIcon( ImageHolder.class.getResource( "xunits.gif" ) );
        LocalAction specCoordsAction =
            new LocalAction( LocalAction.SPECCOORDS_VIEWER,
                             "View/modify spectral coordinates",
                             specCoordsImage, "View/modify the spectral" +
                             " coordinates of the selected spectra");
        viewMenu.add( specCoordsAction ).setMnemonic( KeyEvent.VK_I );
        toolBar.add( specCoordsAction );

        //  Add an action to set the data units (y units).
        ImageIcon dataUnitsImage =
            new ImageIcon( ImageHolder.class.getResource( "yunits.gif" ) );
        LocalAction dataUnitsAction =
            new LocalAction( LocalAction.DATAUNITS_VIEWER,
                             "View/modify data units",
                             dataUnitsImage, "View/modify the data" +
                             " units of the selected spectra");
        viewMenu.add( dataUnitsAction ).setMnemonic( KeyEvent.VK_E );
        toolBar.add( dataUnitsAction );

        //  Add an action to view the FITS headers of the spectra.
        ImageIcon fitsImage =
            new ImageIcon(ImageHolder.class.getResource( "fits.gif" ) );
        LocalAction fitsAction =
            new LocalAction( LocalAction.FITS_VIEWER,
                             "View FITS headers", fitsImage,
                             "View the FITS header cards of the " +
                             "selected spectra");
        viewMenu.add( fitsAction );
        toolBar.add( fitsAction );

        //  Add an action to cascade all the plot windows.
        JMenuItem cascade = new JMenuItem( "Cascade all plots" );
        viewMenu.add( cascade ).setMnemonic( KeyEvent.VK_C );
        cascade.addActionListener( this );

    }

    /**
     * Create the Options menu and populate it with appropriate actions.
     */
    private void createOptionsMenu()
    {

        JMenu optionsMenu = new JMenu( "Options" );
        optionsMenu.setMnemonic( KeyEvent.VK_O );
        menuBar.add( optionsMenu );

        //  Add any locally availabel line identifiers.
        LocalLineIDManager.getInstance().populate( optionsMenu, this );

        //  Add the LookAndFeel selections.
        new SplatLookAndFeelManager( contentPane, optionsMenu );

        //  Add sub-menu to make the various example datasets available.
        new ExamplesManager( optionsMenu, this );

        //  Add option to choose a different colour for each spectrum as they
        //  are loaded.
        colourAsLoadedItem = new JCheckBoxMenuItem( "Auto-colour" );
        ImageIcon rainbowImage =
            new ImageIcon( ImageHolder.class.getResource( "rainbow.gif" ) );
        colourAsLoadedItem.setIcon( rainbowImage );
        optionsMenu.add( colourAsLoadedItem ).setMnemonic( KeyEvent.VK_C );
        colourAsLoadedItem.setToolTipText
            ( "Automatically choose a colour for each spectrum as loaded" );
        colourAsLoadedItem.addItemListener( this );
        setColourAsLoaded( true );

        //  Add facility to colourise all spectra.
        LocalAction colourizeAction  =
            new LocalAction( LocalAction.COLOURIZE, "Re-auto-colour all",
                             rainbowImage,
                             "Automatically choose a colour for all spectra" );
        JMenuItem colourize = optionsMenu.add( colourizeAction );
        colourize.setMnemonic( KeyEvent.VK_C );
        colourize.setAccelerator( KeyStroke.getKeyStroke( "control U" ) );
        toolBar.add( colourizeAction );

        //  Whether global list shows short or full names.
        showShortNamesItem = new JCheckBoxMenuItem( "Show short names" );
        optionsMenu.add( showShortNamesItem ).setMnemonic( KeyEvent.VK_S );
        showShortNamesItem.setToolTipText
            ( "Show short names in global list, otherwise long names" );
        showShortNamesItem.addItemListener( this );

        //  Whether global list shows simple short names.
        showSimpleShortNamesItem =
            new JCheckBoxMenuItem( "Simple short names" );
        optionsMenu.add( showSimpleShortNamesItem )
            .setMnemonic( KeyEvent.VK_I );
        showSimpleShortNamesItem.setToolTipText
            ( "Show simplified short names in global list, " +
              "when they are same as long names" );
        showSimpleShortNamesItem.addItemListener( this );

        //  Whether spectra with bad data limits should be removed when
        //  opening nD data.
        purgeBadDataLimitsItem =
            new JCheckBoxMenuItem( "Purge 2/3D data" );
        optionsMenu.add( purgeBadDataLimitsItem );
        purgeBadDataLimitsItem.setToolTipText
            ( "When opening 2/3D data do not keep spectra with bad limits" );
        purgeBadDataLimitsItem.addItemListener( this );

        //  Arrange the JSplitPane vertically or horizontally.
        splitOrientation = new JCheckBoxMenuItem( "Vertical split" );
        optionsMenu.add( splitOrientation ).setMnemonic( KeyEvent.VK_O );
        splitOrientation.setToolTipText( "How to split the browser window" );
        splitOrientation.addItemListener( this );

        //  Whether to search for spectral coordinate system, or not.
        searchCoordsItem = new JCheckBoxMenuItem( "Find spectral coordinates" );
        optionsMenu.add( searchCoordsItem );
        searchCoordsItem.setToolTipText
            ( "Search for spectral coordinates when necessary" );
        searchCoordsItem.addItemListener( this );
    }

    /**
     * Create the Interop menu and populate it with appropriate actions,
     * if enabled.
     */
    private void createInteropMenu()
    {
        if ( communicator == null ) {
            return;
        }

        JMenu interopMenu = new JMenu( "Interop" );
        interopMenu.setMnemonic( KeyEvent.VK_I );
        menuBar.add( interopMenu );

        //  Add interop status window item.
        Action winAction = communicator.getWindowAction();
        if ( winAction != null ) {
            interopMenu.add( winAction );
            toolBar.add( winAction );
        }

        //  Add protocol-specific actions.
        Action[] interopActions = communicator.getInteropActions();
        for ( int i = 0; i < interopActions.length; i++ ) {
            interopMenu.add( interopActions[ i ] );
        }

        //  Add spectrum transmit menus items.
        Transmitter specTransmitter =
            communicator.createSpecTransmitter( specList );
        interopMenu.addSeparator();
        interopMenu.add( specTransmitter.getBroadcastAction() )
            .setMnemonic( KeyEvent.VK_B );
        interopMenu.add( specTransmitter.createSendMenu() )
            .setMnemonic( KeyEvent.VK_T );
        
        Transmitter binFITSTableTransmitter = 
        		communicator.createBinFITSTableTransmitter(specList);
        interopMenu.addSeparator();
        interopMenu.add( binFITSTableTransmitter.getBroadcastAction() );
        interopMenu.add( binFITSTableTransmitter.createSendMenu() );
        
        Transmitter voTableTransmitter = 
        		communicator.createVOTableTransmitter(specList);
        interopMenu.addSeparator();
        interopMenu.add( voTableTransmitter.getBroadcastAction() );
        interopMenu.add( voTableTransmitter.createSendMenu() );
        
        
        //  Add checkbox for opening the spectra from SAM to the same plot
        interopMenu.addSeparator();
        plotSampSpectraToSameWindowItem = new JCheckBoxMenuItem( "Same window for SAMP spectra" );
        interopMenu.add( plotSampSpectraToSameWindowItem );
        plotSampSpectraToSameWindowItem.setToolTipText( "Show spectra from SAMP in the same window" );
        plotSampSpectraToSameWindowItem.addItemListener( this );
 
    }

    /**
     * Return the instance of {@link SplatCommunicator} to use.
     */
    public SplatCommunicator getCommunicator()
    {
        return communicator;
    }

    /**
     * Set the vertical or horizontal split.
     */
    protected void setSplitOrientation( boolean init )
    {
        if ( init ) {
            //  Restore state of button from Preferences.
            boolean state = getPreference( "SplatBrowser_vsplit", true );
            splitOrientation.setSelected( state );
        }
        boolean selected = splitOrientation.isSelected();

        if ( selected ) {
            splitPane.setOrientation( JSplitPane.HORIZONTAL_SPLIT );
        }
        else {
            splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
        }
        contentPane.revalidate();
        setPreference( "SplatBrowser_vsplit", selected );
    }

    /**
     * Set whether each spectrum should be assigned an automatic colour as
     * loaded.
     */
    protected void setColourAsLoaded( boolean init )
    {
        if ( init ) {
            //  Restore state of button from Preferences.
            boolean state = getPreference( "SplatBrowser_colourize", true );
            colourAsLoadedItem.setSelected( state );
        }
        colourAsLoaded = colourAsLoadedItem.isSelected();
        setPreference( "SplatBrowser_colourize", colourAsLoaded );
    }

    /**
     * Set whether each spectrum is described by its shortname in the
     * global list, otherwise the full name is displayed.
     */
    protected void setShowShortNames( boolean init )
    {
        if ( init ) {
            //  Restore state of button from Preferences.
            boolean state = getPreference( "SplatBrowser_showshortnames",
                                           true );
            showShortNamesItem.setSelected( state );
        }
        boolean state = showShortNamesItem.isSelected();
        setPreference( "SplatBrowser_showshortnames", state );
        ((SpecListModel)specList.getModel()).setShowShortNames( state );
    }

    /**
     * Set whether each spectrum is described by its simplified shortname in
     * the global list, when showing short names.
     */
    protected void setShowSimpleShortNames( boolean init )
    {
        if ( init ) {
            //  Restore state of button from Preferences.
            boolean state = getPreference( "SplatBrowser_showsimpleshortnames",
                                           false );
            showSimpleShortNamesItem.setSelected( state );
        }
        boolean state = showSimpleShortNamesItem.isSelected();
        setPreference( "SplatBrowser_showsimpleshortnames", state );
        ((SpecListModel)specList.getModel()).setShowSimpleShortNames( state );
    }

    /**
     * Set whether to remove any spectra with bad data limits when opening
     * 2/3D data.
     */
    protected void setPurgeBadDataLimits( boolean init )
    {
        if ( init ) {
            //  Restore state of button from Preferences.
            boolean state = getPreference( "SplatBrowser_purgebaddatalimits",
                                           true );
            purgeBadDataLimitsItem.setSelected( state );
        }
        purgeBadDataLimits = purgeBadDataLimitsItem.isSelected();
        setPreference( "SplatBrowser_purgebaddatalimits", purgeBadDataLimits );
    }
    
    /**
     * Set whether to show spectra from SAMP in the same window
     */
    protected void setPlotSampSpectraToSameWindow( boolean init )
    {
        if ( init ) {
            // TODO: add this to the preferences?
            boolean state = false;
            plotSampSpectraToSameWindowItem.setSelected( state );
        }
        plotSampSpectraToSameWindow = plotSampSpectraToSameWindowItem.isSelected();
        if (plotSampSpectraToSameWindow)
            globalList.setLastPlotForSourceType(SourceType.SAMP, null);
        setPreference( "SplatBrowser_plotsampspectratosamewindow", plotSampSpectraToSameWindow );
    }


    /**
     * Set whether each spectrum should be searched for a spectral coordinate
     * system, if the default system is non-spectral.
     */
    protected void setSearchCoords( boolean init )
    {
        if ( init ) {
            //  Restore state of button from Preferences.
            boolean state = getPreference( "SplatBrowser_searchcoords", true );
            searchCoordsItem.setSelected( state );
        }
        boolean state = searchCoordsItem.isSelected();
        setPreference( "SplatBrowser_searchcoords", state );
        SpecData.setSearchForSpecFrames( state );
    }


    /**
     * Create the Operations menu and populate it with appropriate
     * actions.
     */
    protected void createOperationsMenu()
    {
        JMenu operationsMenu = new JMenu( "Operations" );
        operationsMenu.setMnemonic( KeyEvent.VK_P );
        menuBar.add( operationsMenu );

        //  Add controls for the simple maths operations.
        ImageIcon mathImage = new ImageIcon(
            ImageHolder.class.getResource( "binarymath.gif" ) );
        LocalAction binaryMathAction  =
            new LocalAction( LocalAction.BINARY_MATHS,
                             "Simple two spectra maths",
                             mathImage,
                             "Add, subtract, divide or multiply two specra" );
        operationsMenu.add( binaryMathAction );
        toolBar.add( binaryMathAction );

        mathImage = new ImageIcon(
            ImageHolder.class.getResource( "unarymaths.gif" ) );
        LocalAction unaryMathAction  =
            new LocalAction( LocalAction.UNARY_MATHS,
                             "Simple constant maths",
                             mathImage,
                             "Add, subtract, divide or multiply spectrum by constant" );
        operationsMenu.add( unaryMathAction );
        toolBar.add( unaryMathAction );
    }

    /**
     * Create the Help menu and populate it with appropriate actions.
     */
    protected void createHelpMenu()
    {
        JMenu helpMenu = HelpFrame.createButtonHelpMenu( "browser-window",
                                                         "Help on window",
                                                         menuBar, toolBar );

        //  Add an action to display the about dialog.
        Action aboutAction = AboutFrame.getAction( this );
        helpMenu.add( aboutAction );
    }

    /**
     * Initialise the remote control services. These are currently via
     * a socket interface and SOAP based services.
     *
     * This creates a file ~/.splat/.remote with a configuration
     * necessary to contact this through the remote socket control
     * service.
     *
     * @see RemoteServer
     */
    protected void initRemoteServices()
    {
        if ( ! embedded ) {

            //  Client interop registration.
            if ( communicator != null ) {
                try {
                    String msg = "Attempting registration with "
                        + communicator.getProtocolName()
                        + " hub: ";
                    boolean isReg = communicator.setActive();
                    msg += isReg ? "success" : "failure";
                    logger.info( msg );
                }
                catch ( Exception e ) {
                    logger.warning( "Unexpected registration failure: " + e );
                }
            }

            try {
                //  Socket-based services.
                RemoteServer remoteServer = new RemoteServer( this );
                remoteServer.start();

                //  SOAP based services.
                SplatSOAPServer soapServer = SplatSOAPServer.getInstance();
                soapServer.setSplatBrowser( this );
                soapServer.start();
            }
            catch (Exception e) {
                // Not fatal, just no remote control.
                logger.warning( "Failed to start remote services" );
                logger.warning( e.getMessage() );
            }
        }
    }

    /**
     * Initialise the remote control services in a separate thread. Use this
     * to get UI going quickly, but note that the remote services may take
     * some time to initialise.
     */
    protected void threadInitRemoteServices()
    {
        Thread loadThread = new Thread( "Remote services loader" ) {
                public void run() {
                    try {
                        initRemoteServices();
                    }
                    catch (Exception e) {
                        logger.log( Level.INFO, e.getMessage(), e );
                    }
                }
            };

        // Thread runs at lowest priority and is a daemon (i.e. runs
        // until application exits, but does not hold lock to exit).
        loadThread.setDaemon( true );
        loadThread.setPriority( Thread.MIN_PRIORITY );
        loadThread.start();
    }

    /**
     *  Colourize, that is automatically set the colour of all spectra.
     */
    protected void colourizeSpectra()
    {
        int size = globalList.specCount();
        int rgb;
        for ( int i = 0; i < size; i++ ) {
            rgb = MathUtils.getRandomRGB();
            globalList.setKnownNumberProperty(
                             (SpecData) globalList.getSpectrum( i ),
                             SpecData.LINE_COLOUR,
                             new Integer( rgb ) );
        }
    }

    /**
     * Enable the open file chooser. Any selected files are added to
     * the global list of spectra. Note that multiple selections are
     * allowed, but not shown correctly in the list of selected files.
     */
    protected void showOpenFileChooser()
    {
        initFileChooser( true );
        int result = fileChooser.showOpenDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            if ( selectedFiles.length == 0 ) {
                selectedFiles = new File[1];
                selectedFiles[0] = fileChooser.getSelectedFile();
            }
            newFiles = new String[selectedFiles.length];
            for ( int i = 0; i < selectedFiles.length; i++ ) {
                newFiles[i] = selectedFiles[i].getPath();
            }

            //  If the user requested that opened spectra are also
            //  displayed, then respect this.
            displayNewFiles = openDisplayCheckBox.isSelected();

            //  Use the given type for spectra (NDF, FITS etc.).
            openUsertypeIndex = openUsertypeBox.getSelectedIndex();

            //  Set the ndAction.
            setNDAction( (String) ndActionBox.getSelectedItem() );

            //  And the dispersion and select axes.
            KeyValue keyvalue = (KeyValue) dispersionAxisBox.getSelectedItem();
            dispAxis = (Integer) keyvalue.getValue();
            keyvalue = (KeyValue) selectAxisBox.getSelectedItem();
            selectAxis = (Integer) keyvalue.getValue();

            //  Load the spectra.
            threadLoadChosenSpectra();
        }
    }

    /**
     * Enable the save file chooser. The currently selected spectrum
     * is saved to a file with the chosen name and data format.
     */
    protected void showSaveFileChooser()
    {
        //  Check that only one spectrum is selected.
        int[] indices = getSelectedSpectra();
        if ( indices == null || indices.length != 1 ) {
            JOptionPane.showMessageDialog
                ( this,
                  "You need to select one spectrum in the global list",
                  "Too many/too few spectra", JOptionPane.ERROR_MESSAGE );
            return;
        }
        initFileChooser( false );
        int result = fileChooser.showSaveDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            File destFile = fileChooser.getSelectedFile();
            if ( destFile != null ) {
                //  Record any choice of file and table types.
                saveUsertypeIndex = saveUsertypeBox.getSelectedIndex();
                saveTabletypeIndex = saveTabletypeBox.getSelectedIndex();
                threadSaveSpectrum( indices[0], destFile.getPath() );
            }
            else {
                JOptionPane.showMessageDialog( this,
                                               "No spectrum selected",
                                               "No write",
                                               JOptionPane.WARNING_MESSAGE );
            }
        }
    }

    /**
     * Show window for browsing a tree of data files and, importantly"
     * the components of the files too. This allows interactive
     * selection of any displayable components.
     */
    public void showSplatNodeChooser()
    {
        SpecData specData = null;
        if ( splatNodeChooser == null ) {
            splatNodeChooser = new SplatNodeChooser();
        }
        specData = splatNodeChooser.choose( this, "Open", "Select spectrum" );
        if ( specData != null ) {
            addSpectrum( specData );
            displaySpectrum( specData );
        }
    }

    /**
     * Initialise the file chooser to have the necessary filters.
     *
     * @param openDialog if true then the dialog is initialise for
     *                   reading in spectra.
     */
    protected void initFileChooser( boolean openDialog )
    {
        if ( fileChooser == null ) {
            fileChooser = new BasicFileChooser( false );
            fileChooser.setMultiSelectionEnabled( true );
            fileChooser.setFileView( new SpectralFileView() );

            //  Add FileFilters based on extension for all known types.
            BasicFileFilter fileFilter = null;
            String[][] extensions = SpecDataFactory.extensions;
            String[] longNames = SpecDataFactory.longNames;
            for ( int i = 1; i < longNames.length; i++ ) {
                fileFilter = new BasicFileFilter( extensions[i], longNames[i] );
                fileChooser.addChoosableFileFilter( fileFilter );
            }

            // FileFilter for all files (same as Factory types indexed by 0).
            fileChooser.addChoosableFileFilter
                ( fileChooser.getAcceptAllFileFilter() );
        }
        if ( openDialog ) {
            if ( openAccessory == null ) {
                initOpenAccessory();
            }
            fileChooser.setAccessory( openAccessory );
            fileChooser.setMultiSelectionEnabled( true );
        }
        else {
            if ( saveAccessory == null ) {
                initSaveAccessory();
            }
            fileChooser.setAccessory( saveAccessory );
            fileChooser.setMultiSelectionEnabled( false );
        }
    }

    // OpenAccessory components.
    protected JPanel openAccessory = null;
    protected JCheckBox openDisplayCheckBox = null;
    protected JComboBox openUsertypeBox = null;
    protected JComboBox ndActionBox = null;
    protected JComboBox dispersionAxisBox = null;
    protected JComboBox selectAxisBox = null;

    /**
     * Initialise the accessory components for opening spectra. Currently
     * these provide the ability to choose whether to display any opened
     * spectra, what type to assign and what axes to use when handling any
     * 2 or 3D spectra.
     */
    protected void initOpenAccessory()
    {
        openAccessory = new JPanel();
        GridBagLayouter layouter =
            new GridBagLayouter( openAccessory, GridBagLayouter.SCHEME3 );

        openDisplayCheckBox = new JCheckBox();
        openDisplayCheckBox.setToolTipText
            ( "Display opened spectra in new plot" );
        openDisplayCheckBox.setSelected( true );

        layouter.add( "Display:", false );
        layouter.add( openDisplayCheckBox, true );

        // User may override builtin file extensions by providing an
        // explicit type.
        openUsertypeBox = new JComboBox( SpecDataFactory.shortNames );
        openUsertypeBox.setToolTipText
            ( "Choose a type for the selected files" );

        layouter.add( "Format:", false );
        layouter.add( openUsertypeBox, true );

        JPanel ndPanel = new JPanel();
        GridBagLayouter ndLayouter =
            new GridBagLayouter( ndPanel, GridBagLayouter.SCHEME3 );
        ndPanel.setBorder
            ( BorderFactory.createTitledBorder( "2/3D data" ) );

        //  Method to handle 2/3D data.
        ndActionBox = new JComboBox();
        ndActionBox.addItem( collapseDescription );
        ndActionBox.addItem( extractDescription );
        ndActionBox.addItem( vectorizeDescription );
        ndActionBox.setToolTipText( "Choose a method for handling 2/3D data" );

        ndLayouter.add( "Action:", false );
        ndLayouter.add( ndActionBox, true );

        //  Select a dispersion axis.
        dispersionAxisBox = new JComboBox();
        dispersionAxisBox.addItem( new KeyValue( "auto", new Integer( -1 ) ) );
        dispersionAxisBox.addItem( new KeyValue( "1", new Integer( 0 ) ) );
        dispersionAxisBox.addItem( new KeyValue( "2", new Integer( 1 ) ) );
        dispersionAxisBox.addItem( new KeyValue( "3", new Integer( 2 ) ) );
        dispersionAxisBox.setToolTipText
            ( "Choose a dispersion axis for 2/3D data" );

        ndLayouter.add( "Disp axis:", false );
        ndLayouter.add( dispersionAxisBox, true );

        //  Select a dispersion axis.
        selectAxisBox = new JComboBox();
        selectAxisBox.addItem( new KeyValue( "auto", new Integer( -1 ) ) );
        selectAxisBox.addItem( new KeyValue( "1", new Integer( 0 ) ) );
        selectAxisBox.addItem( new KeyValue( "2", new Integer( 1 ) ) );
        selectAxisBox.addItem( new KeyValue( "3", new Integer( 2 ) ) );
        selectAxisBox.setToolTipText
            ( "Select a non-dispersion axis for 3D data" );

        ndLayouter.add( "Select axis:", false );
        ndLayouter.add( selectAxisBox, true );

        layouter.add( ndPanel, true );
        layouter.eatSpare();
    }

    // StackAccessory components.
    protected JPanel stackOpenAccessory = null;
    protected JCheckBox stackOpenDisplayCheckBox = null;

    /**
     * Initialise the accessory components for opening a stack of spectra.
     * Currently this provides the ability to choose whether to display
     * any opened spectra.
     */
    protected void initStackOpenAccessory()
    {
        stackOpenAccessory = new JPanel();
        GridBagLayouter layouter = new GridBagLayouter( stackOpenAccessory );

        stackOpenDisplayCheckBox = new JCheckBox();
        stackOpenDisplayCheckBox.setToolTipText
            ( "Display opened spectra in new plot" );
        stackOpenDisplayCheckBox.setSelected( true );

        layouter.add( "Display:", false );
        layouter.add( stackOpenDisplayCheckBox, true );
        layouter.eatSpare();
    }

    // SaveAccessory components.
    protected JPanel saveAccessory = null;
    protected JComboBox saveUsertypeBox = null;
    protected JComboBox saveTabletypeBox = null;

    /**
     * Initialise the accessory components for saving spectra.
     * Currently these provide the ability to choose whether
     * to display any opened spectra.
     */
    protected void initSaveAccessory()
    {
        saveAccessory = new JPanel();
        GridBagLayouter layouter =
            new GridBagLayouter( saveAccessory, GridBagLayouter.SCHEME5 );
        layouter.setInsets( new Insets( 0, 3, 3, 0 ) );

        // User may override builtin file extensions by providing an
        // explicit type.
        saveUsertypeBox = new JComboBox( SpecDataFactory.shortNames );
        saveUsertypeBox.setToolTipText
            ( "Choose a type for the file when saved" );

        layouter.add( "Format", true );
        layouter.add( saveUsertypeBox, true );

        // List of tables types available.
        List list = specDataFactory.getKnownTableFormats();
        list.add( 0, "default" );
        list.remove( "jdbc" );
        saveTabletypeBox = new JComboBox( list.toArray() );
        saveTabletypeBox.setPrototypeDisplayValue( "votable-binary-inl" );
        saveTabletypeBox.setToolTipText( "Type of table to write" );

        layouter.add( "Table type", true );
        layouter.add( saveTabletypeBox, true );
        layouter.eatSpare();
    }

    /**
     * A request to save the spectrum stack has been received.
     */
    protected void saveStackEvent()
    {
        // Saving the stack performed by the SpecList global
        // object. Just give it a file name to use.
        initStackChooser( false );
        int result = stackChooser.showSaveDialog( this );
        if ( result == stackChooser.APPROVE_OPTION ) {
            File file = stackChooser.getSelectedFile();
            if ( !file.exists() || ( file.exists() && file.canWrite() ) ) {
                SpecList specList = SpecList.getInstance();
                //SpecList.FileFormat fileFormat = SpecList.FileFormat.STK;
                /*for (SpecList.FileFormat ff : SpecList.FileFormat.values()) {
                    if (stackChooser.getFileFilter().getDescription().startsWith(ff.getDescription())) {
                        fileFormat = ff;
                        break;
                    }
                }*/
                SpecList.FileFormat fileFormat = getFileFormat(stackChooser, SpecList.FileFormat.STK);
               
                specList.writeStack( fileFormat, file.getPath() );            
            }
            else {
                JOptionPane.showMessageDialog
                    ( this, "Cannot write to file:" + file.getPath(),
                      "File access error", JOptionPane.ERROR_MESSAGE );
            }
        }
    }

    /**
     * A request to read in a stack of stored spectra.
     */
    protected void readStackEvent()
    {
        // Reading the stack performed by the SpecList global object. Just
        // give it a file name to use.
        initStackChooser( true );
        int result = stackChooser.showOpenDialog( this );
        if ( result == stackChooser.APPROVE_OPTION ) {
            File file = stackChooser.getSelectedFile();
            if ( file.exists() && file.canRead() ) {
                SpecList.FileFormat fileFormat = getFileFormat(stackChooser, SpecList.FileFormat.STK, true);
                readStack( file, fileFormat, stackOpenDisplayCheckBox.isSelected() );
            }
            else {
                JOptionPane.showMessageDialog
                    ( this, "Cannot read file:" + file.getPath(),
                      "File access error", JOptionPane.ERROR_MESSAGE );
            }
        }
    }

    /**
     * Read and optionally display a file containing a stack of spectra.
     *
     * @param file containing serialized SpecList instance.
     * @param display whether to display the new spectra in a new plot.
     * @return the index of the plot created, -1 otherwise.
     */
    public int readStack( File file, boolean display )
    {
        return readStack(file, SpecList.FileFormat.STK, display);
    }

    /**
     * Read and optionally display a file containing a stack of spectra.
     *
     * @param file containing serialized SpecList instance.
     * @param fileFormat Format of input stack file
     * @param display whether to display the new spectra in a new plot.
     * @return the index of the plot created, -1 otherwise.
     */
    public int readStack( File file, SpecList.FileFormat fileFormat, boolean display )
    {
        try {
            return readStack( new FileInputStream( file ), fileFormat, display, file.getPath() );
        }
        catch (Exception e) {
            logger.log( Level.SEVERE, e.getMessage(), e );
        }
        return -1;
    }

    /**
     * Read and optionally display an InputStream that contains a stack of
     * spectra.
     *
     * @param in stream with serialized SpecList instance.
     * @param display whether to display the new spectra in a new plot.
     * @return the index of the plot created, -1 otherwise.
     */
    public int readStack( InputStream in, boolean display )
    {
        return readStack(in, SpecList.FileFormat.STK, display, null);
    }

    /**
     * Read and optionally display an InputStream that contains a stack of
     * spectra.
     *
     * @param in stream with serialized SpecList instance.
     * @param fileFormat Format of input stack file
     * @param display whether to display the new spectra in a new plot.
     * @return the index of the plot created, -1 otherwise.
     */
    public int readStack( InputStream in, SpecList.FileFormat fileFormat, boolean display, String sourcePath )
    {
        int plotIndex = -1;

        if (fileFormat == null)
            return plotIndex;

        SpecList globalSpecList = SpecList.getInstance();
        int nread = 0;

        switch (fileFormat) {
            case STK:
                nread = globalSpecList.readStack( in );
                break;
            case FITS:
                nread = globalSpecList.readStack(sourcePath, fileFormat, this);
                break;
        }

        //  If requested honour the display option.
        if ( ( nread > 0 ) && display ) {
            int count = globalList.specCount();
            deSelectAllPlots();
            plotIndex = displayRange( count - nread, count - 1 );
        }

        return plotIndex;
    }


    /**
     * Initialise the stack file chooser to have the necessary filter.
     *
     * @param openDialog if true then the dialog is initialise for
     * reading in a stack (this adds a component for displaying any
     * spectra as well as opening).
     */
    protected void initStackChooser( boolean openDialog )
    {
        if ( stackChooser == null ) {
            stackChooser = new BasicFileChooser( false );
            stackChooser.setMultiSelectionEnabled( false );
            
            for (SpecList.FileFormat ff : SpecList.FileFormat.values()) {
                BasicFileFilter stackFilter =
                    new BasicFileFilter( ff.getFileExtension(), ff.getDescription() );
                stackChooser.addChoosableFileFilter( stackFilter );
            }

            stackChooser.addChoosableFileFilter
                ( stackChooser.getAcceptAllFileFilter() );
        }
        if ( openDialog ) {
            if ( stackOpenAccessory == null ) {
                initStackOpenAccessory();
            }
            stackChooser.setAccessory( stackOpenAccessory );
        }
        else {
            stackChooser.setAccessory( null );
        }
    }

    /**
     * Enable the location chooser. An attempt to open the result as a
     * spectrum is made. This offers the ability to enter an arbitrary string
     * as a spectrum, such as a URL.
     */
    protected void showLocationChooser()
    {
        if ( locationChooser == null ) {
            locationChooser = new HistoryStringDialog( this, "URL/Location",
                                                       "Enter a location" );
        }
        String result = locationChooser.showDialog( locationChooser );
        if ( result != null ) {
            newFiles = new String[1];
            newFiles[0] = result;
            openUsertypeIndex = SpecDataFactory.GUESS;
            threadLoadChosenSpectra();
        }
    }

    /**
     * Enable the SSAP browser.
     */
    protected void showSSAPBrowser()
    {
        if ( ssapBrowser == null ) {
            try {
                ssapBrowser = new SSAQueryBrowser( new SSAServerList(), this );
                authenticator = ssapBrowser.getAuthenticator();
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
                return;
            }
        } else {
            if (authenticator == null )
                authenticator = ssapBrowser.getAuthenticator();
        }
        ssapBrowser.setVisible( true );
    }

    /**
     * Open the OBSCORE window
     */
    public void showObscorePanel()
    {
        if ( obscorePanel == null ) {
            try {
                obscorePanel = new ObsCorePanel(this);
            }
            catch (Exception e) {
                ErrorDialog.showError( this, e );
                return;
            }
        }
        obscorePanel.setVisible( true );

    }

    
    /**
     * Open and display all the spectra listed in the newFiles array. Uses a
     * thread to load the files so that we do not block the UI (although the
     * wait cursor is enabled so no interaction can be performed).
     */
    protected void threadLoadChosenSpectra()
    {
        SpectrumIO sio = SpectrumIO.getInstance();
        sio.load( this, newFiles, displayNewFiles, openUsertypeIndex );
    }

    /**
     * Load and optionally display a list of spectra with some pre-defined
     * properties that should be applied to the spectra immediately after
     * loading. Uses a thread to load the files so that we do not block the UI.
     *
     * @param props properties of the spectra to be loaded, including names.
     * @param display whether to also display the spectra.
     */
  
    public void threadLoadSpectra( SpectrumIO.Props[] props, boolean display )
    {
        if ( props.length == 0 ) return;
        SpectrumIO sio = SpectrumIO.getInstance();
        sio.load( this, display, props );
    }

    /**
     * Load and optionally display a list of spectra with some pre-defined
     * properties that should be applied to the spectra immediately after
     * loading. Uses a thread to load the files, but waits until all the
     * spectra have been loaded before returning. Returns the number of
     * spectra successfully loaded.
     *
     * @param props properties of the spectra to be loaded, including names.
     * @param display whether to also display the spectra.
     */
    public int blockedThreadLoadSpectra( SpectrumIO.Props[] props,
                                          boolean display )
    {
        if ( props.length == 0 ) return 0;
        SpectrumIO sio = SpectrumIO.getInstance();

        final Object lock = new Object();

        class Watcher implements SpectrumIO.Watch 
        {
            public int nseen = 0;

            public void loadSucceeded( SpectrumIO.Props props )
            {
                nseen++;
                synchronized( lock ) {
                    lock.notify();
                }
            }
            public void loadFailed( SpectrumIO.Props props,
                                    Throwable error )
            {
                nseen++;
                synchronized( lock ) {
                    lock.notify();
                }
            }

            public int getSeen()
            {
                return nseen;
            }
        };

        Watcher watcher = new Watcher();
        sio.setWatcher( watcher );
        sio.load( this, display, props );

        synchronized ( lock ) {
            while ( watcher.getSeen() < props.length ) {
                try {
                    lock.wait();
                }
                catch( InterruptedException i ) {
                    // Give up.
                    break;
                }
            }
        }
        return watcher.getSeen();
    }

    /**
     * Save a spectrum. Uses a thread to load the files so that we do not
     * block the UI (although the wait cursor is enabled so no interaction can
     * be performed).
     * @param globalIndex global list index of the spectrum to save.
     * @param target the file to write the spectrum into.
     */
    protected void threadSaveSpectrum( int globalIndex, String target )
    {
        SpectrumIO sio = SpectrumIO.getInstance();
        sio.save( this, globalIndex, target );
    }

    /**
     * Set the main cursor to indicate waiting for some action to
     * complete and lock the interface by trapping all mouse events.
     */
    public void setWaitCursor()
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
    public void resetWaitCursor()
    {
        getGlassPane().setCursor( null );
        getGlassPane().setVisible( false );
    }

  
    /**
     * Add a new spectrum, with a possibly pre-defined type, to the
     * global list. This becomes the current spectrum. Any errors are reported
     * using an {@link ErrorDialog}.
     *
     *  @param name the name (i.e. file specification) of the spectrum
     *              to add.
     *  @param usertype index of the type of spectrum, 0 for default
     *                  based on file extension, otherwise this is an
     *                  index of the knownTypes array in
     *                  {@link SpecDataFactory}.
     *
     *  @return true if spectrum is added, false otherwise.
     */
    public boolean addSpectrum( String name, int usertype )
    {
        
        try {
            tryAddSpectrum( name, usertype );
            return true;
        }
        catch ( SplatException e ) {
            ErrorDialog.showError( this, e );
        }
        return false;
    }

    /**
     * Add a new spectrum, with a possibly pre-defined type, to the
     * global list. This becomes the current spectrum. If an error occurs a
     * {@link SplatException} is thrown.
     *
     *  @param name the name (i.e. file specification) of the spectrum
     *              to add.
     *  @param usertype index of the type of spectrum, 0 for default
     *                  based on file extension, otherwise this is an
     *                  index of the knownTypes array in
     *                  {@link SpecDataFactory}.
     */
    public void tryAddSpectrum( String name, int usertype )
        throws SplatException
    {
        
        if ( usertype == SpecDataFactory.SED || usertype == SpecDataFactory.TABLE) {
            //  Could be a source of several spectra. Only XML serialisation
            //  understood by this route. FITS should be trapped below.
            SpecData spectra[] = specDataFactory.expandXMLSED( name );
            for ( int i = 0; i < spectra.length; i++ ) {
                addSpectrum( spectra[i] );
            }
        }
        else {           
               
            try {
                List<SpecData> spectra = specDataFactory.getAll( name, usertype );
                if (spectra != null) {
                    for (SpecData spectrum : spectra) {
                        addSpectrum( spectrum );
                    }
                }
            }
            catch (SEDSplatException e) {
                if ( usertype == SpecDataFactory.FITS ) {
                    //  An SED detected behind the scenes (usually a FITS
                    //  tables with vector cells).
                    SpecData spectra[] =
                        specDataFactory.expandFITSSED( name, e.getRows() );
                    for ( int i = 0; i < spectra.length; i++ ) {
                        addSpectrum( spectra[i] );
                    }
                }
                else {
                    if (authenticator.getStatus() != null ) // in this case there as an error concerning authentication
                        throw new SplatException(authenticator.getStatus());
                    else 
                        throw e;
                }
            }//catch
        } // else 
    } // tryaddspectrum

    /**
     * Add a new spectrum, with a possibly pre-defined set of characteristics
     * as defined in a {@link SpectrumIO.Props} instance.  If successful this
     * becomes the current spectrum. If an error occurs a
     * {@link SplatException} is thrown.
     *
     *  @param props a container class for the spectrum properties, including
     *               the specification (i.e. file name etc.) of the spectrum
     * @throws IOException 
     * @throws TableFormatException 
     */
    public void tryAddSpectrum( SpectrumIO.Props props )
        throws SplatException, TableFormatException, IOException
    {
        if ( props.getType() == SpecDataFactory.SED || props.getType() == SpecDataFactory.TABLE ) {
            //  Could be a source of several spectra.
            SpecData spectra[] =
                specDataFactory.expandXMLSED( props.getSpectrum() );
            String shortname=props.getShortName();
            for ( int i = 0; i < spectra.length; i++ ) {
                String str = spectra[i].getShortName();
                if (spectra[i].getObjectType() == null) {
                	spectra[i].setObjectType(props.getObjectType());
                }
                addSpectrum( spectra[i] );
                if (str != null && str.startsWith("order"))
                    props.setShortName(shortname+" ["+str+"]");
                props.apply( spectra[i] );
                
                System.out.println("and146: SED or TABLE #" + i);
            }
        }
        else {
              
            try {      
                String specstr = props.getSpectrum();
                SpecData spectrum;
                List<SpecData> spectra;
                    if (props.getType() == SpecDataFactory.DATALINK) {
                        System.out.println("and146: datalink");
                    	DataLinkParams dlparams = new DataLinkParams(props.getSpectrum());
                        props.setSpectrum(dlparams.getQueryAccessURL(0)); // get the accessURL for the first service read 
                        String stype = null;
                        if (props.getDataLinkFormat() != null ) { // see if user has changed the output format
                        	stype = props.getDataLinkFormat();
                        	props.setType(SpecDataFactory.mimeToSPLATType(stype));
                            //props.setObjectType(SpecDataFactory.mimeToObjectType(stype));
                        }
                        else if ( dlparams.getQueryContentType(0) == null || dlparams.getQueryContentType(0).isEmpty()) //if not, use contenttype
                            props.setType(SpecDataFactory.GUESS);
                        else { 
                            stype = dlparams.getQueryContentType(0);
                        	props.setType(SpecDataFactory.mimeToSPLATType(stype));
                        	//props.setObjectType(SpecDataFactory.mimeToObjectType(stype));
                        }
                    }
                    spectra = specDataFactory.get( props.getSpectrum(), props.getType() ); ///!!! IF it's a list???
                    for (int s=0; s < spectra.size(); s++ ){
                        spectrum=spectra.get(s);
                        String sname = spectrum.getShortName();
                        if (sname != null && ! sname.isEmpty())
                            props.setShortName(sname);
                        spectrum.setObjectType(props.getObjectType());
                        addSpectrum( spectrum );
                        props.apply( spectrum );
                        
                    }
                //}
                
            }
            catch (SEDSplatException se) {

                // Is the spectrum in a file or url?
                String specpath;
                if (se.getSpec() != null)
                    specpath=se.getSpec();
                else 
                    specpath=props.getSpectrum();

                //  Could be a FITS table with an SED representation.
                if ( props.getType() == SpecDataFactory.FITS || se.getType() == SpecDataFactory.FITS) {
                    SpecData spectra[] = specDataFactory.expandFITSSED( specpath, se.getRows() );
                    for ( int i = 0; i < spectra.length; i++ ) {
                        addSpectrum( spectra[i] );
                        props.apply( spectra[i] );
                    }
                } 
                //   }
                if (authenticator.getStatus() != null ) // in this case there as an error concerning authentication
                    throw new SplatException(authenticator.getStatus());
                //        else 
                //                    throw new SplatException( se );

            }
            catch(SplatException sple) {
                if (! sple.getMessage().contains("No TABLE element found")) 
                    JOptionPane.showMessageDialog
                    ( this, sple.getMessage(), "",
                      JOptionPane.ERROR_MESSAGE );
            }
        }
    }

    /**
     * Add a new spectrum to the global list. This becomes the current
     * spectrum.
     *
     *  @param name the name (i.e. file specification) of the spectrum
     *              to add.
     *
     *  @return true if spectrum is added, false otherwise.
     */
    public boolean addSpectrum( String name ) {
        return addSpectrum(name, SourceType.UNDEFINED);
    }
    
    /**
     * Add a new spectrum to the global list. This becomes the current
     * spectrum.
     *
     *  @param name the name (i.e. file specification) of the spectrum
     *              to add.
     *  @param sourceType source type from which the spectra came from
     *
     *  @return true if spectrum is added, false otherwise.
     */
    public boolean addSpectrum( String name, SourceType sourceType )
    {
        
        try {
            SpecData spectrum = specDataFactory.get( name);
            addSpectrum( spectrum );
            return true;
        }
        catch ( SplatException e ) {
            JOptionPane.showMessageDialog( this,
                                           e.getMessage(),
                                           "Error opening spectrum",
                                           JOptionPane.ERROR_MESSAGE );
        }
        return false;
    }

    /**
     * Add a new SpecData object to the global list. This becomes the
     * current spectrum.
     *
     * @param spectrum the SpecData object.
     */
    public void addSpectrum( SpecData spectrum ) {
        addSpectrum(spectrum, SourceType.UNDEFINED);
    }
    /**
     * Add a new SpecData object to the global list. This becomes the
     * current spectrum.
     *
     * @param spectrum the SpecData object.
     * @param sourceType source type from which the spectra came from
     */
    public void addSpectrum( SpecData spectrum, SourceType sourceType )
    {
        //  Get the current top of SpecList.
        SpecList list = SpecList.getInstance();
        int top = list.specCount();

        //  2D spectra may need reprocessing by collapsing or extracting
        //  many spectra. This is performed here. If any of ndAction, dispAxis
        //  or selectAxis are null then defaults will be used.
        SpecData[] moreSpectra = null;
        try {
            moreSpectra = specDataFactory.reprocessTo1D
                ( spectrum, ndAction, dispAxis, selectAxis,
                  purgeBadDataLimits );
        }
        catch (SplatException e) {
            JOptionPane.showMessageDialog
                ( this, e.getMessage(),
                  "Error converting 2D image into spectrum",
                  JOptionPane.ERROR_MESSAGE );

            //  Just use the vectorized form.
            moreSpectra = null;
        }
        if ( moreSpectra != null ) {
            //  Abandon the current spectrum and use these instead.
            colourAsLoaded=true;
            for ( int i = 0; i < moreSpectra.length; i++ ) {
                applyRenderingDefaults( moreSpectra[i] );
                globalList.add( moreSpectra[i], sourceType );
            }
        }
        else {
            applyRenderingDefaults( spectrum );
            globalList.add( spectrum, sourceType );
        }

        //  Latest list entries becomes selected.
        specList.setSelectionInterval( top, list.specCount() - 1 );
    }

    /**
     * Display all the spectra that are selected in the global list view.
     * Each spectrum is displayed in a new plot.
     */
    public void displaySelectedSpectra()
    {
        int[] indices = getSelectedSpectra();
        if ( indices != null ) {
            SpecData spec = null;
            PlotControlFrame plot = null;
            int offset = 50;
            Point lastLocation = new Point( offset, offset );
            for ( int i = 0; i < indices.length; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                plot = displaySpectrum( spec );
                if ( plot != null ) {
                    if ( i != 0 ) {
                        //  Displace Plot slightly so that windows do not
                        //  totally obscure each other, but keep some control.
                        lastLocation.translate( offset, offset );
                        plot.setLocation( lastLocation );
                    }
                    lastLocation = plot.getLocation();
                }
            }
        }
    }

    /**
     * Display all the currently selected spectra in the currently
     * selected plot, or create a new plot for them.
     *
     * @param fit whether to make all spectra fit the width and height
     *            of the plot
     * @return the index of the plot, if one is created, -1 otherwise.
     */
    public int multiDisplaySelectedSpectra( boolean fit )
    {
        int plotIndex = -1;

        boolean samePlotForSampSpectra = getPreference("SplatBrowser_plotsampspectratosamewindow", false);
        
        int[] specIndices = getSelectedSpectra();
        if ( specIndices == null ) {
            return plotIndex;
        }
        SplatException lastException = null;
        int failed = 0;
        final int[] plotIndices = getSelectedPlots();
        if ( plotIndices != null ) {
            //  Add all spectra in a single list for efficiency.
            SpecData spectra[] = new SpecData[specIndices.length];
            for ( int i = 0; i < specIndices.length; i++ ) {
                spectra[i] = globalList.getSpectrum( specIndices[i] );
            }
            for ( int j = 0; j < plotIndices.length; j++ ) {
                try {
                    globalList.addSpectra( plotIndices[j], spectra );
                }
                catch (SplatException e) {
                    failed++;
                    lastException = e;
                    plotIndices[j] = -1;
                }
            }
            if ( fit ) {
                //  Do fit to width and height after realisation.
                Runnable later = new Runnable() {
                        public void run()
                        {
                            GlobalSpecPlotList globalList =
                                GlobalSpecPlotList.getInstance();
                            for ( int j = 0; j < plotIndices.length; j++ ) {
                                if ( plotIndices[j] != -1 ) {
                                    globalList.getPlot( plotIndices[j] )
                                        .fitToWidthAndHeight( false );
                                }
                            }
                        }
                    };
                SwingUtilities.invokeLater( later );
            }
        }
        else {
            
            List<SpecData> allSelectedSpectra = new ArrayList<SpecData>();
            List<SpecData> sampSpectra = new ArrayList<SpecData>();
            
            for ( int i = 0; i < specIndices.length; i++ ) {
                SpecData spectrum = globalList.getSpectrum( specIndices[i] );
                SourceType sourceType = globalList.getSourceType(spectrum);

                switch(sourceType) {
                    case SAMP:
                        if (samePlotForSampSpectra)
                            sampSpectra.add(spectrum);
                        else
                            allSelectedSpectra.add(spectrum);
                        break;
                    default:
                        allSelectedSpectra.add(spectrum);
                }
                
            }

            //  Add all spectra in a single list for efficiency.
            /* SpecData spectra[] = new SpecData[specIndices.length - 1];
            for ( int i = 0; i < specIndices.length - 1; i++ ) {
                spectra[i] = globalList.getSpectrum( specIndices[i+1] );
            }*/
            
            if (allSelectedSpectra.size() > 0) {
                SpecData spec = null;
                //spec = globalList.getSpectrum( specIndices[0] );
                spec = allSelectedSpectra.get(0);
                
                final PlotControlFrame plot = displaySpectrum( spec );
                plotIndex = globalList.getPlotIndex( plot.getPlot() );
                
                allSelectedSpectra.remove(0);
                SpecData spectra[] = allSelectedSpectra.toArray(new SpecData[allSelectedSpectra.size()]);
    
                try {
                    globalList.addSpectra( plot.getPlot(), spectra );
                }
                catch (SplatException e) {
                    failed++;
                    lastException = e;
                }
                if ( fit && failed < specIndices.length ) {
                    //  Do fit to width and height after realisation.
                    Runnable later = new Runnable() {
                            public void run()
                            {
                                if ( plot != null ) {
                                    plot.getPlot().fitToWidthAndHeight( true );
                                }
                            }
                        };
                    SwingUtilities.invokeLater( later );
                }
            }
            
            // show spectra from SAMP to the same plot
            if (samePlotForSampSpectra && sampSpectra.size() > 0 && failed == 0) {
                boolean firstSampSpectra = globalList.getLastPlotForSourceType(SourceType.SAMP) == null;
                final PlotControl sampPlot = 
                        firstSampSpectra
                        ? displaySpectrum( sampSpectra.get(0) ).getPlot()
                        : globalList.getLastPlotForSourceType(SourceType.SAMP);
                
                if (firstSampSpectra)
                    sampSpectra.remove(0);
                try {
                    globalList.addSpectra( sampPlot, sampSpectra.toArray(new SpecData[sampSpectra.size()]) );
                }
                catch (SplatException e) {
                    failed++;
                    lastException = e;
                }
                if ( fit && failed < sampSpectra.size() ) {
                    //  Do fit to width and height after realisation.
                    Runnable later = new Runnable() {
                            public void run()
                            {
                                if ( sampPlot != null ) {
                                    sampPlot.fitToWidthAndHeight( true );
                                }
                            }
                    };
                    SwingUtilities.invokeLater( later );
                }
            }
        }
        if ( lastException != null ) {
            reportOpenListFailed( failed, lastException );
        }
        return plotIndex;
    }

    /**
     * Display a list of spectra given by their file names. The file
     * names are assumed to be in a space separated list stored in a
     * single String.
     *
     * @param list the list of spectra file names (or disk
     *             specifications).
     * @return the identifier of the plot that the spectra are
     *         displayed in, -1 if it fails to display or open any spectra.
     */
    public int displaySpectra( String list )
    {
        // Find out how many spectral names we have.
        StringTokenizer st = new StringTokenizer( list );
        int count = st.countTokens();
        if ( count == 0 ) return -1;

        //  Attempt to open each one and keep a list of their
        //  indices.
        int[] indices = new int[count];
        int openedCount = 0;
        SpecList specList = SpecList.getInstance();
        SpectrumIO.Props props[] = new SpectrumIO.Props[1];

        for ( int i = 0; i < count; i++ ) {
            props[0] = new SpectrumIO.Props( st.nextToken() );
            if ( blockedThreadLoadSpectra( props, false ) == 1 ) {
                indices[openedCount++] = specList.specCount() - 1;
            }
        }
        if ( openedCount == 0 ) return -1;

        //  Create a plot for our spectra and display them all.
        SpecData spec = globalList.getSpectrum( indices[0] );
        PlotControl plot = displaySpectrum( spec ).getPlot();

        if ( openedCount > 1 ) {
            SpecData[] spectra = new SpecData[openedCount-1];
            for ( int i = 0; i < openedCount - 1; i++ ) {
                spectra[i] = globalList.getSpectrum( indices[i+1] );
            }
            try {
                globalList.addSpectra( plot, spectra );
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, "Failed to display spectra", e );
                return -1;
            }
        }
        return plot.getIdentifier();
    }

    /**
     * Display a list of spectra given by their file names, in a plot
     * specified by its identifier. The file names are assumed to be in a
     * space separated list stored in a single String. If the plot doesn't
     * exist then it is created and the identifier of that plot is returned
     * (which will be different from the one requested).
     *
     * @param id the plot identifier number.
     * @param list the list of spectra file names (or disk specifications).
     * @param clear if true then an existing plot will have any displayed
     *              spectra removed.
     * @return the id of the plot that the spectra are displayed
     *         in, -1 if it fails to display.
     */
    public int displaySpectra( int id, String list, boolean clear )
    {
        // Find out how many spectral names we have.
        StringTokenizer st = new StringTokenizer( list );
        int count = st.countTokens();
        if ( count == 0 ) return -1;

        //  Attempt to open each one and keep a list of their
        //  indices.
        int[] indices = new int[count];
        int openedCount = 0;
        SpecList specList = SpecList.getInstance();
        SpectrumIO.Props props[] = new SpectrumIO.Props[1];

        for ( int i = 0; i < count; i++ ) {
            props[0] = new SpectrumIO.Props( st.nextToken() );
            if ( blockedThreadLoadSpectra( props, false ) == 1 ) {
                indices[openedCount++] = specList.specCount() - 1;
            }
        }
        if ( openedCount == 0 ) return -1;

        //  Attempt to access an existing plot with this index.
        int plotIndex = globalList.getPlotIndex( id );
        PlotControl plot = null;

        //  If no current PlotControl instance with this identifier then
        //  create a new one, using the first spectrum to initialise it
        //  (obviously clear has no effect).
        int start = 0;
        if ( plotIndex == -1 ) {
            SpecData spectrum = globalList.getSpectrum( indices[0] );
            start = 1;
            PlotControlFrame plotControlFrame = displaySpectrum(id, spectrum);
            plot = plotControlFrame.getPlot();
        }
        else {
            //  Use existing plot.
            plot = globalList.getPlot( plotIndex );

            //  Remove all existing spectra, if clear is true. Do this before
            //  adding so that there are no problems with matching the
            //  coordinate systems.
            if ( clear ) {
                SpecData[] dispSpec = plot.getPlot().getSpecDataComp().get();
                plot.getPlot().getSpecDataComp().remove( dispSpec );
            }
        }

        //  Display remaining spectra in the plot.
        SpecData spectra[] = new SpecData[openedCount-start];
        for ( int i = start; i < openedCount; i++ ) {
            spectra[i] = globalList.getSpectrum( indices[i] );
        }

        try {
            globalList.addSpectra( plot, spectra );
        }
        catch (SplatException e) {
            ErrorDialog.showError( this, "Failed to display spectra", e );
            return -1;
        }
        return id;
    }
    
    /**
     * Sets the current spectrum in global spectra list
     * (if just one spectrum is selected) and fires 
     * 'current spectrum changed' event
     */
    protected void fireCurrentSpectrumChanged() {
        int[] indexes = getSelectedSpectra();
        if (indexes != null) {
            if (indexes.length == 1) {
                globalList.setCurrentSpectrum(indexes[0]);
            }
        }
    }
    
    /**
     * Make a report using an ErrorDialog for when loading a list
     * of spectra has failed for some reason.
     */
    private void reportOpenListFailed( int failed,
                                       SplatException lastException )
    {
        String message = null;
        if ( failed == 1 ) {
            message = "Failed to display a spectrum";
        }
        else {
            message = "Failed to display " + failed + " spectra ";
        }
        ErrorDialog.showError( this, message, lastException );
    }

    /**
     * Display a tool for selecting from the global list of spectra
     * and then repeatable displaying the selected sequence. This is
     * meant to allow the browsing of a series of spectra, or simulate
     * blink comparison sequence.
     */
    public void animateSelectedSpectra()
    {
        if ( animatorFrame == null ) {
            animatorFrame = new SpecAnimatorFrame( this );

            //  We'd like to know if the window is closed.
            animatorFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        animatorClosed();
                    }
                });
        }
        else {
            Utilities.raiseFrame( animatorFrame );
        }
    }

    /**
     * Animator window is closed.
     */
    protected void animatorClosed()
    {
        // Nullify if method for closing switches to dispose.
        // animatorFrame = null;
    }

    /**
     * Display windows for viewing and possibly modifying the values
     * of the currently selected spectra as formatted numbers (uses a
     * JTable).
     */
    public void viewSelectedSpectra()
    {
        if ( specViewerFrames == null ) {
            specViewerFrames = new ArrayList();
        }

        // Get the selected spectra.
        int[] indices = getSelectedSpectra();
        if ( indices != null ) {

            //  And create view of each one.
            SpecData spec = null;
            for ( int i = 0; i < indices.length; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                specViewerFrames.add( new SpecViewerFrame( spec ) );
            }
        }
    }

    /**
     * Display a window for viewing and possibly modifying the spectral
     * coordinates.
     */
    public void viewSpecCoordinates()
    {
        if ( coordinatesFrame == null ) {
            coordinatesFrame = new SpecCoordinatesFrame( specList );

            //  We'd like to know if the window is closed.
            coordinatesFrame.addWindowListener( new WindowAdapter()
            {
                public void windowClosed( WindowEvent evt )
                {
                    specCoordinatesClosed();
                }
            });
        }
        else {
            Utilities.raiseFrame( coordinatesFrame );
            coordinatesFrame.setSelectionFrom( specList );
        }
    }

    /**
     * Window for viewing and modifying the spectral coordinates is closed.
     */
    protected void specCoordinatesClosed()
    {
        // Nullify if method for closing switches to dispose.
        // coordinatesFrame = null;
    }

    /**
     * Display a window for viewing and possibly modifying the data units.
     */
    public void viewDataUnits()
    {
        if ( dataUnitsFrame == null ) {
            dataUnitsFrame = new SpecDataUnitsFrame( specList );

            //  We'd like to know if the window is closed.
            dataUnitsFrame.addWindowListener( new WindowAdapter()
            {
                public void windowClosed( WindowEvent evt )
                {
                    dataUnitsClosed();
                }
            });
        }
        else {
            Utilities.raiseFrame( dataUnitsFrame );
            dataUnitsFrame.setSelectionFrom( specList );
        }
    }

    /**
     * Window for viewing and modifying the data units is closed.
     */
    protected void dataUnitsClosed()
    {
        // Nullify if method for closing switches to dispose.
        // dataUnitsFrame = null;
    }

    /**
     * Display windows for viewing the FITS headers, if any, of the currently
     * selected spectra.
     */
    public void fitsSelectedSpectra()
    {
        if ( fitsViewerFrames == null ) {
            fitsViewerFrames = new ArrayList();
        }

        // Get the selected spectra.
        int[] indices = getSelectedSpectra();
        if ( indices != null ) {

            //  And create view of each one.
            SpecData spec = null;
            for ( int i = 0; i < indices.length; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                fitsViewerFrames.add( new FITSHeaderFrame( spec ) );
            }
        }
    }

    /**
     * Remove the currently selected spectra from the global list and
     * this interface.
     */
    public void removeSelectedSpectra()
    {
        int[] indices = getSelectedSpectra();
        if ( indices != null ) {

            //  Do in reverse to avoid changes of index.
            for ( int i = indices.length - 1; i >= 0; i-- ) {
                globalList.removeSpectrum( indices[i] );
            }

            //  Try to keep the selection next to the "current" position.
            //  Make this the index nearest to the one at the bottom of the
            //  selection.
            int selbot = indices[0];
            int nspec = specList.getModel().getSize() - 1;
            int selection = Math.min( nspec, selbot );
            globalList.setCurrentSpectrum( selection );
        }
    }

    /**
     * Add and display a new spectrum in a new plot.
     *
     * @param spectrum the name (i.e. file specification) of the spectrum
     *                 to add and display.
     *
     * @return the plot that the spectrum is displayed in.
     */
    public PlotControlFrame displaySpectrum( String spectrum )
    {
        if ( addSpectrum( spectrum ) ) {
            SpecData specData =
                globalList.getSpectrum( globalList.specCount() - 1 );
            return displaySpectrum( specData );
        }
        return null;
    }

    /**
     * Display a spectrum in a new plot.
     *
     * @param spectrum The spectrum to display.
     * @return the plot that the spectrum is displayed in.
     */
    public PlotControlFrame displaySpectrum( SpecData spectrum )
    {
        return displaySpectrum( -1, spectrum );
    }

    /**
     * Display a spectrum in a new plot.
     *
     * @param id plot identifier, -1 for automatic value.
     * @param spectrum The spectrum to display.
     * @return the plot that the spectrum is displayed in.
     */
    public PlotControlFrame displaySpectrum( int id, SpecData spectrum )
    {
        // Set the cursor to wait while the plot is created.
        setWaitCursor();

        SpecDataComp comp = new SpecDataComp( spectrum );
        PlotControlFrame plot = null;
        try {
            plot = new PlotControlFrame( comp, id );
            int index = globalList.add( plot.getPlot() );
            plot.setTitle( Utilities.getReleaseName() + ": " +
                           globalList.getPlotName( index ) );

            //  We'd like to know if the plot window is closed.
            plot.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        removePlot( (PlotControlFrame) evt.getWindow() );
                    }
                });

            //  Reposition the synopsis, after everything has settled.
            final PlotControl plotControl = plot.getPlot();
            SwingUtilities.invokeLater( new Runnable() {
                    public void run()
                    {
                        plotControl.positionSynopsisAnchor();
                        plotControl.updateSynopsis();
                    }
                });
        }
        catch ( SplatException e ) {
            JOptionPane.showMessageDialog( this, e.getMessage(),
                                           "Error creating plot",
                                           JOptionPane.ERROR_MESSAGE );
        }
        catch ( OutOfMemoryError memErr ) {
            //  If we run out of memory during the Plot create, try to make
            //  sure the plot isn't shown.
            JOptionPane.showMessageDialog( this, memErr.getMessage(),
                                           "Error creating plot",
                                           JOptionPane.ERROR_MESSAGE );
            if ( plot != null ) {
                plot.setVisible( false );
            }
            plot = null;
        }
        finally {
            //  Always make sure we reset the cursor.
            resetWaitCursor();
        }
        return plot;
    }

    /**
     * Display a range of spectra, currently shown in the global list, in the
     * selected plot, or a new one if none are selected.
     *
     * @param lower the index of the first spectrum to display.
     * @param upper the index of the last spectrum to display.
     *
     * @return index of the plot if one is created, otherwise -1.
     */
    public int displayRange( int lower, int upper )
    {
        int index = -1;
        if ( lower <= upper ) {
            //  Get the current selection, this is restored later.
            int[] currentSelection = getSelectedSpectra();

            specList.setSelectionInterval( lower, upper );
            index = multiDisplaySelectedSpectra( true );
            if ( currentSelection != null ) {
                specList.setSelectedIndices( currentSelection );
            }
        }
        return index;
    }

    /**
     * Make copies of all the selected spectra. These are memory copies. If
     * sort is true then the coordinates are sorted into increasing order and
     * any duplicates are removed.
     */
    public void copySelectedSpectra( boolean sort )
    {
        int[] indices = getSelectedSpectra();
        if ( indices != null ) {
            SpecData spec = null;
            EditableSpecData newSpec = null;
            String name = null;
            for ( int i = 0; i < indices.length; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                name = "Copy of: " + spec.getShortName();
                try {
                    newSpec = specDataFactory.createEditable(name, spec, sort);
                    globalList.add( newSpec );
                }
                catch (Exception e) {
                    logger.log( Level.INFO, e.getMessage(), e );
                }
            }
        }
    }

    /**
     * Re-open the selected spectra. This should cause any changed contents to
     * be propagated into any plots etc. This may not be possible if any of
     * the spectra are not backed by a local disk file.
     */
    public void reOpenSelectedSpectra()
    {
        int[] indices = getSelectedSpectra();
        if ( indices != null ) {
            SpecData spec = null;
            for ( int i = 0; i < indices.length; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                try {
                    specDataFactory.reOpen( spec );
                    globalList.notifySpecListenersModified( spec );
                }
                catch (SplatException e) {
                    ErrorDialog.showError( this, e );
                }
            }
        }
    }

    /**
     * Remove a plot from the global list.
     *
     * @param plot reference to the frame (i.e. window) containing the
     *             plot (these have a one to one relationship).
     */
    public void removePlot( PlotControlFrame plot )
    {
        globalList.remove( plot.getPlot() );
    }

    /**
     * Remove the currently selected plots from the global list and
     * this interface.
     */
    public void removeSelectedPlots()
    {
        int[] indices = getSelectedPlots();
        if ( indices != null ) {

            //  Do in reverse to avoid changes of index.
            PlotControlFrame frame = null;
            PlotControl control = null;
            for ( int i = indices.length - 1; i >= 0; i-- ) {
                control = globalList.getPlot( indices[i] );
                frame = (PlotControlFrame)
                    SwingUtilities.getWindowAncestor( control );
                frame.closeWindow();
            }
        }
    }

    /**
     * Select all the plots.
     */
    public void selectAllPlots()
    {
        int nplots = globalList.plotCount();
        if ( nplots > 0 ) {
            deSelectAllPlots();
            plotTable.addSelectionInterval( 0, nplots  - 1 );
        }
    }

    /**
     * Deselect all the plots.
     */
    public void deSelectAllPlots()
    {
        plotTable.clearSelection();
    }

    /**
     * Get a list of all plots current selected in the plot table.
     *
     * @return list of indices of selected plot, otherwise null.
     */
    public int[] getSelectedPlots()
    {
        return plotTable.getSelectedIndices();
    }

    /**
     * Select all the spectra.
     */
    public void selectAllSpectra()
    {
        deSelectAllSpectra();
        specList.addSelectionInterval
            ( 0, specList.getModel().getSize() - 1 );
    }

    /**
     * Deselect all the spectra.
     */
    public void deSelectAllSpectra()
    {
        specList.clearSelection();
    }

    /**
     * Get a list of all spectra current selected in the global list.
     *
     * @return list of indices of selected spectra, otherwise null.
     */
    public int[] getSelectedSpectra()
    {
        if ( specList.getModel().getSize() > 0 ) {
            int[] indices = specList.getSelectedIndices();
            if ( indices.length == 0 ) {
                return null;
            }
            return indices;
        }
        return null;
    }

    /**
     * Save a spectrum to disk file.
     *
     * @param globalIndex the index on the global list of the spectrum
     *                    to save.
     * @param spectrum the file to write the spectrum into.
     */
    public void saveSpectrum( int globalIndex, String spectrum )
    {
        SpecData source = globalList.getSpectrum( globalIndex );
        try {
            SpecData target =
                specDataFactory.getClone( source, spectrum, saveUsertypeIndex,
                   (String) saveTabletypeBox.getItemAt( saveTabletypeIndex ) );
            target.save();
            globalList.add( target );
        }
        catch ( SplatException e ) {
            logger.log( Level.INFO, e.getMessage(), e );
            ErrorDialog.showError( this, e.getMessage(), e );
        }
    }

    /**
     * Show window for performing simple binary mathematical
     * operations between spectra.
     */
    public void showBinaryMathsWindow()
    {
        if ( binaryMathsFrame == null ) {
            binaryMathsFrame = new SimpleBinaryMaths();

            //  We'd like to know if the window is closed.
            binaryMathsFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        binaryMathsWindowClosed();
                    }
                });
        }
        else {
            Utilities.raiseFrame( binaryMathsFrame );
        }
    }

    /**
     * Binary maths window is closed.
     */
    protected void binaryMathsWindowClosed()
    {
        // Nullify if method for closing switches to dispose.
        // binaryMathsFrame = null;
    }

    /**
     * Show window for performing simple unary mathematical
     * operations on a spectrum.
     */
    public void showUnaryMathsWindow()
    {
        if ( unaryMathsFrame == null ) {
            unaryMathsFrame = new SimpleUnaryMaths();

            //  We'd like to know if the window is closed.
            unaryMathsFrame.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        unaryMathsWindowClosed();
                    }
                });
        }
        else {
            Utilities.raiseFrame( unaryMathsFrame );
        }
    }

    /**
     * Unary maths window is closed.
     */
    protected void unaryMathsWindowClosed()
    {
        // Nullify if method for closing switches to dispose.
        // unaryMathsFrame = null;
    }

    /**
     * Create a new spectrum with a number of elements (greater than 2)
     * obtained interactively.
     */
    public void createSpectrum()
    {
        Number number =
            DecimalDialog.showDialog( this, "Create new spectrum",
                                      "Number of rows in spectrum",
                                      new ScientificFormat(),
                                      new Integer( 100 ) );
        if ( number != null ) {
            int nrows = number.intValue();
            if ( nrows > 0 ) {
                if ( nrows < 2 ) nrows = 2;
                EditableSpecData newSpec = null;
                String name = "Blank spectrum";
                try {
                    newSpec = specDataFactory.createEditable( name );

                    //  Add the coords and data.
                    double[] coords = new double[nrows];
                    double[] data = new double[nrows];
                    Arrays.fill( data, SpecData.BAD );
                    for ( int i = 0; i < nrows; i++ ) {
                        coords[i] = (double) i + 1;
                    }
                    newSpec.setSimpleDataQuick( coords, null, data );
                    globalList.add( newSpec );
               }
                catch (Exception e) {
                    ErrorDialog.showError( this, e );
                    return;
                }
            }
        }
    }

    /**
     * Set whether the application should behave as embedded.
     */
    public void setEmbedded( boolean embedded )
    {
        this.embedded = embedded;
    }


    /**
     * Remove all spectra that cannot be autoranged from the global
     * list. These are usually extracted spectra that have invalid
     * coordinates.
     */
    protected void purgeSpectra()
    {
        double[] range = null;
        int size = globalList.specCount();

        //  Tranverse in reverse.
        for ( int i = size - 1; i >= 0; i-- ) {
            range = globalList.getSpectrum( i ).getRange();
            if ( range[0] == SpecData.BAD || range[1] == SpecData.BAD ||
                 range[2] == SpecData.BAD || range[3] == SpecData.BAD ) {
                globalList.removeSpectrum( i );
            }
        }
    }

    /**
     * Apply the defaults for rendering spectra, also set the colour if
     * colourising automatically.
     */
    protected void applyRenderingDefaults( SpecData spectrum )
    {
        selectedProperties.applyRenderingProps( spectrum );
        if ( colourAsLoaded ) {
            spectrum.setLineColour( MathUtils.getRandomRGB());
        }
    }

    /**
     * A request to exit the application has been received. Only do
     * this if we're not embedded. In that case just make the window
     * iconized.
     */
    protected void exitApplicationEvent()
    {
        if ( embedded ) {
            setExtendedState( JFrame.ICONIFIED );
        }
        else {
            Utilities.saveFrameLocation( this, prefs, "SplatBrowser" );
            System.exit( 0 );
        }
    }

    /**
     * Exit application when window is closed.
     *
     * @param e WindowEvent
     */
    protected void processWindowEvent( WindowEvent e )
    {
        super.processWindowEvent( e );
        if ( e.getID() == WindowEvent.WINDOW_CLOSING ) {
            exitApplicationEvent();
        }
    }

    /**
     * Class for defining all local Actions. There are a lot of these
     * so use a wrapper to keep code size down and limit number of
     * actual classes.
     */
    protected class LocalAction extends AbstractAction
    {
        public static final int SAVE = 0;
        public static final int OPEN = 1;
        public static final int LOCATION = 2;
        public static final int SSAP = 3;
        public static final int BROWSE = 4;
        public static final int REOPEN = 5;
        public static final int SINGLE_DISPLAY = 6;
        public static final int MULTI_DISPLAY = 7;
        public static final int ANIMATE_DISPLAY = 8;
        public static final int SPEC_VIEWER = 9;
        public static final int SPECCOORDS_VIEWER = 10;
        public static final int DATAUNITS_VIEWER = 11;
        public static final int SAVE_STACK = 12;
        public static final int READ_STACK = 13;
        public static final int REMOVE_SPECTRA = 14;
        public static final int SELECT_SPECTRA = 15;
        public static final int DESELECT_SPECTRA = 16;
        public static final int COLOURIZE = 17;
        public static final int REMOVE_PLOTS = 18;
        public static final int SELECT_PLOTS = 19;
        public static final int DESELECT_PLOTS = 20;
        public static final int BINARY_MATHS = 21;
        public static final int UNARY_MATHS = 22;
        public static final int COPY_SPECTRA = 23;
        public static final int COPYSORT_SPECTRA = 24;
        public static final int CREATE_SPECTRUM = 25;
        public static final int PURGE_SPECTRA = 26;
        public static final int FITS_VIEWER = 27;
        public static final int EXIT = 28;
        public static final int OBSCORE = 29;

        private int type = 0;

        /**
         * Create a Action.
         *
         * @param type the type of local action (see above).
         * @param name simple name for the action (appears in labels).
         * @param icon an icon for for the action (can be null,
         *             appears in all labels).
         * @param help the tooltip help for any labels (can be null).
         */
        public LocalAction( int type, String name, Icon icon, String help )
        {
            super( name, icon );
            this.type = type;
            putValue( SHORT_DESCRIPTION, help );
        }

        /**
         * Create a Action.
         *
         * @param type the type of local action (see above).
         * @param name simple name for the action (appears in labels).
         * @param icon an icon for for the action (can be null,
         *             appears in all labels).
         * @param help the tooltip help for any labels (can be null).
         * @param accel accelerator key description (string for
         *              {@link KeyStroke.getKeyStroke(String)} call).
         *              Accelerator invokes Action immediately.
         */
        public LocalAction( int type, String name, Icon icon, String help,
                            String accel )
        {
            super( name, icon );
            this.type = type;
            putValue( SHORT_DESCRIPTION, help );
            KeyStroke k = KeyStroke.getKeyStroke( accel );
            if ( k != null ) {
                putValue( ACCELERATOR_KEY, k );
            }
        }

        public void actionPerformed( ActionEvent ae )
        {
            switch ( type ) {
               case SAVE: {
                   showSaveFileChooser();
               }
               break;

               case OPEN: {
                   showOpenFileChooser();
               }
               break;

               case LOCATION: {
                   showLocationChooser();
               }
               break;

               case SSAP: {
                   showSSAPBrowser();
               }
               break;

               case BROWSE: {
                   showSplatNodeChooser();
               }
               break;

               case REOPEN: {
                   reOpenSelectedSpectra();
               }
               break;

               case SINGLE_DISPLAY: {
                   displaySelectedSpectra();
               }
               break;

               case MULTI_DISPLAY: {
                   multiDisplaySelectedSpectra( true );
               }
               break;

               case ANIMATE_DISPLAY: {
                   animateSelectedSpectra();
               }
               break;

               case SPEC_VIEWER: {
                   viewSelectedSpectra();
               }
               break;

               case SPECCOORDS_VIEWER: {
                   viewSpecCoordinates();
               }
               break;

               case DATAUNITS_VIEWER: {
                   viewDataUnits();
               }
               break;

               case SAVE_STACK: {
                   saveStackEvent();
               }
               break;

               case READ_STACK: {
                   readStackEvent();
               }
               break;

               case REMOVE_SPECTRA: {
                   removeSelectedSpectra();
               }
               break;

               case SELECT_SPECTRA: {
                   selectAllSpectra();
               }
               break;

               case DESELECT_SPECTRA: {
                   deSelectAllSpectra();
               }
               break;

               case COLOURIZE: {
                   colourizeSpectra();
               }
               break;

               case REMOVE_PLOTS: {
                   removeSelectedPlots();
               }
               break;

               case SELECT_PLOTS: {
                   selectAllPlots();
               }
               break;

               case DESELECT_PLOTS: {
                   deSelectAllPlots();
               }
               break;

               case BINARY_MATHS: {
                   showBinaryMathsWindow();
               }
               break;

               case UNARY_MATHS: {
                   showUnaryMathsWindow();
               }
               break;

               case COPY_SPECTRA: {
                   copySelectedSpectra( false );
               }
               break;

               case COPYSORT_SPECTRA: {
                   copySelectedSpectra( true );
               }
               break;

               case CREATE_SPECTRUM: {
                   createSpectrum();
               }
               break;

               case PURGE_SPECTRA: {
                   purgeSpectra();
               }
               break;

               case FITS_VIEWER: {
                   fitsSelectedSpectra();
               }
               break;
               
               case OBSCORE: {
                   showObscorePanel();
                   break;
               }

               case EXIT: {
                   exitApplicationEvent();
               }
               break;
            }
        }
    }

    //
    // Implement ItemListener interface. This is used for menus items
    // that do not require the full capabilities of an Action.
    //
    public void itemStateChanged( ItemEvent e )
    {
        Object source = e.getSource();
        if ( source.equals( splitOrientation ) ) {
            setSplitOrientation( false );
        }
        else if ( source.equals( colourAsLoadedItem ) ) {
            setColourAsLoaded( false );
        }
        else if ( source.equals( showShortNamesItem ) ) {
            setShowShortNames( false );
        }
        else if ( source.equals( showSimpleShortNamesItem ) ) {
            setShowSimpleShortNames( false );
        }
        else if ( source.equals( purgeBadDataLimitsItem ) ) {
            setPurgeBadDataLimits( false );
        }
        else if ( source.equals( searchCoordsItem ) ) {
            setSearchCoords( false );
        }
        else if ( source.equals( plotSampSpectraToSameWindowItem ) ) {
            setPlotSampSpectraToSameWindow( false );
        }
    }

    //
    // Implement ActionListener interface. This is used for menus items
    // that do not require the full capabilities of an Action.
    //
    public void actionPerformed( ActionEvent e )
    {
        // The cascade request.
        PlotWindowOrganizer organizer = new PlotWindowOrganizer();
        organizer.cascade();
    }


    //  Simple class to contain an object, but return a different value when
    //  queried using toString. Use use when you want to put values into a
    //  JComboBox, but have another representation shown.
    private class KeyValue
    {
        public KeyValue( String key, Object value )
        {
            this.key = key;
            this.value = value;
        }
        private String key = null;
        private Object value = null;

        public String getKey()
        {
            return key;
        }
        public Object getValue()
        {
            return value;
        }
        public String toString()
        {
            return key;
        }
    }
    
    
    /**
     * Determines the selected FileFormat from FileChooser
     * @param fileChooser BasicFileChooser containing the FileFilter
     * @param defaultFileFormat FileFormat that will returned if the selected one cannot be determined
     * @return Selected FileFormat
     */
    private SpecList.FileFormat getFileFormat(BasicFileChooser fileChooser, SpecList.FileFormat defaultFileFormat) {
        SpecList.FileFormat fileFormat = defaultFileFormat;
        for (SpecList.FileFormat ff : SpecList.FileFormat.values()) {
            if (fileChooser.getFileFilter().getDescription().startsWith(ff.getDescription())) {
                fileFormat = ff;
                break;
            }
        }
        
        return fileFormat;
    }
    
    /**
     * Determines the selected FileFormat from FileChooser
     * @param fileChooser BasicFileChooser containing the FileFilter
     * @param defaultFileFormat FileFormat that will returned if the selected one cannot be determined
     * @param useExtensionGuessing If FileFormat cannot be determined directly, try to determine it 
     *      by guessing it based on selected File's extension
     * @return Selected FileFormat
     */
    private SpecList.FileFormat getFileFormat(
            BasicFileChooser fileChooser, 
            SpecList.FileFormat defaultFileFormat, 
            boolean useExtensionGuessing) {
        
        SpecList.FileFormat fileFormat = defaultFileFormat;
        
        SpecList.FileFormat detectedFileFormat = getFileFormat(fileChooser, null);
        
        if (detectedFileFormat == null && useExtensionGuessing) {
            String fileName = fileChooser.getSelectedFile().getName();
            String selectedFileExtension = "";
            if (fileName != null) {
                selectedFileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
                if (selectedFileExtension != null) {
                    selectedFileExtension = selectedFileExtension.trim().toLowerCase();
                }
            }
            
            for (SpecList.FileFormat ff : SpecList.FileFormat.values()) {
                if (selectedFileExtension.equals(ff.getFileExtension().trim().toLowerCase())) {
                    detectedFileFormat = ff;
                    break;
                }
            }
        }
        
        if (detectedFileFormat != null)
            fileFormat = detectedFileFormat;
        
        for (SpecList.FileFormat ff : SpecList.FileFormat.values()) {
            if (fileChooser.getFileFilter().getDescription().startsWith(ff.getDescription())) {
                fileFormat = ff;
                break;
            }
        }
        
        return fileFormat;
    }
}
