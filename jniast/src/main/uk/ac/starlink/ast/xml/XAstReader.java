package uk.ac.starlink.ast.xml;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.ast.XmlChan;
import uk.ac.starlink.util.SourceReader;

/**
 * Provides convenience methods for reading an AstObject from an
 * XML source.  The hard work is done by {@link uk.ac.starlink.ast.XmlChan}.
 *
 * @author   Mark Taylor (Starlink)
 */
public class XAstReader {

    /**
     * Constructs an AstObject from an Element.
     *
     * @param  el  the Element to transform
     * @return     the AstObject constructed from <tt>el</tt>
     * @throws     IOException  if <tt>el</tt> does not have the correct
     *             structure to be an <tt>AstObject</tt>
     */
    public AstObject makeAst( Element el ) throws IOException {
        return makeAst( new DOMSource( el ) );
    }

    /**
     * Constructs an AstObject from an XML Source.
     *
     * @param  xsrc  the XML Source to transform
     * @return the AstObject constructed from <tt>xsrc</tt>
     * @throws    IOException  if <tt>xsrc</tt> does not have the right
     *            structure to be an <tt>AstObject</tt>
     */
    public AstObject makeAst( Source xsrc ) throws IOException {

        // The following is a temporary measure to remove namespacing,
        // to work round a bug in HDX.
        // It must be got rid of when the HDX bug is fixed.
        if ( true ) {
            try {
                Element el = new SourceReader().getElement( xsrc );
                el = (Element) el.cloneNode( true );
                el.setAttribute( "xmlns",
                                 "http://www.starlink.ac.uk/ast/xml/" );
                xsrc = new DOMSource( el );
            }
            catch ( javax.xml.transform.TransformerException e ) {
                throw (AssertionError) 
                      new AssertionError( 
                          "Error in HDX bug workaround code that shouldn't " +
                          "be here anyway" )
                     .initCause( e );
            }
        }

        final InputStream istrm = new SourceReader().getXMLStream( xsrc );
        XmlChan xc = new XmlChan() {
            byte[] buf = new byte[ 64 ];
            protected String source() throws IOException {
                int nchar = istrm.read( buf );
                if ( nchar == -1 ) {
                    return null; 
                }
                else {
                    return new String( buf, 0, nchar );
                }
            }
        };
        return xc.read();
    }
}
