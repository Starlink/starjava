package uk.ac.starlink.treeview;

import java.io.IOException;
import javax.swing.JComponent;
import uk.ac.starlink.datanode.nodes.ComponentMaker;

/**
 * Adaptor class to turn an HTMLViewer into a ComponentMaker.
 * It also pulls documents from a known path in the Treeview resource tree.
 *
 * @author   Mark Taylor (Starlink)
 */
public class HTMLDocComponentMaker implements ComponentMaker {

    /** The base location of documents viewed by this object. */
    public final static String DOCS_PATH = "uk/ac/starlink/treeview/docs/";

    private String docFileName;

    /**
     * Construct a ComponentMaker which will build an HTMLViewer on request.
     * The name of the file to display is given; this is looked for in
     * the default location ({@link #DOCS_PATH}).
     * This constructor is lightweight, all the hard work is deferred
     * until a call of the {@link #getComponent} method.
     *
     * @param  docFileName  the base name (without path) of the HTML
     *         document in the treeview document directory which will be 
     *         displayed
     */
    public HTMLDocComponentMaker( String docFileName ) {
        this.docFileName = docFileName;
    }

    public JComponent getComponent() throws IOException {
        return new HTMLViewer( DOCS_PATH + docFileName );
    }
    
}
