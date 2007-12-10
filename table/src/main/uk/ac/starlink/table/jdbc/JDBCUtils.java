package uk.ac.starlink.table.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Utilties related to JDBC.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2007
 */
public class JDBCUtils {

    /* Ad-hoc regular expressions to identify different JDBC drivers. */
    private static final Pattern POSTGRESQL_DRIVER_REGEX =
        Pattern.compile( ".*PostgreSQL.*", Pattern.CASE_INSENSITIVE );
    private static final Pattern MYSQL_DRIVER_REGEX =
        Pattern.compile( ".*MySQL.*", Pattern.CASE_INSENSITIVE );
    private static final Pattern SQLSERVER_DRIVER_REGEX =
        Pattern.compile( ".*SQL.?Server.*", Pattern.CASE_INSENSITIVE );

    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.jdbc" );

    /**
     * Private sole constructor prevents instantiation.
     */
    private JDBCUtils() {
    }

    /**
     * Returns a statement which tries its best to stream data.
     * It may be necessary to jump through various (database/driver-dependent)
     * hoops to persuade JDBC not to grab the whole query result and
     * store it locally - doing that risks running out of heap memory
     * in this JVM for large queries.
     *
     * <p>Note that in some cases the supplied connection may have its
     * autocommit mode modified by this call.
     *
     * @param   conn  connection
     * @param   update  true for an updatable set, false for read-only
     * @return  statement which (hopefully) streams results
     */
    public static Statement createStreamingStatement( Connection conn, 
                                                      boolean update )
            throws SQLException {
        int concurrency = update ? ResultSet.CONCUR_UPDATABLE
                                 : ResultSet.CONCUR_READ_ONLY;

        /* Work out what database (driver) we are using. */
        DatabaseMetaData metadata = conn.getMetaData();
        String driver = metadata.getDriverName();
        if ( driver == null ) {
            driver = "";
        }

        /* PostgreSQL: see
         * http://jdbc.postgresql.org/documentation/81/query.html
         *    #query-with-cursor */
        if ( POSTGRESQL_DRIVER_REGEX.matcher( driver ).matches() ) {
            logger_.info( "Fixing PostgreSQL driver to stream results" );
            conn.setAutoCommit( false );
            Statement stmt = conn.createStatement( ResultSet.TYPE_FORWARD_ONLY,
                                                   concurrency );
            stmt.setFetchSize( 1024 );
            return stmt;
        }

        /* MySQL: see
         * http://dev.mysql.com/doc/refman/5.0/en/
         *    connector-j-reference-implementation-notes.html */
        else if ( MYSQL_DRIVER_REGEX.matcher( driver ).matches() ) {
            logger_.info( "Fixing MySQL driver to stream results" );
            Statement stmt = conn.createStatement( ResultSet.TYPE_FORWARD_ONLY,
                                                   concurrency );
            stmt.setFetchSize(Integer.MIN_VALUE);
            return stmt;
        }

        /* SQL Server: see
         *    http://msdn2.microsoft.com/en-us/library/ms378405.aspx
         * (untested). */
        else if ( SQLSERVER_DRIVER_REGEX.matcher( driver ).matches() ) {
            logger_.info( "Fixing SQL Server driver to stream results" );
            try {
                int cursorType =
                    Class.forName( "com.microsoft.sqlserver.jdbc."
                                 + "SQLServerResultSet" )
                         .getField( "TYPE_SS_SERVER_CURSOR_FORWARD_ONLY" )
                         .getInt( null );
                assert cursorType == 2004;
                return conn.createStatement( cursorType, concurrency );
            }
            catch ( Throwable e ) {
                logger_.warning( "SQL Server tweaking failed: " + e );
                return conn.createStatement( ResultSet.TYPE_FORWARD_ONLY,
                                             concurrency );
            }
        }

        /* Other. */
        else {
            logger_.info( "No special steps to stream results - "
                       + "may run out of memory for large ResultSet?" );
            return conn.createStatement( ResultSet.TYPE_FORWARD_ONLY,
                                         concurrency );
        }
    }
}
