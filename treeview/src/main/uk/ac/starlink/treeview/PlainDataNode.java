package uk.ac.starlink.treeview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.JComponent;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.URLDataSource;

/**
 * Generic data node representing some stream of data.
 * Compression is not taken into account.
 */
public class PlainDataNode extends DefaultDataNode {

    private DataSource datsrc;
    private String name;
    private JComponent fullView;
    private Boolean isText;

    public PlainDataNode( DataSource datsrc ) throws NoSuchDataException {

        /* Get a data source which is guaranteed not to do automatic 
         * decompression. */
        this.datsrc = datsrc.forceCompression( Compression.NONE );

        this.name = datsrc.getName();
        setLabel( name );
    }

    public String getName() {
        return name;
    }

    public String getNodeTLA() {
        return "DATA";
    }

    public String getNodeType() {
        return "Unknown data";
    }

    public Icon getIcon() {
        return IconFactory.getIcon( IconFactory.DATA );
    }

    public String getPathElement() {
        return name;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
            addDataViews( dv, datsrc );
        }
        return fullView;
    }

    public static void addDataViews( DetailViewer dv, 
                                     final DataSource datsrc ) {
        try {
            long size = datsrc.getLength();
            if ( size >= 0 ) {
                dv.addKeyedItem( "Size", size );
            }
            if ( ! datsrc.isEmpty() ) {
                if ( datsrc.isASCII() ) {
                    dv.addPane( "Text view", new ComponentMaker() {
                        public JComponent getComponent() throws IOException {
                            return new TextViewer( datsrc.getInputStream() );
                        }
                    } );
                }
                dv.addPane( "Hex dump", new ComponentMaker() {
                    public JComponent getComponent() throws IOException {
                        return new HexDumper( datsrc.getInputStream(), 
                                              datsrc.getLength() );
                    }
                } );
            }
        }
        catch ( final IOException e ) {
            dv.addPane( "Error reading data", new ComponentMaker() {
                public JComponent getComponent() {
                    return new TextViewer( e );
                }
            } );
        }
    }

}
