package uk.ac.starlink.treeview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.astrogrid.community.common.exception.CommunityException;
import org.astrogrid.community.common.ivorn.CommunityAccountIvornFactory;
import org.astrogrid.store.Ivorn;
import org.astrogrid.store.tree.Container;
import org.astrogrid.store.tree.File;
import org.astrogrid.store.tree.Node;
import org.astrogrid.store.tree.TreeClient;
import org.astrogrid.store.tree.TreeClientException;
import uk.ac.starlink.astrogrid.AGConnector;
import uk.ac.starlink.astrogrid.AGConnectorFactory;
import uk.ac.starlink.astrogrid.MyspaceDataSource;
import uk.ac.starlink.util.DataSource;

/**
 * DataNode implementation for a MySpace container node.
 *
 * @author   Mark Taylor (Starlink)
 * @since    8 Dec 2004
 */
public class MyspaceContainerDataNode extends DefaultDataNode {

    private final Container container_;
    private final String name_;
    private String ivorn_;
    private static AGConnectorFactory connectorFactory_;

    public MyspaceContainerDataNode( Container container ) {
        container_ = container;
        name_ = container.getName();
        setName( name_ );
        setLabel( name_ );
        setIconID( IconFactory.MYSPACE );
    }

    public MyspaceContainerDataNode( String msloc ) throws NoSuchDataException {
        this( getConnector( msloc ) );
    }

    public MyspaceContainerDataNode( AGConnector conn ) 
            throws NoSuchDataException {
        this( makeContainer( conn ) );
        try {
            Ivorn ivorn = CommunityAccountIvornFactory
                         .createIvorn( conn.getCommunity(), conn.getUser() );
            ivorn_ = ivorn.toString();
        }
        catch ( CommunityException e ) {
            // label not set properly - too bad
        }
    }

    public String getNodeTLA() {
        return "MYS";
    }

    public String getNodeType() {
        return "MySpace container node";
    }

    public String getLabel() {
        return ivorn_ == null ? super.getLabel()
                              : ivorn_;
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        List children = new ArrayList( container_.getChildNodes() );
        Collections.sort( children, new StringComparator() );
        final Iterator it = children.iterator();
        return new Iterator() {
            public boolean hasNext() {
                return it.hasNext();
            }
            public Object next() {
                Node node = (Node) it.next();
                if ( node instanceof File ) {
                    DataSource datsrc = new MyspaceDataSource( (File) node );
                    DataNode dnode = makeChild( datsrc );
                    dnode.setParentObject( container_ );
                    return dnode;
                }
                else {  // presumably a Container
                    return makeChild( node );
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public String getPathElement() {
        return ivorn_ != null ? ivorn_ : name_;
    }

    public String getPathSeparator() {
        return "/";
    }

    private synchronized static AGConnectorFactory getConnectorFactory() {
        if ( connectorFactory_ == null ) {
            connectorFactory_ = AGConnectorFactory.getInstance();
        }
        return connectorFactory_;
    }

    public static boolean isMyspaceLocator( String loc ) {
        return "myspace".equals( loc );
    }

    private static Container makeContainer( TreeClient tc )
            throws NoSuchDataException {
        try {
            if ( tc.getToken().isValid() ) {
                throw new NoSuchDataException( "Security token invalid" );
            }
            else {
                return tc.getRoot();
            }
        }
        catch ( TreeClientException e ) {
            throw new NoSuchDataException( "Couldn't obtain root from " +
                                           "TreeClient", e );
        }
    }

    /**
     * Turns a MySpace locator into a Container.  Currently, the only
     * valid MySpace locator is the literal "myspace".
     * 
     * @param  msloc
     * @return  container node associated with msloc
     */
    private static Container makeContainer( AGConnector conn )
        throws NoSuchDataException {
        try {
            TreeClient tc = conn.getConnection();
            if ( tc != null ) {
                return tc.getRoot();
            }
            else {
                throw new NoSuchDataException( "AstroGrid login failed" );
            }
        }
        catch ( TreeClientException e ) {
            throw new NoSuchDataException( "AstroGrid connection failed", e );
        }
    }

    private static AGConnector getConnector( String msloc )
            throws NoSuchDataException {
        if ( isMyspaceLocator( msloc ) ) {
            return getConnectorFactory().getConnector();
        }
        else {
            throw new NoSuchDataException( "Not a MySpace locator string" );
        }
    }
}
