package uk.ac.starlink.treeview;

import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.swing.JComponent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import uk.ac.starlink.util.DOMUtils;
import uk.ac.starlink.util.SourceReader;

/**
 * Generic node for representing VOTable elements.
 */
public class VOTableDataNode extends VOComponentDataNode {

    private String name;
    private JComponent fullView;

    public VOTableDataNode( Source xsrc ) throws NoSuchDataException {
        super( xsrc, "VOTABLE" );
    }

    public String getNodeTLA() {
        return "VOT";
    }

    public String getNodeType() {
        return "VOTable";
    }

    public short getIconId() {
        return IconFactory.VOTABLE;
    }

    public boolean allowsChildren() {
        return true;
    }

    public boolean hasFullView() {
        return true;
    }

}
