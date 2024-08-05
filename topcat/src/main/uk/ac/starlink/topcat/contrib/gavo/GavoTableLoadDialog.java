package uk.ac.starlink.topcat.contrib.gavo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.table.gui.AbstractTableLoadDialog;
import uk.ac.starlink.table.gui.TableLoader;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.util.URLUtils;

public class GavoTableLoadDialog extends AbstractTableLoadDialog {

    private int nquery = 0;

    private static final String MILL_URL =
        "http://gavo.mpa-garching.mpg.de/Millennium";
    private static final String MYMILL_URL =
        "http://gavo.mpa-garching.mpg.de/MyMillennium";
    private static final String VIRGO_URL =
        "http://virgodb.dur.ac.uk:8080/MyMillennium";
    private static final String EAGLE_URL =
        "http://virgodb.dur.ac.uk:8080/Eagle";
    private static final String QUERY_TRAIL = "?action=doQuery&SQL=";
    private static final Database[] DATABASES = new Database[] {
        new Database( MILL_URL, MILL_URL + QUERY_TRAIL, false ),
        new Database( MYMILL_URL, MYMILL_URL + QUERY_TRAIL, true ),
        new Database( VIRGO_URL, VIRGO_URL + QUERY_TRAIL, true ),
        new Database( EAGLE_URL, EAGLE_URL + QUERY_TRAIL, true ),
    };

    private static final ValueInfo URL_INFO =
        new DefaultValueInfo( "Database", String.class,
                              "Base URL of database providing results" );
    private static final ValueInfo SQL_INFO =
        new DefaultValueInfo( "SQL", String.class,
                              "Text of SQL query" );

    private static final Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.topcat.contrib" );

    private JComboBox<Database> urlField_;
    private JTextField userField_;
    private JPasswordField passField_;
    private JTextArea sqlField_;

    /**
     * Constructor.  A public no-arg constructor is required by STIL's
     * pluggable load dialogue mechanism.
     */
    @SuppressWarnings("this-escape")
    public GavoTableLoadDialog() {
        super( "Virgo-Millennium Simulation Query",
               "Uses GAVO-like services to query databases of " +
               "Millennium, VirgoDB, EAGLE simulations etc" );
        setIcon( ResourceIcon.GAVO );
    }

