package uk.ac.starlink.treeview;

import java.util.Iterator;
import java.util.zip.ZipEntry;
import javax.swing.Icon;

public class ZipBranchDataNode extends DefaultDataNode {

    private final ZipFileDataNode zipfilenode;
    private final ZipEntry zipentry;
    private final String path;
    private final String name;

    /**
     * Constructs a ZipBranchDataNode from a ZipEntry and ZipFile.
     *
     * @param  zipfilenode  DataNode representing the zip file within which
     *         this entry lives
     * @param  entry  the ZipEntry object represented by this node
     */
    public ZipBranchDataNode( ZipFileDataNode zipfilenode, ZipEntry entry ) {
        this.zipfilenode = zipfilenode;
        this.zipentry = entry;
        this.path = entry.getName();
        this.name = path.substring( path.substring( 0, path.length() - 1 ) 
                                   .lastIndexOf( '/' ) + 1 );
        setLabel( name );
    }

    public String getName() {
        return name;
    }

    public String getPathSeparator() {
        return "/";
    }

    public Icon getIcon() {
        return IconFactory.getInstance().getIcon( IconFactory.ZIPBRANCH );
    }

    public String getNodeTLA() {
        return "ZPD";
    }

    public String getNodeType() {
        return "Directory in Zip archive";
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        return zipfilenode.getChildIteratorAtLevel( path, this );
    }

}
