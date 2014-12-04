/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 * Copyright (C) 2007-2009 Science and Technology Facilities Council
 *
 *  History:
 *     11-NOV-2004 (Peter W. Draper):
 *       Original version.
 *     23-FEB-2012 (Margarida Castro Neves, mcneves@ari.uni-heidelberg.de)
 *       Added support for customized metadata parameters.  
 *       Ported SwingWorker from jsky.util.SwingWorker to javax.swing.SwingWorker
 *     08-OCT-2012 Redesigned new GUI
 */
package uk.ac.starlink.splat.vo;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.coords.Coordinates;
import jsky.coords.WorldCoords;
import jsky.util.SwingWorker;

import org.xml.sax.InputSource;

import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.HelpFrame;
import uk.ac.starlink.splat.iface.ProgressPanel;
import uk.ac.starlink.splat.iface.SpectrumIO;
import uk.ac.starlink.splat.iface.SpectrumIO.Props;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatCommunicator;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Transmitter;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.ProxySetup;
import uk.ac.starlink.util.gui.BasicFileChooser;
import uk.ac.starlink.util.gui.BasicFileFilter;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.GridBagLayouter;
import uk.ac.starlink.util.gui.ProxySetupFrame;
import uk.ac.starlink.vo.ResolverInfo;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.ParamElement;
import uk.ac.starlink.votable.TableElement;
import uk.ac.starlink.votable.VOElement;
import uk.ac.starlink.votable.VOElementFactory;
import uk.ac.starlink.votable.VOSerializer;
import uk.ac.starlink.votable.VOStarTable;
import uk.ac.starlink.votable.VOTableWriter;


/**
 * Display a page of controls for querying a list of  SSA servers and
 * displaying the results of those queries. The spectra returned can then be
 * selected and displayed in the main SPLAT browser.
 *
 * @author Peter W. Draper
 * @author Mark Taylor
 * @author Margarida Castro Neves 
 * @version $Id: SSAQueryBrowser.java 10547 2013-04-10 15:10:07Z mcneves $
 *
 */