    protected Component createQueryComponent() {

        /* Set up fields for user interaction. */
        urlField_ = new JComboBox<Database>(DATABASES);
        urlField_.setEditable(true);
        userField_ = new JTextField();
        passField_ = new JPasswordField();
        sqlField_ = new JTextArea();
        sqlField_.setEditable( true );
        sqlField_.setFont( Font.decode( "Monospaced" ) );

        /* Arrange them in a GUI container. */
        LabelledComponentStack stack = new LabelledComponentStack();
        stack.addLine( "Base URL", urlField_ );
        stack.addLine( "User", userField_ );
        final JLabel userLabel =
            stack.getLabels()[ stack.getLabels().length - 1 ];
        stack.addLine( "Password", passField_ );
        final JLabel passLabel =
            stack.getLabels()[ stack.getLabels().length - 1 ];
//        stack.addLine( "Is numeric", isNumericCB_ );

        ActionListener urlListener = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                Database db = getSelectedDatabase();
                boolean acceptsAuth = db != null && db.acceptsAuth_;
                userLabel.setEnabled( acceptsAuth );
                userField_.setEnabled( acceptsAuth );
                passLabel.setEnabled( acceptsAuth );
                passField_.setEnabled( acceptsAuth );
            }
        };
        urlListener.actionPerformed( null );
        urlField_.addActionListener( urlListener );

        JComponent sqlHolder = new JPanel( new BorderLayout() );
        Box labelBox = Box.createVerticalBox();
        labelBox.add( new JLabel( "SQL Query: " ) );
        labelBox.add( Box.createVerticalGlue() );
        sqlHolder.add( labelBox, BorderLayout.WEST );
        sqlHolder.add( new JScrollPane( sqlField_ ), BorderLayout.CENTER );
        sqlHolder.setBorder( BorderFactory.createEmptyBorder( 5, 0, 0, 0 ) );

        /* Menus. */
        JMenu haloMenu =
            createSampleMenu( "HaloSamples", GavoSampleQuery.HALO_SAMPLES );
        haloMenu.setMnemonic( KeyEvent.VK_H );
        JMenu galaxyMenu =
            createSampleMenu( "GalaxySamples", GavoSampleQuery.GAL_SAMPLES );
        galaxyMenu.setMnemonic( KeyEvent.VK_G );
        setMenus( new JMenu[] { haloMenu, galaxyMenu } );

        /* Place the components in a container panel and return it. */
        JPanel queryPanel = new JPanel( new BorderLayout() ) {
            public void setEnabled( boolean enabled ) {
                super.setEnabled( enabled );
                urlField_.setEnabled( enabled );
                userField_.setEnabled( enabled );
                passField_.setEnabled( enabled );
                sqlField_.setEnabled( enabled );
            }
        };
        queryPanel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
        queryPanel.add( stack, BorderLayout.NORTH );
        queryPanel.add( sqlHolder, BorderLayout.CENTER );
        queryPanel.setPreferredSize( new Dimension( 400, 300 ) );
        return queryPanel;
    }

    /**
     * Creates a menu of sample queries.
     *
     * @param   name  menu name
     * @param   list of query definitions
     */
    private JMenu createSampleMenu( String name, GavoSampleQuery[] queries ) {
        JMenu menu = new JMenu( name );
        for ( int i = 0; i < queries.length; i++ ) {
            GavoSampleQuery sample = queries[ i ];
            final String sqlText = sample.getText();
            Action act = new AbstractAction( sample.getName() ) {
                public void actionPerformed( ActionEvent evt ) {
                    sqlField_.setText( sqlText );
                }
            };
            act.putValue( Action.SHORT_DESCRIPTION, sample.getDescription() );
            menu.add( act );
        }
        return menu;
    }

    /**
     * Indicates whether this dialogue is available.  Should return false
     * if you happen to know somehow that that this service is unavailable.
     */
    public boolean isAvailable() {
        return true;
    }

    private Database getSelectedDatabase() {
        Object db = urlField_.getSelectedItem();
        if ( db instanceof Database ) {
            return (Database) db;
        }
        else if ( db instanceof String && ((String) db).trim().length() > 0 ) {
            String baseUrl = (String) db;
            return new Database( baseUrl, baseUrl + QUERY_TRAIL,  true );
        }
        else {
            return null;
        }
    }

    /**
     * Interrogates the internal state of this component and returns a
     * TableLoader object.
     */
    public TableLoader createTableLoader() {

        /* Get state. */
        Database db = getSelectedDatabase();
        if ( db == null ) {
            throw new IllegalArgumentException( "No database URL provided" );
        }
        final boolean acceptsAuth = db.acceptsAuth_;
        final String url = db.url_;
        final String user = acceptsAuth ? userField_.getText() : "";
        final String pass = acceptsAuth
                          ? new String( passField_.getPassword() )
                          : null;
        final String sql = sqlField_.getText();
        if ( sql == null || sql.trim().length() == 0 ) {
            throw new IllegalArgumentException( "No SQL query provided" );
        }
        nquery++;

        String sqlEncoding;
        try {
            sqlEncoding = URLEncoder.encode(sql, "UTF-8");
        }
        catch ( UnsupportedEncodingException e ) {
            throw new AssertionError( "Do what?" );
        }
        String userPassword = user + ":" + pass;
        final String encoding = base64Encode(userPassword.getBytes());
        String urlString = url+sqlEncoding;
        final URL queryUrl;
        try {
            queryUrl = URLUtils.newURL( urlString );
        }
        catch ( MalformedURLException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad url: " + url )
                 .initCause( e );
        }
        final String id = db.toString() + " query " + nquery;
        return new TableLoader() {
            public TableSequence loadTables( StarTableFactory tabFact )
                    throws IOException {
                logger_.info( queryUrl.toString() );
                URLConnection uc = queryUrl.openConnection();
                if ( acceptsAuth ) {
                    uc.setRequestProperty ("Authorization",
                                           "Basic " + encoding);
                }
           
                if ( uc instanceof HttpURLConnection ) {
                    HttpURLConnection huc = (HttpURLConnection) uc;
                    int code = huc.getResponseCode();
                    if ( code == 401 ) {
                        throw new ConnectException(
                            "Authorisation failed\n" + 
                            "Check username/password" );
                    }
                    else if ( code >= 400 ) {
                        throw new ConnectException(
                            "Connection failed:\n" + code + " " +
                            huc.getResponseMessage() );
                    }
                    logger_.info( "URL response: " + code );
                }
                InputStream stream = uc.getInputStream();
                logger_.info( "Content type = " + uc.getContentType() );
                StarTable table;
                GavoCSVTableParser csvParser =
                    new GavoCSVTableParser( tabFact.getStoragePolicy(),
                                            getQueryComponent() );
                try {
                    table = csvParser.parse(stream);
                }
                catch ( Throwable e ) {
                    throw asIOException( e );
                }
                finally {
                    stream.close();
                }
                table.setParameter( new DescribedValue( URL_INFO, url ) );
                table.setParameter( new DescribedValue( SQL_INFO, sql ) );
                return Tables.singleTableSequence( table );
            }
            public String getLabel() {
                return id;
            }
        };
    }

    /**
     * Returns the Base64 encoding of a given byte array.
     *
     * @param  buf  input byte array
     * @return   base64 encoding of input buffer
     */
    private static String base64Encode( byte[] buf ) {
        return new String( Base64.getEncoder().encode( buf ),
                           StandardCharsets.US_ASCII );
    }

    /**
     * Encapsulates the available target databases.
     */
    private static class Database {
        final String name_;
        final String url_;
        final boolean acceptsAuth_;
        Database( String name, String url, boolean acceptsAuth ) {
            name_ = name;
            acceptsAuth_ = acceptsAuth;
            url_ = url;
        }
        public String toString() {
            return name_;
        }
    }
}
