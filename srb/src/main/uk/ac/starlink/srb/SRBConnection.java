package uk.ac.starlink.srb;

import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileSystem;
import java.io.IOException;
import java.util.Map;
import uk.ac.starlink.connect.Branch;
import uk.ac.starlink.connect.Connection;
import uk.ac.starlink.connect.Connector;

/**
 * Connection to an SRB remote filestore.
 *
 * @author   Mark Taylor (Starlink)
 * @since    7 Mar 2005
 */
public class SRBConnection extends Connection {

    private final SRBBranch root_;
    private boolean isClosed_;

    public SRBConnection( Connector connector, Map keys, SRBFile rootFile )
            throws IOException {
        super( connector, keys );
        root_ = new SRBBranch( rootFile, rootFile );
        setLogoutOnExit( true );
    }

    public Branch getRoot() {
        return root_;
    }

    public boolean isConnected() {
        return ! isClosed_;
    }

    public void logOut() throws IOException {
        if ( ! isClosed_ ) {
            isClosed_ = true;
            ((SRBFileSystem) root_.getFile().getFileSystem()).close();
        }
    }
}
