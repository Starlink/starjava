package uk.ac.starlink.connect;

import java.io.File;

/**
 * Node representing a file {@link java.io.File} in a local filesystem.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public abstract class FileNode implements Node {
    final File file_;

    /**
     * Constructs a node from a File.
     *
     * @param  file  file
     */
    protected FileNode( File file ) {
        file_ = file;
    }

    public String getName() {
        return file_.getParentFile() == null
             ? File.separator
             : file_.getName();
    }

    public Branch getParent() {
        File parent = file_.getParentFile();
        return parent == null ? null
                              : new FileBranch( parent );
    }

    public boolean equals( Object other ) {
        if ( other instanceof FileNode ) {
            return file_.equals( ((FileNode) other).file_ );
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

}
