package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;

import jsky.catalog.skycat.SkycatCatalog;
import jsky.util.SwingWorker;
import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.HelpFrame;
import uk.ac.starlink.splat.iface.ProgressPanel;
import uk.ac.starlink.splat.iface.SpectrumIO.Props;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.TableLoadPanel;
import uk.ac.starlink.util.ContentCoding;
import uk.ac.starlink.util.gui.ErrorDialog;
import uk.ac.starlink.vo.TapQuery;
import uk.ac.starlink.votable.VOStarTable;


public class ObsCorePanel extends JFrame implements  PropertyChangeListener
{
    
    //  Logger.
    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.splat.vo.ObsCorePanel" );
    
    /** UI preferences. */
    protected static Preferences prefs = 
            Preferences.userNodeForPackage( ObsCorePanel.class );
    
    
    /** The instance of SPLAT we're associated with. */
    private SplatBrowser browser = null;
    
    private final int width=900;
    private final int height=800;
    private final int divLoc=340; // divider location
    
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
     *  Results area
     */
    /** Tabbed pane showing the query results tables */
    protected ResultsPanel resultsPanel = null;
    protected JScrollPane resultScroller = null;
    protected JPanel buttonsPanel = new JPanel();

    protected TitledBorder resultsTitle =
        BorderFactory.createTitledBorder( "Query results" );
 
    private ObsCoreQueryServerPanel queryPanel;
   
    /** The SpecDataFactory.*/
    private SpecDataFactory specDataFactory = SpecDataFactory.getInstance();
    
    /** the authenticator for access control **/
    private static SSAPAuthenticator authenticator;
    
    /**
     * Frame for adding a new server.
     */
    
    public ObsCorePanel(SplatBrowser browser) {
        
        this.browser = browser;
        
        authenticator = new SSAPAuthenticator();
        Authenticator.setDefault(authenticator);
        specDataFactory.setAuthenticator(authenticator);

        setTitle("OBSCORE SPECTRAL SERVICES");
        setSize(new Dimension(width, height) );

        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.LINE_AXIS) );
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
      
        splitPane.setDividerLocation( divLoc );

        // initialize the right and left panels
      
        try {
            queryPanel = new ObsCoreQueryServerPanel(new ObsCoreServerList(), new Dimension(divLoc,height));
            queryPanel.addPropertyChangeListener(this);
            splitPane.setLeftComponent( queryPanel);
        } catch (SplatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       
        splitPane.setRightComponent( initializeResultsComponents() );
      
        contentPane.add( splitPane, BorderLayout.CENTER );
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
    private void makeQuery(ServerPopupTable serverTable , String querystr) {
        final String query = querystr;
        resultsPanel.removeAllResults();
        
        if (progressFrame != null) {
            progressFrame.closeWindowEvent();
            progressFrame=null;
        }
       // final ProgressPanelFrame 
        progressFrame = new ProgressPanelFrame( "Querying ObsCore servers" );
        int[] services = serverTable.getSelectedRows();
        for (int i=0; i<services.length; i++) {
       
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

            StarPopupTable jtable = new StarPopupTable(table, true);
            jtable.rearrangeObsCore();
            jtable.configureColumnWidths(200, jtable.getRowCount());

            jtable.addMouseListener( resultsPanel );
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
     * Event listener to trigger 
     */
    public void propertyChange(PropertyChangeEvent pvt)
    {        
        if (pvt.getPropertyName().equals("NewQuery")) {
            makeQuery(queryPanel.getServerTable(), queryPanel.getQueryParams());
        }
    }
    
 
    /**
     * Get the main SPLAT browser to download and display spectra.
     * <p>
     * @param propList  the list of spectra to be loaded, their data formats and metadata
     * @param display   boolean: if true display the spectra
     */
    protected void displaySpectra( Props[] propList, boolean display)
    {
        browser.threadLoadSpectra( propList, display );
        browser.toFront();
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
                jtable.addMouseListener( resultsPanel );
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
