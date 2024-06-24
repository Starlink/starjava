package uk.ac.starlink.srb;

import edu.sdsc.grid.io.GeneralFile;
import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileSystem;
import uk.ac.starlink.connect.Branch;
import uk.ac.starlink.connect.Node;

/**
 * Branch implemenatation based on an SRB file object.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    7 Mar 2005
 */
public class SRBBranch extends SRBNode implements Branch {

    /**
     * Constructor.  The SRB file object must be a directory.
     * No check is made here though, since SRBFile.isDirectory seems like
     * an incredibly slow call to make.
     *
     * @param   dir  SRB file object on which this branch is based - must be
     *               of type directory
     * @param   root  the root of the filesystem in which
     *                <code>dir</code> lives.
     */
    public SRBBranch( SRBFile dir, SRBFile root ) {
        super( dir, root );
    }

    public Node[] getChildren() {
        GeneralFile[] childFiles = getFile().listFiles();
        int nchild = childFiles.length;
        SRBNode[] childNodes = new SRBNode[ nchild ];
        for ( int i = 0; i < nchild; i++ ) {
            childNodes[ i ] = createNode( (SRBFile) childFiles[ i ] );
        }
        return childNodes;
    }

    public Node createNode( String location ) {
        SRBFile file = new SRBFile( (SRBFileSystem) getFile().getFileSystem(),
                                    location );
        if ( ! file.isAbsolute() ) {
            file = new SRBFile( getFile(), location );
        }
        return createNode( file );
    }
}
