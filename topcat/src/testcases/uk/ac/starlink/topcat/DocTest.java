package uk.ac.starlink.topcat;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.TestCase;

public class DocTest extends TestCase {

    private static final String basedir = System.getProperty( "basedir" );
    public static final String DOC_NAME = "sun253";
    public static final String DOC_BUILD_DIR = basedir + "/build/docs";
    public static final String DOC_SRC_DIR = basedir + "/src/docs";

    File docFile = new File( DOC_BUILD_DIR, DOC_NAME + ".xml" );
    

    public DocTest( String name ) {
        super( name );
    }

    public void testValidity() throws IOException, SAXException {
        assertValidXML( new InputSource( docFile.toString() ) );
    }

    public void testLinks() 
            throws TransformerException, MalformedURLException {
        File docXslt1 = new File( DOC_SRC_DIR, "toHTML1.xslt" );
        File docXslt = new File( DOC_SRC_DIR, "toHTML.xslt" );
        File context = new File( DOC_BUILD_DIR, DOC_NAME );
        assertTrue( docXslt1.isFile() );
        assertTrue( docXslt.isFile() );
        assertTrue( docFile.isFile() );
        assertTrue( context.isDirectory() );
        LinkChecker checker = new LinkChecker( context.toURL() );
        checker.checkLinks( new StreamSource( docXslt1 ),
                            new StreamSource( docFile ) );
        checker.checkLinks( new StreamSource( docXslt ),
                            new StreamSource( docFile ) );
        assertTrue( checker.getLocalFailures() == 0 );
    }
}
