package uk.ac.starlink.hds;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerException;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.hdx.HdxContainer;
import uk.ac.starlink.hdx.HdxFactory;
import uk.ac.starlink.hdx.HdxResourceType;
import uk.ac.starlink.ndx.BridgeNdx;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxHandler;
import uk.ac.starlink.ndx.NdxIO;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.TestCase;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class NDFNdxTest extends TestCase {

    public static String NDF_FILE = "uk/ac/starlink/hds/reduced_data1.sdf";
    public static String containerName;
    public static File containerFile;
    public static URL ndfURL;
    public static String tmpdir;

    public void setUp() throws MalformedURLException, IOException {
        if ( containerFile == null ) {
            tmpdir = System.getProperty( "java.io.tmpdir" );
            containerName = tmpdir + File.separatorChar + "test_ndf";
            containerFile = new File( containerName + ".sdf" );
	    ndfURL = containerFile.toURI().toURL();
            InputStream istrm = getClass()
                               .getClassLoader()
                               .getResourceAsStream( NDF_FILE );
            assertNotNull( "Failed to open " + NDF_FILE, istrm );
            OutputStream ostrm = new FileOutputStream( containerFile );

            istrm = new BufferedInputStream( istrm );
            ostrm = new BufferedOutputStream( ostrm );
            int b;
            while ( ( b = istrm.read() ) >= 0 ) {
                ostrm.write( b );
            }
            istrm.close();
            ostrm.close();
        }

        if (false) {
            // Set the hdx logger and its associated handlers to log everything
            java.util.logging.Logger logger
                    = java.util.logging.Logger.getLogger("uk.ac.starlink.hdx");
            logger.setLevel(java.util.logging.Level.ALL);
            for (java.util.logging.Logger tl=logger;
                 tl!=null;
                 tl=tl.getParent()) {
                java.util.logging.Handler[] h = tl.getHandlers();
                for (int i=0; i<h.length; i++)
                    h[i].setLevel(java.util.logging.Level.FINE);
            }
        }
    }


    public NDFNdxTest( String name ) {
        super( name );
    }

    public void testHandler() throws Exception {

        NdxHandler handler = NDFNdxHandler.getInstance();

        /* Read the NDF as an NDX. */
        Ndx ndx = handler.makeNdx( ndfURL, AccessMode.READ );

        /* Get an NdxIO and check we are installed there. */
        NdxIO ndxio = new NdxIO();
        List handlers = ndxio.getHandlers();
        boolean hasNDFHandler = false;
        for ( Iterator it = handlers.iterator(); it.hasNext(); ) {
            if ( it.next() instanceof NDFNdxHandler ) {
                hasNDFHandler = true;
            }
        }
        assertTrue( hasNDFHandler );

        /* Write the NDX out as XML. */
        String xloc = tmpdir + "/" + "copy.xml";
        ndxio.outputNdx( xloc, ndx );
        Ndx xndx = ndxio.makeNdx( xloc, AccessMode.READ );
        assertNdxEqual( ndx, xndx );

        /* Compare direct generation of XML from HDX, with the XML in xloc */
        HdxContainer hdx = HdxFactory.getInstance().newHdxContainer( ndx.getHdxFacade() );
        assertDOMEquals( new URL( "file:"+xloc ),
                         hdx.getDOM( null ),
                         "xloc-vs-hdxdom:",
                         IGNORE_WHITESPACE );
        assertSourceEquals( new StreamSource( new FileInputStream( xloc ) ),
                            hdx.getSource( null ),
                            "xloc-vs-hdxsource:",
                            IGNORE_WHITESPACE );
        /*
         * Don't check what ndx.getHdxFacade().getSource(?) produces,
         * since that is not the preferred way of generating XML, and
         * it might become protected in future.
         */

        /* Write the NDX out as an NDF. */
        ndxio.setHandlers( new NdxHandler[] { NDFNdxHandler.getInstance() } );
        String hloc = tmpdir + "/" + "copy.sdf";
        ndxio.outputNdx( hloc, ndx );
        Ndx hndx = ndxio.makeNdx( hloc, AccessMode.READ );
        assertNdxEqual( ndx, hndx );
    }

    public void testHdsAsHdx(  )
            throws Exception {
        // Make an Hdx from an NDF
        HdxResourceType ndxtype = BridgeNdx.getHdxResourceType(  );
        HdxContainer hdx = HdxFactory
                .getInstance(  )
                .newHdxContainer( ndfURL );
        assertNotNull( hdx );
        // check the Ndx we get from the Hdx is equal to the Ndx we
        // get from NDFNdxHandler
        Object ndx = hdx.get( ndxtype );
        assertNotNull( ndx );
        assertTrue( ndxtype.getConstructedClass().isInstance( ndx ) );
        Ndx rawndx = NDFNdxHandler.getInstance().makeNdx( ndfURL,
                                                          AccessMode.READ );
        assertNotNull( rawndx );
        assertNdxEqual( (Ndx)ndx, rawndx );

        // ...and that the generated DOM is sane

        // The output of hdx.getDOM( null ) is rather elaborate, so we
        // don't have a DOM we can usefully compare it with.  Do basic
        // checks.
        Element hdxel = hdx.getDOM( null );
        assertEquals( "hdx", hdxel.getTagName() );
        Element ndxel = (Element) hdxel.getFirstChild( );
        assertEquals( "ndx", ndxel.getTagName() );
        assertNull( ndxel.getNextSibling() );
        String[] kidnames = new String[] {
            "title", "image", "variance", "wcs", "etc" 
        };
        Node kid = ndxel.getFirstChild();
        for ( int i = 0; i < kidnames.length; i++ ) {
            assertNotNull( kid );
            assertEquals( Node.ELEMENT_NODE, kid.getNodeType() );
            assertEquals( kidnames[i], kid.getNodeName() );
            kid = kid.getNextSibling();
        }
        assertNull( kid );
    }

    public void testHdxXML()
            throws Exception {
        // Make an Hdx from an XMLfile which references an NDF
        HdxResourceType ndxtype = BridgeNdx.getHdxResourceType();
        HdxContainer hdx = HdxFactory
                .getInstance()
                .newHdxContainer( this.getClass().getResource( "no-ns.xml" ) );
        assertNotNull( hdx );
        Object ndx = hdx.get( ndxtype );
        
        assertNotNull( ndx );
        assertTrue( ndxtype.getConstructedClass().isInstance( ndx ) );
        assertNotNull( ((Ndx)ndx).getImage() );
        Ndx rawndx = NDFNdxHandler.getInstance(  ).makeNdx( ndfURL,
                                                            AccessMode.READ );
        assertNotNull( rawndx );
        assertNdxEqual( (Ndx)ndx, rawndx );
    }

    private void assertNdxEqual( Ndx ndx1, Ndx ndx2 )
            throws IOException, TransformerException {
        assertNotNull( ndx1.getImage() ); // damn-well better have an image
        assertNotNull( ndx2.getImage() );
        assertTrue( NDArrays.equals( ndx1.getImage(), ndx2.getImage() ) );
        assertTrue( ndx1.hasVariance() == ndx2.hasVariance() );
        assertTrue( ndx1.hasQuality() == ndx2.hasQuality() );
        assertTrue( ndx1.hasTitle() == ndx2.hasTitle() );

        // etc not implemented yet for NDFs
        // assertTrue( "Etc status", ndx1.hasEtc() == ndx2.hasEtc() );

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
        if ( ndx2.hasEtc() ) {
            assertSourceEquals( ndx1.getEtc(), ndx2.getEtc(),
                                null, IGNORE_WHITESPACE|IGNORE_COMMENTS );
        }
        assertEquals( astNormalize( ndx1.getAst() ), 
                      astNormalize( ndx2.getAst() ) );
    }

    /** 
     * Reconstructs a FrameSet from scratch so that its internal structure
     * is canonical.  This is used for FrameSet comparisons.
     */
    private static FrameSet astNormalize( FrameSet fset1 ) {
        FrameSet fset2 = new FrameSet( fset1.getFrame( 1 ) );
        for ( int i = 2; i <= fset1.getNframe(); i++ ) {
            fset2.addFrame( 1, 
                            fset1.getMapping( 1, i ).simplify(), 
                            fset1.getFrame( i ) );
        }
        return fset2;
    }
}
