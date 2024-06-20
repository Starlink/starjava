package uk.ac.starlink.array;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Random;
import uk.ac.starlink.util.TestCase;

public class NDArrayTest extends TestCase {

    private OrderedNDShape shape;
    private NDArray nda;
    private List types;
    private List shapes;
    private List templates;
    private Random rand = new Random( 51L );

    public NDArrayTest( String name ) {
        super( name );
    }

    public void setUp() {
        types = Type.allTypes();
        shapes = new ArrayList();
        shapes.add( new OrderedNDShape( new long[] { 50 },
                                        new long[] { 100 },
                                        Order.COLUMN_MAJOR ) );
        shapes.add( new OrderedNDShape( new long[] { 0, 0 },
                                        new long[] { 32, 64 },
                                        Order.COLUMN_MAJOR ) );
        shapes.add( new OrderedNDShape( new long[] { 3, 4, 5 },
                                        new long[] { 10, 11, 12 },
                                        Order.ROW_MAJOR ) );
        templates = new ArrayList();
        for ( Iterator shIt = shapes.iterator(); shIt.hasNext(); ) {
            OrderedNDShape shape = (OrderedNDShape) shIt.next();
            for ( Iterator tIt = types.iterator(); tIt.hasNext(); ) {
                Type type = (Type) tIt.next();
                BadHandler bh = BadHandler
                               .getHandler( type, type.defaultBadValue() );
                templates.add( 
                    new DefaultArrayDescription( shape, type, bh,
                                                 true, true, true ) );
            }
        }
    }


    public void testTiles() throws IOException {
        for ( Iterator it = ndaIterator(); it.hasNext(); ) {
            NDArray nda1 = (NDArray) it.next();

            OrderedNDShape shape1 = nda1.getShape();
            Type type = nda1.getType();
            Order order = shape1.getOrder();

            for ( Iterator tit = tileIterator( shape1 ); tit.hasNext(); ) {
                OrderedNDShape tshape = 
                    new OrderedNDShape( (NDShape) tit.next(), order );

                ArrayAccess acc1 = nda1.getAccess();
                int tnpix = (int) tshape.getNumPixels();
                Object buf1 = type.newArray( tnpix );
                Object buf2 = type.newArray( tnpix );
                acc1.readTile( buf1, tshape );
                int j = 0;
                nda1.getBadHandler().putBad( buf2, 0, tnpix );
                for ( Iterator pit = tshape.pixelIterator(); pit.hasNext(); ) {
                    long[] pos = (long[]) pit.next();
                    if ( shape1.within( pos ) ) {
                        acc1.setPosition( pos );
                        acc1.read( buf2, j, 1 );
                    }
                    j++;
                }
                acc1.close();

                assertEquals( tnpix, j );
                assertArrayEquals( buf1, buf2 );
            }
            nda1.close();
        }
    }

    public void testDummy() throws IOException {
        for ( Iterator it = ndaIterator(); it.hasNext(); ) {
            NDArray inda = (NDArray) it.next();
            NDArray dum1 = new DummyNDArray( inda );
            exerciseDummy( dum1 );
            NDArray dum2 = new DummyNDArray( inda.getShape(), 
                                             inda.getType(),
                                             inda.getBadHandler() );
            exerciseDummy( dum2 );
            assertTrue( NDArrays.equals( dum1, dum2 ) );
            assertTrue( ! NDArrays.equals( dum1, inda ) );
        }
    }

    public void exerciseDummy( NDArray nda ) throws IOException {
        assertTrue( nda.isReadable() );
        assertTrue( ! nda.isWritable() );
        ArrayAccess acc = nda.getAccess();
        assertTrue( ! acc.isMapped() );
        int npix = (int) nda.getShape().getNumPixels();
        Object buf = nda.getType().newArray( npix );
        acc.read( buf, 0, (int) npix );
        BadHandler bh = nda.getBadHandler();
        for ( int i = 0; i < npix; i++ ) {
            assertTrue( bh.isBad( buf, i ) );
        }
        acc.close();
    }

