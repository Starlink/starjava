package uk.ac.starlink.treeview;

import java.util.Iterator;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.apache.tools.tar.TarEntry;

public class TarEntryDataNode extends DefaultDataNode {

    private TarFileDataNode tarfilenode;
    private TarEntry entry;
    private String name;
    private String path;
    private Icon icon;
    private JComponent fullView;
    private boolean isDirectory;

    /**
     * Constructs a TarEntryDataNode from a TarEntry and TarFile.
     *
     * @param  tarfilenode  DataNode representing the tar file within which
     *         this entry lives
     * @param  entry  the TarEntry object represented by this node
     */
    public TarEntryDataNode( TarFileDataNode tarfilenode, TarEntry entry )
            throws NoSuchDataException {
        this.tarfilenode = tarfilenode;
        this.entry = entry;
        this.path = entry.getName();
        this.name = path.substring( path.substring( 0, path.length() - 1 )
                                   .lastIndexOf( '/' ) + 1 );
        this.isDirectory = entry.isDirectory();
        setLabel( name );
    }

    public String getName() {
        return name;
    }

    public String getPathSeparator() {
        return "";
    }

    public Icon getIcon() {
        if ( icon == null ) {
            icon = IconFactory.getInstance().getIcon( IconFactory.TARENTRY );
        }
        return icon;
    }

    public String getNodeTLA() {
        return isDirectory ? "TRD" : "TRF";
    }

    public String getNodeType() {
        return isDirectory ? "Directory in tar archive" 
                           : "File in tar archive";
    }

    public boolean allowsChildren() {
        return isDirectory;
    }

    public Iterator getChildIterator() {
        return tarfilenode.getChildIteratorAtLevel( path, this );
    }

    public boolean hasFullView() {
        return true;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
            if ( ! isDirectory ) {
                dv.addKeyedItem( "Size", entry.getSize() );
            }
            dv.addSeparator();
            dv.addKeyedItem( "User", entry.getUserName() +
                                     "  (" + entry.getUserId() + ")" );
            dv.addKeyedItem( "Group", entry.getGroupName() +
                                      "  (" + entry.getGroupId() + ")" );
            dv.addSeparator();
            dv.addKeyedItem( "Last modified", entry.getModTime().toString() );
        }
        return fullView;
    }

}
