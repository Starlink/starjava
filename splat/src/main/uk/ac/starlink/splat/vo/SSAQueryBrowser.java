/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     11-NOV-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.vo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Point;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.border.TitledBorder;

import jsky.catalog.BasicQueryArgs;
import jsky.catalog.QueryArgs;
import jsky.catalog.QueryResult;
import jsky.catalog.TableQueryResult;
import jsky.catalog.skycat.SkycatCatalog;
import jsky.catalog.skycat.SkycatConfigEntry;
import jsky.coords.Coordinates;
import jsky.coords.WorldCoords;
import jsky.util.ConnectionUtil;
import jsky.util.SwingWorker;
import jsky.util.gui.ProgressPanel;

import org.us_vo.www.SimpleResource;

import uk.ac.starlink.splat.data.SpecDataFactory;
import uk.ac.starlink.splat.iface.HelpFrame;
import uk.ac.starlink.splat.iface.SpectrumIO.Props;
import uk.ac.starlink.splat.iface.SpectrumIO;
import uk.ac.starlink.splat.iface.SplatBrowser;
import uk.ac.starlink.splat.iface.ToolButtonBar;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.ExceptionDialog;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.util.ProxySetup;
import uk.ac.starlink.util.gui.GridBagLayouter;
import uk.ac.starlink.util.gui.ProxySetupFrame;
import uk.ac.starlink.votable.VOTableBuilder;

