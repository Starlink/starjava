package uk.ac.starlink.treeview;

import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.awt.*;
import javax.swing.*;

/**
 * A {@link DataNode} representing a zip file entry.
 *
 * @author   Mark Taylor (Starlink)
 * @version  $Id$
 */
public class ZipLeafDataNode extends DefaultDataNode {
    private static Icon icon;

    private String name;
    private ZipFile zfile;
    private ZipEntry zentry;
    private JComponent fullView;

    /**
     * Initialises a <code>ZipLeafDataNode</code> from a <code>ZipFile</code>
     * and a <code>ZipEntry</code> object.
     * 
     * @param  zipfile   the <code>ZipFile</code> from which the entry comes
     * @param  zipentry  a <code>ZipEntry</code> from which the node is
     *                   to be formed
     */
    public ZipLeafDataNode( ZipFile zipfile, ZipEntry zipentry ) {
        this.zfile = zipfile;
        this.zentry = zipentry;
        String path = zentry.getName();
        this.name = path.substring( path.lastIndexOf( '/' ) + 1 ) ;
        setLabel( name );
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

    /**
     * Returns string "ZPE".
     *
     * @return  "ZPE"
     */
    public String getNodeTLA() {
        return "ZPE";
    }

    public String getNodeType() {
        return "Zip file normal entry";
    }

    public boolean hasFullView() {
        return true;
    }
    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
            long size = zentry.getSize();
            if ( size > -1 ) {
                dv.addKeyedItem( "Size", size );
            }
            long csize = zentry.getCompressedSize();
            if ( csize > -1 ) {
                dv.addKeyedItem( "Compressed size", csize );
            }
            long time = zentry.getTime();
            if ( time != -1 ) {
                dv.addKeyedItem( "Last modified", new Date( time ) );
            }
            String comm = zentry.getComment();
            if ( comm != null ) {
                dv.addKeyedItem( "Comment", comm );
            }

            /* If it looks like a text file, add the option to view the
             * content. */
            try {
                if ( new StreamCheck( zfile.getInputStream( zentry ) )
                    .isText() ) {
                    dv.addPane( "File text", new ComponentMaker() {
                        public JComponent getComponent() throws IOException {
                            Reader rdr = new InputStreamReader( 
                                zfile.getInputStream( zentry ) );
                            return new TextViewer( rdr );
                        }
                    } );
                }
            }
            catch ( IOException e ) {}
        }
        return fullView;
    }


}
