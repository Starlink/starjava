package uk.ac.starlink.ast.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.ast.AstException;
import uk.ac.starlink.ast.AstObject;
import uk.ac.starlink.ast.Channel;
import uk.ac.starlink.util.SourceReader;

/** 
 * Provides a method of getting an AstObject from its XML representation.
 *
 * @author Mark Taylor
 * @author Peter W. Draper
 */
public class XAstReader {

    /**
     * Constructs an AstObject from an Element.
     *
     * @param  el  the Element to transform
     * @param  prefix the namespace prefix for elements and
     *             attributes, null for none (include :)
     * @return     the AstObject constructed from <tt>el</tt>
     * @throws     IOException  if <tt>el</tt> does not have the correct 
     *             structure to be an <tt>AstObject</tt>
     */
    public AstObject makeAst( Element el, String prefix ) throws IOException {
        ChannelWriter chan = new ChannelWriter( prefix );
        chan.writeElement( el );
        try {
            return chan.read();
        }
        catch ( AstException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }

    /**
     * Constructs an AstObject from an XML Source.
     *
     * @param  xsrc  the XML Source to transform
     * @param  prefix the namespace prefix for elements and
     *             attributes, null for none (include :)
     * @return the AstObject constructed from <tt>xsrc</tt>
     * @throws    IOException  if <tt>xsrc</tt> does not have the right
     *            structure to be an <tt>AstObject</tt>
     */
    public AstObject makeAst( Source xsrc, String prefix ) throws IOException {
        Node node;
        try {
            node = new SourceReader().getDOM( xsrc );
        }
        catch ( TransformerException e ) {
            throw (IOException) 
                  new IOException( "Error transforming XML source: "
                                 + e.getMessage() )
                 .initCause( e );
        }
        Element el;
        if ( node instanceof Document ) {
            el = ((Document) node).getDocumentElement();
        }
        else if ( node instanceof Element ) {
            el = (Element) node;
        }
        else {
            throw new IOException( 
                "Source does not represent an Element or Document" );
        }
        return makeAst( el, prefix );
    }

    /*
     * Handles the work of turning an XML Element into an AstObject.
     */
    private static class ChannelWriter extends Channel {

        private List eLines = new ArrayList();
        private String prefix = null;

        // Instance versions of fixed names (may need namespace
        // qualification).
        private String attributeName = XAstNames.ATTRIBUTE;
        private String isaName = XAstNames.ISA;
        private String labelName = XAstNames.LABEL;
        private String nameName = XAstNames.NAME;
        private String valueName = XAstNames.VALUE;
        private String className = XAstNames.CLASS;
        private String quotedName = XAstNames.QUOTED;

        ChannelWriter( String prefix ) {
            this.prefix = prefix;
            if ( prefix != null ) {
                attributeName = prefix + attributeName;
                isaName = prefix + isaName;
                labelName = prefix + labelName;
                nameName = prefix + nameName;
                valueName = prefix + valueName;
                className = prefix + className;
                quotedName = prefix + quotedName;
            }
        }

        /*
         * Prepares an XML element for writing to an AST Channel.
         * It turns it into a List of strings, which subsequent calls
         * of the sink method can retrieve.  The read method of the
         * Channel can only be called after this method has been called.
         */
        synchronized void writeElement( Element el ) {
            if ( el.hasAttribute( labelName ) ) {
                appendLine( el.getAttribute( labelName ) + " =" );
            }
            String elName = el.getNodeName();
            if ( prefix != null ) {
                elName = elName.substring( prefix.length() );
            }
            appendLine( "Begin " + elName );

            for ( Node child = el.getFirstChild(); child != null;
                  child = child.getNextSibling() ) {
                if ( child.getNodeType() == Node.ELEMENT_NODE ) {
                    Element subel = (Element) child;
                    String name = subel.getTagName();
                    if ( name.equals( attributeName ) ) {
                        String attName = subel.getAttribute( nameName );
                        String attVal = subel.getAttribute( valueName );
                        String quoteVal = subel.getAttribute( quotedName );
                        if ( Boolean.valueOf( quoteVal ).booleanValue() ) {
                            attVal = '"' + attVal + '"';
                        }
                        appendLine( attName + " = " + attVal );
                    }
                    else if ( name.equals( isaName ) ) {
                        appendLine( "IsA " + subel.getAttribute( className ) );
                    }
                    else {
                        writeElement( subel );
                    }
                }
            }
            appendLine( "End " + elName );
        }

        /*
         * Appends a line to the internal buffer used for storing lines
         * of AST-ready text.
         */
        private void appendLine( String s ) {
            eLines.add( s );
        }

        protected synchronized String source() throws IOException {
            return (String) ( eLines.size() > 0 ? eLines.remove( 0 ) : null );
        }
    }
}
