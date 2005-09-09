package uk.ac.starlink.astrogrid;

import java.io.IOException;
import java.io.OutputStream;
import uk.ac.starlink.connect.Leaf;
import uk.ac.starlink.util.DataSource;

/**
 * Leaf implementation which uses the ACR to talk to MySpace.
 *
 * @author   Mark Taylor
 * @since    9 Sep 2005
 */
class AcrLeaf extends AcrNode implements Leaf {

    /**
     * Constructor.
     *
     * @param   connection  the connection object
     * @param   uri  the permanent URI which identifies this leaf
     * @param   name  the name (excluding path) of this leaf
     * @param   branch  this leaf's parent (null if root)
     */ 
    public AcrLeaf( AcrConnection connection, String uri, String name,
                    AcrBranch parent ) {
        super( connection, uri, name, parent );
    }

    public DataSource getDataSource() throws IOException {
        return new AcrDataSource( connection_, uri_ );
    }

    public OutputStream getOutputStream() throws IOException {
        return connection_.getOutputStream( uri_ );
    }
}
