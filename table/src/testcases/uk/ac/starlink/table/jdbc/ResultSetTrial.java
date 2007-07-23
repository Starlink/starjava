package uk.ac.starlink.table.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableOutput;
import uk.ac.starlink.table.StarTableWriter;
import uk.ac.starlink.table.UnrepeatableSequenceException;

/**
 * This is not a JUnit unit test, since it's only going to work if a
 * suitable SQL database is avaialable, which won't be the case in most
 * development contexts.  It is run from its main() method.
 */
public class ResultSetTrial {

    public static void main( String[] args ) throws Exception {
        String url = "jdbc:mysql://localhost/astro1";
        String user = "mbt";
        String passwd = "";
        String sqlText = "SELECT mgc_id, ra2000, dec2000 FROM mgc LIMIT 8";
        StarTableWriter twriter = new StarTableOutput().getHandler( "text" );

        Connection conn = DriverManager.getConnection( url, user, passwd );

        { 
            Statement stmt = conn.createStatement();
            ResultSet rset = stmt.executeQuery( sqlText );
            StarTable table = new SequentialResultSetStarTable( rset );
            System.out.println( "Sequential 1:" );
            twriter.writeStarTable( table, System.out );
            System.out.println( "Sequential 2: (should fail)" );
            try {
                twriter.writeStarTable( table, System.out );
                System.out.println( "do what?" );
                System.exit( 1 );
            }
            catch ( UnrepeatableSequenceException e ) {
                System.out.println( "   failed: " + e.getMessage() );
            }
            stmt.close();
        }

        {
            Statement stmt =
                conn.createStatement( ResultSet.TYPE_SCROLL_INSENSITIVE,
                                      ResultSet.CONCUR_READ_ONLY );
            ResultSet rset = stmt.executeQuery( sqlText );
            StarTable table = new RandomResultSetStarTable( rset );
            System.out.println( "Random 1: " );
            twriter.writeStarTable( table, System.out );
            System.out.println( "Random 2: " );
            twriter.writeStarTable( table, System.out );
        }
    }
}
