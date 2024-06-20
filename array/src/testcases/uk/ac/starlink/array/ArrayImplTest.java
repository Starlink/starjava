package uk.ac.starlink.array;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import uk.ac.starlink.util.TestCase;

public class ArrayImplTest extends TestCase {

    private Random rand = new Random( 23 );

    public ArrayImplTest( String name ) {
        super( name );
    }

    public void testDeterministic() throws IOException {

        NDShape shape = new NDShape( new long[] { 3, 4, 5 },
                                     new long[] { 6, 7, 8 } );
        for ( Iterator it = Type.allTypes().iterator(); it.hasNext(); ) {
            Type type = (Type) it.next();
            ArrayImpl impl = new DeterministicArrayImpl( shape, type );
            assertTrue( ! impl.isWritable() );
            assertTrue( impl.isReadable() );
            assertTrue( impl.isRandom() );
            assertTrue( impl.multipleAccess() );
            exerciseReadableArrayImpl( impl );
        }
    }

    public void testNio() throws IOException {
        OrderedNDShape shape = new OrderedNDShape( new long[] { 1, 1 },
                                                   new long[] { 5, 10 },
                                                   Order.ROW_MAJOR );
        long npix = shape.getNumPixels();
        for ( Iterator it = Type.allTypes().iterator(); it.hasNext(); ) {
            Type type = (Type) it.next();
            Buffer buf = makeFilledNioBuf( type, (int) npix );
            ArrayImpl impl = new NioArrayImpl( buf, shape, type, 
                                               type.defaultBadValue() );
            assertTrue( impl.isWritable() );
            assertTrue( impl.isReadable() );
            assertTrue( impl.isRandom() );
            assertTrue( impl.multipleAccess() );
            exerciseReadableArrayImpl( impl );
        }
    }

    public void exerciseReadableArrayImpl( ArrayImpl impl ) throws IOException {
        assertTrue( impl.isReadable() );

        Number badval = impl.getBadValue();
        Type type = impl.getType();
        OrderedNDShape shape = impl.getShape();
        boolean isRandom = impl.isRandom();
        boolean isReadable = impl.isReadable();
        boolean isWritable = impl.isWritable();
        boolean isMulti = impl.multipleAccess();
        long npix = shape.getNumPixels();

        assertNotNull( type );
        assertNotNull( shape );
        if ( badval != null ) {
            assertEquals( type.defaultBadValue().getClass(),
                          badval.getClass() );
        }

        impl.open();

        AccessImpl accimpl = impl.getAccess();
        exerciseAccessImpl( accimpl, impl );
        accimpl.close();

        if ( isMulti ) {
            exerciseMultiArrayImpl( impl );
        }

        if ( impl.canMap() ) {
            exerciseMappableImpl( impl );
        }

        assertEquals( badval, impl.getBadValue() );
        assertEquals( type, impl.getType() );
        assertEquals( shape, impl.getShape() );
        assertEquals( isRandom, impl.isRandom() );
        assertEquals( isReadable, impl.isReadable() );
        assertEquals( isWritable, impl.isWritable() );
        assertEquals( isMulti, impl.multipleAccess() );

        impl.close();

        assertEquals( badval, impl.getBadValue() );
        assertEquals( type, impl.getType() );
        assertEquals( shape, impl.getShape() );
        assertEquals( isRandom, impl.isRandom() );
        assertEquals( isReadable, impl.isReadable() );
        assertEquals( isWritable, impl.isWritable() );
        assertEquals( isMulti, impl.multipleAccess() );
    }

