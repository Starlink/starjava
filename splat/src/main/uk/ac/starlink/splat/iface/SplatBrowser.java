/*
 * Copyright (C) 2000-2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     25-SEP-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
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
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

import uk.ac.starlink.splat.data.EditableSpecData;
import uk.ac.starlink.splat.data.SpecData;
import uk.ac.starlink.splat.data.SpecDataComp;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.data.SpecDataImpl;
import uk.ac.starlink.splat.data.SpecList;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.plot.DivaPlot;
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.ExceptionDialog;
import uk.ac.starlink.splat.util.RemoteServer;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.SplatSOAPServer;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.util.TreeviewAccess;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;
import uk.ac.starlink.util.gui.GridBagLayouter;

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
        new Rectangle( 10, 10, 550, 450 );

    /**
     *  Content pane of JFrame and its layout manager.
     */
    protected JPanel contentPane;
    protected BorderLayout mainLayout = new BorderLayout();

    /**
     *  Main menubar and various menus.
     */
    protected JMenuBar menuBar = new JMenuBar();
    protected JMenu themeMenu = null;

    /**
     *  Toolbar and contents.
     */
    protected JPanel toolBarContainer = new JPanel();
    protected JToolBar toolBar = new JToolBar();

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
     * Whither orientation of split
     */
    protected JCheckBoxMenuItem splitOrientation = null;

    /**
     *  Open or save file chooser.
     */
    protected BasicFileChooser fileChooser = null;

    /**
     *  Names of files that are passed to other threads for loading.
     */
    protected File[] newFiles = null;

    /**
     *  Whether files loaded in other threads should also be displayed
     *  (in a single new plot).
     */
    protected boolean displayNewFiles = false;

    /**
     *  Stack open or save chooser.
     */
    protected BasicFileChooser stackChooser = null;

    /**
     *  Progress monitor for startup and loading files.
     */
    protected int progressValue;
    protected int progressMaximum;
    protected ProgressMonitor progressMonitor = null;

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
     * X axis coordinate type viewer frame.
     */
    protected SpecXCoordTypeFrame xCoordTypeFrame = null;

    /**
     * Whether the application is embedded. In this case application
     * exit is assumed controlled by the embedding app.
     */
    protected boolean embedded = false;

    /**
     *  Create a browser with no existing spectra.
     */
    public SplatBrowser()
    {
        this( null, false );
    }

    /**
     *  Create a browser with no existing spectra that may be suitable
     *  for embedding (not remote control, exit disabled).
     */
    public SplatBrowser( boolean embedded )
    {
        this( null, embedded );
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
        this( inspec, false );
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
        //  Webstart bug: http://developer.java.sun.com/developer/bugParade/bugs/4665132.html
        //  Don't know where to put this though.
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());

        setEmbedded( embedded );
        enableEvents( AWTEvent.WINDOW_EVENT_MASK );
        try {
            initComponents();
        }
        catch ( Exception e ) {
            e.printStackTrace();
            return;
        }

        //  Now add any command-line spectra. Do this after the
        //  interface is visible and in a separate thread from
        //  the GUI and event queue. Note this may cause the GUI to be
        //  realized, so any additional work must be done on the event
        //  queue.
        if ( inspec != null ) {
            newFiles = new File[inspec.length];
            for ( int i = 0; i < inspec.length; i++ ) {
                newFiles[i] = new File( inspec[i] );
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
            // Make sure we start the remote services, but avoid
            // contention with image loading by also doing this as
            // above when there are files to be loaded.
            SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        threadInitRemoteServices();
                    }
                });
        }
    }

    /**
     *  Initialise all visual components.
     */
    protected void initComponents()
    {
        //  Set up the content pane and window size.
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout( mainLayout );
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

        //  Double click on item(s) to display new plots.
        specList.addMouseListener( new MouseAdapter() {
                public void mouseClicked( MouseEvent e ) {
                    if ( e.getClickCount() >= 2 ) {
                        displaySelectedSpectra();
                    }
                }
            });

        //  Drag to a plot to display?
        specList.setDragEnabled( true );
        specList.setTransferHandler( new SpecTransferHandler() );

        //  Set up the control area.
        controlArea.setLayout( controlAreaLayout );
        controlArea.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );
        controlScroller.getViewport().add( controlArea, null );

        //  Set up split pane.
        splitPane.setOneTouchExpandable( true );
        setSplitOrientation( true );
        specList.setSize( new Dimension( 190, 0 ) );
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
     * Initialise the startup progress monitor.
     *
     * @param intervals the number of intervals (i.e. calls to
     *                   updateProgressMonitor) expected before action
     *                   is complete.
     * @param title the title for the monitor window.
     * @see #updateProgressMonitor
     */
    protected void initProgressMonitor( int intervals, String title )
    {
        closeProgressMonitor();
        progressValue = 0;
        progressMaximum = intervals - 1;
        progressMonitor = new ProgressMonitor( this, title, "", 0,
                                               progressMaximum );
        progressMonitor.setMillisToDecideToPopup( 500 );
        progressMonitor.setMillisToPopup( 250 );
    }

    /**
     *  Update the progress monitor.
     *
     *  @param note note to show in the progress monitor dialog.
     *
     *  @see #initProgressMonitor
     */
    protected void updateProgressMonitor( String note )
    {
        progressMonitor.setProgress( ++progressValue );
        progressMonitor.setNote( note );
    }

    /**
     *  Close the progress monitor.
     *
     *  @see #initProgressMonitor
     */
    protected void closeProgressMonitor()
    {
        if ( progressMonitor != null ) {
            progressMonitor.close();
            progressMonitor = null;
        }
    }

    /**
     *  Setup the menus and toolbar.
     */
    protected void setupMenusAndToolbar()
    {
        //  Add the menuBar.
        this.setJMenuBar( menuBar );

        //  Add the toolbar to a container. Need extra component so we can
        //  reorient with the split pane.
        toolBarContainer.setLayout( new BorderLayout() );
        toolBarContainer.add( toolBar, BorderLayout.CENTER );

        //  Would be nice to have something that wrapped/scrolled when the
        //  toolbar doesn't fit (scrollbar eats into toolbar space, never got
        //  wrapping to work).
        //JScrollPane pane = new JScrollPane( toolBar );
        //toolBarContainer.add( pane, BorderLayout.CENTER );

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

        //  Help menu.
        createHelpMenu();
    }

    /**
     * Create the File menu and populate it with appropriate actions.
     */
    protected void createFileMenu()
    {
        JMenu fileMenu = new JMenu( "File" );
        menuBar.add( fileMenu );

        //  Add action to open a list of spectrum stored in files.
        ImageIcon openImage = 
            new ImageIcon( ImageHolder.class.getResource( "openfile.gif" ) );
        LocalAction openAction  = new LocalAction( LocalAction.OPEN,
                                                   "Open", openImage,
                                                   "Open spectra" );
        fileMenu.add( openAction );
        toolBar.add( openAction );

        //  Add action to browse the local file system and look for tables
        //  etc. in sub-components.
        ImageIcon browseImage = 
            new ImageIcon( ImageHolder.class.getResource( "browse.gif" ) );
            javax.swing.plaf.metal.MetalIconFactory.getTreeControlIcon(true);

        LocalAction browseAction  = new LocalAction( LocalAction.BROWSE,
                                                     "Browse", browseImage,
                                                     "Browse for spectra" );
        fileMenu.add( browseAction );
        toolBar.add( browseAction );

        //  Add action to save a spectrum
        ImageIcon saveImage = 
            new ImageIcon( ImageHolder.class.getResource( "savefile.gif" ) );
        LocalAction saveAction  =
            new LocalAction( LocalAction.SAVE, "Save", saveImage,
                             "Save a spectrum to disk file" );
        fileMenu.add( saveAction );
        toolBar.add( saveAction );

        //  Add an action to read in a stack of spectra.
        ImageIcon readStackImage = 
            new ImageIcon( ImageHolder.class.getResource( "readstack.gif" ) );
        LocalAction readStackAction =
            new LocalAction( LocalAction.READ_STACK, "Read stack",
                             readStackImage,
                             "Read back spectra stored in a disk file");
        fileMenu.add( readStackAction );
        toolBar.add( readStackAction );

        //  Add an action to save the stack of spectra.
        ImageIcon saveStackImage = 
            new ImageIcon( ImageHolder.class.getResource( "savestack.gif" ) );
        LocalAction saveStackAction =
            new LocalAction( LocalAction.SAVE_STACK, "Save stack",
                             saveStackImage, "Save all spectra to disk file" );
        fileMenu.add( saveStackAction );
        toolBar.add( saveStackAction );

        //  Add an action to exit application.
        ImageIcon exitImage = 
            new ImageIcon( ImageHolder.class.getResource( "exit.gif" ) );
        LocalAction exitAction = new LocalAction( LocalAction.EXIT,
                                                  "Exit", exitImage,
                                                  "Exit program" );
        fileMenu.add( exitAction );
    }

    /**
     * Create the Edit menu and populate it with appropriate actions.
     */
    protected void createEditMenu()
    {
        JMenu editMenu = new JMenu( "Edit" );
        menuBar.add( editMenu );

        //  Add an action to remove the selected spectra.
        ImageIcon removeImage =
            new ImageIcon( ImageHolder.class.getResource( "remove.gif" ) );
        LocalAction removeSpectraAction =
            new LocalAction( LocalAction.REMOVE_SPECTRA,
                             "Remove selected spectra", removeImage,
                             "Close any spectra selected in global list" );
        editMenu.add( removeSpectraAction );
        toolBar.add( removeSpectraAction );

        //  Add an action to select all spectra.
        LocalAction selectSpectraAction =
            new LocalAction( LocalAction.SELECT_SPECTRA,
                             "Select all spectra", null,
                             "Select all spectra in list" );
        editMenu.add( selectSpectraAction );

        //  Add an action to deselect all spectra.
        LocalAction deSelectSpectraAction =
            new LocalAction( LocalAction.DESELECT_SPECTRA,
                             "Deselect all spectra", null,
                             "Deselect any spectra selected in list" );
        editMenu.add( deSelectSpectraAction );

        //  Add an action to remove the selected plots.
        ImageIcon removePlotImage =
            new ImageIcon( ImageHolder.class.getResource( "removeplot.gif" ) );
        LocalAction removePlotAction =
            new LocalAction( LocalAction.REMOVE_PLOTS,
                             "Remove selected plots", removePlotImage,
                             "Close any plots selected in views list" );
        editMenu.add( removePlotAction );

        //  Add an action to select all plots.
        LocalAction selectPlotAction =
            new LocalAction( LocalAction.SELECT_PLOTS,
                             "Select all plots", null,
                             "Select all plots in list" );
        editMenu.add( selectPlotAction );

        //  Add an action to deselect all plots.
        LocalAction deSelectPlotAction =
            new LocalAction( LocalAction.DESELECT_PLOTS,
                             "Deselect all plots", null,
                             "Deselect any plots selected in list" );
        editMenu.add( deSelectPlotAction );

        //  Add an action to copy all selected spectra. This makes
        //  memory copies.
        LocalAction copySelectedSpectraAction =
            new LocalAction( LocalAction.COPY_SPECTRA,
                             "Copy selected spectra", null,
                             "Make memory copies of all selected spectra" );
        editMenu.add( copySelectedSpectraAction );

        //  Add an action to copy and sort all selected spectra. This makes
        //  memory copies.
        LocalAction copySortSelectedSpectraAction =
            new LocalAction( LocalAction.COPYSORT_SPECTRA,
                             "Copy and sort selected spectra", null,
                             "Make memory copies of all selected spectra " +
                             "and sort their coordinates if necessary" );
        editMenu.add( copySortSelectedSpectraAction );

        //  Add an action to create a new spectrum. The size is
        //  obtained from a dialog.
        LocalAction createSpectrumAction =
            new LocalAction( LocalAction.CREATE_SPECTRUM,
                             "Create new spectrum", null,
                             "Create a new spectrum with unset elements" );
        editMenu.add( createSpectrumAction );
    }

    /**
     * Create the View menu and populate it with appropriate actions.
     */
    protected void createViewMenu()
    {
        JMenu viewMenu = new JMenu( "View" );
        menuBar.add( viewMenu );

        //  Add an action to display the selected spectra.
        ImageIcon displayImage =
            new ImageIcon(ImageHolder.class.getResource("display.gif"));
        LocalAction displayAction =
            new LocalAction( LocalAction.SINGLE_DISPLAY,
                             "Display in new plots",
                             displayImage,
                             "Display selected spectra in separate windows" );
        viewMenu.add( displayAction );
        toolBar.add( displayAction );

        //  Add an action to display the selected spectra into the
        //  selected plots.
        ImageIcon multiDisplayImage =
            new ImageIcon(ImageHolder.class.getResource("multidisplay.gif"));
        LocalAction multiDisplayAction =
            new LocalAction( LocalAction.MULTI_DISPLAY,
                             "Display/add to plot", multiDisplayImage,
            "Display selected spectra in one plot or add to selected plots");
        viewMenu.add( multiDisplayAction );
        toolBar.add( multiDisplayAction );

        //  Add an action to display the selected spectra in a single
        //  plot, one at a time.
        ImageIcon animateImage =
            new ImageIcon(ImageHolder.class.getResource("animate.gif"));
        LocalAction animateAction =
            new LocalAction( LocalAction.ANIMATE_DISPLAY,
                             "Animate spectra", animateImage,
                "Animate selected spectra by displaying one at a time" );
        viewMenu.add( animateAction );
        toolBar.add( animateAction );

        //  Add an action to view the values of the spectra.
        ImageIcon viewerImage =
            new ImageIcon(ImageHolder.class.getResource("table.gif"));
        LocalAction viewerAction =
            new LocalAction( LocalAction.SPEC_VIEWER,
                             "View/modify spectra values", viewerImage,
                             "View/modify the values of the selected spectra");
        viewMenu.add( viewerAction );
        toolBar.add( viewerAction );

        //  Add an action to set the units of the X axis.
        ImageIcon xCoordTypeImage =
            new ImageIcon(ImageHolder.class.getResource("xunits.gif"));
        LocalAction xCoordTypeAction =
            new LocalAction( LocalAction.XCOORDTYPE_VIEWER,
                             "View/modify X axis coordinate type",
                             xCoordTypeImage, "View/modify the X axis" +
                             " coordinate type of selected spectra");
        viewMenu.add( xCoordTypeAction );
        toolBar.add( xCoordTypeAction );

        //  Add an action to cascade all the plot windows.
        JMenuItem cascade = new JMenuItem( "Cascade all plots" );
        viewMenu.add( cascade );
        cascade.addActionListener( this );
    }

    /**
     * Create the Options menu and populate it with appropriate actions.
     */
    protected void createOptionsMenu()
    {

        JMenu optionsMenu = new JMenu( "Options" );
        menuBar.add( optionsMenu );

        //  Add any locally availabel line identifiers.
        new LocalLineIDManager( optionsMenu, this );

        //  Add the LookAndFeel selections.
        new SplatLookAndFeelManager( contentPane, optionsMenu );

        //  Add facility to colourise all spectra.
        ImageIcon rainbowImage = new ImageIcon(
            ImageHolder.class.getResource( "rainbow.gif" ) );
        LocalAction colourizeAction  =
            new LocalAction( LocalAction.COLOURIZE, "Auto-colour spectra",
                             rainbowImage,
                             "Automatically choose a colour for all spectra" );
        optionsMenu.add( colourizeAction );
        toolBar.add( colourizeAction );

        //  Arrange the JSplitPane vertically or horizontally.
        splitOrientation = new JCheckBoxMenuItem( "Vertical split" );
        optionsMenu.add( splitOrientation );
        splitOrientation.addItemListener( this );
    }

    /**
     * Set the vertical or horizontal split. Also positions the
     * toolbar to match.
     */
    protected void setSplitOrientation( boolean init )
    {
        if ( init ) {
            //  Restore state of button from Preferences.
            boolean state = prefs.getBoolean("SplatBrowser_vsplit", true);
            splitOrientation.setSelected( state );
        }
        boolean selected = splitOrientation.isSelected();

        contentPane.remove( toolBarContainer );
        if ( selected ) {
            splitPane.setOrientation( JSplitPane.HORIZONTAL_SPLIT );
            toolBar.setOrientation( JToolBar.HORIZONTAL );
            contentPane.add( toolBarContainer, BorderLayout.NORTH );
        }
        else {
            splitPane.setOrientation( JSplitPane.VERTICAL_SPLIT );
            toolBar.setOrientation( JToolBar.VERTICAL );
            contentPane.add( toolBarContainer, BorderLayout.WEST );
        }
        contentPane.revalidate();
        prefs.putBoolean( "SplatBrowser_vsplit", selected );
    }

    /**
     * Create the Operations menu and populate it with appropriate
     * actions.
     */
    protected void createOperationsMenu()
    {

        JMenu operationsMenu = new JMenu( "Operations" );
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
        JMenu helpMenu = HelpFrame.createHelpMenu( "browser-window",
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
                System.err.println( "Failed to start remote services" );
                System.err.println( e.getMessage() );
            }
        }
    }

    /**
     * Initialise the remote control services in a separate
     * thread. Use this to get UI going quickly, but note that the
     * remote services may take some time to initialise.
     */
    protected void threadInitRemoteServices()
    {
        Thread loadThread = new Thread( "Remote services loader" ) {
                public void run() {
                    try {
                        initRemoteServices();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
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
     *  Colourize, i.e.&nbsp;automatically set the colour of all spectra.
     *  The colours applied depend on the number of spectra shown.
     */
    protected void colourizeSpectra()
    {
        int size = globalList.specCount();
        for ( int i = 0; i < size; i++ ) {
            int rgb = Utilities.getRandomRGB( (float) size );
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
     *
     * A better version of this would add a display as well as open
     * option and could provide a query for the contents of HDS
     * container and FITS MEF files.
     */
    protected void showOpenFileChooser()
    {
        initFileChooser( true );
        int result = fileChooser.showOpenDialog( this );
        if ( result == fileChooser.APPROVE_OPTION ) {
            newFiles = fileChooser.getSelectedFiles();
            if ( newFiles.length == 0 ) {
                newFiles = new File[1];
                newFiles[0] = fileChooser.getSelectedFile();
            }

            //  If the user requested that opened spectra are also
            //  displayed, then respect this.
            displayNewFiles = openDisplayCheckBox.isSelected();
            openUsertypeIndex = openUsertypeBox.getSelectedIndex();
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
        try {
            specData = TreeviewAccess.getInstance().splatNodeChooser
                ( this, "Open", "Select spectrum" );
        }
        catch (SplatException e) {
            new ExceptionDialog( this, e );
            return;
        }
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
    protected int openUsertypeIndex = 0;
    protected JPanel openAccessory = null;
    protected JCheckBox openDisplayCheckBox = null;
    protected JComboBox openUsertypeBox = null;

    /**
     * Initialise the accessory components for opening
     * spectra. Currently these provide the ability to choose whether
     * to display any opened spectra.
     */
    protected void initOpenAccessory()
    {
        openAccessory = new JPanel();
        GridBagLayouter layouter = new GridBagLayouter( openAccessory );

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
    protected int saveUsertypeIndex = 0;
    protected JPanel saveAccessory = null;
    protected JComboBox saveUsertypeBox = null;
    protected int saveTabletypeIndex = 0;
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
                specList.writeStack( file.getPath() );
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
        // Reading the stack performed by the SpecList global
        // object. Just give it a file name to use.
        initStackChooser( true );
        int result = stackChooser.showOpenDialog( this );
        if ( result == stackChooser.APPROVE_OPTION ) {
            File file = stackChooser.getSelectedFile();
            if ( file.exists() && file.canRead() ) {
                SpecList globalSpecList = SpecList.getInstance();
                int nread = globalSpecList.readStack( file.getPath() );

                //  If requested honour the display option.
                if ( ( nread > 0 ) && stackOpenDisplayCheckBox.isSelected() ) {
                    int[] currentSelection = getSelectedSpectra();
                    int count = globalList.specCount();
                    specList.setSelectionInterval( count - nread, count - 1 );
                    multiDisplaySelectedSpectra( true );
                    specList.setSelectedIndices( currentSelection );
                }
            }
            else {
                JOptionPane.showMessageDialog
                    ( this, "Cannot read file:" + file.getPath(),
                      "File access error", JOptionPane.ERROR_MESSAGE );
            }
        }
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

            BasicFileFilter stackFilter =
                new BasicFileFilter( "stk", "Stack files" );
            stackChooser.addChoosableFileFilter( stackFilter );

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
     * Load all the spectra stored in the newFiles array. Use a new
     * Thread so that we do not block the GUI or event threads.
     */
    protected void threadLoadChosenSpectra()
    {
        if ( newFiles != null ) {

            //  Monitor progress by checking the filesDone variable.
            initProgressMonitor( newFiles.length, "Loading spectra..." );
            waitTimer = new Timer ( 250, new ActionListener() {
                    public void actionPerformed( ActionEvent e ) {
                        progressMonitor.setProgress( filesDone );
                        if ( filesDone < newFiles.length ) {
                            progressMonitor.setNote( newFiles[filesDone].getPath() );
                        }
                    }
                });
            setWaitCursor();
            waitTimer.start();

            //  Now create the thread that reads the spectra.
            Thread loadThread = new Thread( "Spectra loader" ) {
                    public void run() {
                        try {
                            addChosenSpectra();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        finally {

                            //  Always tidy up and rewaken interface
                            //  when complete (including if an error
                            //  is thrown).
                            resetWaitCursor();
                            waitTimer.stop();
                            closeProgressMonitor();
                        }
                    }
                };

            //  Start loading spectra.
            loadThread.start();
        }
    }

    /**
     * Save a given spectrum as a file. Use a thread so that we do not
     * block the GUI or event threads.
     *
     * @param globalIndex the index on the global list of the spectrum
     *                    to save.
     * @param target the file to write the spectrum into.
     */
    protected void threadSaveSpectrum( int globalIndex, String target )
    {
        final int localGlobalIndex = globalIndex;
        final String localTarget = target;

        //  Monitor progress by checking the filesDone variable.
        initProgressMonitor( 1, "Saving spectrum..." );
        progressMonitor.setNote( "as " + localTarget );
        waitTimer = new Timer ( 250, new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    progressMonitor.setProgress( filesDone );
                }
            });
        setWaitCursor();
        waitTimer.start();

        //  Now create the thread that saves the spectrum.
        Thread saveThread = new Thread( "Spectrum saver" ) {
                public void run() {
                    try {
                        saveSpectrum( localGlobalIndex, localTarget );
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {

                        //  Always tidy up and rewaken interface when
                        //  complete (including if an error is thrown).
                        resetWaitCursor();
                        waitTimer.stop();
                        closeProgressMonitor();
                    }
                }
            };

        //  Start saving spectrum.
        saveThread.start();
    }

    /**
     * Timer for used for event queue actions.
     */
    private Timer waitTimer;

    /**
     * Number of spectra currently loaded by the addChosenSpectra method.
     */
    private int filesDone = 0;

    /**
     * Add spectra listed in the newFiles array to global list. If
     * given attempt to open the files using the type provided by the
     * user (in the open file dialog).
     */
    protected void addChosenSpectra()
    {
        // Add all spectra.
        filesDone = 0;
        int validFiles = 0;
        for ( int i = 0; i < newFiles.length; i++ ) {
            if ( addSpectrum( newFiles[i].getPath(), openUsertypeIndex ) ) {
                validFiles++;
            }
            filesDone++;
        }

        //  And now display them if required.
        if ( displayNewFiles && validFiles > 0 ) {
            int[] currentSelection = getSelectedSpectra();
            int count = globalList.specCount();
            if ( count - newFiles.length <= count - 1 ) {
                specList.setSelectionInterval( count - newFiles.length,
                                               count - 1 );
                multiDisplaySelectedSpectra( true );
                if ( currentSelection != null ) {
                    specList.setSelectedIndices( currentSelection );
                }
            }
        }
    }

    /**
     * Add a new spectrum, with a possibly pre-defined type, to the
     * global list. This becomes the current spectrum.
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
            SpecData spectrum = specDataFactory.get( name, usertype );
            addSpectrum( spectrum );
            return true;
        }
        catch ( SplatException e ) {
            new ExceptionDialog( this, e );
        }
        return false;
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
    public boolean addSpectrum( String name )
    {
        try {
            SpecData spectrum = specDataFactory.get( name );
            addSpectrum( spectrum );
            return true;
        }
        catch ( SplatException e ) {
            JOptionPane.showMessageDialog( this,
                                           e.getMessage(),
                                           "Error opening spectrum",
                                           JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    /**
     * Add a new SpecData object to the global list. This becomes the
     * current spectrum.
     *
     * @param spectrum the SpecData object.
     */
    public void addSpectrum( SpecData spectrum )
    {
        globalList.add( spectrum );

        //  Latest list entry becomes selected.
        SpecList list = SpecList.getInstance();
        specList.setSelectedIndex( list.specCount() - 1 );
    }

    /**
     * Display all the spectra that are selected in the global list
     * view. Each spectrum is displayed in a new plot.
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
                if ( plot != null && i != 0 ) {
                    //  Displace Plot slightly so that windows do not
                    //  totally obscure each other, but keep some control.
                    lastLocation.translate( offset, offset );
                    plot.setLocation( lastLocation );
                }
                lastLocation = plot.getLocation();
            }
        }
    }

    /**
     * Display all the currently selected spectra in the currently
     * selected plot, or create a new plot for them.
     *
     * @param fit whether to make all spectra fit the width and height
     *            of the plot
     */
    public void multiDisplaySelectedSpectra( boolean fit )
    {
        int[] specIndices = getSelectedSpectra();
        if ( specIndices == null ) {
            return;
        }
        SplatException lastException = null;
        int failed = 0;
        int[] plotIndices = getSelectedPlots();
        if ( plotIndices != null ) {
            SpecData spec = null;
            for ( int i = 0; i < specIndices.length; i++ ) {
                spec = globalList.getSpectrum( specIndices[i] );
                for ( int j = 0; j < plotIndices.length; j++ ) {
                    try {
                        globalList.addSpectrum( plotIndices[j], spec );
                    }
                    catch (SplatException e) {
                        failed++;
                        lastException = e;
                        plotIndices[j] = -1;
                    }
                }
            }
            if ( fit ) {
                for ( int j = 0; j < plotIndices.length; j++ ) {
                    if ( plotIndices[j] != -1 ) {
                        globalList.getPlot( plotIndices[j] ).fitToWidthAndHeight();
                    }
                }
            }
        }
        else {
            SpecData spec = null;
            spec = globalList.getSpectrum( specIndices[0] );
            PlotControlFrame plot = displaySpectrum( spec );
            for ( int i = 1; i < specIndices.length; i++ ) {
                spec = globalList.getSpectrum( specIndices[i] );
                try {
                    globalList.addSpectrum( plot.getPlot(), spec );
                }
                catch (SplatException e) {
                    failed++;
                    lastException = e;
                }
            }
            if ( fit && failed < specIndices.length ) {
                plot.getPlot().fitToWidthAndHeight();
            }
        }
        if ( lastException != null ) {
            reportOpenListFailed( failed, lastException );
        }
    }

    /**
     * Display a list of spectra given by their file names. The file
     * names are assumed to be in a space separated list stored in a
     * single String.
     *
     * @param list the list of spectra file names (or disk
     *             specifications).
     * @return the identifier of the plot that the spectra are
     *         displayed in, -1 if it fails.
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
        for ( int i = 0; i < count; i++ ) {
            if ( addSpectrum( st.nextToken() ) ) {
                indices[openedCount++] = specList.specCount() - 1;
            }
        }
        if ( openedCount == 0 ) return -1;

        //  Create a plot for our spectra and display them all.
        SpecData spec = globalList.getSpectrum( indices[0] );
        PlotControl plot = displaySpectrum( spec ).getPlot();
        int failed = 0;
        SplatException lastException = null;
        for ( int i = 1; i < openedCount; i++ ) {
            spec = globalList.getSpectrum( indices[i] );
            try {
                globalList.addSpectrum( plot, spec );
            }
            catch (SplatException e) {
                failed++;
                lastException = e;
            }
        }
        if ( lastException != null ) {
            reportOpenListFailed( failed, lastException );
            if ( failed == openedCount ) {
                return -1;
            }
        }
        return plot.getIdentifier();
    }

    /**
     * Display a list of spectra given by their file names, in a plot
     * specified by its identifier. The file names are assumed to be
     * in a space separated list stored in a single String. If the
     * plot doesn't exist then it is created and the identifier of
     * that plot is returned (which will be different from the one
     * requested).
     *
     * @param id the plot identifier number.
     * @param list the list of spectra file names (or disk
     *             specifications).
     * @return the id of the plot that the spectra are displayed
     *         in, -1 if it fails.
     */
    public int displaySpectra( int id, String list )
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
        for ( int i = 0; i < count; i++ ) {
            if ( addSpectrum( st.nextToken() ) ) {
                indices[openedCount++] = specList.specCount() - 1;
            }
        }
        if ( openedCount == 0 ) return -1;

        //  Access or create a plot for our spectra.
        int plotIndex = globalList.getPlotIndex( id );
        int newId = 0;
        int failed = 0;
        SplatException lastException = null;
        if ( plotIndex == -1 ) {
            SpecData spec = globalList.getSpectrum( indices[0] );
            PlotControlFrame plot = displaySpectrum( spec );
            for ( int i = 1; i < openedCount; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                try {
                    globalList.addSpectrum( plot.getPlot(), spec );
                }
                catch (SplatException e) {
                    failed++;
                    lastException = e;
                }
            }
            newId = plot.getPlot().getIdentifier();
        }
        else {
            PlotControl plot = globalList.getPlot( plotIndex );
            SpecData spec = null;
            for ( int i = 0; i < openedCount; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                try {
                    globalList.addSpectrum( plot, spec );
                }
                catch (SplatException e) {
                    failed++;
                    lastException = e;
                }

            }
            newId = plot.getIdentifier();
        }
        if ( failed != 0 ) {
            reportOpenListFailed( failed, lastException );
            if ( failed == openedCount ) {
                return -1;
            }
        }
        return newId;
    }

    /**
     * Make a report using an ExceptionDialog for when loading a list
     * of spectra has failed for some.
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
        new ExceptionDialog( this, message, lastException );
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
     * Display a window for viewing and possibly modifying the
     * coordinate type of the X axis.
     */
    public void viewXCoordType()
    {
        if ( xCoordTypeFrame == null ) {
            xCoordTypeFrame = new SpecXCoordTypeFrame( specList );

            //  We'd like to know if the window is closed.
            xCoordTypeFrame.addWindowListener( new WindowAdapter()
            {
                public void windowClosed( WindowEvent evt )
                {
                    xCoordTypeClosed();
                }
            });
        }
        else {
            Utilities.raiseFrame( xCoordTypeFrame );
            xCoordTypeFrame.setSelectionFrom( specList );
        }
    }

    /**
     * Animator window is closed.
     */
    protected void xCoordTypeClosed()
    {
        // Nullify if method for closing switches to dispose.
        // xCoordTypeFrame = null;
    }

    /**
     * Remove the currently selected spectra from the global list and
     * this interface.
     */
    public void removeSelectedSpectra()
    {
        int[] indices = getSelectedSpectra();
        if ( indices != null ) {
            SpecData spec = null;

            //  Do in reverse to avoid changes of index.
            for ( int i = indices.length - 1; i >= 0; i-- ) {
                globalList.removeSpectrum( indices[i] );
            }
            //uk.ac.starlink.splat.util.Utilities.fullGC( false );

            //  Make the first spectrum the selected one. Needed to
            //  progate changes to all listeners (even when now empty).
            globalList.setCurrentSpectrum( 0 );
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

    private int plotProgress = 0;
    /**
     * Display a spectrum in a new plot.
     *
     * @param spectrum The spectrum to display.
     * @return the plot that the spectrum is displayed in.
     */
    public PlotControlFrame displaySpectrum( SpecData spectrum )
    {
        // Set the cursor to wait while the plot is created.
        setWaitCursor();

        SpecDataComp comp = new SpecDataComp( spectrum );
        PlotControlFrame plot = null;
        try {
            plot = new PlotControlFrame( comp );
            int index = globalList.add( plot.getPlot() );
            plot.setTitle( Utilities.getReleaseName() + ": " +
                           globalList.getPlotName( index ) );

            //  We'd like to know if the plot window is closed.
            plot.addWindowListener( new WindowAdapter() {
                    public void windowClosed( WindowEvent evt ) {
                        removePlot( (PlotControlFrame) evt.getWindow() );
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
                    e.printStackTrace();
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
            globalList.add( globalIndex, target );
        }
        catch ( SplatException e ) {
            e.printStackTrace();
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
     * Create a new spectrum with a number of elements (greater than
     * 2) obtained interactively.
     */
    public void createSpectrum()
    {
        Number number =
            DecimalDialog.showDialog( this, "Create new spectrum",
                                      "Number of rows in spectrum",
                                      new DecimalFormat(),
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
                    newSpec.setDataQuick( coords, data );
                    globalList.add( newSpec );
               }
                catch (Exception e) {
                    new ExceptionDialog( this, e );
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
        public static final int BROWSE = 2;
        public static final int SINGLE_DISPLAY = 3;
        public static final int MULTI_DISPLAY = 4;
        public static final int ANIMATE_DISPLAY = 5;
        public static final int SPEC_VIEWER = 6;
        public static final int XCOORDTYPE_VIEWER = 7;
        public static final int SAVE_STACK = 8;
        public static final int READ_STACK = 9;
        public static final int REMOVE_SPECTRA = 10;
        public static final int SELECT_SPECTRA = 11;
        public static final int DESELECT_SPECTRA = 12;
        public static final int COLOURIZE = 13;
        public static final int REMOVE_PLOTS = 14;
        public static final int SELECT_PLOTS = 15;
        public static final int DESELECT_PLOTS = 16;
        public static final int BINARY_MATHS = 17;
        public static final int UNARY_MATHS = 18;
        public static final int COPY_SPECTRA = 19;
        public static final int COPYSORT_SPECTRA = 20;
        public static final int CREATE_SPECTRUM = 21;
        public static final int EXIT = 22;

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

        public void actionPerformed( ActionEvent ae ) 
        {
            switch (type) {
               case SAVE: {
                   showSaveFileChooser();
               }
               break;
               case OPEN: {
                   showOpenFileChooser();
               }
               break;
               case BROWSE: {
                   showSplatNodeChooser();
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
               case XCOORDTYPE_VIEWER: {
                   viewXCoordType();
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
               case COPYSORT_SPECTRA: {
                   copySelectedSpectra( true );
               }
               break;
               case CREATE_SPECTRUM: {
                   createSpectrum();
               }
               break;
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
        //  Only used for split window request.
        setSplitOrientation( false );
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
}
