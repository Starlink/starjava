package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.StarTable;

public class JDBCHandler {

    private JDBCAuthenticator auth;
    private String user;
    private String passwd;
    private boolean authDone;

    public JDBCHandler() {
        this( new TerminalAuthenticator() );
    }

    public JDBCHandler( JDBCAuthenticator auth ) {
        this.auth = auth;
    }

    /**
     * Clone constructor.  This constructor creates a new JDBCHandler
     * with the all the same fields as the given one <tt>jh</tt>.
     *
     * @param   jh  the handler whose fields to copy
     */
    public JDBCHandler( JDBCHandler jh ) {
        this( jh.auth );
        this.user = jh.user;
        this.passwd = jh.passwd;
        this.authDone = jh.authDone;
    }

    public JDBCAuthenticator getAuthenticator() {
        return auth;
    }

    public void setAuthenticator( JDBCAuthenticator auth ) {
        auth.getClass();  // check for null
        this.auth = auth;
    }

    public StarTable makeStarTable( String spec, boolean wantRandom )
            throws IOException {

        /* Reject if it doesn't look like a JDBC URL. */
        if ( ! spec.startsWith( "jdbc:" ) ) {
            throw new IllegalArgumentException( "Not a JDBC-protocol URL" );
        }

        /* Reject if no SQL query is present. */
        spec = unEscape( spec );
        int hashPos = spec.indexOf( "#" );
        if ( hashPos < 0 ) {
            throw new IOException( 
                "Bad JDBC specification, should be jdbc:...#SQL-query" );
        }
        String frag = spec.substring( hashPos + 1 );
        final String url = spec.substring( 0, hashPos );

        /* Try to get a StarTable using the query. */
        try {
            Connector connector = new Connector() {
                public Connection getConnection() throws SQLException {
                    try {
                        return JDBCHandler.this.getConnection( url );
                    }
                    catch ( IOException e ) {
                        throw (SQLException) 
                              new SQLException( "Authentication failed" )
                             .initCause( e );
                    }
                }
            };
            try {
                return new JDBCStarTable( connector, frag, wantRandom );
            }

            /* The open may fail if we've asked for a random one due to 
             * server restrictions - in this case try getting a sequential
             * one instead. */
            catch ( SQLException e ) {
                if ( wantRandom ) {
                    return new JDBCStarTable( connector, frag, false );
                }
                else {
                    throw e;
                }
            }
        }
        catch ( SQLException e ) {
            StringBuffer sbuf = new StringBuffer()
                .append( "Error making connection " )
                .append( url );
            Enumeration den = DriverManager.getDrivers();
            if ( den.hasMoreElements() ) {
                sbuf.append( " - known JDBC drivers:\n" );
                while ( den.hasMoreElements() ) {
                    sbuf.append( "   " )
                        .append( den.nextElement().getClass().getName() )
                        .append( '\n' );
                }
            }
            else {
                sbuf.append( " - no known JDBC drivers" );
            }
            throw (IOException) new IOException( sbuf.toString() )
                               .initCause( e );
        }
    }

    public void createJDBCTable( StarTable startab, String spec ) 
            throws IOException, SQLException {

        /* Reject if it doesn't look like a JDBC URL. */
        if ( ! spec.startsWith( "jdbc: " ) ) { 
            throw new IllegalArgumentException( "Not a JDBC-protocol URL" );
        }

        /* Reject if no table name is present. */
        int hashPos = spec.indexOf( "#" );
        if ( hashPos < 0 ) {
            throw new IOException(
                "Bad JDBC specification, should be \"jdbc:...#table-name\"" );
        }
        String frag = spec.substring( hashPos + 1 );
        String url = spec.substring( 0, hashPos );

        /* Try to get a connection using the URL. */
        Connection conn = getConnection( url );
 
        /* And write the data to the table. */
        new JDBCFormatter( conn ).createJDBCTable( startab, frag );
    }

    private Connection getConnection( String url )
            throws IOException, SQLException {
        if ( ! authDone ) {

            /* First attempt a connection without any authentication. */
            try {
                return DriverManager.getConnection( url );
            }

            /* If that fails for any reason, request authentication details
             * so we can try again. */
            catch ( SQLException e ) {
                String[] authInfo = auth.authenticate();
                user = authInfo[ 0 ];
                passwd = authInfo[ 1 ];
                authDone = true;
            }
        }
        return DriverManager.getConnection( url, user, passwd );
    }

    /**
     * Indicates whether any drivers are installed.
     *
     * @return  <tt>true</tt> iff at least one JDBC driver is available
     *          for URL resolution
     */
    public static boolean hasDrivers() {
        return DriverManager.getDrivers().hasMoreElements();
    }

    /**
     * Unescapes URL-type escaped characters (%xx).
     *
     * @param   url   string which may contain escaped characters
     * @return  string with escaped characters replaced by literals
     */
    private static String unEscape( String url ) {
        StringBuffer buf = new StringBuffer();
        Matcher m = Pattern.compile( "%[0-9a-fA-F][0-9a-fA-F]" )
                           .matcher( url );
        while ( m.find() ) {
            char c = (char) Integer.parseInt( m.group().substring( 1, 3 ), 16 );
            m.appendReplacement( buf, new String( new char[] { c } ) );
        }
        m.appendTail( buf );
        return buf.toString();
    }

}
