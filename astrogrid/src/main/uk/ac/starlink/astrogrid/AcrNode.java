package uk.ac.starlink.astrogrid;

import java.io.IOException;
import java.util.logging.Logger;
import uk.ac.starlink.connect.Branch;
import uk.ac.starlink.connect.Node;

/**
 * Node implementation which uses the ACR to talk to MySpace.
 *
 * @author   Mark Taylor
 * @since    9 Sep 2005
 */
class AcrNode implements Node {

    protected final AcrConnection connection_;
    protected final String uri_;
    protected final AcrBranch parent_;
    private final String name_;

    private final static Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.astrogrid" );

    /**
     * Constructor.
     *
     * @param   connection  the connection object
     * @param   uri  the permanent URI which identifies this node
     * @param   name  the name (excluding path) of this node
     * @param   branch  this node's parent (null if root)
     */ 
    public AcrNode( AcrConnection connection, String uri, String name,
                    AcrBranch parent ) {
        connection_ = connection;
        uri_ = uri;
        name_ = name;
        parent_ = parent;
    }

    public Branch getParent() {
        return parent_;
    }

    public String getName() {
        return name_;
    }

    public String toString() {
        return uri_;
    }

    /**
     * Convenience method which executes a multi-argument 
     * <code>astrogrid.myspace</code> method using the ACR.
     * 
     * @param  myspaceCmd  command name, excluding the "astrogrid.myspace"
     *         prefix
     * @param  args  argument array
     * @return  result of the call
     */
    Object executeMyspace( String myspaceCmd, Object[] args )
            throws IOException {
        return connection_.execute( "astrogrid.myspace." + myspaceCmd, args );
    }

    /**
     * Convenience method which executes a no-argument 
     * <code>astrogrid.myspace</code> method using the ACR.
     * 
     * @param  myspaceCmd  command name, excluding the "astrogrid.myspace"
     *         prefix
     * @return  result of the call
     */
    Object executeMyspace( String myspaceCmd ) throws IOException {
        return executeMyspace( myspaceCmd, null );
    }

}
