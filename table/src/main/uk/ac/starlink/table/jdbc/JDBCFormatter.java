package uk.ac.starlink.table.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.util.Loader;

public class JDBCFormatter {

    private Connection conn;
    private static Logger logger = 
        Logger.getLogger( "uk.ac.starlink.table.jdbc" );

    public JDBCFormatter( Connection conn ) {
        this.conn = conn;
    }

    public void createJDBCTable( StarTable table, String tableName )
            throws IOException, SQLException {

        /* Get an iterator over the table rows. */
        RowSequence rseq = table.getRowSequence();
 
        /* Table deletion. */
        Statement stmt = conn.createStatement();
        try {
            stmt.executeUpdate( "DELETE FROM " + tableName );
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
        int[] sqlTypes = new int[ ncol ];
        boolean first = true;
        Set cnames = new HashSet();
        for ( int icol = 0; icol < ncol; icol++ ) {
            ColumnInfo col = table.getColumnInfo( icol );
            String colName = col.getName();

            /* Massage the column name to make sure it is in a sensible
             * format. */
            colName = colName.replaceAll( "[\\s\\.\\(\\)\\[\\]\\-\\+]+", "_" );
            if ( colName.length() > 64 ) {
                colName = colName.substring( 0, 60 );
            }

            /* Check that we don't have a duplicate column name. */
            while ( cnames.contains( colName ) ) {
                colName = colName + "_" + ( icol + 1 );
            }
            cnames.add( colName );

            /* Add the column name to the statement string. */
            sqlTypes[ icol ] = getSqlType( col.getContentClass() );
            if ( sqlTypes[ icol ] != Types.NULL ) {
                if ( ! first ) {
                    cmd.append( ',' );
                }
                first = false;
                cmd.append( ' ' )
               .append( colName )
               .append( ' ' )
               .append( typeName( sqlTypes[ icol ] ) );
            }
        }
        cmd.append( " )" );

        /* Create the table. */
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
        while ( rseq.hasNext() ) {
            rseq.next();
            Object[] row = rseq.getRow();
            int pix = 0;
            for ( int icol = 0; icol < ncol; icol++ ) {
                if ( sqlTypes[ icol ] != Types.NULL ) {
                    pix++;
                    // pstmt.setObject( pix, row[ icol ], sqlTypes[ icol ] );
                    pstmt.setObject( pix, row[ icol ] );
                }
            }
            pstmt.executeUpdate();
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
            return Types.BLOB;
        }
        else {
            return Types.BLOB;
        }
    }

    public static String typeName( int sqlType ) {
        switch ( sqlType ) {
            case Types.NULL:        return "NULL";
            case Types.TINYINT:     return "TINYINT";
            case Types.SMALLINT:    return "SMALLINT";
            case Types.INTEGER:     return "INTEGER";
            case Types.BIGINT:      return "BIGINT";
            case Types.FLOAT:       return "FLOAT";
            case Types.DOUBLE:      return "DOUBLE";
            case Types.BIT:         return "BIT";
            case Types.CHAR:        return "CHAR";
            case Types.LONGVARCHAR: return "LONGVARCHAR";
            case Types.BLOB:        return "BLOB";
            default:                return "unknown-type";
        }
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
        StarTable intab = new StarTableFactory().makeStarTable( inTable );
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
