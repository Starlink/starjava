package uk.ac.starlink.array;

import java.util.Iterator;
import java.util.Random;
import uk.ac.starlink.util.TestCase;

public class ShapeTest extends TestCase {

    private Random rand;

    public ShapeTest( String name ) {
        super( name );
    }

    public void setUp() {
        rand = new Random( 23L );
    }

    public void testNDShapeObject() {
        long[] origin = new long[] { 0, 10, 20, -100 };
        long[] dims = new long[] { 20, 30, 40, 50 };
        NDShape sh = new NDShape( origin, dims );

        // make sure we're not giving out references to private structures
        long[] originCopy = sh.getOrigin();
        long[] dimsCopy = sh.getDims();
        originCopy[ 0 ]++;
        dimsCopy[ 0 ]++;
        assertArrayEquals( origin, sh.getOrigin() );
        assertArrayEquals( dims, sh.getDims() );

        NDShape sh1 = new NDShape( sh );
        assertEquals( sh, sh1 );
        assertEquals( sh.hashCode(), sh1.hashCode() );
        assertTrue( sh.sameShape( sh1 ) );

        NDShape sh2 = (NDShape) sh.clone();
        assertEquals( sh, sh2 );

        long[] oX = (long[]) origin.clone();
        oX[ 1 ]++;
        NDShape shX = new NDShape( oX, dims );
        assertTrue( ! sh.equals( shX ) );
        assertTrue( sh.hashCode() != shX.hashCode() ); 
                                           // well, could be I suppose..
        assertTrue( ! sh.sameShape( shX ) );
    }


    public void testOrderedNDShapeObject() {
        long[] origin = new long[] { -100, -200, -300 };
        long[] dims = new long[] { 43, 53, 63 };
        NDShape sh = new NDShape( origin, dims );

        OrderedNDShape os1 = new OrderedNDShape( sh, Order.COLUMN_MAJOR );
        OrderedNDShape os2 = new OrderedNDShape( sh, Order.ROW_MAJOR );
        assertTrue( ! os1.equals( os2 ) );
        assertTrue( ! os1.sameSequence( os2 ) );

        NDShape line = new NDShape( new long[] { 1 },
                                    new long[] { Long.MAX_VALUE } );
        OrderedNDShape line1 = new OrderedNDShape( line, Order.COLUMN_MAJOR );
        OrderedNDShape line2 = new OrderedNDShape( line, Order.ROW_MAJOR );
        assertTrue( ! line1.equals( line2 ) );
        assertTrue( line1.sameSequence( line2 ) );
        assertTrue( line2.sameSequence( line1 ) );

        OrderedNDShape oss0 = new OrderedNDShape( sh );
        OrderedNDShape oss1 = new OrderedNDShape( os1 );
        OrderedNDShape oss2 = new OrderedNDShape( os2 );
        assertEquals( os1, oss1 );
        assertEquals( os2, oss2 );
        assertTrue( oss0.equals( os1 ) ^ oss0.equals( os2 ) );

        OrderedNDShape osh = new OrderedNDShape( origin, dims, null );
        assertTrue( osh.equals( os1 ) ^ osh.equals( os2 ) );
        assertTrue( osh.sameSequence( os1 ) ^ osh.sameSequence( os2 ) );
        assertTrue( osh.hashCode() == os1.hashCode() ^
                    osh.hashCode() == os2.hashCode() );
        assertTrue( os1.hashCode() != os2.hashCode() );
        assertTrue( os1.sameShape( os2 ) );
        assertTrue( os1.sameShape( sh ) );
        assertTrue( sh.sameShape( os1 ) );
        assertTrue( sh.sameShape( os2 ) );

        OrderedNDShape oshape1 = new OrderedNDShape( os1, os1.getOrder() );
        assertEquals( os1, oshape1 );
        assertEquals( os1.hashCode(), oshape1.hashCode() );
        assertTrue( os1.sameSequence( oshape1 ) );

        OrderedNDShape oshape2 = (OrderedNDShape) os2.clone();
        assertEquals( os2.getClass(), OrderedNDShape.class );
        assertTrue( os2 != oshape2 );
        assertEquals( os2, oshape2 );
        assertEquals( os2.hashCode(), oshape2.hashCode() );
        assertTrue( os2.sameSequence( oshape2 ) );

        // make sure we're not giving out references to private structures
        long[] originCopy = osh.getOrigin();
        long[] dimsCopy = osh.getDims();
        originCopy[ 0 ]++;
        dimsCopy[ 0 ]++;
        assertArrayEquals( origin, osh.getOrigin() );
        assertArrayEquals( dims, osh.getDims() );
    }

