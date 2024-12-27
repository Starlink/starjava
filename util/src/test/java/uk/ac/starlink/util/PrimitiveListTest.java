package uk.ac.starlink.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PrimitiveListTest {

    @Test
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
        int k = 23;
        assertEquals( blist.toByteArray()[ k ], blist.getByteBuffer()[ k ] );
        assertEquals( slist.toShortArray()[ k ], slist.getShortBuffer()[ k ] );
        assertEquals( ilist.toIntArray()[ k ], ilist.getIntBuffer()[ k ] );
        assertEquals( llist.toLongArray()[ k ], llist.getLongBuffer()[ k ] );
        assertEquals( flist.toFloatArray()[ k ], flist.getFloatBuffer()[ k ] );
        assertEquals( dlist.toDoubleArray()[ k ], dlist.getDoubleBuffer()[ k ]);
    }

    @Test
    public void testAddAll() {
        assertArrayEquals( new byte[] { 0, 0, 1, 2, 3 },
                addList( new ByteList( new byte[ 2 ] ),
                        new ByteList( new byte[] { 1, 2, 3 } ) ) );
        assertArrayEquals( new short[] { 0, 0, 1, 2, 3 },
                           addList( new ShortList( new short[ 2 ] ),
                                    new ShortList( new short[] { 1, 2, 3 } )));
        assertArrayEquals( new int[] { 0, 0, 1, 2, 3 },
                           addList( new IntList( new int[ 2 ] ),
                                    new IntList( new int[] { 1, 2, 3 } ) ) );
        assertArrayEquals( new long[] { 0, 0, 1, 2, 3 },
                           addList( new LongList( new long[ 2 ] ),
                                    new LongList( new long[] { 1, 2, 3 } ) ) );
        assertArrayEquals( new float[] { 0, 0, 1, 2, 3 },
                           addList( new FloatList( new float[ 2 ] ),
                                    new FloatList( new float[] { 1, 2, 3 } )));
        assertArrayEquals( new double[] { 0, 0, 1, 2, 3 },
                           addList( new DoubleList( new double[ 2 ] ), 
                                    new DoubleList( new double[] { 1,2,3 } )));

        assertArrayEquals(new int[0],
                addList(new IntList(), new IntList()));

        IntList il1 = new IntList( new int[] { 5, 10, 16, 23 } );
        il1.clear();
        assertArrayEquals( new int[ 0 ],
                           addList( il1, new IntList() ) );

        ByteList b = new ByteList( new byte[ 2 ] );
        ShortList s = new ShortList( new short[ 2 ] );
        IntList i = new IntList( new int[ 2 ] );
        LongList l = new LongList( new long[ 2 ] );
        FloatList f = new FloatList( new float[ 2 ] );
        DoubleList d = new DoubleList( new double[ 2 ] );
        b.addAll( new byte[] { 1, 2, 3 } );
        s.addAll( new short[] { 1, 2, 3 } );
        i.addAll( new int[] { 1, 2, 3 } );
        l.addAll( new long[] { 1, 2, 3 } );
        f.addAll( new float[] { 1, 2, 3 } );
        d.addAll( new double[] { 1, 2, 3 } );
        assertArrayEquals( new byte[] { 0, 0, 1, 2, 3 }, b.toByteArray() );
        assertArrayEquals( new short[] { 0, 0, 1, 2, 3 }, s.toShortArray() );
        assertArrayEquals( new int[] { 0, 0, 1, 2, 3 }, i.toIntArray() );
        assertArrayEquals( new float[] { 0, 0, 1, 2, 3 }, f.toFloatArray() );
        assertArrayEquals( new double[] { 0, 0, 1, 2, 3 }, d.toDoubleArray() );
    }

    @Test
    public void testString() {
        ByteList buf = new ByteList();
        buf.add( (byte) 'R' );
        buf.add( (byte) 'e' );
        buf.add( (byte) 'x' );
        assertEquals( "Rex", buf.decodeUtf8() );

        buf.clear();
        buf.add( (byte) 0 );
        buf.add( (byte) 'R' );
        buf.add( (byte) 0 );
        buf.add( (byte) 'e' );
        buf.add( (byte) 0 );
        buf.add( (byte) 'x' );
        assertEquals( "Rex", buf.decodeString( StandardCharsets.UTF_16 ) );

        buf = new ByteList();
        buf.add( (byte) 0x24 );
        buf.add( (byte) 0xc2 );
        buf.add( (byte) 0xa3 );
        assertEquals( "\u0024\u00a3", buf.decodeUtf8() );
    }

    private <T> T addList(PrimitiveList list1, PrimitiveList list2 ) {
        boolean changed = list1.addAll( list2 );
        assertTrue( changed ^ list2.size() == 0 );
        return (T) list1.toArray();
    }
}
