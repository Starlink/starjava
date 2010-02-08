package uk.ac.starlink.table.join;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import uk.ac.starlink.util.TestCase;

public class BinnerTest extends TestCase {

    private final Random rnd_;

    public BinnerTest( String name ) {
        super( name );
        rnd_ = new Random( 1122334455L );
    }

    public void testLongBinner() {
        for ( int i = 1; i < 100; i++ ) {
            exerciseLongBinner( Binners.createLongBinner( i ), i );
            exerciseLongBinner( Binners.createLongBinner( -1 ), i );
        }
    }

    private void exerciseLongBinner( LongBinner binner, int count ) {
        String[] keys = new String[] { "A", "B", };
        long[] a1 = new long[ count * 2 ];
        long[] a2 = new long[ count ];
        for ( int i = 0; i < count; i++ ) {
            long ltem = (long) rnd_.nextInt( count );
            binner.addItem( keys[ 0 ], ltem );
            binner.addItem( keys[ 1 ], ltem );
            binner.addItem( keys[ 0 ], ltem );
            a1[ i * 2 ] = ltem;
            a1[ i * 2 + 1 ] = ltem;
            a2[ i ] = ltem;
        }
        assertArrayEquals( a1, binner.getLongs( keys[ 0 ] ) );
        assertArrayEquals( a2, binner.getLongs( keys[ 1 ] ) );
        assertNull( binner.getLongs( "X" ) );
        Set kset = new HashSet();
        for ( Iterator it = binner.getKeyIterator(); it.hasNext(); ) {
            kset.add( it.next() );
        }
        assertEquals( new HashSet( Arrays.asList( keys ) ), kset );
    }
}