    public void testOrigins() {
        assertEquals( NDShape.DEFAULT_ORIGIN, 1L );
        int ndim = 4;
        long[] defaultOrigin = new long[ ndim ];
        long[] ldims = new long[ ndim ];
        int[] idims = new int[ ndim ];
        for ( int i = 0; i < ndim; i++ ) {
            defaultOrigin[ i ] = NDShape.DEFAULT_ORIGIN;
            idims[ i ] = i + 23;
            ldims[ i ] = idims[ i ];
        }
        NDShape shape1 = new NDShape( defaultOrigin, idims );
        NDShape shape2 = new NDShape( defaultOrigin, ldims );
        NDShape shape3 = new NDShape( idims );
        NDShape shape4 = new NDShape( ldims );
        assertEquals( shape1, shape2 );
        assertEquals( shape1, shape3 );
        assertEquals( shape1, shape4 );

        Order or = Order.COLUMN_MAJOR;
        OrderedNDShape oshape1 = new OrderedNDShape( defaultOrigin, ldims, or );
        OrderedNDShape oshape2 = new OrderedNDShape( ldims, or );
        OrderedNDShape oshapeX = new OrderedNDShape( ldims, Order.ROW_MAJOR );
        assertEquals( oshape1, oshape2 );
        assertTrue( ! oshape1.equals( oshapeX ) );
        assertTrue( ! oshape2.equals( oshapeX ) );
    }


    public void testNDShapeFunctions() {
        assertEquals( NDShape.toString( new long[] { 9, 8, 7 } ), "(9,8,7)" );
        assertEquals( NDShape.toString( new long[] { 9, 8, Long.MIN_VALUE } ),
                                        "(9,8,*)" );

        long[] origin = new long[] { 0, 10, 20 };
        long[] dims = new long[] { 2, 3, 4 };
        NDShape sh = new NDShape( origin, dims );

        assertEquals( sh.getNumDims(), 3 );
        assertEquals( sh.getNumPixels(),  2 * 3 * 4 );
        assertArrayEquals( sh.getOrigin(), origin );
        assertArrayEquals( sh.getDims(), dims );

        long[] limits = sh.getLimits();
        long[] ubnds = sh.getUpperBounds();
        for ( int i = 0; i < dims.length; i++ ) {
            assertEquals( origin[ i ] + dims[ i ], limits[ i ] );
            assertEquals( origin[ i ] + dims[ i ] - 1, ubnds[ i ] );
        }

        assertTrue( sh.within( new long[] { 0, 10, 20 } ) );
        assertTrue( sh.within( new long[] { 1, 12, 23 } ) );
        assertTrue( ! sh.within( new long[] { 0, 9, 20 } ) );
        assertTrue( ! sh.within( new long[] { 1, 12, 24 } ) );
        NDShape shapeA = new NDShape( new long[] { 100 }, new long[] { 10 } );
        NDShape shapeB = new NDShape( new long[] { 90 }, new long[] { 20 } );
        assertTrue( shapeA.intersection( shapeB ).equals( shapeA ) );
        assertTrue( shapeB.intersection( shapeA ).equals( shapeA ) );
        assertTrue( shapeA.union( shapeB ).equals( shapeB ) );
        assertTrue( shapeB.union( shapeA ).equals( shapeB ) );
    }

    public void testOrderedNDShapeFunctions() {
        long[] origin = new long[] { 100, 200, 300 };
        long[] dims = new long[] { 4, 5, 6 };
        NDShape shape = new NDShape( origin, dims );
        OrderedNDShape os1 = new OrderedNDShape( shape, Order.COLUMN_MAJOR );
        OrderedNDShape os2 = new OrderedNDShape( shape, Order.ROW_MAJOR );

        Iterator it1 = os1.pixelIterator();
        long n1 = 0;
        long[] pos1 = new long[ 3 ];
        for ( long k = shape.getOrigin()[ 2 ];
              k < shape.getLimits()[ 2 ]; k++ ) {
            pos1[ 2 ] = k;
            for ( long j = shape.getOrigin()[ 1 ];
                  j < shape.getLimits()[ 1 ]; j++ ) {
                pos1[ 1 ] = j;
                for ( long i = shape.getOrigin()[ 0 ];
                      i < shape.getLimits()[ 0 ]; i++ ) {
                    pos1[ 0 ] = i;
                    assertTrue( it1.hasNext() );
                    assertArrayEquals( (long[]) it1.next(), pos1 );
                    assertArrayEquals( os1.offsetToPosition( n1 ), pos1 );
                    assertEquals( n1, os1.positionToOffset( pos1 ) );
                    n1++;
                }
            }
        }
        assertTrue( ! it1.hasNext() );

        Iterator it2 = os2.pixelIterator();
        long n2 = 0;
        long[] pos2 = new long[ 3 ];
        for ( long i = shape.getOrigin()[ 0 ];
              i < shape.getLimits()[ 0 ]; i++ ) {
            pos2[ 0 ] = i;
            for ( long j = shape.getOrigin()[ 1 ];
                  j < shape.getLimits()[ 1 ]; j++ ) {
                pos2[ 1 ] = j;
                for ( long k = shape.getOrigin()[ 2 ];
                      k < shape.getLimits()[ 2 ]; k++ ) {
                    pos2[ 2 ] = k;
                    assertTrue( it2.hasNext() );
                    assertArrayEquals( (long[]) it2.next(), pos2 );
                    assertArrayEquals( os2.offsetToPosition( n2 ), pos2 );
                    assertEquals( n2, os2.positionToOffset( pos2 ) );
                    n2++;
                }
            }
        }
        assertTrue( ! it2.hasNext() );

        long npix = shape.getNumPixels();
        long[] last = shape.getLimits();
        for ( int i = 0; i < shape.getNumDims(); i++ ) {
            last[ i ]--;
        }

        Iterator ita1 = os1.pixelIterator( npix - 1, 1 );
        assertArrayEquals( last, (long[]) ita1.next() );
        assertTrue( ! ita1.hasNext() );

        Iterator ita2 = os2.pixelIterator( npix - 1, 1 );
        assertArrayEquals( last, (long[]) ita2.next() );
        assertTrue( ! ita2.hasNext() );
    }


