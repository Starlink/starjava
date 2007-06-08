package uk.ac.starlink.topcat.contrib;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
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
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.BasicTableLoadDialog;
import uk.ac.starlink.table.gui.LabelledComponentStack;

public class GavoTableLoadDialog extends BasicTableLoadDialog {

    private int nquery = 0;

    private static final String MILL_URL =
        "http://www.g-vo.org/Millennium";
    private static final String MYMILL_URL =
        "http://www.g-vo.org/MyMillennium";
    private static final String QUERY_TRAIL = "?action=doQuery&SQL=";
    private static final Database[] DATABASES = new Database[] {
        new Database( MILL_URL, MILL_URL + QUERY_TRAIL, false ),
        new Database( MYMILL_URL, MYMILL_URL + QUERY_TRAIL, true ),
    };

    private static final ValueInfo URL_INFO =
        new DefaultValueInfo( "Database", String.class,
                              "Base URL of database providing results" );
    private static final ValueInfo SQL_INFO =
        new DefaultValueInfo( "SQL", String.class,
                              "Text of SQL query" );

    private static final Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.topcat.contrib" );

    private final JComboBox urlField_;
    private final JTextField userField_;
    private final JPasswordField passField_;
    private final JEditorPane sqlField_;

    /**
     * Constructor.  A public no-arg constructor is required by STIL's
     * pluggable load dialogue mechanism.
     */
    public GavoTableLoadDialog() {
        super( "GAVO query", "Queries GAVO database" );

        /* Set up fields for user interaction. */
        urlField_ = new JComboBox(DATABASES);
        urlField_.setEditable(true);
        userField_ = new JTextField();
        passField_ = new JPasswordField();
        sqlField_ = new JEditorPane();
        sqlField_.setEditable( true );

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
        setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );

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

        /* Place the container in the body of this component for display. */
        setLayout( new BorderLayout() );
        add( stack, BorderLayout.NORTH );
        add( sqlHolder, BorderLayout.CENTER );
        setPreferredSize( new Dimension( 400, 300 ) );
    }

    protected JDialog createDialog( java.awt.Component parent ) {
        JDialog dialog = super.createDialog( parent );
        JMenuBar mbar = new JMenuBar();
        dialog.setJMenuBar( mbar );
        JMenu sampleMenu = new JMenu( "SampleQueries" );
        mbar.add( sampleMenu );
        GavoSampleQuery[] queries = GavoSampleQuery.SAMPLES;
        for ( int i = 0; i < queries.length; i++ ) {
            GavoSampleQuery sample = queries[ i ];
            final String sqlText = sample.getText();
            Action act = new AbstractAction( sample.getName() ) {
                public void actionPerformed( ActionEvent evt ) {
                    sqlField_.setText( sqlText );
                }
            };
            act.putValue( Action.SHORT_DESCRIPTION, sample.getDescription() );
            sampleMenu.add( act );
        }
        return dialog;
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
     * TableSupplier object.
     */
    protected TableSupplier getTableSupplier() {

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
        final String encoding = new sun.misc.BASE64Encoder()
                               .encode (userPassword.getBytes());
        String urlString = url+sqlEncoding;
        final URL queryUrl;
        try {
            queryUrl = new URL( urlString );
        }
        catch ( MalformedURLException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad url: " + url )
                 .initCause( e );
        }
        final String id = db.toString() + " query " + nquery;
        return new TableSupplier() {
            public StarTable getTable( StarTableFactory tabFact, String fmt )
                    throws IOException {
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
                    new GavoCSVTableParser( tabFact.getStoragePolicy() );
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
                return table;
            }
            public String getTableID() {
                return id;
            }
        };
    }

    /**
     * Configures the components of this panel to match the enabledness of
     * this panel itself.
     *
     * @param  enabled  enabled status
     */
    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        urlField_.setEnabled( enabled );
        userField_.setEnabled( enabled );
        passField_.setEnabled( enabled );
        sqlField_.setEnabled( enabled );
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
