package uk.ac.starlink.votable.soap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import org.apache.axis.Constants;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.encoding.Serializer;
import org.apache.axis.wsdl.fromJava.Types;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.util.SourceReader;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * Custom serializer for StarTables.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Mar 2005
 */
public class AxisTableSerializer implements Serializer {

    private static Element schema_;
    private static Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.votable.soap" );

    /**
     * Writes <code>table</code> out to the serialization context as a
     * binary-inline VOTable.
     *
     * @param  name  qname
     * @param  atts  attribute
     * @param  value   {@link uk.ac.starlink.table.StarTable} object
     * @param  context  context
     * @throws  ClassCastException if <code>table</code> is not a StarTable
     */
    public void serialize( QName name, Attributes atts, Object value,
                           SerializationContext context ) throws IOException {
        StarTable table = (StarTable) value;
        VOTableWriter vowriter = 
            new VOTableWriter( DataFormat.BINARY, true );
        vowriter.setXMLDeclaration( null );
        BufferedWriter writer = 
            new BufferedWriter( new ContextWriter( context ) );
        context.startElement( name, atts );
        vowriter.writeInlineStarTable( table, writer );
        writer.flush();
        context.endElement();
    }

    /**
     * Returns an element containing the VOTable 1.1 schema.
     *
     * <p>When this method is called by AXIS in the course of creating
     * WSDL (using the service?wsdl URL) it results in a WRONG_DOCUMENT_ERR
     * DOM exception.  I can only imagine this is an AXIS bug, arising
     * from AXIS not calling importNode on the returned Element.
     */
    @SuppressWarnings("rawtypes")
    public Element writeSchema( Class javaType, Types types ) {
        if ( schema_ == null ) {
            try {
                schema_ = getVOTableSchema();
            }
            catch ( TransformerException e ) {
                logger_.warning( "VOTable schema production failed: " + e );
            }
        }
        return schema_;
    }

    public String getMechanismType() {
        return Constants.AXIS_SAX;
    }

    /**
     * Helper class which implements a Writer on top of a SerializationContext.
     * Anything written to the writer is squirted out as part of the
     * serialization.
     */
    private static class ContextWriter extends Writer {
        final SerializationContext context_;

        ContextWriter( SerializationContext context ) {
            context_ = context;
        }

        public void write( char[] cbuf, int off, int len ) throws IOException {

            /* Note DON'T use writeChars() instead of writeString here
             * (unless they've changed the implementation) since writeChars
             * is implemented in terms of writeSafeString, so escapes all
             * the XML characters. */
            context_.writeString( new String( cbuf, off, len ) );
        }

        public void flush() {
        }

        public void close() {
            flush();
        }
    }

    /**
     * Reads the VOTable 1.1 schema and returns an element containing it.
     *
     * @return   VOTable schema element
     */
    private static Element getVOTableSchema() throws TransformerException {
        URL url = AxisTableSerializer.class
                 .getResource( "/uk/ac/starlink/util/text/VOTable1.1.xsd" );
        return new SourceReader()
              .getElement( new StreamSource( url.toString() ) );
    }
}
