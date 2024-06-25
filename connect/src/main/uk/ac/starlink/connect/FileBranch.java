package uk.ac.starlink.connect;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.filechooser.FileSystemView;

/**
 * Branch representing a directory file.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public class FileBranch extends FileNode implements Branch {

    private boolean hidingEnabled_ = true;
    private static final FileSystemView fsv_ = 
        FileSystemView.getFileSystemView();

    /**
     * Constructs a branch from a File object representing an existing
     * directory.
     *
     * @param  dir  directory
     * @throws  IllegalArgumentException  if <code>dir</code> is not a directory
     */
    public FileBranch( File dir ) {
        super( dir );
        if ( ! dir.isDirectory() ) {
            throw new IllegalArgumentException( dir + " is not directory" );
        }
    }

    public Node[] getChildren() {
        File[] files = file_.listFiles();
        if ( files == null ) {
            return new Node[ 0 ];
        }
        List<Node> nodeList = new ArrayList<Node>( files.length );
        for ( int i = 0; i < files.length; i++ ) {
            File file = files[ i ];
            if ( ! hidingEnabled_ || ! fsv_.isHiddenFile( file ) ) {
                nodeList.add( createNode( file ) );
            }
        }
        return nodeList.toArray( new Node[ 0 ] );
    }

    public Node createNode( String location ) {
        File file = new File( location );
        if ( ! file.isAbsolute() ) {
            file = new File( file_, location );
        }
        Node node = createNode( file );
        if ( node instanceof FileBranch ) {
            ((FileBranch) node).setHidingEnabled( hidingEnabled_ );
        }
        return node;
    }

    /**
     * Indicates whether files marked as hidden by the default FileSystemView
     * are shown or not.
     *
     * @return  true  if hidden files are not shown
     */
    public boolean isHidingEnabled() {
        return hidingEnabled_;
    }

    /**
     * Sets whether files marked as hidden by the default FileSystemView
     * are shown or not.  The default is true.
     *
     * @param   hiding   true  to hide hidden files
     */
    public void setHidingEnabled( boolean hiding ) {
        hidingEnabled_ = hiding;
    }
}