    public void testIntsToLongs() {
        int num = 1000;
        int[] iarray = new int[ num ];
        iarray[ 1 ] = Integer.MAX_VALUE;
        iarray[ 2 ] = Integer.MIN_VALUE;
        for ( int i = 3; i < num; i++ ) {
            iarray[ i ] = rand.nextInt();
        }
        long[] larray = NDShape.intsToLongs( iarray );
        for ( int i = 0; i < iarray.length; i++ ) {
            assertEquals( iarray[ i ], (int) larray[ i ] );
        }
    }

    public void testLongsToInts() {
        int num = 1000;
        long[] larray = new long[ num ];
        larray[ 1 ] = Integer.MAX_VALUE;
        larray[ 2 ] = Integer.MIN_VALUE;
        for ( int i = 3; i < num; i++ ) {
            larray[ i ] = rand.nextInt();
        }
        int[] iarray = NDShape.longsToInts( larray );
        for ( int i = 0; i < larray.length; i++ ) {
            assertEquals( larray[ i ], (long) iarray[ i ] );
        }

        larray[ num / 2 ] = ( (long) Integer.MAX_VALUE ) + 1L;
        try {
            NDShape.longsToInts( larray );
            fail();
        }
        catch ( IndexOutOfBoundsException e ) {}
    }

    public void testNDShapeExceptions() {
        try {
            new NDShape( new long[ 2 ], new long[] { 1 } );
            fail();
        }
        catch ( IllegalArgumentException e ) {}
        try {
            new NDShape( new long[ 1 ], new int[] { 0 } );
            fail();
        }
        catch ( IllegalArgumentException e ) {}
        try {
            new NDShape( new long[ 1 ], new long[] { -1 } );
            fail();
        }
        catch ( IllegalArgumentException e ) {}
    }

    public void testOrderedNDShapeExceptions() {
        Order o = Order.ROW_MAJOR;
        try {
            new OrderedNDShape( new long[ 2 ], new long[] { 1 }, o );
            fail();
        }
        catch ( IllegalArgumentException e ) {}
        try { 
            new OrderedNDShape( new long[ 1 ], new long[] { -1 }, o );
            fail();
        }
        catch ( IllegalArgumentException e ) {}

        long[] origin = new long[] { 0, 0 };
        long[] dims = new long[] { 10, 10 };
        OrderedNDShape osh = new OrderedNDShape( origin, dims, null );
        try {
            osh.offsetToPosition( -1L );
            fail();
        }
        catch ( IndexOutOfBoundsException e ) {}
        try {
            osh.offsetToPosition( 10L * 10L );
            fail();
        }
        catch ( IndexOutOfBoundsException e ) {}
        try {
            osh.positionToOffset( new long[] { -1L, 0L } );
            fail();
        }
        catch ( IndexOutOfBoundsException e ) {}
        try {
            osh.positionToOffset( new long[] { 10L, 0L } );
            fail();
        }
        catch ( IndexOutOfBoundsException e ) {}

        try {
            osh.pixelIterator( -1L, 1 );
            fail();
        }
        catch ( IllegalArgumentException e ) {}
        osh.pixelIterator( osh.getNumPixels() - 1L, 1L );
        try {
            osh.pixelIterator( osh.getNumPixels(), 1L );
            fail();
        }
        catch ( IllegalArgumentException e ) {}
    }
}
