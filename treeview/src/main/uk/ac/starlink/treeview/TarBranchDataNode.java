package uk.ac.starlink.treeview;

import java.io.IOException;
import java.util.Iterator;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.apache.tools.tar.TarEntry;

public class TarBranchDataNode extends DefaultDataNode {

    private TarStreamDataNode archivenode;
    private String path;
    private String name;
    private JComponent fullView;

    /**
     * Constructs a TarBranchDataNode from a TarEntry and TarStreamArchive.
     *
     * @param  tarstreamnode  DataNode representing the tar file within which
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
    }

    public String getName() {
        return name;
    }

    public String getPathSeparator() {
        return "/";
    }

    public Icon getIcon() {
        return IconFactory.getIcon( IconFactory.TARBRANCH );
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

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            try {
                int nent = archivenode.getEntriesAtLevel( path ).size();
                dv.addSeparator();
                dv.addKeyedItem( "Number of entries", nent );
            }
            catch ( IOException e ) {
                // oh well
            }
        }
        return fullView;
    }

}
