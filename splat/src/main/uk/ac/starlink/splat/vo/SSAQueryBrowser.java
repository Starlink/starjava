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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
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

import uk.ac.starlink.splat.iface.HelpFrame;
import uk.ac.starlink.splat.iface.images.ImageHolder;
import uk.ac.starlink.splat.util.Utilities;
import uk.ac.starlink.splat.util.ExceptionDialog;
import uk.ac.starlink.util.ProxySetup;
import uk.ac.starlink.util.gui.GridBagLayouter;
import uk.ac.starlink.util.gui.ProxySetupFrame;

import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.gui.StarJTable;
import uk.ac.starlink.votable.VOTableBuilder;

/**
 * Display a page of controls for querying an SSA server and display the
 * results of that query. The spectra returned from that query can then be
 * selected and displayed in the main SPLAT browser.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class SSAQueryBrowser
    extends JFrame
{
    /**
     * The object holding the list of servers that we should use for SSA
     * querys.
     */
    private SSAServerList serverList = null;

    /** Content pane of frame */
    protected JPanel contentPane = null;

    /** Panel for action buttons */
    protected JPanel actionBarContainer = null;

    /** Menubar */
    protected JMenuBar menuBar = null;

    /** The file menu */
    protected JMenu fileMenu = null;

    /** Object name */
    protected JTextField nameField = null;

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

    /** NED nameserver catalogue */
    protected SkycatCatalog nedCatalogue = null;

    /** SIMBAD nameserver catalogue */
    protected SkycatCatalog simbadCatalogue = null;

    static {
        ProxySetup.getInstance().restore();
    }

    /**
     * Create an instance.
     */
    public SSAQueryBrowser( SSAServerList serverList )
    {
        this.serverList = serverList;
        initUI();
        initMenus();
        initFrame();
    }

    /**
     * Initialise frame properties (disposal, title, menus etc.).
     */
    private void initFrame()
    {
        setTitle( Utilities.getTitle( "Query VO for Spectra" ) );
        setDefaultCloseOperation( JFrame.HIDE_ON_CLOSE );
        contentPane.add( actionBarContainer, BorderLayout.SOUTH );
        setSize( new Dimension( 550, 500 ) );
        setVisible( true );
    }

    /**
     * Initialise the menu bar, action bar and related actions.
     */
    private void initMenus()
    {
        //  Add the menuBar.
        menuBar = new JMenuBar();
        setJMenuBar( menuBar );

        //  Get icons.
        ImageIcon closeImage = new ImageIcon(
            ImageHolder.class.getResource( "close.gif" ) );
        ImageIcon helpImage = new ImageIcon(
            ImageHolder.class.getResource( "help.gif" ) );

        //  Create the File menu.
        fileMenu = new JMenu( "File" );
        menuBar.add( fileMenu );

        //  Add an action to close the window.
        CloseAction closeAction = new CloseAction( "Close", closeImage );
        fileMenu.add( closeAction );
        JButton closeButton = new JButton( closeAction );
        actionBarContainer = new JPanel();
        actionBarContainer.add( closeButton );
        closeButton.setToolTipText( "Close window" );

        //  Create the Help menu.
        HelpFrame.createHelpMenu( "ssa-window", "Help on window",
                                  menuBar, null );
    }


    /**
     * Create and display the UI components.
     */
    private void initUI()
    {
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout( new BorderLayout() );
        initQueryComponents();

        initResultsComponent();

        setDefaultNameServers();
    }

    /**
     * Populate the NORTH part of window with the basic query components.
     */
    private void initQueryComponents()
    {
        JPanel queryPanel = new JPanel();
        queryPanel.setBorder
            ( BorderFactory.createTitledBorder( "Search region:" ) );

        GridBagLayouter layouter = 
            new GridBagLayouter( queryPanel, GridBagLayouter.SCHEME3 );
        contentPane.add( queryPanel, BorderLayout.NORTH );

        //  Object name.
        JLabel nameLabel = new JLabel( "Object:" );
        nameField = new JTextField( 15 );
        nameField.setToolTipText( "Enter the name of an object " +
                                  "and press return to get coordinates" );
        layouter.add( nameLabel, false );
        layouter.add( nameField, false );
        
        JButton nameLookup = new JButton( "Lookup" );
        layouter.add( nameLookup, true );

        nameLookup.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    resolveName();
                }
            });

        //  Also need to arrange for a resolver to look up the coordinates of
        //  the object name, when return is pressed.
        nameField.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    resolveName();
                }
            });

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
        
        //  Do the search.
        JButton goButton = new JButton( "Go" );
        JPanel buttonPanel = new JPanel();
        buttonPanel.add( goButton );

        layouter.add( buttonPanel, true );
        layouter.eatSpare();

        goButton.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    doQuery();
                }
            });
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
        JButton displayButton = new JButton( "Display" );
        controlPanel.add( displayButton );
        resultsPanel.add( controlPanel, BorderLayout.SOUTH );

        contentPane.add( resultsPanel, BorderLayout.CENTER );
    }


    /**
     * Arrange to resolve the object name into coordinates.
     */
    protected void resolveName()
    {
        String objectName = nameField.getText();
        if ( objectName != null && objectName.length() > 0 ) {

            //  Should offer choice between NED and SIMBAD.
            final QueryArgs queryArgs = new BasicQueryArgs( simbadCatalogue );
            queryArgs.setId( objectName );

            Thread thread = new Thread( "Name server" ) {
               public void run() {
                  try {
                      QueryResult r = simbadCatalogue.query( queryArgs );
                      if ( r instanceof TableQueryResult ) {
                          Coordinates coords =
                              ((TableQueryResult) r).getCoordinates(0);
                          if ( coords instanceof WorldCoords ) {
                              //  Enter values into RA and Dec fields.
                              String[] radec = ((WorldCoords)coords).format();
                              raField.setText( radec[0] );
                              decField.setText( radec[1] );
                          }
                      }
                  }
                  catch (Exception e) {
                      e.printStackTrace();
                  }
               }
            };

            //  Start the nameserver.
            thread.start();
        }
    }

    /**
     * Setup the default name servers (SIMBAD and NED) to use to resolve
     * astronomical object names. Note these are just those used in JSky.
     * A better implementation should reuse the JSky classes.
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
     * Perform the query to all the known SSA servers (XXX need to select
     * which servers to use).
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
        SSAServer server = null;
        while( i.hasNext() ) {
            SSAQuery ssaQuery = new SSAQuery( (SSAServer) i.next() );
            ssaQuery.setPosition( ra, dec );
            ssaQuery.setRadius( radius );
            queryList.add( ssaQuery );
        }

        // Now actually do the queries, these are performed in a separate
        // Thread so we avoid locking the interface.
        processQueryList( queryList );
    }

    /**
     * Process a list of URL queries to SSA servers and display the
     * results. All processing is performed in a background Thread.
     * 
     * XXX need to use an interruptable thread.
     */
    protected void processQueryList( ArrayList queryList )
    {
        final ArrayList localQueryList = queryList;
        Thread thread = new Thread( "SSA server queries" ) {
                public void run() 
                {
                    Iterator i = localQueryList.iterator();
                    StarTableFactory factory = new StarTableFactory();
                    TableBuilder[] blist = { new VOTableBuilder() };
                    factory.setDefaultBuilders( blist );
                    StarTable starTable = null;

                    int j = 0;
                    while( i.hasNext() ) {
                        try {
                            SSAQuery ssaQuery = (SSAQuery) i.next();
                            URL url = ssaQuery.getQueryURL();
                            System.out.println( "URL = " + url );
                            starTable = factory.makeStarTable( url );
                            ssaQuery.setStarTable( starTable );
                            System.out.println( starTable );
                            
                            uk.ac.starlink.table.StarTableOutput sto = 
                                new uk.ac.starlink.table.StarTableOutput();
                            sto.writeStarTable( starTable, 
                                                "votable" + j + ".xml", null);
                            j++;
                        }
                        catch (IOException ie) {
                            //  XXX Should make a proper report.
                            ie.printStackTrace();
                        }
                    }
                }
            };

        //  Start the nameserver.
        thread.start();

        // Wait for completion?
        try {
            thread.join();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println( "Completed" );

        //  Display the results.
        makeResultsDisplay( localQueryList );
    }

    /**
     * Display the results of the queries to the SSA servers.
     */
    protected void makeResultsDisplay( ArrayList tableList )
    {
        if ( starJTables == null ) {
            starJTables = new ArrayList();
        }

        //  Remove existing tables (XXX reuse for efficiency).
        Iterator i = starJTables.iterator();
        StarJTable table = null;
        resultsPane.removeAll();
        starJTables.clear();

        i = tableList.iterator();
        JScrollPane scrollPane = null;
        SSAQuery ssaQuery = null;
        StarTable starTable = null;
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
            }
        }
    }

    /**
     *  Close the window.
     */
    protected void closeWindowEvent()
    {
        this.dispose();
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
        SSAQueryBrowser b = new SSAQueryBrowser( new SSAServerList() );
        b.pack();
        b.setVisible( true );
    }
}
