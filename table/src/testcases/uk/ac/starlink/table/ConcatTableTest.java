package uk.ac.starlink.table;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import uk.ac.starlink.util.LogUtils;

public class ConcatTableTest extends TableCase {

    public ConcatTableTest( String name ) {
        super( name );
        LogUtils.getLogger( "uk.ac.starlink.table" ).setLevel( Level.SEVERE );
    }

    public void testDiverse() throws IOException {
        ColumnInfo c0 = new ColumnInfo( "A", Integer.class, null );
        ColumnInfo c1n = new ColumnInfo( "B", Number.class, null );
        ColumnInfo c1f = new ColumnInfo( "B", Float.class, null );
        ColumnInfo c2 = new ColumnInfo( "C", String.class, null );
        Object[] row0 = { Integer.valueOf( 23 ), Double.valueOf( Math.E ),
                          "Knickers" };
        Object[] row1 = { Integer.valueOf( 24 ), "Bums" };
        Object[] row2 = { Integer.valueOf( 25 ),
                          Float.valueOf( (float) Math.PI ), "Boobs" };
        StarTable t0 = new ConstantStarTable( new ColumnInfo[] { c0, c1n, c2 },
                                              row0, 100 );
        StarTable t1 = new ConstantStarTable( new ColumnInfo[] { c0, c2 },
                                              row1, 100 );
        StarTable t2 = new ConstantStarTable( new ColumnInfo[] { c0, c1f, c2 },
                                              row2, 100 );

        StarTable[] tables = new StarTable[] { t0, t1, t2 };
        try {
            StarTable ct = new ConcatStarTable( t0, tables );
            fail();
        }
        catch ( IOException e ) {
        }

        StarTable ct =
            new ConcatStarTable( t0, Arrays.asList( tables ).iterator() ) ;
        checkStarTable( ct );
        ct = Tables.randomTable( ct );

        assertEquals( "A", ct.getColumnInfo( 0 ).getName() );
        assertEquals( "B", ct.getColumnInfo( 1 ).getName() );
        assertEquals( "C", ct.getColumnInfo( 2 ).getName() );
        assertEquals( Integer.class, ct.getColumnInfo( 0 ).getContentClass() );
        assertEquals( Number.class, ct.getColumnInfo( 1 ).getContentClass() );
        assertEquals( String.class, ct.getColumnInfo( 2 ).getContentClass() );

        assertArrayEquals( row0, ct.getRow( 50 ) );
        assertArrayEquals( row2, ct.getRow( 150 ) );

        assertEquals( 200L, ct.getRowCount() );
    }

    public void testSame() throws IOException {
        ColumnInfo[] infos = new ColumnInfo[] {
            new ColumnInfo( "A", Double.class, null ),
            new ColumnInfo( "B", Double.class, null ),
        };
        Object[] row = { Double.valueOf( 5 ), Double.valueOf( 10 ) };
        StarTable t1 = new ConstantStarTable( infos, row, 10 );
        StarTable[] items = new StarTable[ 10 ];
        Arrays.fill( items, t1 );
        StarTable ct = new ConcatStarTable( items[ 0 ], items );
        checkStarTable( t1 );
        checkStarTable( ct );
        checkStarTable( new ConcatStarTable( items[ 0 ], items ) {
            @Override
            public boolean isRandom() {
                return false;
            }
        } );
        assertArrayEquals( row, t1.getRow( 5 ) );
        assertEquals( 100L, ct.getRowCount() );
        for ( int i = 0; i < ct.getRowCount(); i++ ) {
            assertArrayEquals( row, ct.getRow( i ) );
        }
    }

}
