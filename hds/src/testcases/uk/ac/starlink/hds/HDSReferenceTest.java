package uk.ac.starlink.hds;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import uk.ac.starlink.util.TestCase;

public class HDSReferenceTest extends TestCase {

    public static String NDF_FILE = "uk/ac/starlink/hds/reduced_data1.sdf";
    public static String containerName;
    public static File containerFile;

    public void setUp() throws IOException {
        if ( containerFile == null ) {
            String tmpdir = System.getProperty( "java.io.tmpdir" );
            containerName = tmpdir + File.separatorChar + "test_ndf";
            containerFile = new File( containerName + ".sdf" );
            InputStream istrm = getClass()
                               .getClassLoader()
                               .getResourceAsStream( NDF_FILE );
            assertNotNull( "Failed to open " + NDF_FILE, istrm );
            OutputStream ostrm = new FileOutputStream( containerFile );
            containerFile.deleteOnExit();

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

    public HDSReferenceTest( String name ) {
        super( name );
    }

    public void testHDSReference()
            throws IOException, MalformedURLException, HDSException {

        HDSReference r1 = new HDSReference( containerFile,
                                            new String[] { "more",
                                                           "ccdpack" } );
        HDSReference r2 = new HDSReference( r1.getURL() );

        // getContainerFile
        assertEquals( r1.getContainerFile().getCanonicalPath(),
                      r2.getContainerFile().getCanonicalPath() );

        // getContainerName
        assertEquals( r1.getContainerName(), r2.getContainerName() );

        // getPath
        assertArrayEquals( r1.getPath(), r2.getPath() );

        // getURL
        assertEquals( r1.getURL().toString(), r2.getURL().toString() );

        // clone
        HDSReference r4 = (HDSReference) r1.clone();
        assertTrue( ! ( r4 == r1 ) );
        assertTrue( r4.getURL().equals( r1.getURL() ) );
        r4.push( "ELEMENT" );
        assertTrue( ! r4.getURL().equals( r1.getURL() ) );

        // getObject
        HDSObject o = r1.getObject( "READ" );
        assertEquals( o.datName(), "CCDPACK" );
        o.datAnnul();

        HDSReference r3 = new HDSReference( containerName + ".more.ccdpack" );
        assertEquals( r3.getContainerFile().getCanonicalPath(),
                     r1.getContainerFile().getCanonicalPath() );
        assertArrayEquals( r3.getPath(), r1.getPath() );
    }
}
