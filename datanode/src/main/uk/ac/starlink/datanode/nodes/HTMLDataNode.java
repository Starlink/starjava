package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import java.io.File;
import java.net.URL;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import uk.ac.starlink.datanode.viewers.TextViewer;

/**
 * DataNode implementation which describes a top-level HTML document.
 * The formatted HTML page is displayed and hyperlinks can be followed.
 * Note that if links are followed then the original document must be reloaded
 * in order to display the original page again.
 *
 * @author   David Giaretta (Starlink)
 * @version $Id$
 */
public class HTMLDataNode extends DefaultDataNode {

    public final File file;
    private final URL url;

    /**
     * Constructs a URL to be displayed in the pane, if possible.
     */
    public HTMLDataNode( File file ) throws NoSuchDataException {
        try {
            this.file = file;
            url = file.toURL();
            setName( "<" + url + ">" );
            setIconID( IconFactory.XML_DOCUMENT );
        } catch (Exception e) {
            throw new NoSuchDataException("Cannot convert " + file + " to URL");
        }
    }

    /**
     * HTML type does not allow children.
     * @return false
     */
    public boolean allowsChildren() {
        return false;
    }

   /**
    * Returns Three Letter Acronym "HTM".
    *
    * @return "HTM"
    */
    public String getNodeTLA() {
        return "HTM";
    }

    public String getNodeType() {
        return "HTML document";
    }

    /**
     * Sets up a pane to display the HTML file.
     * Also activates the hyperlinks.
     * Note that if any hyperlinks are followed then the new page will
     * be cached and displayed until the original docuemnt is reloaded.
     */
    public void configureDetail( DetailViewer dv ) {

        try {
        /* Add a pane with formatted results. */
            dv.addPane( "HTML file", new ComponentMaker() {
                public JComponent getComponent() throws IOException {
                    final JEditorPane jep;
                    jep  = new JEditorPane(url);
                    jep.setEditable(false);
                    /* Enable the use of hyperlinks. */
                    jep.addHyperlinkListener(new HyperlinkListener(){
                        public void hyperlinkUpdate(HyperlinkEvent evt){
                            if ( evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
                                JEditorPane pane = (JEditorPane)evt.getSource();
                                try {
                                    pane.setPage(evt.getURL());
                                } catch (IOException e) {
                                }
                            }
                        }
                    } );
                    return jep;
                }
            } );
        }
        catch ( Exception e ) {
            dv.addPane( "HTML file error", new ExceptionComponentMaker( e ) );
        }
    }
}
