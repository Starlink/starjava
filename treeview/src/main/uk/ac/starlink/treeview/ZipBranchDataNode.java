package uk.ac.starlink.treeview;

import java.util.*;
import java.util.zip.*;
import javax.swing.*;

/**
 * A {@link DataNode} representing a zip file entry which is represents
 * a directory.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class ZipBranchDataNode extends DefaultDataNode {
    private static Icon icon;

    private String name;
    private ZipFile zfile;
    private ZipEntry zentry;
    private String path;

    /**
     * Intializes a ZipBranchDataNode from its pathname.
     *
     * @param  zipfile  the ZipFile to which the entry refers
     * @param  name     the pathname of the directory entry within the zipfile
     */
    public ZipBranchDataNode( ZipFile zipfile, String path ) {
        this.zfile = zipfile;
        this.zentry = null;
        this.path = path;
        this.name = path.substring( path.substring( 0, path.length() - 1 )
                                        .lastIndexOf( '/' ) + 1 );
        setLabel( name );
    }

    /**
     * Initializes a ZipBranchDataNode from a ZipEntry object.
     *
     * @param  zipfile   the ZipFile from which the entry comes
     * @param  zipentry  the ZipEntry from which the node is to be formed
     */
    public ZipBranchDataNode( ZipFile zipfile, ZipEntry zipentry ) {
        this( zipfile, zipentry.getName() );
        this.zentry = zipentry;
    }

    public String getName() {
        return name;
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( IconFactory.ZIPENTRY );
        }
        return icon;
    }

    public String getNodeTLA() {
        return "ZPD";
    }

    public String getNodeType() {
        return "Zip file directory entry";
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        DataNode[] nodes = ZipFileDataNode.getEntriesAtLevel( zfile, path );
        final Iterator nodeIt = Arrays.asList( nodes ).iterator();
        return new Iterator() {
            public boolean hasNext() {
                return nodeIt.hasNext();
            }
            public Object next() {
                DataNode node = (DataNode) nodeIt.next();
                try {
                    return getChildMaker().makeDataNode( node );
                }
                catch ( NoSuchDataException e ) {
                    return node;
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
