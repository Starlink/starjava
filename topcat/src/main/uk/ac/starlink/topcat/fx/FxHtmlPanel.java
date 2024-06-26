package uk.ac.starlink.topcat.fx;

import java.awt.BorderLayout;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.SwingUtilities;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import uk.ac.starlink.topcat.AbstractHtmlPanel;

/**
 * AbstractHtmlPanel implementation based on the JavaFX WebView class.
 * That is much smarter than the Swing JEditorPane; in particular it
 * can cope with javascript.  It's also a bit less ugly.
 *
 * @author   Mark Taylor
 * @since    17 Jul 2019
 */
public class FxHtmlPanel extends AbstractHtmlPanel {

    private final JFXPanel fxPanel_;
    private WebEngine engine_;

    // The JavaFX-embedded-in-Swing code here was written with reference
    // to Oracle's example SimpleSwingBrowser class; see
    // https://docs.oracle.com/javafx/2/swing/swing-fx-interoperability.htm

    public FxHtmlPanel() {
        fxPanel_ = new JFXPanel();
        add( fxPanel_, BorderLayout.CENTER );
        Platform.runLater( () -> initFxWebView() );
    }

    public void setUrl( final URL url ) {
        Platform.runLater( () -> {
            final URL oldUrl = toUrl( engine_.locationProperty().getValue() );
            engine_.load( url == null ? null : url.toString() );
            SwingUtilities.invokeLater( () ->
                FxHtmlPanel.this.firePropertyChange( "url", oldUrl, url )
            );
        } );
    }

    public URL getUrl() {
        return toUrl( engine_.locationProperty().getValue() );
    }

    /**
     * Performs JavaFX initialisation.  Must be executed on a JavaFX-friendly
     * thread.
     */
    private void initFxWebView() {
        WebView view = new WebView();
        engine_ = view.getEngine();
        engine_.locationProperty().addListener( (ov, oldValue, newValue) -> {
            final URL oldUrl = toUrl( oldValue );
            final URL newUrl = toUrl( newValue );
            SwingUtilities.invokeLater( () -> 
                FxHtmlPanel.this.firePropertyChange( "url", oldUrl, newUrl )
            );
        } );
        final Worker<Void> loadWorker = engine_.getLoadWorker();
        loadWorker.stateProperty().addListener( (ov, oldState, state) -> {
            if ( state == Worker.State.FAILED ) {
                Throwable err = loadWorker.exceptionProperty().getValue();
                StringBuffer msg = new StringBuffer()
                   .append( "<html><body>" )
                   .append( "<p><b>Page load error" )
                   .append( err == null ? "" : ":" )
                   .append( "</b></p>" );
                if ( err != null ) {
                    msg.append( "<p>" )
                       .append( err )
                       .append( "</p>" );
                }
                engine_.loadContent( msg.toString(), "text/html" );
            }
        } );
        fxPanel_.setScene( new Scene( view ) );
    }

    /**
     * Converts a string to a URL without exceptions.
     *
     * @param  loc   URL string
     * @return  URL, or null
     */
    private static URL toUrl( String loc ) {
        try {
            return new URL( loc );
        }
        catch ( MalformedURLException e ) {
            return null;
        }
    }
}
