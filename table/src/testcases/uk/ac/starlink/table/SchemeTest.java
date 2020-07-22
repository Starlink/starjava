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
        assertEquals(
            new HashSet<String>( Arrays.asList( "jdbc", "class", "loop" ) ),
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

    public void testLoop() throws IOException {
        StarTableFactory tfact = new StarTableFactory();
        StarTable tloop = tfact.makeStarTable( ":loop:10" );
        failCreateTable( tfact, ":loop:" );
        failCreateTable( tfact, ":loop:a,b,c" );
        failCreateTable( tfact, ":loop:one" );
        StarTable t1 = tfact.makeStarTable( ":loop:10" );
        StarTable t2 = tfact.makeStarTable( ":loop:1000,1010" );
        StarTable t3 = tfact.makeStarTable( ":loop:0,100,10" );
        for ( int i = 0; i < 10; i++ ) {
            assertEquals( i, t1.getCell( i, 0 ) );
            assertEquals( i + 1000, t2.getCell( i, 0 ) );
            assertEquals( i * 10, t3.getCell( i, 0 ) );
        }
        tfact.getSchemes().remove( "loop" );
        failCreateTable( tfact, ":loop:10" );
    }

    public void testClazz() throws IOException {
        StarTableFactory tfact = new StarTableFactory();
        failCreateTable( tfact, ":class:java.lang.String:10" );
        failCreateTable( tfact,
                         ":class:uk.ac.starlink.table.LoopTableScheme:do_what");
        String l10spec = ":class:uk.ac.starlink.table.LoopTableScheme:10";
        StarTable tcloop = tfact.makeStarTable( l10spec );
        assertEquals( 1, tcloop.getColumnCount() );
        assertEquals( 10, tcloop.getRowCount() );
        tfact.getSchemes().remove( "class" );
        failCreateTable( tfact, l10spec );
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