    public void exerciseMultiArrayImpl( ArrayImpl arrimpl ) throws IOException {
        if ( arrimpl.isRandom() ) {
            exerciseMultiRandomArrayImpl( arrimpl );
        }

        long npix = arrimpl.getShape().getNumPixels();
        Type type = arrimpl.getType();
        BadHandler bh = makeBadHandler( type );
        Object buf1a = type.newArray( 1 );
        Object buf1b = type.newArray( 1 );
        AccessImpl acc1 = arrimpl.getAccess();
        AccessImpl acc2 = arrimpl.getAccess();
        for ( RandomChunkStepper cit = new RandomChunkStepper( npix );
              cit.hasNext(); cit.next() ) {
             int size = cit.getSize();
             long base = cit.getBase();

             // No-op - move one to current position
             acc1.setOffset( base );

             Object buf = type.newArray( size + 2 );
             bh.putBad( buf, 0 );
             bh.putBad( buf, size + 1 );
             acc1.read( buf, 1, size );
             assertTrue( bh.isBad( buf, 0 ) );
             assertTrue( bh.isBad( buf, size + 1 ) );
             for ( int i = 0; i < size; i++ ) {
                 acc2.read( buf1a, 0, 1 );
                 System.arraycopy( buf, i + 1, buf1b, 0, 1 );
                 assertArrayEquals( buf1a, buf1b );
             }
        }
        acc1.close();
        acc2.close();
    }

    public void exerciseMultiRandomArrayImpl( ArrayImpl arrimpl ) 
            throws IOException {
        
        long npix = arrimpl.getShape().getNumPixels();
        Type type = arrimpl.getType();
        BadHandler bh = makeBadHandler( type );
        Object buf1a = type.newArray( 1 );
        Object buf1b = type.newArray( 1 );
        AccessImpl acc1 = arrimpl.getAccess();
        AccessImpl acc2 = arrimpl.getAccess();
        for ( RandomChunkDispenser cdisp = new RandomChunkDispenser( npix );
              cdisp.hasNext(); cdisp.next() ) {
            long base = cdisp.getBase();
            int size = cdisp.getSize();
            Object buf = type.newArray( size + 2 );
            bh.putBad( buf, 0 );
            bh.putBad( buf, size + 1 );
            acc1.setOffset( base );
            acc1.read( buf, 1, size );
            assertTrue( bh.isBad( buf, 0 ) );
            assertTrue( bh.isBad( buf, size + 1 ) );
            for ( int i = size - 1; i >= 0; i-- ) {
                acc2.setOffset( base + i );
                acc2.read( buf1a, 0, 1 );
                System.arraycopy( buf, i + 1, buf1b, 0, 1 );
                assertArrayEquals( buf1a, buf1b );
            }
        }
        acc1.close();
        acc2.close();
    }

    public void exerciseAccessImpl( AccessImpl accimpl, 
                                    ArrayImpl arrimpl ) throws IOException {
        exerciseSerialReadableAccessImpl( accimpl, arrimpl );
        if ( arrimpl.isRandom() ) {
            exerciseRandomAccessImpl( accimpl, arrimpl );
        }
    }

    public void exerciseSerialReadableAccessImpl( AccessImpl accimpl, 
                                                  ArrayImpl arrimpl ) 
            throws IOException {
        long npix = arrimpl.getShape().getNumPixels();
        Type type = arrimpl.getType();
        BadHandler bh = makeBadHandler( type );
        boolean weirdBadValue = type.equals( Type.INT )
                             || type.equals( Type.FLOAT )
                             || type.equals( Type.DOUBLE );
        for ( RandomChunkStepper cit = new RandomChunkStepper( npix );
              cit.hasNext(); cit.next() ) {
            int size = cit.getSize();
            int start = rand.nextInt( size );
            long base = cit.getBase();
            accimpl.setOffset( base );  // nop
            Object buf = type.newArray( size * 2 );
            bh.putBad( buf, 0, size * 2 );
            accimpl.read( buf, start, size );
            for ( int i = 0; i < start; i++ ) {
                assertTrue( bh.isBad( buf, i ) );
            }
            for ( int i = start + size; i < size * 2; i++ ) {
                assertTrue( bh.isBad( buf, i ) );
            }
            boolean nochange = true;
            for ( int i = start; i < start + size; i++ ) {
                boolean same = bh.isBad( buf, i );
                if ( same && weirdBadValue ) {

                    // If a failure occurs here it means that either the 
                    // ArrayImpl is buggy, or a surprising value has 
                    // cropped up in the input data.  If the ArrayImpl is
                    // not at fault, you should change the test data
                    // (see makeBadHandler for the values to avoid).
                    fail( "Possibly not an error but looks suspicious: "
                        + "element " + ( cit.getBase() + i - start ) 
                        + " = " + bh.getBadValue() );
                }
                nochange = nochange && same;
            }
            if ( nochange && ! weirdBadValue ) {

                // If a failure occurs here it means that either the
                // ArrayImpl is buggy, or a sequence of identical slightly 
                // surprising values is present in the input data.
                // If the ArrayImpl is not at fault, you should change the
                // test data (see makeBadHandler for the values to avoid).
                String msg = "Possibly not an error, but looks suspicious: " 
                           + "all values " + cit.getBase() + ".." 
                           + ( cit.getBase() + cit.getSize() - 1 ) + " "
                           + "have value " + bh.getBadValue();
                fail( msg );
            }
        }
    }


