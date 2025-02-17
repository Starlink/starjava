package uk.ac.starlink.topcat;

import cds.tools.ExtApp;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultButtonModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;
import org.astrogrid.samp.client.DefaultClientProfile;
import uk.ac.starlink.auth.AuthManager;
import uk.ac.starlink.plastic.PlasticUtils;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.jdbc.TextModelsAuthenticator;
import uk.ac.starlink.table.gui.TableLoadClient;
import uk.ac.starlink.table.gui.TableLoadDialog;
import uk.ac.starlink.table.gui.TableLoadWorker;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.table.storage.MonitorStoragePolicy;
import uk.ac.starlink.topcat.activate.ActivationWindow;
import uk.ac.starlink.topcat.contrib.basti.BaSTITableLoadDialog;
import uk.ac.starlink.topcat.contrib.gavo.GavoTableLoadDialog;
import uk.ac.starlink.topcat.interop.PlasticCommunicator;
import uk.ac.starlink.topcat.interop.SampCommunicator;
import uk.ac.starlink.topcat.interop.TopcatCommunicator;
import uk.ac.starlink.topcat.interop.Transmitter;
import uk.ac.starlink.topcat.join.CdsUploadMatchWindow;
import uk.ac.starlink.topcat.join.ConeMultiWindow;
import uk.ac.starlink.topcat.join.DalMultiWindow;
import uk.ac.starlink.topcat.join.SiaMultiWindow;
import uk.ac.starlink.topcat.join.SsaMultiWindow;
import uk.ac.starlink.topcat.join.MatchWindow;
import uk.ac.starlink.topcat.plot.Cartesian3DWindow;
import uk.ac.starlink.topcat.plot.DensityWindow;
import uk.ac.starlink.topcat.plot.GraphicsWindow;
import uk.ac.starlink.topcat.plot.HistogramWindow;
import uk.ac.starlink.topcat.plot.LinesWindow;
import uk.ac.starlink.topcat.plot.PlotWindow;
import uk.ac.starlink.topcat.plot.SphereWindow;
import uk.ac.starlink.topcat.plot2.Control;
import uk.ac.starlink.topcat.plot2.PlotWindowType;
import uk.ac.starlink.topcat.plot2.StackPlotWindow;
import uk.ac.starlink.topcat.vizier.VizierTableLoadDialog;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.Loader;
import uk.ac.starlink.util.gui.DragListener;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.MemoryMonitor;
import uk.ac.starlink.util.gui.StringPaster;
import uk.ac.starlink.vo.DalLoader;
import uk.ac.starlink.vo.DalTableLoadDialog;
import uk.ac.starlink.vo.SkyDalTableLoadDialog;

/**
 * Main window providing user control of the TOPCAT application.
 * This is a singleton class.
 *
 * <p><strong>Note:</strong> there is a lot wrong with this class.
 * It's been here for as long as topcat has (i.e. since before I knew
 * better), and it does far too much, often in the wrong way.
 * It would be nice to do something about it one day, but in the
 * meantime, don't assume that there's a good reason for all the
 * implementation details that you see here.
 *
 * @author   Mark Taylor (Starlink)
 * @since    9 Mar 2004
 */
