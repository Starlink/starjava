package uk.ac.starlink.treeview;

import java.util.Iterator;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

public class TarBranchDataNode extends DefaultDataNode {

    private TarFileDataNode tarfilenode;
    private TarEntry entry;
    private String name;
    private String path;
    private JComponent fullView;

    /**
     * Constructs a TarBranchDataNode from a TarEntry and TarFile.
     *
     * @param  tarfilenode  DataNode representing the tar file within which
     *         this entry lives
     * @param  entry  the TarEntry object represented by this node
     */
    public TarBranchDataNode( TarFileDataNode tarfilenode, TarEntry entry ) {
        this.tarfilenode = tarfilenode;
        this.entry = entry;
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
        return IconFactory.getInstance().getIcon( IconFactory.TARBRANCH );
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
            boolean implied = ( entry.getFile() == null );
            dv.addKeyedItem( "Entry in archive", ! implied );
            if ( ! implied ) {
                dv.addKeyedItem( "User", entry.getUserName() +
                                         "  (" + entry.getUserId() + ")" );
                dv.addKeyedItem( "Group", entry.getGroupName() +
                                          "  (" + entry.getGroupId() + ")" );
                dv.addSeparator();
                dv.addKeyedItem( "Last modified", 
                                 entry.getModTime().toString() );
            }
        }
        return fullView;
    }

}
