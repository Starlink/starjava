package uk.ac.starlink.hds;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.ArrayBuilder;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.ChunkStepper;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDShape;
import uk.ac.starlink.array.Order;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.util.TestCase;

public class HDSNDArrayTest extends TestCase {

    public HDSNDArrayTest( String name ) {
        super( name );
    }

    public void testBuilder() throws MalformedURLException, IOException {
        String tmpdir = System.getProperty( "java.io.tmpdir" );
        URL url = new URL( "file:" + tmpdir + "/newarray.sdf" );

        ArrayBuilder fact = HDSArrayBuilder.getInstance();

        NDShape shape = new NDShape( new long[] { 101, 201 },
                                     new long[] { 10, 20 } );
        int npix = (int) shape.getNumPixels();
        Type type = Type.FLOAT;
        HDSType htype = HDSType.fromJavaType( type );
        assertNotNull( htype );
        Number hbad = htype.getBadValue();
        assertNotNull( hbad );

        NDArray nda1 = fact.makeNewNDArray( url, shape, type, null );

        OrderedNDShape shape1 = nda1.getShape();
        Order order1 = shape1.getOrder();
        Type type1 = nda1.getType();
        BadHandler bh1 = nda1.getBadHandler();
        Number badval1 = bh1.getBadValue();

        assertTrue( shape.sameShape( shape1 ) );
        assertEquals( Order.COLUMN_MAJOR, order1 );
        assertEquals( type, type1 );
        assertEquals( type, bh1.getType() );
        assertEquals( hbad, badval1 );

        Object buf1 = type.newArray( npix );
        fillRandom( buf1, -100, 100 );
        
        ArrayAccess acc1 = nda1.getAccess();
        for ( ChunkStepper cit = new ChunkStepper( npix, 10 ); 
              cit.hasNext(); cit.next() ) {
            acc1.write( buf1, (int) cit.getBase(), cit.getSize() );
        }
        acc1.close();
        nda1.close();

        NDArray nda2 = fact.makeNDArray( url, AccessMode.READ );

        OrderedNDShape shape2 = nda2.getShape();
        Order order2 = shape2.getOrder();
        Type type2 = nda2.getType();
        BadHandler bh2 = nda2.getBadHandler();
        Number badval2 = bh2.getBadValue();

        assertTrue( shape.sameShape( shape2 ) );
        assertEquals( Order.COLUMN_MAJOR, order2 );
        assertEquals( type, type2 );
        assertEquals( type, bh2.getType() );
        assertEquals( hbad, badval2 );

        Object buf2 = type.newArray( npix );
        ArrayAccess acc2 = nda2.getAccess();
        for ( ChunkStepper cit = new ChunkStepper( npix, 14 );
              cit.hasNext(); cit.next() ) {
            acc2.read( buf2, (int) cit.getBase(), cit.getSize() );
        }
        acc2.close();
        nda2.close();
        
        assertArrayEquals( buf1, buf2 );
        bh2.putBad( buf2, 21 );
        assertArrayNotEquals( buf1, buf2 );
    }
}
