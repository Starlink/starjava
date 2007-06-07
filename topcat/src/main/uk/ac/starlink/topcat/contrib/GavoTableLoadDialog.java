package uk.ac.starlink.topcat.contrib;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
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

    private List queries = new Vector();
    private GavoCSVTableParser csvParser = new GavoCSVTableParser();

    private static final Database[] DATABASES = new Database[] {
        new Database( "Millennium",
                      "http://www.g-vo.org/Millennium?action=doQuery&SQL=",
                      false ),
        new Database( "MyMillennium",
                      "http://www.g-vo.org/MyMillennium?action=doQuery&SQL=",
                      true ),
    };

    private static final ValueInfo URL_INFO =
        new DefaultValueInfo( "Database", String.class,
                              "Base URL of database providing results" );
    private static final ValueInfo SQL_INFO =
        new DefaultValueInfo( "SQL", String.class,
                              "Text of SQL query" );
    
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
        stack.addLine( "Query URL", urlField_ );
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
                Database db = (Database) urlField_.getSelectedItem();
                boolean requiresAuth = db != null && db.requiresAuth_;
                userLabel.setEnabled( requiresAuth );
                userField_.setEnabled( requiresAuth );
                passLabel.setEnabled( requiresAuth );
                passField_.setEnabled( requiresAuth );
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


    /**
     * Indicates whether this dialogue is available.  Should return false
     * if you happen to know somehow that that this service is unavailable.
     */
    public boolean isAvailable() {
        return true;
    }

    /**
     * Interrogates the internal state of this component and returns a
     * TableSupplier object.
     */
    protected TableSupplier getTableSupplier() {

        /* Get state. */
        Database db = (Database) urlField_.getSelectedItem();
        final boolean requiresAuth = db.requiresAuth_;
        final String url = db.url_;
        final String user = requiresAuth ? userField_.getText() : "";
        final String pass = requiresAuth
                          ? new String( passField_.getPassword() )
                          : null;
        final String sql = sqlField_.getText();
        queries.add(sql);

        /* Validate state, throwing an IllegalArgumentException if we can
         * tell straight away that this isn't going to result in a readable
         * table. */
        if ( url == null || url.trim().length() == 0 ||
             sql == null || sql.trim().length() == 0 ||
             requiresAuth && ( user == null || user.trim().length() == 0 ||
                               pass == null || pass.trim().length() == 0 ) ) {
            throw new IllegalArgumentException(
                "All fields must be filled in" );
        }

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
            // shouldn't happen
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad url: " + url )
                 .initCause( e );
        }
        final String id = db.toString() + " query " + queries.size();
        return new TableSupplier() {
            public StarTable getTable( StarTableFactory tabFact, String fmt )
                    throws IOException {
                URLConnection uc = queryUrl.openConnection();
                if ( requiresAuth ) {
                    uc.setRequestProperty ("Authorization",
                                           "Basic " + encoding);
                }
                InputStream stream = uc.getInputStream();
                Logger.getLogger( "uk.ac.starlink.topcat.contrib" )
                      .info( "Content type = " + uc.getContentType() );
                StarTable table;
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
        final boolean requiresAuth_;
        Database( String name, String url, boolean requiresAuth ) {
            name_ = name;
            requiresAuth_ = requiresAuth;
            url_ = url;
        }
        public String toString() {
            return name_;
        }
    }
}