    public void testMoulded() throws IOException {
        for ( Iterator it = ndaIterator(); it.hasNext(); ) {
            NDArray nda1 = (NDArray) it.next();

            int ndim = nda1.getShape().getNumDims();
            int npix = (int) nda1.getShape().getNumPixels();
            long[] o2 = nda1.getShape().getOrigin();
            long[] d2 = nda1.getShape().getDims();
            for ( int j = 0; j < ndim; j++ ) {
                o2[ j ] += 10;
            }
            NDShape shape2 = new NDShape( o2, d2 );
            ArrayImpl impl2 = new MouldArrayImpl( nda1, shape2 );
            NDArray nda2 = NDArrays.scratchCopy( impl2 );
            assertTrue( ! nda1.getShape().sameShape( nda2.getShape() ) );
            assertTrue( ! nda1.getShape().sameSequence( nda2.getShape() ) );
            assertTrue( o2 != nda2.getShape().getOrigin() );
            assertArrayEquals( o2, nda2.getShape().getOrigin() );
            assertTrue( nda1.getShape().getOrigin()[ 0 ] !=
                        nda2.getShape().getOrigin()[ 0 ] );

            Object a1 = nda1.getType().newArray( npix );
            Object a2 = nda2.getType().newArray( npix );
            Iterator it1 = nda1.getShape().pixelIterator();
            Iterator it2 = nda2.getShape().pixelIterator();
            ArrayAccess acc1 = nda1.getAccess();
            ArrayAccess acc2 = nda2.getAccess();
            for ( int j = 0; j < npix; j++ ) {
                long[] p1 = acc1.getPosition();
                long[] p2 = acc2.getPosition();
                for ( int k = 0; k < ndim; k++ ) {
                    assertEquals( p1[ k ] + 10, p2[ k ] );
                }
                assertArrayEquals( it1.next(), p1 );
                assertArrayEquals( it2.next(), p2 );
                acc1.read( a1, j, 1 );
                acc2.read( a2, j, 1 );
            }
            assertEquals( npix, acc1.getOffset() );
            assertEquals( npix, acc2.getOffset() );
            acc1.close();
            acc2.close();
            nda1.close();
            nda2.close();
        }
    }

    public void testWindow() throws IOException {
        for ( Iterator it = ndaIterator(); it.hasNext(); ) {
            NDArray nda1 = (NDArray) it.next();

            OrderedNDShape shape1 = nda1.getShape();
            Type type = nda1.getType();
            Order order = shape1.getOrder();

            for ( Iterator tit = tileIterator( shape1 ); tit.hasNext(); ) {
                OrderedNDShape winshape =
                    new OrderedNDShape( (NDShape) tit.next(), order );
                int winpix = (int) winshape.getNumPixels();
                Object buf1 = type.newArray( winpix );
                Object buf2 = type.newArray( winpix );

                ArrayAccess acc1 = nda1.getAccess();
                acc1.readTile( buf1, winshape );
                acc1.close();

                ArrayImpl impl2 = new WindowArrayImpl( nda1, winshape );
                NDArray nda2 = NDArrays.scratchCopy( impl2 );
                assertEquals( winshape, nda2.getShape() );
                ArrayAccess acc2 = nda2.getAccess();
                acc2.read( buf2, 0, winpix );
                acc2.close();
                nda2.close();

                assertArrayEquals( buf1, buf2 );
            }
            nda1.close();
        }
    }


    public void testOffset() throws IOException {
        OffsetMapper mapper = new OffsetMapper() {
            public long mapOffset( long off1 ) {
                return ( ( off1 % 3 ) == 0 ) ? off1 : -1L;
            }
            public long[] mapRange( long[] range1 ) {
                return new long[] { -1L, range1[1] };
            }
        };

        for ( Iterator it = ndaIterator(); it.hasNext(); ) {
            NDArray nda1 = (NDArray) it.next();
            Type type = nda1.getType();
            OrderedNDShape shape = nda1.getShape();
            BadHandler bh = nda1.getBadHandler();
            int npix = (int) shape.getNumPixels();
            int npix2 = npix / 2;
            Object buf1 = type.newArray( npix );
            Object buf2 = type.newArray( npix );

            ArrayAccess acc1 = nda1.getAccess();
            acc1.read( buf1, 0, npix2 );
            acc1.close();

            NDArray nda2 = 
                new BridgeNDArray( new PixelMapArrayImpl( nda1, shape,
                                                          mapper ) );
            ArrayAccess acc2 = nda2.getAccess();
            acc2.read( buf2, 0, npix2 );
            acc2.close();

            int nnull = 0;
            for ( int i = 0; i < npix2; i++ ) {
                Number num1 = bh.makeNumber( buf1, i );
                Number num2 = bh.makeNumber( buf2, i );
                if ( ( i % 3 ) == 0 ) {
                    assertEquals( num1, num2 );
                    if ( num2 == null ) {
                        nnull++;
                    }
                }
                else {
                    assertNull( num2 );
                }
            }
            assertTrue( nnull < npix2 / 2 );
            nda2.close();
            nda1.close();
        }
    }


