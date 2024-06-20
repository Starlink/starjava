package uk.ac.starlink.array;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import uk.ac.starlink.util.TestCase;

public class TypeTest extends TestCase {

    public TypeTest( String name ) {
        super( name );
    }

    public void testTypeSet() {

        // Check we know what all the types are.  If this fails because
        // a new type has been added, it is likely that lots of if/then
        // chains in the array classes will need to be updated.
        Set types = new HashSet();
        types.add( Type.BYTE );
        types.add( Type.SHORT );
        types.add( Type.INT );
        types.add( Type.FLOAT );
        types.add( Type.DOUBLE );
        assertEquals( new HashSet( Type.allTypes() ), types );
    }

    public void testCheckArray() {
        Type t = Type.BYTE;
        t.checkArray( new byte[ 10 ], 0 );
        t.checkArray( new byte[ 10 ], 10 );
        try { t.checkArray( new byte[ 10 ], 11 ); fail(); }
            catch ( IllegalArgumentException e ) {}
        try { t.checkArray( new short[ 10 ], 9 ); fail(); }
            catch ( IllegalArgumentException e ) {}
        try { t.checkArray( new Object(), 0 ); fail(); }
            catch ( IllegalArgumentException e ) {}
        try { t.checkArray( new Object[ 10 ], 9 ); fail(); }
            catch ( IllegalArgumentException e ) {}
    }

    public void testNewArray() {
        assertEquals( byte.class,
                      Type.BYTE.newArray( 0 ).getClass().getComponentType() );
        assertEquals( short.class,
                      Type.SHORT.newArray( 1 ).getClass().getComponentType() );
        assertEquals( int.class,
                      Type.INT.newArray( 2 ).getClass().getComponentType() );
        assertEquals( float.class,
                      Type.FLOAT.newArray( 3 ).getClass().getComponentType() );
        assertEquals( double.class,
                      Type.DOUBLE.newArray( 4 ).getClass().getComponentType() );
        assertEquals( 99, Array.getLength( Type.INT.newArray( 99 ) ) );
    }

    public void testBad() {
        Number badByte = Byte.valueOf( Byte.MIN_VALUE );
        Number badShort = Short.valueOf( Short.MIN_VALUE );
        Number badInt = Integer.valueOf( Integer.MIN_VALUE );
        Number badFloat = Float.valueOf( Float.NaN );
        Number badDouble = Double.valueOf( Double.NaN );

        assertEquals( (byte) -128, badByte.byteValue() );
        assertEquals( (short) -32768, badShort.shortValue() );
        assertEquals( -2147483648, badInt.intValue() );

        assertEquals( badByte, Type.BYTE.defaultBadValue() );
        assertEquals( badShort, Type.SHORT.defaultBadValue() );
        assertEquals( badInt, Type.INT.defaultBadValue() );
        assertEquals( badFloat, Type.FLOAT.defaultBadValue() );
        assertEquals( badDouble, Type.DOUBLE.defaultBadValue() );

        assertEquals( BadHandler.getHandler( Type.BYTE, null ),
                      Type.BYTE.defaultBadHandler() );
        assertEquals( BadHandler.getHandler( Type.SHORT, badShort ),
                      Type.SHORT.defaultBadHandler() );
        assertEquals( BadHandler.getHandler( Type.INT, badInt ),
                      Type.INT.defaultBadHandler() );
        assertEquals( BadHandler.getHandler( Type.FLOAT, badFloat ),
                      Type.FLOAT.defaultBadHandler() );
        assertEquals( BadHandler.getHandler( Type.DOUBLE, badDouble ),
                      Type.DOUBLE.defaultBadHandler() );
    }

    public void testCharacteristics() {
        assertEquals( 1, Type.BYTE.getNumBytes() );
        assertEquals( 2, Type.SHORT.getNumBytes() );
        assertEquals( 4, Type.INT.getNumBytes() );
        assertEquals( 4, Type.FLOAT.getNumBytes() );
        assertEquals( 8, Type.DOUBLE.getNumBytes() );

        assertTrue( ! Type.BYTE.isFloating() );
        assertTrue( ! Type.SHORT.isFloating() );
        assertTrue( ! Type.INT.isFloating() );
        assertTrue( Type.FLOAT.isFloating() );
        assertTrue( Type.DOUBLE.isFloating() );

        assertEquals( byte.class, Type.BYTE.javaClass() );
        assertEquals( short.class, Type.SHORT.javaClass() );
        assertEquals( int.class, Type.INT.javaClass() );
        assertEquals( float.class, Type.FLOAT.javaClass() );
        assertEquals( double.class, Type.DOUBLE.javaClass() );
    }

    public void testMinMax() {
        for ( Iterator it = Type.allTypes().iterator(); it.hasNext(); ) {
            Type type = (Type) it.next();
            assertTrue( type.minimumValue() < type.maximumValue() );
            assertTrue( ! ( type.minimumValue() > type.maximumValue() ) );
        }
    }

    public void testGetType() {
        assertEquals( Type.getType( byte.class ), Type.BYTE );
        assertEquals( Type.getType( short.class ), Type.SHORT );
        assertEquals( Type.getType( int.class ), Type.INT );
        assertEquals( Type.getType( float.class ), Type.FLOAT );
        assertEquals( Type.getType( double.class ), Type.DOUBLE );
        assertNull( Type.getType( long.class ) );
        assertNull( Type.getType( HashSet.class ) );
        assertNull( Type.getType( null ) );
    }

}
