package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
//import java.util.Properties;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
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
//import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.coords.Coordinates;
import jsky.coords.DMS;
import jsky.coords.HMS;
import jsky.coords.WorldCoords;
import jsky.util.SwingWorker;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.HelpFrame;
import uk.ac.starlink.splat.iface.ProgressFrame;
import uk.ac.starlink.splat.iface.ProgressPanel;
import uk.ac.starlink.splat.iface.SpectrumIO;
import uk.ac.starlink.splat.iface.SpectrumIO.Props;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.vo.SSAQueryBrowser.LocalAction;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.table.gui.TableLoadPanel;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.util.gui.GridBagLayouter;
import uk.ac.starlink.vo.ResolverInfo;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.votable.VOStarTable;


public class ObsCorePanel extends JFrame implements ActionListener, MouseListener,  DocumentListener, PropertyChangeListener
{
    
    //  Logger.
    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.splat.vo.ObsCorePanel" );
    
    /** UI preferences. */
    protected static Preferences prefs = 
            Preferences.userNodeForPackage( ObsCorePanel.class );
    
    
    /** The instance of SPLAT we're associated with. */
    private SplatBrowser browser = null;
    
    /**
     *  Content pane of JFrame.
     */
    protected JPanel contentPane;
    
    
    /**
     * The ProgressFrame. This appears to denote that something is happening.
     */
    static ProgressPanelFrame progressFrame = null;
    
    /** NED name resolver catalogue */
    protected SkycatCatalog nedCatalogue = null;

    /** SIMBAD name resolver catalogue */
    protected SkycatCatalog simbadCatalogue = null;

    /** The current name resolver, if using Skycat method */
    protected SkycatCatalog resolverCatalogue = null;

   /**
     *  Simple query window like SSAP
     */
    /** Central RA */
    protected JTextField raField = null;

    /** Central Dec */
    protected JTextField decField = null;

    /** Region radius */
    protected JTextField radiusField = null;
    
    /** Maxrec */
    protected JTextField maxrecField = null;

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
    