    public void testToRequired() throws IOException {
        for ( Iterator it = ndaIterator(); it.hasNext(); ) {
            NDArray nda1 = (NDArray) it.next();
            OrderedNDShape shape1 = nda1.getShape();
            Type type1 = nda1.getType();
            BadHandler bh1 = nda1.getBadHandler();

            Type type = (Type) Type.FLOAT;
            Requirements req = new Requirements( AccessMode.READ );
            BadHandler bh = 
                BadHandler.getHandler( type, Float.valueOf( 23e23f ) );

            for ( Iterator wit = oshapeIterator( shape1 ); wit.hasNext(); ) {
                OrderedNDShape oshape = (OrderedNDShape) wit.next();

                Order order = oshape.getOrder();
                NDShape window = new NDShape( oshape );

                req.setType( type )
                   .setBadHandler( bh )
                   .setOrder( order )
                   .setWindow( window );

                NDArray nda2 = NDArrays.toRequiredArray( nda1, req );

                OrderedNDShape shape2 = nda2.getShape();
                Order order2 = shape2.getOrder();
                BadHandler bh2 = nda2.getBadHandler();
                Type type2 = nda2.getType();

                assertEquals( type, type2 );
                assertEquals( bh, bh2 );
                assertEquals( order, order2 );
                assertTrue( window.sameShape( shape2 ) );

                Object buf1 = type1.newArray( 1 );
                Object buf2 = type2.newArray( 1 );
                ArrayAccess acc1 = nda1.getAccess();
                ArrayAccess acc2 = nda2.getAccess();
                for ( Iterator pit = shape2.pixelIterator(); pit.hasNext(); ) {
                    long[] pos = (long[]) pit.next();
                    acc2.read( buf2, 0, 1 );
                    if ( shape1.within( pos ) ) {
                        acc1.setPosition( pos );
                        acc1.read( buf1, 0, 1 );
                        Number num1 = bh1.makeNumber( buf1, 0 );
                        Number num2 = bh2.makeNumber( buf2, 0 );
                        if ( num1 == null ) {
                            assertNull( num2 );
                        }
                        else {
                            assertEquals( num1.doubleValue(), 
                                          num2.doubleValue(), 0.01 );
                        }
                    }
                    else {
                        assertTrue( bh2.isBad( buf2, 0 ) );
                    }
                }
                acc1.close();
                acc2.close();
            }
            nda1.close();
        }
    }

    public void testNDArrays() throws IOException {
        for ( Iterator it = ndaIterator(); it.hasNext(); ) {
            NDArray nda1 = (NDArray) it.next();
            OrderedNDShape oshape1 = nda1.getShape();
            OrderedNDShape oshape2 = new OrderedNDShape( oshape1, 
                                                         Order.ROW_MAJOR );
            Type type = nda1.getType();
            BadHandler bh1 = nda1.getBadHandler();
            BadHandler bh2;
            if ( type == Type.FLOAT ) {
                bh2 = BadHandler.getHandler( Type.FLOAT,
                                             Float.valueOf( 19e19f ) );
            }
            else {
                bh2 = bh1;
            }
            NDArray nda2 = new ScratchNDArray( oshape2, type, bh2 );
            NDArrays.copy( nda1, nda2 );
            assertTrue( NDArrays.equals( nda1, nda2 ) );

            ArrayAccess acc1 = nda1.getAccess();
            int pos = 15; // if this accidentally hits a bad one, change it
            Object orig = type.newArray( 1 );
            acc1.setOffset( pos );
            acc1.read( orig, 0, 1 );

            Object changed = type.newArray( 1 );
            acc1.setOffset( pos );
            acc1.write( changed, 0, 1 );
            assertTrue( ! NDArrays.equals( nda1, nda2 ) );

            bh1.putBad( changed, 0 );
            acc1.setOffset( pos );
            acc1.write( changed, 0, 1 );
            assertTrue( ! NDArrays.equals( nda1, nda2 ) );
            
            acc1.setOffset( pos );
            acc1.write( orig, 0, 1 );
            assertTrue( NDArrays.equals( nda1, nda2 ) );

            acc1.close();
            nda1.close();
            nda2.close();
        }
    }


