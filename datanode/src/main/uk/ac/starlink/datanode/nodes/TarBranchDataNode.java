package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
import java.util.Iterator;
import org.apache.tools.tar.TarEntry;

public class TarBranchDataNode extends DefaultDataNode {

    private TarStreamDataNode archivenode;
    private String path;
    private String name;

    /**
     * Constructs a TarBranchDataNode from a TarEntry and TarStreamArchive.
     *
     * @param  tarnode  DataNode representing the tar file within which
     *         this entry lives
     * @param  entry  the TarEntry object represented by this node
     */
    public TarBranchDataNode( TarStreamDataNode tarnode, TarEntry entry ) {
        this.archivenode = tarnode;
        this.path = entry.getName();
        this.name = path.substring( path.substring( 0, path.length() - 1 )
                                   .lastIndexOf( '/' ) + 1 );
        if ( name.endsWith( "/" ) ) {
            name = name.substring( 0, name.length() - 1 );
        }
        setLabel( name );
        setIconID( IconFactory.TARBRANCH );
    }

    public String getName() {
        return name;
    }

    public String getPathElement() {
        return name;
    }

    public String getPathSeparator() {
        return "/";
    }

    public String getNodeTLA() {
        return "TRD";
    }

    public String getNodeType() {
        return "Directory in Tar archive";
    }

    public boolean allowsChildren() {
        return true;
    }

    public Iterator getChildIterator() {
        return archivenode.getChildIteratorAtLevel( path, this );
    }

    public void configureDetail( DetailViewer dv ) {
        try {
            int nent = archivenode.getEntriesAtLevel( path ).size();
            dv.addKeyedItem( "Number of entries", nent );
        }
        catch ( IOException e ) {
            // oh well
        }
    }

}
