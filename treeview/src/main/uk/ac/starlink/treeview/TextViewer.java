package uk.ac.starlink.treeview;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.awt.Font;
import javax.swing.JTextArea;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

/**
 * A component which presents textual data in a monospaced font.
 */
class TextViewer extends JTextArea {

    private Writer appender = new Writer() {
        public void write( char[] cbuf, int off, int len ) {
            append( new String( cbuf, off, len ) );
        }
        public void close() {}
        public void flush() {}
    };

    /**
     * Creates an empty text viewer panel.  Use the <code>append</code> 
     * method to write lines to it.
     */
    public TextViewer() {

        /* Set some characteristics of the text area. */
        setEditable( false );
        Font font = new Font( "Monospaced", 
                              getFont().getStyle(), getFont().getSize() );
        setFont( font );
    }

    /**
     * Displays a javax.xml.transform.Source.
     */
    public TextViewer( Source xsrc ) throws TransformerException {
        this();

        /* Obtain and configure a transformer to turn the XML into text. */
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();
        trans.setOutputProperty( OutputKeys.INDENT, "yes" );
        trans.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
        trans.setOutputProperty( OutputKeys.METHOD, "xml" );

        /* Attempt to set the indent amount; this may have no effect if we
         * don't have an apache transformer, but at worst it is harmless. */
        trans.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", 
                                 "2" );
        Result xres = new StreamResult( appender );

        /* Do the transformation. */
        trans.transform( xsrc, xres );
    }

     
    /**
     * Displays the text obtained from a Reader.
     */
    public TextViewer( Reader rdr ) throws IOException {
        this();
        BufferedReader brdr = ( rdr instanceof BufferedReader ) 
                                  ? (BufferedReader) rdr
                                  : new BufferedReader( rdr );
        String line;
        while ( ( line = brdr.readLine() ) != null ) {
            append( line + '\n' );
        }
        brdr.close();
    }

    /**
     * Displays the strings iterated over by an Iterator.
     * The strings are not expected to contain carriage return characters.
     *
     * @throws  ClassCastException  if any of the objects iterated over 
     *                              is not a String
     */
    public TextViewer( Iterator it ) {
        this();
        while ( it.hasNext() ) {
            append( it.next().toString() + "\n" );
        }
    }

}