    /** 
     * Iterates over a set of test NDArrays.  They have various shapes,
     * orderings and types, and are filled with random data, including
     * some bad values.
     */
    private Iterator ndaIterator() {
        return new Iterator() {
            private Iterator it = templates.iterator();
            public boolean hasNext() {
                return it.hasNext();
            }
            public Object next() {
                try {
                    ArrayDescription template = (ArrayDescription) it.next();
                    NDArray nda = new ScratchNDArray( template );
                    Type type = nda.getType();
                    long npix = nda.getShape().getNumPixels();
                    BadHandler bh = nda.getBadHandler();
                    ChunkStepper cit = new ChunkStepper( npix );
                    Object array = type.newArray( cit.getSize() );
                    ArrayAccess acc = nda.getAccess();
                    for ( ; cit.hasNext(); cit.next() ) {
                        int size = cit.getSize();
                        fillRandom( array, -500, 500 );
                        if ( size > 1 ) {
                            bh.putBad( array, rand.nextInt( size ) );
                        }
                        acc.write( array, 0, size );
                    }
                    acc.close();
                    return nda;
                }
                catch ( IOException e ) {
                    fail( e.getMessage() );
                    return null;
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }


    /**
     * Iterates over a set of tiles relative to a given shape.  
     * A combination of overlapping, entirely within and entirely without
     * is given. 
     */
    private Iterator tileIterator( NDShape shape ) {
        List tlist = new ArrayList();

        int ndim = shape.getNumDims();
        long[] origin = shape.getOrigin();
        long[] dims = shape.getDims();
        long[] o;
        long[] d;

        // identical
        tlist.add( shape );

        // subset
        o = shape.getOrigin();
        d = shape.getDims();
        for ( int i = 0; i < ndim; i++ ) {
            o[ i ] = origin[ i ] + 2;
            d[ i ] = dims[ i ] - 4;
        }
        tlist.add( new NDShape( o, d ) );

        // superset
        o = shape.getOrigin();
        d = shape.getDims();
        for ( int i = 0; i < ndim; i++ ) {
            o[ i ] = origin[ i ] - 2;
            d[ i ] = dims[ i ] + 4;
        }
        tlist.add( new NDShape( o, d ) );
        
        // overlap
        o = shape.getOrigin();
        d = shape.getDims();
        for ( int i = 0; i < ndim; i++ ) {
            o[ i ] = origin[ i ] + 2;
        }
        tlist.add( new NDShape( o, d ) );
        
        // outside
        o = shape.getOrigin();
        d = shape.getDims();
        for ( int i = 0; i < ndim; i++ ) {
            o[ i ] = origin[ i ] + dims[ i ] + 2;
        }
        tlist.add( new NDShape( o, d ) );

        return tlist.iterator();
    }

    /**
     * Iterates over a set of ordered shapes relative to a given shape.
     * A combination of overlapping, entirely within and entirely without
     * as well as various pixel ordering schemes are given.
     */
    private Iterator oshapeIterator( NDShape shape ) {
        List oslist = new ArrayList();
        for ( Iterator it = tileIterator( shape ); it.hasNext(); ) {
            NDShape sh = (NDShape) it.next();
            oslist.add( new OrderedNDShape( sh, Order.COLUMN_MAJOR ) );
            oslist.add( new OrderedNDShape( sh, Order.ROW_MAJOR ) );
        }
        return oslist.iterator();
    }


}
