package uk.ac.starlink.topcat;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.URL;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;

/**
 * HTML rendering panel based on a Swing JEditorPane.
 *
 * @author   Mark Taylor
 * @since    17 Jul 2019
 */
public class SwingHtmlPanel extends AbstractHtmlPanel {

    private final JEditorPane textPane_;
    private URL lastUrl_;

    @SuppressWarnings("this-escape")
    public SwingHtmlPanel() {
        textPane_ = new JEditorPane();
        textPane_.setEditable( false );
        add( new JScrollPane( textPane_ ), BorderLayout.CENTER );
        textPane_.addHyperlinkListener( new HyperlinkListener() {
            public void hyperlinkUpdate( HyperlinkEvent evt ) {
                if ( evt.getEventType() ==
                     HyperlinkEvent.EventType.ACTIVATED ) {
                    setUrl( evt.getURL() );
                }
            }
        } );
    }

    public void setUrl( final URL url ) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                firePropertyChange( "url", textPane_.getPage(), url );
                Document doc = textPane_.getDocument();
                if ( doc instanceof AbstractDocument ) {
                    ((AbstractDocument) doc).setAsynchronousLoadPriority( -1 );
                }
                try {
                    textPane_.setPage( url );
                }
                catch ( IOException e ) {
                    String errTxt = "<html><body>"
                        + "<p><b>Page load error:</b></p>"
                        + "<p>" + e + "</p>"
                        + "</body></html>";
                    textPane_.setText( errTxt );
                }
            }
        } );
    }

    public URL getUrl() {
        return textPane_.getPage();
    }
}
