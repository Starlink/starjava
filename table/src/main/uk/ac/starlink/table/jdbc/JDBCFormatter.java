package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.Loader;

/**
 * Handles conversion of a StarTable into a new table in an RDBMS.
 *
 * @author   Mark Taylor
 */
public class JDBCFormatter {

    private final Connection conn_;
    private final String quote_;
    private final int maxColLeng_;
    private final int maxTableLeng_;
    private final StarTable table_;
    private final SqlColumn[] sqlCols_;
    private final int[] sqlTypes_;
    private final Map typeNameMap_;

    private static Logger logger = 
        Logger.getLogger( "uk.ac.starlink.table.jdbc" );

    /**
     * Constructor.
     *
     * @param  conn  JDBC connection
     * @param  table   input table
     */
    public JDBCFormatter( Connection conn, StarTable table )
            throws SQLException, IOException {
        conn_ = conn;
        table_ = table;
        typeNameMap_ = makeTypesMap( conn_ );
        DatabaseMetaData meta = conn_.getMetaData();
        quote_ = meta.getIdentifierQuoteString();
        maxColLeng_ = meta.getMaxColumnNameLength();
        maxTableLeng_ = meta.getMaxTableNameLength();

        /* Work out column types and see if we need to work out maximum string
         * lengths. */
        int ncol = table_.getColumnCount();
        boolean[] charType = new boolean[ ncol ];
        sqlTypes_ = new int[ ncol ];
        int[] charSizes = new int[ ncol ];
        boolean[] needSizes = new boolean[ ncol ];
        boolean needSomeSizes = false;
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo colInfo = table_.getColumnInfo( icol );
            sqlTypes_[ icol ] = getSqlType( colInfo.getContentClass() );
            if ( sqlTypes_[ icol ] == Types.VARCHAR ) {
                int leng = colInfo.getElementSize();
                if ( leng > 0 ) {
                    charSizes[ icol ] = leng;
                }
                else {
                    needSizes[ icol ] = true;
                    needSomeSizes = true;
                }
            }
        }

        /* Work out maximum string lengths if necessary. */
        if ( needSomeSizes ) {
            RowSequence rseq = table_.getRowSequence();
            try {
                while ( rseq.next() ) {
                    for ( int icol = 0; icol < ncol; icol++ ) {
                        if ( needSizes[ icol ] ) {
                            Object val = rseq.getCell( icol );
                            if ( val != null ) {
                                charSizes[ icol ] = 
                                    Math.max( charSizes[ icol ],
                                              val.toString().length() );
                            }
                        }
                    }
                }
            }
            finally {
                rseq.close();
            }
        }

        /* Work out and store column specifications. */
        sqlCols_ = new SqlColumn[ ncol ];
        Set cnames = new HashSet();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo col = table_.getColumnInfo( icol );
            String colName = fixName( col.getName(), maxColLeng_ );

            /* Check that we don't have a duplicate column name. */
            while ( cnames.contains( colName ) ) {
                colName = colName + "_" + ( icol + 1 );
            }
            cnames.add( colName );

