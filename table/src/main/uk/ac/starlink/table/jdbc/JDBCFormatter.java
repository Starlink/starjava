package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.sql.Connection;
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

public class JDBCFormatter {

    private Connection conn;
    private Map typeNames;

    private static Logger logger = 
        Logger.getLogger( "uk.ac.starlink.table.jdbc" );

    public JDBCFormatter( Connection conn ) {
        this.conn = conn;
    }

    public void createJDBCTable( StarTable table, String tableName )
            throws IOException, SQLException {
 
        /* Table deletion. */
        Statement stmt = conn.createStatement();
        try {
            String cmd = "DROP TABLE " + tableName;
            logger.info( cmd );
            stmt.executeUpdate( cmd );
            logger.warning( "Dropped existing table " + tableName + 
                            " to write new one" );
        }
        catch ( SQLException e ) {
            // no action - might not be there
        }

        /* Table creation. */
        StringBuffer cmd = new StringBuffer();
        cmd.append( "CREATE TABLE " )
           .append( tableName )
           .append( " (" );

        /* Specify table columns. */
        int ncol = table.getColumnCount();
        boolean[] charType = new boolean[ ncol ];

        /* Work out column types and see if we need to work out maximum string
         * lengths. */
        int[] sqlTypes = new int[ ncol ];
        int[] charSizes = new int[ ncol ];
        boolean[] needSizes = new boolean[ ncol ];
        boolean needSomeSizes = false;
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo colInfo = table.getColumnInfo( icol );
            sqlTypes[ icol ] = getSqlType( colInfo.getContentClass() );
            if ( sqlTypes[ icol ] == Types.VARCHAR ) {
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
            RowSequence rseq = table.getRowSequence();
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

        /* Create a new SQL table with the right columns. */
        boolean first = true;
        Set cnames = new HashSet();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo col = table.getColumnInfo( icol );
            String colName = fixColumnName( col.getName() );

            /* Check that we don't have a duplicate column name. */
            while ( cnames.contains( colName ) ) {
                colName = colName + "_" + ( icol + 1 );
            }
            cnames.add( colName );

            /* Add the column name to the statement string. */
            int sqlType = sqlTypes[ icol ];
            String tName = typeName( sqlType );
            if ( tName == null ) {
                sqlType = Types.NULL;
                logger.warning( "Can't write column " + colName + " type " 
                              + col.getClass() );
            }
            if ( sqlType != Types.NULL ) {
                if ( ! first ) {
                    cmd.append( ',' );
                }
                first = false;
                cmd.append( ' ' )
               .append( colName )
               .append( ' ' )
               .append( tName );
                if ( sqlType == Types.VARCHAR ) {
                    cmd.append( '(' )
                       .append( charSizes[ icol ] )
                       .append( ')' );
                }
            }
        }
        cmd.append( " )" );

        /* Create the table. */
        logger.info( cmd.toString() );
        stmt.executeUpdate( cmd.toString() );

        /* Prepare a statement for adding the data. */
        cmd = new StringBuffer();
        cmd.append( "INSERT INTO " )
           .append( tableName )
           .append( " VALUES(" );
        first = true;
        for ( int icol = 0; icol < ncol; icol++ ) {
            if ( sqlTypes[ icol ] != Types.NULL ) {
                if ( ! first ) {
                    cmd.append( ',' );
                }
                first = false;
                cmd.append( ' ' ) 
                   .append( '?' );
            }
        }
        cmd.append( " )" );
        PreparedStatement pstmt = conn.prepareStatement( cmd.toString() );

        /* Add the data. */
        RowSequence rseq = table.getRowSequence();
        try {
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                int pix = 0;
                for ( int icol = 0; icol < ncol; icol++ ) {
                    if ( sqlTypes[ icol ] != Types.NULL ) {
                        pix++;
                        Object val = row[ icol ];
                        if ( Tables.isBlank( val ) ) {
                            pstmt.setObject( pix, null );
                        }
                        else {
                            // pstmt.setObject( pix, row[ icol ],
                            //                  sqlTypes[ icol ] );
                            pstmt.setObject( pix, row[ icol ] );
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
        if ( typeNames == null ) {
            typeNames = makeTypesMap( conn );
        }
        Object key = new Integer( sqlType );
        return typeNames.containsKey( key ) ? (String) typeNames.get( key )
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
     * Massages a column name to make it acceptable for SQL.
     *
     * @param  name  initial column name
     * @return   fixed column name (may be the same as <tt>name</tt>)
     */
    private static String fixColumnName( String name ) {

        /* Escape special characters, replacing them with an underscore. */
        name = name.replaceAll( "[\\s\\.\\(\\)\\[\\]\\-\\+]+", "_" );

        /* Trim extra-long column names. */
        if ( name.length() > 64 ) {
            name = name.substring( 0, 60 );
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
            new JDBCFormatter( conn ).createJDBCTable( intab, tableName );
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
}
