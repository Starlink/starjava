package uk.ac.starlink.astrogrid;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.astrogrid.store.tree.Container;
import org.astrogrid.store.tree.File;
import org.astrogrid.store.tree.TreeClientException;
import uk.ac.starlink.connect.Branch;
import uk.ac.starlink.connect.Leaf;
import uk.ac.starlink.util.DataSource;

/**
 * Filestore Branch implementation based on a Myspace Container.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Feb 2005
 */
class MyspaceBranch extends MyspaceNode implements Branch {

    private final Container container_;

    /**
     * Construct a branch with no known parent.
     *
     * @param   container on which this branch is based
     */
    public MyspaceBranch( Container container ) {
        this( container, null );
    }

    /**
     * Construct a branch with a given parent. 
     *
     * @param  container on which this branch is based
     * @param  parent  parent container branch
     */
    private MyspaceBranch( Container container, MyspaceBranch parent ) {
        super( container, parent );
        container_ = container;
    }

    public uk.ac.starlink.connect.Node[] getChildren() {
        List childList = new ArrayList();
        for ( Iterator it = container_.getChildNodes().iterator();
              it.hasNext(); ) {
            org.astrogrid.store.tree.Node myNode =
                (org.astrogrid.store.tree.Node) it.next();
            MyspaceNode node = null;
            if ( myNode instanceof File ) {
                node = new MyspaceLeaf( (File) myNode, this );
            }
            else if ( myNode instanceof Container ) {
                node = new MyspaceBranch( (Container) myNode, this );
            }
            else {
                assert false;
            }
            if ( node != null ) {
                childList.add( node );
            }
        }
        return (MyspaceNode[]) childList.toArray( new MyspaceNode[ 0 ] );
    }

    public uk.ac.starlink.connect.Node createNode( String location ) {
        uk.ac.starlink.connect.Node[] children = getChildren();
        for ( int i = 0; i < children.length; i++ ) {
            if ( children[ i ].getName().equals( location ) ) {
                return children[ i ];
            }
        }
        return new PotentialLeaf( this, location );
    }

    /**
     * Represents an abstract path which does not yet correspond to an
     * extant file in the remote filestore.  Only if/when the 
     * {@link #getOutputStream} method is called will the file actually
     * be created in the remote filestore.
     */
    class PotentialLeaf implements Leaf {

        private final String name_;
        private final MyspaceBranch parent_;
        private File myFile_;

        /**
         * Constructor.
         *
         * @param  parent  parent branch
         * @param  name  name of the new leaf within <tt>parent</tt>
         */
        PotentialLeaf( MyspaceBranch parent, String name ) {
            name_ = name;
            parent_ = parent;
        }

        public String getName() {
            return name_;
        }

        public Branch getParent() {
            return parent_;
        }

        public DataSource getDataSource() throws IOException {
            throw new FileNotFoundException( "No such node " + this );
        }

        public OutputStream getOutputStream() throws IOException {
            try {
                synchronized ( this ) {
                    if ( myFile_ == null ) {
                        myFile_ = ((Container) parent_.getMyspaceNode())
                                 .addFile( name_ );
                    }
                }
                return myFile_.getOutputStream();
            }
            catch ( TreeClientException e ) {
                throw (IOException) new IOException( e.getMessage() )
                                   .initCause( e );
            }
        }

        public String toString() {
            return parent_.toString() + "/" + name_;
        }
    } 
}
