package uk.ac.starlink.srb;

import edu.sdsc.grid.io.srb.SRBFile;
import java.io.IOException;
import uk.ac.starlink.connect.Branch;
import uk.ac.starlink.connect.Node;

/**
 * Remote file node based on a JARGON GeneralFile.
 *
 * @author   Mark Taylor
 * @since    7 Mar 2005
 */
public abstract class SRBNode implements Node {

    private final SRBFile file_;
    private final SRBFile root_;
    private final String name_;
    private final boolean isRoot_;

    /**
     * Constructor.
     *
     * @param  srbFile  the SRB file object this node is based on
     * @param  srbRoot  the root of the filesystem in which this file lives
     */
    public SRBNode( SRBFile srbFile, SRBFile srbRoot ) {
        file_ = srbFile;
        root_ = srbRoot;
        isRoot_ = srbFile.equals( srbRoot );
        String name = file_.getName();
        name_ = name.substring( name.indexOf( '/' ) + 1 );
    }

    public String getName() {
        return name_;
    }

    public Branch getParent() {
        return isRoot_ 
             ? null
             : new SRBBranch( (SRBFile) file_.getParentFile(), root_ );
    }

    public SRBFile getFile() {
        return file_;
    }

    SRBFile getRoot() {
        return root_;
    }

    public boolean equals( Object other ) {
        if ( other.getClass().equals( getClass() ) ) {
            return file_.equals( ((SRBNode) other).file_ );
        }
        else {
            return false;
        }
    }

    public int hashCode() {
        return file_.hashCode();
    }

    public String toString() {
        return file_.toString();
    }

    /**
     * Creates a new node from an SRB file object.
     *
     * @param  file  SRB file object
     * @return  node based on <tt>file</tt>
     */
    SRBNode createNode( SRBFile file ) {
        boolean isDir = file.isDirectory();
        return isDir ? (SRBNode) new SRBBranch( file, root_ ) 
                     : (SRBNode) new SRBLeaf( file, root_ );
    }
}