            /* Add the column name to the statement string. */
            int sqlType = sqlTypes_[ icol ];
            String tSpec = typeName( sqlType );
            if ( tSpec == null ) {
                logger.warning( "Can't write column " + colName + " type " 
                              + col.getClass() );
            }
            else {
                if ( sqlType == Types.VARCHAR ) {
                    tSpec += "(" + charSizes[ icol ] + ")";
                }
                sqlCols_[ icol ] = new SqlColumn( sqlType, colName, tSpec );
            }
        }
    }

    /**
     * Returns the text of a suitable CREATE TABLE statement.
     *
     * @param  tableName   name of the new SQL table
     */
    public String getCreateStatement( String tableName ) {
        StringBuffer sql = new StringBuffer();
        sql.append( "CREATE TABLE " )
           .append( quote_ )
           .append( fixName( tableName, maxTableLeng_ ) )
           .append( quote_ )
           .append( " (" );
        int ncol = sqlCols_.length;
        boolean first = true;
        for ( int icol = 0; icol < ncol; icol++ ) {
            SqlColumn sqlCol = sqlCols_[ icol ];
            if ( sqlCol != null ) {
                if ( ! first ) {
                    sql.append( ',' );
                }
                first = false;
                sql.append( ' ' )
                   .append( quote_ )
                   .append( sqlCol.getColumnName() )
                   .append( quote_ )
                   .append( ' ' )
                   .append( sqlCol.getTypeSpec() );
            }
        }
        sql.append( ')' );
        return sql.toString();
    }

    /**
     * Returns the text of a suitable parametric statement for inserting a
     * row.  Data placeholders for writable columns will be represented
     * by '?' characters.
     *
     * @param   tableName  name SQL table for insertion
     */
    public String getInsertStatement( String tableName ) {
        StringBuffer sql = new StringBuffer();
        sql.append( "INSERT INTO " )
           .append( quote_ )
           .append( fixName( tableName, maxTableLeng_ ) )
           .append( quote_ )
           .append( " VALUES(" );
        boolean first = true;
        int ncol = sqlCols_.length;
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( sqlCols_[ icol ] != null ) {
                if ( ! first ) {
                    sql.append( ',' );
                }
                first = false;
                sql.append( ' ' ) 
                   .append( '?' );
            }
        }
        sql.append( " )" );
        return sql.toString();
    }

    /**
     * Writes data from this formatter's input table into the database.
     * This method is somewhat misnamed - depending on the write mode, 
     * a new table may or may not be created in the database.
     *
     * @param   tableName  name of the new table to write to in the database
     * @param   mode   mode for writing records
     */
    public void createJDBCTable( String tableName, WriteMode mode )
            throws IOException, SQLException {
        Statement stmt = conn_.createStatement();
 
        /* Table deletion. */
        if ( mode.getAttemptDrop() ) {
            try {
                String cmd = "DROP TABLE "
                           + quote_
                           + fixName( tableName, maxTableLeng_ )
                           + quote_;
                logger.info( cmd );
                stmt.executeUpdate( cmd );
                logger.warning( "Dropped existing table " + tableName + 
                                " to write new one" );
            }
            catch ( SQLException e ) {
                // no action - might not be there
            }
        }

        /* Table creation. */
        if ( mode.getCreate() ) {
            String create = getCreateStatement( tableName );
            logger.info( create );
            stmt.executeUpdate( create );
        }

        /* Prepare a statement for adding the data. */
        String insert = getInsertStatement( tableName );
        logger.info( insert );
        PreparedStatement pstmt = conn_.prepareStatement( insert );

        /* Add the data. */
        int ncol = sqlCols_.length;
        RowSequence rseq = table_.getRowSequence();
        try {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                int pix = 0;
                for ( int icol = 0; icol < ncol; icol++ ) {
                    if ( sqlCols_[ icol ] != null ) {
                        pix++;
                        Object val = row[ icol ];
                        if ( Tables.isBlank( val ) ) {
                            pstmt.setNull( pix, sqlTypes_[ icol ] );
                        }
                        else {
                            pstmt.setObject( pix, row[ icol ],
                                             sqlTypes_[ icol ] );
                        }
                    }
                }
                pstmt.executeUpdate();
            }
        }
        finally {
            rseq.close();
        }
    }

    /**
     * Returns the SqlColumn object describing how a given column of this
     * formatter's input table will be written into the RDBMS.
     * If the value for a given column is <code>null</code>, it means that
     * column cannot, and will not, be written.
     *
     * @param  icol   column index in input table
     * @return   SQL column description
     */
    public SqlColumn getColumn( int icol ) {
        return sqlCols_[ icol ];
    }

    /**
     * Returns an SQL type code suitable for a given class.
     *
     * @param  clazz   java class of data
     * @return   one of the {@link java.sql.Types} codes
     */
    public int getSqlType( Class clazz ) {
        if ( clazz.equals( Byte.class ) ) {
            return Types.TINYINT;
        }
        else if ( clazz.equals( Short.class ) ) {
            return Types.SMALLINT;
        }
        else if ( clazz.equals( Integer.class ) ) {
            return Types.INTEGER;
        }
        else if ( clazz.equals( Long.class ) ) {
            return Types.BIGINT;
        }
        else if ( clazz.equals( Float.class ) ) {
            return Types.FLOAT;
        }
        else if ( clazz.equals( Double.class ) ) {
            return Types.DOUBLE;
        }
        else if ( clazz.equals( Boolean.class ) ) {
            return Types.BIT;
        }
        else if ( clazz.equals( Character.class ) ) {
            return Types.CHAR;
        }
        else if ( clazz.equals( String.class ) ) {
            return Types.VARCHAR;
        }
        else {
            return Types.BLOB;
        }
    }

    /**
     * Returns the name used by the connection's database to reference a 
     * JDBC type.
     * 
     * @param  sqlType  type id (as per {@link java.sql.Types})
     * @return  connection-specific type name
     */
    public String typeName( int sqlType ) throws SQLException {
        Object key = new Integer( sqlType );
        return typeNameMap_.containsKey( key )
             ? (String) typeNameMap_.get( key )
             : null;
    }

    /**
     * Returns a mapping of Type id to SQL type name.  The map key is
     * an Integer object with the value of the corresponding 
     * {@link java.sql.Types} constant and the value is a string which
     * <tt>conn</tt> will understand.
     *
     * @param  conn  the connection to work out the mapping for
     * @return   a new type id-&gt;name mapping for <tt>conn</tt>
     */
    private static Map makeTypesMap( Connection conn ) throws SQLException {
        Map types = new HashMap();
        ResultSet typeInfos = conn.getMetaData().getTypeInfo();
        while ( typeInfos.next() ) {
            String name = typeInfos.getString( "TYPE_NAME" );
            int id = (int) typeInfos.getShort( "DATA_TYPE" );
            Object key = new Integer( id );
            if ( ! types.containsKey( key ) ) {
                types.put( key, name );
            }
        }
        typeInfos.close();
        if ( ! types.containsKey( new Integer( Types.NULL ) ) ) {
            types.put( new Integer( Types.NULL ), "NULL" );
        }

        /* Hack for PostgreSQL for which the above procedure results in
         * "text" instead of "varchar" for the Types.VARCHAR type
         * (driver is at fault I'd say, should report varchar before text). */
        types.put( new Integer( Types.VARCHAR ), "VARCHAR" );

        /* Hack for DBMSs which don't return the right types in other ways. */
        setTypeFallback( types, Types.FLOAT, Types.REAL );
        setTypeFallback( types, Types.REAL, Types.FLOAT );
        setTypeFallback( types, Types.FLOAT, Types.DOUBLE );
        setTypeFallback( types, Types.DOUBLE, Types.FLOAT );
        setTypeFallback( types, Types.SMALLINT, Types.INTEGER );
        setTypeFallback( types, Types.TINYINT, Types.SMALLINT );
        setTypeFallback( types, Types.BIGINT, Types.INTEGER );
        return types;
    }

    /**
     * Doctors a type map by adding an entry for a given type <tt>req</tt> 
     * with a copy of an existing one <tt>fallback</tt>, if the map
     * doesn't contain <tt>req</tt> in the first place.
     *
     * @param  types  type -> name mapping
     * @param  req   required type code
     * @param  fallback  fallback type code
     */
    private static void setTypeFallback( Map types, int req, int fallback ) {
        Object reqKey = new Integer( req );
        Object fallbackKey = new Integer( fallback );
        if ( ! types.containsKey( reqKey ) &&
             types.containsKey( fallbackKey ) ) {
            types.put( reqKey, types.get( fallbackKey ) );
        }
    }

    /**
     * Massages a column or table name to make it acceptable for SQL.
     *
     * @param  name  initial column name
     * @param  maxLeng  maximum name length; 0 means no limit
     * @return   fixed column name (may be the same as <tt>name</tt>)
     */
    private String fixName( String name, int maxLeng ) {

        /* Escape special characters, replacing them with an underscore. */
        name = name.replaceAll( "\\W+", "_" );

        /* Trim extra-long column names. */
        if ( maxLeng > 0 && name.length() >= maxLeng ) {
            name = name.substring( 0, maxColLeng_ - 4 );
        }

        /* Replace reserved words.  This list is not complete. */
        if ( SqlReserved.isReserved( name ) ) {
            logger.info( "Renaming column " + name + " to " + name + 
                         "_ (SQL reserved word)" );
            name = name + "_";
            assert ! SqlReserved.isReserved( name );
        }
        return name;
    }

    /**
     * Main method.
     * Not really intended for use but may be helpful with debugging.
     */
    public static void main( String[] args ) throws IOException, SQLException {
        String usage = "\nUsage: JDBCFormatter" 
                     + " intable"
                     + " jdbcURL"
                     + " tableName\n";
        if ( args.length != 3 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        Loader.loadProperties();
        String inTable = args[ 0 ];
        String jdbcUrl = args[ 1 ];
        String tableName = args[ 2 ];
        StarTable intab = new StarTableFactory( false )
                         .makeStarTable( inTable );
        try {
            Connection conn = DriverManager.getConnection( jdbcUrl );
            new JDBCFormatter( conn, intab )
               .createJDBCTable( tableName, WriteMode.CREATE );
            conn.close();
        }
        catch ( SQLException e ) {
            if ( e.getNextException() != null ) {
                System.err.println( "SQL exception chain: " );
                for ( SQLException nextEx = e; nextEx != null; 
                      nextEx = nextEx.getNextException() ) {
                    System.err.println( "   " + e );
                }
            }
            throw e;
        }
    }

    /**
     * Describes a column as it will be written to a table in an RDBMS.
     */
    public static class SqlColumn {
        private final int sqlType_;
        private final String colName_;
        private final String typeSpec_;

        /**
         * Constructor.
         */
        SqlColumn( int sqlType, String colName, String typeSpec ) {
            sqlType_ = sqlType;
            colName_ = colName;
            typeSpec_ = typeSpec;
        }

        /**
         * Returns the SQL type code for this column.
         *
         * @return  symbolic integer from {@link java.sql.Types}
         */
        public int getSqlType() {
            return sqlType_;
        }

        /**
         * Name used for the column.
         *
         * @return   column name
         */
        public String getColumnName() {
            return colName_;
        }

        /**
         * Type specification as used in CREATE statement. 
         *
         * @return  column type specification
         */
        public String getTypeSpec() {
            return typeSpec_;
        }
    }
}
