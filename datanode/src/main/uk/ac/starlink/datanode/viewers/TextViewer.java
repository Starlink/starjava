package uk.ac.starlink.datanode.viewers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
public class TextViewer extends JTextArea {

    private Writer appender = new Writer() {
        public void write( char[] cbuf, int off, int len ) {
            TextViewer.this.append( new String( cbuf, off, len ) );
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
    public TextViewer( final Source xsrc ) {
        this();

        /* Obtain and configure a transformer to turn the XML into text. */
        final SourceReader sr = new SourceReader();
        sr.setIndent( 2 );
        sr.setIncludeDeclaration( false );
        new Thread() {
            public void run() {
                try {
                    sr.writeSource( xsrc, appender );
                }
                catch ( TransformerException e ) {
                    e.printStackTrace( new PrintWriter( appender ) );
                }
            }
        }.start();
    }

     
    /**
     * Displays the text obtained from a Reader.
     */
    public TextViewer( Reader rdr ) {
        this();
        final BufferedReader brdr = ( rdr instanceof BufferedReader ) 
                                      ? (BufferedReader) rdr
                                      : new BufferedReader( rdr );
        new Thread() {
            public void run() {
                try {
                    String line;
                    while ( ( line = brdr.readLine() ) != null ) {
                        append( line + '\n' );
                    }
                    brdr.close();
                }
                catch ( IOException e ) {
                    e.printStackTrace( new PrintWriter( appender ) );
                }
            }
        }.start();
    }

    /**
     * Displays the text returned from an InputStream.
     */
    public TextViewer( InputStream strm ) {
        this( new InputStreamReader( strm ) );
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

    /**
     * Displays the stacktrace of a given <code>Throwable</code>
     * in a text viewer window.
     *
     * @param  th  the Throwable to print the stack trace of
     */
    public TextViewer( Throwable th ) {
        this();
        PrintWriter pw = new PrintWriter( appender );
        th.printStackTrace( pw );
        pw.close();
    }

    /**
     * Returns a Writer object; any thing you write into it will appera
     * in the TextViewer.
     *
     * @return  a writer that writes into this viewer
     */
    public Writer getAppender() {
        return appender;
    }

}
