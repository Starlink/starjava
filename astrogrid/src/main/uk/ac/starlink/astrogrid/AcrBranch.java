package uk.ac.starlink.astrogrid;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.connect.Branch;
import uk.ac.starlink.connect.ErrorLeaf;
import uk.ac.starlink.connect.Leaf;
import uk.ac.starlink.connect.Node;
import uk.ac.starlink.util.DataSource;

/**
 * Branch implementaton which uses the ACR to access MySpace.
 *
 * @author   Mark Taylor
 * @since    9 Sep 2005
 */
class AcrBranch extends AcrNode implements Branch {

    private static final Logger logger_ =
            Logger.getLogger( "uk.ac.starlink.astrogrid" );
    private final AcrConnection connection_;
    private Node[] children_;

    /**
     * Constructor.
     *
     * @param   connection  the connection object
     * @param   uri  the permanent URI which identifies this branch
     * @param   name  the name (excluding path) of this branch
     * @param   branch  this branch's parent (null if root)
     */
    public AcrBranch( AcrConnection connection, String uri, String name,
                      AcrBranch parent ) {
        super( connection, uri, name, parent );
        connection_ = connection;
    }

    public Node[] getChildren() {
        if ( connection_.getCacheDirectories() ) {
            if ( children_ == null ) {
                children_ = readChildren();
            }
            return children_;
        }
        else {
            return readChildren();
        }
    }

    private Node[] readChildren() {
        try {
            return getAcrChildNodes();
        }
        catch ( IOException e ) {
            logger_.log( Level.WARNING, "Error connecting to ACR", e );
            return new Node[] { new ErrorLeaf( this, e ) };
        }
    }

    private Node[] getAcrChildNodes() throws IOException {
        Object[] childInfos = 
            (Object[]) executeMyspace( "listNodeInformation",
                                       new Object[] { uri_ } );
        int nChild = childInfos.length;
        List childNodeList = new ArrayList();
        for ( int i = 0; i < nChild; i++ ) {
            Map childInfo = (Map) childInfos[ i ];
            String childUri = (String) childInfo.get( "id" );
            String childName = (String) childInfo.get( "name" );
            boolean isFile =
                ((Boolean) childInfo.get( "file" )).booleanValue();
            boolean isFolder =
                ((Boolean) childInfo.get( "folder" )).booleanValue();
            if ( isFile != isFolder ) {
                if ( isFolder ) {
                    childNodeList.add( new AcrBranch( connection_, childUri,
                                                      childName, this ) );
                }
                else if ( isFile ) {
                    childNodeList.add( new AcrLeaf( connection_, childUri, 
                                                    childName, this ) );
                }
            }
            else {
                logger_.warning( childUri 
                               + ": is it a file or is it a folder??"
                               + " - skipping" );
            }
        }
        return (Node[]) childNodeList.toArray( new Node[ 0 ] );
    }

    public Node createNode( String name ) {
        Node[] children = getChildren();
        for ( int i = 0; i < children.length; i++ ) {
            if ( children[ i ].getName().equals( name ) ) {
                return children[ i ];
            }
        }
        return new PotentialChildLeaf( name );
    }

    /**
     * Represents an abstract path which does not yet correspond to an
     * extant file in the remote filestore.  Only if/when the
     * {@link #getOutputStream} method is called will the file actually
     * be created in the remote filestore.
     */
    private class PotentialChildLeaf implements Leaf {

        private final String name_;
        private final AcrBranch parent_;
        private String childUri_;

        /**
         * Constructor. 
         *
         * @param  name of the new leaf within the parent
         */
        PotentialChildLeaf( String name ) {
            name_ = name;
            parent_ = AcrBranch.this;
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
            synchronized ( this ) {
                if ( childUri_ == null ) {
                    childUri_ = (String)
                        executeMyspace( "createChildFile", 
                                        new Object[] { parent_.uri_, name_ } );
                }
            }
            return connection_.getOutputStream( childUri_ );
        }

        public String toString() {
            return childUri_ == null ? ( parent_.toString() + "/" + name_ )
                                     : childUri_;
        }
    }
}
