package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    public JDBCAuthenticator getAuthenticator() {
        return auth;
    }

    public void setAuthenticator( JDBCAuthenticator auth ) {
        auth.getClass();  // check for null
        this.auth = auth;
    }

    public StarTable makeStarTable( String spec ) throws IOException {

        /* Reject if it doesn't look like a JDBC URL. */
        if ( ! spec.startsWith( "jdbc:" ) ) {
            throw new IllegalArgumentException( "Not a JDBC-protocol URL" );
        }

        /* Reject if no SQL query is present. */
        int hashPos = spec.indexOf( "#" );
        if ( hashPos < 0 ) {
            throw new IOException( 
                "Bad JDBC specification, should be jdbc:...#SQL-query" );
        }
        String frag = spec.substring( hashPos + 1 );
        String url = spec.substring( 0, hashPos );

        /* Try to get a ResultSet and hence a StarTable using the query. */
        try {
            Connection conn = getConnection( url );
            Statement stmt = conn.createStatement();
            ResultSet rset = stmt.executeQuery( frag );
            return new ResultSetStarTable( rset );
        }
        catch ( SQLException e ) {
            throw (IOException) new IOException( "SQL error" ).initCause( e );
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
            String[] authInfo = auth.authenticate();
            user = authInfo[ 0 ];
            passwd = authInfo[ 1 ];
            authDone = true;
        }
        return DriverManager.getConnection( url, user, passwd );
    }

}
