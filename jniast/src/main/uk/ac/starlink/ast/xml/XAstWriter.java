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
            xmlChan.write( obj );
        }
        catch ( IOException e ) {
            throw new AssertionError( "That shouldn't happen." );
        }
        InputStream istrm = 
            new ByteArrayInputStream( xmlChan.getContentString().getBytes() );
        xmlChan.clear();
        return new StreamSource( istrm );
    }
}
