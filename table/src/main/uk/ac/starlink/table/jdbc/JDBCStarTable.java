package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.ValueInfo;


/**
 * A StarTable implementation based on the results of an SQL query 
 * on a JDBC table.
 */
public class JDBCStarTable extends AbstractStarTable {

    private int ncol;
    private ColumnInfo[] colinfo;
    private final Connector connx;
    private final String sql;

    /**
     * Holds a TYPE_SCROLL ResultSet if this object provides random access.
     * The contents of this variable (null or otherwise) is used as the
     * flag to indicate whether this object provides random access or not.
     */
    private ResultSet randomResultSet;

    private static Logger logger =
        Logger.getLogger( "uk.ac.starlink.table.jdbc" );

    /* Auxiliary metadata. */
    private final static ValueInfo labelInfo =
        new DefaultValueInfo( "Label", String.class );
    private final static List auxDataInfos = Arrays.asList( new ValueInfo[] {
        labelInfo,
    } );

    /* Parameters. */
    private final static ValueInfo sqlInfo =
        new DefaultValueInfo( "SQL", String.class, "SQL query text" );

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
        List params = getParameters();
        params.add( new DescribedValue( sqlInfo, sql ) );
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
        this.connx = connx;
        this.sql = sql;
        Connection conn = connx.getConnection();
        setName( conn.getMetaData().getURL() + '#' + sql );

        /* If random access is required, create a scrollable ResultSet
         * which we can use for random access queries. */
        if ( isRandom ) {
            Statement stmt = 
                conn.createStatement( ResultSet.TYPE_SCROLL_INSENSITIVE,
                                      ResultSet.CONCUR_READ_ONLY );
            randomResultSet = makeRandomResultSet( conn, sql );
        }

        /* Get a resultset to determine necessary metadata.  If we already
         * have a random one use that, otherwise just knock up a quickie one. */
        ResultSetMetaData rsmeta;
        if ( isRandom ) {
            rsmeta = randomResultSet.getMetaData();
        }
        else {
            Statement stmt = conn.createStatement();
            stmt.setMaxRows( 1 );
            ResultSet rset = stmt.executeQuery( sql );
            rsmeta = rset.getMetaData();
            rset.close();
        }

        /* Extract and store column metadata. */
        ncol = rsmeta.getColumnCount();
        colinfo = new ColumnInfo[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {

            /* SQL columns are based at 1 not 0. */
            int jcol = icol + 1;

            /* Set up the name and metadata for this column. */
            String name = rsmeta.getColumnName( jcol );
            colinfo[ icol ] = new ColumnInfo( name );
            ColumnInfo col = colinfo[ icol ];

            /* Find out what class objects will have.  If the class hasn't
             * been loaded yet, just call it an Object (could try obtaining
             * an object from that column and using its class, but then it
             * might be null...). */
            try {
                Class ccls = this.getClass().forName( rsmeta.getColumnClassName( jcol ) );
                col.setContentClass( ccls );
            }
            catch ( ClassNotFoundException e ) {
                col.setContentClass( Object.class );
            }
            if ( rsmeta.isNullable( jcol ) ==
                 ResultSetMetaData.columnNoNulls ) {
                col.setNullable( false );
            }
            List auxdata = col.getAuxData();
            String label = rsmeta.getColumnLabel( jcol );
            if ( label != null &&
                 label.trim().length() > 0 &&
                 ! label.equalsIgnoreCase( name ) ) {
                auxdata.add( new DescribedValue( labelInfo, label.trim() ) );
            }
        }
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colinfo[ icol ];
    }

    public List getColumnAuxDataInfos() {
        return auxDataInfos;
    }

    public int getColumnCount() {
        return ncol;
    }

    public long getRowCount() {

        /* If we have random access, get the number of rows by going to the
         * end and finding what row number it is.  This may not be cheap,
         * but random access tables are obliged to know how many rows
         * they have.  Unfortunately this is capable of throwing an 
         * SQLException, but this method is not declared to throw anything,
         * so deal with it best we can. */
        if ( randomResultSet != null ) {
            try {
                int lastRow;
                synchronized ( randomResultSet ) {
                    randomResultSet.afterLast();
                    randomResultSet.previous();
                    lastRow = randomResultSet.getRow();
                }
                return (long) lastRow;
            }
            catch ( SQLException e ) {
                logger.warning( "Failed to get length of table: " + e );
                return 0L;
            }
        }

        /* Otherwise, we don't know. */
        else {
            return -1L;
        }
    }

    /**
     * Ensures that this table provides random access.
     * Following this call the <tt>isRandom</tt> method will return true.
     * Calling this method multiple times is harmless.
     */
    public void setRandom() throws SQLException {
        if ( randomResultSet == null ) {
            randomResultSet = makeRandomResultSet( connx.getConnection(), sql );
            checkConsistent( randomResultSet );
        }
    }

