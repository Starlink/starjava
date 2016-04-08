package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
//import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.coords.Coordinates;
import jsky.coords.DMS;
import jsky.coords.HMS;
import jsky.coords.WorldCoords;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.ProgressFrame;
import uk.ac.starlink.splat.iface.SpectrumIO;
import uk.ac.starlink.splat.iface.SpectrumIO.Props;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.vo.SSAServerTable.PopupMenuAction;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.StarTableModel;
import uk.ac.starlink.table.gui.TableLoadPanel;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.GridBagLayouter;
import uk.ac.starlink.vo.ResolverInfo;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.vo.TapTableLoadDialog;


public class ObsCorePanel extends JFrame implements ActionListener, MouseListener,  DocumentListener, PropertyChangeListener
{
    
    //  Logger.
    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.splat.vo.ObsCorePanel" );
    
    
    /** The instance of SPLAT we're associated with. */
    private SplatBrowser browser = null;
    
    /**
     *  Content pane of JFrame.
     */
    protected JPanel contentPane;

    /**
     *  Main menubar and various menus.
     */
    protected JMenuBar menuBar = new JMenuBar();
    
    /**
     * The ProgressFrame. This appears to denote that something is happening.
     */
    private ProgressFrame progressFrame =
        new ProgressFrame( "Loading spectra..." );


    /**
     *  Server selection and query panels.
     */
    protected JSplitPane splitPane = new JSplitPane();  
    protected JPanel serverPanel = new JPanel();
    protected JScrollPane serverScroller = null;
    protected JPanel querySearchPanel = new JPanel();
    protected JPanel queryADQLPanel = new JPanel();
    protected TitledBorder serverTitle =
            BorderFactory.createTitledBorder( "Spectral services" );
    
    
    /** NED name resolver catalogue */
    protected SkycatCatalog nedCatalogue = null;

    /** SIMBAD name resolver catalogue */
    protected SkycatCatalog simbadCatalogue = null;

    /** The current name resolver, if using Skycat method */
    protected SkycatCatalog resolverCatalogue = null;

    /** list of manually added servers **/
 //   StarTableModel addedServers= new StarTableModel(null);

    /**
     *  Simple query window like SSAP
     */
    /** Central RA */
    protected JTextField raField = null;

    /** Central Dec */
    protected JTextField decField = null;

    /** Region radius */
    protected JTextField radiusField = null;

    /** Lower limit for BAND */
    protected JTextField lowerBandField = null;

    /** Upper limits for BAND */
    protected JTextField upperBandField = null;

    /** Lower limit for TIME */
    protected JTextField lowerTimeField = null;

    /** Upper limits for TIME */
    protected JTextField upperTimeField = null;

    /** The query text */
    protected JTextArea queryText = null;
    
    /** The extended query text */
    protected String extendedQueryText = null;
    

    /** Object name */
    protected JTextField nameField = null;

    /** Resolve object name button */
    protected JButton nameLookup = null;

    /** Make the query to all selected servers */
    protected JButton goButton = null;
    
    /** the  value to the adql search parameter */
    protected JTextField textValue = null;

    
    /**
     *  Results area
     */
    /** Tabbed pane showing the query results tables */
    protected JPanel resultsPanel = new JPanel();
    protected JTabbedPane resultsTabPane = null;
    protected JScrollPane resultScroller = null;
    protected JPanel buttonsPanel = new JPanel();

    protected TitledBorder resultsTitle =
        BorderFactory.createTitledBorder( "Query results" );
 
    private String[] parameters = {"", "target_name", "s_ra", "s_dec", "s_fov", "s_region", "s_resolution", 
                                  "t_min", "t_max", "t_exptime", "em_min", "em_max", "em_res_power", "pol_states", "calib_level"};
    private String[] comparisons = {"", "=", "!=", "<>", "<", ">", "<=", ">="};
    private String[] conditions = {"", "AND", "OR"};
    private String queryPrefix = "SELECT TOP 10000 * from ivoa.Obscore WHERE dataproduct_type=\'spectrum\' ";

    private String queryParams = null;

    
    private String[] subquery = null;
 //   private StarTable servers = null;
    private JPanel servPanel;
    private JPanel rightPanel;
    private JPanel leftPanel;
    /** The SpecDataFactory.*/
    private SpecDataFactory specDataFactory = SpecDataFactory.getInstance();

