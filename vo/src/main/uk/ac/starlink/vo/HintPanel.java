package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

/**
 * Panel that displays an ADQL cheat sheet.
 *
 * @author   Mark Taylor
 * @since    15 Oct 2015
 */
public class HintPanel extends JPanel {

    private final UrlHandler urlHandler_;
    private final JEditorPane edPanel_;
    private final HTMLDocument doc_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /** Name of HTML file for ADQL cheat sheet panel. */
    static final String HINTS_FILE = "adql_hints.html";

    /**
     * Constructor.
     *
     * @param   urlHandler   handler for link click actions
     */
    public HintPanel( final UrlHandler urlHandler ) {
        super( new BorderLayout() );
        urlHandler_ = urlHandler;
        HTMLEditorKit edKit = new HTMLEditorKit();
        doc_ = (HTMLDocument) edKit.createDefaultDocument();
        edPanel_ = new JEditorPane();
        edPanel_.setEditorKit( edKit );
        edPanel_.putClientProperty( JEditorPane.HONOR_DISPLAY_PROPERTIES,
                                    true );
        edPanel_.setEditable( false );
        edPanel_.setOpaque( false );
        URL docResource = HintPanel.class.getResource( HINTS_FILE );
        if ( docResource != null ) {
            try {
                edPanel_.read( docResource.openStream(), doc_ );
            }
            catch ( IOException e ) {
                String msg = "Read error for: " + docResource + " - " + e;
                logger_.warning( msg );
                edPanel_.setText( msg );
            }
        }
        else {
            String msg = "No content";
            logger_.warning( msg );
            edPanel_.setText( msg );
        }
        edPanel_.setCaretPosition( 0 );
        if ( urlHandler != null ) {
            edPanel_.addHyperlinkListener( new HyperlinkListener() {
                public void hyperlinkUpdate( HyperlinkEvent evt ) {
                    if ( HyperlinkEvent.EventType.ACTIVATED
                                                 .equals(evt.getEventType())) {
                        URL url = evt.getURL();
                        urlHandler.clickUrl( url );
                    }
                }
            } );
        }
        add( edPanel_, BorderLayout.CENTER );
    }

    /**
     * Sets the known URL for service-specific examples, which may be null
     * if there are none.  The text will be updated accordingly.
     *
     * @param  url  examples URL, or null
     */
    public void setExamplesUrl( String url ) {
        StringBuffer sbuf = new StringBuffer();
        if ( url == null ) {
            sbuf.append( "This service has no " )
                .append( "<em>service-provided</em> examples" );
        }
        else {
            sbuf.append( "This service has data-specific examples: <br></br>" )
                .append( "you can see them in the " )
                .append( "<span id='menu'>Service Provided</span> " )
                .append( "Examples sub-menu, <br></br>" )
                .append( "or <a href='" )
                .append( url )
                .append( "'>with explanation</a> in your browser." );
        }
        try {
            HTMLDocument doc = (HTMLDocument) edPanel_.getDocument();
            Element exEl = doc.getElement( "EXAMPLE_CONTENT" );
            doc.setInnerHTML( exEl, sbuf.toString() );
        }
        catch ( Exception e ) {
            logger_.warning( "Trouble editing HTML: " + e );
        }
    }
}
