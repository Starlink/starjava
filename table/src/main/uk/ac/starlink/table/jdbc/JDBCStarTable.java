package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowAccess;
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
    @SuppressWarnings("this-escape")
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
     * are limits to the number of <code>ResultSet</code>s that can be
     * simultaneously open on a <code>Connection</code>.
     *
     * @param  connx object which can supply JDBC connections
     * @param  sql   text of the SQL query
     * @param  isRandom  whether this table needs to provide random access or
     *         not (there are costs associated with this)
     */
    @SuppressWarnings("this-escape")
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
            Statement stmt = JDBCUtils.createStreamingStatement( conn, false );
            stmt.setMaxRows( 1 );
            ResultSet rset = stmt.executeQuery( sql );
            colInfos_ = new StarResultSet( rset ).getColumnInfos();
            rset.close();
        }
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public List<ValueInfo> getColumnAuxDataInfos() {
        return TypeMappers.STANDARD.getColumnAuxDataInfos();
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
     * Following this call the <code>isRandom</code> method will return true.
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

    public RowAccess getRowAccess() throws IOException {
        if ( randomSet_ == null ) {
            throw new UnsupportedOperationException( "No random access" );
        }
        else {
            final StarResultSet rset;
            Connection conn = null;
            try {
                conn = connx_.getConnection();
                rset = new StarResultSet( makeRandomResultSet( conn, sql_ ) );
                checkConsistent( rset );
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
                throw (OutOfMemoryError)
                      new OutOfMemoryError( msg ).initCause( e );
            }
            final Connection conn0 = conn;
            return new RowAccess() {
                public void setRowIndex( long irow ) throws IOException {
                    rset.setRowIndex( irow );   
                }
                public Object getCell( int icol ) throws IOException {
                    return rset.getCell( icol );
                }
                public Object[] getRow() throws IOException {
                    return rset.getRow();
                }
                public void close() throws IOException {
                    try {
                        conn0.close();
                    }
                    catch ( SQLException e ) {
                        throw (IOException) new IOException( e.getMessage() )
                                           .initCause( e );
                    }
                }
            };
        }
    }

    public RowSequence getRowSequence() throws IOException {
        final StarResultSet srset;
        Connection conn = null;
        try {
            conn = connx_.getConnection();
            Statement stmt = JDBCUtils.createStreamingStatement( conn, false );
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
}