    private JTextArea querytext;

    private ServerPopupTable serverTable;
    
    JPopupMenu specPopup=null;
    
 //   private SSAServerTable serverTable;
    
    /** the authenticator for access control **/
    private static SSAPAuthenticator authenticator;
    
    /**
     * Frame for adding a new server.
     */
    protected AddNewServerFrame addServerWindow = null;

    
    public ObsCorePanel(SplatBrowser browser) {
        
        this.browser = browser;
        
        authenticator = new SSAPAuthenticator();
        Authenticator.setDefault(authenticator);
        specDataFactory.setAuthenticator(authenticator);

        setTitle("OBSCORE SPECTRAL SERVICES");
        subquery = new String[4];
        setSize(new Dimension(800, 600) );
      //  setPreferredSize(new Dimension(800, 600) );

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.LINE_AXIS) );
       
        queryParams = queryPrefix;
        
        serverTable=new ServerPopupTable();
        getServers();
        
        JPopupMenu serverPopup = makeServerPopup();
        serverTable.setComponentPopupMenu(serverPopup);
       // serverTable.addMouseListener(new ServerTableMouseListener());
        
        specPopup = makeSpecPopup();
        
        initComponents();
        initQueryComponents();
       // addQueryPanel();
      //  makeTestQuery();
        setVisible(true);

    }
    
    
    
    private JPopupMenu makeServerPopup() {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem infoMenuItem = new JMenuItem("Info");
        infoMenuItem.addActionListener(new ServerPopupMenuAction());
        popup.add(infoMenuItem);
        return popup;
    }
    
    private JPopupMenu makeSpecPopup() {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem dlMenuItem = new JMenuItem("Download");
        dlMenuItem.addActionListener(new SpecPopupMenuAction());
        popup.add(dlMenuItem);
        JMenuItem infoMenuItem = new JMenuItem("Info");
        infoMenuItem.addActionListener(new SpecPopupMenuAction());
        popup.add(infoMenuItem);
        JMenuItem dispMenuItem = new JMenuItem("Display");
        dispMenuItem.addActionListener(new SpecPopupMenuAction());
        popup.add(dispMenuItem);
        return popup;
    }



    /**
     *  Initialise all visual components.
     */
    private void initComponents()
    {
        //  Set up the content pane and window size.
        contentPane = (JPanel) getContentPane();

        contentPane.setLayout( new BorderLayout() );

        setTitle( "SPLAT ObsCore Browser" );

  
        //  Set up split pane.
        splitPane.setOneTouchExpandable( true );     
        splitPane.setOrientation( JSplitPane.HORIZONTAL_SPLIT );
        
        //specList.setSize( new Dimension( 190, 0 ) );
      
        splitPane.setDividerLocation( 320 );

        // initialize the right and left panels
        initialiseUI();
        //  Finally add the main components to the content and split
        //  panes. Also positions the toolbar.
      //  initResultsComponent();
        splitPane.setLeftComponent( leftPanel );
        splitPane.setRightComponent( rightPanel );

      
        contentPane.add( splitPane, BorderLayout.CENTER );
    }

    private void initialiseUI() {
       leftPanel = new JPanel();
       servPanel = new JPanel();
       rightPanel = new JPanel();
       JTabbedPane queryTabPanel = new JTabbedPane();
       
       leftPanel.setBorder(BorderFactory.createLineBorder(Color.black));
       leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));
       //leftPanel.add(new JLabel("Obscore Spectral Services"));     
       leftPanel.setPreferredSize(new Dimension(320,600));
       
       rightPanel.setLayout( new BoxLayout(rightPanel, BoxLayout.PAGE_AXIS ));
       rightPanel.setBorder(BorderFactory.createLineBorder(Color.black));
       rightPanel.setPreferredSize(new Dimension(580,600));
   //    this.add(leftPanel);
  //     this.add( rightPanel);
       
       servPanel.setBorder(BorderFactory.createTitledBorder("Obscore Spectral Services"));
       servPanel.setLayout( new BoxLayout(servPanel, BoxLayout.PAGE_AXIS ));
     
       //  servPanel.setSize(new Dimension(200,200));
       //  Add the list of servers to its scroller.
     
       serverScroller = new JScrollPane(serverTable); 
       serverScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
       servPanel.add(serverScroller);
       
       //queryPanel = new JPanel();
       querySearchPanel.setBorder(BorderFactory.createTitledBorder("Query"));
       querySearchPanel.setLayout( new BoxLayout(querySearchPanel, BoxLayout.PAGE_AXIS ));
       querySearchPanel.setPreferredSize(new Dimension(310,300));
       //mainPanel.add(queryPanel);
       
       JPanel buttonsPanel = new JPanel();
       buttonsPanel.setBorder(BorderFactory.createEmptyBorder());
       buttonsPanel.setLayout( new BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS ));
    //   buttonsPanel.setPreferredSize(new Dimension(310,100));
       
       JButton newServerButton = new JButton("Add New Service");
       newServerButton.addActionListener( this );
       newServerButton.setActionCommand("NEWSERVICE");
       newServerButton.setToolTipText("Manually add a new service");  
       buttonsPanel.add(newServerButton);
       
       JButton refreshButton = new JButton("Query Registry");
       refreshButton.addActionListener( this );
       refreshButton.setActionCommand("REFRESH");
       refreshButton.setToolTipText("Query registry and refresh services list");  
       buttonsPanel.add(refreshButton);
       
       servPanel.add(buttonsPanel);
       
       queryTabPanel.add("Cone search", querySearchPanel);
       
       addQueryADQLPanel();
       queryTabPanel.add("ADQL search", queryADQLPanel);
       
       leftPanel.add(queryTabPanel);
       leftPanel.add(servPanel);
       
       resultsPanel = new JPanel();
       resultsPanel.setBorder(BorderFactory.createTitledBorder("Query results"));
       resultsPanel.setToolTipText( "Results of query to the current list "+
               "of SSAP servers. One table per server" );
       resultsPanel.setLayout( new BoxLayout(resultsPanel, BoxLayout.PAGE_AXIS ));
       resultsPanel.setSize(new Dimension(495,500));
       resultsTabPane = new JTabbedPane();
     //  resultsTabPane.setPreferredSize(new Dimension(600,310));
       resultsPanel.add( resultsTabPane, BorderLayout.NORTH );
       rightPanel.add(resultsPanel);
      
    }

    private void makeQuery(int[] services, String query) {
        
        
        TapQuery tq=null;
        StarTableFactory tfact = new StarTableFactory();
        boolean ok=true;
        resultsPanel.removeAll();
        resultsTabPane.removeAll();
        resultsPanel.updateUI();
       
        for (int i=0; i<services.length; i++) {
            ok=true;
            String s=null;
            String shortname=null;
            try {
                 s = serverTable.getAccessURL(services[i]);
                 // if shortname is empty, go on using the accessURL
                 shortname = serverTable.getShortName(services[i]);
                 if (shortname == null)
                     shortname = s;
               
                 tq =  new TapQuery( new URL(s), query,  null );
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
               ok=false;
            }
            if (ok) {
                StarTable table = null;
             
                
                try {
                    table = tq.executeSync( tfact.getStoragePolicy(), ContentCoding.NONE ); // to do check storagepolicy
                  
                } catch (IOException e) {
                    
                    ErrorDialog.showError( this, "TAP query error: ", e, e.getMessage());
                     ok = false;
                } 
               
                if (ok ) {
                    logger.info("Status "+table.getParameterByName("QUERY_STATUS"));
                 
                    if ( table != null &&  table.getRowCount() > 0 ) {
                        
                        StarPopupTable jtable = new StarPopupTable(table, false);
                        jtable.setComponentPopupMenu(specPopup);
                        jtable.configureColumnWidths(200, jtable.getRowCount());
                       
                       resultScroller=new JScrollPane(jtable);
                       jtable.addMouseListener( this );
                       resultsTabPane.addTab(shortname, resultScroller );
                    
                    } 
                    
                }
            }
        }
        if (resultsTabPane.getTabCount() > 0)
            resultsPanel.add(resultsTabPane);
        resultsPanel.updateUI();
        
    }

    
 
    @SuppressWarnings("null")
    private void getServers() {

        StarTable st = queryRegistry();
        serverTable.updateServers(st);
        
       // serverTable.updateServers(table);
       //serverScroller = new JScrollPane(serverTable);
       //serverScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
       
    }
    
    private StarTable queryRegistry() {
        
        StarTable table;
        try {
            
            table = TableLoadPanel.loadTable( this, new SSARegistryQueryDialog("ObsCore"), new StarTableFactory() );
         }
        catch ( IOException e ) {
            ErrorDialog.showError( this, "Registry query failed", e );
            return null;
        }
        
        return table;
 
    }
 
   private void addQueryADQLPanel() {
        
    //    JPanel      queryInputPanel = new JPanel();
        
      //  queryADQLPanel=new JPanel();
        queryADQLPanel.setLayout(new BorderLayout());
        addQueryParams();
        querytext = new JTextArea(10, 8);
        querytext.setLineWrap(true);
        querytext.setEditable(true);
        querytext.setWrapStyleWord(true);
        querytext.setText(queryPrefix);
        querytext.getDocument().putProperty("owner", querytext); //set the owner
        querytext.getDocument().addDocumentListener( this );
        queryADQLPanel.add(querytext,BorderLayout.CENTER);
     
//        JLabel targetlabel = new JLabel( "Target Name: ");
//        JTextField targettext = new JTextField(80);
//        targettext.addActionListener( this );
 //           queryInputPanel.add(targetlabel);
//        queryInputPanel.add(targettext);
//        queryInputPanel.add(targetlabel);
      
        JButton getbutton = new JButton("Send Query");
        getbutton.addActionListener( this );
        getbutton.setActionCommand( "QUERY" );    
        queryADQLPanel.add(getbutton,BorderLayout.PAGE_END);
      //  servPanel.add(queryInputPanel);
        
    }
  
    
    private void addQueryParams()  {
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        JComboBox parambox = new JComboBox(parameters) ;
        parambox.addActionListener(this);
        parambox.setActionCommand( "PARAM" );
        panel.add(parambox);
        
        JComboBox compbox = new JComboBox(comparisons);
        compbox.addActionListener(this);
        compbox.setActionCommand( "COMPARE" );
        panel.add(compbox);
        
        textValue = new JTextField(10);
        textValue.addActionListener(this);
        textValue.setActionCommand( "VALUE" );
        textValue.getDocument().putProperty("owner", textValue); //set the owner
        textValue.getDocument().addDocumentListener( this );
        panel.add(textValue);
        
        JButton andButton = new JButton("AND"); 
        andButton.addActionListener(this);
        andButton.setActionCommand( "AND" );
 //       panel.add(andButton);
//        queryPanel.add(panel);
        
        JPanel okpanel = new JPanel();
        JButton backButton = new JButton("Back"); 
        backButton.addActionListener(this);
        backButton.setActionCommand( "Back" );
        okpanel.add(backButton);
        JButton okButton = new JButton("OK"); 
        okButton.addActionListener(this);
        okButton.setActionCommand( "OK" );
        okpanel.add(okButton);
        //panel.add(okButton);
        inputPanel.add(panel,BorderLayout.PAGE_START);
        inputPanel.add(okpanel, BorderLayout.PAGE_END);
        queryADQLPanel.add(inputPanel, BorderLayout.PAGE_START);
        //queryADQLPanel.add(okpanel,BorderLayout.LINE_END);
        
    }
    

    private void initQueryComponents()
    {
      
       // queryPanel = new JPanel();
        JPanel queryParamPanel = new JPanel();
       // queryPanel.setBorder ( BorderFactory.createTitledBorder( "Search parameters:" ) );
        querySearchPanel.setLayout( new GridBagLayout());
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
        simpleQueryPanel.setBorder(BorderFactory.createTitledBorder("Query Parameters"));
      
        queryParamPanel.add(simpleQueryPanel, c);
        
        c.weightx = 0.5;
        c.gridx = 1;
        c.gridy = 0;
       
       
        
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
      //  queryLine.setRadius(10.0);
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
   
  
        
        layouter.eatSpare();
        
     //   simpleQueryPanel.add(simpleQuery1, BorderLayout.LINE_START);
     //   simpleQueryPanel.add(calibOptions, BorderLayout.LINE_END);
   
  
         
        // the query string display Panel
        //-------------------------------
        goButton = new JButton( "    SEND QUERY    " );
      //  goButton.setBackground(Color.green);
        goButton.addActionListener( this );
      
  //      showQueryButton = new JButton( "Query: ");
  //      showQueryButton.setToolTipText("show/update query string");
  //      showQueryButton.addActionListener( this );
        JPanel sendQueryPanel = new JPanel(new BorderLayout());
     //   sendQueryPanel.add(new JLabel("Query:"), BorderLayout.LINE_START);
  //      sendQueryPanel.add(showQueryButton, BorderLayout.LINE_START);
    //    queryText = new JTextArea(2,25);
   //     JScrollPane queryScroller = new JScrollPane();
  //      queryScroller.add(queryText);
     //   queryScroller.setV
   //     queryText.setEditable(false);
   //     sendQueryPanel.add(queryText);
   //     queryText.setLineWrap(true);     
        sendQueryPanel.add(goButton, BorderLayout.PAGE_END); // LINE_END
       
        c.fill=GridBagConstraints.BOTH;
        c.anchor=GridBagConstraints.NORTHWEST;
        c.weighty=.5;
        c.gridx = 0;
        c.gridy = 0;
        
        querySearchPanel.add(queryParamPanel, c);
        c.gridy=1;
        querySearchPanel.add( sendQueryPanel, c);
       
     //   centrePanel.add( queryPanel, gbcentre );
       
        
        // add query text to query text area
     //   updateQueryText();
        
    }
    /**
     *  action performed
     *  process the actions
     */
    public void actionPerformed(ActionEvent e) {

        Object command = e.getActionCommand();
        Object obj = e.getSource();
        JComboBox cb; String str;
       
        if ( obj.equals( goButton ) ) {
            {             
               querySelectedServices( true );
               
            }        
            return;
        } 
     
        if ( obj.equals( nameLookup ) /*|| obj.equals( nameField ) */) {
                     
            resolveName();

            return;
        } 
           
        if (obj.equals(raField) || obj.equals(decField) || obj.equals(nameField)) {

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
         
               return;   
            }
            if ( obj.equals( radiusField )  ) {
            
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
         //       queryLine.setRadius(radius);
            
             
                return;            
            }
            
            //  Spectral bandpass. These should be in meters. XXX allow other
            //  units and do the conversion.
            if (  obj.equals( lowerBandField ) || obj.equals( upperBandField )) {
                 
                String lowerBand = lowerBandField.getText();
                if ( "".equals( lowerBand ) ) {
                    lowerBand = null;
                }
                String upperBand = upperBandField.getText();
                if ( "".equals( upperBand ) ) {
                    upperBand = null;
                }
        //          queryLine.setBand(lowerBand, upperBand);
                 
                  return;            
            }
           
            if (  obj.equals( upperTimeField ) || obj.equals( lowerTimeField ))  {
                
                String lowerTime = lowerTimeField.getText();
                if ( "".equals( lowerTime ) ) {
                    lowerTime = null;
                }
                String upperTime = upperTimeField.getText();
                if ( "".equals( upperTime ) ) {
                    upperTime = null;
                }
   //               queryLine.setTime(lowerTime, upperTime);
               
                  return;            
            }
        
        if ( command.equals( "QUERY" ) ) // query
        {
             querySelectedServices( false );       
        }
        if ( command.equals( "PARAM" ) ) 
        {
            subquery[0] = (String) ((JComboBox) e.getSource()).getSelectedItem();
         //   querytext.append(" AND "+ subquery[0] );
        }
        if ( command.equals( "COMPARE" ) ) 
        {
            subquery[1] = (String) ((JComboBox) e.getSource()).getSelectedItem();
           // querytext.append(" "+ subquery[1] );
        }
        if ( command.equals( "VALUE" ) ) 
       // if (obj.equals(textValue))
        {
            subquery[2] = (String) ((JTextField) e.getSource()).getText();
            // querytext.append(" "+ subquery[2] );
        }
        if ( command.equals( "Back" ) ) 
        {
            String st=querytext.getText();
            if (st.endsWith(" AND")) {
                st = st.substring(0, st.lastIndexOf("AND"));
            }
            st = st.substring(0, st.lastIndexOf("AND"));
            querytext.setText(st);

           
       //    subquery[0]=subquery[1]=subquery[2]="";
        }
        if ( command.equals( "AND" ) || command.equals("OK")) 
        {
           if(command.equals("OK"))
               querytext.append( " AND " + subquery[0]+" "+ subquery[1]+" "+subquery[2]);
           else 
               querytext.append( " AND " + subquery[0]+" "+ subquery[1]+" "+subquery[2]);
           
       //    subquery[0]=subquery[1]=subquery[2]="";
        }
        
    /*    if ( command.equals( "reset" ) ) // reset text fields
        {
            statusLabel.setText(new String(""));
            resetFields();
        }
        if ( command.equals( "close" ) ) // close window
        {
            closeWindowEvent();
        }
        */
        if ( command.equals( "NEWSERVICE" ) ) // add new server
        {
            addNewServer();
        }
        if ( command.equals( "REFRESH" ) ) // query registry - refresh server table
        {         
            getServers();           
        }
        querySearchPanel.updateUI();
        queryADQLPanel.updateUI();
    }

    
   
        
    
    /**
     * Perform the query to all the currently selected servers.
     */

    private String getQueryText() 
    {
        String queryString = "";
        
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
                    return null;
                }

                //  To be clear.
                ra = null;
                dec = null;
                objectName = null;
            }
        } 
 
        double raVal=0, decVal=0;
        if (objectName != null && ra == null && dec == null ) 
            queryString += " AND target_name=\'"+objectName+"\'";
        else {
            if (ra != null ) {
                // Convert coordinates to ICRS
                HMS hms = new HMS( ra );
                raVal=hms.getVal()*15.0;
          //      queryString += " AND s_ra="+raVal;
            }
               
            if (dec != null ) {
                // Convert coordinates to ICRS
               DMS dms = new DMS( dec );
               decVal=dms.getVal();
           //    queryString += " AND s_dec="+decVal;
            }
          
        
         
        //  And the radius.
        String radiusText = radiusField.getText();
        double radius=0;// = 10.0;
        if ( radiusText != null && radiusText.length() > 0 ) {
            try {
                radius = Double.parseDouble( radiusText );
                queryString += " AND CONTAINS( POINT('ICRS', s_ra, s_dec), CIRCLE('ICRS', "+raVal+", "+decVal+","+radius/60.+")) =1"; // convert to degrees
            }
            catch (NumberFormatException e) {
                ErrorDialog.showError( this, "Cannot understand radius value", e );
            }
        }
            String lowerBand = lowerBandField.getText();
            if ( ! lowerBand.isEmpty() ) 
                queryString += " AND em_min<=\'"+ lowerBand+"\'";
            
            String upperBand = upperBandField.getText();
            if ( ! upperBand.isEmpty() ) 
                queryString += " AND em_max>=\'"+ upperBand+"\'";
   
            String lowerTime = lowerTimeField.getText();
            if ( ! lowerTime.isEmpty() ) 
                queryString += " AND t_min<=\'"+ lowerTime+"\'";
            
            String upperTime = upperTimeField.getText();
            if ( ! upperTime.isEmpty() )
                queryString += " AND t_max>=\'"+ upperTime+"\'";
        }    
        
        return queryString;
    }
          
 

    /**
     * Initialise the window to insert a new server to the list.
     */
    protected void initAddServerWindow()
    {
        if ( addServerWindow == null ) {
            addServerWindow = new AddNewServerFrame();
            addServerWindow.addPropertyChangeListener(this);
        }
    }
    
    /**
     *  Add new server to the server list
     */
    protected void addNewServer()
    {
        initAddServerWindow();
        addServerWindow.setVisible( true );
    }
    
    private void querySelectedServices( boolean conesearch ) {
        

        if (conesearch ) {
            String queryText=getQueryText();
            if (queryText == null)
                return;
            queryParams=queryPrefix+queryText;
        }
        else {   // adql expression search
            String str=querytext.getText();
            if (str.endsWith(" AND")) {
                str = str.substring(0, str.lastIndexOf("AND"));
            }
            queryParams=str;
        }
        
        logger.info( "QUERY= "+queryParams);
        int[] services = serverTable.getSelectedRows();
       // for (int i=1; i<services.length; i++) {
            makeQuery(services, queryParams);
      //  }
        
    }


 
    
    /**
     * Event listener to trigger a list update when a new server is
     * added to addServerWIndow
     */
    public void propertyChange(PropertyChangeEvent pvt)
    {
       //StarTable st new VOStarTable(); // serverList.addServer(addServerWindow.getResource());
      //  serverList.unselectServer(addServerWindow.getResource().getShortName());
        DefaultTableModel model = (DefaultTableModel) serverTable.getModel();
        
        model.addRow(new Object[]{ addServerWindow.getShortName(), addServerWindow.getServerTitle(),addServerWindow.getDescription(),"","","", addServerWindow.getAccessURL()});
        serverTable.setModel( model );
      //  updateTree();
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
            
      //      if (starJTables == null)  // avoids NPE if no results are present
      //          return;
            //  Visit all the tabbed StarJTables.
     //       Iterator<StarJTable> i = starJTables.iterator();
   //         while ( i.hasNext() ) {
      //          extractSpectraFromTable( i.next(), specList,
      //                  selected, -1 );
    //        }
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
            int specstartcol=-1;
            int specstopcol=-1;
            int ucdcol=-1;
            ColumnInfo colInfo;
            String ucd;
            String utype;
            
            for( int k = 0; k < ncol; k++ ) {
                colInfo = starTable.getColumnInfo( k );
                String colName = colInfo.getName();
                utype = colInfo.getUtype();
               

                if (colName != null) { 
                    colName = colName.toLowerCase();
                    if ( colName.endsWith( "access_url" ) ) {
                        linkcol = k;
                    }
                    else if ( colName.endsWith( "access_format" ) ) {
                        typecol = k;
                    }
                    else if ( colName.endsWith( "target_name" ) ) {
                        namecol = k;
                    }
                    else if ( colName.endsWith( "obs_ucd" ) ) {
                        ucdcol = k;
                    }           
                    else if ( colName.endsWith( "obs_publisher_did" ) ) {
                        pubdidcol = k;
                    }
                    else if ( colName.endsWith( "em_min" ) ) {
                        specstartcol = k;
                    }
                    else if ( colName.endsWith( "em_max" ) ) {
                        specstopcol = k;
                    }
                } 
                else if ( utype != null ) {
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
             
         
                
            } // for
            
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
                                    //props.setObjectType(SpecDataFactory.mimeToObjectType(value));
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
                                        //props.setObjectType(SpecDataFactory.mimeToObjectType(value));
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
    public SSAPAuthenticator getAuthenticator() {
        return authenticator;
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
                          
                            raField.setText( radec[0] );
                            decField.setText( radec[1] );
                            nameField.setForeground(Color.black);
                         
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

    public void changedUpdate(DocumentEvent arg0) {
        // TODO Auto-generated method stub
        
    }

    public void insertUpdate(DocumentEvent de) {
        Object owner = de.getDocument().getProperty("owner");
       

        if (owner == textValue ) {
            subquery[2] =  textValue.getText();
        }
        if (owner == querytext ) {
           
        }
        if (owner == radiusField ) {
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
        }
        
    }

    public void removeUpdate(DocumentEvent arg0) {
        // TODO Auto-generated method stub
        
    }
    
    protected class ServerPopupMenuAction extends AbstractAction
    {
        
        public void actionPerformed( ActionEvent e) {
            int r = serverTable.getPopupRow();
                serverTable.showInfo(r, "OBSCore");        
            
        }
    }
    
    
    private class SpecPopupMenuAction extends AbstractAction
    {
        
        public void actionPerformed( ActionEvent e) {
            JMenuItem jmi  = (JMenuItem) e.getSource();
            JPopupMenu jpm = (JPopupMenu) jmi.getParent();
            StarPopupTable table = (StarPopupTable) jpm.getInvoker();
           
           int row = table.getPopupRow();
          
            if (e.getActionCommand().equals("Info")) {
                table.showInfo(row);
            }
            else if (e.getActionCommand().equals("Display")) {
                displaySpectra( false, true, (StarJTable) table, row );
            }   
            else if (e.getActionCommand().equals("Download")) {
                displaySpectra( false, true, (StarJTable) table, row );
            } 
            
        }
    }

    public void mouseClicked(MouseEvent me) {

        if ( me.getClickCount() == 2 ) { // display if StarJTable

            StarPopupTable table = (StarPopupTable) me.getSource();
            Point p = me.getPoint();
            int row = table.rowAtPoint( p );
            displaySpectra( false, true, table, row );
        }
    }

    public void mouseEntered(MouseEvent me) {}
    public void mouseExited(MouseEvent me) {}
    public void mousePressed(MouseEvent me) {}
    public void mouseReleased(MouseEvent me) {}

}
