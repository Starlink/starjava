package uk.ac.starlink.treeview;

import java.awt.Toolkit;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import uk.ac.starlink.datanode.nodes.IconFactory;

/**
 * Views an HTML document.  Some pre-processing may be done on the raw
 * HTML; in particular any string of the form "IconFactory.XXXX",
 * where XXXX is one of the public static final fields of the
 * {@link IconFactory} class is replaced by the URL of the gif representing
 * that icon.  So you can write
 * <pre>
 *    &lt;img src="IconFactory.TREE_LOGO"/&gt;
 * </pre>
 * and get the IconFactory.TREE_LOGO picture without having to know the 
 * name or location of the file that contains it.
 *
 * @author   Mark Taylor (Starlink)
 */
public class HTMLViewer extends JEditorPane {

    /** Location of the CSS file used for HTML formatting. */
    public final static String CSS_RESOURCE = 
        "uk/ac/starlink/treeview/docs/help.css";

    private static ClassLoader loader = HTMLViewer.class.getClassLoader();

    /**
     * Constructs a viewer which views HTML found in a named resource.
     *
     * @param   resourceName  the name of the resource where the HTML 
     *          file can be found 
     *          (as for {@link java.lang.ClassLoader#getResource})
     * @throws IOException if the resource cannot be found or read
     */
    public HTMLViewer( String resourceName ) throws IOException {
        this( getResourceURL( resourceName ) );
    }

    public HTMLViewer( File file ) throws IOException {
        this( new URL( "file:" + file ) );
    }

    /**
     * Constructs a viewer which views HTML found at a given URL.
     *
     * @param  docURL  the URL at which the HTML can be found
     * @throws IOException if the resource cannot be found or read
     */
    public HTMLViewer( URL docURL ) throws IOException {
        setEditable( false );
        InputStream docstrm = docURL.openStream();

        /* Bail out if there is no resource. */
        if ( docstrm == null ) {
            throw new FileNotFoundException( 
                "Resource " + docURL + " not found" );
        }

        /* Configure the EditorPane to read and display HTML. */
        final HTMLEditorKit htmlkit = new HTMLEditorKit();
        try {
            StyleSheet css = new StyleSheet();
            InputStream cssstrm = loader.getResourceAsStream( CSS_RESOURCE );
            css.loadRules( new InputStreamReader( cssstrm ), null );
            htmlkit.setStyleSheet( css );
        }
        catch ( Exception e ) {
        }

        /* Make it squeal if a hyperlink is activated. */
        addHyperlinkListener( new HyperlinkListener() {
            public void hyperlinkUpdate( HyperlinkEvent ev ) {
                if ( ev.getEventType() == HyperlinkEvent.EventType.ACTIVATED ) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        } );

        StyleSheet css = htmlkit.getStyleSheet();
        HTMLDocument hdoc = new HTMLDocument( css );
        Reader docrdr = 
            new InputStreamReader( new HTMLDoctorStream( docstrm ) );
        hdoc.setBase( docURL );
        setEditorKit( htmlkit );

        /* Load the HTML into the document.  This should be done
         * asynchronously since it may be slow, but I'm having trouble
         * making it work. */
        setDocument( hdoc );
        try {
            htmlkit.read( docrdr, hdoc, 0 );
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
        
    }

    private static URL getResourceURL( String resourceName )
            throws FileNotFoundException {
        URL url = loader.getResource( resourceName );
        if ( url == null ) {
            throw new FileNotFoundException( 
                "No resource found at " + resourceName );
        }
        return url;
    }

    private static class HTMLDoctorStream extends InputStream {

        InputStream baseStream;
        InputStream lineStream;

        public HTMLDoctorStream( InputStream baseStream ) throws IOException {
            this.baseStream = baseStream;
            this.lineStream = nextLine();
        }

        public int read() throws IOException {
            if ( lineStream == null ) {
                return -1;
            }
            int val = lineStream.read();
            if ( lineStream.available() == 0 ) {
                lineStream = nextLine();
            }
            return val;
        }

        private InputStream nextLine() throws IOException {

            /* Read a new line from the base stream. */
            StringBuffer sb = new StringBuffer();
            for ( int b = 0; b != '\n' && b != -1; ) {
                b = baseStream.read();
                if ( b != -1 ) {
                    sb.append( (char) b );
                }
            }
            int leng = sb.length();

            /* Check for no next line. */
            if ( leng == 0 ) {
                return null;
            }

            /* Substitution. */
            Pattern pat = Pattern.compile( "(.*?)IconFactory\\.([A-Z0-9_]+)" );
            Matcher mat = pat.matcher( sb );
            StringBuffer subst = new StringBuffer();
            int end = 0;
            while ( mat.find() ) {
                subst.append( mat.group( 1 ) );
                subst.append( getIconURL( mat.group( 2 ) ) );
                end = mat.end();
            }
            subst.append( sb.substring( end ) );

            /* Set up the input stream based on the next line. */
            leng = subst.length();
            byte[] bytes = new byte[ leng ];
            for ( int i = 0; i < leng; i++ ) {
                bytes[ i ] = (byte) subst.charAt( i );
            }
            return new ByteArrayInputStream( bytes );
        }

        private String getIconURL( String symbol ) {
            try {
                Field field = IconFactory.class.getField( symbol );
                short id = field.getShort( null );
                URL imageURL = IconFactory.getIconURL( id );
                return ( imageURL == null ) ? "unknown" : imageURL.toString();
            }
            catch ( Exception e ) {
                e.printStackTrace();
                return symbol;
            }
        }

    }
}
