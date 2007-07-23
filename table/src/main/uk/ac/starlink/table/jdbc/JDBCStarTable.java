package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.WrapperRowSequence;

/**
 * A StarTable implementation based on the results of an SQL query 
 * on a JDBC table.
 */
public class JDBCStarTable extends AbstractStarTable {

    private ColumnInfo[] colInfos_;
    private final Connector connx_;
    private final String sql_;

    /**
     * Holds a random access ResultSet if this object provides random access.
     * The contents of this variable (null or otherwise) is used as the
     * flag to indicate whether this object provides random access or not.
     */
    private StarResultSet randomSet_;

    /* Ad-hoc regular expressions to identify different JDBC drivers. */
    private static final Pattern POSTGRESQL_DRIVER_REGEX =
        Pattern.compile( ".*PostgreSQL.*", Pattern.CASE_INSENSITIVE );
    private static final Pattern MYSQL_DRIVER_REGEX =
        Pattern.compile( ".*MySQL.*", Pattern.CASE_INSENSITIVE );
    private static final Pattern SQLSERVER_DRIVER_REGEX =
        Pattern.compile( ".*SQL.?Server.*", Pattern.CASE_INSENSITIVE );

    /* Parameters. */
    private final static ValueInfo SQL_INFO =
        new DefaultValueInfo( "SQL", String.class, "SQL query text" );

    private static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.table.jdbc" );

    /**
     * Constructs a StarTable representing the data returned by an
     * SQL query using a JDBC connections from a given source, 
     * with sequential access only.
     *
     * @param  connx object which can supply JDBC connections
     * @param  sql   text of the SQL query
     */
    public JDBCStarTable( Connector connx, String sql ) throws SQLException {
        this( connx, sql, false );
        getParameters().add( new DescribedValue( SQL_INFO, sql ) );
    }

