package uk.ac.starlink.vo;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
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
    private final Map<AdqlVersion,HTMLDocument> docMap_;
    private final Supplier<HTMLDocument> docSupplier_;
    private AdqlVersion adqlVersion_;
    private String examplesUrl_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.vo" );

    /**
     * Constructor.
     *
     * @param   urlHandler   handler for link click actions
     */
    public HintPanel( final UrlHandler urlHandler ) {
        super( new BorderLayout() );
        urlHandler_ = urlHandler;
        docMap_ = new HashMap<AdqlVersion,HTMLDocument>();
        HTMLEditorKit edKit = new HTMLEditorKit();
        docSupplier_ = () -> (HTMLDocument) edKit.createDefaultDocument();
        edPanel_ = new JEditorPane();
        edPanel_.setEditorKit( edKit );
        edPanel_.putClientProperty( JEditorPane.HONOR_DISPLAY_PROPERTIES,
                                    true );
        edPanel_.setEditable( false );
        edPanel_.setOpaque( false );
        if ( urlHandler != null ) {
            edPanel_.addHyperlinkListener( new HyperlinkListener() {
                public void hyperlinkUpdate( HyperlinkEvent evt ) {
                    if ( HyperlinkEvent.EventType.ACTIVATED
                                       .equals( evt.getEventType() ) ) {
                        URL url = evt.getURL();
                        urlHandler.clickUrl( url );
                    }
                }
            } );
        }
        setAdqlVersion( AdqlVersion.V21 );
        add( edPanel_, BorderLayout.CENTER );
    }

    /**
     * Configures the content of this panel to describe a given version of
     * ADQL.
     *
     * @param  version   language version
     */
    public void setAdqlVersion( AdqlVersion version ) {
        if ( adqlVersion_ != version && version != null ) {
            adqlVersion_ = version;
            updateContent();
        }
    }

    /**
     * Sets the known URL for service-specific examples, which may be null
     * if there are none.  The text will be updated accordingly.
     *
     * @param  url  examples URL, or null
     */
    public void setExamplesUrl( String url ) {
        if ( examplesUrl_ == null || ! examplesUrl_.equals( url ) ) {
            examplesUrl_ = url;
            updateContent();
        }
    }

    /**
     * Called when the configuration has changed to update the content
     * of this panel.  Ideally should not be called if the content has
     * not materially changed.
     */
    private void updateContent() {

        /* Set the version-sensitive document text. */
        HTMLDocument doc =
            docMap_.computeIfAbsent( adqlVersion_,
                                     v -> createHintsDocument( v ) );
        if ( edPanel_.getDocument() != doc ) {
            edPanel_.setDocument( doc );
        }

        /* Update the text with information about examples. */
        Element exEl = doc.getElement( "EXAMPLE_CONTENT" );
        if ( exEl != null ) {
            StringBuffer sbuf = new StringBuffer();
            if ( examplesUrl_ == null ) {
                sbuf.append( "This service has no " )
                    .append( "<em>service-provided</em> examples" );
            }
            else {
                sbuf.append( "This service has data-specific examples:" )
                    .append( "<br></br>" )
                    .append( "you can see them in the " )
                    .append( "<span id='menu'>Service Provided</span> " )
                    .append( "Examples sub-menu, <br></br>" )
                    .append( "or <a href='" )
                    .append( examplesUrl_ )
                    .append( "'>with explanation</a> in your browser." );
            }
            try {
                doc.setInnerHTML( exEl, sbuf.toString() );
            }
            catch ( BadLocationException | IOException e ) {
                logger_.warning( "Problem editing HTML" );
            }
        }

        /* Scroll back to the top otherwise the document position ends up
         * in some non-obvious place. */
        edPanel_.setCaretPosition( 0 );
    }

    /**
     * Creates a Document containing generic hints HTML specific to
     * a given ADQL version.
     *
     * @param  adqlVersion  version
     * @return  new document
     */
    private final HTMLDocument createHintsDocument( AdqlVersion adqlVersion ) {
        URL docResource = getDocResource( adqlVersion );
        HTMLDocument doc = docSupplier_.get();
        String errMsg;
        if ( docResource != null ) {
            try {
                edPanel_.read( docResource.openStream(), doc );
                errMsg = null;
            }
            catch ( IOException e ) {
                errMsg = "Read error for " + docResource + " - " + e;
            }
        }
        else {
            errMsg = "No hints document for ADQL " + adqlVersion;
        }
        if ( errMsg != null ) {
            logger_.warning( errMsg );
            try {
                doc.insertString( 0, errMsg, null );
            }
            catch ( BadLocationException e ) {
                logger_.warning( "Can't even insert error into document: "
                               + errMsg );
            }
        }
        return doc;
    }

    /**
     * Returns the resource URL at which the hints HTML document can be found
     * for a given ADQL version.
     *
     * @param  adqlVersion  version
     * @return   URL for HTML resource
     */
    static final URL getDocResource( AdqlVersion adqlVersion ) {
        final String docName;
        switch ( adqlVersion ) {
            case V20:
                docName = "adql_hints_v2.0.html";
                break;
            case V21:
                docName = "adql_hints_v2.1.html";
                break;
            default:
                assert adqlVersion == null;
                return null;
        }
        return HintPanel.class.getResource( docName );
    }
}
