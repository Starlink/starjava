package uk.ac.starlink.util;

import java.lang.reflect.Array;

public class PrimitiveListTest extends TestCase {

    public PrimitiveListTest( String name ) {
        super( name );
    }

    public void testLists() {
        ByteList blist;
        ShortList slist;
        IntList ilist;
        LongList llist;
        FloatList flist;
        DoubleList dlist;
        PrimitiveList[] lists = new PrimitiveList[] {
            blist = new ByteList(),
            slist = new ShortList(),
            ilist = new IntList(),
            llist = new LongList(),
            flist = new FloatList(),
            dlist = new DoubleList(),
        };
        for ( int i = 0; i < 109; i++ ) {
            for ( int ib = 0; ib < lists.length; ib++ ) {
                PrimitiveList list = lists[ ib ];
                assertEquals( i, list.size() );
                assertEquals( i, Array.getLength( list.toArray() ) );
            }

            byte[] barr = new byte[ i ];
            short[] sarr = new short[ i ];
            int[] iarr = new int[ i ];
            long[] larr = new long[ i ];
            float[] farr = new float[ i ];
            double[] darr = new double[ i ];
            for ( int j = 0; j < i; j++ ) {
                barr[ j ] = (byte) j;
                sarr[ j ] = (short) j;
                iarr[ j ] = (int) j;
                larr[ j ] = (long) j;
                farr[ j ] = (float) j;
                darr[ j ] = (double) j;
                assertEquals( (byte) j, blist.get( j ) );
                assertEquals( (short) j, slist.get( j ) );
                assertEquals( (int) j, ilist.get( j ) );
                assertEquals( (long) j, llist.get( j ) );
                assertEquals( (float) j, flist.get( j ) );
                assertEquals( (double) j, dlist.get( j ) );
            }
            assertArrayEquals( barr, blist.toByteArray() );
            assertArrayEquals( sarr, slist.toShortArray() );
            assertArrayEquals( iarr, ilist.toIntArray() );
            assertArrayEquals( larr, llist.toLongArray() );
            assertArrayEquals( farr, flist.toFloatArray() );
            assertArrayEquals( darr, dlist.toDoubleArray() );
            try {
                ilist.get( i );
                fail();
            }
            catch ( RuntimeException e ) {
            }

            blist.add( (byte) i );
            slist.add( (short) i );
            ilist.add( (int) i );
            llist.add( (long) i );
            flist.add( (float) i );
            dlist.add( (double) i );
        }
    }
}
