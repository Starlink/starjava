package uk.ac.starlink.connect;

import java.io.File;

/**
 * Branch representing a directory file.
 *
 * @author   Mark Taylor (Starlink)
 * @since    18 Feb 2005
 */
public class FileBranch extends FileNode implements Branch {

    private final File dir_;

    /**
     * Constructs a branch from a File object representing an existing
     * directory.
     *
     * @param  dir  directory
     * @throws  IllegalArgumentException  if <tt>dir</tt> is not a directory
     */
    public FileBranch( File dir ) {
        super( dir );
        if ( ! dir.isDirectory() ) {
            throw new IllegalArgumentException( dir + " is not directory" );
        }
        dir_ = dir;
    }

    public Node[] getChildren() {
        File[] files = dir_.listFiles();
        Node[] children = new Node[ files.length ];
        for ( int i = 0; i < files.length; i++ ) {
            children[ i ] = createNode( files[ i ] );
        }
        return children;
    }

    public Node createNode( String location ) {
        File file = new File( location );
        if ( ! file.isAbsolute() ) {
            file = new File( dir_, location );
        }
        return createNode( file );
    }
}
