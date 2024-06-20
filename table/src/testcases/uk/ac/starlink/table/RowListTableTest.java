package uk.ac.starlink.table;

import java.io.IOException;
import uk.ac.starlink.util.TestCase;

public class RowListTableTest extends TestCase {

    AutoStarTable fixed;
    ColumnInfo[] infos;

    public RowListTableTest( String name ) {
        super( name );
        fixed = new AutoStarTable( 13 );
        fixed.addColumn( new ColumnInfo( "X", Integer.class, null ) );
        fixed.addColumn( new ColumnInfo( "Y", Float.class, null ) );
        fixed.addColumn( new ColumnInfo( "NAME", String.class, null ) );
        infos = Tables.getColumnInfos( fixed );
    }

    public void testTable() throws IOException {
        RowListStarTable rl = new RowListStarTable( infos );
        Tables.checkTable( fixed );
        Tables.checkTable( rl );
        assertEquals( fixed.getColumnCount(), rl.getColumnCount() );
        assertEquals( 0, rl.getRowCount() );
        for ( RowSequence rseq = fixed.getRowSequence(); rseq.next(); ) {
            rl.addRow( rseq.getRow() );
        }
        Tables.checkTable( rl );

        assertEquals( fixed.getColumnCount(), rl.getColumnCount() );
        assertEquals( fixed.getRowCount(), rl.getRowCount() );

        for ( long lrow = 0; lrow < fixed.getRowCount(); lrow++ ) {
            for ( int icol = 0; icol < fixed.getColumnCount(); icol++ ) {
                assertEquals( fixed.getCell( lrow, icol ), 
                              rl.getCell( lrow, icol ) );
            }
        }
 
        assertArrayEquals( fixed.getRow( 1 ), rl.getRow( 1 ) );
        assertArrayEquals( fixed.getRow( 4 ), rl.getRow( 4 ) );

        rl.removeRow( 3 );
        assertEquals( fixed.getRowCount() - 1, rl.getRowCount() );
        assertArrayEquals( fixed.getRow( 1 ), rl.getRow( 1 ) );
        assertArrayEquals( fixed.getRow( 4 ), rl.getRow( 3 ) );

        rl.insertRow( 10, new Object[ rl.getColumnCount() ] );
        assertEquals( fixed.getRowCount(), rl.getRowCount() );
        assertArrayEquals( fixed.getRow( 12 ), rl.getRow( 12 ) );

        int val30 = ((Integer) rl.getCell( 3, 0 )).intValue();
        assertEquals( Integer.valueOf( val30 ), rl.getCell( 3, 0 ) );
        rl.setCell( 3, 0, Integer.valueOf( val30 + 1 ) );
        assertTrue( ! Integer.valueOf( val30 ).equals( rl.getCell( 3, 0 ) ) );
        rl.setCell( 3, 0, Integer.valueOf( val30 ) );
        assertTrue( Integer.valueOf( val30 ).equals( rl.getCell( 3, 0 ) ) );

    }
}
