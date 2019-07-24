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

    public void testAddAll() {
        assertArrayEquals( new byte[] { 0, 0, 1, 2, 3 },
                           addArray( new ByteList( new byte[ 2 ] ),
                                     new ByteList( new byte[] { 1, 2, 3 } ) ) );
        assertArrayEquals( new short[] { 0, 0, 1, 2, 3 },
                           addArray( new ShortList( new short[ 2 ] ),
                                     new ShortList( new short[] { 1, 2, 3 } )));
        assertArrayEquals( new int[] { 0, 0, 1, 2, 3 },
                           addArray( new IntList( new int[ 2 ] ),
                                     new IntList( new int[] { 1, 2, 3 } ) ) );
        assertArrayEquals( new long[] { 0, 0, 1, 2, 3 },
                           addArray( new LongList( new long[ 2 ] ),
                                     new LongList( new long[] { 1, 2, 3 } ) ) );
        assertArrayEquals( new float[] { 0, 0, 1, 2, 3 },
                           addArray( new FloatList( new float[ 2 ] ),
                                     new FloatList( new float[] { 1, 2, 3 } )));
        assertArrayEquals( new double[] { 0, 0, 1, 2, 3 },
                           addArray( new DoubleList( new double[ 2 ] ), 
                                     new DoubleList( new double[] { 1,2,3 } )));

        assertArrayEquals( new int[ 0 ],
                           addArray( new IntList(), new IntList() ) );
    }

    private Object addArray( PrimitiveList list1, PrimitiveList list2 ) {
        boolean changed = list1.addAll( list2 );
        assertTrue( changed ^ list2.size() == 0 );
        return list1.toArray();
    }
}