    /**
     * Constructs a StarTable representing the data returned by an
     * SQL query using JDBC connections from a given source,
     * optionally providing random access.
     * <p>
     * This was initially written to take a {@link java.sql.Connection} 
     * rather than a {@link Connector} object, but it seems that there
     * are limits to the number of <tt>ResultSet</tt>s that can be
     * simultaneously open on a <tt>Connection</tt>.
     *
     * @param  connx object which can supply JDBC connections
     * @param  sql   text of the SQL query
     * @param  isRandom  whether this table needs to provide random access or
     *         not (there are costs associated with this)
     */
    public JDBCStarTable( Connector connx, String sql, boolean isRandom ) 
            throws SQLException {
        connx_ = connx;
        sql_ = sql;
        Connection conn = connx.getConnection();
        setName( conn.getMetaData().getURL() + '#' + sql );

        /* If random access is required, create a scrollable ResultSet
         * which we can use for random access queries. */
        if ( isRandom ) {
            Statement stmt = 
                conn.createStatement( ResultSet.TYPE_SCROLL_INSENSITIVE,
                                      ResultSet.CONCUR_READ_ONLY );
            randomSet_ = new StarResultSet( makeRandomResultSet( conn, sql ) );
        }

        /* Get a resultset to determine necessary metadata.  If we already
         * have a random one use that, otherwise just knock up a quickie one. */
        if ( isRandom ) {
            colInfos_ = randomSet_.getColumnInfos();
        }
        else {

            /* The intention here is only to select a single row, which 
             * should be cheap.  However, MySQL at least takes just as long
             * to retrieve this single row as to retrieve the entire 
             * ResultSet :-(. */
            Statement stmt = createStreamingStatement( conn );
            stmt.setMaxRows( 1 );
            ResultSet rset = stmt.executeQuery( sql );
            colInfos_ = new StarResultSet( rset ).getColumnInfos();
            rset.close();
        }
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public List getColumnAuxDataInfos() {
        return StarResultSet.getColumnAuxDataInfos();
    }

    public int getColumnCount() {
        return colInfos_.length;
    }

    public long getRowCount() {
        return randomSet_ == null ? -1L
                                  : randomSet_.getRowCount();
    }

    /**
     * Ensures that this table provides random access.
     * Following this call the <tt>isRandom</tt> method will return true.
     * Calling this method multiple times is harmless.
     */
    public void setRandom() throws SQLException {
        if ( randomSet_ == null ) {
            randomSet_ =
                new StarResultSet( makeRandomResultSet( connx_.getConnection(),
                                                        sql_ ) );
            checkConsistent( randomSet_ );
        }
    }

    public boolean isRandom() {
        return randomSet_ != null;
    }

    public Object getCell( long lrow, int icol ) throws IOException {
        if ( randomSet_ == null ) {
            throw new UnsupportedOperationException( "No random access" );
        }
        else {
            synchronized ( randomSet_ ) {
                randomSet_.setRowIndex( lrow );
                return randomSet_.getCell( icol );
            }
        }
    }

    public Object[] getRow( long lrow ) throws IOException {
        if ( randomSet_ == null ) {
            throw new UnsupportedOperationException( "No random access" );
        }
        else {
            synchronized ( randomSet_ ) {
                randomSet_.setRowIndex( lrow );
                return randomSet_.getRow();
            }
        }
    }

    public RowSequence getRowSequence() throws IOException {
        final StarResultSet srset;
        Connection conn = null;
        try {
            conn = connx_.getConnection();
            Statement stmt = createStreamingStatement( conn );
            srset = new StarResultSet( stmt.executeQuery( sql_ ) );
            checkConsistent( srset );
        }
        catch ( SQLException e ) {
            if ( conn != null ) {
                try {
                    conn.close();
                }
                catch ( SQLException e2) {
                }
            }
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        catch ( OutOfMemoryError e ) {
            if ( conn != null ) {
                try {
                    conn.close();
                }
                catch ( SQLException e2) {
                }
            }
            String msg = "Out of memory during SQL statement execution; "
                       + "looks like JDBC driver is assembling a read-only "
                       + "ResultSet in memory on the client, "
                       + "which is questionable behaviour";
            throw (OutOfMemoryError) new OutOfMemoryError( msg ).initCause( e );
        }
        assert conn != null;
        final Connection connection = conn;
        return new WrapperRowSequence( srset.createRowSequence() ) {
            public void close() throws IOException {
                try {
                    super.close();
                    if ( ! connection.getAutoCommit() ) {
                        connection.commit();
                    }
                    connection.close();
                }
                catch ( SQLException e ) {
                    throw (IOException) new IOException( e.getMessage() )
                                       .initCause( e );
                }
            }
        };
    }

    /**
     * Returns a JDBC Connection that can supply the data for this table.
     *
     * @return  a JDBC Connection object
     */
    public Connection getConnection() throws SQLException {
        return connx_.getConnection();
    }

    /**
     * Returns the text of the SQL query used for this table.
     *
     * @return   the SQL query text
     */
    public String getSql() {
        return sql_;
    }

    /**
     * Returns a new ResultSet suitable for random access.
     *
     * @param  conn  database connection
     * @param  sql   query text
     */
    private static ResultSet makeRandomResultSet( Connection conn, String sql )
            throws SQLException {
        return conn.createStatement( ResultSet.TYPE_SCROLL_INSENSITIVE,
                                     ResultSet.CONCUR_READ_ONLY )
                   .executeQuery( sql );
    }

    /**
     * Ensures that a given result set is consistent with the configuration
     * that this table thinks it has.  If not, an IllegalStateException
     * is thrown.  An inconsistency could arise if the underlying table
     * was modified in certain ways (e.g. a column added) between
     * execution of the SQL query.
     *
     * @param  srset  a new StarResultSet to check for 
     *                consistency with this object
     * @throws IllegalStateException  in case of inconsistency
     */
    private void checkConsistent( StarResultSet srset ) throws SQLException {
        if ( srset.getColumnInfos().length != colInfos_.length ) {
            throw new IllegalStateException( 
                "ResultSet column count has changed" );
        }
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
     * @return  statement which (hopefully) streams results
     */
    public static Statement createStreamingStatement( Connection conn )
            throws SQLException {

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
            Statement stmt = conn.createStatement();
            stmt.setFetchSize( 1024 );
            return stmt;
        }

        /* MySQL: see
         * http://dev.mysql.com/doc/refman/5.0/en/
         *    connector-j-reference-implementation-notes.html */
        else if ( MYSQL_DRIVER_REGEX.matcher( driver ).matches() ) {
            logger_.info( "Fixing MySQL driver to stream results" );
            Statement stmt = conn.createStatement( ResultSet.TYPE_FORWARD_ONLY,
                                                   ResultSet.CONCUR_READ_ONLY );
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
                return conn.createStatement( cursorType, 
                                             ResultSet.CONCUR_READ_ONLY );
            }
            catch ( Throwable e ) {
                logger_.warning( "SQL Server tweaking failed: " + e );
                return conn.createStatement();
            }
        }

        /* Other. */
        else {
            logger_.info( "No special steps to stream results - "
                       + "may run out of memory for large ResultSet?" );
            return conn.createStatement();
        }
    }
}