    public void exerciseRandomAccessImpl( AccessImpl accimpl, 
                                          ArrayImpl arrimpl )
            throws IOException {
        long npix = arrimpl.getShape().getNumPixels();
        Type type = arrimpl.getType();
        BadHandler bh = makeBadHandler( type );
        Object buf1a = type.newArray( 1 );
        Object buf1b = type.newArray( 1 );
        for ( RandomChunkDispenser cdisp = new RandomChunkDispenser( npix );
              cdisp.hasNext(); cdisp.next() ) {
            long base = cdisp.getBase();
            int size = cdisp.getSize();
            Object buf = type.newArray( size + 2 );
            bh.putBad( buf, 0, 1 );
            bh.putBad( buf, size + 1, 1 );
            accimpl.setOffset( base );
            accimpl.read( buf, 1, size );
            assertTrue( bh.isBad( buf, 0 ) );
            assertTrue( bh.isBad( buf, size + 1 ) );
            for ( int i = size - 1; i >= 0; i-- ) {
                accimpl.setOffset( base + i );
                accimpl.read( buf1a, 0, 1 );
                System.arraycopy( buf, i + 1, buf1b, 0, 1 );
                assertArrayEquals( buf1a, buf1b );
            }
        }
    }

    public void exerciseMappableImpl( ArrayImpl arrimpl ) throws IOException {
        Object mapped = arrimpl.getMapped();
        long npix = arrimpl.getShape().getNumPixels();
        Type type = arrimpl.getType();
        if ( arrimpl.multipleAccess() ) {
            AccessImpl accimpl = arrimpl.getAccess();
            for ( ChunkStepper cit = new ChunkStepper( npix );
                  cit.hasNext(); cit.next() ) {
                int size = cit.getSize();
                int base = (int) cit.getBase();
                Object buf1 = type.newArray( size );
                Object buf2 = type.newArray( size );
                System.arraycopy( mapped, base, buf1, 0, size );
                // accimpl.setOffset( base );  // nop
                accimpl.read( buf2, 0, size );
                assertArrayEquals( buf1, buf2 );
            }
        }
    }

