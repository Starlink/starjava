package uk.ac.starlink.astrogrid;

import java.io.IOException;
import java.io.InputStream;
import org.astrogrid.store.tree.TreeClientServiceException;
import uk.ac.starlink.util.DataSource;

/**
 * DataSource implementation for a MySpace node.
 *
 * @author   Mark Taylor (Starlink)
 * @since    2 Dec 2004
 */
public class MyspaceDataSource extends DataSource {

    private final org.astrogrid.store.tree.File node_;

    /**
     * Constructs a new data source from a MySpace file node.
     *
     * @param  node  node representing a file in MySpace
     */
    public MyspaceDataSource( org.astrogrid.store.tree.File node ) {
        node_ = node;
        setName( node.getName() );
    }

    protected InputStream getRawInputStream() throws IOException {
        try {
            return node_.getInputStream();
        }
        catch ( TreeClientServiceException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }
}
