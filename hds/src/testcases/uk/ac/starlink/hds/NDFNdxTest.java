package uk.ac.starlink.hds;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
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
import javax.xml.transform.TransformerException;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.array.NDArrays;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxHandler;
import uk.ac.starlink.ndx.NdxIO;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.TestCase;

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
            ndfURL = new URL( "file:" + containerFile );
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
    }


    public NDFNdxTest( String name ) {
        super( name );
    }

    public void testHandler() throws IOException, TransformerException {

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

        /* Write the NDX out as an NDF. */
        ndxio.setHandlers( new NdxHandler[] { NDFNdxHandler.getInstance() } );
        String hloc = tmpdir + "/" + "copy.sdf";
        ndxio.outputNdx( hloc, ndx );
        Ndx hndx = ndxio.makeNdx( hloc, AccessMode.READ );
        assertNdxEqual( ndx, hndx );
        
    }

    private void assertNdxEqual( Ndx ndx1, Ndx ndx2 )
            throws IOException, TransformerException {
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

        assertEquals( ndx1.getAst(), ndx2.getAst() );
    }
}
