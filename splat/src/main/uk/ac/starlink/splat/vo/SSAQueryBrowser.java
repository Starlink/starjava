/*
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
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.xml.sax.InputSource;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.coords.Coordinates;
import jsky.coords.WorldCoords;
import jsky.util.SwingWorker;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.AbstractServerPanel;
import uk.ac.starlink.splat.iface.HelpFrame;
import uk.ac.starlink.splat.iface.ProgressPanel;
import uk.ac.starlink.splat.iface.SpectrumIO;
import uk.ac.starlink.splat.iface.SpectrumIO.Props;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.EventEnabledTransmitter;
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
implements ActionListener, DocumentListener, PropertyChangeListener
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
     * Clear input fields button
     * @uml.property  name="clear"
     * @uml.associationEnd  
     */
    protected JButton clearButton = null;

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
  
    /**
     * Display  DATA Link parameters and activation status
     * @uml.property  name="dataLinkButton"
     * @uml.associationEnd  
     */
    protected JToggleButton  dataLinkButton;
    
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
     * Region MAXREC
     * @uml.property  name="maxrexField"
     * @uml.associationEnd  
     */
    protected JTextField maxRecField = null;

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
     * Results panel
     */
     protected ResultsPanel resultsPanel = null;
     
    /**
     * Tabbed pane showing the query results tables
     * @uml.property  name="resultsPane"
     * @uml.associationEnd  
     */
  //  protected JTabbedPane resultsPane = null;

    /**
     * The list of StarJTables in use
     * @uml.property  name="starJTables"
     * @uml.associationEnd  multiplicity="(0 -1)" elementType="uk.ac.starlink.table.gui.StarJTable"
     */
    protected ArrayList<StarPopupTable> starJTables = null;

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
    
    /**
     * The Button for querying services for parameters
     * @uml.property  name="updateParamsButton"
     * @uml.associationEnd  
     */
    protected JButton updateParamsButton = null;
    
    /** The list of all input parameters read from the servers */
   // protected static SSAMetadataFrame metaFrame = null;
    protected static SSAMetadataPanel metaPanel = null;
    
    /**
     * SAMP transmitter for selected FITS results
     */
    protected EventEnabledTransmitter binFITSTransmitter;
    
    /**
     * SAMP transmitter for selected VOTable results
     */
    protected EventEnabledTransmitter voTableTransmitter;

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
  //  private static HashMap<String, MetadataInputParameter> metaParam=null;  
    
    /** The list of all input parameters read from the servers as a hash map */
    //private static HashMap< JTextField, String > queryMetaParam=null; 
    
    
    /** the authenticator for access control **/
    private static SSAPAuthenticator authenticator;
       
    /**
     * the Panel handling with services and options
     * @uml.property  name="serverPanel"
     * @uml.associationEnd  
     */
    private SSAServerTable serverPanel;
    
   
    static ProgressPanelFrame progressFrame = null;

    /**
     * @uml.property  name="dataLinkFrame"
     * @uml.associationEnd  
     */
    private DataLinkQueryFrame dataLinkFrame = null;

    // private JPopupMenu specPopupMenu;

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
       // queryMetaParam = new HashMap< JTextField, String >();
                
        metaPanel = new SSAMetadataPanel();
        metaPanel.addPropertyChangeListener(this);

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
                
        JSplitPane splitPanel = new JSplitPane();
        splitPanel.setOneTouchExpandable(true);
        splitPanel.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        splitPanel.setDividerLocation(0.3);
      
       
        this.add(splitPanel);
        leftPanel = initServerComponents();
   
      
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
         
         splitPanel.setLeftComponent(leftPanel); // the server selection area
         splitPanel.setRightComponent(centrePanel); // the query area
        
         
         contentPane.add(splitPanel);
     
    }


    public JPanel initServerComponents()
    {
        JPanel sp = new JPanel();
        sp.setLayout(new BoxLayout(sp, BoxLayout.Y_AXIS));
        sp.setAlignmentY((float) 1.);

        serverPanel=new SSAServerTable( serverList );
        serverPanel.addPropertyChangeListener(this);
        sp.add(serverPanel);
        return sp;
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
       
        jrbmi.setToolTipText
        ( "CDS Sesame service queries SIMBAD, NED and Vizier" );

        resolverCatalogue = null;
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
        
        interopMenu.addSeparator();
        binFITSTransmitter = communicator.createBinFITSTableTransmitter( this );
        interopMenu.add( binFITSTransmitter.getBroadcastAction() );
        interopMenu.add( binFITSTransmitter.createSendMenu() );
        
        interopMenu.addSeparator();
        voTableTransmitter = communicator.createVOTableTransmitter( this );
        interopMenu.add( voTableTransmitter.getBroadcastAction() );
        interopMenu.add( voTableTransmitter.createSendMenu() );
        

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

        GridBagLayouter layouter =  new GridBagLayouter( simpleQueryPanel, GridBagLayouter.SCHEME4 ); // label:[field] label;[field]

        //  Object name. Arrange for a resolver to look up the coordinates of
        //  the object name, when return or the lookup button are pressed.
        JLabel nameLabel = new JLabel( "Object:" );
        nameField = new JTextField( 15 );
        nameField.setFocusable(true);
        nameField.setToolTipText( "Enter the name of an object " +
                " -- press return to get coordinates" );
        nameField.addActionListener( this );
        nameField.getDocument().putProperty("owner", nameField); //set the owner
        nameField.getDocument().addDocumentListener( this );
          
        JPanel clearLookupPanel = new JPanel();
        nameLookup = new JButton( "Lookup" );
        nameLookup.addActionListener( this );
        nameLookup.setToolTipText( "Press to get coordinates of Object" );
        clearLookupPanel.add(nameLookup);
        
        clearButton = new JButton( "Clear" );
        clearButton.addActionListener( this );
        clearButton.setToolTipText( "Clear all fields" );
        clearLookupPanel.add(clearButton);
        
        JPanel objPanel = new JPanel( new GridBagLayout());
        GridBagConstraints gbcs = new GridBagConstraints();
        gbcs.weightx = 1.0;
        gbcs.fill = GridBagConstraints.HORIZONTAL;
        gbcs.ipadx = 15;
        gbcs.gridx=0;
        objPanel.add(nameField, gbcs);
        gbcs.gridx=1;
        objPanel.add(nameLookup, gbcs);
        gbcs.gridx=2;
        objPanel.add(clearButton, gbcs);
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
        radiusField = new JTextField( "10.0", 12 );
        queryLine.setRadius(10.0);
        radiusField.addActionListener( this );
  //      layouter.add( radiusLabel, false );
  //      layouter.add( radiusField, false );
        radiusField.setToolTipText( "Enter radius of field to search" +
                " from given centre, arcminutes" );
        radiusField.addActionListener( this );
        radiusField.getDocument().putProperty("owner", radiusField); //set the owner
        radiusField.getDocument().addDocumentListener( this );
        
        // Maxrec Field
        
        JLabel maxRecLabel = new JLabel( "MAXREC:" );
        maxRecField = new JTextField( "", 9 );
        maxRecField.addActionListener( this );
     //   layouter.add( maxRecLabel, false );
    //    layouter.add( maxRecField, true );
        maxRecField.setToolTipText( "The maximum number of records to be returned from a service" );
        maxRecField.addActionListener( this );
        maxRecField.getDocument().putProperty("owner", maxRecField); //set the owner
        maxRecField.getDocument().addDocumentListener( this );
        JPanel radmaxrecPanel = new JPanel( new GridBagLayout() );
        
        gbc = new GridBagConstraints();
     
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        radmaxrecPanel.add( radiusField, gbc );
        gbc.weightx=0.0;
        gbc.fill = GridBagConstraints.NONE;
        radmaxrecPanel.add(maxRecLabel, gbc);
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        radmaxrecPanel.add(maxRecField, gbc);
        
        layouter.add(radiusLabel, false);
        layouter.add(radmaxrecPanel,true);

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
        String[] formats =  { "None", "ALL", "COMPLIANT", "votable", "fits", "xml", "native", "image/fits" };
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
        
        updateParamsButton = new JButton("Update");
        updateParamsButton.addActionListener( this );
        updateParamsButton.setToolTipText("query services to update parameters");
        updateParamsButton.setMargin(new Insets(2,2,2,2));  
    //    optionalQueryPanel.add(deselectAllParamsButton, BorderLayout.EAST);
        paramButtonsPanel.add( selectAllParamsButton );
        paramButtonsPanel.add( deselectAllParamsButton );
        paramButtonsPanel.add( updateParamsButton );
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
        
        JTabbedPane resultsPane = new JTabbedPane();
  
//        resultsPane.setPreferredSize(new (600,310));
        
        resultsPanel = new ResultsPanel(resultsPane, this);
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
            { "None", "ALL", "COMPLIANT", "votable", "fits", "xml", "native", "image/fits" };
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
                            //isLookup=true;
                            raField.setText( radec[0] );
                            decField.setText( radec[1] );
                            nameField.setForeground(Color.black);
                            //isLookup=false;
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

        try {
            queryLine.setPosition(ra, dec);
        } catch (NumberFormatException e) {
            ErrorDialog.showError( this, "Position input error", e );
            return;
        }
        
       
        // update serverlist from serverTable class
        final SSAServerList slist=(SSAServerList) serverPanel.getServerList();
        
        //  Create a stack of all queries to perform.
        ArrayList<SSAQuery> queryList = new ArrayList<SSAQuery>();
        
        // SSAP recommended and optional parameters - service will be queried even if not supported
        String [] recParams = {"APERTURE", "SPECRP", "SPATRES", "TIMERES", "SNR", "REDSHIFT", "VARAMPL", 
                "TARGETCLASS", "PUBDID", "CREATORDID", "COLLECTION", "TOP", "MTIME", "MAXREC", "RUNID" };
      
        ArrayList <String>  recommendedParams = new ArrayList<String>();
        for (int i=0;i<recParams.length;i++)
            recommendedParams.add(recParams[i]);
        
        Iterator i = slist.getIterator();

        SSAPRegResource server = null;
        while( i.hasNext() ) {
            server = (SSAPRegResource) i.next();
            if (server != null )
                try {
                    
                    if (serverPanel.isServerSelected(server.getShortName())) {

                        SSAQuery ssaQuery =  new SSAQuery( server );
                        // ssaQuery.setServer(server) ; //Parameters(queryLine); // copy the query parameters to the new query
                        ssaQuery.setQuery(queryText.getText());
                        ArrayList<String> extp = ssaQuery.getParamList(queryText.getText()); 
                        if ( extp != null ) {

                            boolean supportsAll = true;
                         //   boolean supportsSome = false;
                            for (int j=0; j<extp.size(); j++) {
                                if (! recommendedParams.contains( extp.get(j)) ) {
                                    // parameter is a service specific parameter - query only ifsuported
                                    supportsAll = supportsAll && paramSupported(server, extp.get(j));
                                }
                            }
                            if (supportsAll )  
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

    private boolean paramSupported(SSAPRegResource server, String param) {
        
        ArrayList<MetadataInputParameter> mips = (ArrayList<MetadataInputParameter>) server.getMetadata();
        for (int i=0;i<mips.size();i++)
            if (mips.get(i).getName().replace("INPUT:", "").equals(param))
                return true;
        return false;
        
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

        DataLinkParams dataLinkParams = null;
      
        URL queryURL = null;

        logger.info( "Querying: " + queryURL );
        progressPanel.logMessage( ssaQuery.getBaseURL() );
      
        try {             
                queryURL = ssaQuery.getRequestURL();             
                logger.info( "Query string " + queryURL.toString() );
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
            //  Handle redirects
            if ( con instanceof HttpURLConnection ) {
                int code = ((HttpURLConnection)con).getResponseCode();
                
               
                if ( code == HttpURLConnection.HTTP_MOVED_PERM ||
                     code == HttpURLConnection.HTTP_MOVED_TEMP ||
                     code == HttpURLConnection.HTTP_SEE_OTHER ) {
                    String newloc = con.getHeaderField( "Location" );
                    ssaQuery.setServer(newloc);
                    URL newurl = ssaQuery.getRequestURL();
                    con = newurl.openConnection();
                }
                
            }
           
            con.setConnectTimeout(10 * 1000); // 10 seconds
            con.setReadTimeout(30*1000);
            con.connect();
            
            InputSource inSrc = new InputSource( con.getInputStream() );
                 
            // inSrc.setSystemId( ssaQuery.getBaseURL() );
            inSrc.setSystemId( queryURL.toString());
            
            VOElementFactory vofact = new VOElementFactory();
            
            VOElement voe = DalResourceXMLFilter.parseDalResult(vofact, inSrc);
           
            starTable = DalResourceXMLFilter.getDalResultTable( voe );
            
            // if the VOTable contains datalink service definitions, add to the SSAQuery.
            dataLinkParams = DalResourceXMLFilter.getDalGetServiceElement(voe); 
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
            starJTables = new ArrayList<StarPopupTable>();
        }

        //  Remove existing tables.
        //resultsPane.removeAll();
        resultsPanel.removeAllResults();
        starJTables.clear();

        if ( tableList != null ) {
            dataLinkFrame = resultsPanel.getDataLinkFrame();
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
        StarPopupTable table = null;
        StarTable starTable = null;
        DataLinkParams  dataLinkParams = null;
        String shortName = null;
     
        //boolean hasParams = false;
       
        ImageIcon cutImage = new ImageIcon( ImageHolder.class.getResource("smallcutter.gif") );
   
        if ( next instanceof SSAQuery && next != null ) {
            ssaQuery = (SSAQuery) next; 
            starTable = ssaQuery.getStarTable();
            // check for duplicate pubdid and different mimetypes
            // !! assume for now that tables are sorted by pubdid - change later !!!!
            // make one line
        
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
                table = new StarPopupTable( starTable, true );
                table.rearrangeSSAP();
                //table.removeduplicates();
               
                if (dataLinkParams != null) { // if datalink services are present, create a frame
                    
                    if ( dataLinkFrame == null ) {
                         dataLinkFrame = new DataLinkQueryFrame();
                    } 
                    dataLinkFrame.addServer(shortName, dataLinkParams);  // associate this datalink service information to the current server
                    resultsPanel.enableDataLink(dataLinkFrame);
                    resultsPanel.addTab(shortName, cutImage, table );
                }
                else {
                    if  (dataLinkFrame != null && dataLinkFrame.getServerParams(shortName) != null )  // if table is read from a file, dataLinkFrame has already been set                         
                        resultsPanel.addTab( shortName, cutImage, table );
                    else 
                        resultsPanel.addTab( shortName, table );
                }
                starJTables.add( table );

                //  Set widths of columns.
                if ( nrows >= 1 ) {
                    nrows = ( nrows > 5 ) ? 5 : nrows;
                    table.configureColumnWidths( 200, nrows );
                }

                //  Double click on row means load just that spectrum.
                table.addMouseListener( binFITSTransmitter );
                table.addMouseListener( voTableTransmitter );
                table.addMouseListener( resultsPanel );
            }
        }
    }

       
    /**
     * Deselect all spectra in the visible table, or deselect all tables.
     */
    protected void deselectSpectra( boolean all, Component c )
    {
        if (starJTables == null)  // avoids NPE if no results are present
            return;
        if ( all ) {
            //  Visit all the tabbed StarJTables.
            Iterator<StarPopupTable> i = starJTables.iterator();
            while ( i.hasNext() ) {
                i.next().clearSelection();
            }
        }
        else {
            //Component c = resultsPane.getSelectedComponent();
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
    protected void displaySpectra( Props[] propList, boolean display)

    {

        browser.threadLoadSpectra( propList, display );
        browser.toFront();
    }
    
    public List<Props> getSpectraAsList(boolean selected) {
        return resultsPanel.getSpectraAsList(selected, null, -1);
    }
    
    
    /**
     *  Restore a set of previous query results that have been written to a
     *  VOTable. The file name is obtained interactively.
     */
    public void readQueryFromFile()
    {
        
        ArrayList<VOStarTable> tableList = resultsPanel.readQueryFromFile();
        if ( tableList != null && ! tableList.isEmpty() )
            makeResultsDisplay( tableList );
        
    }

 
    /**
     * Return a StarTable of the currently selected tab of query results.
     */
    public StarTable getCurrentTable()
    {
        if ( starJTables != null && starJTables.size() > 0 ) {
            int index = resultsPanel.getSelectedIndex();
            if ( index > -1 ) {
                StarJTable jTable = starJTables.get( index );
                return jTable.getStarTable();
            }
        }
        return null;
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
        //String cmd = e.getActionCommand();

        if ( source.equals( goButton ) ) {
            {
                if (dataLinkFrame != null) 
                    dataLinkFrame.setVisible(false);
                resultsPanel.deactivateDataLinkSupport(); 
                resultsPanel.removeDataLinkButton();
                doQuery();
               
            }        
            return;
        } 
        if ( source.equals( clearButton ) ) {
 
            double defaultRadius=10.0;
            raField.setText("");
            decField.setText("");
            nameField.setText("");
            radiusField.setText(Double.toString(defaultRadius));//default value
            lowerBandField.setText("");
            lowerTimeField.setText("");
            upperTimeField.setText("");
            queryLine = new SSAQuery("<SERVER>");
            queryLine.setRadius(defaultRadius);
            updateQueryText();

            return;
        } 
        if ( source.equals( nameLookup ) /*|| source.equals( nameField ) */) {
           
            resolveName();
           
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
            if ( source.equals( maxRecField )  ) {
                
                String maxrecText = maxRecField.getText();
                int maxrec = 0;
                if ( maxrecText != null && maxrecText.length() > 0 ) {
                    try {
                        maxrec = Integer.parseInt(maxrecText);
                    }
                    catch (NumberFormatException e1) {
                        ErrorDialog.showError( this, "Cannot understand maxrec value", e1);                         
                        return;
                    }
                }
                queryLine.setMaxrec(maxrec);
            
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
                   
                    try {
                        queryLine.setFormat(formatList.getSelectedItem().toString());
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
          
            return;
        } 
        if ( source.equals( deselectAllParamsButton ) ) 
        {
            
            metaPanel.deselectAll();
           // metaPanel.removeSelectedMetadata();
        
            return;
        } 


    }


    /**
     * Event listener 
     * 
     */
    public void propertyChange(PropertyChangeEvent pvt)
    {
        // trigger a metadata update if metadata has been added
        if (pvt.getPropertyName().equals("changeQuery")) {
            updateQueryText();
        }
        else if (pvt.getPropertyName().equals("changedValue")) {
            serverList = (SSAServerList) serverPanel.getServerList();
            serverList.addMetadata((MetadataInputParameter) pvt.getNewValue()); 
            serverPanel.setServerListValue(serverList);
            updateQueryText(); 
        }
        // update if the server list has been modified at ssaservertable (for example, new registry query)
        else if (pvt.getPropertyName().equals("changeServerlist")) {
            serverList = (SSAServerList) serverPanel.getServerList();
            queryCustomParameters();
            updateParameters();
            metaPanel.updateUI();
            //serverList.saveServers();
            serverPanel.saveAll();
 
        }
        else if (pvt.getPropertyName().equals("selectionChanged")) {
            updateQueryText();
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
        //serverList = serverPanel.getServerList();
       // ArrayList<String> parameters = new ArrayList();
        Iterator srv=serverList.getIterator();
        while ( srv.hasNext() ) {    
            SSAPRegResource server = (SSAPRegResource) srv.next();
            if (serverPanel.isServerSelected(server.getShortName())) {
                    metaPanel.addParams((ArrayList<MetadataInputParameter>) server.getMetadata());
            }
        }            
        metaPanel.setVisible(true);
        metaPanel.repaint();
          
    }
    
    /**
     * updateParameters
     * updates the list of optional metadata parameters that can be added to the query.
     * 
     */
    private void updateParameters() {
        if (serverPanel.getSelectionCount() == 1)
            metaPanel.removeAll();
        showParameters();
    }
    /**
     * Customize metadata parameters
     * Read metadata from pre-stored file
     */
    private void restoreCustomParameters() 
    {
        HashMap<String, MetadataInputParameter> params = null;
        try {
           metaPanel.restoreParams();
           params = metaPanel.getParams();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     //   if (params != null) 
     //       createRelation();//params);
        
    } // readcustomParameters
    


    /**
     * Customize metadata parameters
     * Query metadata from known servers to retrieve all possible parameters. Every
     * query is performed in a different thread, which will perform the query and
     * update the parameter table
     */
    private void queryCustomParameters() 
    {
        int nrServers = serverList.getSize();
        final WorkQueue workQueue = new WorkQueue(nrServers);
    
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
            if (metadata!=null) {
               workQueue.setServer(server);
                workQueue.addWork(metadata);
            }
            return null;
        } //doinbackground

        public void finished()
        {
            //  display the final status of the query
            if ( metadata != null ) {
              //  server.setMetadata(metadata);
                // adds parameter information into the metaParam hash
                logger.info("RESPONSE "+queryURL+"---"+server.getShortName()+"returned "+ metadata.length +"parameters ");
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
                e.printStackTrace();
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
        SSAPRegResource server=null;

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
            //  logger.info( "GETWORK " + workedItems + " " + maxItems );
            if (workedItems >= maxItems) return null;
            while (queue.isEmpty()) {
                this.wait();
            }
            ParamElement [] data = (ParamElement[]) queue.removeFirst();     
           // logger.info( "GETWORK " + workedItems + " " + maxItems + " server "+ this.getServer().getShortName()+" nrparams "+ data.length);
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
        public SSAPRegResource getServer() {
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
    private synchronized static void processMetadata( ParamElement[] metadata, SSAPRegResource server) {

        int i=0;
       
        ArrayList<MetadataInputParameter> serverparams = new ArrayList<MetadataInputParameter>();
               
        while ( i < metadata.length ) {
            String paramName = metadata[i].getName();
            if (    ! paramName.equalsIgnoreCase("INPUT:REQUEST") &&  // these parameters should be ignored
                    ! paramName.equalsIgnoreCase("INPUT:COLLECTION") && 
                    ! paramName.equalsIgnoreCase("INPUT:COMPRESS") && 
                    ! paramName.equalsIgnoreCase("INPUT:OUTPUTFORMAT") &&
                    ! paramName.equalsIgnoreCase("INPUT:COLLECTION") && 
                    ! paramName.equalsIgnoreCase("INPUT:POS") &&        // these parameters should be entered in the main browser
                    ! paramName.equalsIgnoreCase("INPUT:SIZE") &&
                    ! paramName.equalsIgnoreCase("INPUT:MAXREC") &&
                    ! paramName.equalsIgnoreCase("INPUT:BAND") &&
                    ! paramName.equalsIgnoreCase("INPUT:TIME") && 
                    ! paramName.equalsIgnoreCase("INPUT:FORMAT") && 
                    ! paramName.equalsIgnoreCase("INPUT:WAVECALIB") && 
                    ! paramName.equalsIgnoreCase("INPUT:FLUXCALIB") && 
                    ! paramName.equalsIgnoreCase("INPUT:TARGETNAME")  ) 
            {
              
                MetadataInputParameter mip = new MetadataInputParameter(metadata[i], server.getShortName());
                serverparams.add(mip);                
            } // if
            i++;
        } // while
        server.setMetadata(serverparams);
        metaPanel.refreshParams();

    }//processMetadata
    

    //
    // MouseListener interface. Double clicks display the clicked spectrum.
    //
    /*
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
    }*/

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
                    resultsPanel.saveQueryToFile();
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
            if (owner == maxRecField ) {
                String maxRecText = maxRecField.getText();
                int maxRec = 0;
                if ( maxRecText != null && maxRecText.length() > 0 ) {
                    try {
                        maxRec = Integer.parseInt( maxRecText );
                    }
                    catch (NumberFormatException e1) {
                        maxRecField.setForeground(Color.red);
                        //ErrorDialog.showError( this, "Cannot understand maxRec value", e1);                         
                        return;
                    }
                    maxRecField.setForeground(Color.black);
                }
                if (maxRec > 0)
                    queryLine.setMaxrec(maxRec);
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
            }
            
            if (owner == upperTimeField || owner == lowerTimeField) {
                queryLine.setTime(lowerTimeField.getText(), upperTimeField.getText());
            }
            
        }
        updateQueryText();
        
    }
    
}
