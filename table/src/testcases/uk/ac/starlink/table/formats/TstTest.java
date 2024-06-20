package uk.ac.starlink.table.formats;

import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import uk.ac.starlink.table.ColumnPermutedStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.LogUtils;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLDataSource;

public class TstTest extends TestCase {

    private static final ValueInfo DUMMY_PARAM = 
        new DefaultValueInfo( "dummy", String.class, "Testing only" );
    public TstTest( String name ) {
        super( name );
        LogUtils.getLogger( "uk.ac.starlink.table.storage" )
                .setLevel( Level.WARNING );
    }

    public void testSimple() throws Exception {
        StarTable simple = readTst( "simple.TAB" );
        assertEquals( 4, simple.getRowCount() );
        assertEquals( 6, simple.getColumnCount() );
        assertEquals( "meta.id", simple.getColumnInfo( 0 ).getUCD() );
        assertEquals( "pos.eq.ra", simple.getColumnInfo( 1 ).getUCD() );
        assertEquals( "pos.eq.dec", simple.getColumnInfo( 2 ).getUCD() );
        assertEquals( "J2000.0",
                      simple.getParameterByName( "EQUINOX" ).getValue() );
        assertEquals( "J1996.35",
                      simple.getParameterByName( "EPOCH" ).getValue() );
        assertEquals( "Simple TST example; stellar photometry catalogue.",
                      simple.getName() );
        assertEquals( "hms", simple.getColumnInfo( 1 ).getUnitString() );
        assertEquals( "dms", simple.getColumnInfo( 2 ).getUnitString() );
        assertEquals( "U_B", simple.getColumnInfo( 5 ).getName() );
        assertEquals( "B_V", simple.getColumnInfo( 4 ).getName() );
        assertArrayEquals(
            Tables.randomTable( simple ).getRow( 1 ),
            new Object[] { "Obj. 2", " 5:07:50.9", " -5:05:11",
                           Float.valueOf( 2.79f ), Float.valueOf( 0.13f ),
                           Float.valueOf( 0.10f ) } );
        assertTrue( ((String)
                     simple.getParameterByName( TstStarTable.DESCRIPTION_INFO
                                                            .getName() )
                           .getValue())
                   .indexOf( "Pumpkin" ) > 0 );
    }

    public void testSex() throws Exception {
        StarTable sex = readTst( "sextractor.TAB" );
        assertEquals( 20, sex.getRowCount() );
        assertEquals( 16, sex.getColumnCount() );
        assertEquals( "SExCat",
                      sex.getParameterByName( "short_name" ).getValue() );
    }

    public void testOut() throws Exception {
        StarTable simple = readTst( "simple.TAB" );
        StarTable permut = 
            new ColumnPermutedStarTable( simple, new int[] { 5, 4, 3, 2, 1 } );
        permut.setParameter( new DescribedValue( DUMMY_PARAM, "Joselin" ) );
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        new TstTableWriter().writeStarTable( permut, bout );
        bout.close();
        String ttxt = new String( bout.toByteArray() );
        assertTrue( ttxt.indexOf( "\nid_col: 5\n" ) > 0 );
        assertTrue( ttxt.indexOf( "\nra_col: 4\n" ) > 0 );
        assertTrue( ttxt.indexOf( "\ndec_col: 3\n" ) > 0 );
        assertTrue( ttxt.indexOf( "\ndummy: Joselin\n" ) > 0 ); 
    }

    private static TstStarTable readTst( String name ) throws Exception {
        return (TstStarTable)
               new TstTableBuilder()
              .makeStarTable( new URLDataSource( TstTest.class
                                                .getResource( name ) ),
                              false, StoragePolicy.getDefaultPolicy() );
    }

}
