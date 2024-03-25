package uk.ac.starlink.topcat;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import uk.ac.starlink.util.TestCase;
import uk.ac.starlink.util.URLUtils;
import uk.ac.starlink.xdoc.LinkChecker;

public class DocTest extends TestCase {

    private static final String basedir = System.getProperty( "basedir" );
    public static final String DOC_NAME = "sun253";
    public static final String DOC_BUILD_DIR = basedir + "/build/docs";
    public static final String XSLT_DIR = System.getProperty( "xdoc.etc" );

    File docFile = new File( DOC_BUILD_DIR, DOC_NAME + ".xml" );

    public DocTest( String name ) {
        super( name );
    }

    public void testValidity() throws IOException, SAXException {
        assertValidXML( new InputSource( docFile.toString() ) );
    }

    public void testLinks() 
            throws TransformerException, MalformedURLException {
        File docXslt1 = new File( XSLT_DIR, "toHTML1.xslt" );
        File docXslt = new File( XSLT_DIR, "toHTML.xslt" );
        File context = new File( DOC_BUILD_DIR, DOC_NAME );
        assertTrue( docXslt1.isFile() );
        assertTrue( docXslt.isFile() );
        assertTrue( docFile.isFile() );
        assertTrue( context.isDirectory() );
        boolean attemptExt = Boolean.getBoolean( "tests.withnet" );
        Map<String,String> xsltParams = new HashMap<>();
        xsltParams.put( "BASEDIR", context.toString() );
        LinkChecker checker =
            new LinkChecker( URLUtils.makeFileURL( context ), attemptExt );
        checker.checkLinks( new StreamSource( docXslt1 ),
                            new StreamSource( docFile ),
                            xsltParams );
        checker.checkLinks( new StreamSource( docXslt ),
                            new StreamSource( docFile ),
                            xsltParams );
        assertTrue( checker.getLocalFailures() == 0 );
    }
}
