package uk.ac.starlink.table;

import java.io.IOException;
import uk.ac.starlink.util.TestCase;

public class ConstantTableTest extends TestCase {

    public ConstantTableTest( String name ) {
        super( name );
    }

    public void testTable() throws IOException {
        ColumnInfo c1 = new ColumnInfo( "integer", Integer.class, null );
        ColumnInfo c2 = new ColumnInfo( "double_precision", Double.class,
                                        null );
        ColumnInfo c3 = new ColumnInfo( "char_string", String.class, null );
        ColumnInfo[] infos = new ColumnInfo[] { c1, c2, c3 };
        Object[] row = { Integer.valueOf( 23 ), Double.valueOf( Math.E ),
                         "Knickers" };
        StarTable table = new ConstantStarTable( infos, row, 101 );
        Tables.checkTable( table );
        assertEquals( 101L, table.getRowCount() );
        assertEquals( 3, table.getColumnCount() );
        assertEquals( "Knickers", table.getCell( 25L, 2 ) );
        assertEquals( "double_precision", table.getColumnInfo( 1 ).getName() );
        assertArrayEquals( row, table.getRow( 45 ) );
    }
}
