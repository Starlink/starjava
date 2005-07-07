package uk.ac.starlink.astrogrid;

import java.io.FilterInputStream;
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
        InputStream strm;
        try {
            strm = node_.getInputStream();
        }
        catch ( TreeClientServiceException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }

        /* At least at some versions of Sun's JRE (e.g. 1.4.2_02,
         * though not 1.4.1_03), the FileStoreInputStream claims
         * erroneously that it supports mark/reset (the behaviour is 
         * probably down to sun.net.www.MeteredStream).  
         * We correct that here.  This workaround could be removed if
         * FileStoreInputStream gets fixed. */
        strm = new FilterInputStream( strm ) {
            public boolean markSupported() {
                return false;
            }
        };
        return strm;
    }
}
