package uk.ac.starlink.connect;

import java.io.File;
import java.io.IOException;

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
        file_ = file.getAbsoluteFile();
    }

    public String getName() {
        return file_.getParentFile() == null ? file_.toString()
                                             : file_.getName();
    }

    public Branch getParent() {
        File parent = file_.getParentFile();
        return ( parent != null && parent.isDirectory() )
             ? new FileBranch( parent )
             : null;
    }

    public File getFile() {
        return file_;
    }

    public boolean equals( Object other ) {
        if ( other.getClass().equals( getClass() ) ) {
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

    /**
     * Creates a new FileNode from a File.
     *
     * @param   file   file
     * @return  branch or leaf representing <code>file</code>
     */
    public static FileNode createNode( File file ) {
        return file.isDirectory() ? (FileNode) new FileBranch( file )
                                  : (FileNode) new FileLeaf( file );
    }
}
