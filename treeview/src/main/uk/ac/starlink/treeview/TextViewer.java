package uk.ac.starlink.treeview;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.awt.Font;
import javax.swing.JTextArea;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import uk.ac.starlink.util.SourceReader;

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
        SourceReader sr = new SourceReader();
        sr.setIndent( 2 );
        sr.setIncludeDeclaration( false );
        sr.writeSource( xsrc, appender );
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
