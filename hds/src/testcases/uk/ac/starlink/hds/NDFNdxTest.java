package uk.ac.starlink.hds;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxBuilder;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.util.TestCase;

public class NDFNdxTest extends TestCase {

    public static String NDF_FILE = "uk/ac/starlink/hds/reduced_data1.sdf";
    public static String containerName;
    public static File containerFile;
    public static URL ndfURL;

    public void setUp() throws MalformedURLException, IOException {
        if ( containerFile == null ) {
            String tmpdir = System.getProperty( "java.io.tmpdir" );
            containerName = tmpdir + File.separatorChar + "test_ndf";
            containerFile = new File( containerName + ".sdf" );
            ndfURL = new URL( "file:" + containerFile );
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


    public NDFNdxTest( String name ) {
        super( name );
    }

    public void testBuilder() throws IOException, TransformerException {

        NdxBuilder builder = NDFNdxBuilder.getInstance();

        Ndx ndx = builder.makeNdx( ndfURL, AccessMode.READ );
        Source xndx = ndx.toXML();
        // new SourceReader().writeSource( xndx, System.out );
    }
}