public class ControlWindow extends AuxWindow
                           implements ListSelectionListener, 
                                      ListDataListener,
                                      TableModelListener,
                                      TableColumnModelListener,
                                      TopcatListener {

    private static ControlWindow instance_;
    private static Logger logger_ = Logger.getLogger( "uk.ac.starlink.topcat" );

    /** "plastic", "samp" or null to indicate communications preference. */
    static String interopType_;

    /**
     * System property giving a list of custom actions to appear in toolbar.
     * Colon-separated classnames for Action implementations with no-arg
     * constructors.
     */
    public static String TOPCAT_TOOLS_PROP = "topcat.exttools";

    private final JList<TopcatModel> tablesList_;
    private final DefaultListModel<TopcatModel> tablesModel_;
    private final DefaultListModel<LoadingToken> loadingModel_;
    private final TableModelListener tableWatcher_ = this;
    private final TopcatListener topcatWatcher_ = this;
    private final ListSelectionListener selectionWatcher_ = this;
    private final ListDataListener tablesWatcher_ = this;
    private final TableColumnModelListener columnWatcher_ = this;
    private final WindowListener windowWatcher_ = new ControlWindowListener();
    private final StarTableOutput taboutput_ = new StarTableOutput();
    private final boolean canWrite_ = true;
    private final boolean canRead_ = true;
    private final TransferHandler importTransferHandler_ = 
        new ControlTransferHandler( true, false );
    private final TransferHandler exportTransferHandler_ =
        new ControlTransferHandler( false, true );
    private final TransferHandler bothTransferHandler_ =
        new ControlTransferHandler( true, true );
    private final Window window_ = this;
    private final ButtonModel dummyButtonModel_ = new DefaultButtonModel();
    private StarTableFactory tabfact_ = new StarTableFactory( true );
    private final RowRunner runner_ = RowRunner.DEFAULT;
    private final boolean showListToolBar_ = false;
    private LoadWindow loadWindow_;
    private SaveQueryWindow saveWindow_;
    private ConcatWindow concatWindow_;
    private ConeMultiWindow multiconeWindow_;
    private SiaMultiWindow multisiaWindow_;
    private SsaMultiWindow multissaWindow_;
    private CdsUploadMatchWindow cdsmatchWindow_;
    private ExtApp extApp_;
    private TopcatModel currentModel_;
    private int iPlotwin_;

    private final JTextField idField_ = new JTextField();
    private final JLabel indexLabel_ = new JLabel();
    private final JLabel locLabel_ = new JLabel();
    private final JLabel nameLabel_ = new JLabel();
    private final JLabel rowsLabel_ = new JLabel();
    private final JLabel qstatusLabel_ = new JLabel();
    private final JLabel colsLabel_ = new JLabel();
    private final JComboBox<RowSubset> subsetSelector_ = new JComboBox<>();
    private final SortOrderSelector sortSelector_ = new SortOrderSelector();
    private final JToggleButton sortSenseButton_ = new UpDownButton();
    private final JLabel activatorLabel_ = new JLabel();
    private final TopcatCommunicator communicator_;

    private final Action readAct_;
    private final Action saveAct_;
    private final Action dupAct_;
    private final Action mirageAct_;
    private final Action removeAct_;
    private final Action concatAct_;
    private final Action upAct_;
    private final Action downAct_;
    private final Action multiconeAct_;
    private final Action multisiaAct_;
    private final Action multissaAct_;
    private final Action cdsmatchAct_;
    private final ModelViewAction datalinkAct_;
    private final Action logAct_;
    private final Action authResetAct_;
    private final Action[] matchActs_;
    private final ShowAction[] showActs_;
    private final ModelViewAction[] viewActs_;
    private final Action[] graphicsActs_;

    /**
     * Constructs a new window.
     */
    private ControlWindow() {
        super( "TOPCAT", null );

        /* Configure table factory. */
        tabfact_.setPreparation( new TopcatPreparation( tabfact_
                                                       .getPreparation() ) );
        tabfact_.getJDBCHandler()
                .setAuthenticator( new TextModelsAuthenticator() );
        taboutput_.setJDBCHandler( tabfact_.getJDBCHandler() );

        /* Set up a list of the known tables. */
        tablesModel_ = new DefaultListModel<TopcatModel>();
        loadingModel_ = new DefaultListModel<LoadingToken>();
        tablesList_ = new JList<TopcatModel>( tablesModel_ );

        /* Watch the list. */
        tablesList_.addListSelectionListener( selectionWatcher_ );
        tablesModel_.addListDataListener( tablesWatcher_ );

        /* Watch the label field. */
        idField_.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                getCurrentModel().setLabel( idField_.getText() );
            }
        } );

        /* Watch the sort configuration controls. */
        ActionListener sortListener = evt -> {
            if ( currentModel_ != null ) {
                SortOrder sortOrder =
                    currentModel_.getSortSelectionModel()
                                 .getSelectedSortOrder();
                boolean sortSense =
                    currentModel_.getSortSenseModel().isSelected();
                currentModel_.sortBy( sortOrder, sortSense );
            }
        };
        sortSelector_.addActionListener( sortListener );
        sortSenseButton_.addActionListener( sortListener );
        dummyButtonModel_.setSelected( true );

        /* Set up a panel displaying table information. */
        JComponent rowsLine = Box.createHorizontalBox();
        rowsLine.add( rowsLabel_ );
        rowsLine.add( Box.createHorizontalStrut( 5 ) );
        rowsLine.add( qstatusLabel_ );
        qstatusLabel_.setForeground( UIManager
                                    .getColor( "Label.disabledForeground" ) );
        InfoStack info = new InfoStack();
        info.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) );
        info.addLine( "Label", idField_ );
        info.addLine( "Location", locLabel_ );
        info.addLine( "Name", nameLabel_ );
        info.addLine( "Rows", rowsLine );
        info.addLine( "Columns", colsLabel_ );
        info.addLine( "Sort Order", new Component[] { sortSenseButton_,
                                                      sortSelector_ } );
        info.addGap();
        info.addLine( "Row Subset", subsetSelector_ );
        info.addLine( "Activation Actions", activatorLabel_ );
        info.fillIn();

        /* Reduce size of unused control panel. */
        JComponent controlPanel = getControlPanel();
        controlPanel.setLayout( new BoxLayout( controlPanel,
                                               BoxLayout.X_AXIS ) );

        /* Set up a split pane in the main panel. */
        JSplitPane splitter = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );
        JList<LoadingToken> loadingList = new LoadingList( loadingModel_ );
        JScrollPane listScroller =
            new JScrollPane( new JList2( tablesList_, loadingList ) );
        JPanel listPanel = new JPanel( new BorderLayout() );
        listPanel.add( listScroller, BorderLayout.CENTER );
        JScrollPane infoScroller = new JScrollPane( info );
        JComponent infoPanel = new JPanel( new BorderLayout() );
        MemoryMonitor memmon = new MemoryMonitor();
        memmon.setPreferredSize( new Dimension( Integer.MAX_VALUE, 24 ) );
        listPanel.add( memmon, BorderLayout.SOUTH );
        JToolBar listToolbar = new JToolBar( JToolBar.VERTICAL );
        listToolbar.setFloatable( false );
        if ( showListToolBar_ ) {
            listPanel.add( listToolbar, BorderLayout.EAST );
        }
        infoPanel.add( infoScroller, BorderLayout.CENTER );
        listScroller.setBorder( makeTitledBorder( "Table List" ) );
        infoScroller.setBorder( makeTitledBorder( "Current Table " +
                                                  "Properties" ) );
        listPanel.setPreferredSize( new Dimension( 200, 200 ) );
        splitter.setLeftComponent( listPanel );
        splitter.setRightComponent( infoPanel );
        splitter.setDividerLocation( 192 + ( showListToolBar_ ? 32 : 0 ) );
        getMainArea().add( splitter );

        /* Configure drag and drop on the list panel. */
        tablesList_.setDragEnabled( true );
        tablesList_.setTransferHandler( bothTransferHandler_ );
        loadingList.setTransferHandler( importTransferHandler_ );
        listScroller.setTransferHandler( importTransferHandler_ );

        /* SAMP/PLASTIC interoperability. */
        communicator_ = createCommunicator( this );
        if ( communicator_ != null ) {
            JComponent interopPanel = communicator_.createInfoPanel();
            if ( interopPanel != null ) {
                infoPanel.add( interopPanel, BorderLayout.SOUTH );
            }
        }

        /* Set up actions. */
        removeAct_ = new ControlAction( "Discard Table(s)", ResourceIcon.DELETE,
                                        "Remove the selected table or tables "
                                      + "from the application" );
        readAct_ = new ControlAction( "Load Table", ResourceIcon.LOAD,
                                      "Open a new table" );
        saveAct_ = new ControlAction( "Save Table(s)/Session",
                                      ResourceIcon.SAVE,
                                      "Saves one or more tables or "
                                    + "the TOPCAT session" );
        concatAct_ = new ControlAction( "Concatenate Tables",
                                        ResourceIcon.CONCAT,
                                        "Join tables by concatenating them" );
        upAct_ = new ControlAction( "Move table up", ResourceIcon.MOVE_UP,
                                    "Moves the current table "
                                  + "one position up in the tables list" );
        downAct_ = new ControlAction( "Move table down", ResourceIcon.MOVE_DOWN,
                                      "Moves the current table "
                                    + "one position down in the tables list" );
        multiconeAct_ =
            new ControlAction( "Multicone", ResourceIcon.MULTICONE,
                               "Multiple cone search"
                             + " (one for each row of input table)" );
        multisiaAct_ =
            new ControlAction( "Multiple SIA", ResourceIcon.MULTISIA,
                               "Multiple Simple Image Access query"
                             + " (one for each row of input table)" );
        multissaAct_ =
            new ControlAction( "Multiple SSA", ResourceIcon.MULTISSA,
                               "Multiple Simple Spectral Access query"
                             + " (one for each row of input table)" );
        cdsmatchAct_ =
            new ControlAction( "CDS Upload X-Match", ResourceIcon.CDSXMATCH,
                               "Sky crossmatch against remote tables from the "
                             + "CDS VizieR or SIMBAD services" );
        logAct_ = new ControlAction( "View Log", ResourceIcon.LOG,
                                     "Display the log of events" );
        authResetAct_ =
            new ControlAction( "Reset Authentication", ResourceIcon.RESET_AUTH,
                               "Clear any authentication credentials "
                             + "entered so far in the GUI" );
                                         
        readAct_.setEnabled( canRead_ );
        saveAct_.setEnabled( canWrite_ );
        logAct_.setEnabled( LogHandler.getInstance() != null );

        dupAct_ = new ExportAction( "Duplicate Table", ResourceIcon.COPY,
                                    "Create a duplicate of the current table" );

        mirageAct_ = new ExportAction( "Export To Mirage", null,
                               "Launch Mirage to display the current table" );
        mirageAct_.setEnabled( MirageHandler.isMirageAvailable() );

        final ModelViewAction viewerAct;
        viewActs_ = new ModelViewAction[] {
            viewerAct =
            new ModelViewWindowAction<TableViewerWindow>
                                     ( "Table Data", ResourceIcon.VIEWER,
                                       "Display table cell data",
                                       TableViewerWindow.class ),
            new ModelViewWindowAction<ParameterWindow>
                                     ( "Table Parameters", ResourceIcon.PARAMS,
                                       "Display table metadata",
                                       ParameterWindow.class ),
            new ModelViewWindowAction<ColumnInfoWindow>
                                     ( "Column Info", ResourceIcon.COLUMNS,
                                       "Display column metadata",
                                       ColumnInfoWindow.class ),
            new ModelViewWindowAction<SubsetWindow>
                                     ( "Row Subsets", ResourceIcon.SUBSETS,
                                       "Display row subsets",
                                       SubsetWindow.class ),
            new ModelViewWindowAction<StatsWindow>
                                     ( "Column Statistics", ResourceIcon.STATS,
                                       "Display statistics for each column",
                                       StatsWindow.class ),
            new ActivationWindowAction( "Activation Actions",
                                        ResourceIcon.ACTIVATE,
                                        "Display actions invoked when rows "
                                      + "are selected" ),
            datalinkAct_ =
            new ModelViewWindowAction<DatalinkWindow>
                                     ( "DataLink View", ResourceIcon.DATALINK,
                                       "Show row data as a DataLink table",
                                       DatalinkWindow.class ),
        };
        graphicsActs_ = new Action[] {
            new GraphicsWindowAction( "Histogram Plot (old)",
                                      ResourceIcon.HISTOGRAM,
                                      "Old-style histogram plot window",
                                      HistogramWindow.class ),
            new GraphicsWindowAction( "2D Plot (old)", ResourceIcon.PLOT,
                                      "Old-style 2D scatter plot window",
                                      PlotWindow.class ),
            new GraphicsWindowAction( "3D Plot (old)", ResourceIcon.PLOT3D,
                                      "Old-style 3D scatter plot window",
                                      Cartesian3DWindow.class ),
            new GraphicsWindowAction( "Sphere Plot (old)", ResourceIcon.SPHERE,
                                      "Old-style spherical polar scatter plot",
                                      SphereWindow.class ),
            new GraphicsWindowAction( "Lines Plot (old)", ResourceIcon.STACK,
                                      "Old-style stacked line plot",
                                      LinesWindow.class ),
            new GraphicsWindowAction( "Density Plot (old)",
                                      ResourceIcon.DENSITY,
                                      "Old-style density plot (2D histogram)",
                                      DensityWindow.class ),
        };
        Action[] plot2Acts = new Action[] {
            new Plot2WindowAction( PlotWindowType.HISTOGRAM ),
            new Plot2WindowAction( PlotWindowType.PLANE ),
            new Plot2WindowAction( PlotWindowType.SKY ),
            new Plot2WindowAction( PlotWindowType.CUBE ),
            new Plot2WindowAction( PlotWindowType.SPHERE ),
            new Plot2WindowAction( PlotWindowType.MATRIX ),
            new Plot2WindowAction( PlotWindowType.TIME ),
        };
        Action[] morePlot2Acts = new Action[] {
        };

        matchActs_ = new Action[] {
            new MatchWindowAction( "Internal Match", ResourceIcon.MATCH1,
                                   "Perform row matching on a single table", 
                                   1 ),
            new MatchWindowAction( "Pair Match", ResourceIcon.MATCH2,
                                   "Create new table by matching rows in " +
                                   "two existing tables", 2 ),
            new MatchWindowAction( "Triple Match", ResourceIcon.MATCHN,
                                   "Create new table by matching rows in " +
                                   "three existing tables", 3 ),
            new MatchWindowAction( "Quadruple Match", ResourceIcon.MATCHN,
                                   "Create new table by matching rows in " +
                                   "four existing tables", 4 ),
            new MatchWindowAction( "Quintuple Match", ResourceIcon.MATCHN,
                                   "Create new table by matching rows in " +
                                   "five existing tables", 5 ),
        };

        /* Hack.  We want to get an action to launch the TAP load
         * dialogue here.  One way is to call getLoadWindow() and get it
         * from there.  However, that instantiates the Load Window too
         * early, which is not only inefficient (not otherwise required
         * at startup) but also causes some trouble with JDBC
         * (the SQL load dialog fails for reasons I haven't identified).
         * So do it another way. */
        Action tapAct =
           createLoadDialogAction( TopcatTapTableLoadDialog.class );

        Transmitter tableTransmitter = communicator_ == null
                                     ? null
                                     : communicator_.getTableTransmitter();
        Action interopAct = communicator_ == null
                          ? null
                          : communicator_.createWindowAction( this );

        /* Configure the list to try to load a table when you paste 
         * text location into it. */
        MouseListener pasteLoader = new StringPaster() {
            public void pasted( final String loc ) {
                TableLoader loader = new TableLoader() {
                    public String getLabel() {
                        return "Pasted";
                    }
                    public TableSequence loadTables( StarTableFactory tfact )
                            throws IOException {
                        return tfact.makeStarTables( loc.trim(), null );
                    }
                };
                runLoading( loader, new TopcatLoadClient( ControlWindow.this,
                                                          ControlWindow.this ),
                            null );
            }
        };
        listScroller.addMouseListener( pasteLoader );

        /* Configure load button for mouse actions. */
        JButton readButton = new JButton( readAct_ );
        readButton.setText( null );
        readButton.setTransferHandler( importTransferHandler_ );
        readButton.addMouseListener( pasteLoader );

        /* Bind an action for double-click or Enter key on the list. */
        tablesList_.addMouseListener( new MouseAdapter() {
            public void mouseClicked( MouseEvent evt ) {
                if ( evt.getClickCount() >= 2 ) {
                    ActionEvent aevt = new ActionEvent( evt.getSource(),
                                                        evt.getID(),
                                                        "Display Table" );
                    viewerAct.actionPerformed( aevt );
                }
            }
        } );

        /* Keystroke invoked actions on tables list. */
        InputMap listInputs = tablesList_.getInputMap();
        ActionMap listActs = tablesList_.getActionMap();
        Object actkey = viewerAct.getValue( Action.NAME );
        listInputs.put( KeyStroke.getKeyStroke( KeyEvent.VK_ENTER, 0 ),
                        actkey );
        listActs.put( actkey, viewerAct );
        Object delkey = removeAct_.getValue( Action.NAME );
        listInputs.put( KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0 ),
                        delkey );
        listActs.put( delkey, removeAct_ );
        Object upkey = upAct_.getValue( Action.NAME );
        listInputs.put( KeyStroke.getKeyStroke( KeyEvent.VK_UP,
                                                InputEvent.ALT_DOWN_MASK ),
                        upkey );
        listActs.put( upkey, upAct_ );
        Object downkey = downAct_.getValue( Action.NAME );
        listInputs.put( KeyStroke.getKeyStroke( KeyEvent.VK_DOWN,
                                                InputEvent.ALT_DOWN_MASK ),
                        downkey );
        listActs.put( downkey, downAct_ );

        /* Add actions to the list toolbar. */
        listToolbar.add( upAct_ );
        listToolbar.add( downAct_ );
        listToolbar.add( dupAct_ );
        listToolbar.add( removeAct_ );

        /* Add load/save control buttons to the toolbar. */
        JToolBar toolBar = getToolBar();
        toolBar.setFloatable( true );
        toolBar.add( readButton );
        configureExportSource( toolBar.add( saveAct_ ) );
        if ( tableTransmitter != null ) {
            toolBar.add( tableTransmitter.getBroadcastAction() );
        }
        toolBar.addSeparator();

        /* Add table view buttons to the toolbar. */
        for ( Action viewAct : viewActs_ ) {
            if ( viewAct != datalinkAct_ ) {
                toolBar.add( viewAct );
            }
        }
        toolBar.addSeparator();

        /* Add visualisation buttons to the toolbar. */
        for ( int i = 0; i < plot2Acts.length; i++ ) {
            toolBar.add( plot2Acts[ i ] );
        }
        toolBar.addSeparator();

        /* Add join/match control buttons to the toolbar. */
        toolBar.add( matchActs_[ 1 ] );
        if ( tapAct != null ) {
            toolBar.add( tapAct );
        }
        toolBar.add( cdsmatchAct_ );
        toolBar.addSeparator();

        /* Add miscellaneous actions to the toolbar. */
        toolBar.add( MethodWindow.getWindowAction( this, false ) );
        for ( TopcatToolAction tact :
              Loader.getClassInstances( TOPCAT_TOOLS_PROP,
                                        TopcatToolAction.class ) ) {
            tact.setParent( this );
            toolBar.add( tact );
        }
        toolBar.addSeparator();

        /* Add actions to the file menu.  This is a rebadged version of the
         * standard Window menu used by most other windows. */
        JMenu fileMenu = getWindowMenu();
        fileMenu.setText( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        for ( int i = fileMenu.getItemCount() - 1; i >= 0; i-- ) {
            if ( "Scrollable".equals( fileMenu.getItem( i ).getText() ) ) {
                fileMenu.remove( i );
            }
        }
        int fileMenuPos = 0;
        fileMenu.insert( readAct_, fileMenuPos++ );
        fileMenu.insert( saveAct_, fileMenuPos++ );
        fileMenu.insert( dupAct_, fileMenuPos++ )
                      .setMnemonic( KeyEvent.VK_P );
        fileMenu.insert( removeAct_, fileMenuPos++ )
                      .setMnemonic( KeyEvent.VK_D );
        fileMenu.insert( upAct_, fileMenuPos++ );
        fileMenu.insert( downAct_, fileMenuPos++ );
        if ( tableTransmitter != null ) {
            fileMenu.insertSeparator( fileMenuPos++ );
            fileMenu.insert( tableTransmitter.getBroadcastAction(),
                             fileMenuPos++ );
            fileMenu.insert( tableTransmitter.createSendMenu(), fileMenuPos++ );
        }
        if ( MirageHandler.isMirageAvailable() ) {
            fileMenu.insert( mirageAct_, fileMenuPos++ );
        }
        fileMenu.insertSeparator( fileMenuPos++ );
        fileMenu.insert( logAct_, fileMenuPos++ );
        fileMenu.insert( authResetAct_, fileMenuPos++ );
        fileMenu.insertSeparator( fileMenuPos++ );

        /* Add a menu for the table views. */
        JMenu viewMenu = new JMenu( "Views" );
        viewMenu.setMnemonic( KeyEvent.VK_V );
        for ( int i = 0; i < viewActs_.length; i++ ) {
            viewMenu.add( viewActs_[ i ] );
        }
        getJMenuBar().add( viewMenu );

        /* Add a menu for visualisation windows. */
        JMenu graphicsMenu = new JMenu( "Graphics" );
        graphicsMenu.setMnemonic( KeyEvent.VK_G );
        for ( int i = 0; i < plot2Acts.length; i++ ) {
            graphicsMenu.add( plot2Acts[ i ] );
        }
        for ( int i = 0; i < morePlot2Acts.length; i++ ) {
            graphicsMenu.add( morePlot2Acts[ i ] );
        }
        graphicsMenu.addSeparator();
        for ( int i = 0; i < graphicsActs_.length; i++ ) {
            graphicsMenu.add( graphicsActs_[ i ] );
        }
        getJMenuBar().add( graphicsMenu );

        /* Add a menu for table joining. */
        JMenu joinMenu = new JMenu( "Joins" );
        joinMenu.setMnemonic( KeyEvent.VK_J );
        joinMenu.add( concatAct_ );
        joinMenu.add( cdsmatchAct_ );
        joinMenu.add( multiconeAct_ );
        joinMenu.add( multisiaAct_ );
        joinMenu.add( multissaAct_ );
        for ( int i = 0; i < matchActs_.length; i++ ) {
            joinMenu.add( matchActs_[ i ] );
        }
        getJMenuBar().add( joinMenu );

        /* Add a menu for window management. */
        JMenu winMenu = new JMenu( "Windows" );
        winMenu.setMnemonic( KeyEvent.VK_W );
        showActs_ = makeShowActions();
        for ( int i = 0; i < showActs_.length; i++ ) {
            winMenu.add( showActs_[ i ] );
        }
        getJMenuBar().add( winMenu );

        /* Add a menu for miscellaneous Virtual Observatory operations. 
         * Defer population of the menu until such time, if ever, that 
         * the menu is posted, since (a) the fully configured chooser 
         * is not available until after the construction of this window, 
         * and (b) it involves loading of classes which might not 
         * otherwise be required.  (a) is basically down to bad design - 
         * the Driver and ControlWindow classes are poorly organised. */
        final JMenu voMenu = new JMenu( "VO" );
        voMenu.setMnemonic( KeyEvent.VK_V );
        voMenu.addMenuListener( new MenuListener() {
            public void menuCanceled( MenuEvent evt ) {
            }
            public void menuDeselected( MenuEvent evt ) {
            }
            public void menuSelected( MenuEvent evt ) {
                voMenu.removeMenuListener( this );
                List<Class<? extends TableLoadDialog>> tldClasses =
                    new ArrayList<Class<? extends TableLoadDialog>>();
                tldClasses.add( TopcatConeSearchDialog.class );
                tldClasses.add( TopcatSiapTableLoadDialog.class );
                tldClasses.add( TopcatSsapTableLoadDialog.class );
                tldClasses.add( TopcatTapTableLoadDialog.class );
                tldClasses.add( VizierTableLoadDialog.class );
                tldClasses.add( TopcatHapiTableLoadDialog.class );
                tldClasses.add( GavoTableLoadDialog.class );
                tldClasses.add( BaSTITableLoadDialog.class );
                LoadWindow loadWin = getLoadWindow();
                for ( Class<? extends TableLoadDialog> clazz : tldClasses ) {
                    Action act = loadWin.getDialogAction( clazz );
                    assert act != null;
                    voMenu.add( act );
                }
                voMenu.addSeparator();
                voMenu.add( cdsmatchAct_ );
                voMenu.add( multiconeAct_ );
                voMenu.add( multisiaAct_ );
                voMenu.add( multissaAct_ );
            }
        } );
        getJMenuBar().add( voMenu );

        /* Add a menu for tool interop. */
        if ( communicator_ != null ) {
            JMenu interopMenu = new JMenu( "Interop" );
            interopMenu.setMnemonic( KeyEvent.VK_I );
            if ( interopAct != null ) {
                interopMenu.add( interopAct );
            }
            Action[] commActions = communicator_.getInteropActions();
            for ( int ia = 0; ia < commActions.length; ia++ ) {
                interopMenu.add( commActions[ ia ] );
            }
            interopMenu.add( tableTransmitter.getBroadcastAction() );
            interopMenu.add( tableTransmitter.createSendMenu() );
            getJMenuBar().add( interopMenu );
        }

        /* Mark this window as top-level. */
        setCloseIsExit();

        /* Add help information. */
        addHelp( "ControlWindow" );
        JMenu helpMenu = getHelpMenu();
        int ihPos = helpMenu.getItemCount() - 1;
        helpMenu.insert( MethodWindow.getWindowAction( this, false ), ihPos++ );
        helpMenu.insertSeparator( ihPos++ );

        /* Make closing this window equivalent to closing the application,
         * since without it the application can't be controlled. */
        setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
        addWindowListener( windowWatcher_ );

        /* Prepare to display the window. */
        updateInfo();
        updateControls();
    }

    /**
     * Returns the sole instance of this window, made visible on initialisation.
     * 
     * @return  instance of control window
     */
    public static ControlWindow getInstance() {
        return getInstance( true );
    }

    /**
     * Returns the sole instance of this window, optionally made visible
     * on initialisation.
     *
     * @param  initialiseVisible  if this call results in lazy creation of
     *                            a window instance, set it visible
     * @return  instance of control window
     */
    public static ControlWindow getInstance( boolean initialiseVisible ) {
        if ( instance_ == null ) {
            instance_ = new ControlWindow();
            if ( initialiseVisible ) {
                instance_.pack();
                instance_.setVisible( true );
            }
        }
        return instance_;
    }

    /**
     * Returns a CDS-style ExtApp object which can be used for control
     * of this control window instance.
     *
     * @return   external application object for TOPCAT
     */
    public ExtApp getExtApp() {
        if ( extApp_ == null ) {
            extApp_ = new TopcatExtApp( this );
        }
        return extApp_;
    }

    /**
     * Returns the object which acts as this window's server
     * for interop requests.
     *
     * @return  plastic server  
     */
    public TopcatCommunicator getCommunicator() {
        return communicator_;
    }

    /**
     * Returns the RowRunner to be used for multithreaded table processing.
     *
     * @return  row runner
     */
    public RowRunner getRowRunner() {
        return runner_;
    }

    /**
     * Returns an action that will reset all authentication contexts.
     *
     * @return   auth reset action
     */
    public Action getAuthResetAction() {
        return authResetAct_;
    }

    /**
     * Adds a table to this windows list.
     * Following this, a user will be able to do TOPCATty things with
     * the table in question from this control window.
     *
     * @param  table  the table to add
     * @param  location  location string indicating the provenance of
     *         <code>table</code> - preferably a URL or filename or something
     * @param  select  true iff the newly-added table should become the
     *         currently selected table
     * @return the newly-created TopcatModel object corresponding to 
     *         <code>table</code>
     */
    public TopcatModel addTable( StarTable table, String location,
                                 boolean select ) {
        TopcatModel tcModel =
            TopcatUtils.decodeSession( table, location, this );
        if ( tcModel == null ) {
            tcModel = TopcatModel
                     .createDefaultTopcatModel( table, location, this );
            tcModel.setLabel( shorten( location ) );
        }
        tablesModel_.addElement( tcModel );
        logger_.info( "Load new table " + tcModel + " from " + location );
        if ( select || tablesList_.getSelectedValue() == null ) {
            tablesList_.setSelectedValue( tcModel, true );
        }
        if ( select ) {
            makeVisible();
        }
        return tcModel;
    }

    /**
     * Removes an entry from the table list.
     *
     * @param  model  the table entry to remove
     */
    public void removeTable( TopcatModel model ) {
        if ( tablesModel_.contains( model ) ) {
            setViewsVisible( model, false );
            tablesList_.clearSelection();
            tablesModel_.removeElement( model );
        }
    }

    /**
     * Moves the current table one item up or down in the tables list.
     *
     * @param   up  true for move up (to lower index),
     *              false for move down (to higher index)
     */
    public void moveCurrent( boolean up ) {
        ListSelectionModel selModel = tablesList_.getSelectionModel();
        int iFrom = selModel.getMinSelectionIndex();
        TopcatModel tcFrom = tablesModel_.getElementAt( iFrom );
        int iTo = iFrom + ( up ? -1 : +1 );
        int iLo = iFrom + ( up ? -1 : 0 );
        tablesModel_.insertElementAt( tablesModel_.getElementAt( iLo ),
                                      iLo + 2 );
        tablesModel_.remove( iLo );
        assert tablesModel_.get( iTo ) == tcFrom;
        tablesList_.setSelectionInterval( iTo, iTo );
    }

    /**
     * Passes tables from a loader to a load client, presenting progress
     * information and cancellation control as appropriate in the GUI.
     * If the load client is a TopcatLoadClient, this will have the effect
     * of loading the tables into the application.
     * This method is the usual way of inserting new tables which may be
     * time-consuming to load into the TOPCAT application.
     *
     * @param   loader  table source
     * @param   loadClient  table destination
     *                      (usually a {@link TopcatLoadClient})
     * @param   icon   optional icon to accompany the progress GUI
     */
    public void runLoading( TableLoader loader,
                            final TableLoadClient loadClient,
                            final Icon icon ) {
        new LoadRunner( loader, loadClient, icon, 750 ).start();
    }

    /**
     * Adds a LoadingToken to the load list.
     * This indicates that a table is in the process of being loaded.
     * The caller must remove the token later, when the table load has
     * either succeeded or failed.
     *
     * @param  token  token to add
     */
    public void addLoadingToken( LoadingToken token ) {
        loadingModel_.addElement( token );
    }

    /**
     * Removes a LoadingToken from the load list.
     *
     * @param  token  token to remove
     */
    public void removeLoadingToken( LoadingToken token ) {
        loadingModel_.removeElement( token );
    }

    /**
     * Updates the state of a LoadingToken.  If it is in the loading list,
     * it will be repainted.
     *
     * @param  token  token to update
     */
    public void updateLoadingToken( LoadingToken token ) {
        int ix = loadingModel_.indexOf( token );
        if ( ix >= 0 ) {
            loadingModel_.set( ix, token );
        }
    }

    /**
     * Returns a new StarTableFactory which will update the given 
     * LoadingToken as rows are read into row stores provided by its
     * storage policy.  By using this rather than this control window's
     * basic StarTableFactory, the LoadingToken's display will monitor
     * the number of rows loaded, which is useful visual feedback for
     * the user, especially for large/slow tables.
     *
     * @param   token  token to update
     * @return   table factory; note this should only be used for work
     *           associated with the given token
     */
    public StarTableFactory createMonitorFactory( final LoadingToken token ) {
        TableSink monitorSink = new TableSink() {
            long nrow;
            long irow;
            Timer timer;
            public void acceptMetadata( StarTable meta ) {
                nrow = meta.getRowCount();
                timer = new Timer( 100, new ActionListener() {
                    public void actionPerformed( ActionEvent evt ) {
                        updateLoadingToken( token );
                    }
                } );
                irow = 0;
                timer.start();
            }
            public void acceptRow( Object[] row ) {
                StringBuffer sbuf = new StringBuffer();
                sbuf.append( TopcatUtils.formatLong( ++irow ) );
                if ( nrow > 0 ) {
                    sbuf.append( '/' );
                    sbuf.append( TopcatUtils.formatLong( nrow ) );
                }
                token.setProgress( sbuf.toString() );
            }
            public void endRows() {
                timer.stop();
            }
        };
        StarTableFactory tfact = new StarTableFactory( getTableFactory() );
        StoragePolicy policy =
            new MonitorStoragePolicy( tfact.getStoragePolicy(), monitorSink );
        tfact.setStoragePolicy( policy );
        return tfact;
    }

    /**
     * Returns the TopcatModel corresponding to the currently selected table.
     *
     * @return  selected model
     */
    public TopcatModel getCurrentModel() {
        return currentModel_;
    }

    /**
     * Returns an array of all selected tables.
     * This is not widely used - for most purposes it is assumed that only
     * a maximum of one table is selected.  However, it turns out that
     * it's always been possible to select multiple tables, and this
     * can be used when deleting multiple tables.
     * Possibly this method may get withdrawn in the future, so think
     * carefully before using it.
     *
     * @return   array of selected tables
     */
    private TopcatModel[] getSelectedModels() {
        return tablesList_.getSelectedValuesList()
                          .toArray( new TopcatModel[ 0 ] );
    }

    /**
     * Returns the list model which keeps track of which tables are available
     * to the application.
     *
     * @return  list model of {@link TopcatModel} objects
     */
    public ListModel<TopcatModel> getTablesListModel() {
        return tablesModel_;
    }

    /**
     * Returns the JList displaying tables available in the application.
     *
     * @return  list of {@link TopcatModel} objects
     */
    public JList<TopcatModel> getTablesList() {
        return tablesList_;
    }

    /**
     * Returns a dialog used for loading new tables.
     *
     * @return  a table load window
     */
    public LoadWindow getLoadWindow() {
        if ( loadWindow_ == null ) {
            loadWindow_ = new LoadWindow( null, tabfact_ );
        }
        return loadWindow_;
    }

    /**
     * Returns a dialog used for saving tables.
     *
     * @return  a table save window
     */
    public SaveQueryWindow getSaver() {
        if ( saveWindow_ == null ) {
            saveWindow_ =
                new SaveQueryWindow( getTableOutput(), getLoadWindow(),
                                     ControlWindow.this );
        }
        return saveWindow_;
    }

    /**
     * Returns a dialog used for doing table concatenation.
     *
     * @return  concatenation window
     */
    public ConcatWindow getConcatWindow() {
        if ( concatWindow_ == null ) {
            concatWindow_ = new ConcatWindow( this );
        }
        return concatWindow_;
    }

    /**
     * Returns a dialog used for a multiple cone search join.
     *
     * @return  multicone window
     */
    public ConeMultiWindow getConeMultiWindow() {
        if ( multiconeWindow_ == null ) {
            multiconeWindow_ = new ConeMultiWindow( this );
        }
        return multiconeWindow_;
    }

    /**
     * Returns a dialog used for a multiple SIA join.
     *
     * @return   multi-SIA window
     */
    public SiaMultiWindow getSiaMultiWindow() {
        if ( multisiaWindow_ == null ) {
            multisiaWindow_ = new SiaMultiWindow( this );
        }
        return multisiaWindow_;
    }

    /**
     * Returns a dialog used for a multiple SSA join.
     *
     * @return   multi-SSA window
     */
    public SsaMultiWindow getSsaMultiWindow() {
        if ( multissaWindow_ == null ) {
            multissaWindow_ = new SsaMultiWindow( this );
        }
        return multissaWindow_;
    }

    /**
     * Returns a dialog used for an upload match.
     *
     * @return   upload crossmatch window
     */
    public CdsUploadMatchWindow getCdsUploadMatchWindow() {
        if ( cdsmatchWindow_ == null ) {
            cdsmatchWindow_ = new CdsUploadMatchWindow( this );
        }
        return cdsmatchWindow_;
    }

    /**
     * Returns the table factory used by this window.
     *
     * @return  table factory
     */
    public StarTableFactory getTableFactory() {
        return tabfact_;
    }

    /**
     * Returns the table output manager used by this window.
     *
     * @return  table outputter
     */
    public StarTableOutput getTableOutput() {
        return taboutput_;
    }

    /**
     * Sets the table factory used by this window.
     *
     * @param   tabfact   table factory
     */
    public void setTableFactory( StarTableFactory tabfact ) {
        tabfact_ = tabfact;
        taboutput_.setJDBCHandler( tabfact.getJDBCHandler() );
    }

    /**
     * Takes a sky position acquired from somewhere and does something with it.
     *
     * @param  raDegrees   right ascension in degrees
     * @param  decDegrees  declination in degrees
     * @return  true if any useful work was done
     */
    public boolean acceptSkyPosition( double raDegrees, double decDegrees ) {
        boolean accepted = false;
        if ( loadWindow_ != null ) {
            TableLoadDialog[] tlds = loadWindow_.getKnownDialogs();
            for ( int i = 0; i < tlds.length; i++ ) {
                TableLoadDialog tld = tlds[ i ];
                if ( tld instanceof SkyDalTableLoadDialog ) {
                    accepted = accepted
                            || ((SkyDalTableLoadDialog) tld)
                              .acceptSkyPosition( raDegrees, decDegrees );
                }
                else if ( tld instanceof TopcatTapTableLoadDialog ) {
                    accepted = accepted
                            || ((TopcatTapTableLoadDialog) tld)
                              .acceptSkyPosition( raDegrees, decDegrees );
                }
            }
        }
        return accepted;
    }

    /**
     * Load received VO resource identifiers into appropriate windows.
     *
     * @param  ids  array of candidate ivo:-type resource identifiers to load
     * @param  msg  text to explain to the user what's being loaded
     * @param  dalLoaderClass   DalLoader subclass for
     *         dialogues which may be affected by the loaded IDs
     * @param  dalMultiWindowClass  DalMultiWindow subclass for
     *         dialogues which may be affected by the loaded IDs
     */
    public boolean acceptResourceIdList(
                       String[] ids, String msg,
                       Class<? extends DalLoader> dalLoaderClass,
                       Class<? extends DalMultiWindow> dalMultiWindowClass ) {
        boolean accepted = false;

        /* Handle single table load dialogues. */
        if ( loadWindow_ != null ) {
            TableLoadDialog[] tlds = loadWindow_.getKnownDialogs();
            for ( int i = 0; i < tlds.length; i++ ) {
                if ( loadDialogMatches( tlds[ i ], dalLoaderClass ) ) {
                    boolean acc = ((DalLoader) tlds[ i ])
                                 .acceptResourceIdList( ids, msg );
                    accepted = accepted || acc;
                }
            }
        }

        /* Handle multi-DAL load windows. */
        DalMultiWindow[] multiWins = new DalMultiWindow[] {
            multiconeWindow_,
            multisiaWindow_,
            multissaWindow_,
        };
        for ( int i = 0; i < multiWins.length; i++ ) {
            DalMultiWindow multiWin = multiWins[ i ];
            if ( multiWin != null &&
                 multiWindowMatches( multiWin, dalMultiWindowClass ) ) {
                boolean acc = multiWin.acceptResourceIdList( ids, msg );
                accepted = accepted || acc;
            }
        }

        /* Return status. */
        return accepted;
    }

    /**
     * Indicates whether the given load dialogue is of the type indicated
     * by the given class.
     *
     * @param  tld  load dialogue
     * @param  tldClass  load dialogue type
     */
    public boolean
            loadDialogMatches( TableLoadDialog tld, Class<?> tldClass ) {
        return tldClass != null && tldClass.isAssignableFrom( tld.getClass() );
    }

    /**
     * Indicates whether the given multi window is of the type indicated
     * by the given class.
     */
    public boolean
        multiWindowMatches( DalMultiWindow mw,
                            Class<? extends DalMultiWindow> mwClass ) {
        return mwClass != null && mwClass.isAssignableFrom( mw.getClass() );
    }

    /**
     * Reveals or hides any existing view windows for a given table.
     *
     * @param  tcModel  table to affect
     * @param  visible   true to reveal, false to hide
     */
    public void setViewsVisible( TopcatModel tcModel, boolean visible ) {
        for ( int i = 0; i < viewActs_.length; i++ ) {
            viewActs_[ i ].setViewVisible( tcModel, visible );
        }
    }

    /**
     * Shuts down TOPCAT.  According to whether or not it is running 
     * standalone, this may invoke {@link java.lang.System#exit} itself,
     * or it may just attempt to get rid of all the windows associated
     * with the TOPCAT application.  In the latter case, the JVM should
     * survive.
     *
     * @param   confirm  whether to seek confirmation from the user
     * @return  whether shutdown took place.  If the user aborted the
     *          exit, then <code>false</code> will be returned.  If the exit
     *          did happen, then either <code>true</code> will be returned
     *          or (standalone case) there will be no return.
     */
    public boolean exit( boolean confirm ) {
        if ( ( ! confirm ) || 
             tablesModel_.getSize() == 0 ||
             confirm( "Shut down TOPCAT", "Confirm Exit" ) ) {
            removeWindowListener( windowWatcher_ );
            if ( Driver.isStandalone() ) {
                System.exit( 0 );
            }
            else {
                for ( Enumeration<?> en = tablesModel_.elements();
                      en.hasMoreElements(); ) {
                    removeTable( (TopcatModel) en.nextElement() );
                }
                dispose();
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Updates the information displayed in the info panel of this window.
     * This should be invoked at least when the selection is changed.
     */
    private void updateInfo() {
        TopcatModel tcModel = getCurrentModel();
        boolean hasModel = tcModel != null;

        /* Ensure the info panel is up to date. */
        if ( hasModel ) {
            StarTable dataModel = tcModel.getDataModel();
            ViewerTableModel viewModel = tcModel.getViewModel();
            TableColumnModel columnModel = tcModel.getColumnModel();
            long totCols = dataModel.getColumnCount();
            long totRows = dataModel.getRowCount();
            int visCols = columnModel.getColumnCount();
            int visRows = viewModel.getRowCount();
            String loc = tcModel.getLocation();
            String name = dataModel.getName();

            idField_.setText( tcModel.getLabel() );
            indexLabel_.setText( tcModel.getID() + ": " );
            locLabel_.setText( loc );
            nameLabel_.setText( loc.equals( name ) ? null : name );
            rowsLabel_.setText( TopcatUtils.formatLong( totRows ) + 
                                ( ( visRows == totRows ) 
                              ? ""
                              : " (" + TopcatUtils.formatLong( visRows )
                                     + " apparent)" ) );
            qstatusLabel_.setText( getQueryStatus( dataModel ) );
            colsLabel_.setText( totCols +
                                ( ( visCols == totCols )
                                            ? ""
                                            : " (" + visCols + " apparent)" ) );

            sortSelector_.setModel( tcModel.getSortSelectionModel() );
            subsetSelector_.setModel( tcModel.getSubsetSelectionModel() );
            sortSenseButton_.setModel( tcModel.getSortSenseModel() );
            activatorLabel_.setText( tcModel.getActivationWindow()
                                            .getActivationSummary() );
        }
        else {
            idField_.setText( null );
            indexLabel_.setText( "0: " );
            locLabel_.setText( null );
            nameLabel_.setText( null );
            rowsLabel_.setText( null );
            colsLabel_.setText( null );

            sortSelector_.setModel( SortOrderSelector.Model.DUMMY_MODEL );
            subsetSelector_.setModel( new DefaultComboBoxModel<RowSubset>() );
            sortSenseButton_.setModel( dummyButtonModel_ );
            activatorLabel_.setText( null );
        }

        /* Make sure that the actions which relate to a particular table model
         * are up to date. */
        dupAct_.setEnabled( hasModel );
        if ( communicator_ != null ) {
            communicator_.getTableTransmitter().setEnabled( hasModel );
        }
        mirageAct_.setEnabled( hasModel );
        removeAct_.setEnabled( hasModel );
        saveAct_.setEnabled( hasModel );
        upAct_.setEnabled( tcModel != null &&
                           tcModel != tablesModel_.getElementAt( 0 ) );
        downAct_.setEnabled( tcModel != null &&
                             tcModel != tablesModel_
                                       .getElementAt( tablesModel_
                                                     .getSize() - 1 ) );
        subsetSelector_.setEnabled( hasModel );
        sortSelector_.setEnabled( hasModel );
        sortSenseButton_.setEnabled( hasModel );
        for ( int i = 0; i < viewActs_.length; i++ ) {
            viewActs_[ i ].setEnabled( hasModel );
        }
        datalinkAct_.setEnabled( hasModel &&
                                 DatalinkWindow.isUseful( tcModel ) );
        for ( int i = 0; i < showActs_.length; i++ ) {
            ShowAction sact = showActs_[ i ];
            if ( sact.selEffect != sact.otherEffect ) {
                sact.setEnabled( hasModel );
            }
        }
        idField_.setEnabled( hasModel );
        idField_.setEditable( hasModel );
    }

    /**
     * Updates some window state.  This should be called at least when the
     * list of tables changes.
     */
    public void updateControls() {
        boolean hasTables = tablesModel_.getSize() > 0;
        concatAct_.setEnabled( hasTables );
        multiconeAct_.setEnabled( hasTables );
        multisiaAct_.setEnabled( hasTables );
        multissaAct_.setEnabled( hasTables );
        cdsmatchAct_.setEnabled( hasTables );
        for ( int i = 0; i < matchActs_.length; i++ ) {
            matchActs_[ i ].setEnabled( hasTables );
        }
        for ( int i = 0; i < graphicsActs_.length; i++ ) {
            graphicsActs_[ i ].setEnabled( hasTables );
        }
        int isel = tablesList_.getSelectedIndex();
        upAct_.setEnabled( isel > 0 );
        downAct_.setEnabled( isel >= 0 && isel < tablesModel_.getSize() - 1 );
    }

    /*
     * Listener implementations.
     */

    public void valueChanged( ListSelectionEvent evt ) {
        TopcatModel nextModel = tablesList_.getSelectedValue();
        if ( nextModel != currentModel_ ) {
            if ( currentModel_ != null ) {
                currentModel_.removeTopcatListener( topcatWatcher_ );
                currentModel_.getViewModel()
                             .removeTableModelListener( tableWatcher_ );
                currentModel_.getColumnModel()
                             .removeColumnModelListener( columnWatcher_ );
            }
            currentModel_ = nextModel;
            if ( currentModel_ != null ) {
                currentModel_.addTopcatListener( topcatWatcher_ );
                currentModel_.getViewModel()
                             .addTableModelListener( tableWatcher_ );
                currentModel_.getColumnModel()
                             .addColumnModelListener( columnWatcher_ );
            }
            updateInfo();
        }
    }

    public void tableChanged( TableModelEvent evt ) {
        updateInfo();
    }

    public void modelChanged( TopcatEvent evt ) {
        switch ( evt.getCode() ) {

            /* Model label has changed. */
            case TopcatEvent.LABEL:
                updateInfo();
                int index = tablesModel_.indexOf( evt.getModel() );

                /* If the model is represented in the list panel (presumably
                 * it is), update the list panel by firing events on the 
                 * list model's listeners. */
                if ( index >= 0 ) {
                    ListDataEvent event = 
                        new ListDataEvent( this, ListDataEvent.CONTENTS_CHANGED,
                                           index, index );
                    ListDataListener[] listWatchers = 
                        tablesModel_.getListDataListeners();
                    for ( int i = 0; i < listWatchers.length; i++ ) {
                        listWatchers[ i ].contentsChanged( event );
                    }
                }
                break;

            /* Activation actions have changed. */
            case TopcatEvent.ACTIVATOR:
                if ( evt.getModel() == getCurrentModel() ) {
                    String actTxt = evt.getDatum() instanceof String
                                  ? (String) evt.getDatum()
                                  : evt.getModel().getActivationWindow()
                                                  .getActivationSummary();
                    activatorLabel_.setText( actTxt );
                }
                break;
        }
    }

    public void columnAdded( TableColumnModelEvent evt ) {
        updateInfo();
    }
    public void columnRemoved( TableColumnModelEvent evt ) {
        updateInfo();
    }
    public void columnMarginChanged( ChangeEvent evt ) {}
    public void columnMoved( TableColumnModelEvent evt ) {}
    public void columnSelectionChanged( ListSelectionEvent evt ) {}

    public void contentsChanged( ListDataEvent evt ) {
        updateControls();
    }
    public void intervalAdded( ListDataEvent evt ) {
        updateControls();
    }
    public void intervalRemoved( ListDataEvent evt ) {
        updateControls();
    }

    /**
     * Returns a string giving additional query status information for a
     * given table.  This extracts information that may have been present in
     * DALI-style INFO/@name="QUERY_STATUS" nodes in the original VOTable
     * and flattens them into a user-readable form.  An empty string
     * is returned if there's nothing to say (table apparently loaded OK).
     *
     * @param  table  table
     * @return  query status string; may be empty, but not null
     * @see   <a href="http://www.ivoa.net/documents/DALI/">DALI</a>
     */
    private static String getQueryStatus( StarTable table ) {
        for ( DescribedValue dval : table.getParameters() ) {
            ValueInfo info = dval.getInfo();
            if ( Tables.QUERY_STATUS_INFO.getName().equals( info.getName() ) ) {
                Object vobj = dval.getValue();

                /* According to DALI, the only values of interest here
                 * would be OVERFLOW and ERROR.  However, if something
                 * else other than OK is present, it's probably worth
                 * reporting anyway; most likely it's a misspelling or
                 * something, that the user may be able to make of. */
                if ( vobj instanceof String ) {
                    String status = ((String) vobj).trim();
                    if ( ! "OK".equals( status ) &&
                         status.length() > 0 ) {
                        StringBuffer sbuf = new StringBuffer()
                            .append( " " )
                            .append( "(" )
                            .append( status );
                        sbuf.append( ")" );
                        return sbuf.toString();
                    }
                }
            }
        }

        /* Cone Search 1.03 Section 2 paragraph 3 requires a non-DALI
         * variant. */
        for ( DescribedValue dval : table.getParameters() ) {
            ValueInfo info = dval.getInfo();
            if ( "Error".equals( info.getName() ) ) {
                Object vobj = dval.getValue();
                return vobj instanceof String ? ((String) vobj).trim()
                                              : "Error";
            }
        }
        return "";
    }

    /**
     * Constructs a new TopcatCommunicator.
     * The implementation used is determined by the value of the 
     * <code>interopType_</code> static variable.
     *
     * @param  control   control window
     * @return   communicator, or null if interop is disabled
     */
    private static TopcatCommunicator
                   createCommunicator( ControlWindow control ) {
        try {
            return attemptCreateCommunicator( control );
        }
        catch ( SecurityException e ) {
            logger_.log( Level.WARNING,
                         "No tool interop, permission denied: ", e );
            return null;
        }
    }

    /**
     * Attempts to construct a new TopcatCommunicator.
     * The implementation used is determined by the value of the 
     * <code>interopType_</code> static variable.
     *
     * @param  control   control window
     * @return   communicator, or null if interop is disabled
     * @throws  SecurityException  in case of insufficient permissions
     */
    private static TopcatCommunicator
                   attemptCreateCommunicator( ControlWindow control ) {
        if ( "none".equals( interopType_ ) ) {
            logger_.info( "Run with no interop" );
            return null;
        }
        else if ( "plastic".equals( interopType_ ) ) {
            logger_.info( "Run in PLASTIC mode by request" );
            return new PlasticCommunicator( control );
        }
        else {
            final String msg;
            if ( "samp".equals( interopType_ ) ) {
                msg = "Run in SAMP mode by request";
            }
            else {
                assert interopType_ == null;
                msg = DefaultClientProfile.getProfile().isHubRunning()
                    ? "SAMP hub running - run in SAMP mode"
                    : "Run in SAMP mode by default";
            }
            logger_.info( msg );
            try {
                return new SampCommunicator( control );
            }
            catch ( IOException e ) {
                throw new RuntimeException( "SAMP config failed", e );
            }
        }
    }

    /**
     * General control actions.
     */
    private class ControlAction extends BasicAction {
        
        ControlAction( String name, Icon icon, String shortdesc ) {
            super( name, icon, shortdesc );
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( this == readAct_ ) {
                getLoadWindow().makeVisible();
            }
            else if ( this == saveAct_ ) {
                getSaver().makeVisible();
            }
            else if ( this == removeAct_ ) {
                TopcatModel[] tcModels = getSelectedModels();
                int nt = tcModels.length;
                if ( nt == 0 ) {
                    return;
                }
                final Object msg;
                if ( nt == 1 ) {
                    msg = "Remove table " + tcModels[ 0 ] + " from list?";
                }
                else {
                    List<String> msgList = new ArrayList<String>();
                    msgList.add( "Remove tables from list?" );
                    for ( int i = 0; i < nt; i++ ) {
                        msgList.add( "    " + tcModels[ i ] );
                    }
                    msg = msgList.toArray( new String[ 0 ] );
                }
                if ( confirm( msg, "Confirm Remove" ) ) {
                    for ( int i = 0; i < nt; i++ ) {
                        removeTable( tcModels[ i ] );
                    }
                }
            }
            else if ( this == concatAct_ ) {
                getConcatWindow().makeVisible();
            }
            else if ( this == upAct_ ) {
                moveCurrent( true );
            }
            else if ( this == downAct_ ) {
                moveCurrent( false );
            }
            else if ( this == multiconeAct_ ) {
                getConeMultiWindow().makeVisible();
            }
            else if ( this == multisiaAct_ ) {
                getSiaMultiWindow().makeVisible();
            }
            else if ( this == multissaAct_ ) {
                getSsaMultiWindow().makeVisible();
            }
            else if ( this == cdsmatchAct_ ) {
                getCdsUploadMatchWindow().makeVisible();
            }
            else if ( this == logAct_ ) {
                LogHandler.getInstance().showWindow( ControlWindow.this );
            }
            else if ( this == authResetAct_ ) {
                AuthManager.getInstance().clear();
            }
            else {
                assert false;
            }
        }
    }

    /**
     * Actions for popping up match windows.
     */
    private class MatchWindowAction extends BasicAction {

        private final int nTable;
        private MatchWindow matchWin;

        MatchWindowAction( String name, Icon icon, String shortdesc,
                           int nTable ) {
            super( name, icon, shortdesc );
            this.nTable = nTable;
        }

        public void actionPerformed( ActionEvent evt ) {
            if ( matchWin == null ) {
                matchWin = new MatchWindow( ControlWindow.this, nTable );
            }
            matchWin.makeVisible();
        }
    }

    /**
     * Class for showing a certain kind of window which applies to TopcatModels.
     */
    private interface ModelViewAction extends Action {

        /**
         * Ensures that this action's window for a given table is visible
         * or invisible, if it exists.
         * The window will not be created specially however. 
         *
         * @param  tcModel  table to reveal for
         * @param  visible  true to reveal, false to hide
         */
        void setViewVisible( TopcatModel tcModel, boolean visible );
    }

    /**
     * ModelViewAction class for actions which pop up a view window.
     */
    private class ModelViewWindowAction<W extends AuxWindow>
            extends BasicAction
            implements ModelViewAction {

        W window_;
        final Constructor<W> constructor_;
        private final Map<TopcatModel,W> modelWindows_ =
            new WeakHashMap<TopcatModel,W>();

        /**
         * Constructor.
         *
         * @param  name  action name
         * @param  icon  action icon
         * @param  shortdesc  action short description
         * @param  winClass  AuxWindow subclass - must have a
         *         constructor that takes (TopcatModel,Component).
         */
        ModelViewWindowAction( String name, Icon icon, String shortdesc,
                               Class<W> winClass ) {
            super( name, icon, shortdesc );
            try {
                constructor_ = winClass.getConstructor( new Class<?>[] {
                    TopcatModel.class, Component.class,
                } );
            }
            catch ( NoSuchMethodException e ) {
                throw (IllegalArgumentException)
                      new IllegalArgumentException( "No suitable constructor" )
                     .initCause( e );
            }
        }

        /**
         * Displays this action's window type for the currently selected
         * table.
         */
        public void actionPerformed( ActionEvent evt ) {
            TopcatModel tcModel = getCurrentModel();
            if ( tcModel == null ) {
                return;
            }
            W window = modelWindows_.get( tcModel );
            if ( window == null ) {
                window = createWindow( tcModel );
                modelWindows_.put( tcModel, window );
            }
            window.setVisible( true );
        }

        public void setViewVisible( TopcatModel tcModel, boolean visible ) {
            W window = modelWindows_.get( tcModel );
            if ( window != null ) {
                if ( visible ) {
                    window.setVisible( true );
                }
                else {
                    window.dispose();
                }
            }
        }

        /**
         * Creates this action's window for a given table.
         *
         * @param  tcModel  table the window will apply to
         */
        private W createWindow( TopcatModel tcModel ) {
            try {
                Object[] args = new Object[] { tcModel, ControlWindow.this };
                try {
                    return constructor_.newInstance( args );
                }
                catch ( InvocationTargetException e ) {
                    throw e.getCause();
                }
            }
            catch ( RuntimeException e ) {
                throw e;
            }
            catch ( Error e ) {
                throw e;
            }
            catch ( Throwable e ) {
                throw new RuntimeException( "Window creation failed???", e );
            }
        }
    }

    /**
     * Action for opening the Activation window.
     */
    private class ActivationWindowAction extends BasicAction
                                         implements ModelViewAction {
        ActivationWindowAction( String name, Icon icon, String shortdesc ) {
            super( name, icon, shortdesc );
        }
        public void actionPerformed( ActionEvent evt ) {
            TopcatModel tcModel = getCurrentModel();
            if ( tcModel != null ) {
                tcModel.getActivationWindow().setVisible( true );
            }
        }
        public void setViewVisible( TopcatModel tcModel, boolean visible ) {
            Window window = tcModel.getActivationWindow();
            if ( window != null ) {
                if ( visible ) {
                    window.setVisible( true );
                }
                else {
                    window.dispose();
                }
            }
        }
    }

    /**
     * Action class for old-style graphics windows.
     */
    private class GraphicsWindowAction
            extends TopcatWindowAction<GraphicsWindow> {

        /**
         * Constructor.
         *
         * @param  name  action name
         * @param  icon  action icon
         * @param  shortdesc  action short description
         * @param  winClass  GraphicsWindow subclass - must have a
         *         constructor that takes (Component)
         */
        GraphicsWindowAction( String name, Icon icon, String shortdesc,
                              Class<? extends GraphicsWindow> winClass ) {
            super( name, icon, shortdesc, winClass );
        }

        @Override
        public void actionPerformed( ActionEvent evt ) {
            GraphicsWindow window = createWindow();
            TopcatModel tcModel = getCurrentModel();
            if ( tcModel != null ) {
                int npoint =
                    (int) Math.min( tcModel.getDataModel().getRowCount(),
                                    (long) Integer.MAX_VALUE );
                window.setGuidePointCount( npoint );
                window.setVisible( true );
                if ( tcModel != null ) {
                    window.setMainTable( tcModel );
                }
            }
            window.setVisible( true );
        }
    }

    /**
     * Action class for new-style graphics windows.
     */
    private class Plot2WindowAction extends BasicAction
                                    implements TopcatToolAction {
        private final PlotWindowType ptype_;
        private Component plot2parent_;

        /**
         * Constructor.
         *
         * @param  ptype  plot type
         */
        Plot2WindowAction( PlotWindowType ptype ) {
            super( ptype.getName() + " Plot", ptype.getIcon(),
                   ptype.getDescription() );
            ptype_ = ptype;
        }

        public void actionPerformed( ActionEvent evt ) {
            StackPlotWindow<?,?> window =
                ptype_.createWindow( plot2parent_, tablesModel_ );
            window.setTitle( ptype_.getName() + " Plot (" + ++iPlotwin_ + ")" );
            TopcatModel tcModel = getCurrentModel();
            Control dfltControl =
                window.getControlManager().createDefaultControl( tcModel );
            if ( dfltControl != null ) {
                window.getControlStack().addControl( dfltControl );
            }
            window.setVisible( true );
        }

        public void setParent( Component parent ) {
            plot2parent_ = parent;
        }
    }

    /**
     * Implementation of actions relating to hiding/revealing view windows
     * associated with some or all of the tables.
     */
    private class ShowAction extends BasicAction {

        final WindowEffect selEffect;
        final WindowEffect otherEffect;

        /**
         * Constructs a new action with particular effects for the selected
         * table and the others.
         *
         * @param  name  action name
         * @param  shortdesc  action short description
         * @param  selEffect  effect of action on the selected table
         *         (one of HIDE, REVEAL, NOOP)
         * @param  otherEffect  effect of action on the unselected tables
         *         (one of HIDE, REVEAL, NOOP)
         */
        ShowAction( String name, String shortdesc, WindowEffect selEffect, 
                    WindowEffect otherEffect ) {
            super( name, null, shortdesc );
            this.selEffect = selEffect;
            this.otherEffect = otherEffect;
        }

        public void actionPerformed( ActionEvent evt ) {
            int ntab = tablesModel_.getSize();
            for ( int i = 0; i < ntab; i++ ) {
                boolean isSelected = tablesList_.isSelectedIndex( i );
                TopcatModel tcModel = tablesModel_.getElementAt( i );
                Object effect = isSelected ? selEffect : otherEffect;
                if ( effect == WindowEffect.HIDE ) {
                    setViewsVisible( tcModel, false );
                }
                else if ( effect == WindowEffect.REVEAL ) {
                    setViewsVisible( tcModel, true );
                }
            }
        }
    }

    /**
     * Enumeration for use with ShowAction class.
     */
    private static class WindowEffect {
        final static WindowEffect HIDE = new WindowEffect();
        final static WindowEffect REVEAL = new WindowEffect();
        final static WindowEffect NOOP = null;
        private WindowEffect() {
        }
    }

    /**
     * Returns an array of actions concerned with hiding or revealing 
     * various table view windows.
     */
    private ShowAction[] makeShowActions() {
        return new ShowAction[] {
            new ShowAction( "Show Selected Views Only",
                            "Show viewer windows for selected table only",
                            WindowEffect.REVEAL, WindowEffect.HIDE ),
            new ShowAction( "Show Selected Views",
                            "Show viewer windows for selected table",
                            WindowEffect.REVEAL, WindowEffect.NOOP ),
            new ShowAction( "Show All Views",
                            "Show viewer windows of all tables",
                            WindowEffect.REVEAL, WindowEffect.REVEAL ),
            new ShowAction( "Hide Unselected Views",
                            "Hide viewer windows for tables except " +
                            "selected one",
                            WindowEffect.NOOP, WindowEffect.HIDE ),
            new ShowAction( "Hide Selected Views",
                            "Hide viewer windows for selected table",
                            WindowEffect.HIDE, WindowEffect.NOOP ),
            new ShowAction( "Hide All Views",
                            "Hide viewer windows for all tables",
                            WindowEffect.HIDE, WindowEffect.HIDE ),
        };
    }

    /**
     * Sets up a component so that it will act as a drag source for 
     * drag'n'drop actions, picking up the currently selected
     * table.
     *
     * @param  comp  the component to configure as an export source
     */
    private void configureExportSource( JComponent comp ) {
        MouseInputAdapter dragListener = new DragListener();
        comp.addMouseMotionListener( dragListener );
        comp.addMouseListener( dragListener );
        comp.setTransferHandler( exportTransferHandler_ );
    }

    /**
     * Utility method to turn a location string into a shorter version 
     * by stripping directory information etc.
     *
     * @param  label  original label
     * @return  possibly shortened version
     */
    private static String shorten( String label ) {
        if ( label.matches( ":([a-zA-Z0-9_-]+):(.*)" ) ) {
            // scheme syntax - leave it alone
        }
        else if ( label.matches( ".*[T:][0-9][0-9]:.*" ) ) {
            // ISO-8601 syntax - leave it alone
        }
        else {
            int sindex = label.lastIndexOf( '/' );
            if ( sindex < 0 || sindex == label.length() - 1 ) {
                sindex = label.lastIndexOf( '\\' );
            }
            if ( sindex < 0 || sindex == label.length() - 1 ) {
                sindex = label.lastIndexOf( ':' );
            }
            if ( sindex > 0 && sindex < label.length() - 1 ) {
                label = label.substring( sindex + 1 );
            }
        }
        if ( label.length() > 48 ) {
            label = label.substring( 0, 48 ) + "...";
        }
        return label;
    }

    /**
     * Returns an action which will launch a load dialogue of a particular
     * class.  These individual load dialogues are really owned by the
     * LoadWindow, which doesn't (and shouldn't) exist during
     * ControlWindow construction, so we have to jump through some hoops.
     */
    private Action
            createLoadDialogAction( final Class<? extends TableLoadDialog>
                                    tldClazz ) {

        /* Get a dialogue instance for the metadata: name, icon, description.
         * Throw it away after that.  Not ideal. */
        TableLoadDialog tld0;
        try {
            tld0 = tldClazz.getDeclaredConstructor().newInstance();
        }
        catch ( Throwable e ) {
            logger_.log( Level.WARNING, "Can't set up TAP action", e );
            return null;
        }

        /* Construct and return an action which will lazily acquire the
         * LoadWindow's copy of the relevant dialogue as required. */
        return new BasicAction( tld0.getName(), tld0.getIcon(),
                                tld0.getDescription() ) {
            Action act_;
            public void actionPerformed( ActionEvent evt ) {
                if ( act_ == null ) {
                    act_ = getLoadWindow().getDialogAction( tldClazz );
                }
                if ( act_ != null ) {
                    act_.actionPerformed( evt );
                }
            }
        };
    }

    /**
     * Actions which correspond to output of a table.
     */
    private class ExportAction extends BasicAction {

        ExportAction( String name, Icon icon, String shortdesc ) {
            super( name, icon, shortdesc );
        }

        public void actionPerformed( ActionEvent evt ) {
            TopcatModel tcModel = getCurrentModel();
            assert tcModel != null : "Action should be disabled!";
            StarTable table = tcModel.getApparentStarTable();
            if ( this == dupAct_ ) {
                addTable( table, "Copy of " + tcModel.getID(), true );
            }
            else if ( this == mirageAct_ ) {
                assert MirageHandler.isMirageAvailable();
                try {
                    MirageHandler.invokeMirage( table, null );
                }
                catch ( Exception e ) {
                    ErrorDialog.showError( window_, "Mirage Error", e );
                }
            }
        }
    }

    /**
     * Drag and drop handler class.
     */
    private class ControlTransferHandler extends TransferHandler {
        private boolean imprt;
        private boolean export;

        /**
         * Constructs a new TransferHandler.
         *
         * @param  imprt   whether handler should accept dropped tables
         * @param  export  whether handler should act as a drag source
         */
        ControlTransferHandler( boolean imprt, boolean export ) {
            this.imprt = imprt;
            this.export = export;
        }
        public int getSourceActions( JComponent comp ) {
            return ( export && getCurrentModel() != null ) ? COPY : NONE;
        }
        public boolean canImport( JComponent comp, DataFlavor[] flavs ) {
            return imprt && tabfact_.canImport( flavs );
        }
        public Icon getVisualRepresentation() {
            return ResourceIcon.TABLE;
        }
        protected Transferable createTransferable( JComponent comp ) {
            return taboutput_
                  .transferStarTable( TopcatUtils
                                     .getSaveTable( getCurrentModel() ) );
        }
        public boolean importData( JComponent comp, final Transferable trans ) {

            /* Use of loading tokens here is sort of the right thing to
             * do, but it doesn't help much, because this method is 
             * invoked from the AWT event dispatch thread, so you most
             * likely never see the visual effect of it (unless the 
             * randomise takes a long time). */
            final LoadingToken token = new LoadingToken( "Drag'n'drop" );
            addLoadingToken( token );
            StarTable table;
            try {

                /* Would like to put this in an external thread, but can't -
                 * the transferable becomes unusable (or at least may do)
                 * after this method executes.  Not sure if this could be
                 * improved by doing the drag'n'drop using custom code
                 * rather than the Swing classes. */
                table = tabfact_.makeStarTable( trans );
            }
            catch ( final Throwable e ) {
                table = null;
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        ErrorDialog.showError( window_, "Drop Error", e,
                                               "Table drop operation failed" );
                    }
                } );
                removeLoadingToken( token );
                return false;
            }
            final StarTable table0 = table;
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    try {
                        StarTable table1 = tabfact_.randomTable( table0 );
                        String loc = table1.getName();
                        loc = loc == null ? "dropped" : loc;
                        addTable( table1, loc, true );
                    }
                    catch ( final IOException e ) {
                        SwingUtilities.invokeLater( new Runnable() {
                            public void run() {
                                ErrorDialog.showError( window_, "I/O Error", e,
                                                      "Can't randomise table" );
                            }
                        } );
                    }
                    finally {
                        removeLoadingToken( token );
                    }
                }
            } );
            return true;
        }
    }

    /**
     * An instance of this class handles passing tables from a TableLoader
     * to a TableLoadClient, taking care of displaying progress in the
     * load window if loading takes a while.
     */
    private class LoadRunner {
        private final Icon icon_;
        private final TableLoadWorker worker_;
        private final Timer timer_;
        private volatile boolean workerAdded_;
        private volatile boolean loadFinished_;

        /**
         * Constructor.
         *
         * @param   loader  table source
         * @param   loadClient  table destination
         *                      (usually a {@link TopcatLoadClient})
         * @param   delay  number of milliseconds before the progress bar is
         *                 displayed
         */
        LoadRunner( TableLoader loader, TableLoadClient loadClient,
                    Icon icon, int delay ) {
            icon_ = icon;
            worker_ = new TableLoadWorker( loader, loadClient ) {
                protected void finish( boolean cancelled ) {
                    super.finish( cancelled );
                    loadFinished_ = true;
                    if ( timer_.isRunning() ) {
                        timer_.stop();
                    }
                    if ( workerAdded_ ) {
                        assert loadWindow_ != null;
                        loadWindow_.removeWorker( this );
                    }
                    else if ( loadWindow_ != null ) {
                        loadWindow_.conditionallyClose();
                    }
                }
            };
            timer_ = new Timer( delay, new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    if ( ! loadFinished_ ) {
                        getLoadWindow().addWorker( worker_, icon_ );
                        workerAdded_ = true;
                    }
                }
            } );
            timer_.setRepeats( false );
        }

        /**
         * Initiates the load.
         */
        public void start() {
            worker_.start();
            timer_.start();
        }
    }

    /**
     * Ensures that closing the control window is equivalent to shutting
     * down the application.
     */
    private class ControlWindowListener extends WindowAdapter {
        public void windowClosing( WindowEvent evt ) {
            exit( true );
        }
        public void windowClosed( WindowEvent evt ) {
            if ( ! exit( true ) ) {
                setVisible( true );
            }
        }
    }

    /**
     * Layout handler for info window.  GridBagLayouts are so horrible 
     * it's easiest to write this in its own class.
     */
    private static class InfoStack extends JPanel {
        GridBagLayout layer = new GridBagLayout();
        GridBagConstraints c1 = new GridBagConstraints();
        GridBagConstraints c2 = new GridBagConstraints();

        InfoStack() {
            setLayout( layer );
            c1.gridx = 0;
            c1.ipadx = 2;
            c1.ipady = 2;
            c1.anchor = GridBagConstraints.EAST;

            c2.gridx = 1;
            c2.ipadx = 2;
            c2.weightx = 1.0;
            c2.fill = GridBagConstraints.NONE;
            c2.gridwidth = GridBagConstraints.REMAINDER;
            c2.anchor = GridBagConstraints.WEST;
        }

        void addLine( String name, JComponent comp ) {
            c1.gridy++;
            c2.gridy++;

            addItem( new JLabel( name + ": " ), c1 );

            GridBagConstraints c2c = (GridBagConstraints) c2.clone();
            if ( comp instanceof JTextField ) {
                c2c.fill = GridBagConstraints.HORIZONTAL;
            }
            addItem( comp, c2c );
        }

        void addLine( String name, Component[] comps ) {
            Box compBox = Box.createHorizontalBox();
            for ( int i = 0; i < comps.length; i++ ) {
                if ( i > 0 ) {
                    compBox.add( Box.createHorizontalStrut( 5 ) );
                }
                compBox.add( comps[ i ] );
            }
            addLine( name, compBox );
        }

        void addGap() {
            c1.gridy++;
            c2.gridy++;
            addItem( Box.createVerticalStrut( 2 ),
                     (GridBagConstraints) c2.clone() );
        }

        void addItem( Component comp, GridBagConstraints c ) {
            layer.setConstraints( comp, c );
            add( comp );
        }

        void fillIn() {
            c1.gridy++;
            Component filler = Box.createHorizontalBox();
            c1.weighty = 1.0;
            layer.setConstraints( filler, c1 );
            add( filler );
        }
    }
}
