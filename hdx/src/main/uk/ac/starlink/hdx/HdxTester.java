package uk.ac.starlink.hdx;

import java.io.*;
import java.net.*;
import uk.ac.starlink.hds.*;
import uk.ac.starlink.hdx.array.*;
import uk.ac.starlink.util.Tester;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

class HdxTester extends Tester {

    private URL ndxurl;

    public static void main( String[] args ) {
        if ( args.length == 0 ) {
            throw new Error( "usage: HdxTester ndf [ndf] ..." );
        }
        for ( int i = 0; i < args.length; i++ ) {
            HdxTester tester = new HdxTester( args[ i ] );
            tester.doTest();
        }
    }

    private HdxTester( String ndxstr ) {
         try {
             this.ndxurl = makeURL( ndxstr );
         }
         catch ( MalformedURLException e ) {
             throw new Error( e.getMessage() );
         }
    }

    public void testScript() throws Throwable {
        testHdx();
        testDefaultAccess();
        testSimpleFuncAccess();
    }

    private void testHdx() throws Exception {
        logMessage( "Hdx" );
        NdxImpl impl1 = new HdsNdxImpl( ndxurl, "READ" );
        Ndx ndx1 = new BridgeNdx( impl1 );
        Element el = ndx1.toDOM();
        Source xsrc = new DOMSource( el );
        Result xres = new StreamResult( System.out );
        Transformer trans = TransformerFactory.newInstance().newTransformer();
        trans.setOutputProperty( OutputKeys.INDENT, "yes" );
        // trans.transform( xsrc, xres );

        org.dom4j.Element el4j = toDom4j( el );
        NdxImpl impl2 = new DomNdxImpl( el4j );
        Ndx ndx2 = new BridgeNdx( impl2 );
        assertEqual( "" + ndx1.getImage(), "" + ndx2.getImage() );
        assertEqual( "" + ndx1.getVariance(), "" + ndx2.getVariance() );
        assertEqual( "" + ndx1.getQuality(), "" + ndx2.getQuality() );
    }

    private void testDefaultAccess() throws Exception {
        logMessage( "DefaultAccess" );
        Ndx ndx1 = new BridgeNdx( new HdsNdxImpl( ndxurl, "READ" ) );
        assertTrue( ndx1.isPersistent() );
        NDShape shape = ndx1.getImage().getShape();
        long[] origin = shape.getOrigin();
        long[] dims = shape.getDims();
        for ( int i = 0; i < shape.getNumDims(); i++ ) {
            origin[ i ] += 5;
            dims[ i ] -= 10;
        }
        NDShape window = new NDShape( origin, dims );
        int npix = (int) window.getNumPixels();
        Requirements req = new Requirements( Requirements.Mode.READ )
                          .setWindow( window );
        NdxAccess access1 = ndx1.getAccess( req, true, false );
        Type type = access1.getType();
        boolean hasVar = access1.hasVariance();
        Object image1 = type.newArray( npix );
        Object var1 = hasVar ? type.newArray( npix ) : null;
        access1.read( image1, var1, null, 0, npix );
        access1.close();
        
        Ndx ndx2 = new BridgeNdx( new HdsNdxImpl( ndxurl, "READ" ) );
        NdxAccess access2 = ndx2.getAccess( null, true, false );
        Object image2 = type.newArray( npix );
        Object var2 = hasVar ? type.newArray( npix ) : null;
        access2.readTile( image2, var2, null, window );
        access2.close();

        assertEqual( image1, image2 );
        if ( hasVar ) {
            assertEqual( var1, var2 );
        }
    }

    private void testSimpleFuncAccess() throws Exception {
        logMessage( "SimpleFuncAccess" );

        Requirements req = new Requirements( Requirements.Mode.READ )
                          .setType( Type.INT );
        double factor = 2.0;

        Ndx ndx1 = new BridgeNdx( new HdsNdxImpl( ndxurl, "READ" ) );
        NdxAccess access1 = ndx1.getAccess( req, true, false );
        BadHandler handler1 = access1.getBadHandler();
        boolean hasVar = access1.hasVariance();

        Ndx ndx2 = new BridgeNdx( new HdsNdxImpl( ndxurl, "READ" ) );
        NdxAccess access2 = ndx2.getAccess( req, hasVar, false );
        access2 = new SimpleFuncAccess( access2, new MultFunction( factor ) );
        BadHandler handler2 = access2.getBadHandler();

        ChunkIterator cIt = 
            new ChunkIterator( access1.getShape().getNumPixels() );
        int bsize = cIt.getSize();
        int[] im1 = new int[ bsize ];
        int[] im2 = new int[ bsize ];
        int[] var1 = hasVar ? new int[ bsize ] : null;
        int[] var2 = hasVar ? new int[ bsize ] : null;
        for ( ; cIt.hasNext(); cIt.next() ) {
            int size = cIt.getSize();
            access1.read( im1, var1, null, 0, size );
            access2.read( im2, var2, null, 0, size );
            for ( int i = 0; i < size; i++ ) {
                if ( ! handler1.isBad( im1, i ) &&
                     ! handler2.isBad( im2, i )  ) {
                    assertEqual( im1[ i ] * factor, im2[ i ] );
                }
                if ( hasVar && ! handler1.isBad( var1, i ) 
                            && ! handler2.isBad( var2, i ) ) {
                    assertEqual( var1[ i ] * factor * factor, var2[ i ] );
                }
            }
        }
        access1.close();
        access2.close();
    }

    private static URL makeURL( String loc ) throws MalformedURLException {
        URL context = new File( "." ).toURI().toURL();
        return new URL( context, loc );
    }

    /**
     * Gets a dom4j Element from a w3c Element - currently only works if
     * the element in question is the root element of a document 
     * (the transformation is not as easy as you might think).
     */
    private static org.dom4j.Element toDom4j( org.w3c.dom.Element el3c )
            throws Exception {
        org.dom4j.Element el4j;
        org.w3c.dom.Document doc3c = el3c.getOwnerDocument();
        org.dom4j.io.DOMReader reader4j = new org.dom4j.io.DOMReader();
        org.dom4j.Document doc4j = reader4j.read( doc3c );
        el4j = doc4j.getRootElement();
        return el4j;
    }


    private static class MultFunction implements SimpleNdxFunction {
        private double factor;
        private double factor2;
        MultFunction( double factor ) {
            this.factor = factor;
            this.factor2 = factor * factor;
        }
        public void forward( double[] image, double[] variance, byte[] quality, 
                             int start, int size ) {
            while ( size-- > 0 ) {
                image[ start ] *= factor;
                if ( variance != null ) {
                    variance[ start ] *= factor2;
                }
                start++;
            }
        }
        public void inverse( double[] image, double[] variance, byte[] quality,
                             int start, int size ) {
            while ( size-- > 0 ) {
                image[ start ] /= factor;
                if ( variance != null ) {
                    variance[ start ] /= factor2;
                }
                start++;
            }
        }
    }

}