    /** the  value to the adql search parameter */
    protected JTextField textValue = null;

    
    /**
     *  Results area
     */
    /** Tabbed pane showing the query results tables */
    protected ResultsPanel resultsPanel = null;
    protected JScrollPane resultScroller = null;
    protected JTabbedPane queryTabPanel = null;
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
    private JPanel servPanel;
   
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

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.LINE_AXIS) );
       
        queryParams = queryPrefix;
        
        serverTable=new ServerPopupTable();
        getServers();
        
        JPopupMenu serverPopup = makeServerPopup();
        serverTable.setComponentPopupMenu(serverPopup);
        
        specPopup = makeSpecPopup();
        initMenubar();
        initComponents();
      
        setVisible(true);

    }
    
    /**
     *  Initialise the Menubar .
     */
    private void initMenubar() {
        
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar( menuBar );
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
                "Save results of query "+"to disk file" );

        fileMenu.add( saveAction );
        
      
        LocalAction readAction = new LocalAction( LocalAction.READ,
                "Restore query results",
                readImage,
                "Read results of a " +
                        "previous query back " +
                "from disk file" );
        fileMenu.add( readAction );
        
        //  Add an action to close the window.
        LocalAction closeAction = new LocalAction( LocalAction.CLOSE,
                "Close", closeImage,
                "Close window",
                "control W" );
        fileMenu.add( closeAction ).setMnemonic( KeyEvent.VK_C );

        //  Create the Help menu.
        HelpFrame.createButtonHelpMenu( "obscore-window", "Help on window", menuBar, null /*toolBar*/ );
 
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
        
        JSplitPane splitPane = new JSplitPane();  
        splitPane.setOneTouchExpandable( true );     
        splitPane.setOrientation( JSplitPane.HORIZONTAL_SPLIT );
      
        splitPane.setDividerLocation( 320 );

        // initialize the right and left panels
     
        splitPane.setLeftComponent( initializeQueryComponents() );
        splitPane.setRightComponent( initializeResultsComponents() );

      
        contentPane.add( splitPane, BorderLayout.CENTER );
    }

    private JPanel initializeQueryComponents() {
       JPanel leftPanel = new JPanel();
       servPanel = new JPanel();
     
       queryTabPanel = new JTabbedPane();
       
       leftPanel.setBorder(BorderFactory.createLineBorder(Color.black));
       leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.PAGE_AXIS));    
       leftPanel.setPreferredSize(new Dimension(320,600));
       
       servPanel.setBorder(BorderFactory.createTitledBorder("Obscore Services"));
       servPanel.setLayout( new BoxLayout(servPanel, BoxLayout.PAGE_AXIS ));
    
       //  Add the list of servers to its scroller.
     
       JScrollPane serverScroller = new JScrollPane(serverTable); 
       serverScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
       servPanel.add(serverScroller);
       
       
       JPanel buttonsPanel = new JPanel();
       buttonsPanel.setBorder(BorderFactory.createEmptyBorder());
       buttonsPanel.setLayout( new BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS ));
       
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

       queryTabPanel.add("Simple search", initSimpleQueryPanel());
       
       queryTabPanel.add("ADQL search", initQueryADQLPanel());
       
       JButton getButton = new JButton("Send Query");
       getButton.addActionListener( this );
       getButton.setActionCommand( "QUERY" );    
       
       JButton clearButton = new JButton("Clear");
       clearButton.addActionListener( this );
       clearButton.setActionCommand( "CLEAR" );    
       
       JPanel queryButtonsPanel = new JPanel();
       queryButtonsPanel.add(clearButton);
       queryButtonsPanel.add(getButton);
     
       JPanel queryPanel = new JPanel();
       queryPanel.setBorder(BorderFactory.createEtchedBorder() );
       queryPanel.setLayout(new BoxLayout(queryPanel, BoxLayout.PAGE_AXIS));   
       queryPanel.add(queryTabPanel);
       queryPanel.add(queryButtonsPanel);
       leftPanel.add(queryPanel);
       leftPanel.add(servPanel);
       
       return leftPanel;
      
    }
    
    /**
     * Make the results component. This is mainly JTabbedPane containing a
     * JTable for each set of results (the tables are realized later) and
     * a button to display the selected spectra.
     */
    private  JPanel initializeResultsComponents()
    {
        
      resultsPanel = new ResultsPanel(this);

      resultsPanel.setBorder(BorderFactory.createTitledBorder("Query results"));
      resultsPanel.setToolTipText( "Results of query to the current list "+
              "of Obscore servers. One table per server" );
   
      return resultsPanel;
    
    }
    
    /**
     * Query the selected services.
     */
    private void makeQuery(int[] services, String querystr) {
        final String query = querystr;
        final TapQuery tq=null;
        boolean ok=true;
        resultsPanel.removeAllResults();
        //resultsTabPane.removeAll();
       // resultsPanel.updateUI();
        if (progressFrame != null) {
            progressFrame.closeWindowEvent();
            progressFrame=null;
        }
       // final ProgressPanelFrame 
        progressFrame = new ProgressPanelFrame( "Querying ObsCore servers" );
       
        for (int i=0; i<services.length; i++) {
            ok=true;
            
            int row = serverTable.convertRowIndexToModel(services[i]);
            final String serverUrl = serverTable.getAccessURL(row);
            final String shortname;
            String sname = serverTable.getShortName(row);
            if (sname==null)
                shortname=serverUrl;
            else shortname = sname;

            final ProgressPanel progressPanel = new ProgressPanel( "Querying: " + shortname );
            progressFrame.addProgressPanel( progressPanel );

            final SwingWorker worker = new SwingWorker()
            {
                boolean interrupted = false;
                public Object construct() 
                {
                    progressPanel.start();
                    try {
                        startQuery( serverUrl, shortname, query, progressPanel );
                    }
                    catch (Exception e) {
                        interrupted = true;
                    }
                    return null;
                }

                public void finished()
                {
                    progressPanel.stop();
                    //  Display the results.
                    if ( ! interrupted ) {
                //        addResultsDisplay( ssaQuery );
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
           
         resultsPanel.updateUI();        
    }
    
    /**
     * Send a tap query to a service
     */   
    void startQuery(String queryUrl, String shortname, String query, ProgressPanel progressPanel ) throws InterruptedException {
        logger.info( "Querying: " + queryUrl );
        progressPanel.logMessage( query );
        StarTable table = null;
        
        // Execute query
         
        try {
             table = doQuery( queryUrl, query);
        } catch (MalformedURLException e) {
            progressPanel.logMessage( e.getMessage() );
            logger.info( "Malformed URL "+queryUrl );
            return;
        }
        catch (IOException e) {
            progressPanel.logMessage( e.getMessage() );
            logger.info( "Exception "+queryUrl );
            return;
        } 

        // add table
        if ( table != null &&  table.getRowCount() > 0 ) {

            StarPopupTable jtable = new StarPopupTable(table, false);
            jtable.setComponentPopupMenu(specPopup);
            jtable.configureColumnWidths(200, jtable.getRowCount());

            //resultScroller=new JScrollPane(jtable);
            jtable.addMouseListener( this );
            resultsPanel.addTab(shortname, jtable );
            progressPanel.logMessage( "Completed download" );
            
        } else {
            progressPanel.logMessage( "No results returned" );
        }
        
        if ( Thread.interrupted() ) {
            progressPanel.logMessage( "Interrupted" );
            throw new InterruptedException();
        }
        
    }    
 
    StarTable doQuery(String queryUrl, String query)  throws IOException {

        TapQuery tq;
        StarTableFactory tfact = new StarTableFactory();
        
        // Initializes TapQuery       
        tq =  new TapQuery( new URL(queryUrl), query,  null );
        // Executes query
        return tq.executeSync( tfact.getStoragePolicy(), ContentCoding.NONE ); // to do check storagepolicy
    }

    private void getServers() {

        StarTable st = queryRegistry();
        serverTable.updateServers(st);
     
    }
    
    StarTable queryRegistry() {
        
        StarTable table;
        try {
            
            table = TableLoadPanel.loadTable( this, new SSARegistryQueryDialog(SplatRegistryQuery.OBSCORE), new StarTableFactory() );
         }
        catch ( IOException e ) {
            ErrorDialog.showError( this, "Registry query failed", e );
            return null;
        }
        
        return table;
 
    }
 
    /** 
     * initialize the adql input interface  
     */
   private JPanel initQueryADQLPanel() {
        
    //    JPanel      queryInputPanel = new JPanel();
        
        JPanel queryADQLPanel=new JPanel();
        queryADQLPanel.setLayout(new BorderLayout());
        queryADQLPanel.add(initADQLQueryParams(), BorderLayout.PAGE_START);
        querytext = new JTextArea(10, 8);
        querytext.setLineWrap(true);
        querytext.setEditable(true);
        querytext.setWrapStyleWord(true);
        querytext.setText(queryPrefix);
        querytext.getDocument().putProperty("owner", querytext); //set the owner
        querytext.getDocument().addDocumentListener( this );
        queryADQLPanel.add(querytext,BorderLayout.CENTER);
     
       return queryADQLPanel;
        
    }
  
    
    private JPanel initADQLQueryParams()  {
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
        
        JPanel okpanel = new JPanel();
        JButton backButton = new JButton("Back"); 
        backButton.addActionListener(this);
        backButton.setActionCommand( "Back" );
        okpanel.add(backButton);
        JButton okButton = new JButton("OK"); 
        okButton.addActionListener(this);
        okButton.setActionCommand( "OK" );
        okpanel.add(okButton);
        
        inputPanel.add(panel,BorderLayout.PAGE_START);
        inputPanel.add(okpanel, BorderLayout.PAGE_END);
        return inputPanel;
        
       
    }
    
    /** 
     * initialize the simple query input interface  
     */
    
    private JPanel initSimpleQueryPanel()
    {
      
      
        JPanel queryParamPanel = new JPanel();
        JPanel querySearchPanel = new JPanel();
        querySearchPanel.setBorder(BorderFactory.createTitledBorder("Query"));
        querySearchPanel.setLayout( new BoxLayout(querySearchPanel, BoxLayout.PAGE_AXIS ));
        querySearchPanel.setPreferredSize(new Dimension(310,300));

        querySearchPanel.setLayout( new GridBagLayout());
      
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
        GridBagLayouter layouter =  new GridBagLayouter( simpleQueryPanel, GridBagLayouter.SCHEME4 /*SCHEME4*/ );

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
        radiusField.setToolTipText( "Enter radius of field to search" +
                " from given centre, arcminutes" );
        radiusField.addActionListener( this );
        radiusField.getDocument().putProperty("owner", radiusField); //set the owner
        radiusField.getDocument().addDocumentListener( this );
        
        JLabel maxrecLabel = new JLabel( "Maxrec:" );
        maxrecField = new JTextField( "10000", 10 );      //  queryLine.setmaxrec(10.0);
        maxrecField.addActionListener( this );
        maxrecField.setToolTipText( "Enter maxrec of field to search" +
                " from given centre, arcminutes" );
        maxrecField.addActionListener( this );
        maxrecField.getDocument().putProperty("owner", maxrecField); //set the owner
        maxrecField.getDocument().addDocumentListener( this );
        
        JPanel radMaxrecPanel = new JPanel( new GridBagLayout() );
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.weightx = 1.0;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        radMaxrecPanel.add( radiusField, gbc2 );
        gbc2.weightx=0.0;
        gbc2.fill = GridBagConstraints.NONE;
        radMaxrecPanel.add(maxrecLabel, gbc2);
        gbc2.weightx = 1.0;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        radMaxrecPanel.add(maxrecField, gbc2);
        
        layouter.add( radiusLabel, false );
        layouter.add(radMaxrecPanel,true);

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
        GridBagConstraints gbc3 = new GridBagConstraints();

        gbc3.weightx = 1.0;
        gbc3.fill = GridBagConstraints.HORIZONTAL;
        bandPanel.add( lowerBandField, gbc3 );

     
        gbc3.weightx = 0.0;
        gbc3.fill = GridBagConstraints.NONE;
        bandPanel.add( new JLabel( "/" ), gbc3 );

 
        gbc3.weightx = 1.0;
        gbc3.fill = GridBagConstraints.HORIZONTAL;
        bandPanel.add( upperBandField, gbc3 );

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
    //    JPanel calibOptions = new JPanel(new GridLayout(3,2));        
    //    layouter.eatSpare();
        
     //   simpleQueryPanel.add(simpleQuery1, BorderLayout.LINE_START);
     //   simpleQueryPanel.add(calibOptions, BorderLayout.LINE_END);
   
  
         
        // the query string display Panel
/*      
        goButton = new JButton( "    SEND QUERY    " );
      //  goButton.setBackground(Color.green);
        goButton.addActionListener( this );*/
      
  //      showQueryButton = new JButton( "Query: ");
  //      showQueryButton.setToolTipText("show/update query string");
  //      showQueryButton.addActionListener( this );
   //     JPanel sendQueryPanel = new JPanel(new BorderLayout());
     //   sendQueryPanel.add(new JLabel("Query:"), BorderLayout.LINE_START);
  //      sendQueryPanel.add(showQueryButton, BorderLayout.LINE_START);
    //    queryText = new JTextArea(2,25);
   //     JScrollPane queryScroller = new JScrollPane();
  //      queryScroller.add(queryText);
     //   queryScroller.setV
   //     queryText.setEditable(false);
   //     sendQueryPanel.add(queryText);
   //     queryText.setLineWrap(true);     
  //      sendQueryPanel.add(goButton, BorderLayout.PAGE_END); // LINE_END
       
 //       c.fill=GridBagConstraints.BOTH;
  //      c.anchor=GridBagConstraints.NORTHWEST;
  //      c.weighty=.5;
  //      c.gridx = 0;
  //      c.gridy = 0;
        
  //      querySearchPanel.add(queryParamPanel, c);
    //    c.gridy=1;
      //  querySearchPanel.add( sendQueryPanel, c);
       
     //   centrePanel.add( queryPanel, gbcentre );
       
        
        // add query text to query text area
     //   updateQueryText();
        return queryParamPanel;
        
    }
    /**
     *  action performed
     *  process the actions
     */
    public void actionPerformed(ActionEvent e) {

        Object command = e.getActionCommand();
        Object obj = e.getSource();
     
       
        
        
     
        if ( obj.equals( nameLookup ) /*|| obj.equals( nameField ) */) {
                     
            resolveName();

            return;
        } 

        if ( command.equals( "CLEAR" ) ) {
            {             
                int index=queryTabPanel.getSelectedIndex();
                if (queryTabPanel.getTitleAt(index).equals("Simple search")) {
                    queryTabPanel.remove(index);
                    queryTabPanel.addTab("Simple search", initSimpleQueryPanel());                   
                } 
                else {
                    queryTabPanel.remove(index);
                    queryTabPanel.addTab("ADQL search", initQueryADQLPanel());
                } 
                queryTabPanel.setSelectedIndex(index==0?1:0); // back to the right tab
            }        
            return;
        } 
        if ( command.equals( "QUERY" ) ) // query
        {
             if (queryTabPanel.getTitleAt(queryTabPanel.getSelectedIndex()).equals("Simple search")) {
                 querySelectedServices( true );
             }
             else { // ADQL Search
                 querySelectedServices( false );       
             }
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
        //querySearchPanel.updateUI();
      //  queryADQLPanel.updateUI();
    }



    /**
     * Returns the adql query for a query from the simple query interface      
     */

    private String getQueryText() throws NumberFormatException
    {
        String queryString = "";
        
//      MAXREC/TOP.
        int maxrec=0;
        String maxrecText = maxrecField.getText();
      
        if ( maxrecText != null && maxrecText.length() > 0 ) {
            try {
                maxrec = Integer.parseInt( maxrecText );
            }
            catch (NumberFormatException e) {
        //        ErrorDialog.showError( this, "Cannot understand maxrec value", e );
                maxrec=0;
                throw e;
            }
        }
       
        
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
          //  try {
                radius = Double.parseDouble( radiusText );
         //   }
         //   catch (NumberFormatException e) {
                //ErrorDialog.showError( this, "Cannot understand radius value", e );
                
        //    }
            queryString += " AND CONTAINS( POINT('ICRS', s_ra, s_dec), CIRCLE('ICRS', "+raVal+", "+decVal+","+radius/60.+")) =1"; // convert to degrees

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
        
        if (maxrec == 0) {
            return queryPrefix+queryString;
        } else {
            return  "SELECT TOP "+maxrec+ " * from ivoa.Obscore WHERE dataproduct_type=\'spectrum\' "+queryString;
        }
       
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
            String queryText=null;
            try {
                 queryText=getQueryText();
            }
            catch (NumberFormatException e ) {
                ErrorDialog.showError( this, "Please check given parameters", e );  
            }
            if (queryText == null)
                return;
            queryParams=queryText; 
        }
        else {   // adql expression search
            String str=querytext.getText();
            if (str.endsWith(" AND")) {
                str = str.substring(0, str.lastIndexOf("AND"));
            }
            queryParams=str;
        }
        
        logger.info( "QUERY= "+queryParams);
        
        makeQuery(serverTable.getSelectedRows(), queryParams);
       
        
    }


 
    
    /**
     * Event listener to trigger a list update when a new server is
     * added to addServerWIndow
     */
    public void propertyChange(PropertyChangeEvent pvt)
    {
       
        DefaultTableModel model = (DefaultTableModel) serverTable.getModel();
        
        model.addRow(new Object[]{ addServerWindow.getShortName(), addServerWindow.getServerTitle(),addServerWindow.getDescription(),"","","", addServerWindow.getAccessURL()});
        serverTable.setModel( model );
      
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
         
        if ( table != null ) { 
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
                                    ( SpecDataFactory.mimeToSPLATType( value ) );
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
                                        ( SpecDataFactory.mimeToSPLATType( value ) );
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
     *
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
    }*/

    public void changedUpdate(DocumentEvent arg0) {
        // TODO Auto-generated method stub
        
    }

    public void removeUpdate(DocumentEvent de) {
        insertUpdate(de);
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
            if ( radiusText != null && radiusText.length() > 0 ) {
                try {
                    double radius = Double.parseDouble( radiusText );
                }
                catch (NumberFormatException e1) {
                    ErrorDialog.showError( this, "Cannot understand radius value", e1);  
                    radiusField.setForeground(Color.RED);
                    return;
                }
            }
            radiusField.setForeground(Color.BLACK);
        }
        if (owner == maxrecField ) {      
            String maxrecText = maxrecField.getText(); 
            if ( maxrecText != null && maxrecText.length() > 0 ) {
                try {
                    int maxrec = Integer.parseInt( maxrecText );
                }
                catch (NumberFormatException e2) {
                   ErrorDialog.showError( this, "Cannot understand maxrec value", e2 );
                   maxrecField.setForeground(Color.RED);
                   return;
                }                            
            }
            maxrecField.setForeground(Color.BLACK);               
        }
        
    }

    /**
     *  Restore a set of previous query results that have been written to a
     *  VOTable. The file name is obtained interactively.
     */
    public void readQueryFromFile()
    {

        //  Remove existing tables.
        resultsPanel.removeAllResults();
        
        ArrayList<VOStarTable> tableList = resultsPanel.readQueryFromFile();
        if ( tableList != null && ! tableList.isEmpty() ) {
           
            Iterator<VOStarTable> i = tableList.iterator();
            while ( i.hasNext() ) {
                StarTable table = (StarTable) i.next();
                // get shortname
                String shortName = "";
                DescribedValue  dValue = table.getParameterByName( "ShortName" );
                if ( dValue == null ) {
                    shortName = table.getName();
                }
                else {
                    shortName = (String)dValue.getValue();
                }
                StarPopupTable jtable = new StarPopupTable(table, false);
                //jtable.setComponentPopupMenu(specPopup);
                //jtable.configureColumnWidths(200, jtable.getRowCount());
                jtable.addMouseListener( this );
                resultsPanel.addTab(shortName, jtable );
            }
        }        
    }
    
    /**
     *  Close the window.
     */
    protected void closeWindowEvent()
    {
       
        Utilities.saveFrameLocation( this, prefs, "ObsCorePanel" );
        this.dispose();
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
    
    
    //
    // LocalAction to encapsulate all trivial local Actions into one class.
    //
    class LocalAction
    extends AbstractAction
    {
        //  Types of action.
        //public static final int PROXY = 0;
        //public static final int SERVER = 1;
        public static final int SAVE = 2;
        public static final int READ = 3;
        public static final int CLOSE = 4;

        //  The type of this instance.
        private int actionType = 0;

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

}