    public boolean isRandom() {
        return randomResultSet != null;
    }

    public Object getCell( long lrow, int icol ) throws IOException {
        if ( randomResultSet == null ) {
            throw new UnsupportedOperationException( "No random access" );
        }
        try {
            synchronized ( randomResultSet ) {
                randomResultSet.absolute( checkedLongToInt( lrow ) + 1 );
                return getPackagedCell( randomResultSet, icol );
            }
        }
        catch ( SQLException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    public Object[] getRow( long lrow ) throws IOException {
        if ( randomResultSet == null ) {
            throw new UnsupportedOperationException( "No random access" );
        }
        try {
            Object[] row = new Object[ ncol ];
            synchronized ( randomResultSet ) {
                randomResultSet.absolute( checkedLongToInt( lrow ) + 1 );
                for ( int i = 0; i < ncol; i++ ) {
                    row[ i ] = getPackagedCell( randomResultSet, i );
                }
            }
            return row;
        }
        catch ( SQLException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    public RowSequence getRowSequence() throws IOException {
        final ResultSet rset;
        try {
            rset = connx.getConnection().createStatement().executeQuery( sql );
            checkConsistent( rset );
        }
        catch ( SQLException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
        return new RowSequence() {

            public void next() throws IOException {
               if ( hasNext() ) {
                    try {
                        boolean valid = rset.next();
                        if ( ! valid ) {
                            throw new IOException( "Next row not valid??" );
                        }
                    }
                    catch ( SQLException e ) {
                        throw (IOException) new IOException( e.getMessage() )
                                           .initCause( e );
                    }
                }
                else {
                    throw new IllegalStateException( "No next row" );
                }
            }        

            public void advance( long nrow ) throws IOException {
                if ( nrow >= 0 ) {

                    /* Can't use relative on a non-random ResultSet. */
                    try {
                        while ( nrow-- > 0 ) {
                            boolean valid = rset.next();
                            if ( ! valid ) {
                                throw new IOException( "Reached end of table" );
                            }
                        }
                    }
                    catch ( SQLException e ) {
                        throw (IOException) new IOException( e.getMessage() )
                                           .initCause( e );
                    }
                }
                else {
                    throw new IllegalArgumentException( "nrow < 0" );
                }
            }

            public long getRowIndex() {
                try {
                    return (long) rset.getRow() - 1;
                }
                catch ( SQLException e ) {
                    logger.warning( "Error getting row index: " + e );
                    return 0L;
                }
            }

            public boolean hasNext() {
                try {
                    return ! rset.isLast();
                }
                catch ( SQLException e ) {
                    logger.warning( e.getMessage() );
                    return false;
                }
            }

            public Object getCell( int icol ) throws IOException {
                try {
                    if ( rset.isBeforeFirst() ) {
                        throw new IllegalStateException( "No current row" );
                    }
                    else {
                        return getPackagedCell( rset, icol );
                    }
                }
                catch ( SQLException e ) {
                    throw (IOException) new IOException( e.getMessage() )
                                       .initCause( e );
                }
            }

            public Object[] getRow() throws IOException {
                try {
                    if ( rset.isBeforeFirst() ) {
                        throw new IllegalStateException( "No current row" );
                    }
                    else {
                        Object[] row = new Object[ ncol ];
                        for ( int i = 0; i < ncol; i++ ) {
                            row[ i ] = getPackagedCell( rset, i );
                        }
                        return row;
                    }
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
        return connx.getConnection();
    }

    /**
     * Returns the text of the SQL query used for this table.
     *
     * @return   the SQL query text
     */
    public String getSql() {
        return sql;
    }

    /**
     * Returns the object at a given column in the current row of a ResultSet,
     * in a form suitable for use as the content of a StarTable cell.
     *
     * @param  rset  the result set
     * @param  icol  the column to use (first column is 0)
     * @return the   cell value in an exportable form
     */
    private Object getPackagedCell( ResultSet rset, int icol )
            throws SQLException {
        Object base = rset.getObject( icol + 1 );
        Class colclass = getColumnInfo( icol ).getContentClass();
        if ( base instanceof byte[] && 
             ! colclass.equals( byte[].class ) ) {
            return new String( (byte[]) base );
        }
        else if ( base instanceof char[] && 
                 ! colclass.equals( char[].class ) ) {
            return new String( (char[]) base );
        }
        return base;
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
     * @param  rset  a new ResultSet to check for consistency with this object
     * @throws IllegalStateException  in case of inconsistency
     */
    private void checkConsistent( ResultSet rset ) throws SQLException {
        ResultSetMetaData rsmeta1 = rset.getMetaData();
        if ( rsmeta1.getColumnCount() != ncol ) {
            throw new IllegalStateException( 
                "ResultSet column count has changed" );
        }
    }


}