public class SSAQueryBrowser
extends JFrame
implements ActionListener, MouseListener, DocumentListener, PropertyChangeListener
{
    // Logger.
    private static Logger logger =  Logger.getLogger( "uk.ac.starlink.splat.vo.SSAQueryBrowser" );

    /** UI preferences. */
    protected static Preferences prefs = 
            Preferences.userNodeForPackage( SSAQueryBrowser.class );

    /** Initial window size and location */
    private static final Rectangle defaultWindowLocation =
            new Rectangle( 0, 0, 800, 600 );

    /**
     * The object holding the list of servers that we should use for SSA queries.
     * @uml.property  name="serverList"
     * @uml.associationEnd  multiplicity="(1 1)"
     */
    private SSAServerList serverList = null;

    /**
     * The instance of SPLAT we're associated with.
     * @uml.property  name="browser"
     * @uml.associationEnd  multiplicity="(1 1)" inverse="ssapBrowser:uk.ac.starlink.splat.iface.SplatBrowser"
     */
    private SplatBrowser browser = null;

    /**
     * The SpecDataFactory.
     * @uml.property  name="specDataFactory"
     * @uml.associationEnd  multiplicity="(1 1)"
     */
    private SpecDataFactory specDataFactory = SpecDataFactory.getInstance();

    /**
     * File chooser used for saving and restoring queries.
     * @uml.property  name="fileChooser"
     * @uml.associationEnd  
     */
    protected BasicFileChooser fileChooser = null;

    /**
     * Content pane of frame
     * @uml.property  name="contentPane"
     * @uml.associationEnd  
     */
    protected JPanel contentPane = null;

    /**
     * Centre panel
     * @uml.property  name="centrePanel"
     * @uml.associationEnd  multiplicity="(1 1)"
     */
    protected JPanel centrePanel = null;
    /**
     * @uml.property  name="gbcentre"
     */
    protected GridBagConstraints gbcentre;

    /**
     * Servers panel
     * @uml.property  name="leftPanel"
     * @uml.associationEnd  multiplicity="(1 1)"
     */
    protected JPanel leftPanel = null;
  
    
    
    /**
     * Query panel
     * @uml.property  name="queryPanel"
     * @uml.associationEnd  
     */
    protected JPanel queryPanel = null;

    /**
     * Basic Query panel
     * @uml.property  name="basicQueryPanel"
     * @uml.associationEnd  
     */
    protected JPanel basicQueryPanel = null;

    /**
     * Customized Query panel
     * @uml.property  name="customQueryPanel"
     * @uml.associationEnd  
     */
    protected JPanel customQueryPanel = null;

    /**
     * Object name
     * @uml.property  name="nameField"
     * @uml.associationEnd  
     */
    protected JTextField nameField = null;

    /**
     * Resolve object name button
     * @uml.property  name="nameLookup"
     * @uml.associationEnd  
     */
    protected JButton nameLookup = null;

    /**
     * Download and display selected spectra
     * @uml.property  name="displaySelectedButton"
     * @uml.associationEnd  
     */
    protected JButton displaySelectedButton = null;

    /**
     * Download and display all spectra
     * @uml.property  name="displayAllButton"
     * @uml.associationEnd  
     */
    protected JButton displayAllButton = null;

    /**
     * Download selected spectra
     * @uml.property  name="downloadSelectedButton"
     * @uml.associationEnd  
     */
    protected JButton downloadSelectedButton = null;

    /**
     * Download all spectra
     * @uml.property  name="downloadAllButton"
     * @uml.associationEnd  
     */
    protected JButton downloadAllButton = null;

    /**
     * Deselect spectra in visible table
     * @uml.property  name="deselectVisibleButton"
     * @uml.associationEnd  
     */
    protected JButton deselectVisibleButton = null;

    /**
     * Deselect all spectra in all tables
     * @uml.property  name="deselectAllButton"
     * @uml.associationEnd  
     */
    protected JButton deselectAllButton = null;

    /**
     * Display basic search parameters
     * @uml.property  name="basicSearchButton"
     * @uml.associationEnd  readOnly="true"
     */
    protected JRadioButton  basicSearchButton;

    /**
     * Display extended search parameters
     * @uml.property  name="customSearchButton"
     * @uml.associationEnd  readOnly="true"
     */
    protected JRadioButton  customSearchButton;

    /** Display GET DATA  parameters and activation status */
//    protected JButton  getDataButton;
    
//    protected boolean getDataEnabled = false;
   
    /**
     * Display  DATA Link parameters and activation status
     * @uml.property  name="dataLinkButton"
     * @uml.associationEnd  
     */
    protected JButton  dataLinkButton;
    
    /**
     * @uml.property  name="dataLinkEnabled"
     */
    protected boolean dataLinkEnabled = false;
    
    /**
     * Make the query to all known servers
     * @uml.property  name="goButton"
     * @uml.associationEnd  
     */
    protected JButton goButton = null;
    
    /**
     * update/display query string
     * @uml.property  name="showQueryButton"
     * @uml.associationEnd  
     */
    JButton showQueryButton = null;

    /**
     * Allows user to customize search parameters
     * @uml.property  name="customButton"
     * @uml.associationEnd  
     */
    protected JButton customButton = null;

    /**
     * Central RA
     * @uml.property  name="raField"
     * @uml.associationEnd  
     */
    protected JTextField raField = null;

    /**
     * Central Dec
     * @uml.property  name="decField"
     * @uml.associationEnd  
     */
    protected JTextField decField = null;

    /**
     * Region radius
     * @uml.property  name="radiusField"
     * @uml.associationEnd  
     */
    protected JTextField radiusField = null;

    /**
     * Lower limit for BAND
     * @uml.property  name="lowerBandField"
     * @uml.associationEnd  
     */
    protected JTextField lowerBandField = null;

    /**
     * Upper limits for BAND
     * @uml.property  name="upperBandField"
     * @uml.associationEnd  
     */
    protected JTextField upperBandField = null;

    /**
     * Lower limit for TIME
     * @uml.property  name="lowerTimeField"
     * @uml.associationEnd  
     */
    protected JTextField lowerTimeField = null;

    /**
     * Upper limits for TIME
     * @uml.property  name="upperTimeField"
     * @uml.associationEnd  
     */
    protected JTextField upperTimeField = null;

    /**
     * The query text
     * @uml.property  name="queryText"
     * @uml.associationEnd  
     */
    protected JTextArea queryText = null;
    
    /**
     * The extended query text
     * @uml.property  name="extendedQueryText"
     */
    protected String extendedQueryText = null;
    

    /**
     * ButtonGroup for the format selection
     * @uml.property  name="formatGroup"
     * @uml.associationEnd  
     */
    protected ButtonGroup formatGroup = null;
    /**
     * @uml.property  name="formatList"
     * @uml.associationEnd  
     */
    protected JComboBox formatList = null;

    /**
     * ButtonGroup for the FLUXCALIB selection
     * @uml.property  name="fluxCalibGroup"
     * @uml.associationEnd  
     */
    protected ButtonGroup fluxCalibGroup = null;
    /**
     * @uml.property  name="flcalibList"
     * @uml.associationEnd  
     */
    protected JComboBox flcalibList = null;
    

    /**
     * ButtonGroup for the WAVECALIB selection
     * @uml.property  name="waveCalibGroup"
     * @uml.associationEnd  
     */
    protected ButtonGroup waveCalibGroup = null;
    /**
     * @uml.property  name="wlcalibList"
     * @uml.associationEnd  
     */
    protected JComboBox wlcalibList = null;

    /**
     * Tabbed pane showing the query results tables
     * @uml.property  name="resultsPane"
     * @uml.associationEnd  
     */
    protected JTabbedPane resultsPane = null;

    /**
     * The list of StarJTables in use
     * @uml.property  name="starJTables"
     * @uml.associationEnd  multiplicity="(0 -1)" elementType="uk.ac.starlink.table.gui.StarJTable"
     */
    protected ArrayList<StarJTable> starJTables = null;

    /**
     * NED name resolver catalogue
     * @uml.property  name="nedCatalogue"
     * @uml.associationEnd  
     */
    protected SkycatCatalog nedCatalogue = null;

    /**
     * SIMBAD name resolver catalogue
     * @uml.property  name="simbadCatalogue"
     * @uml.associationEnd  
     */
    protected SkycatCatalog simbadCatalogue = null;

    /**
     * The current name resolver, if using Skycat method
     * @uml.property  name="resolverCatalogue"
     * @uml.associationEnd  
     */
    protected SkycatCatalog resolverCatalogue = null;

    /**
     * The proxy server dialog
     * @uml.property  name="proxyWindow"
     * @uml.associationEnd  
     */
    protected ProxySetupFrame proxyWindow = null;

    /** The SSA servers window */
   // protected SSAServerFrame serverWindow = null;

    /**
     * The Button for adding optional parameters to the query
     * @uml.property  name="selectAllParamsButton"
     * @uml.associationEnd  
     */
    protected JButton selectAllParamsButton = null;
    /**
     * The Button for removing optional parameters from the optional parameters list
     * @uml.property  name="selectAllParamsButton"
     * @uml.associationEnd  
     */
    protected JButton deselectAllParamsButton = null;
    
    /** The list of all input parameters read from the servers */
   // protected static SSAMetadataFrame metaFrame = null;
    protected static SSAMetadataPanel metaPanel = null;

    /** Make sure the proxy environment is setup */
    static {
        ProxySetup.getInstance().restore();
    }

    /* The Query text that will be displayed */
    /**
     * @uml.property  name="queryLine"
     * @uml.associationEnd  
     */
    private SSAQuery queryLine;
    
    /** The list of all input parameters read from the servers as a hash map */
    private static HashMap<String, MetadataInputParameter> metaParam=null;  
    
    /** The list of all input parameters read from the servers as a hash map */
    private static HashMap< JTextField, String > queryMetaParam=null; 
    
    
    /** the authenticator for access control **/
    private static SSAPAuthenticator authenticator;
    
    /** two way mapping servers-metadata parameters **/
    private static ServerParamRelation serverParam;
    
    /**
     * the serverlist as a tree
     * @uml.property  name="tree"
     * @uml.associationEnd  
     */
    private SSAServerTree tree;
    
    /**
     * @uml.property  name="isLookup"
     */
    private boolean isLookup = false;
    /**
     * @uml.property  name="getDataSelection"
     */
    private boolean getDataSelection = false;
   
    static ProgressPanelFrame progressFrame = null;

    /**
     * @uml.property  name="getDataFrame"
     * @uml.associationEnd  
     */
    private GetDataQueryFrame getDataFrame = null;
    /**
     * @uml.property  name="dataLinkFrame"
     * @uml.associationEnd  
     */
    private DataLinkQueryFrame dataLinkFrame = null;


    /**
     * Create an instance.
     */
    public SSAQueryBrowser( SSAServerList serverList, SplatBrowser browser )
    {
        this.serverList = serverList;
        this.browser = browser;
        
        
        authenticator = new SSAPAuthenticator();
        Authenticator.setDefault(authenticator);
        specDataFactory.setAuthenticator(authenticator);
        serverParam = new ServerParamRelation();
        queryMetaParam = new HashMap< JTextField, String >();
        if (serverList != null) customParameters();
       
       
        initUI();
        this.pack();
      
        this.setVisible(true);
        initMenusAndToolbar();
        initFrame(); 
        
    }

    public SSAPAuthenticator getAuthenticator() {
        return authenticator;
    }
    /**
     * Create and display the UI components.
     */
    private void initUI()
    {
       
        JPanel contentPane = (JPanel) getContentPane();
      
        contentPane.setPreferredSize(new Dimension(800,720));
        contentPane.setMinimumSize(new Dimension(600,400));
   //     contentPane.setLayout( new BoxLayout(contentPane, BoxLayout.X_AXIS) );
     //   JTabbedPane tabPane = new JTabbedPane();
                
        JSplitPane splitPanel = new JSplitPane();
        splitPanel.setOneTouchExpandable(true);
        splitPanel.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        splitPanel.setDividerLocation(0.3);
      
       
        this.add(splitPanel);
        leftPanel = new JPanel( );
   
        initServerComponents();
    //    tabPane.addTab("Server selection", leftPanel);
      
        centrePanel = new JPanel( new GridBagLayout() );
        centrePanel.setMinimumSize(new Dimension(400,200));
        gbcentre=new GridBagConstraints();
        gbcentre.anchor=GridBagConstraints.NORTHWEST;
        gbcentre.gridx=0;
        gbcentre.gridy=0;
        gbcentre.weightx=1;
        gbcentre.fill=GridBagConstraints.HORIZONTAL;
         initQueryComponents();
         gbcentre.gridy=1;
         gbcentre.weightx=1;
         gbcentre.weighty=1;
         gbcentre.fill=GridBagConstraints.BOTH;
         initResultsComponent();
         
         setDefaultNameServers();
      //   tabPane.addTab("Query", centrePanel);
         
     //    tabPane.setSelectedComponent(centrePanel);
         splitPanel.setLeftComponent(leftPanel); // the server selection area
         splitPanel.setRightComponent(centrePanel); // the query area
        
         
         contentPane.add(splitPanel);
     
    }


    public void initServerComponents()
    {
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setAlignmentY((float) 1.);

        tree=new SSAServerTree( serverList, serverParam );   
        leftPanel.add(tree);
    }
    
    
    /**
     * Initialise the menu bar, action bar and related actions.
     */
    private void initMenusAndToolbar()
    {
        //  Add the menuBar.
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar( menuBar );

        //  Create the toolbar.
       // ToolButtonBar toolBar = new ToolButtonBar( contentPane );
        JPanel actionBarContainer = new JPanel();

        //  Get icons.
        ImageIcon closeImage =
                new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon saveImage =
                new ImageIcon( ImageHolder.class.getResource( "savefile.gif" ) );
        ImageIcon readImage =
                new ImageIcon( ImageHolder.class.getResource( "openfile.gif" ) );
        ImageIcon helpImage =
                new ImageIcon( ImageHolder.class.getResource( "help.gif" ) );
        ImageIcon ssaImage =
                new ImageIcon( ImageHolder.class.getResource("ssapservers.gif") );

        //  Create the File menu.
        JMenu fileMenu = new JMenu( "File" );
        fileMenu.setMnemonic( KeyEvent.VK_F );
        menuBar.add( fileMenu );

        //  Add options to save and restore the query result.
        LocalAction saveAction = new LocalAction( LocalAction.SAVE,
                "Save query results",
                saveImage,
                "Save results of query " +
                "to disk file" );

        fileMenu.add( saveAction );
        JButton saveButton = new JButton( saveAction );
        actionBarContainer.add( saveButton );

        LocalAction readAction = new LocalAction( LocalAction.READ,
                "Restore query results",
                readImage,
                "Read results of a " +
                        "previous query back " +
                "from disk file" );
        fileMenu.add( readAction );
        JButton readButton = new JButton( readAction );
        actionBarContainer.add( readButton );

        //  Add an action to close the window.
        LocalAction closeAction = new LocalAction( LocalAction.CLOSE,
                "Close", closeImage,
                "Close window",
                "control W" );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        JButton closeButton = new JButton( closeAction );
        actionBarContainer.add( closeButton );

        //  Create the options menu.
        JMenu optionsMenu = new JMenu( "Options" );
        optionsMenu.setMnemonic( KeyEvent.VK_O );
        menuBar.add( optionsMenu );

        LocalAction proxyAction = new LocalAction( LocalAction.PROXY,
                "Configure connection " +
                "proxy..." );
        optionsMenu.add( proxyAction );

 /*       //  Add item to control the use of SSA servers.
        LocalAction serverAction = new LocalAction(LocalAction.SERVER,
                "Configure SSAP servers...",
                ssaImage,
                "Configure SSAP servers" );
        optionsMenu.add( serverAction );
    //    toolBar.add( serverAction );

        //  SSAP version 1 format control, wavelength calibation and
        //  flux calibration options.
        initFormatOptions( optionsMenu );
        initWaveCalibOptions( optionsMenu );
        initFluxCalibOptions( optionsMenu );
*/
        //  Create a menu containing all the name resolvers.
        JMenu resolverMenu = new JMenu( "Resolver" );
        resolverMenu.setMnemonic( KeyEvent.VK_R );
        menuBar.add( resolverMenu );

        ButtonGroup bg = new ButtonGroup();

        JRadioButtonMenuItem jrbmi = new JRadioButtonMenuItem();
        resolverMenu.add( jrbmi );
        jrbmi.setSelected( false );
        bg.add( jrbmi );
        jrbmi.setAction( new ResolverAction( "SIMBAD via CADC",
                simbadCatalogue ) );
        jrbmi.setToolTipText( "SIMBAD service served by CADC" );

        jrbmi = new JRadioButtonMenuItem();
        resolverMenu.add( jrbmi );
        jrbmi.setSelected( false );
        bg.add( jrbmi );
        jrbmi.setAction( new ResolverAction( "NED via ESO", nedCatalogue ) );
        jrbmi.setToolTipText( "NED catalogue served by ESO" );

        jrbmi = new JRadioButtonMenuItem();
        resolverMenu.add( jrbmi );
        jrbmi.setSelected( true );
        bg.add( jrbmi );
        jrbmi.setAction( new ResolverAction( "CDS Sesame", null  ) );
        resolverCatalogue = null;
        jrbmi.setToolTipText
        ( "CDS Sesame service queries SIMBAD, NED and Vizier" );


        //  Create a menu for inter-client communications.
        JMenu interopMenu = new JMenu( "Interop" );
        interopMenu.setMnemonic( KeyEvent.VK_I );
        menuBar.add( interopMenu );

        //  Need an actions to transmit and broadcast the current
        //  results table.
        SplatCommunicator communicator = browser.getCommunicator();

        // Add table transmit options.
        Transmitter transmitter = communicator.createTableTransmitter( this );
        interopMenu.add( transmitter.getBroadcastAction() )
        .setMnemonic( KeyEvent.VK_B );
        interopMenu.add( transmitter.createSendMenu() )
        .setMnemonic( KeyEvent.VK_T );

        //  Create the Help menu.
        HelpFrame.createButtonHelpMenu( "ssa-window", "Help on window", menuBar, null /*toolBar*/ );

        //  ActionBar goes at bottom.
        //contentPane.add( actionBarContainer, BorderLayout.SOUTH );
        gbcentre.anchor=GridBagConstraints.SOUTHWEST;
        gbcentre.gridy=2;
        gbcentre.weighty=0;
        gbcentre.fill=GridBagConstraints.HORIZONTAL;
        centrePanel.add( actionBarContainer, gbcentre );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    private void initFrame()
    {
        setTitle( Utilities.getTitle( "Query VO for Spectra" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        Utilities.setFrameLocation( this, defaultWindowLocation, prefs,
                "SSAQueryBrowser" );
        setVisible( true );
    }

    /**
     * Populate the north part of center window with the basic query
     * components.
     */
    private void initQueryComponents()
    {
        queryLine = new SSAQuery("<SERVER>");
        queryPanel = new JPanel();
        JPanel queryParamPanel = new JPanel();
        queryPanel.setBorder ( BorderFactory.createTitledBorder( "Search parameters:" ) );
        queryPanel.setLayout( new GridBagLayout());
       // customScrollPanel = new JScrollPane( customQueryPanel );
        queryParamPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill=GridBagConstraints.BOTH;
        c.anchor=GridBagConstraints.NORTHWEST;
        c.weightx=.5;
        c.weighty=1.;
        c.gridx = 0;
        c.gridy = 0;
        
        JPanel simpleQueryPanel = new JPanel();
        simpleQueryPanel.setLayout(new BorderLayout());
        simpleQueryPanel.setBorder(BorderFactory.createTitledBorder("Simple Query"));
      
        queryParamPanel.add(simpleQueryPanel, c);
        
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 0;
       
        JPanel optionalQueryPanel = new JPanel();
        optionalQueryPanel.setBorder(BorderFactory.createTitledBorder("Optional Parameters"));
  //      optionalQueryPanel.setPreferredSize(new Dimension(250,210));
        queryParamPanel.add(optionalQueryPanel, c);
        
        
        // The simple query panel
        //------------------------
           
       // GridBagLayouter layouter =  new GridBagLayouter( queryPanel, GridBagLayouter.SCHEME3 /*SCHEME4*/ );
        GridBagLayouter layouter =  new GridBagLayouter( simpleQueryPanel, GridBagLayouter.SCHEME4 /*SCHEME4*/ );
       // GridBagLayouter layouter2 =  new GridBagLayouter( simpleQuery2/*Panel*/, GridBagLayouter.SCHEME4 /*SCHEME4*/ );


        //  Object name. Arrange for a resolver to look up the coordinates of
        //  the object name, when return or the lookup button are pressed.
        JLabel nameLabel = new JLabel( "Object:" );
        nameField = new JTextField( 10 );
        nameField.setFocusable(true);
        nameField.setToolTipText( "Enter the name of an object " +
                " -- press return to get coordinates" );
        nameField.addActionListener( this );
        nameField.getDocument().putProperty("owner", nameField); //set the owner
        nameField.getDocument().addDocumentListener( this );
      //  nameField.addMouseListener( this );
        
        nameLookup = new JButton( "Lookup" );
        nameLookup.addActionListener( this );
        nameLookup.setToolTipText( "Press to get coordinates of Object" );
        
        JPanel objPanel = new JPanel( new GridBagLayout());
        GridBagConstraints gbcs = new GridBagConstraints();
        gbcs.weightx = 1.0;
        gbcs.fill = GridBagConstraints.HORIZONTAL;
        gbcs.ipadx = 15;
        objPanel.add(nameField, gbcs);
        objPanel.add(nameLookup, gbcs);
        layouter.add( nameLabel, false );
     
        layouter.add(objPanel, true);
 
        layouter.eatLine();

       
        //  RA and Dec fields. We're free-formatting on these (decimal degrees
        //  not required).

        JLabel raLabel = new JLabel( "RA:" );
        raField = new JTextField( 10 );
        raField.addActionListener(this);
        raField.getDocument().putProperty("owner", raField); //set the owner
        raField.getDocument().addDocumentListener( this );

        JLabel decLabel = new JLabel( "Dec:" );
        decField = new JTextField( 10 );
        decField.addActionListener(this);
        decField.getDocument().putProperty("owner", decField); //set the owner
        decField.getDocument().addDocumentListener( this );

        JPanel posPanel = new JPanel( new GridBagLayout() );
        GridBagConstraints gbc = new GridBagConstraints();
     
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        posPanel.add( raField, gbc );
        gbc.weightx=0.0;
        gbc.fill = GridBagConstraints.NONE;
        posPanel.add(decLabel, gbc);
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        posPanel.add(decField, gbc);
        
        layouter.add( raLabel, false );
        layouter.add(posPanel,true);
        decField.setToolTipText( "Enter the Dec of field centre, decimal degrees or dd:mm:ss.ss" );
        raField.setToolTipText( "Enter the RA of field centre, decimal degrees or hh:mm:ss.ss" );


        //  Radius field.
        JLabel radiusLabel = new JLabel( "Radius:" );
        radiusField = new JTextField( "10.0", 10 );
        queryLine.setRadius(10.0);
        radiusField.addActionListener( this );
        layouter.add( radiusLabel, false );
        layouter.add( radiusField, true );
        radiusField.setToolTipText( "Enter radius of field to search" +
                " from given centre, arcminutes" );
        radiusField.addActionListener( this );
        radiusField.getDocument().putProperty("owner", radiusField); //set the owner
        radiusField.getDocument().addDocumentListener( this );


        //  Band fields.
        JLabel bandLabel = new JLabel( "Band:" );
        lowerBandField = new JTextField( 8 );
        lowerBandField.addActionListener( this );
        lowerBandField.getDocument().putProperty("owner", lowerBandField); //set the owner
        lowerBandField.getDocument().addDocumentListener( this );

        upperBandField = new JTextField( 8 );
        upperBandField.addActionListener( this );
        upperBandField.getDocument().putProperty("owner", upperBandField); //set the owner
        upperBandField.getDocument().addDocumentListener( this );


        JPanel bandPanel = new JPanel( new GridBagLayout() );
        GridBagConstraints gbc2 = new GridBagConstraints();

        gbc2.weightx = 1.0;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        bandPanel.add( lowerBandField, gbc2 );

     
        gbc2.weightx = 0.0;
        gbc2.fill = GridBagConstraints.NONE;
        bandPanel.add( new JLabel( "/" ), gbc2 );

 
        gbc2.weightx = 1.0;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        bandPanel.add( upperBandField, gbc2 );

        layouter.add( bandLabel, false );
        layouter.add( bandPanel, true );
        lowerBandField.setToolTipText( "Lower limit, or single include " +
                "value, for spectral band, in meters" );
        upperBandField.setToolTipText
        ( "Upper limit for spectral band, in meters" );

        //  Time fields, note this shares a line with the band fields.
        JLabel timeLabel = new JLabel( "Time:" );
        lowerTimeField = new JTextField( 8 );
        lowerTimeField.addActionListener( this );
        lowerTimeField.getDocument().putProperty("owner", lowerTimeField); //set the owner
        lowerTimeField.getDocument().addDocumentListener( this );

        upperTimeField = new JTextField( 8 );
        upperTimeField.addActionListener( this );
        upperTimeField.getDocument().putProperty("owner", upperTimeField); //set the owner
        upperTimeField.getDocument().addDocumentListener( this );

        JPanel timePanel = new JPanel( new GridBagLayout() );

        GridBagConstraints gbc4 = new GridBagConstraints();
        gbc4.weightx = 1.0;
        gbc4.fill = GridBagConstraints.HORIZONTAL;
        timePanel.add( lowerTimeField, gbc4 );

        GridBagConstraints gbc5 = new GridBagConstraints();
        gbc5.weightx = 0.0;
        gbc5.fill = GridBagConstraints.NONE;
        timePanel.add( new JLabel( "/" ), gbc5 );

        GridBagConstraints gbc6 = new GridBagConstraints();
        gbc6.weightx = 1.0;
        gbc6.fill = GridBagConstraints.HORIZONTAL;
        timePanel.add( upperTimeField, gbc6 );

        layouter.add( timeLabel, false );
        layouter.add( timePanel, true );
        lowerTimeField.setToolTipText( "Lower limit, or single include " +
                "value for time coverage, " +
                "ISO 8601 format " +
                "(e.g 2008-10-15T20:48Z)" );
        upperTimeField.setToolTipText( "Upper limit for time coverage, " +
                "in ISO 8601 format " +
                "(e.g 2008-10-15T20:48Z)" );
        
        //
        // format and calibration options:
        JPanel calibOptions = new JPanel(new GridLayout(3,2));
   //     calibOptions.setPreferredSize(new Dimension(100,200));
        // Formats
        String[] formats =  { "None", "ALL", "COMPLIANT", "votable", "fits", "xml", "native" };
        formatList = new JComboBox(formats);
        formatList.setSelectedIndex(0);
        formatList.addActionListener(this);
        calibOptions.add(new JLabel("Query Format:"));
        
        calibOptions.add( formatList, true);
        
        //Wavelength Calibration
        String[] wlcalibs = { "None", "any", "absolute", "relative" };
        wlcalibList = new JComboBox(wlcalibs);      
        wlcalibList.setSelectedIndex(0);
        wlcalibList.addActionListener(this);
        calibOptions.add(new JLabel( "Wavelength calibration:"), false);
       
        calibOptions.add( wlcalibList, true);
        
        //Flux Calibration
        String[] flcalibs = { "None", "any", "absolute", "relative", "normalized" };
        flcalibList = new JComboBox(flcalibs);
        flcalibList.setSelectedIndex(0);
        flcalibList.addActionListener(this);
        calibOptions.add(new JLabel( "Flux calibration:"), false);
        calibOptions.add( flcalibList, true);
        
        layouter.add(calibOptions, true);
        //simpleQueryPanel.add(calibOptions);
        
        layouter.eatSpare();
        
     //   simpleQueryPanel.add(simpleQuery1, BorderLayout.LINE_START);
     //   simpleQueryPanel.add(calibOptions, BorderLayout.LINE_END);
        
        // The optional query parameter (metadata) Panel
        //----------------------------------------------
     
        optionalQueryPanel.setLayout( new BoxLayout(optionalQueryPanel, BoxLayout.PAGE_AXIS) );
       metaPanel = new SSAMetadataPanel();
       metaPanel.addPropertyChangeListener(this);
       //showParameters();
      // metaPanel.setVisible(false);
      // layouter.add(metaPanel, true);
    //   metaPanel.setTableWidth(200);
       optionalQueryPanel.add(metaPanel);//, BorderLayout.NORTH);
       showParameters();
    //   queryTextPanel.add(new JLabel("Query:"));
        JPanel paramButtonsPanel = new JPanel( );
        selectAllParamsButton = new JButton("Select all");
        selectAllParamsButton.addActionListener( this );
       // selectAllParamsButton.setToolTipText("Add optional parameters");
        selectAllParamsButton.setMargin(new Insets(2,2,2,2));  
       // optionalQueryPanel.add(selectAllParamsButton, BorderLayout.WEST);
        deselectAllParamsButton = new JButton("Deselect all");
        deselectAllParamsButton.addActionListener( this );
     //   deselectAllParamsButton.setToolTipText("Remove selected parameters");
        deselectAllParamsButton.setMargin(new Insets(2,2,2,2));  
    //    optionalQueryPanel.add(deselectAllParamsButton, BorderLayout.EAST);
        paramButtonsPanel.add( selectAllParamsButton );
        paramButtonsPanel.add( deselectAllParamsButton );
        optionalQueryPanel.add(paramButtonsPanel, BorderLayout.SOUTH);
    
         
        // the query string display Panel
        //-------------------------------
        goButton = new JButton( "    SEND QUERY    " );
        goButton.setBackground(Color.green);
        goButton.addActionListener( this );
      
  //      showQueryButton = new JButton( "Query: ");
  //      showQueryButton.setToolTipText("show/update query string");
  //      showQueryButton.addActionListener( this );
        JPanel sendQueryPanel = new JPanel(new BorderLayout());
        sendQueryPanel.add(new JLabel("Query:"), BorderLayout.LINE_START);
  //      sendQueryPanel.add(showQueryButton, BorderLayout.LINE_START);
        queryText = new JTextArea(2,25);
        JScrollPane queryScroller = new JScrollPane();
        queryScroller.add(queryText);
     //   queryScroller.setV
        queryText.setEditable(true);
        
        sendQueryPanel.add(queryText);
        queryText.setLineWrap(true);     
        sendQueryPanel.add(goButton, BorderLayout.LINE_END);
       
        c.fill=GridBagConstraints.BOTH;
        c.anchor=GridBagConstraints.NORTHWEST;
        c.weighty=.5;
        c.gridx = 0;
        c.gridy = 0;
        
        queryPanel.add(queryParamPanel, c);
        c.gridy=1;
        queryPanel.add( sendQueryPanel, c);
       
        centrePanel.add( queryPanel, gbcentre );
       
        
        // add query text to query text area
        updateQueryText();
        
    }

    /**
     * Make the results component. This is mainly JTabbedPane containing a
     * JTable for each set of results (the tables are realized later) and
     * a button to display the selected spectra.
     */
    private  void initResultsComponent()
    {
        JPanel resultsPanel = new JPanel( );
        resultsPanel.setLayout(new GridBagLayout());
        resultsPanel.setBorder ( BorderFactory.createTitledBorder( "Query results:" ) );
        resultsPanel.setToolTipText( "Results of query to the current list of SSAP servers. One table per server" );
     
        GridBagConstraints gbc=new GridBagConstraints();
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.weighty=1;
        gbc.weightx=1;
        gbc.anchor=GridBagConstraints.NORTHWEST;
        gbc.fill=GridBagConstraints.BOTH;
        resultsPane = new JTabbedPane();
//        resultsPane.setPreferredSize(new (600,310));
        resultsPanel.add( resultsPane , gbc);
        resultsPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {//XXXXX
                if ( dataLinkEnabled ) {
                   if (resultsPane.getIconAt(resultsPane.getSelectedIndex())!=null) { // it's a datalink service

                        if (dataLinkFrame != null && dataLinkEnabled) {
                            dataLinkFrame.setServer(resultsPane.getTitleAt(resultsPane.getSelectedIndex()));
                            dataLinkFrame.setVisible(true);
                        } else 

                            if (getDataFrame != null && dataLinkEnabled) {
                                getDataFrame.setService(resultsPane.getTitleAt(resultsPane.getSelectedIndex()));
                            } 

                    } else {
                        dataLinkFrame=null;
                        getDataFrame=null;
                    }
                }
            }
        });
        
     
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcontrol = new GridBagConstraints();
        gbcontrol.gridx=0;
        gbcontrol.gridy=0;
        gbcontrol.weightx=1;
        gbcontrol.weighty=0;
        gbcontrol.fill = GridBagConstraints.HORIZONTAL;

        //  Download and display.
        displaySelectedButton = new JButton( "<html>Display<BR> selected</html>" );
        displaySelectedButton.addActionListener( this );
        displaySelectedButton.setMargin(new Insets(1, 10, 1, 10));  
        displaySelectedButton.setToolTipText
        ( "Download and display all spectra selected in all tables" );
        controlPanel.add( displaySelectedButton,gbcontrol );


        displayAllButton = new JButton( "<html>Display<BR>all</html>" );
        displayAllButton.addActionListener( this );
        displayAllButton.setMargin(new Insets(1,10,1,10));  
        displayAllButton.setToolTipText
        ( "Download and display all spectra in all tables" );
        gbcontrol.gridx=1;
        controlPanel.add( displayAllButton, gbcontrol );

        //  Just download.
        downloadSelectedButton = new JButton( "<html>Download<BR>selected</html>" );
        downloadSelectedButton.addActionListener( this );
        downloadSelectedButton.setMargin(new Insets(1,10,1,10));  
        downloadSelectedButton.setToolTipText
        ( "Download all spectra selected in all tables");
        gbcontrol.gridx=2;
        controlPanel.add( downloadSelectedButton, gbcontrol );
      

        downloadAllButton = new JButton( "<html>Download<BR> all</html>" );
        downloadAllButton.addActionListener( this );
        downloadAllButton.setMargin(new Insets(1,10,1,10));  
        downloadAllButton.setToolTipText
        ( "Download all spectra in all tables");
        gbcontrol.gridx=3;
        controlPanel.add( downloadAllButton , gbcontrol);


        //  Deselect
        deselectVisibleButton = new JButton( "<html>Deselect<br>table</html>" );
        deselectVisibleButton.addActionListener( this );
        deselectVisibleButton.setMargin(new Insets(1,10,1,10));  
        deselectVisibleButton.setToolTipText
        ( "Deselect all spectra in displayed table" );
      //  controlPanel2.add( deselectVisibleButton );
        gbcontrol.gridx=4;
        controlPanel.add( deselectVisibleButton, gbcontrol );


        deselectAllButton = new JButton( "<html>Deselect <BR>all</html>" );
        deselectAllButton.addActionListener( this );
        deselectAllButton.setMargin(new Insets(1,10,1,10));  
        deselectAllButton.setToolTipText
        ( "Deselect all spectra in all tables" );
     //   controlPanel2.add( deselectAllButton );
        gbcontrol.gridx=5;
        controlPanel.add( deselectAllButton , gbcontrol);
/*
        getDataButton = new JButton( "<html>GET<BR> DATA</html>" );
        getDataButton.addActionListener( this );
        getDataButton.setMargin(new Insets(1,10,1,10));  
        getDataButton.setToolTipText ( "Server-side processing parameters" );
        getDataButton.setEnabled(false);
        getDataButton.setVisible(false);
     //   controlPanel2.add( deselectAllButton );
        gbcontrol.gridx=6;
        controlPanel.add( getDataButton, gbcontrol );
 */     
        dataLinkButton = new JButton( "<html>DataLink<BR>Services</html>" );
        dataLinkButton.addActionListener( this );
        dataLinkButton.setMargin(new Insets(1,10,1,10));  
        dataLinkButton.setToolTipText ( "DataLink parameters" );
        dataLinkButton.setEnabled(false);
        dataLinkButton.setVisible(false);
     //   controlPanel2.add( deselectAllButton );
        gbcontrol.gridx=6;
        controlPanel.add( dataLinkButton, gbcontrol );
        gbc.gridx=0;
        gbc.gridy=1;
        gbc.weighty=0;
        gbc.anchor = GridBagConstraints.PAGE_END;
        gbc.fill=GridBagConstraints.HORIZONTAL;
        resultsPanel.add( controlPanel, gbc );
        centrePanel.add( resultsPanel, gbcentre );
     
    }

    /**
     * Initialise the SSAP version 1 data formats. Don't want
     * one of these by default.
     */
    protected void initFormatOptions( JMenu optionsMenu )
    {
        JMenu formatMenu = new JMenu( "Query format" );
        String[] names =
            { "None", "ALL", "COMPLIANT", "votable", "fits", "xml", "native" };
        JRadioButtonMenuItem item;
        formatGroup = new ButtonGroup();
        for ( int i = 0; i < names.length; i++ ) {
            item = new JRadioButtonMenuItem( names[i] );
            if ( i == 0 ) {
                item.setSelected( true );
            }
            item.setActionCommand( names[i] );
            formatGroup.add( item );
            formatMenu.add( item );
        }
        optionsMenu.add( formatMenu );
        this.pack();
    }

    /**
     * Initialise the SSAP version 1 wavecalib formats. Don't want
     * one of these by default.
     */
    protected void initWaveCalibOptions( JMenu optionsMenu )
    {
        JMenu calibMenu = new JMenu( "Wavelength calibration" );
        String[] names =
            { "None", "any", "absolute", "relative" };
        JRadioButtonMenuItem item;
        waveCalibGroup = new ButtonGroup();
        for ( int i = 0; i < names.length; i++ ) {
            item = new JRadioButtonMenuItem( names[i] );
            if ( i == 0 ) {
                item.setSelected( true );
            }
            item.setActionCommand( names[i] );
            waveCalibGroup.add( item );
            calibMenu.add( item );
        }
        optionsMenu.add( calibMenu );
    }

    /**
     * Initialise the SSAP version 1 fluxcalib formats. Don't want
     * one of these by default.
     */
    protected void initFluxCalibOptions( JMenu optionsMenu )
    {
        JMenu calibMenu = new JMenu( "Flux calibration" );
        String[] names =
            { "None", "any", "absolute", "relative", "normalized" };
        JRadioButtonMenuItem item;
        fluxCalibGroup = new ButtonGroup();
        for ( int i = 0; i < names.length; i++ ) {
            item = new JRadioButtonMenuItem( names[i] );
            if ( i == 0 ) {
                item.setSelected( true );
            }
            item.setActionCommand( names[i] );
            fluxCalibGroup.add( item );
            calibMenu.add( item );
        }
        optionsMenu.add( calibMenu );
    }

    /**
     * Arrange to resolve the object name into coordinates.
     */
    protected void resolveName()
    {
        String name = nameField.getText().trim();
        if ( name != null && name.length() > 0 ) {

            QueryArgs qargs = null;
            if ( resolverCatalogue != null ) {
                //  Skycat resolver.
                qargs = new BasicQueryArgs( resolverCatalogue );

                // If objectName has spaces we should protect them.
                name = name.replaceAll( " ", "%20" );
                qargs.setId( name );
            }
            final QueryArgs queryArgs = qargs;
            final String objectName = name;

            Thread thread = new Thread( "Name server" )
            {
                public void run()
                {
                    try {
                        if ( queryArgs == null ) {
                            ResolverInfo info =
                                    ResolverInfo.resolve( objectName );
                            WorldCoords coords =
                                    new WorldCoords( info.getRaDegrees(),
                                            info.getDecDegrees() );
                            String[] radec = coords.format();
                            isLookup=true;
                            raField.setText( radec[0] );
                            decField.setText( radec[1] );
                            nameField.setForeground(Color.black);
                            isLookup=false;
                         //   queryLine.setPosition(radec[0], radec[1]);
                        }
                        else {
                            QueryResult r =
                                    resolverCatalogue.query( queryArgs );
                            if ( r instanceof TableQueryResult ) {
                                Coordinates coords =
                                        ((TableQueryResult) r).getCoordinates( 0 );
                                if ( coords instanceof WorldCoords ) {
                                    //  Enter values into RA and Dec fields.
                                    String[] radec =
                                            ((WorldCoords) coords).format();
                                    raField.setText( radec[0] );
                                    decField.setText( radec[1] );
                                    // 
                           //         queryLine.setPosition(radec[0], radec[1]);
                                }
                            }
                        }
                    }
                    catch (Exception e) {
                        ErrorDialog.showError( null, e );
                    }
                }
            };

            //  Start the nameserver.
            raField.setText( "" );
            decField.setText( "" );
            thread.start();
        }
    }

    /**
     * Setup the default name servers (SIMBAD and NED) to use to resolve
     * astronomical object names. Note these are just those used in JSky.
     * A better implementation should reuse the JSky classes.
     * <p>
     * XXX refactor these into an XML file external to the application.
     * Maybe switch to the CDS Sesame webservice.
     */
    private void setDefaultNameServers()
    {
        Properties p1 = new Properties();
        p1.setProperty( "serv_type", "namesvr" );
        p1.setProperty( "long_name", "SIMBAD Names via CADC" );
        p1.setProperty( "short_name", "simbad_ns@cadc" );
        p1.setProperty
        ( "url",
                "http://cadcwww.dao.nrc.ca/cadcbin/sim-server?&o=%id" );
        SkycatConfigEntry entry = new SkycatConfigEntry( p1 );
        simbadCatalogue = new SkycatCatalog( entry );

        Properties p2 = new Properties();
        p2.setProperty( "serv_type", "namesvr" );
        p2.setProperty( "long_name", "NED Names" );
        p2.setProperty( "short_name", "ned@eso" );
        p2.setProperty
        ( "url",
                "http://archive.eso.org/skycat/servers/ned-server?&o=%id" );
        entry = new SkycatConfigEntry( p2 );
        nedCatalogue = new SkycatCatalog( entry );
    }

    /**
     * Perform the query to all the currently selected servers.
     */
    public void doQuery()
    {
        //  Get the position. Allow the object name to be passed using
        //  TARGETNAME, useful for solar system objects.
        String ra = raField.getText();
        String dec = decField.getText();
        String objectName = nameField.getText().trim();
        if ( ra == null || ra.length() == 0 ||
                dec == null || dec.length() == 0 ) {

            //  Check for an object name. Need one or the other.
            if ( objectName != null && objectName.length() > 0 ) {
                ra = null;
                dec = null;
            }
            else {
                int n = JOptionPane.showConfirmDialog( this,
                        "You have not supplied " +
                                "a search centre or object " +
                                "name, do you want to proceed?",
                                "No RA or Dec",
                                JOptionPane.YES_NO_OPTION );
                if ( n == JOptionPane.NO_OPTION ) {
                    return;
                }

                //  To be clear.
                ra = null;
                dec = null;
                objectName = null;
            }
        }

        //  And the radius.
        String radiusText = radiusField.getText();
        double radius=0;// = 10.0;
        if ( radiusText != null && radiusText.length() > 0 ) {
            try {
                radius = Double.parseDouble( radiusText );
            }
            catch (NumberFormatException e) {
                ErrorDialog.showError( this, "Radius input error", e );
                return;
            }
        }

       
            ErrorDialog.showError( this, "Cannot understand this value", e );
       // queryLine.setQueryParameters(ra, dec, objectName, radiusText, lowerBandField.getText(), upperBandField.getText(), lowerTimeField.getText(), upperTimeField.getText(), 
       //                             waveCalibGroup.getSelection().getActionCommand(), fluxCalibGroup.getSelection().getActionCommand(), formatGroup.getSelection().getActionCommand());
        try {
            queryLine.setPosition(ra, dec);
        } catch (NumberFormatException e) {
            ErrorDialog.showError( this, "Position input error", e );
            return;
        }
   /*     queryLine.setRadius( radius );
        queryLine.setBand(lowerBandField.getText(), upperBandField.getText());
        queryLine.setTime(lowerTimeField.getText(), upperTimeField.getText());
        queryLine.setWaveCalib(wlcalibList.getSelectedItem().toString());
        queryLine.setFluxCalib(flcalibList.getSelectedItem().toString());
        queryLine.setFormat(formatList.getSelectedItem().toString());
     */   
       // !!!!!
       
        // update serverlist from servertree class
        final SSAServerList slist=tree.getServerList();
        
        //  Create a stack of all queries to perform.
        ArrayList<SSAQuery> queryList = new ArrayList<SSAQuery>();
        
        //serverList = tree.getServerList();
        Iterator i = slist.getIterator();

        SSAPRegResource server = null;
        while( i.hasNext() ) {
            server = (SSAPRegResource) i.next();
            if (server != null )
                try {
                    if (slist.isServerSelected(server.getShortName())) {

                        SSAQuery ssaQuery =  new SSAQuery( server );
                        // ssaQuery.setServer(server) ; //Parameters(queryLine); // copy the query parameters to the new query
                        ssaQuery.setQuery(queryText.getText());
                        ArrayList<String> extp = ssaQuery.getParamList(queryText.getText()); 
                        if ( extp != null ) {

                            boolean supportsAll = true;
                            boolean supportsSome = false;
                            for (int j=0; j<extp.size(); j++) {

                                supportsAll = supportsAll && serverParam.paramSupported(server.getShortName(), extp.get(j));
                                supportsSome = supportsSome || serverParam.paramSupported(server.getShortName(), extp.get(j));
                            }
                            if (supportsSome ) // or supportsAll...  
                                queryList.add(ssaQuery);
                        } else  //  or the query has no extra parameters
                            queryList.add(ssaQuery);

                    }

                } catch(Exception npe) {
                    ErrorDialog.showError( this, "Exception", npe );
                    npe.printStackTrace();
                }

        }//while

        // Now actually do the queries, these are performed in a separate
        // Thread so we avoid locking the interface.
        if ( queryList.size() > 0 ) {
            processQueryList( queryList );
        }
        else {
            JOptionPane.showMessageDialog( this,
                    "There are no SSA servers currently selected",
                    "No SSA servers", JOptionPane.ERROR_MESSAGE );
        }
    }

    /**
     * Process a list of URL queries to SSA servers and display the
     * results. All processing is performed in a background Thread.
     */
    protected void processQueryList( ArrayList<SSAQuery> queryList )
    {
        // final serverlist
   
        //  final ArrayList localQueryList = queryList;
        makeResultsDisplay( null );
        
        if (progressFrame != null) {
            progressFrame.closeWindowEvent();
            progressFrame=null;
        }
       // final ProgressPanelFrame 
        progressFrame = new ProgressPanelFrame( "Querying SSAP servers" );
        

        Iterator<SSAQuery> i = queryList.iterator();
        while ( i.hasNext() ) {
            final SSAQuery ssaQuery = i.next();
         

            final ProgressPanel progressPanel = new ProgressPanel( "Querying: " + ssaQuery.getDescription());
            progressFrame.addProgressPanel( progressPanel );

            final SwingWorker worker = new SwingWorker()
            {
                boolean interrupted = false;
                public Object construct() 
                {
                    progressPanel.start();
                    try {
                        runProcessQuery( ssaQuery, progressPanel );
                    }
                    catch (InterruptedException e) {
                        interrupted = true;
                    }
                    return null;
                }

                public void finished()
                {
                    progressPanel.stop();
                    //  Display the results.
                    if ( ! interrupted ) {
                        addResultsDisplay( ssaQuery );
                    }
                }
            };

            progressPanel.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    if ( worker != null ) {
                         worker.interrupt();
                    }
                }
            });

            worker.start();  
        }
    }

    /**
     * Do a query to an SSAP server.
     */
    private void runProcessQuery( SSAQuery ssaQuery, ProgressPanel progressPanel ) throws InterruptedException
    {
        boolean failed = false;
        boolean overflow = false;

        //  We just download VOTables, so avoid attempting to build the other
        //  formats.
        StarTable starTable = null;
        GetDataTable getDataTable = null;
        DataLinkParams dataLinkParams = null;
      
        URL queryURL = null;

        logger.info( "Querying: " + queryURL );
        progressPanel.logMessage( ssaQuery.getBaseURL() );
      
        try { //!!!!!
            
            //queryURL = ssaQuery.getBaseURL();
 
            // check if more parameters have been added
            // Not very nice... should think of a better way to do that
            //     
            //String extendedQuery =metaPanel.getParamsQueryString();
           // logger.info( "Extended Query string " + extendedQuery );
            //if (extendedQuery != null && extendedQuery.length() > 0) 
           // {
                queryURL = ssaQuery.getRequestURL();
             
                logger.info( "Query string " + queryURL.toString() );
                //queryURL = new URL(newURL);
           // }

        }   
        catch ( MalformedURLException mue ) {
            progressPanel.logMessage( mue.getMessage() );
            logger.info( "Malformed URL "+queryURL );
            failed = true;
            return;
        }
        catch ( UnsupportedEncodingException uee) {
            progressPanel.logMessage( uee.getMessage() );
            logger.info( "URL Encoding Exception "+queryURL );
            failed = true;
            return;
        }
        
        //  Do the query and get the result as a StarTable. Uses this
        //  method for efficiency as non-result tables are ignored.
        try {
            
            
            URLConnection con =  queryURL.openConnection();
           
            con.setConnectTimeout(10 * 1000); // 10 seconds
            con.setReadTimeout(30*1000);
            con.connect();
            
            InputSource inSrc = new InputSource( con.getInputStream() );
                 
            // inSrc.setSystemId( ssaQuery.getBaseURL() );
            inSrc.setSystemId( queryURL.toString());
            
            VOElementFactory vofact = new VOElementFactory();
            
            VOElement voe = DalResourceXMLFilter.parseDalResult(vofact, inSrc);
           
        
            getDataTable = DalResourceXMLFilter.getDalGetDataTable( voe );
            starTable = DalResourceXMLFilter.getDalResultTable( voe );
            
            
            if (getDataTable != null) {
                ssaQuery.setGetDataTable( getDataTable);

            }
            
            // if the VOTable contains datalink service definitions, add to the SSAQuery.
            dataLinkParams = DalResourceXMLFilter.getDalGetServiceElement(voe); //!!!!!!!!!!!!!!!!!
            if (dataLinkParams != null) {
                ssaQuery.setDataLinkParams( dataLinkParams );

            }

          
            //  Check parameter QUERY_STATUS, this should be set to OK
            //  when the query
            String queryOK = null;
            String queryDescription = null;
            try {
                queryOK = starTable
                        .getParameterByName( "QUERY_STATUS" )
                        .getValueAsString( 100 );
                queryDescription = starTable
                        .getParameterByName( "QUERY_STATUS" )
                        .getInfo().getDescription();
            }
            catch (NullPointerException ne) {
                // Whoops, that's not good, but see what we can do.
                queryOK = "FAILED";
            }
            if ( "OK".equalsIgnoreCase( queryOK ) ) {
                ssaQuery.setStarTable( starTable );
                progressPanel.logMessage( "Done" );
            }
            else if ("OVERFLOW".equalsIgnoreCase( queryOK ) ) {
                ssaQuery.setStarTable( starTable );
                progressPanel.logMessage( queryDescription );
                logger.info( queryDescription);
                overflow=true;
            }
            else {
                //  Some problem with the service, report that.
                progressPanel.logMessage( "Query failed: " + queryOK );
                logger.info( "Query failed: " + queryOK );
                if ( queryDescription != null ) {
                    progressPanel.logMessage( queryDescription );
                    logger.info( queryDescription);
                }
                failed = true;
            }
           
            //  Dump query results as VOTables.
            //uk.ac.starlink.table.StarTableOutput sto =
            //    new uk.ac.starlink.table.StarTableOutput();
            //sto.writeStarTable( starTable,
            //                    "votable" + j + ".xml", null);
            //j++;
        }
        catch (TableFormatException te) {
            progressPanel.logMessage( te.getMessage() );
            logger.info( ssaQuery.getDescription() + ": " + te.getMessage() );
            failed = true;
        }
        catch (IOException ie) {
            progressPanel.logMessage( ie.getMessage() );
            logger.info( ssaQuery.getDescription() + ": " + ie.getMessage() );
            failed = true;
        }
        catch (Exception ge) {
            //  General exception.
            progressPanel.logMessage( ge.getMessage() );
            logger.info( ssaQuery.getDescription() + ": " + ge.getMessage() );
            failed = true;
        } 

        if ( Thread.interrupted() ) {
            throw new InterruptedException();
        }
        if ( ! failed && ! overflow ) {
            progressPanel.logMessage( "Completed download" );
        }

    } //runProcessQUery
 
    /**
     * Display the results of the queries to the SSA servers. The results can
     * be either a list of SSAQuery instances, or the StarTables themselves
     * (usually these are from a disk-file restoration, not a live query).
     */
    protected void makeResultsDisplay( ArrayList<VOStarTable> tableList )
    {
      
        
        if ( starJTables == null ) {
            starJTables = new ArrayList<StarJTable>();
        }

        //  Remove existing tables.
        resultsPane.removeAll();
        starJTables.clear();

        if ( tableList != null ) {
            Iterator<VOStarTable> i = tableList.iterator();
            while ( i.hasNext() ) {
                addResultsDisplay( i.next() );
               
            }
        }
    }
    protected synchronized void addResultsDisplay( Object next )
    {
        DescribedValue dValue = null;
        JScrollPane scrollPane = null;
        SSAQuery ssaQuery = null;
        StarJTable table = null;
        StarTable starTable = null;
        GetDataTable getDataTable = null;
        DataLinkParams  dataLinkParams = null;
        String shortName = null;
     
        boolean hasParams = false;
       
        ImageIcon cutImage = new ImageIcon( ImageHolder.class.getResource("smallcutter.gif") );
   
        if ( next instanceof SSAQuery && next != null ) {
            ssaQuery = (SSAQuery) next;
            starTable = ssaQuery.getStarTable();
            getDataTable = ssaQuery.getGetDataTable();
            dataLinkParams = ssaQuery.getDataLinkParams(); // get the data link services information
            shortName = ssaQuery.getDescription();
            if ( starTable != null ) {
            String baseurl = ssaQuery.getBaseURL();
                try {
                    starTable.setURL(new URL(baseurl));
                } catch (MalformedURLException e) {
                    logger.info( "Malformed base URL for " + baseurl );
                } 
            }
            
        }
        else if ( next instanceof StarTable) {
            starTable = (StarTable) next;
                
            dValue = starTable.getParameterByName( "ShortName" );
            if ( dValue == null ) {
                shortName = starTable.getName();
            }
            else {
                shortName = (String)dValue.getValue();
            }
           
        }
        else {
            logger.info( "Couldn't handle: " + next );
        }
        if ( starTable != null ) {
            //  Check if table has rows, if not skip.
            int nrows = (int) starTable.getRowCount();
            if (  nrows > 0 ) {
                table = new StarJTable( starTable, true );
                scrollPane = new JScrollPane( table );
              //  scrollPane.setPreferredSize(new Dimension(600,400));
                
                if (dataLinkParams != null) { // if datalink services are present, create a frame
                    
                    if ( dataLinkFrame == null ) {
                         dataLinkFrame = new DataLinkQueryFrame();
                    } 
                  
                    dataLinkFrame.addServer(shortName, dataLinkParams);  // associate this datalink service information to the current server
                    dataLinkButton.setEnabled(true);
                    dataLinkButton.setVisible(true);
                    dataLinkButton.setForeground(Color.GRAY);
                    resultsPane.addTab( shortName, cutImage, scrollPane );
                }
                else if (getDataTable != null) { // if no dataLink services present, check if there are getData services
                    if ( getDataFrame == null ) {
                         getDataFrame = new GetDataQueryFrame();
                    } 
                  
                    getDataFrame.addService(shortName, getDataTable);                    
  //                  getDataButton.setEnabled(true);
  //                  getDataButton.setVisible(true);
  //                  getDataButton.setForeground(Color.GRAY);
                    dataLinkButton.setEnabled(true);
                    dataLinkButton.setVisible(true);
                    dataLinkButton.setForeground(Color.GRAY);
                    resultsPane.addTab( shortName, cutImage, scrollPane );
                }
                else resultsPane.addTab( shortName, scrollPane );
                starJTables.add( table );

                //  Set widths of columns.
                if ( nrows > 1 ) {
                    nrows = ( nrows > 5 ) ? 5 : nrows;
                    table.configureColumnWidths( 200, nrows );
                }

                //  Double click on row means load just that spectrum.
                table.addMouseListener( this );
            }
        }
    }

       
    /**
     * Deselect all spectra in the visible table, or deselect all tables.
     */
    protected void deselectSpectra( boolean all )
    {
        if (starJTables == null)  // avoids NPE if no results are present
            return;
        if ( all ) {
            //  Visit all the tabbed StarJTables.
            Iterator<StarJTable> i = starJTables.iterator();
            while ( i.hasNext() ) {
                i.next().clearSelection();
            }
        }
        else {
            Component c = resultsPane.getSelectedComponent();
            if ( c != null ) {
                JScrollPane sp = (JScrollPane) c;
                StarJTable table = (StarJTable ) sp.getViewport().getView();
                table.clearSelection();
            }
        }
    }



    /**
     * Get the main SPLAT browser to download and display spectra.
     * <p>
     * spectrum from a row in a given table. If table is null, then the
     * selected parameter determines the behaviour of all or just the selected
     * spectra.
     */
    protected void displaySpectra( boolean selected, boolean display,
            StarJTable table, int row )
    {
        //  List of all spectra to be loaded and their data formats and short
        //  names etc.
        ArrayList<Props> specList = new ArrayList<Props>();
     
        
        if ( table == null ) { 
            
            if (starJTables == null)  // avoids NPE if no results are present
                return;
            //  Visit all the tabbed StarJTables.
            Iterator<StarJTable> i = starJTables.iterator();
            while ( i.hasNext() ) {
                extractSpectraFromTable( i.next(), specList,
                        selected, -1 );
            }
        }
        else {
            extractSpectraFromTable( table, specList, selected, row );
        }

        //  If we have no spectra complain and stop.
        if ( specList.size() == 0 ) {
            String mess;
            if ( selected ) {
                mess = "There are no spectra selected";
            }
            else {
                mess = "No spectra available";
            }
            JOptionPane.showMessageDialog( this, mess, "No spectra",
                    JOptionPane.ERROR_MESSAGE );
            return;
        }

        //  And load and display...
        SpectrumIO.Props[] propList = new SpectrumIO.Props[specList.size()];
        specList.toArray( propList );
        
        // check for authentication
        for (int p=0; p<propList.length; p++ ) {
            URL url=null;
            try {
                 url = new URL(propList[p].getSpectrum());
                 logger.info("Spectrum URL"+url);
            } catch (MalformedURLException mue) {
                logger.info(mue.getMessage());
            }
        }
        

        browser.threadLoadSpectra( propList, display );
        browser.toFront();
    }

    /**
     * Extract all the links to spectra for downloading, plus the associated
     * information available in the VOTable. Each set of spectral information
     * is used to populated a SpectrumIO.Prop object that is added to the
     * specList list.
     * <p>
     * Can return the selected spectra, if requested, otherwise all spectra
     * are returned or if a row value other than -1 is given just one row.
     * @throws SplatException 
     */
    private void extractSpectraFromTable( StarJTable starJTable,
            ArrayList<Props> specList,
            boolean selected,
            int row )
    {
        int[] selection = null;
      
        HashMap< String, String > getDataParam = null;
        
        HashMap< String, String > dataLinkQueryParams = null;
        String idSource = null;
        String accessURL = null;
        if ( dataLinkFrame != null && dataLinkFrame.isVisible() ) {
            dataLinkQueryParams = dataLinkFrame.getParams();
            idSource = dataLinkFrame.getIDSource(); 
            accessURL = dataLinkFrame.getAccessURL();
        }
        else if ( getDataFrame != null && getDataFrame.isVisible() ) {
            getDataParam = getDataFrame.getParams();
            idSource="PUBDid";
        }
        
       
        
        //  Check for a selection if required, otherwise we're using the given
        //  row.
        if ( selected && row == -1 ) {
            selection = starJTable.getSelectedRows();
        }
        else if ( row != -1 ) {
            selection = new int[1];
            selection[0] = row;
        }

        // Only do this if we're processing all rows or we have a selection.
        if ( selection == null || selection.length > 0 ) {
            StarTable starTable = starJTable.getStarTable();

            //  Check for a column that contains links to the actual data
            //  (XXX these could be XML links to data within this
            //  document). The signature for this is an UCD of DATA_LINK,
            //  or a UTYPE of Access.Reference.
            int ncol = starTable.getColumnCount();
            int linkcol = -1;
            int typecol = -1;
            int namecol = -1;
            int axescol = -1;
            int specaxiscol = -1;
            int fluxaxiscol = -1;
            int unitscol = -1;
            int specunitscol = -1;
            int fluxunitscol = -1;
            int fluxerrorcol = -1;
            int pubdidcol=-1;
            int idsrccol=-1;
            int specstartcol=-1;
            int specstopcol=-1;
            ColumnInfo colInfo;
            String ucd;
            String utype;
            String getDataRequest="";
            String dataLinkRequest="";
            
            for( int k = 0; k < ncol; k++ ) {
                colInfo = starTable.getColumnInfo( k );
                ucd = colInfo.getUCD();
              
                //  Old-style UCDs for backwards compatibility.
                if ( ucd != null ) {
                    ucd = ucd.toLowerCase();
                    if ( ucd.equals( "data_link" ) ) {
                        linkcol = k;
                    }
                    else if ( ucd.equals( "vox:spectrum_format" ) ) {
                        typecol = k;
                    }
                    else if ( ucd.equals( "vox:image_title" ) ) {
                        namecol = k;
                    }
                    else if ( ucd.equals( "vox:spectrum_axes" ) ) {
                        axescol = k;
                    }
                    else if ( ucd.equals( "vox:spectrum_units" ) ) {
                        unitscol = k;
                    }
                }

                //  Version 1.0 utypes. XXX not sure if axes names
                //  are in columns or are really parameters. Assume
                //  these work like the old-style scheme and appear in
                //  the columns.
                utype = colInfo.getUtype();
                if ( utype != null ) {
                    utype = utype.toLowerCase();
                    if ( utype.endsWith( "access.reference" ) ) {
                        linkcol = k;
                    }
                    else if ( utype.endsWith( "access.format" ) ) {
                        typecol = k;
                    }
                    else if ( utype.endsWith( "target.name" ) ) {
                        namecol = k;
                    }
                    else if ( utype.endsWith( "char.spectralaxis.name" ) ) {
                        specaxiscol = k;
                    }
                    else if ( utype.endsWith( "char.spectralaxis.unit" ) ) {
                        specunitscol = k;
                    }
                    else if ( utype.endsWith( "char.fluxaxis.name" ) ) {
                        fluxaxiscol = k;
                    }
                    else if ( utype.endsWith( "char.fluxaxis.accuracy.staterror" ) ) {
                        fluxerrorcol = k;
                    }
                    else if ( utype.endsWith( "char.fluxaxis.unit" ) ) {
                        fluxunitscol = k;
                    }
                    else if ( utype.endsWith( "Curation.PublisherDID" ) ) {
                        pubdidcol = k;
                    }
                    else if ( utype.endsWith( "char.spectralAxis.coverage.bounds.start" ) ) {
                        specstartcol = k;
                    }
                    else if ( utype.endsWith( "char.spectralAxis.coverage.bounds.stop" ) ) {
                        specstopcol = k;
                    }
                }
                if (colInfo.getName().equals("ssa_pubDID"))
                    pubdidcol = k;
                if (colInfo.getName().equals(idSource))
                    idsrccol = k;
                
            } // for
            
            if (idsrccol != -1  && dataLinkQueryParams != null ) { // check if datalink parameters are present
                 
                if ( ! dataLinkQueryParams.isEmpty() ) {                   
                    for (String key : dataLinkQueryParams.keySet()) {
                        String value = dataLinkQueryParams.get(key);
                                if (value != null && value.length() > 0) {
                                    try {//
                                           
                                            if (! key.equals("IDSource") && ! (key.equals("AccessURL"))) {
                                                dataLinkRequest+="&"+key+"="+URLEncoder.encode(value, "UTF-8");
                                            }
                                       
                                    } catch (UnsupportedEncodingException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }                                     
                                }
                    }
                }
            } else if (pubdidcol != -1  && getDataParam != null ) { // if we have a pubDID col, check if the getData parameters are set.    
                if ( ! getDataParam.isEmpty() ) {                   
                    for (String key : getDataParam.keySet()) {
                        String value = getDataParam.get(key);
                                if (value == null || value.length() > 0) {
                                    try {//
                                       
                                        ///
                                        // float specstart = Float.parseFloat(rseq.getCell( linkcol ).);
                                       //  float specstop = Float.parseFloat();
                                      //  double specend = Double.parseDouble(params[i].getAttribute("ssa_specend"));
                                      //  double  maxval =   double specstart = Double.parseDouble(values.getMaximum());
                                     //   double  minval =   double specstart = Double.parseDouble(values.getMinimum());
                                        ///
                                        getDataRequest+="&"+key+"="+URLEncoder.encode(value, "UTF-8");
                                    } catch (UnsupportedEncodingException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }                                     
                                }
                    }
                }
            }
           
        
            //  If we have a DATA_LINK column, gather the URLs it contains
            //  that are appropriate.
            if ( linkcol != -1 ) {
                RowSequence rseq = null;
                SpectrumIO.Props props = null;
                String value = null;
                String[] axes;
                String[] units;
                try {
                    if ( ! selected && selection == null ) {
                        //  Using all rows.
                        rseq = starTable.getRowSequence();
                        while ( rseq.next() ) {
                            value = ( (String) rseq.getCell( linkcol ).toString() );
                            value = value.trim();
                            props = new SpectrumIO.Props( value );
                            if ( typecol != -1 ) {
                                value = ((String)rseq.getCell( typecol ).toString() );
                                if ( value != null ) {
                                    value = value.trim();
                                    props.setType
                                    ( specDataFactory
                                            .mimeToSPLATType( value ) );
                                }
                            } //while
                            if ( namecol != -1 ) {
                                value = ( (String)rseq.getCell( namecol ).toString() );
                                if ( value != null ) {
                                    value = value.trim();
                                    props.setShortName( value );
                                }
                            }

                            if ( axescol != -1 ) {

                                //  Old style column names.
                                value = ( (String)rseq.getCell( axescol ).toString() );
                                if ( value != null ) {
                                    value = value.trim();
                                    axes = value.split("\\s");
                                    props.setCoordColumn( axes[0] );
                                    props.setDataColumn( axes[1] );
                                    if ( axes.length == 3 ) {
                                        props.setErrorColumn( axes[2] );
                                    }
                                }
                            } // if axescol !- 1
                            else {

                                //  Version 1.0 style.
                                if ( specaxiscol != -1 ) {
                                    value = (String)rseq.getCell(specaxiscol).toString();
                                    props.setCoordColumn( value );
                                }
                                if ( fluxaxiscol != -1 ) {
                                    value = (String)rseq.getCell(fluxaxiscol).toString();
                                    props.setDataColumn( value );
                                }
                                if ( fluxerrorcol != -1 ) {
                                    value = (String)rseq.getCell(fluxerrorcol).toString();
                                    props.setErrorColumn( value );
                                }
                            } //else 

                            if ( unitscol != -1 ) {

                                //  Old style column names.
                                value = ( (String)rseq.getCell( unitscol ).toString() );
                                if ( value != null ) {
                                    value = value.trim();
                                    units = value.split("\\s");
                                    props.setCoordUnits( units[0] );
                                    props.setDataUnits( units[1] );
                                    //  Error must have same units as data.
                                }
                            }
                            else {

                                //  Version 1.0 style.
                                if ( specunitscol != -1 ) {
                                    value = (String)rseq.getCell(specunitscol).toString();
                                    props.setCoordUnits( value );
                                }
                                if ( fluxunitscol != -1 ) {
                                    value = (String)rseq.getCell(fluxunitscol).toString();
                                    props.setDataUnits( value );
                                }
                            }
                         
                            if (idsrccol != -1  && dataLinkQueryParams != null) { //!!!!!
                                
                                if (! dataLinkQueryParams.isEmpty()) { 
                                   props.setIdValue(rseq.getCell(idsrccol).toString());
                                   props.setIdSource(idSource);
                                   props.setDataLinkRequest(dataLinkRequest);
                                   props.setServerURL(dataLinkQueryParams.get("AccessURL"));
                                   String format = dataLinkQueryParams.get("FORMAT");
                                   if (format != null && format != "") {
                                       props.setDataLinkFormat(format);
                                       props.setType(specDataFactory.mimeToSPLATType( format ));
                                   }
                                }
                            } else if (pubdidcol != -1  && getDataParam != null) {
                               
                                if (! getDataParam.isEmpty()) { 
                                   props.setIdValue(rseq.getCell(pubdidcol).toString());
                                   props.setGetDataRequest(getDataRequest);
                                   props.setServerURL(starTable.getURL().toString());
                                   String format = getDataParam.get("FORMAT");
                                   if (format != "")
                                       props.setGetDataFormat(format);
                                }
                            }
                            specList.add( props );
                        } //while
                    } // if selected
                    else {
                        //  Just using selected rows. To do this we step
                        //  through the table and check if that row is the
                        //  next one in the selection (the selection is
                        //  sorted).
                        rseq = starTable.getRowSequence();
                        int k = 0; // Table row
                        int l = 0; // selection index
                        while ( rseq.next() ) {
                            if ( k == selection[l] ) {

                                // Store this one as matches selection.
                                if (rseq.getCell( linkcol ) != null)                                      
                                    value = ( (String)rseq.getCell( linkcol ).toString() );
                                if (value != null ) {         
                                    value = value.trim();
                                    props = new SpectrumIO.Props( value );
                                } 
                                if ( typecol != -1 ) {
                                    value = null;
                                    Object obj = rseq.getCell(typecol);
                                    if (obj != null) 
                                        value =((String)rseq.getCell(typecol).toString());
                                    if ( value != null ) {
                                        value = value.trim();
                                        props.setType
                                        ( specDataFactory
                                                .mimeToSPLATType( value ) );
                                    }
                                }
                                if ( namecol != -1 ) {
                                    value = null;
                                    Object obj = rseq.getCell(namecol);
                                    if (obj != null) 
                                    value = ((String)rseq.getCell( namecol ).toString());
                                    if ( value != null ) {
                                        value = value.trim();
                                        props.setShortName( value );
                                    }
                                }
                                if ( axescol != -1 ) {
                                    value = null;
                                    Object obj = rseq.getCell(axescol);
                                    if (obj != null) 
                                        value = ((String)obj.toString());
                                    
                                    if (value != null ) {
                                         value = value.trim();
                                        axes = value.split("\\s");
                                        props.setCoordColumn( axes[0] );
                                        props.setDataColumn( axes[1] );
                                    }
                                }
                                if ( unitscol != -1 ) {
                                    value = null;
                                    Object obj = rseq.getCell(unitscol);
                                    if (obj != null) 
                                        value = ((String)rseq.getCell(unitscol).toString());
                                    if ( value != null ) {
                                        units = value.split("\\s");
                                        props.setCoordUnits( units[0] );
                                        props.setDataUnits( units[1] );
                                    }
                                }
                               /*
                                *  if (pubdidcol != -1  && getDataParam != null) {
                                
                                    if (! getDataParam.isEmpty()) { 
                                       props.setPubdidValue(rseq.getCell(pubdidcol).toString());
                                       props.setGetDataRequest(getDataRequest);
                                       props.setServerURL(starTable.getURL().toString());
                                       String format = getDataParam.get("FORMAT");
                                       if (format != "")
                                           props.setGetDataFormat(format);
                                       props.setShortName(props.getShortName() + " [" + getDataParam.get("BAND") + "]" );
                                    }
                                }*/
                                if (idsrccol != -1  && dataLinkQueryParams != null) {  // !!!!!
                                    if (! dataLinkQueryParams.isEmpty()) { 
                                        props.setIdValue(rseq.getCell(idsrccol).toString());
                                        props.setIdSource(idSource);
                                       props.setDataLinkRequest(dataLinkRequest);
                                      // props.setServerURL(dataLinkQueryParam.get("AccessURL"));
                                       props.setServerURL(accessURL);
                                       String format = dataLinkQueryParams.get("FORMAT");
                                       if (format != null && format != "") {
                                           props.setDataLinkFormat(format);
                                           props.setType(specDataFactory.mimeToSPLATType( format ) );
                                      // props.setShortName(props.getShortName() + " [" + getDataParam.get("BAND") + "]" );
                                       }
                                    }
                                } else  if (pubdidcol != -1  && getDataParam != null) {
                                
                                if (! getDataParam.isEmpty()) { 
                                   props.setIdValue(rseq.getCell(pubdidcol).toString());
                                   props.setGetDataRequest(getDataRequest);
                                   props.setServerURL(starTable.getURL().toString());
                                   String format = getDataParam.get("FORMAT");
                                   if (format != "")
                                       props.setGetDataFormat(format);
                                   props.setShortName(props.getShortName() + " [" + getDataParam.get("BAND") + "]" );
                                }
                            }
                                specList.add( props );

                                //  Move to next selection.
                                l++;
                                if ( l >= selection.length ) {
                                    break;
                                }
                            }
                            k++;
                        }
                    } // if selected
                } // try
                catch (IOException ie) {
                    ie.printStackTrace();
                }
                catch (NullPointerException ee) {
                    ErrorDialog.showError( this, "Failed to parse query results file", ee );
                }
                finally {
                    try {
                        if ( rseq != null ) {
                            rseq.close();
                        }
                    }
                    catch (IOException iie) {
                        // Ignore.
                    }
                }
            }// if linkcol != -1
        } 
    }

    /**
     *  Restore a set of previous query results that have been written to a
     *  VOTable. The file name is obtained interactively.
     */
    public void readQueryFromFile()
    {
        initFileChooser();
        int result = fileChooser.showOpenDialog( this );
        if ( result == JFileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            try {
                readQuery( file );
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }
    }

    /**
     *  Restore a set of query results from a File. The File should have the
     *  results written previously as a VOTable, with a RESOURCE containing
     *  the various query results as TABLEs.
     */
    protected void readQuery( File file )
            throws SplatException
            {
        VOElement rootElement = null;
        try {
            rootElement = new VOElementFactory().makeVOElement( file );
        }
        catch (Exception e) {
            throw new SplatException( "Failed to open query results file", e );
        }

        //  First element should be a RESOURCE.
        VOElement[] resource = rootElement.getChildren();
        VOStarTable table = null;
        ArrayList<VOStarTable> tableList = new ArrayList<VOStarTable>();
        String tagName = null;
        for ( int i = 0; i < resource.length; i++ ) {
            tagName = resource[i].getTagName();
            if ( "RESOURCE".equals( tagName ) ) {

                //  Look for the TABLEs.
                VOElement child[] = resource[i].getChildren();
                for ( int j = 0; j < child.length; j++ ) {
                    tagName = child[j].getTagName();
                    if ( "TABLE".equals( tagName ) ) {
                        try {
                            table = new VOStarTable( (TableElement) child[j] );
                        }
                        catch (IOException e) {
                            throw new SplatException( "Failed to read query result", e );
                        }
                        tableList.add( table );
                    }
                }
            }
        }
        if ( tableList.size() > 0 ) {
            makeResultsDisplay( tableList );
        }
        else {
            throw new SplatException( "No query results found" );
        }
            }

    /**
     *  Interactively get a file name and save current query results to it as
     *  a VOTable.
     */
    public void saveQueryToFile()
    {
        if ( starJTables == null || starJTables.size() == 0 ) {
            JOptionPane.showMessageDialog( this,
                    "There are no queries to save",
                    "No queries", JOptionPane.ERROR_MESSAGE );
            return;
        }

        initFileChooser();
        int result = fileChooser.showSaveDialog( this );
        if ( result == JFileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            try {
                saveQuery( file );
            }
            catch (SplatException e) {
                ErrorDialog.showError( this, e );
            }
        }
    }

    /**
     *  Save current query to a File, writing the results as a VOTable.
     */
    protected void saveQuery( File file )
            throws SplatException
            {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter( new FileWriter( file ) );
        }
        catch (IOException e) {
            throw new SplatException( e );
        }
        saveQuery( writer );
        try {
            writer.close();
        }
        catch (IOException e) {
            throw new SplatException( e );
        }
            }

    /**
     *  Save current query results to a BufferedWriter. The resulting document
     *  is a VOTable with a RESOURCE that contains a TABLE for each set of
     *  query results.
     */
    protected void saveQuery( BufferedWriter writer )
            throws SplatException
            {
        String xmlDec = VOTableWriter.DEFAULT_XML_DECLARATION;
        try {
            writer.write( xmlDec );
            writer.newLine();
            writer.write( "<VOTABLE version=\"1.1\"" );
            writer.newLine();
            writer.write( "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" );
            writer.newLine();
            writer.write( "xsi:schemaLocation=\"http://www.ivoa.net/xml/VOTable/v1.1\"" );
            writer.newLine();
            writer.write( "xmlns=\"http://www.ivoa.net/xml/VOTable/v1.1\">" );
            writer.newLine();
            writer.write( "<RESOURCE>" );
            writer.newLine();

            StarJTable starJTable = null;
            StarTable table = null;
            VOSerializer serializer = null;
            Iterator<StarJTable> i = starJTables.iterator();
            while ( i.hasNext() ) {
                starJTable = i.next();
                table = starJTable.getStarTable();

                //  Write <TABLE> element. First need to remove FIELD
                //  IDS. These are no longer unique for the whole document.
                int n = table.getColumnCount();
                for ( int j = 0; j < n; j++ ) {
                    ColumnInfo ci = table.getColumnInfo( j );
                    ci.setAuxDatum( new DescribedValue( VOStarTable.ID_INFO, null ) );
                }
                serializer = VOSerializer.makeSerializer( DataFormat.TABLEDATA, table );
                serializer.writeInlineTableElement( writer );
            }
            writer.write( "</RESOURCE>" );
            writer.newLine();
            writer.write( "</VOTABLE>" );
            writer.newLine();
            writer.close();
        }
        catch (IOException e) {
            throw new SplatException( "Failed to save queries", e );
        }
            }

    /**
     * Return a StarTable of the currently selected tab of query results.
     */
    public StarTable getCurrentTable()
    {
        if ( starJTables != null && starJTables.size() > 0 ) {
            int index = resultsPane.getSelectedIndex();
            if ( index > -1 ) {
                StarJTable jTable = starJTables.get( index );
                return jTable.getStarTable();
            }
        }
        return null;
    }

    /**
     * Initialise the file chooser to have the necessary filters.
     */
    protected void initFileChooser()
    {
        if ( fileChooser == null ) {
            fileChooser = new BasicFileChooser( false );
            fileChooser.setMultiSelectionEnabled( false );

            //  Add a filter for XML files.
            BasicFileFilter xmlFileFilter =
                    new BasicFileFilter( "xml", "XML files" );
            fileChooser.addChoosableFileFilter( xmlFileFilter );

            //  But allow all files as well.
            fileChooser.addChoosableFileFilter
            ( fileChooser.getAcceptAllFileFilter() );
        }
    }

    /**
     * Set the proxy server and port.
     */
    protected void showProxyDialog()
    {
        if ( proxyWindow == null ) {
            ProxySetupFrame.restore( null );
            proxyWindow = new ProxySetupFrame();
        }
        proxyWindow.setVisible( true );
    }

    /**
     *  Close the window.
     */
    protected void closeWindowEvent()
    {
       
        Utilities.saveFrameLocation( this, prefs, "SSAQueryBrowser" );
        this.dispose();
    }

    /**
     * Configure the SSA servers.
     *
    protected void showServerWindow()
    {
        if ( serverWindow == null ) {
            serverWindow = new SSAServerFrame( serverList );
        }
        serverWindow.setVisible( true );
    }
    */
    public static void main( String[] args )
    {
        try {
            SSAQueryBrowser b =
                    new SSAQueryBrowser( new SSAServerList(), null );
            b.pack();
            b.setVisible( true );
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //
    // ActionListener interface.
    //
    public void actionPerformed( ActionEvent e )
    {
        Object source = e.getSource();
        String cmd = e.getActionCommand();

        if ( source.equals( goButton ) ) {
            {
               // deactivateGetDataSupport();
               // getDataButton.setVisible(false);
                deactivateDataLinkSupport(); //!!!!
                dataLinkButton.setVisible(false);
                doQuery();
               
            }        
            return;
        } 
     
                try {
                    queryText.setText(queryLine.getQueryURLText()+ extendedQueryText);
                } catch (UnsupportedEncodingException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }        
            return;
        } */
        if ( source.equals( nameLookup ) /*|| source.equals( nameField ) */) {
            
            
          
          
            resolveName();
           // queryLine.setPosition(raField.getText(), decField.getText());
            //updateQueryText();
            //try {
            //    String q = queryLine.getQueryURLText();
           //     queryText.setText(q);
         //   } catch (UnsupportedEncodingException e1) {
          //      // TODO Auto-generated catch block
           //     e1.printStackTrace();
          //  }
            return;
        } 
           
        if (source.equals(raField) || source.equals(decField) || source.equals(nameField)) {

                //  Get the position. Allow the object name to be passed using
                //  TARGETNAME, useful for solar system objects.
                String ra = raField.getText();
                String dec = decField.getText();
                String objectName = nameField.getText();
                if ( ra == null || ra.length() == 0 ||
                        dec == null || dec.length() == 0 ) {

                    //  Check for an object name. Need one or the other.
                    if ( objectName != null && objectName.length() > 0 ) {
                        ra = null;
                        dec = null;
                    }
                    else { 
                        //  To be clear.
                        ra = null;
                        dec = null;
                        objectName = null;
                    }
                }
                try {
                    queryLine.setPosition( ra, dec );
                    } catch (NumberFormatException nfe) {
                        ErrorDialog.showError( this, "Cannot understand this value", nfe );
                        return;
                    }
                queryLine.setTargetName(objectName);
                try {
                    queryText.setText(queryLine.getQueryURLText());
                } catch (UnsupportedEncodingException e1) {
                // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                return;   
            }
            if ( source.equals( radiusField )  ) {
            
                String radiusText = radiusField.getText();
                double radius = 0.0;
                if ( radiusText != null && radiusText.length() > 0 ) {
                    try {
                        radius = Double.parseDouble( radiusText );
                    }
                    catch (NumberFormatException e1) {
                        ErrorDialog.showError( this, "Cannot understand radius value", e1);                         
                        return;
                    }
                }
                queryLine.setRadius(radius);
            
                try {
                    queryText.setText(queryLine.getQueryURLText());
                } catch (UnsupportedEncodingException e1) {
                // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                return;            
            }
            
            //  Spectral bandpass. These should be in meters. XXX allow other
            //  units and do the conversion.
            if (  source.equals( lowerBandField ) || source.equals( upperBandField )) {
                 
                String lowerBand = lowerBandField.getText();
                if ( "".equals( lowerBand ) ) {
                    lowerBand = null;
                }
                String upperBand = upperBandField.getText();
                if ( "".equals( upperBand ) ) {
                    upperBand = null;
                }
                  queryLine.setBand(lowerBand, upperBand);
                  try {
                      queryText.setText(queryLine.getQueryURLText());
                  } catch (UnsupportedEncodingException e1) {
                  // TODO Auto-generated catch block
                      e1.printStackTrace();
                  }
                  return;            
            }
           
            if (  source.equals( upperTimeField ) || source.equals( lowerTimeField ))  {
                
                String lowerTime = lowerTimeField.getText();
                if ( "".equals( lowerTime ) ) {
                    lowerTime = null;
                }
                String upperTime = upperTimeField.getText();
                if ( "".equals( upperTime ) ) {
                    upperTime = null;
                }
                  queryLine.setTime(lowerTime, upperTime);
                  try {
                      queryText.setText(queryLine.getQueryURLText());
                  } catch (UnsupportedEncodingException e1) {
                  // TODO Auto-generated catch block
                      e1.printStackTrace();
                  }
                  return;            
            }
            if ( source.equals(formatList)) {
                    queryLine.setFormat(formatList.getSelectedItem().toString());
                    try {
                        queryText.setText(queryLine.getQueryURLText());
                    } catch (UnsupportedEncodingException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                
                return;     
            }
            if ( source.equals(wlcalibList)) {
                
                    queryLine.setWaveCalib(wlcalibList.getSelectedItem().toString());
                    try {
                        queryText.setText(queryLine.getQueryURLText());
                    } catch (UnsupportedEncodingException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                
                return;     
            }
            if ( source.equals(flcalibList)) {
            
                    queryLine.setFluxCalib(flcalibList.getSelectedItem().toString());
                    try {
                        queryText.setText(queryLine.getQueryURLText());
                    } catch (UnsupportedEncodingException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                
                return;     
            }
                                 
        if ( source.equals( selectAllParamsButton ) ) 
        {
            metaPanel.selectAll();
            //tree.setParamMap(serverParam);
           /* addParameter();
            if (extendedQueryText != null && extendedQueryText.length() > 0) 
            {
                try {
                    queryText.setText(queryLine.getQueryURLText() + extendedQueryText );
                } catch (UnsupportedEncodingException e1) {
                        // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
          */
            return;
        } 
        if ( source.equals( deselectAllParamsButton ) ) 
        {
            // de-select and update query text
            metaPanel.deselectAll();
           // metaPanel.removeSelectedMetadata();
        /*    if (extendedQueryText != null && extendedQueryText.length() > 0) 
            {
                try {
                    queryText.setText(queryLine.getQueryURLText() + extendedQueryText );
                } catch (UnsupportedEncodingException e1) {
                        // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
         */ 
            return;
        } 

        if ( source.equals( customButton ) ) {
   //         if (metaFrame == null) {
                customParameters();
   //         } else {
   //             metaFrame.openWindow();
    //        }
            return;
        }

        if ( source.equals( displaySelectedButton ) ) {
            displaySpectra( true, true, null, -1 );
            return;
        }
        if ( source.equals( displayAllButton ) ) {
            displaySpectra( false, true, null, -1 );
            return;
        }

        if ( source.equals( downloadSelectedButton ) ) {
            displaySpectra( true, false, null, -1 );
            return;
        }
        if ( source.equals( downloadAllButton ) ) {
            displaySpectra( false, false, null, -1 );
            return;
        }

        if ( source.equals( deselectVisibleButton ) ) {
            deselectSpectra( false );
            return;
        }
        if ( source.equals( deselectAllButton ) ) {
            deselectSpectra( true );
            return;
        }/*
        if ( source.equals( getDataButton ) ) {
            if (getDataFrame == null || getDataFrame.getParams() == null)
                return;
            if (getDataFrame.isVisible()) { // deactivate
                getDataFrame.setVisible(false);
                //getDataButton.set.setEnabled(false);
                deactivateGetDataSupport();
            
            } else {
                getDataFrame.setVisible(true);
               // getDataButton.setEnabled(true);
                activateGetDataSupport();
               
            }
            return;
        }
        */
        if ( source.equals( dataLinkButton ) ) {
            if (dataLinkFrame != null && dataLinkFrame.getParams() != null) {
                if (dataLinkFrame.isVisible()) { // deactivate
                    dataLinkFrame.setVisible(false);
                    //getDataButton.set.setEnabled(false);
                    deactivateDataLinkSupport();
            
                } else {
                    activateDataLinkSupport();
                    if (resultsPane.isEnabledAt(resultsPane.getSelectedIndex()))
                        dataLinkFrame.setVisible(true);
                    // getDataButton.setEnabled(true);
                  
               
                }
            } else if (getDataFrame != null && getDataFrame.getParams() != null) {
                if (getDataFrame == null || getDataFrame.getParams() == null)
                    return;
                if (getDataFrame.isVisible()) { // deactivate
                    getDataFrame.setVisible(false);
                    //getDataButton.set.setEnabled(false);
                   // deactivateGetDataSupport();
                    deactivateDataLinkSupport();
                
                } else {
                    getDataFrame.setVisible(true);
                   // getDataButton.setEnabled(true);
                   // activateGetDataSupport();
                    activateDataLinkSupport();
                   
                }
            } 
                
            return;
        }

    }
    /**
     * ActivateGetDataSupport
     * deactivate all sites that do not support getData
     * activate getData queries on supported sites
     *//*
    private void activateGetDataSupport() {
        
        getDataEnabled=true;
        int selected=-1;
        int anyGDIndex = -1;
        getDataButton.setForeground(Color.BLACK);
        int nrTabs = resultsPane.getTabCount();
        for(int i = 0; i < nrTabs; i++)
        {
           if (resultsPane.getIconAt(i) == null) {
               resultsPane.setEnabledAt(i, false);
           }
           else {
               if (resultsPane.getSelectedIndex() == i)
                   selected = i;
               anyGDIndex = i;
              // resultsPane.setSelectedIndex(i);
           }
        }
        
        // if current selection is a getData service, keep it. If not,
        // set selection to one getData service.
        if (selected < 0)
            selected=anyGDIndex;
        resultsPane.setSelectedIndex(selected);
        
        //getDataFrame.setService(resultsPane.getTitleAt(selected));
        dataLinkFrame.setService(resultsPane.getTitleAt(selected));
            
    }
    */
    /**
     * ActivateDataLinkSupport
     * deactivate all sites that do not support DataLink/getData
     * activate DataLink/getData queries on supported sites
     */
    private void activateDataLinkSupport() {
       
        dataLinkEnabled=true;
        int selected=-1;
        //int anyIndex = -1;
        dataLinkButton.setForeground(Color.BLACK);
        int nrTabs = resultsPane.getTabCount();
        for(int i = 0; i < nrTabs; i++)
        {
           if (resultsPane.getIconAt(i) == null) { // no datalink service
               resultsPane.setEnabledAt(i, false);
           }
           else {
               if (resultsPane.getSelectedIndex() == i)
                   selected = i;
              
              // anyIndex = i;
              // resultsPane.setSelectedIndex(i);
           }
        }
        
        // if current selection is not a DataLink service do nothing
        if (selected < 0)
            return;
            //selected=anyIndex;
            //resultsPane.setSelectedIndex(selected);
        
        //getDataFrame.setService(resultsPane.getTitleAt(selected));
        if (dataLinkFrame.setServer(resultsPane.getTitleAt(selected)) == null)
            getDataFrame.setService(resultsPane.getTitleAt(selected));
            
    }
    /**
     * DeactivateGetDataSupport
     * activate all sites, without getData support
     */
  /*
   *   private void deactivateGetDataSupport() {
  
        getDataEnabled=false;
        getDataButton.setForeground(Color.GRAY);
        int nrTabs = resultsPane.getTabCount();
        for(int i = 0; i < nrTabs; i++)
        {
            resultsPane.setEnabledAt(i, true);      
        }
    }
  */  
    /**
     * DeactivateDataLinkSupport
     * activate all sites, without getData support
     */
    private void deactivateDataLinkSupport() {
        
        dataLinkEnabled=false;
        dataLinkButton.setForeground(Color.GRAY);
        int nrTabs = resultsPane.getTabCount();
        for(int i = 0; i < nrTabs; i++)
        {
            resultsPane.setEnabledAt(i, true);      
        }
    }

    /**
     * Event listener 
     * 
     */
    public void propertyChange(PropertyChangeEvent pvt) //!!!!!!!!!!!!!!!!!!
    {
        // trigger a metadata update if metadata has been added
        if (pvt.getPropertyName().equals("changeQuery")) {
            updateQueryText(); 
            String txt = "";
        }
            // update if the server list has been modifyed at ssaservertree (for example, new registry query)
        else if (pvt.getPropertyName().equals("changeServerlist")) {
            serverList = tree.getServerList();
            updateParameters();
            metaPanel.updateUI();
        }
    }
    
    private void updateQueryText() {
        
        if (metaPanel != null)
            extendedQueryText=metaPanel.getParamsQueryString(); 
           
            try {
                String txt = queryLine.getQueryURLText() + extendedQueryText;
                queryText.setText(txt /*queryLine.getQueryURLText() + extendedQueryText */);
                txt = null;
                
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }           
    }

    /**
     * showParameters
     * shows a list of optional metadata parameters that can be added to the query.
     * 
     */
    private void showParameters() 
    {
        // check serverlist (selected servers!!)
        // update serverlist
        serverList = tree.getServerList();
        ArrayList<String> parameters = new ArrayList();
        Iterator srv=serverList.getIterator();
        while ( srv.hasNext() ) {
            SSAPRegResource server = (SSAPRegResource) srv.next();
            if (serverList.isServerSelected(server.getShortName())) {
                ArrayList<String> serverpars = serverParam.getParams(server.getShortName());
                if (serverpars != null )
                    parameters.addAll(serverpars);
            }
        }
        // make the list unique 
        HashSet<String> hs = new HashSet<String>();
        hs.addAll(parameters);
        parameters.clear();
        parameters.addAll(hs);
        Collections.sort(parameters);
        
        // remove the INPUT: Prefix from the parameters
        for (int i=0; i<parameters.size(); i++) {
            String param = parameters.get(i); //.substring(6); // INPUT: = 6 characters
           // parameters.set(i,parameters.get(i).substring(6));// INPUT: = 6 characters
            metaPanel.addRow(metaParam.get(param), false); 
        
        
         
        Object selectedValue = JOptionPane.showInputDialog(this, "Supported Parameters", "Input", JOptionPane.INFORMATION_MESSAGE, null, 
                (Object[]) parameters.toArray(), null);
        if (selectedValue != null) {
            
            metaPanel.addRow(metaParam.get("INPUT:"+selectedValue.toString()), false); // INPUT: has to be added again
    //        metaPanel.
            metaPanel.setVisible(true);
        }
        metaPanel.repaint();
       // queryMetaParam.put(txtf, selectedValue.toString());
      //  JLabel label = new JLabel(selectedValue.toString() + ":");
      //  customQueryPanel.add(label); queryPanel.add(txtf);
       // customQueryPanel.repaint();
      
    }
    /**
     * Customize metadata parameters
     * Query metadata from known servers to retrieve all possible parameters. Every
     * query is performed in a different thread, which will perform the query and
     * update the parameter table
     */
    private void customParameters() 
    {
        int nrServers = serverList.getSize();
        final WorkQueue workQueue = new WorkQueue(nrServers);

        metaParam = new HashMap<String, MetadataInputParameter>();
     
/*
        if ( metaFrame == null ) {
            metaFrame = new SSAMetadataFrame( metaParam );
            metaFrame.addPropertyChangeListener(this);
        } else
            metaFrame.updateMetadata( metaParam );
*/
        final ProgressPanelFrame metadataProgressFrame = new ProgressPanelFrame( "Querying Metadata" );

        Iterator i = serverList.getIterator();

        while( i.hasNext() ) {

            final SSAPRegResource server = (SSAPRegResource) i.next();
            final ProgressPanel metadataProgressPanel = new ProgressPanel( "Querying: " + server.getShortName());
            metadataProgressFrame.addProgressPanel( metadataProgressPanel );
            //final MetadataQueryWorker queryWorker  = new MetadataQueryWorker(server, workQueue);
            final MetadataQueryWorker queryWorker  = new MetadataQueryWorker(workQueue, server, metadataProgressPanel);
            queryWorker.start();
            final MetadataProcessWorker processWorker  = new MetadataProcessWorker(workQueue);           
            processWorker.start();

        }// while


        /*
         *  XXX to do eventually: create the parameter frame only after the first parameter has been found. After all threads are
         *  done, show a dialog message in case no parameters have been found.
         */
       // tree.setParamMap(serverParam);
    } // customParameters

    /**
     *  Metadata Query worker class
     *  makes the query, adds resulting metadata to the queue, notify the metadata process worker 
     *  
     * @author mm
     *
     */

    class MetadataQueryWorker extends SwingWorker 
    {
        URL queryURL=null;
        SSAPRegResource server=null;
        ParamElement [] metadata = null;
        WorkQueue workQueue=null;
        ProgressPanel progressPanel = null;

        /**
         * Constructor
         * @param queue  the work queue
         * @param server the server which to query for metadata
         */
        public MetadataQueryWorker(WorkQueue queue, SSAPRegResource server) {
            this.server=server;
            this.workQueue=queue;
        }
        /**
         * Constructor
         * @param queue  the work queue
         * @param server the server which to query for metadata
         * @param panel  the progress panel
         */
        public MetadataQueryWorker(WorkQueue queue, SSAPRegResource server,ProgressPanel panel) {
            this.server=server;
            this.workQueue=queue;
            this.progressPanel=panel;
        }

        public Object construct()
        {   
            final SSAMetadataParser ssaMetaParser = new SSAMetadataParser( server );    
               if (progressPanel != null)
                progressPanel.start();            

            try {
                queryURL = ssaMetaParser.getQueryURL();
                logger.info( "Querying metadata from " + queryURL + " contact:" + server.getContact() );
            } catch (MalformedURLException e) {
                if (progressPanel != null)
                    progressPanel.logMessage("Malformed URL");
                queryURL=null;
            }
            if ( queryURL != null ) 
            {
                try {                 
                    if (progressPanel != null)
                        metadata = ssaMetaParser.queryMetadata( queryURL, progressPanel );
                    else
                        metadata = ssaMetaParser.queryMetadata( queryURL );

                } catch (InterruptedException e) {
                    if (progressPanel != null)
                        progressPanel.logMessage("Interrupted");
                    metadata = null;
                }
                catch (Exception e) {
                    if (progressPanel != null)
                        progressPanel.logMessage("Other Exception"); // this should not happen
                    e.printStackTrace();
                    metadata = null;
                }
            } else
                metadata = null;

            if (progressPanel != null)
                progressPanel.stop();  
            // add results to the queue
            workQueue.setServer(server);
            workQueue.addWork(metadata);
            return null;
        } //doinbackground

        public void finished()
        {
            //  display the final status of the query
            if ( metadata != null ) {
                // adds parameter information into the metaParam hash
                logger.info("RESPONSE "+queryURL+"returned "+ metadata.length +"parameters ");
                progressPanel.logMessage( metadata.length +"  input parameters found");
            } else {
                logger.info( "RESPONSE No input parameters loaded from " + queryURL );                
            }
            if (progressPanel != null)
                progressPanel.stop();  

        } // done

    };

    /**
     *  Metadata Process worker class
     *  makes the query, adds resulting metadata to the queue, notify the metadata process worker 
     *  
     * @author Margarida Castro Neves
     *
     */

    class MetadataProcessWorker extends SwingWorker 
    {
        WorkQueue workQueue=null;

        public MetadataProcessWorker( WorkQueue queue) {          
            this.workQueue=queue;
        }
        public Object construct()
        {
            // progressPanel.start();
            try {
                ParamElement [] data = workQueue.getWork();
                if ( data != null)
                    processMetadata(data, workQueue.getServer());
            } 
            catch (InterruptedException e) {
            }
            catch (Exception e) {
            }

            return null;
        } //construct


        public void finished()
        {
        } // done

    };



    /**
     * queue that receives information from the QueryWorker threads
     * and process them
     */
    class WorkQueue {
        LinkedList<Object> queue = new LinkedList<Object>();
        int workedItems = 0;
        int maxItems=0;
        SSAPRegResource server;

        public WorkQueue( int total ) {
            maxItems = total;
        }

        // add work to the queue
        public synchronized void addWork(Object o) {
            //  logger.info( "ADDWORK " + workedItems);
            queue.addLast(o);
            this.notify();
        }

        // takes the work from the queue as soon as it's not empty
        public synchronized ParamElement[] getWork() throws InterruptedException {
            //  logger.info( "GETWORK " + workedItems + " " + maxItems);
            if (workedItems >= maxItems) return null;
            while (queue.isEmpty()) {
                this.wait();
            }
            ParamElement [] data = (ParamElement[]) queue.removeFirst();     
            workedItems++;
            return (data);
        }
        
        public void setServer( SSAPRegResource server ) {
            this.server=server;
        }
        public SSAPRegResource getServer(  ) {
            return this.server;
        }
    } // WorkerQueue


    /**
     * adds the parameters to a hashmap. Every parameter should be unique, 
     * and a counter shows how many servers support each parameter  
     * Exclude the parameters that are already included in the main menues of splat query browser
     * @param - metadata the parameters read from all servers 
     * 
     */
    private void processMetadata( ParamElement[] metadata, SSAPRegResource server) {

        int i=0;
        //boolean changed = false;

        while ( i < metadata.length ) {
            String paramName = metadata[i].getName();
            if (! paramName.equalsIgnoreCase("INPUT:POS") &&        // these parameters should be entered in the main browser
                    ! paramName.equalsIgnoreCase("INPUT:SIZE") &&
                    ! paramName.equalsIgnoreCase("INPUT:BAND") &&
                    ! paramName.equalsIgnoreCase("INPUT:TIME") && 
                    ! paramName.equalsIgnoreCase("INPUT:FORMAT") && 
                    ! paramName.equalsIgnoreCase("INPUT:WAVECALIB") && 
                    ! paramName.equalsIgnoreCase("INPUT:FLUXCALIB") && 
                    ! paramName.equalsIgnoreCase("INPUT:TARGETNAME") ) 
            {
                addRelation( server.getShortName(), paramName);
                MetadataInputParameter mip = new MetadataInputParameter(metadata[i]);
                addMetaParam(mip);    // call static synchronized method to change the data with mutual exclusion             
            } // if
            i++;
        } // while

    }//processMetadata
    private synchronized static void addRelation( String server, String param ) {
        
        serverParam.addRelation(server, param);

    }

    /*
     * Adds parameters to the hash and to the parameter table
     * 
     * @param mip - the metadata parameter object
     */
    private synchronized static void addMetaParam( MetadataInputParameter mip) {

        if (! metaParam.containsKey(mip.getName())) 
        {
            metaParam.put( mip.getName(), mip );
            // add new element to the parameter table
         //   metaPanel.addRow(mip); //!! frame

        } else {
            // increase counter of existing hash member
            mip = metaParam.get(mip.getName());
            mip.increase();
            metaParam.put( mip.getName(), mip );
            // update nr server column in the table
    //        metaPanel.setNrServers(mip.getCounter(), mip.getName()); // !! Frame
            
        }
        
        /* 
        Object selectedValue = JOptionPane.showInputDialog(this, "Supported Parameters", "Input", JOptionPane.INFORMATION_MESSAGE, null, 
                (Object[]) parameters.toArray(), null);
        if (selectedValue != null) {
            
            metaPanel.addRow(metaParam.get("INPUT:"+selectedValue.toString()), false); // INPUT: has to be added again
    //        metaPanel.
            metaPanel.setVisible(true);
        }
        */
        metaPanel.setVisible(true);
        metaPanel.repaint();
       // queryMetaParam.put(txtf, selectedValue.toString());
      //  JLabel label = new JLabel(selectedValue.toString() + ":");
      //  customQueryPanel.add(label); queryPanel.add(txtf);
       // customQueryPanel.repaint();
      
    }
    
    /**
     * updateParameters
     * updates the list of optional metadata parameters that can be added to the query.
     * 
     */
    private void updateParameters() {
        metaPanel.removeAll();
        showParameters();
    }
    /**
     * Customize metadata parameters
     * Query metadata from known servers to retrieve all possible parameters. Every
     * query is performed in a different thread, which will perform the query and
     * update the parameter table
     */
    private void customParameters() 
    {
        int nrServers = serverList.getSize();
        final WorkQueue workQueue = new WorkQueue(nrServers);

        metaParam = new HashMap<String, MetadataInputParameter>();
     
/*
        if ( metaFrame == null ) {
            metaFrame = new SSAMetadataFrame( metaParam );
            metaFrame.addPropertyChangeListener(this);
        } else
            metaFrame.updateMetadata( metaParam );
*/
        final ProgressPanelFrame metadataProgressFrame = new ProgressPanelFrame( "Querying Metadata" );

        Iterator i = serverList.getIterator();

        while( i.hasNext() ) {

            final SSAPRegResource server = (SSAPRegResource) i.next();
            final ProgressPanel metadataProgressPanel = new ProgressPanel( "Querying: " + server.getShortName());
            metadataProgressFrame.addProgressPanel( metadataProgressPanel );
            //final MetadataQueryWorker queryWorker  = new MetadataQueryWorker(server, workQueue);
            final MetadataQueryWorker queryWorker  = new MetadataQueryWorker(workQueue, server, metadataProgressPanel);
            queryWorker.start();
            final MetadataProcessWorker processWorker  = new MetadataProcessWorker(workQueue);           
            processWorker.start();

        }// while


        /*
         *  XXX to do eventually: create the parameter frame only after the first parameter has been found. After all threads are
         *  done, show a dialog message in case no parameters have been found.
         */
       // tree.setParamMap(serverParam);
    } // customParameters

    /**
     * Metadata Query worker class makes the query, adds resulting metadata to the queue, notify the metadata process worker 
     * @author  mm
     */

    class MetadataQueryWorker extends SwingWorker 
    {
        URL queryURL=null;
        /**
         * @uml.property  name="server"
         * @uml.associationEnd  
         */
        SSAPRegResource server=null;
        ParamElement [] metadata = null;
        /**
         * @uml.property  name="workQueue"
         * @uml.associationEnd  
         */
        WorkQueue workQueue=null;
        /**
         * @uml.property  name="progressPanel"
         * @uml.associationEnd  
         */
        ProgressPanel progressPanel = null;

        /**
         * Constructor
         * @param queue  the work queue
         * @param server the server which to query for metadata
         */
        public MetadataQueryWorker(WorkQueue queue, SSAPRegResource server) {
            this.server=server;
            this.workQueue=queue;
        }
        /**
         * Constructor
         * @param queue  the work queue
         * @param server the server which to query for metadata
         * @param panel  the progress panel
         */
        public MetadataQueryWorker(WorkQueue queue, SSAPRegResource server,ProgressPanel panel) {
            this.server=server;
            this.workQueue=queue;
            this.progressPanel=panel;
        }

        public Object construct()
        {   
            final SSAMetadataParser ssaMetaParser = new SSAMetadataParser( server );    
               if (progressPanel != null)
                progressPanel.start();            

            try {
                queryURL = ssaMetaParser.getQueryURL();
                logger.info( "Querying metadata from " + queryURL + " contact:" + server.getContact() );
            } catch (MalformedURLException e) {
                if (progressPanel != null)
                    progressPanel.logMessage("Malformed URL");
                queryURL=null;
            }
            if ( queryURL != null ) 
            {
                try {                 
                    if (progressPanel != null)
                        metadata = ssaMetaParser.queryMetadata( queryURL, progressPanel );
                    else
                        metadata = ssaMetaParser.queryMetadata( queryURL );

                } catch (InterruptedException e) {
                    if (progressPanel != null)
                        progressPanel.logMessage("Interrupted");
                    metadata = null;
                }
                catch (Exception e) {
                    if (progressPanel != null)
                        progressPanel.logMessage("Other Exception"); // this should not happen
                    e.printStackTrace();
                    metadata = null;
                }
            } else
                metadata = null;

            if (progressPanel != null)
                progressPanel.stop();  
            // add results to the queue
            workQueue.setServer(server);
            workQueue.addWork(metadata);
            return null;
        } //doinbackground

        public void finished()
        {
            //  display the final status of the query
            if ( metadata != null ) {
                // adds parameter information into the metaParam hash
                logger.info("RESPONSE "+queryURL+"returned "+ metadata.length +"parameters ");
                progressPanel.logMessage( metadata.length +"  input parameters found");
            } else {
                logger.info( "RESPONSE No input parameters loaded from " + queryURL );                
            }
            if (progressPanel != null)
                progressPanel.stop();  

        } // done

    };

    /**
     * Metadata Process worker class makes the query, adds resulting metadata to the queue, notify the metadata process worker 
     * @author  Margarida Castro Neves
     */

    class MetadataProcessWorker extends SwingWorker 
    {
        /**
         * @uml.property  name="workQueue"
         * @uml.associationEnd  
         */
        WorkQueue workQueue=null;

        public MetadataProcessWorker( WorkQueue queue) {          
            this.workQueue=queue;
        }
        public Object construct()
        {
            // progressPanel.start();
            try {
                ParamElement [] data = workQueue.getWork();
                if ( data != null)
                    processMetadata(data, workQueue.getServer());
            } 
            catch (InterruptedException e) {
            }
            catch (Exception e) {
            }

            return null;
        } //construct


        public void finished()
        {
        } // done

    };



    /**
     * queue that receives information from the QueryWorker threads and process them
     */
    class WorkQueue {
        LinkedList<Object> queue = new LinkedList<Object>();
        int workedItems = 0;
        int maxItems=0;
        /**
         * @uml.property  name="server"
         * @uml.associationEnd  
         */
        SSAPRegResource server;

        public WorkQueue( int total ) {
            maxItems = total;
        }

        // add work to the queue
        public synchronized void addWork(Object o) {
            //  logger.info( "ADDWORK " + workedItems);
            queue.addLast(o);
            this.notify();
        }

        // takes the work from the queue as soon as it's not empty
        public synchronized ParamElement[] getWork() throws InterruptedException {
            //  logger.info( "GETWORK " + workedItems + " " + maxItems);
            if (workedItems >= maxItems) return null;
            while (queue.isEmpty()) {
                this.wait();
            }
            ParamElement [] data = (ParamElement[]) queue.removeFirst();     
            workedItems++;
            return (data);
        }
        
        /**
         * @param server
         * @uml.property  name="server"
         */
        public void setServer( SSAPRegResource server ) {
            this.server=server;
        }
        /**
         * @return
         * @uml.property  name="server"
         */
        public SSAPRegResource getServer(  ) {
            return this.server;
        }
    } // WorkerQueue


    /**
     * adds the parameters to a hashmap. Every parameter should be unique, 
     * and a counter shows how many servers support each parameter  
     * Exclude the parameters that are already included in the main menues of splat query browser
     * @param - metadata the parameters read from all servers 
     * 
     */
    private void processMetadata( ParamElement[] metadata, SSAPRegResource server) {

        int i=0;
        //boolean changed = false;

        while ( i < metadata.length ) {
            String paramName = metadata[i].getName();
            if (! paramName.equalsIgnoreCase("INPUT:POS") &&        // these parameters should be entered in the main browser
                    ! paramName.equalsIgnoreCase("INPUT:SIZE") &&
                    ! paramName.equalsIgnoreCase("INPUT:BAND") &&
                    ! paramName.equalsIgnoreCase("INPUT:TIME") && 
                    ! paramName.equalsIgnoreCase("INPUT:FORMAT") && 
                    ! paramName.equalsIgnoreCase("INPUT:WAVECALIB") && 
                    ! paramName.equalsIgnoreCase("INPUT:FLUXCALIB") && 
                    ! paramName.equalsIgnoreCase("INPUT:TARGETNAME") ) 
            {
                addRelation( server.getShortName(), paramName);
                MetadataInputParameter mip = new MetadataInputParameter(metadata[i]);
                addMetaParam(mip);    // call static synchronized method to change the data with mutual exclusion             
            } // if
            i++;
        } // while

    }//processMetadata
    private synchronized static void addRelation( String server, String param ) {
        
        serverParam.addRelation(server, param);

    }

    /*
     * Adds parameters to the hash and to the parameter table
     * 
     * @param mip - the metadata parameter object
     */
    private synchronized static void addMetaParam( MetadataInputParameter mip) {

        if (! metaParam.containsKey(mip.getName())) 
        {
            metaParam.put( mip.getName(), mip );
            // add new element to the parameter table
         //   metaPanel.addRow(mip); //!! frame

        } else {
            // increase counter of existing hash member
            mip = metaParam.get(mip.getName());
            mip.increase();
            metaParam.put( mip.getName(), mip );
            // update nr server column in the table
    //        metaPanel.setNrServers(mip.getCounter(), mip.getName()); // !! Frame
        }
        
    }
    //
    // MouseListener interface. Double clicks display the clicked spectrum.
    //
    public void mousePressed( MouseEvent e ) {}
    public void mouseReleased( MouseEvent e ) {}
    public void mouseEntered( MouseEvent e ) {}
    public void mouseExited( MouseEvent e ) {}
    public void mouseClicked( MouseEvent e )
    {
       
        //requestFocusInWindow();
     //   if (e.getSource().getClass() == StarTable.class ) {

            if ( e.getClickCount() == 2 ) {
                StarJTable table = (StarJTable) e.getSource();
                Point p = e.getPoint();
                int row = table.rowAtPoint( p );
                displaySpectra( false, true, table, row );
            }
      //  }
    }

    //
    //  Action for switching name resolvers.
    //
    class ResolverAction
    extends AbstractAction
    {
        SkycatCatalog resolver = null;
        public ResolverAction( String name, SkycatCatalog resolver )
        {
            super( name );
            this.resolver = resolver;
        }
        public void actionPerformed( ActionEvent e )
        {
            resolverCatalogue = resolver;
        }
    }

    //
    // LocalAction to encapsulate all trivial local Actions into one class.
    //
    class LocalAction
    extends AbstractAction
    {
        //  Types of action.
        public static final int PROXY = 0;
        public static final int SERVER = 1;
        public static final int SAVE = 2;
        public static final int READ = 3;
        public static final int CLOSE = 4;

        //  The type of this instance.
        private int actionType = PROXY;

        public LocalAction( int actionType, String name )
        {
            super( name );
            this.actionType = actionType;
        }

        public LocalAction( int actionType, String name, Icon icon,
                String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );
            this.actionType = actionType;
        }

        public LocalAction( int actionType, String name, Icon icon,
                String help, String accel )
        {
            this( actionType, name, icon, help );
            putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( accel ) );
        }

        public void actionPerformed( ActionEvent ae )
        {
            switch ( actionType )
            {
                case PROXY: {
                    showProxyDialog();
                    break;
                }
                case SERVER: {
                 //   showServerWindow();
                    break;
                }
                case SAVE: {
                    saveQueryToFile();
                    break;
                }
                case READ: {
                    readQueryFromFile();
                    break;
                }
                case CLOSE: {
                    closeWindowEvent();
                    break;
                }
            }
        }
    }


 
    /**
     * A Metadata input parameter class, to contain the parameter element and the number of servers that accept this parameter
     */

    public class MetadataInputParameter
    {
        ParamElement param;
        String paramName = null;
        
        String description = null;
        
        String unit = null;
        String UCD = null;
        String value = null;
        String nullValue = null;
        String[] options;
        //ValuesElement values = null;
       
        String datatype = null;
        String min=null;    
        String max=null;


       
        int counter;

        MetadataInputParameter(ParamElement param) {

            this.param = param; // TO DO  decide if it is really necessary to keep this object here...
            counter=1; 
            paramName = param.getName();
            description = param.getDescription();
            if (description == null)
                description = "";
            datatype = param.getDatatype();
            value = param.getValue();
            //if (value.equals("NaN") || value.contains("null") || value.contains("NULL") || value.contains("Null"))
            //    value = "";
            unit = param.getUnit();
            
            UCD = param.getUcd();
            
            /* To be done later after some questions have been cleared:
             * 
             * 
    		ValuesElement values = param.getActualValues(); // or legal values??
    		if  (values != null) {
    		        options = values.getOptions();
    		        max = values.getMaximum();
    		        min = values.getMinimum();
    		        nullValue=values.getNull();
    		}
             */	    		
        }
        /**
         * @return
         * @uml.property  name="counter"
         */
        protected int getCounter() {
            return counter;
        }
        protected String getName() {
            return paramName;
        }
        /**
         * @return
         * @uml.property  name="value"
         */
        protected String getValue() {
            return value;
        }
        /**
         * @return
         * @uml.property  name="description"
         */
        protected String getDescription() {
            return description;
        }
        /**
         * @return
         * @uml.property  name="datatype"
         */
        protected String getDatatype() {
            return datatype;
        }
        /**
         * @return
         * @uml.property  name="unit"
         */
        protected String getUnit() {
            return unit;
        }
        /**
         * @return
         * @uml.property  name="UCD"
         */
        protected String getUCD() {
            return UCD;
        }
        /*
    	protected void setDescription(String description) {
    		 this.description = description;
    	}
    	protected void setDatatype(String datatype) {
   		 this.datatype = datatype;
    	}
    	protected void setUnit(String unit) {
      		 this.datatype = unit;
       	}
         */
        protected void increase() {
            counter++;
        }
    } // MetadataInputParameter


    public void changedUpdate(DocumentEvent de) {
        // Plain text components don't fire these events.
    }

    public void insertUpdate(DocumentEvent de) {
        changeUpdate(de);
    }
    public void removeUpdate(DocumentEvent de) {
        changeUpdate(de);
    }
    private void changeUpdate(DocumentEvent de) {
        //get the owner of this document
        Object owner = de.getDocument().getProperty("owner");
        if(owner != null){
            if (owner == nameField ) {
                nameField.setForeground(Color.black);
                queryLine.setTargetName(nameField.getText());
            }
            if (owner == radiusField ) {
                String radiusText = radiusField.getText();
                double radius = 0.0;
                if ( radiusText != null && radiusText.length() > 0 ) {
                    try {
                        radius = Double.parseDouble( radiusText );
                    }
                    catch (NumberFormatException e1) {
                        radiusField.setForeground(Color.red);
                        //ErrorDialog.showError( this, "Cannot understand radius value", e1);                         
                        return;
                    }
                    radiusField.setForeground(Color.black);
                }
                queryLine.setRadius(radius);
            }
            if (owner == raField || owner == decField ) {
                if (raField.getText().length() > 0 && decField.getText().length() > 0 ) {
                    try {
                            queryLine.setPosition(raField.getText(), decField.getText());
                    } catch (NumberFormatException nfe) {
                       // if (owner == raField )
                            raField.setForeground(Color.red);
                     //   else 
                            decField.setForeground(Color.red);
                       // ErrorDialog.showError( this, "Invalid coordinate format", nfe);
                        return;
                    }
                    raField.setForeground(Color.black);
                    decField.setForeground(Color.black);
                }
            }
           
            if (owner == upperBandField || owner == lowerBandField ) {
                queryLine.setBand(lowerBandField.getText(), upperBandField.getText());
                //if (getDataFrame != null)
                //    getDataFrame.setBandParams(lowerBandField.getText(), upperBandField.getText());
            }
            
            if (owner == upperTimeField || owner == lowerTimeField) {
                queryLine.setTime(lowerTimeField.getText(), upperTimeField.getText());
            }
            
        }
        updateQueryText();
        
    }

    /*     public void removeUpdate(DocumentEvent de) {
        
        
        Object owner = de.getDocument().getProperty("owner");
        
        if(owner != null){
            if (owner == nameField ) {
                nameField.setForeground(Color.black);
                queryLine.setTargetName(nameField.getText());
                // when name is changed, remove current coordinates.
                queryLine.setPosition(0, 0);
                raField.setText("");
                decField.setText("");
            } 
            if (owner == radiusField) {
                
            }
            if (owner == raField || owner == decField ) {
                 if  (! isLookup ) {
                     // coordinate manually changed ( not through lookup)
                   nameField.setForeground(Color.gray);
                } else { 
                    nameField.setForeground(Color.black);
                }
            
                if (raField.getText().length() > 0 && decField.getText().length() > 0 ) {
                    try {
                            queryLine.setPosition(raField.getText(), decField.getText());
                    } catch (NumberFormatException nfe) {
                     //   if (owner == raField ) 
                            raField.setForeground(Color.red);
                     //   else 
                            decField.setForeground(Color.red);
                        //ErrorDialog.showError( this, "Invalid coordinate format", nfe);
                        return;
                    }
                    raField.setForeground(Color.black);
                    decField.setForeground(Color.black);
                //nameField.setText("");
                }               
            }
        }
        updateQueryText();
        
    }
 */   
}
