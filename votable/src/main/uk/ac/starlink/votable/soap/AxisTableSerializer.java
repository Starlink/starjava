package uk.ac.starlink.votable.soap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import javax.xml.namespace.QName;
import org.apache.axis.Constants;
import org.apache.axis.encoding.SerializationContext;
import org.apache.axis.encoding.Serializer;
import org.apache.axis.wsdl.fromJava.Types;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableWriter;

/**
 * Custom serializer for StarTables.
 *
 * @author   Mark Taylor (Starlink)
 * @since    23 Mar 2005
 */
public class AxisTableSerializer implements Serializer {

    public static final QName SOAP_STARTABLE = 
        new QName( "http://www.starlink.ac.uk/stil/", "StarTable" );

    /**
     * Writes <tt>table</tt> out to the serialization context as a
     * binary-inline VOTable.
     *
     * @param  name  qname
     * @param  atts  attribute
     * @param  value   {@link uk.ac.starlink.table.StarTable} object
     * @param  context  context
     * @throws  ClassCastException if <tt>table</tt> is not a StarTable
     */
    public void serialize( QName name, Attributes atts, Object value,
                           SerializationContext context ) throws IOException {
        StarTable table = (StarTable) value;
        VOTableWriter vowriter = 
            new VOTableWriter( DataFormat.BINARY, true );
        vowriter.setDoctypeDeclaration( null );
        vowriter.setXMLDeclaration( null );
        BufferedWriter writer = 
            new BufferedWriter( new ContextWriter( context ) );
        context.startElement( name, atts );
        vowriter.writeInlineStarTable( table, writer );
        writer.flush();
        context.endElement();
    }

    /**
     * Currently returns null.  Could return the VOTable schema I suppose.
     */
    public Element writeSchema( Class javaType, Types types ) {
        return null;
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
}
