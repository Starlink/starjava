package uk.ac.starlink.table;

import java.io.IOException;
import java.util.BitSet;
import uk.ac.starlink.util.TestCase;

public class RowSubsetTest extends TestCase {

    public RowSubsetTest( String name ) {
        super( name );
    }

    public void testRowSubset() throws IOException {
        ColumnStarTable base = ColumnStarTable.makeTableWithRows( 1000 );
        int[] data = new int[ 1000 ];
        fillCycle( data, 0, 1000 );
        base.addColumn( ArrayColumn.makeColumn( "data", data ) );

        BitSet mask = new BitSet();
        RowSubsetStarTable rowtab = new RowSubsetStarTable( base, mask );

        assertHasRows( 0, rowtab );
        assertArrayEquals( new int[ 0 ], getData( rowtab ) );

        mask.flip( 0, 500 );
        assertHasRows( 500, rowtab );
        int[] data500 = new int[ 500 ];
        System.arraycopy( data, 0, data500, 0, 500 );
        assertArrayEquals( data500, getData( rowtab ) );
    }

    private static void assertHasRows( int nrows, StarTable st ) 
            throws IOException {
        int count = 0; 
        for ( RowSequence rseq = st.getRowSequence(); rseq.next(); ) {
            count++;
        }
        assertEquals( nrows, count );
    }

    private static int[] getData( StarTable st ) 
            throws IOException {
        int nrow = 0;
        for ( RowSequence rseq = st.getRowSequence(); rseq.next(); ) {
            nrow++;
        }
        int[] data = new int[ nrow ];
        int i = 0;
        for ( RowSequence rseq = st.getRowSequence(); rseq.next(); ) {
            data[ i++ ] = ((Integer) rseq.getCell( 0 )).intValue();
        }
        assertEquals( nrow, i );
        return data;
    }
}
