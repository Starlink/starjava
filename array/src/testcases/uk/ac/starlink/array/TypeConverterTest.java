package uk.ac.starlink.array;

import java.util.Iterator;
import uk.ac.starlink.util.TestCase;

public class TypeConverterTest extends TestCase {

    private int npix = 1000;

    public TypeConverterTest( String name ) {
        super( name );
    }

    public void testConversionSame() {
        for ( Iterator it = Type.allTypes().iterator(); it.hasNext(); ) {
            exerciseSame( (Type) it.next() );
        }
    }

    public void testConversionDown() {
        Type t1 = Type.DOUBLE;
        double[] a1 = new double[ npix ];
        BadHandler bh1 = nonNullHandler( t1 );
        fillRandom( a1, -50, 50 );
        double tooBig = 1e100;
        a1[ 0 ] = tooBig;
        bh1.putBad( a1, 1 );
        for ( Iterator it = Type.allTypes().iterator(); it.hasNext(); ) {
            Type t2 = (Type) it.next();
            if ( t2 != Type.DOUBLE ) {
                BadHandler bh2 = makeBadHandler( t2, 99. );
                TypeConverter tc = new TypeConverter( t1, bh1, t2, bh2, null );
                Object a2 = t2.newArray( npix );
                tc.convert12( a1, 0, a2, 0, npix );
                assertTrue( bh2.isBad( a2, 0 ) );
                assertTrue( bh2.isBad( a2, 1 ) );
                for ( int i = 2; i < npix; i++ ) {
                    assertEquals( a1[ i ],
                                  bh2.makeNumber( a2, i ).intValue(), 1.0 );
                }
                double[] a1back = new double[ npix ];
                a1back[ 0 ] = tooBig;
                tc.convert21( a2, 1, a1back, 1, npix - 1 );
                assertArrayEquals( a1, a1back, 1. );
            }
        }
    }

    public void testConversionUp() {
        Type t1 = Type.BYTE;
        byte[] a1 = new byte[ npix ];
        BadHandler bh1 = nonNullHandler( t1 );
        fillRandom( a1, -50, 50 );
        bh1.putBad( a1, 0 );
        for ( Iterator it = Type.allTypes().iterator(); it.hasNext(); ) {
            Type t2 = (Type) it.next();
            if ( t2 != Type.BYTE ) {
                BadHandler bh2 = nonNullHandler( t2 );
                TypeConverter tc = new TypeConverter( t1, bh1, t2, bh2, null );
                Object a2 = t2.newArray( npix );
                tc.convert12( a1, 0, a2, 0, npix );
                assertTrue( bh2.isBad( a2, 0 ) );
                for ( int i = 1; i < npix; i++ ) {
                    assertEquals( a1[ i ], 
                                  bh2.makeNumber( a2, i ).byteValue() );
                }
                byte[] a1back = new byte[ npix ];
                tc.convert21( a2, 0, a1back, 0, npix );
                assertArrayEquals( a1, a1back );
            }
        }
    }

    public void testConversionFunc() {
        Function doubler = new Function() {
            public double forward( double x ) { return x * 2.0; }
            public double inverse( double y ) { return y / 2.0; }
        };

        int np = 5;
        BadHandler bhShort = nonNullHandler( Type.SHORT );
        BadHandler bhByte = nonNullHandler( Type.BYTE );
        BadHandler bhFloat = nonNullHandler( Type.FLOAT );

        short badShort = bhShort.getBadValue().shortValue();
        byte badByte = bhByte.getBadValue().byteValue();
        float badFloat = bhFloat.getBadValue().floatValue();

        Converter sbc = new TypeConverter( Type.SHORT, bhShort,
                                           Type.BYTE, bhByte, doubler );
        Converter sfc = new TypeConverter( Type.SHORT, bhShort,
                                           Type.FLOAT, bhFloat, doubler );

        short[] array = new short[] { 99, 2, 100, badShort, 99 };

        byte[] byteBuf = new byte[ 5 ];
        sbc.convert12( array, 1, byteBuf, 1, 3 );
        assertArrayEquals( new byte[] { 0, 4, badByte, badByte, 0 }, byteBuf );

        float[] floatBuf = new float[ 5 ];
        sfc.convert12( array, 1, floatBuf, 1, 3 );
        assertArrayEquals( new float[] { 0.0f, 4.0f , 200.0f, badFloat, 0.0f },
                           floatBuf );
     
    }

    public void exerciseSame( Type t ) {

        BadHandler bh1 = nonNullHandler( t );
        TypeConverter tc = new TypeConverter( t, bh1, t, bh1 );
        assertEquals( bh1, tc.getBadHandler1() );
        assertEquals( bh1, tc.getBadHandler2() );
        assertEquals( t, tc.getType1() );
        assertEquals( t, tc.getType2() );
        assertTrue( tc.isUnit12() );
        assertTrue( tc.isUnit21() );

        Object a1 = t.newArray( npix );
        Object a2 = t.newArray( npix );
        fillRandom( a1, -1e4, 1e4 );
        bh1.putBad( a1, 10, 20 );
        tc.convert12( a1, 0, a2, 0, npix );
        assertArrayEquals( a1, a2 );
        tc.convert21( a1, 0, a2, 0, npix );
        assertArrayEquals( a1, a2 );

        BadHandler bh2 = makeBadHandler( t, 99. );
        assertTrue( ! bh1.equals( bh2 ) );
        tc = new TypeConverter( t, bh1, t, bh2 );
        assertEquals( bh1, tc.getBadHandler1() );
        assertEquals( bh2, tc.getBadHandler2() );
        assertEquals( t, tc.getType1() );
        assertEquals( t, tc.getType2() );
        assertTrue( ! tc.isUnit12() );
        assertTrue( ! tc.isUnit21() );
 
        Object b1 = t.newArray( npix );
        Object b2 = t.newArray( npix );
        bh1.putBad( b1, 10, 20 );
        tc.convert12( b1, 0, b2, 0, npix );
        assertArrayNotEquals( b1, b2 );
        bh2.putBad( b1, 10, 20 );
        assertArrayEquals( b1, b2 );
    }

    private static BadHandler makeBadHandler( Type type, double number ) {
        Number badval = null;
        if ( type == Type.BYTE ) {
            badval = Byte.valueOf( (byte) number );
        }
        else if ( type == Type.SHORT ) {
            badval = Short.valueOf( (short) number );
        }
        else if ( type == Type.INT ) {
            badval = Integer.valueOf( (int) number );
        }
        else if ( type == Type.FLOAT ) {
            badval = Float.valueOf( (float) number );
        }
        else if ( type == Type.DOUBLE ) {
            badval = Double.valueOf( (double) number );
        }
        return BadHandler.getHandler( type, badval );
    }

    /**
     * Same as Type.defaultBadHandler except for Type.BYTE.
     */
    private static BadHandler nonNullHandler( Type type ) {
        return BadHandler.getHandler( type, type.defaultBadValue() );
    }

}
