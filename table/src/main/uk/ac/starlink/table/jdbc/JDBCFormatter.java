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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private final int maxColLeng_;
    private final int maxTableLeng_;
    private final StarTable table_;
    private final SqlColumn[] sqlCols_;
    private final int[] sqlTypes_;
    private final Map<Integer,String> typeNameMap_;
    private final SqlSyntax sqlSyntax_;
    private final boolean upperCasePreferred_;

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
        maxColLeng_ = meta.getMaxColumnNameLength();
        maxTableLeng_ = meta.getMaxTableNameLength();
        upperCasePreferred_ = meta.storesUpperCaseIdentifiers();
        sqlSyntax_ = getSqlSyntax( meta );

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
        Set<String> cnames = new HashSet<String>();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo col = table_.getColumnInfo( icol );
            String colName = fixName( col.getName(), maxColLeng_, "column" );

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

                    /* Pin the minimum size to 1, since at least some
                     * RDBMS (Postgres) don't like VARCHAR(0). */
                    int charSize = Math.max( charSizes[ icol ], 1 );
                    tSpec += "(" + charSize + ")";
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
           .append( defensiveQuoteTable( tableName ) )
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
                   .append( defensiveQuoteColumn( sqlCol.getColumnName() ) )
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
           .append( defensiveQuoteTable( tableName ) )
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
            tableName = fixName( tableName, maxTableLeng_, "table" );
            try {
                String cmd = "DROP TABLE " + defensiveQuoteTable( tableName );
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
            tableName = fixName( tableName, maxTableLeng_, "table" );
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
    public int getSqlType( Class<?> clazz ) {
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
        return typeNameMap_.get( new Integer( sqlType ) );
    }

    /**
     * Returns a mapping of Type id to SQL type name.  The map key is
     * an Integer object with the value of the corresponding 
     * {@link java.sql.Types} constant and the value is a string which
     * <code>conn</code> will understand.
     *
     * @param  conn  the connection to work out the mapping for
     * @return   a new type id-&gt;name mapping for <code>conn</code>
     */
    private static Map<Integer,String> makeTypesMap( Connection conn )
            throws SQLException {
        Map<Integer,String> types = new HashMap<Integer,String>();
        ResultSet typeInfos = conn.getMetaData().getTypeInfo();
        while ( typeInfos.next() ) {
            String name = typeInfos.getString( "TYPE_NAME" );
            int id = (int) typeInfos.getShort( "DATA_TYPE" );
            Integer key = new Integer( id );
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
     * Doctors a type map by adding an entry for a given type <code>req</code> 
     * with a copy of an existing one <code>fallback</code>, if the map
     * doesn't contain <code>req</code> in the first place.
     *
     * @param  types  type -> name mapping
     * @param  req   required type code
     * @param  fallback  fallback type code
     */
    private static <V> void setTypeFallback( Map<Integer,V> types,
                                             int req, int fallback ) {
        Integer reqKey = new Integer( req );
        Integer fallbackKey = new Integer( fallback );
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
     * @param  idType   type of identifier being quoted, used in log messages
     * @return   fixed column name (may be the same as <code>name</code>)
     */
    private String fixName( String name, int maxLeng, String idtype ) {
        final String name0 = name;

        /* Escape special characters, replacing them with an underscore. */
        name = name.replaceAll( "\\W+", "_" );

        /* Trim extra-long column names. */
        if ( maxLeng > 0 && name.length() >= maxLeng ) {
            name = name.substring( 0, maxColLeng_ - 4 );
        }

        /* Replace reserved words. */
        if ( sqlSyntax_.isReserved( name ) ) {
            name += "_";
        }

        /* Report any identifier changes. */
        if ( ! name0.equals( name ) ) {
            logger.warning( "Renamed " + idtype + '"' + name0 + '"' + " to "
                          + '"' + name + '"' + " (SQL syntax)" );
        }
        assert sqlSyntax_.isIdentifier( name );
        assert ! sqlSyntax_.isReserved( name );
        return name;
    }

    /**
     * Perform quoting of an identifier which is intended to defend against
     * the situation in which that identifier is unexpectedly a reserved word.
     * The intention is that the quoting works as though it's not there for
     * unreserved names, though this is not easy to achieve.
     * Because of the quoting semantics (delimited identifier) in SQL92,
     * and its abuse by RDBMSs in practice, the most harmless way to do
     * this seems to be to fold the case to that preferred by the DB and
     * then quote it using the DB's favoured quote characters.
     * I've taken advice on this from Markus Demleitner, but the whole thing
     * seems like a bit of a minefield, so there may be cases where this
     * does not do what's required.
     * An alternative implementation that might work better for some purposes
     * would be to return the argument unchanged.
     *
     * @param  name   identifier to quote
     * @return  identifier possibly defensively quoted in some way
     */
    private String defensiveQuote( String name ) {
        return sqlSyntax_.quote( upperCasePreferred_ ? name.toUpperCase()
                                                     : name.toLowerCase() );
    }

    /**
     * Defensively quotes a column name.
     *
     * @param  name   identifier to quote
     * @return  identifier possibly defensively quoted in some way
     */
    private String defensiveQuoteColumn( String name ) {
        return defensiveQuote( name );
    }

    /**
     * Defensively quotes a table name.
     *
     * <p>The current implementation is a no-op.
     * On MySQL at least, quoting table names seems more problematic than
     * quoting column names, for instance
     * <blockquote>
     *    CREATE TABLE `animals` (`legs` INTEGER)
     *    mysql> select LEGS from animals;
     *    Empty set (0.00 sec)
     *    mysql> select legs from ANIMALS;
     *    ERROR 1146 (42S02): Table 'test.ANIMALS' doesn't exist
     * </blockquote>
     * And table names are less likely to result in surprising name clashes
     * than column names (at least, there are fewer of them).
     * So don't quote them, and cross fingers.
     *
     * @param  name   identifier to quote
     * @return  identifier possibly defensively quoted in some way
     */
    private String defensiveQuoteTable( String name ) {
        return name;
    }

    /**
     * Returns an SqlSyntax object for a given database connection.
     * If something goes wrong, it returns one with default characteristics.
     *
     * @param  meta   db metadata
     * @return   syntax object
     */
    private static SqlSyntax getSqlSyntax( DatabaseMetaData meta ) {

        /* Assemble a list of reserved words.  In principle it should only
         * be necessary to use the SQL92 set and the result of calling
         * meta.getSQLKeywords() on the JDBC driver metadata object.
         * But in practice this seems to miss some (e.g. my first try with
         * mysql-connector-java-5.0.4 did not report the word "INDEX").
         * So, be paranoid and quote things.  It's not very satisfactory, 
         * but it seems pretty hard to do this in a way which is robust for
         * all or most RDBMSs. */
        Collection<String> words = new HashSet<String>();
        words.addAll( Arrays.asList( SqlSyntax.getParanoidReservedWords() ) );
        char quoteChar = '"';
        try {
            String quote = meta.getIdentifierQuoteString();
            quoteChar = quote.length() == 1 ? quote.charAt( 0 ) : ' ';
            words.addAll( getWords( meta.getSQLKeywords() ) );

            /* There are also a bunch of other get*Functions methods on the
             * database that we could throw in here.  However, (on advice)
             * I don't believe these sit in the same namespace as the table
             * or column names, so it shouln't be necessary to add them. */
        }
        catch ( Exception e ) {
            logger.warning( "Some problem determining SQL syntax: " + e );
            logger.warning( "Use default SQL syntax" );
        }
        return new SqlSyntax( words.toArray( new String[ 0 ] ),
                              SqlSyntax.SQL92_IDENTIFIER_REGEX, quoteChar );
    }

    /**
     * Splits a comma-separated string into individual words.
     *
     * @param  commaList  comma-separated string
     * @return  list of words
     */
    private static final List<String> getWords( String commaList ) {
        return commaList == null || commaList.trim().length() == 0
             ? new ArrayList<String>()
             : Arrays.asList( commaList.split( ", *" ) );
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
