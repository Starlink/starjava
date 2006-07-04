package uk.ac.starlink.table;

import java.io.IOException;
import uk.ac.starlink.util.TestCase;

public class ConcatTableTest extends TestCase {

    public ConcatTableTest( String name ) {
        super( name );
    }

    public void testConcatTable() throws IOException {
        ColumnInfo c0 = new ColumnInfo( "A", Integer.class, null );
        ColumnInfo c1n = new ColumnInfo( "B", Number.class, null );
        ColumnInfo c1f = new ColumnInfo( "B", Float.class, null );
        ColumnInfo c2 = new ColumnInfo( "C", String.class, null );
        Object[] row0 = new Object[] { new Integer( 23 ), new Double( Math.E ),
                                      "Knickers" };
        Object[] row1 = new Object[] { new Integer( 24 ), "Bums" };
        Object[] row2 = new Object[] { new Integer( 25 ), new Float( Math.PI ),
                                       "Boobs" };
        StarTable t0 = new ConstantStarTable( new ColumnInfo[] { c0, c1n, c2 },
                                              row0, 100 );
        StarTable t1 = new ConstantStarTable( new ColumnInfo[] { c0, c2 },
                                              row1, 100 );
        StarTable t2 = new ConstantStarTable( new ColumnInfo[] { c0, c1f, c2 },
                                              row2, 100 );
        StarTable ct = new ConcatStarTable( new StarTable[] { t0, t1, t2 } );
        assertEquals( "A", ct.getColumnInfo( 0 ).getName() );
        assertEquals( "B", ct.getColumnInfo( 1 ).getName() );
        assertEquals( "C", ct.getColumnInfo( 2 ).getName() );
        assertEquals( Integer.class, ct.getColumnInfo( 0 ).getContentClass() );
        assertEquals( Number.class, ct.getColumnInfo( 1 ).getContentClass() );
        assertEquals( String.class, ct.getColumnInfo( 2 ).getContentClass() );

        assertEquals( 300L, ct.getRowCount() );

        assertArrayEquals( row0, ct.getRow( 50 ) );
        assertArrayEquals( new Object[] { new Integer( 24 ), null, "Bums" },
                           ct.getRow( 150 ) );
        assertArrayEquals( row2, ct.getRow( 250 ) );

        Tables.checkTable( ct );
    }
}
