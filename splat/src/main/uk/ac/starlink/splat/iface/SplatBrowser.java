package uk.ac.starlink.splat.iface;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
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
import uk.ac.starlink.splat.plot.PlotControl;
import uk.ac.starlink.splat.util.RemoteServer;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.util.SplatSOAPServer;

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
 * in the plots (see #PlotControlFrame, #PlotControl and #Plot).
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see #GlobalSpecPlotList
 * @see #SplatSelectedProperties
 * @see #SplatPlotTable
 * @see #PlotControlFrame
 * @see #PlotControl
 * @see #Plot
 *
 * @since $Date$
 * @since 25-SEP-2000
 */
public class SplatBrowser extends JFrame
{
    /**
     *  The global list of spectra and plots.
     */
    protected GlobalSpecPlotList
        globalList = GlobalSpecPlotList.getReference();

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
     *  Open or save file chooser.
     */
    protected JFileChooser fileChooser = null;

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
    protected JFileChooser stackChooser = null;

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
     * The look and feel, plus metal themes manager.
     */
    protected SplatLookAndFeelManager splatLookAndFeelManager = null;

    /**
     *  Create a browser with no existing spectra.
     */
    public SplatBrowser()
    {
        this( null );
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
        initProgressMonitor( 6, "Starting up..." );
        enableEvents( AWTEvent.WINDOW_EVENT_MASK );
        try {
            initComponents();
        }
        catch ( Exception e ) {
            e.printStackTrace();
            return;
        }
        closeProgressMonitor();

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
                    }
                });
        }
        initRemoteServices();
    }

    /**
     *  Initialise all visual components.
     */
    protected void initComponents()
    {
        updateProgressMonitor( "init interface" );

        //  Set up the content pane and window size.
        contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout( mainLayout );
        this.setSize( new Dimension( 550, 450 ) );
        this.setTitle( Utilities.getFullDescription() );

        //  Set up menus and toolbar.
        updateProgressMonitor( "menus" );
        setupMenusAndToolbar();

        //  Add the list of spectra to its scroller.
        updateProgressMonitor( "global list" );
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
        updateProgressMonitor( "actions" );
        specList.addMouseListener( new MouseAdapter() {
                public void mouseClicked( MouseEvent e ) {
                    if ( e.getClickCount() >= 2 ) {
                        displaySelectedSpectra();
                    }
                }
            });

        //  Set up the control area.
        controlArea.setLayout( controlAreaLayout );
        controlArea.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );
        controlScroller.getViewport().add( controlArea, null );

        //  Set up split pane.
        updateProgressMonitor( "split screen" );
        splitPane.setOneTouchExpandable( true );
        specList.setSize( new Dimension( 190, 0 ) );
        splitPane.setDividerLocation( 200 );

        //  Finally add the main components to the content and split
        //  panes.
        updateProgressMonitor( "layout" );
        splitPane.setLeftComponent( specListScroller );
        splitPane.setRightComponent( controlScroller );
        controlArea.add( plotTable, BorderLayout.WEST );
        controlArea.add( selectedProperties, BorderLayout.NORTH );
        contentPane.add( toolBarContainer, BorderLayout.NORTH );
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
        progressMonitor.setMillisToDecideToPopup( 2000 );
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

        //  Add the toolbar to a container. Need extra component for
        //  sensible float behaviour.
        toolBarContainer.setLayout( new BorderLayout() );
        toolBarContainer.add( toolBar );

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

        //  Add action to open a list of spectrum
        ImageIcon openImage = new ImageIcon(
            ImageHolder.class.getResource( "openfile.gif" ) );
        LocalAction openAction  = new LocalAction( LocalAction.OPEN,
                                                   "Open", openImage,
                                                   "Open spectra" );
        fileMenu.add( openAction );
        toolBar.add( openAction );

        //  Add action to save a spectrum
        ImageIcon saveImage = new ImageIcon(
            ImageHolder.class.getResource( "savefile.gif" ) );
        LocalAction saveAction  =
            new LocalAction( LocalAction.SAVE, "Save", saveImage,
                             "Save a spectrum to disk file" );
        fileMenu.add( saveAction );
        toolBar.add( saveAction );

        //  Add an action to read in a stack of spectra.
        ImageIcon readStackImage = new ImageIcon(
            ImageHolder.class.getResource( "readstack.gif" ) );
        LocalAction readStackAction =
            new LocalAction( LocalAction.READ_STACK, "Read stack",
                             readStackImage,
                             "Read back spectra stored in a disk file");
        fileMenu.add( readStackAction );
        toolBar.add( readStackAction );

        //  Add an action to save the stack of spectra.
        ImageIcon saveStackImage = new ImageIcon(
            ImageHolder.class.getResource( "savestack.gif" ) );
        LocalAction saveStackAction =
            new LocalAction( LocalAction.SAVE_STACK, "Save stack",
                             saveStackImage, "Save all spectra to disk file" );
        fileMenu.add( saveStackAction );
        toolBar.add( saveStackAction );

        //  Add an action to exit application.
        ImageIcon exitImage = new ImageIcon(
            ImageHolder.class.getResource( "exit.gif" ) );
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
                             "Display/add to plot",
                             multiDisplayImage,
            "Display selected spectra in one plot or add to selected plots");
        viewMenu.add( multiDisplayAction );
        toolBar.add( multiDisplayAction );

        //  Add an action to display the selected spectra in a single
        //  plot, one at a time.
        ImageIcon animateImage =
            new ImageIcon(ImageHolder.class.getResource("animate.gif"));
        LocalAction animateAction =
            new LocalAction( LocalAction.ANIMATE_DISPLAY,
                             "Animate spectra",
                             animateImage,
            "Display selected spectra in a single plot, one at a time");
        viewMenu.add( animateAction );
        toolBar.add( animateAction );

        //  Add an action to view the values of the spectra.
        ImageIcon viewerImage =
            new ImageIcon(ImageHolder.class.getResource("table.gif"));
        LocalAction viewerAction =
            new LocalAction( LocalAction.SPEC_VIEWER, "View spectra values",
                             viewerImage,
                             "View/modify the values of the selected spectra");
        viewMenu.add( viewerAction );
        toolBar.add( viewerAction );
    }

    /**
     * Create the Options menu and populate it with appropriate actions.
     */
    protected void createOptionsMenu()
    {

        JMenu optionsMenu = new JMenu( "Options" );
        menuBar.add( optionsMenu );

        //  Add the LookAndFeel selections.
        splatLookAndFeelManager =
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
    }

    /**
     * Create the Operations menu and populate it with appropriate actions.
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
            e.printStackTrace();
        }
    }

    /**
     *  Colourize, i.e. automatically set the colour of all spectra.
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
            displayNewFiles = accessoryDisplayCheckBox.isSelected();
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
               threadSaveSpectrum( indices[0], destFile.getPath() );
            }
            else {
                //  This occasionally happens (1.4), not sure why...
                JOptionPane.showMessageDialog( this,
                                               "No spectrum selected",
                                               "No write",
                                               JOptionPane.WARNING_MESSAGE );
            }
        }
    }

    /**
     * Initialise the file chooser to have the necessary filters.
     *
     * @param openDialog if true then the dialog is initialise for
     * reading in spectra (this adds a component for displaying any
     * spectra as well as opening).
     */
    protected void initFileChooser( boolean openDialog )
    {
        if (fileChooser == null ) {
            fileChooser = new JFileChooser( System.getProperty( "user.dir" ) );
            fileChooser.setMultiSelectionEnabled( true );

            String[] textExtensions = { "txt", "lis" };
            SpectralFileFilter textFileFilter =
                new SpectralFileFilter( textExtensions, "TEXT files" );
            fileChooser.addChoosableFileFilter( textFileFilter );

            String[] fitsExtensions = { "fits", "fit" };
            SpectralFileFilter fitsFileFilter =
                new SpectralFileFilter( fitsExtensions, "FITS files" );
            fileChooser.addChoosableFileFilter( fitsFileFilter );

            SpectralFileFilter hdsFileFilter =
                new SpectralFileFilter ( "sdf", "HDS container files" );
            fileChooser.addChoosableFileFilter( hdsFileFilter );

            fileChooser.addChoosableFileFilter
                ( fileChooser.getAcceptAllFileFilter() );
        }
        if ( openDialog ) {
            if ( openAccessory == null ) {
                initOpenAccessory();
            }
            fileChooser.setAccessory( openAccessory );
        }
        else {
            fileChooser.setAccessory( null );
        }
    }

    /**
     * Initialise the accessory components for opening
     * spectra. Currently these provide the ability to choose whether
     * to display any opened spectra.
     */
    protected void initOpenAccessory()
    {
        openAccessory = new JPanel();
        accessoryDisplayCheckBox = new JCheckBox( "Display" );
        accessoryDisplayCheckBox.setToolTipText( "Display opened spectra "+
                                                 "in new plot" );
        accessoryDisplayCheckBox.setSelected( true );
        openAccessory.add( accessoryDisplayCheckBox );
    }
    protected JPanel openAccessory = null;
    protected JCheckBox accessoryDisplayCheckBox = null;

    /**
     * A request to save the spectrum stack has been received.
     */
    protected void saveStackEvent()
    {
        // Saving the stack performed by the SpecList global
        // object. Just give it a file name to use.
        initStackChooser( true );
        int result = stackChooser.showSaveDialog( this );
        if ( result == stackChooser.APPROVE_OPTION ) {
            File file = stackChooser.getSelectedFile();
            if ( !file.exists() || ( file.exists() && file.canWrite() ) ) {
                SpecList specList = SpecList.getReference();
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
                SpecList globalSpecList = SpecList.getReference();
                int nread = globalSpecList.readStack( file.getPath() );

                //  If requested honour the display option.
                if ( ( nread > 0 ) && accessoryDisplayCheckBox.isSelected() ) {
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
            stackChooser = new JFileChooser( System.getProperty( "user.dir" ) );
            stackChooser.setMultiSelectionEnabled( false );

            SpectralFileFilter stackFilter =
                new SpectralFileFilter( "stk", "Stack files" );
            stackChooser.addChoosableFileFilter( stackFilter );

            stackChooser.addChoosableFileFilter
                ( stackChooser.getAcceptAllFileFilter() );
        }
        if ( openDialog ) {
            if ( openAccessory == null ) {
                initOpenAccessory();
            }
            stackChooser.setAccessory( openAccessory );
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

                        //  Always tidy up and rewaken interface
                        //  when complete (including if an error
                        //  is thrown).
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
     * Add spectra listed in the newFiles array to global list.
     */
    protected void addChosenSpectra()
    {
        // Add all spectra.
        filesDone = 0;
        int validFiles = 0;
        for ( int i = 0; i < newFiles.length; i++ ) {
            if ( addSpectrum( newFiles[i].getPath() ) ) {
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
            SpecDataFactory factory = SpecDataFactory.getReference();
            SpecData spectrum = factory.get( name );
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
        SpecList list = SpecList.getReference();
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
        int[] plotIndices = getSelectedPlots();
        if ( plotIndices != null ) {
            SpecData spec = null;
            PlotControlFrame plot = null;
            for ( int i = 0; i < specIndices.length; i++ ) {
                spec = globalList.getSpectrum( specIndices[i] );
                for ( int j = 0; j < plotIndices.length; j++ ) {
                    globalList.addSpectrum( plotIndices[j], spec );
                }
            }
            if ( fit ) {
                for ( int j = 0; j < plotIndices.length; j++ ) {
                    globalList.getPlot( plotIndices[j] ).fitToWidthAndHeight();
                }
            }
        }
        else {
            SpecData spec = null;
            spec = globalList.getSpectrum( specIndices[0] );
            PlotControlFrame plot = displaySpectrum( spec );
            for ( int i = 1; i < specIndices.length; i++ ) {
                spec = globalList.getSpectrum( specIndices[i] );
                globalList.addSpectrum( plot.getPlot(), spec );
            }
            if ( fit ) {
                plot.getPlot().fitToWidthAndHeight();
            }
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
        SpecList specList = SpecList.getReference();
        for ( int i = 0; i < count; i++ ) {
            if ( addSpectrum( st.nextToken() ) ) {
                indices[openedCount++] = specList.specCount() - 1;
            }
        }
        if ( openedCount == 0 ) return -1;

        //  Create a plot for our spectra and display them all.
        SpecData spec = globalList.getSpectrum( indices[0] );
        PlotControl plot = displaySpectrum( spec ).getPlot();
        for ( int i = 1; i < openedCount; i++ ) {
            spec = globalList.getSpectrum( indices[i] );
            globalList.addSpectrum( plot, spec );
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
        SpecList specList = SpecList.getReference();
        for ( int i = 0; i < count; i++ ) {
            if ( addSpectrum( st.nextToken() ) ) {
                indices[openedCount++] = specList.specCount() - 1;
            }
        }
        if ( openedCount == 0 ) return -1;

        //  Access or create a plot for our spectra.
        int plotIndex = globalList.getPlotIndex( id );
        int newId = 0;
        if ( plotIndex == -1 ) {
            SpecData spec = globalList.getSpectrum( indices[0] );
            PlotControlFrame plot = displaySpectrum( spec );
            for ( int i = 1; i < openedCount; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                globalList.addSpectrum( plot.getPlot(), spec );
            }
            newId = plot.getPlot().getIdentifier();
        }
        else {
            PlotControl plot = globalList.getPlot( plotIndex );
            SpecData spec = null;
            for ( int i = 0; i < openedCount; i++ ) {
                spec = globalList.getSpectrum( indices[i] );
                globalList.addSpectrum( plot, spec );
            }
            newId = plot.getIdentifier();
        }
        return newId;
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
            uk.ac.starlink.splat.util.Utilities.fullGC( false );

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
        finally {
            //  Always make sure we reset the cursor.
            resetWaitCursor();
        }
        return plot;
    }

    /**
     * Make copies of all the selected spectra. These are memory
     * copies.
     */
    public void copySelectedSpectra()
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
                    newSpec = SpecDataFactory.getReference().
                        createEditable( name, spec );
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
        SpecDataFactory factory = SpecDataFactory.getReference();
        try {
            SpecData target = factory.getClone( source, spectrum );
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
     * A request to exit the application has been received.
     */
    protected void exitApplicationEvent()
    {
        System.exit( 0 );
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
        public static final int SINGLE_DISPLAY = 2;
        public static final int MULTI_DISPLAY = 3;
        public static final int ANIMATE_DISPLAY = 4;
        public static final int SPEC_VIEWER = 5;
        public static final int SAVE_STACK = 6;
        public static final int READ_STACK = 7;
        public static final int REMOVE_SPECTRA = 8;
        public static final int SELECT_SPECTRA = 9;
        public static final int DESELECT_SPECTRA = 10;
        public static final int COLOURIZE = 11;
        public static final int REMOVE_PLOTS = 12;
        public static final int SELECT_PLOTS = 13;
        public static final int DESELECT_PLOTS = 14;
        public static final int BINARY_MATHS = 15;
        public static final int UNARY_MATHS = 16;
        public static final int COPY_SPECTRA = 17;
        public static final int EXIT = 18;

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
        public LocalAction( int type, String name, Icon icon, String help ) {
            super( name, icon );
            this.type = type;
            putValue( SHORT_DESCRIPTION, help );
        }

        public void actionPerformed( ActionEvent ae ) {
            switch (type) {
               case SAVE: {
                   showSaveFileChooser();
               }
               break;
               case OPEN: {
                   showOpenFileChooser();
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
                   copySelectedSpectra();
               }
               break;
               case EXIT: {
                   exitApplicationEvent();
               }
               break;
            }
        }
    }
}
