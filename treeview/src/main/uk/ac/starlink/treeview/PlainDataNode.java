package uk.ac.starlink.treeview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import javax.swing.JComponent;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;

/**
 * Generic data node representing some stream of data.
 * Compression is not taken into account.
 */
public class PlainDataNode extends DefaultDataNode {

    private DataSource datsrc;
    private String name;
    private Boolean isText;

    public PlainDataNode( DataSource datsrc ) throws NoSuchDataException {

        /* Get a data source which is guaranteed not to do automatic 
         * decompression. */
        this.datsrc = datsrc.forceCompression( Compression.NONE );

        this.name = datsrc.getName();
        setLabel( name );
        setIconID( IconFactory.DATA );
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

    public String getPathElement() {
        return name;
    }

    public void configureDetail( DetailViewer dv ) {
        try {
            long size = datsrc.getLength();
            if ( size >= 0 ) {
                dv.addKeyedItem( "Size", size );
            }
            byte[] intro = datsrc.getIntro();
            if ( intro.length > 0 ) {
                if ( TreeviewUtil.isASCII( datsrc.getIntro() ) ) {
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
