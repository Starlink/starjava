package uk.ac.starlink.fits;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Node;
import uk.ac.starlink.array.ArrayAccess;
import uk.ac.starlink.array.BadHandler;
import uk.ac.starlink.array.NDArray;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.array.OrderedNDShape;
import uk.ac.starlink.array.ScratchNDArray;
import uk.ac.starlink.array.Type;
import uk.ac.starlink.ndx.ArraysBulkDataImpl;
import uk.ac.starlink.ndx.BulkDataImpl;
import uk.ac.starlink.ndx.DefaultMutableNdx;
import uk.ac.starlink.ndx.MutableNdx;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxHandler;
import uk.ac.starlink.ndx.NdxIO;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.TestCase;

public class FitsNdxTest extends TestCase {

    private String tmpdir;

    public FitsNdxTest( String name ) {
        super( name );
    }

    public void setUp() {
        tmpdir = System.getProperty( "java.io.tmpdir" );
    }

    public void testHandler() throws TransformerException, IOException {
        NdxHandler handler = FitsNdxHandler.getInstance();

        /* Get an NdxIO and check we are installed there. */
        NdxIO ndxio = new NdxIO();
        List handlers = ndxio.getHandlers();
        boolean hasFitsHandler = false;
        for ( Iterator it = handlers.iterator(); it.hasNext(); ) {
            if ( it.next() instanceof FitsNdxHandler ) {
                hasFitsHandler = true;
            }
        }
        assertTrue( hasFitsHandler );

        /* Write the NDX out as XML. */
        Ndx ndx = virtualNdx();
        String xloc = tmpdir + "/" + "copy.xml";
        ndxio.outputNdx( xloc, ndx );
        Ndx xndx = ndxio.makeNdx( xloc );
        assertNdxEqual( ndx, xndx );

        /* Write the NDX out as a new FITS file. */
        ndxio.setHandlers( new NdxHandler[] { FitsNdxHandler.getInstance() } );
        String hloc = tmpdir + "/" + "copy.fits";
        ndxio.outputNdx( hloc, ndx );
        Ndx hndx = ndxio.makeNdx( hloc );
        assertNdxEqual( ndx, hndx );

    }

    private void assertNdxEqual( Ndx ndx1, Ndx ndx2 )
            throws IOException, TransformerException {
        assertTrue( NDArrays.equals( ndx1.getImage(), ndx2.getImage() ) );
        assertEquals( ndx1.hasVariance(), ndx2.hasVariance() );
        assertEquals( ndx1.hasQuality(), ndx2.hasQuality() );
        assertEquals( ndx1.hasTitle(), ndx2.hasTitle() );

        assertEquals( "Etc status", ndx1.hasEtc(), ndx2.hasEtc() );

        if ( ndx1.hasVariance() ) {
            assertTrue( NDArrays.equals( ndx1.getVariance(),
                                         ndx2.getVariance() ) );
        }
        if ( ndx1.hasQuality() ) {
            assertTrue( NDArrays.equals( ndx1.getQuality(),
                                         ndx2.getQuality() ) );
        }
        if ( ndx1.hasTitle() ) {
            assertEquals( ndx1.getTitle(), ndx2.getTitle() );
        }
        if ( ndx1.hasEtc() ) {
            StringWriter sw1 = new StringWriter();
            StringWriter sw2 = new StringWriter();
            SourceReader sr = new SourceReader();
            sr.setIncludeDeclaration( false );
            sr.setIndent( 0 );
            sr.writeSource( ndx1.getEtc(), sw1 );
            sr.writeSource( ndx2.getEtc(), sw2 );
            assertEquals( sw1.toString().replaceAll( "\\s", "" ),
                          sw2.toString().replaceAll( "\\s", "" ) );
        }

        // It's been through a FITS encoding - not easy to check WCS
        // FrameSets for equivalence.
        // assertEquals( ndx1.getWCS(), ndx2.getWCS() );
    }

    private Ndx virtualNdx() throws TransformerException, IOException {

        OrderedNDShape shape = new OrderedNDShape( new long[] { 1, 11, 21 },
                                                   new long[] { 10, 20, 30 },
                                                   null );
        Type type = Type.FLOAT;
        BadHandler bh = type.defaultBadHandler();
        NDArray im = new ScratchNDArray( shape, type, bh );
        NDArray var = new ScratchNDArray( shape, type, bh );
        BadHandler nbh = BadHandler.getHandler( Type.BYTE, null );
        NDArray qual = new ScratchNDArray( shape, Type.BYTE, nbh );
        int npix = (int) shape.getNumPixels();
        ArrayAccess iacc = im.getAccess();
        ArrayAccess vacc = var.getAccess();
        ArrayAccess qacc = qual.getAccess();
        assertTrue( iacc.isMapped() );
        assertTrue( vacc.isMapped() );
        assertTrue( qacc.isMapped() );
        float[] imap = (float[]) iacc.getMapped();
        float[] vmap = (float[]) vacc.getMapped();
        byte[] qmap = (byte[]) qacc.getMapped();
        byte qval = 0;
        fillCycle( imap, 0, 100 );
        for ( int i = 0; i < npix; i++ ) {
            vmap[ i ] = imap[ i ] * imap[ i ];
            qmap[ i ] = qval++;
        }
        iacc.close();
        vacc.close();
        qacc.close();
        BulkDataImpl datimp = new ArraysBulkDataImpl( im, var, qual );
        MutableNdx ndx = new DefaultMutableNdx( datimp );
        ndx.setTitle( "NDX for FITS testing" );
        ndx.setBadBits( (byte) 128 );
        String etcstr = 
              "<etc>"
            + "<weather><cloudy/><temperature value='cold'/></weather>"
            + "<location><altitude value='quite high'/></location>"
            + "</etc>";
        Node etc = new SourceReader()
                  .getDOM( new StreamSource( new StringReader( etcstr ) ) );
        ndx.setEtc( etc );
        return ndx;
    }
}
