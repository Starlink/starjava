package uk.ac.starlink.table;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import junit.framework.TestCase;

public class SchemeTest extends TestCase {

    public void testSchemes() throws IOException {
        StarTableFactory tfact = new StarTableFactory();
        Map<String,TableScheme> schemes = tfact.getSchemes();
        assertEquals( new HashSet<String>( Arrays.asList( "jdbc" ) ),
                      schemes.keySet() );
        failCreateTable( tfact, ":num:10" );
        tfact.addScheme( new NumTableScheme() );
        failCreateTable( tfact, ":num:ten" );
        StarTable tnum = tfact.makeStarTable( ":num:10" );
        assertEquals( 1, tnum.getColumnCount() );
        for ( int i = 0; i < 10; i++ ) {
            assertEquals( i, tnum.getCell( i, 0 ) );
        }
        tfact.getSchemes().remove( "num" );
        failCreateTable( tfact, ":num:ten" );
    }

    private void failCreateTable( StarTableFactory tfact, String tspec ) {
        try {
            tfact.makeStarTable( tspec );
            fail();
        }
        catch ( IOException e ) {
            return;
        }
    }

    private static class NumTableScheme implements TableScheme {
        public String getSchemeName() {
            return "num";
        }
        public String getSchemeUsage() {
            return "<number>";
        }
        public String getExampleSpecification() {
            return null;
        }
        public StarTable createTable( String spec ) throws IOException {
            int num;
            try {
                num = Integer.parseInt( spec );
            }
            catch ( NumberFormatException e ) {
                throw new IOException( "Not a number: " + spec );
            }
            ColumnStarTable table = ColumnStarTable.makeTableWithRows( num );
            int[] data = new int[ num ];
            for ( int i = 0; i < num; i++ ) {
                data[ i ] = i;
            }
            table.addColumn( ArrayColumn.makeColumn( "data", data ) );
            return table;
        }
    }
}
