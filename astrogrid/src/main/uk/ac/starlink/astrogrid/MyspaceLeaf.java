package uk.ac.starlink.astrogrid;

import java.io.IOException;
import java.io.OutputStream;
import org.astrogrid.store.tree.File;
import org.astrogrid.store.tree.TreeClientException;
import uk.ac.starlink.connect.Leaf;
import uk.ac.starlink.util.DataSource;

/**
 * Leaf in virtual filespace implemented on top of a Myspace File.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    25 Feb 2005
 */
class MyspaceLeaf extends MyspaceNode implements Leaf {

    final File myFile_;

    /**
     * Constructor.
     *
     * @param   myFile  Myspace file
     * @param   parent  parent branch
     */
    MyspaceLeaf( File myFile, MyspaceBranch parent ) {
        super( myFile, parent );
        myFile_ = myFile;
    }

    public DataSource getDataSource() {
        return new MyspaceDataSource( myFile_ );
    }

    public OutputStream getOutputStream() throws IOException {
        try {
            return myFile_.getOutputStream();
        }
        catch ( TreeClientException e ) {
            throw (IOException) new IOException( e.getMessage() )
                               .initCause( e );
        }
    }
}
