package uk.ac.starlink.treeview;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import uk.ac.starlink.util.SourceReader;

public class VODataNodeBuilder extends DataNodeBuilder {

    public boolean suitable( Class objClass ) {
        return Source.class.isAssignableFrom( objClass );
    }

    public DataNode buildNode( Object obj ) {
        if ( obj instanceof Source ) {
            Node domNode;
            Source xsrc;
            if ( obj instanceof DOMSource ) {
                xsrc = (Source) obj;
                domNode = ((DOMSource) obj).getNode();
            }
            else {
                try {
                    domNode = new SourceReader().getDOM( (Source) obj );
                    xsrc = new DOMSource( domNode );
                    xsrc.setSystemId( ((Source) obj).getSystemId() );
                }
                catch ( TransformerException e ) {
                    return null;
                }
            }
            try {
                if ( domNode instanceof Element ) {
                    String elname = ((Element) domNode).getTagName();
                    if ( elname.equals( "VOTABLE" ) ) {
                        return new VOTableDataNode( xsrc );
                    }
                    else if ( elname.equals( "TABLE" ) ) {
                        return new VOTableTableDataNode( xsrc );
                    }
                    else {
                        return new VOComponentDataNode( xsrc );
                    }
                }
                else {
                    return null;
                }
            }
            catch ( NoSuchDataException e ) {
                return null;
            }
        }
        else {
            throw new IllegalArgumentException();
        }
    }
}
