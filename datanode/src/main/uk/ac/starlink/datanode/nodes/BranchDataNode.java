package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import javax.swing.Icon;
import uk.ac.starlink.connect.Branch;
import uk.ac.starlink.connect.Connection;
import uk.ac.starlink.connect.Connector;
import uk.ac.starlink.connect.Leaf;
import uk.ac.starlink.connect.Node;
import uk.ac.starlink.connect.NodeComparator;

/**
 * DataNode implementation for a branch of a virtual filestore.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    25 Feb 2005
 */
public class BranchDataNode extends DefaultDataNode {

    private Branch branch_;
    private Connection connection_;
    private Icon icon_;
    private String tla_;
    private String type_;
    private String path_;

    public BranchDataNode( Branch branch ) {
        branch_ = branch;
        String name = branch.getName();
        if ( ( name == null || name.length() == 0 )
             && branch.getParent() == null ) {
            name = "/";
        }
        setName( name );
        setLabel( name );
        setIconID( IconFactory.DIRECTORY );
    }

    /**
     * Sets the Connection object associated with this branch.
     * It's not compulsory to call this on a BranchDataNode, but it enables
     * it to report more things about itself.
     *
     * @param   connection  connection from which this item is derived
     */
    public void setConnection( Connection connection ) {
        connection_ = connection;
        Connector connector = connection.getConnector();
        if ( connector != null ) {
            if ( branch_.getParent() == null ) {
                icon_ = connector.getIcon();
            }
            tla_ = connector.getName().toUpperCase();
            if ( tla_.length() > 3 ) {
                tla_ = tla_.substring( 0, 3 );
            }
            type_ = connector.getName() + " branch";
        }
        if ( branch_.getParent() == null ) {
            setName( connection.toString() );
        }
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        Node[] children = branch_.getChildren();
        Arrays.sort( children, NodeComparator.getInstance() );
        final Iterator it = Arrays.asList( children ).iterator();
        return new Iterator() {
            public boolean hasNext() {
                return it.hasNext();
            }
            public Object next() {
                Node node = (Node) it.next();
                if ( node instanceof Leaf ) {
                    try {
                        return makeChild( ((Leaf) node).getDataSource() );
                    }
                    catch ( IOException e ) {
                        return makeErrorChild( e );
                    }
                }
                else {
                    assert node instanceof Branch;
                    DataNode dataNode = makeChild( node );
                    if ( dataNode instanceof BranchDataNode &&
                         connection_ != null ) {
                        ((BranchDataNode) dataNode)
                                         .setConnection( connection_ );
                    }
                    return dataNode;
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * Sets the absolute path of this node.
     */
    public void setPath( String path ) {
        path_ = path;
    }

    public Object getParentObject() {
        return branch_.getParent();
    }

    public String getPathSeparator() {
        return "/";
    }

    public String getPathElement() {
        return path_ == null ? super.getPathElement()
                             : path_;
    }

    public Icon getIcon() {
        return icon_ == null ? super.getIcon()
                             : icon_;
    }

    public String getNodeTLA() {
        return tla_ == null ? "BRA"
                            : tla_;
    }

    public String getNodeType() {
        return type_ == null ? "Remote branch"
                             : type_;
    }
}
