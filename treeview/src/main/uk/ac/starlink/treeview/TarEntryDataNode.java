package uk.ac.starlink.treeview;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import javax.swing.Icon;
import javax.swing.JComponent;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;

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

            /* Allow to look at the content. */
            if ( entry.getSize() > 0 ) {
                StreamCheck schk;
                try {
                    schk = new StreamCheck( getContentStream() );
                }
                catch ( IOException e ) {
                    schk = null;
                }
                if ( schk != null ) {
                    if ( schk.isText() ) {
                        dv.addPane( "File text", new ComponentMaker() {
                            public JComponent getComponent() 
                                    throws IOException {
                                InputStream istrm = getContentStream();
                                Reader rdr = new InputStreamReader( istrm );
                                return new TextViewer( rdr );
                            }
                        } );
                    }
                    else {
                        dv.addPane( "Hex dump", new ComponentMaker() {
                            public JComponent getComponent()
                                    throws IOException {
                                InputStream istrm = getContentStream();
                                return new HexDumper( istrm, 
                                                      (int) entry.getSize() );
                            }
                        } );
                    }
                }
            }
        }
        return fullView;
    }

    /**
     * Returns an InputStream representing the content of this 
     * entry, if it can be done.  Returns <tt>null</tt> if it can't.
     */
    private InputStream getContentStream() throws IOException {
        TarInputStream tstrm = tarfilenode.getTarInputStream();
        if ( tstrm == null ) {
            return null;
        }
        while ( true ) {
            TarEntry ent = tstrm.getNextEntry();
            if ( ent.equals( this.entry ) ) {
                return tstrm;
            }
            if ( ent == null ) {
                throw new FileNotFoundException( "Entry not found in file" );
            }
        }
    }

}
