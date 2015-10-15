package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
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
        edPanel_ = new JEditorPane();
        edPanel_.setEditorKit( new HTMLEditorKit() );
        edPanel_.putClientProperty( JEditorPane.HONOR_DISPLAY_PROPERTIES,
                                    true );
        edPanel_.setEditable( false );
        edPanel_.setOpaque( false );
        URL docResource = HintPanel.class.getResource( HINTS_FILE );
        if ( docResource != null ) {
            try {
                edPanel_.read( docResource.openStream(), new HTMLDocument() );
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
}