    /**
     * Get a bad value handler, preferably with a bad value which is not
     * likely to crop up much in the data.  In particular, the standard
     * floating NaN value is not used.
     */
    private BadHandler makeBadHandler( Type type ) {
        if ( type.equals( Type.BYTE ) ) {
            return BadHandler.getHandler( type, Byte.valueOf( (byte) -126 ) );
        }
        else if ( type.equals( Type.SHORT ) ) {
            return BadHandler.getHandler( type,
                                          Short.valueOf( (short) -32766 ) );
        }
        else if ( type.equals( Type.INT ) ) {
            return BadHandler.getHandler( type,
                                          Integer.valueOf( -2147483646 ) );
        }
        else if ( type.equals( Type.FLOAT ) ) {

            // Make a non-standard NaN value.
            String bits = 
                  "1"                             // sign
                + "11111111"                      // exponent (= NaN flag)
                + "01010101010101010101010";      // non-standard NaN
            assertEquals( 32, bits.length() );
            final float badval = 
                Float.intBitsToFloat( new BigInteger( bits, 2 ).intValue() );
            assertTrue( Float.isNaN( badval ) );
            assertTrue( Float.floatToRawIntBits( badval ) !=
                        Float.floatToRawIntBits( Float.NaN ) );

            // Return a bad value handler which recognises only that as bad.
            return new BadHandler( Type.FLOAT, Float.valueOf( badval ) ) {
                private int pattern = Float.floatToRawIntBits( badval );
                public boolean isBad( Object array, int pos ) {
                    float val = ((float[]) array)[ pos ];
                    return Float.floatToRawIntBits( val ) == pattern;
                }
                public void putBad( Object array, int pos ) {
                    ((float[]) array)[ pos ] = badval;
                }
                public void putBad( Object array, int start, int size ) {
                    Arrays.fill( (float[]) array, start, start+size, badval );
                }
                public Number makeNumber( Object array, int pos ) {
                    float val = ((float[]) array)[ pos ];
                    return isBad( array, pos ) ? null : Float.valueOf( val );
                }
                public BadHandler.ArrayHandler arrayHandler( final Object ar ) {
                    return new BadHandler.ArrayHandler() {
                        float[] array = (float[]) ar;
                        public boolean isBad( int pos ) {
                            float val = array[ pos ];
                            return Float.floatToRawIntBits( val ) == pattern;
                        }
                        public void putBad( int pos ) {
                            array[ pos ] = badval;
                        }
                    };
                }
            };
        }
        else if ( type.equals( Type.DOUBLE ) ) {

            // Make a non-standard NaN value.
            String bits = 
                "1"                               // sign
              + "11111111111"                     // exponent (= NaN flag)
              + "01010101010101010101010101"      // non-standard NaN
              + "01010101010101010101010101";
            assertEquals( 64, bits.length() );
            final double badval = 
                Double.longBitsToDouble( new BigInteger( bits, 2 )
                                        .longValue() );
            assertTrue( Double.isNaN( badval ) );
            assertTrue( Double.doubleToRawLongBits( badval ) !=
                        Double.doubleToRawLongBits( Double.NaN ) );

            // Return a bad value handler which recognises only that as bad.
            return new BadHandler( Type.DOUBLE, Double.valueOf( badval ) ) {
                private long pattern = Double.doubleToRawLongBits( badval );
                public boolean isBad( Object array, int pos ) {
                    double val = ((double[]) array)[ pos ];
                    return Double.doubleToRawLongBits( val ) == pattern;
                }
                public void putBad( Object array, int pos ) {
                    ((double[]) array)[ pos ] = badval;
                }
                public void putBad( Object array, int start, int size ) {
                    Arrays.fill( (double[]) array, start, start+size, badval );
                }
                public Number makeNumber( Object array, int pos ) {
                    double val = ((double[]) array)[ pos ];
                    return isBad( array, pos ) ? null : Double.valueOf( val );
                }
                public BadHandler.ArrayHandler arrayHandler( final Object ar ) {
                    return new BadHandler.ArrayHandler() {
                        double[] array = (double[]) ar;
                        public boolean isBad( int pos ) {
                            double val = array[ pos ];
                            return Double.doubleToRawLongBits( val ) == pattern;
                        }
                        public void putBad( int pos ) {
                            array[ pos ] = badval;
                        }
                    };
                }
            };
        }
        else {
            fail( "Unknown type" );
            return null;
        }
    }

    Buffer makeFilledNioBuf( Type type, int size ) {
        if ( type == Type.BYTE ) {
            byte[] array = new byte[ size ];
            fillRandom( array, -128, 127 );
            return ByteBuffer.wrap( array );
        }
        else if ( type == Type.SHORT ) {
            short[] array = new short[ size ];
            fillRandom( array, -32768, 32767 );
            return ShortBuffer.wrap( array );
        }
        else if ( type == Type.INT ) {
            int[] array = new int[ size ];
            fillRandom( array, -1000, 1000 );
            return IntBuffer.wrap( array );
        }
        else if ( type == Type.FLOAT ) {
            float[] array = new float[ size ];
            fillRandom( array, -1e5, 1e5 );
            return FloatBuffer.wrap( array );
        }
        else if ( type == Type.DOUBLE ) {
            double[] array = new double[ size ];
            fillRandom( array, -1e5, 1e5 );
            return DoubleBuffer.wrap( array );
        }
        else {
            fail( "Unknown type" );
            return null;
        }
    }
}
