package uk.ac.starlink.xdoc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import junit.framework.TestCase;
import uk.ac.starlink.util.URLUtils;

public class TransformTest extends TestCase {

    public void testHtml() throws Exception {
        String result = transform( "doc.xhtml", "xhtml.xslt" );
        assertTrue( result.matches( ".*width=.47..*" ) );
        assertTrue( result.matches( ".*height=.48..*" ) );
    }

    public void testSun() throws Exception {
        String result = transform( "sun.xml", "toHTML1.xslt" );
        assertTrue( result.matches( ".*width=.47..*" ) );
        assertTrue( result.matches( ".*height=.48..*" ) );
    }

    private String transform( String inFile, String inXslt ) throws Exception {
        URL inUrl = TransformTest.class.getResource( inFile );
        Source inSrc = new StreamSource( inUrl.openStream() );
        Source xsltSrc =
            new StreamSource( TransformTest.class
                             .getResource( "../../../../../etc/" + inXslt )
                             .openStream() );
        Transformer transformer =
            TransformerFactory.newInstance().newTransformer( xsltSrc );
        String inDir = URLUtils.urlToFile( inUrl.toString() ).getParent();
        transformer.setParameter( "BASEDIR", inDir );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Result result = new StreamResult( out );
        transformer.transform( inSrc, result );
        return new String( out.toByteArray(), "UTF-8" ).replaceAll( "\n", " " );
    }

}
