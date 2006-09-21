package uk.ac.starlink.ast.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Element;
import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.ast.XmlChan;
import uk.ac.starlink.util.SourceReader;

/**
 * Provides convenience methods for serializing AstObjects to XML.
 * The hard work is done by {@link uk.ac.starlink.ast.XmlChan}.
 *
 * @author   Mark Taylor (Starlink)
 */
public class XAstWriter {

    private BufferXmlChan xmlChan = new BufferXmlChan();

    /**
     * Returns the XmlChan used by this writer.  Its attributes may be
     * modified to affect the details of the XML that is written.
     *
     * @return  channel object
     */
    public XmlChan getXmlChan() {
        return xmlChan;
    }

    /**
     * Turns an AstObject into an Element.
     *
     * @param   obj  the AstObject to be serialised
     * @return  an Element representing <tt>obj</tt>
     */
    public Element makeElement( AstObject obj ) {
        try {
            return new SourceReader().getElement( makeSource( obj ) );
        }
        catch ( TransformerException e ) {
            throw (AssertionError)
                  new AssertionError( "Unexpected trouble obtaining " +
                                      "Element from XML" )
                 .initCause( e );
        }
    }

    /**
     * Turns an AstObject into an XML Source.
     *
     * @param   obj     the AstObject to be serialised
     * @return  a Source representing <tt>obj</tt>
     */
    public Source makeSource( AstObject obj ) {
        xmlChan.clear();
        try {
            int nwrite = xmlChan.write( obj );
            assert nwrite == 1;
        }
        catch ( IOException e ) {
            throw new AssertionError( "That shouldn't happen." );
        }
        String content = xmlChan.getContentString();

        // The following is a temporary measure to remove namespacing,
        // to work round a bug in HDX.
        // It must be got rid of when the HDX bug is fixed.
        if ( true ) {
            content = content.replaceFirst( 
                " xmlns=.http://www.starlink.ac.uk/ast/xml/.", "" );
        }

        xmlChan.clear();
        InputStream istrm = new ByteArrayInputStream( content.getBytes() );
        return new StreamSource( istrm );
    }
}