/**
 * Display a page of controls for querying a list of  SSA servers and
 * displaying the results of those queries. The spectra returned can then be
 * selected and displayed in the main SPLAT browser.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SSAQueryBrowser
    extends JFrame
    implements ActionListener, MouseListener
{
    /**
     * The object holding the list of servers that we should use for SSA
     * querys.
     */
    private SSAServerList serverList = null;

    /**
     * The instance of SPLAT we're associated with.
     */
    private SplatBrowser browser = null;

    /** ProgressPanel used when downloading query responses */
    private ProgressPanel progressPanel = null;

    /** Worker thread used with ProgressPanel */
    private SwingWorker worker = null;

    /** Content pane of frame */
    protected JPanel contentPane = null;

    /** Centre panel */
    protected JPanel centrePanel = null;

    /** Object name */
    protected JTextField nameField = null;

    /** Resolve object name button */
    protected JButton nameLookup = null;

    /** Download and display selected spectra */
    protected JButton displaySelectedButton = null;

    /** Download and display all spectra */
    protected JButton displayAllButton = null;

    /** Make the query to all known servers */
    protected JButton goButton = null;

    /** Central RA */
    protected JTextField raField = null;

    /** Central Dec */
    protected JTextField decField = null;

    /** Region radius */
    protected JTextField radiusField = null;

    /** Tabbed pane showing the query results tables */
    protected JTabbedPane resultsPane = null;

    /** The list of StarJTables in use */
    protected ArrayList starJTables = null;

    /** NED name resolver catalogue */
    protected SkycatCatalog nedCatalogue = null;

    /** SIMBAD name resolver catalogue */
    protected SkycatCatalog simbadCatalogue = null;

    /** The current name resolver */
    protected SkycatCatalog resolverCatalogue = null;

    /** The proxy server dialog */
    protected ProxySetupFrame proxyWindow = null;

    /** The SSA servers window */
    protected SSAServerFrame serverWindow = null;

    /** Make sure the proxy environment is setup */
    static {
        ProxySetup.getInstance().restore();
    }

    /**
     * Create an instance.
     */
    public SSAQueryBrowser( SSAServerList serverList, SplatBrowser browser )
    {
        this.serverList = serverList;
        this.browser = browser;
        initUI();
        initMenusAndToolbar();
        initFrame();
    }

    /**
     * Create and display the UI components.
     */
    private void initUI()
    {
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout( new BorderLayout() );
        centrePanel = new JPanel( new BorderLayout() );
        contentPane.add( centrePanel, BorderLayout.CENTER );

        initQueryComponents();
        initResultsComponent();
        setDefaultNameServers();
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
        ToolButtonBar toolBar = new ToolButtonBar( contentPane );

        //  Get icons.
        ImageIcon closeImage =
            new ImageIcon( ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon helpImage =
            new ImageIcon( ImageHolder.class.getResource( "help.gif" ) );
        ImageIcon ssaImage =
            new ImageIcon( ImageHolder.class.getResource( "ssapservers.gif" ) );

        //  Create the File menu.
        JMenu fileMenu = new JMenu( "File" );
        menuBar.add( fileMenu );

        //  Create the options menu.
        JMenu optionsMenu = new JMenu( "Options" );
        menuBar.add( optionsMenu );

        ProxyAction proxyAction =
            new ProxyAction( "Configure connection proxy..." );
        optionsMenu.add( proxyAction );

        //  Add item to control the use of SSA servers.
        ServerAction serverAction =
            new ServerAction( "Configure SSAP servers...", ssaImage,
                              "Configure SSAP servers" );
        optionsMenu.add( serverAction );
        toolBar.add( serverAction );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction );
        JButton closeButton = new JButton( closeAction );
        JPanel actionBarContainer = new JPanel();
        actionBarContainer.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        //  Create a menu containing all the name resolvers.
        JMenu resolverMenu = new JMenu( "Resolver" );
        menuBar.add( resolverMenu );

        ButtonGroup bg = new ButtonGroup();

        JRadioButtonMenuItem jrbmi = new JRadioButtonMenuItem();
        resolverMenu.add( jrbmi );
        jrbmi.setSelected( true );
        bg.add( jrbmi );
        jrbmi.setAction( new ResolverAction( "SIMBAD", simbadCatalogue ) );

        jrbmi = new JRadioButtonMenuItem();
        resolverMenu.add( jrbmi );
        bg.add( jrbmi );
        jrbmi.setAction( new ResolverAction( "NED", nedCatalogue ) );
        resolverCatalogue = simbadCatalogue;

        //  Create the Help menu.
        HelpFrame.createButtonHelpMenu( "ssa-window", "Help on window",
                                        menuBar, toolBar );

        //  ActionBar goes at bottom.
        contentPane.add( actionBarContainer, BorderLayout.SOUTH );
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    private void initFrame()
    {
        setTitle( Utilities.getTitle( "Query VO for Spectra" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        setSize( new Dimension( 550, 500 ) );
        setVisible( true );
    }


    /**
     * Populate the north part of center window with the basic query
     * components.
     */
    private void initQueryComponents()
    {
        JPanel queryPanel = new JPanel();
        queryPanel.setBorder
            ( BorderFactory.createTitledBorder( "Search region:" ) );

        GridBagLayouter layouter =
            new GridBagLayouter( queryPanel, GridBagLayouter.SCHEME3 );
        centrePanel.add( queryPanel, BorderLayout.NORTH );

        //  Object name. Arrange for a resolver to look up the coordinates of
        //  the object name, when return or the lookup button are pressed.
        JLabel nameLabel = new JLabel( "Object:" );
        nameField = new JTextField( 15 );
        nameField.setToolTipText( "Enter the name of an object " +
                                  "and press return to get coordinates" );
        nameField.addActionListener( this );
        layouter.add( nameLabel, false );
        layouter.add( nameField, false );

        nameLookup = new JButton( "Lookup" );
        nameLookup.addActionListener( this );
        layouter.add( nameLookup, true );


        //  RA and Dec fields. We're free-formatting on these (decimal degrees
        //  not required).
        JLabel raLabel = new JLabel( "RA:" );
        raField = new JTextField( 15 );
        layouter.add( raLabel, false );
        layouter.add( raField, true );
        raField.setToolTipText( "Enter the RA of field centre, " +
                                "decimal degrees or hh:mm:ss.ss" );

        JLabel decLabel = new JLabel( "Dec:" );
        decField = new JTextField( 15 );
        layouter.add( decLabel, false );
        layouter.add( decField, true );
        decField.setToolTipText( "Enter the Dec of field centre, " +
                                 "decimal degrees or dd:mm:ss.ss" );

        //  Radius field.
        JLabel radiusLabel = new JLabel( "Radius:" );
        radiusField = new JTextField( "10.0", 15 );
        layouter.add( radiusLabel, false );
        layouter.add( radiusField, true );
        radiusField.setToolTipText( "Enter radius of field to search" +
                                    " from given centre, arcminutes" );
        radiusField.addActionListener( this );

        //  Do the search.
        goButton = new JButton( "Go" );
        goButton.addActionListener( this );
        JPanel buttonPanel = new JPanel();
        buttonPanel.add( goButton );

        layouter.add( buttonPanel, true );
        layouter.eatSpare();

    }

    /**
     * Make the results component. This is mainly JTabbedPane containing a
     * JTable for each set of results (the tables are realized later) and
     * a button to display the selected spectra.
     */
    private void initResultsComponent()
    {
        JPanel resultsPanel = new JPanel( new BorderLayout() );
        resultsPanel.setBorder
            ( BorderFactory.createTitledBorder( "Query results:" ) );

        resultsPane = new JTabbedPane();
        resultsPanel.add( resultsPane, BorderLayout.CENTER );

        JPanel controlPanel = new JPanel();
        displaySelectedButton = new JButton( "Display selected" );
        controlPanel.add( displaySelectedButton );

        //  Add action to display all currently selected spectra.
        displaySelectedButton.addActionListener( this );

        displayAllButton = new JButton( "Display all" );
        controlPanel.add( displayAllButton );

        //  Add action to display all spectra.
        displayAllButton.addActionListener( this );

        resultsPanel.add( controlPanel, BorderLayout.SOUTH );
        centrePanel.add( resultsPanel, BorderLayout.CENTER );
    }


    /**
     * Arrange to resolve the object name into coordinates.
     */
    protected void resolveName()
    {
        String objectName = nameField.getText().trim();
        if ( objectName != null && objectName.length() > 0 ) {

            final QueryArgs queryArgs =
                new BasicQueryArgs( resolverCatalogue );

            // If objectName has spaces we should protect them.
            objectName = objectName.replaceAll( " ", "%20" );
            queryArgs.setId( objectName );

            Thread thread = new Thread( "Name server" )
            {
                public void run()
                {
                    try {
                        QueryResult r = resolverCatalogue.query( queryArgs );
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
                    catch (Exception e) {
                        new ExceptionDialog( null, e );
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
        //  Get the position.
        String ra = raField.getText();
        String dec = decField.getText();
        if ( ra == null || ra.length() == 0 ||
             dec == null || dec.length() == 0 ) {
            JOptionPane.showMessageDialog( this,
               "You have not supplied a search centre",
               "No RA or Dec", JOptionPane.ERROR_MESSAGE );
            return;
        }

        //  And the radius.
        String radiusText = radiusField.getText();
        double radius = 10.0;
        if ( radiusText != null && radiusText.length() > 0 ) {
            try {
                radius = Double.parseDouble( radiusText );
            }
            catch (NumberFormatException e) {
                new ExceptionDialog(this, "Cannot understand radius value", e);
                return;
            }
        }

        //  Create a stack of all queries to perform.
        ArrayList queryList = new ArrayList();
        Iterator i = serverList.getIterator();
        SimpleResource server = null;
        while( i.hasNext() ) {
            server = (SimpleResource) i.next();
            SSAQuery ssaQuery = new SSAQuery( server );
            ssaQuery.setPosition( ra, dec );
            ssaQuery.setRadius( radius );
            queryList.add( ssaQuery );
        }

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
     * If it does not already exist, make the panel for displaying
     * the progress of network access.
     */
    protected void makeProgressPanel()
    {
        if ( progressPanel == null ) {
            progressPanel = ProgressPanel.makeProgressPanel
                ( "Querying SSA servers for spectra...", this );

            //  When the cancel button is pressed we want to stop the
            //  SwingWorker thread. XXX Can we just skip current server?
            progressPanel.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    if ( worker != null ) {
                        worker.interrupt();
                        worker = null;
                    }
                }
            });
        }
    }

    /**
     * Process a list of URL queries to SSA servers and display the
     * results. All processing is performed in a background Thread.
     */
    protected void processQueryList( ArrayList queryList )
    {
        final ArrayList localQueryList = queryList;
        makeProgressPanel();

        worker = new SwingWorker()
        {
            boolean interrupted = false;
            public Object construct()
            {
                progressPanel.start();
                try {
                    runProcessQueryList( localQueryList );
                }
                catch (InterruptedException e) {
                    interrupted = true;
                }
                return null;
            }

            public void finished()
            {
                progressPanel.stop();
                worker = null;
                queryThread = null;

                //  Display the results.
                if ( ! interrupted ) {
                    makeResultsDisplay( localQueryList );
                }
            }
        };
        worker.start();
    }

    private Thread queryThread = null;

    /**
     * Do the query to all the SSAP servers.
     */
    private void runProcessQueryList( ArrayList queryList )
        throws InterruptedException
    {
        //  We just download VOTables, so avoid attempting to build the other
        //  formats.
        StarTableFactory factory = new StarTableFactory();
        TableBuilder[] blist = { new VOTableBuilder() };
        factory.setDefaultBuilders( blist );

        StarTable starTable = null;
        Iterator i = queryList.iterator();
        int j = 0;
        while( i.hasNext() ) {
            try {
                SSAQuery ssaQuery = (SSAQuery) i.next();
                URL url = ssaQuery.getQueryURL();
                progressPanel.logMessage( "Querying: " +
                                          ssaQuery.getDescription() );
                starTable = factory.makeStarTable( url );

                //  Check parameter QUERY_STATUS, this should be set to OK
                //  when the query
                String queryOK = null;
                try {
                    queryOK = starTable
                        .getParameterByName( "QUERY_STATUS" )
                        .getValueAsString( 100 );
                }
                catch (NullPointerException ne) {
                    // Whoops, that's not good, but see what we can do.
                    queryOK = "OK";
                }
                if ( "OK".equalsIgnoreCase( queryOK ) ) {
                    ssaQuery.setStarTable( starTable );
                    progressPanel.logMessage( "Done" );
                }
                else {
                    //  Some problem with the service.
                    progressPanel.logMessage( "Query failed: " + queryOK );
                }

                //  Dump query results as VOTables.
                //uk.ac.starlink.table.StarTableOutput sto =
                //    new uk.ac.starlink.table.StarTableOutput();
                //sto.writeStarTable( starTable,
                //                    "votable" + j + ".xml", null);
                j++;
            }
            catch (TableFormatException te) {
                progressPanel.logMessage( te.getMessage() );
                te.printStackTrace();
            }
            catch (IOException ie) {
                progressPanel.logMessage( ie.getMessage() );
                ie.printStackTrace();
            }
            catch (Exception ge ) {
                //  General exception.
                progressPanel.logMessage( ge.getMessage() );
                ge.printStackTrace();
            }
            if ( Thread.interrupted() ) {
                throw new InterruptedException();
            }
        }
        progressPanel.logMessage( "Completed downloads" );
    }

    /**
     * Display the results of the queries to the SSA servers.
     */
    protected void makeResultsDisplay( ArrayList tableList )
    {
        if ( starJTables == null ) {
            starJTables = new ArrayList();
        }

        //  Remove existing tables.
        resultsPane.removeAll();
        starJTables.clear();

        Iterator i = tableList.iterator();
        JScrollPane scrollPane = null;
        SSAQuery ssaQuery = null;
        StarTable starTable = null;
        StarJTable table = null;
        while ( i.hasNext() ) {
            ssaQuery = (SSAQuery) i.next();
            starTable = ssaQuery.getStarTable();
            if ( starTable != null ) {
                table = new StarJTable( starTable, true );
                scrollPane = new JScrollPane( table );
                resultsPane.addTab( ssaQuery.getDescription(), scrollPane );
                starJTables.add( table );

                //  Set widths of columns.
                table.configureColumnWidths( 200, 5 );

                //  Double click on row means load just that spectrum.
                table.addMouseListener( this );
            }
        }
    }

    /**
     * Get the main SPLAT browser to download and display spectra.
     * <p>
     * Can either display all the spectra, just the selected spectra, or the
     * spectrum from a row in a given table. If table is null, then the
     * selected parameter determines the behaviour of all or just the selected
     * spectra.
     */
    protected void displaySpectra( boolean selected, StarJTable table,
                                   int row )
    {
        //  List of all spectra to be loaded and their data formats and short
        //  names etc.
        ArrayList specList = new ArrayList();

        if ( table == null ) {
            //  Visit all the tabbed StarJTables.
            Iterator i = starJTables.iterator();
            while ( i.hasNext() ) {
                extractSpectraFromTable( (StarJTable) i.next(), specList,
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
        browser.threadLoadSpectra( propList );
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
     */
    private void extractSpectraFromTable( StarJTable table,
                                          ArrayList specList,
                                          boolean selected,
                                          int row )
    {
        int[] selection = null;

        //  Check for a selection if required, otherwise we're using the given
        //  row.
        if ( selected && row == -1 ) {
            selection = table.getSelectedRows();
        }
        else if ( row != -1 ) {
            selection = new int[1];
            selection[0] = row;
        }

        // Only do this if we're processing all rows or we have a selection.
        if ( selection == null || selection.length > 0 ) {
            StarTable starTable = table.getStarTable();

            //  Check for a column that contains links to the actual data
            //  (XXX these could be XML links to data within this
            //  document). The signature for this is an UCD of DATA_LINK.
            int ncol = starTable.getColumnCount();
            int linkcol = -1;
            int typecol = -1;
            int namecol = -1;
            int axescol = -1;
            int unitscol = 1;
            ColumnInfo colInfo;
            String ucd;
            for( int k = 0; k < ncol; k++ ) {
                colInfo = starTable.getColumnInfo( k );
                ucd = colInfo.getUCD().toLowerCase();
                if ( ucd.equals( "data_link" ) ) {
                    linkcol = k;
                }
                if ( ucd.equals( "vox:spectrum_format" ) ) {
                    typecol = k;
                }
                if ( ucd.equals( "vox:image_title" ) ) {
                    namecol = k;
                }
                if ( ucd.equals( "vox:spectrum_axes" ) ) {
                    axescol = k;
                }
                if ( ucd.equals( "vox:spectrum_units" ) ) {
                    unitscol = k;
                }
            }

            //  If we have a DATA_LINK column, gather the URLs it contains
            //  that are appropriate.
            if ( linkcol != -1 ) {
                RowSequence rseq = null;
                SpectrumIO.Props props;
                String value;
                String[] axes;
                String[] units;
                try {
                    if ( ! selected && selection == null ) {
                        //  Using all rows.
                        rseq = starTable.getRowSequence();
                        while ( rseq.next() ) {
                            value = ((String)rseq.getCell( linkcol )).trim();
                            props = new SpectrumIO.Props( value );
                            if ( typecol != -1 ) {
                                value = ((String)rseq.getCell(typecol)).trim();
                                props.setType( mimeToSPLATType( value ) );
                            }
                            if ( namecol != -1 ) {
                                value = ((String)rseq.getCell(namecol)).trim();
                                props.setShortName( value );
                            }
                            if ( axescol != -1 ) {
                                value = ((String)rseq.getCell(axescol)).trim();
                                axes = value.split("\\s");
                                props.setCoordColumn( axes[0] );
                                props.setDataColumn( axes[1] );
                                if ( axes.length == 3 ) {
                                    props.setErrorColumn( axes[2] );
                                }
                            }
                            if ( unitscol != -1 ) {
                                value =
                                    ((String)rseq.getCell( unitscol )).trim();
                                units = value.split("\\s");
                                props.setCoordUnits( units[0] );
                                props.setDataUnits( units[1] );
                                //  Error must have same units as data.
                            }
                            specList.add( props );
                        }
                    }
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
                                value = ((String)rseq.getCell(linkcol)).trim();
                                props = new SpectrumIO.Props( value );
                                if ( typecol != -1 ) {
                                    value =
                                        ((String)rseq.getCell(typecol)).trim();
                                    props.setType( mimeToSPLATType( value ) );
                                }
                                if ( namecol != -1 ) {
                                    value =
                                        ((String)rseq.getCell(namecol)).trim();
                                    props.setShortName( value );
                                }
                                if ( axescol != -1 ) {
                                    value =
                                        ((String)rseq.getCell(axescol)).trim();
                                    axes = value.split("\\s");
                                    props.setCoordColumn( axes[0] );
                                    props.setDataColumn( axes[1] );
                                }
                                if ( unitscol != -1 ) {
                                    value =
                                        ((String)rseq.getCell(unitscol)).trim();
                                    units = value.split("\\s");
                                    props.setCoordUnits( units[0] );
                                    props.setDataUnits( units[1] );
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
                    }
                }
                catch (IOException ie) {
                    ie.printStackTrace();
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
            }
        }
    }

    /**
     * Convert a of mime types into the equivalent SPLAT type (these are
     * int constants defined in SpecDataFactory).
     */
    private int mimeToSPLATType( String type )
    {
        int stype = SpecDataFactory.DEFAULT;
        String simpleType = type.toLowerCase();
        if ( simpleType.equals( "application/fits" ) ) {
            //  FITS format, is that image or table?
            stype = SpecDataFactory.FITS;
        }
        else if ( simpleType.equals( "spectrum/fits" ) ) {
            //  FITS format, is that image or table? Don't know who
            //  thought this was a mime-type?
            stype = SpecDataFactory.FITS;
        }
        else if ( simpleType.equals( "text/plain" ) ) {
            //  ASCII table of some kind.
            stype = SpecDataFactory.TABLE;
        }
        else if ( simpleType.equals( "application/x-votable+xml" ) ) {
            // VOTable spectrum.
            stype = SpecDataFactory.TABLE;
        }
        else if ( simpleType.equals( "spectrum/votable" ) ) {
            // VOTable spectrum or SED (from SDSS?)...
            stype = SpecDataFactory.SED;
        }
        return stype;
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
        proxyWindow.show();
    }

    /**
     *  Close the window.
     */
    protected void closeWindowEvent()
    {
        this.dispose();
    }

    /**
     * Configure the SSA servers.
     */
    protected void showServerWindow()
    {
        if ( serverWindow == null ) {
            serverWindow = new SSAServerFrame( serverList );
        }
        serverWindow.setVisible( true );
    }

    /**
     * Inner class defining Action for closing window.
     */
    protected class CloseAction extends AbstractAction
    {
        public CloseAction( String name, Icon icon )
        {
            super( name, icon );
        }
        public void actionPerformed( ActionEvent ae )
        {
            closeWindowEvent();
        }
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
        if ( source.equals( nameLookup ) || source.equals( nameField ) ) {
            resolveName();
            return;
        }

        if ( source.equals( radiusField ) || source .equals( goButton ) ) {
            doQuery();
            return;
        }

        if ( source.equals( displaySelectedButton ) ) {
            displaySpectra( true, null, -1 );
            return;
        }
        if ( source.equals( displayAllButton ) ) {
            displaySpectra( false, null, -1 );
            return;
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
        if ( e.getClickCount() == 2 ) {
            StarJTable table = (StarJTable) e.getSource();
            Point p = e.getPoint();
            int row = table.rowAtPoint( p );
            displaySpectra( false, table, row );
        }
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
    //  Action to display the proxy dialog.
    //
    protected class ProxyAction
        extends AbstractAction
    {
        public ProxyAction( String name )
        {
            super( name );
        }
        public void actionPerformed( ActionEvent ae )
        {
            showProxyDialog();
        }
    }

    //
    //  Action to display the SSA server configuration window.
    //
    protected class ServerAction
        extends AbstractAction
    {
        public ServerAction( String name, Icon icon, String help )
        {
            super( name, icon );
            putValue( SHORT_DESCRIPTION, help );
        }
        public void actionPerformed( ActionEvent ae )
        {
            showServerWindow();
        }
    }
}

