package uk.ac.starlink.oldfits;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
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

public class FitsNDArrayTest extends TestCase {

    public FitsNDArrayTest( String name ) {
        super( name );
        Logger.getLogger( "uk.ac.starlink.oldfits" ).setLevel( Level.WARNING );
    }

    public void testBuilder() throws MalformedURLException, IOException {
        String tmpdir = System.getProperty( "java.io.tmpdir" );
        URL container = new URL( "file:" + tmpdir + "/newfits.fits" );

        ArrayBuilder fact = FitsArrayBuilder.getInstance();

        int hdu = 0;
        int xdim = 100;
        int ydim = 80;
        for ( Iterator tit = Type.allTypes().iterator(); tit.hasNext();
              hdu++ ) {
            Type type = (Type) tit.next();
            NDShape shape = 
                new NDShape( new long[] { 51, 101 },
                             new long[] { xdim--, ydim++ } );

            try {
                URL badurl = new URL( container, "#" + 10 );
                fact.makeNewNDArray( badurl, shape, type, null );
                fail();
            }
            catch ( IOException e ) {
                // OK
            }
         
            URL url = ( hdu == 0 ) ? container
                                   : new URL( container, "#" + hdu );
            NDArray nda1 = fact.makeNewNDArray( url, shape, type, null );
  
            int npix = (int) shape.getNumPixels();
            Object buf1 = type.newArray( npix );
            // fillCycle( buf1, 0, xdim );
            fillRandom( buf1, -100, 100 );

            ArrayAccess acc1 = nda1.getAccess();
            for ( ChunkStepper cit = new ChunkStepper( npix, 23 );
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

            assertTrue( shape.sameShape( shape2 ) );
            assertEquals( Order.COLUMN_MAJOR, order2 );
            assertEquals( type, type2 );
            assertEquals( type, bh2.getType() );

            Object buf2 = type.newArray( npix );
            ArrayAccess acc2 = nda2.getAccess();
            for ( ChunkStepper cit = new ChunkStepper( npix, 103 );
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
}
