package uk.ac.starlink.treeview;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import javax.swing.Icon;

public class ZipBranchDataNode extends DefaultDataNode {

    private final ZipArchiveDataNode ziparchivenode;
    private final ZipEntry zipentry;
    private final String path;
    private String name;

    /**
     * Constructs a ZipBranchDataNode from a ZipEntry and ZipArchive.
     *
     * @param  ziparchivenode  DataNode representing the zip file within which
     *         this entry lives
     * @param  entry  the ZipEntry object represented by this node
     */
    public ZipBranchDataNode( ZipArchiveDataNode ziparchivenode,
                              ZipEntry entry ) {
        this.ziparchivenode = ziparchivenode;
        this.zipentry = entry;
        this.path = entry.getName();
        this.name = path.substring( path.substring( 0, path.length() - 1 ) 
                                   .lastIndexOf( '/' ) + 1 );
        if ( name.endsWith( "/" ) ) {
            name = name.substring( 0, name.length() - 1 );
        }
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
        try {
            return ziparchivenode.getChildIteratorAtLevel( path, this );
        }
        catch ( IOException e ) {
            return Collections
                  .singleton( getChildMaker().makeErrorDataNode( this, e ) )
                  .iterator();
        }
    }

}
