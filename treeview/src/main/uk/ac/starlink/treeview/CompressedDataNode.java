package uk.ac.starlink.treeview;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.swing.JComponent;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;
import uk.ac.starlink.util.URLDataSource;

/**
 * Generic data node representing a stream of data compressed according
 * to a known compression technique.
 */
public class CompressedDataNode extends DefaultDataNode {

    private final DataSource datsrc;
    private final Compression compress;
    private JComponent fullView;
    private final String name;

    public CompressedDataNode( DataSource datsrc ) throws NoSuchDataException {
        this.datsrc = datsrc;
        try {
            compress = datsrc.getCompression();
        }
        catch ( IOException e ) {
            throw new NoSuchDataException( e );
        }
        if ( compress == Compression.NONE ) {
            throw new NoSuchDataException( 
                "Data source uses no known compression format" );
        }
        this.name = datsrc.getName();
        setLabel( name );
        setIconID( IconFactory.COMPRESSED );
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return "(" + compress + ")";
    }

    public String getNodeTLA() {
        return "CMP";
    }

    public String getNodeType() {
        return "Compressed data";
    }

    public String getPathElement() {
        return name;
    }

    public JComponent getFullView() {
        if ( fullView == null ) {
            DetailViewer dv = new DetailViewer( this );
            fullView = dv.getComponent();
            dv.addSeparator();
            dv.addKeyedItem( "Compression format", compress );
            long rawLeng = datsrc.getRawLength();
            long cookLeng = datsrc.getLength();
            if ( rawLeng >= 0 ) {
                dv.addKeyedItem( "Compressed size", rawLeng );
            }
            if ( cookLeng >= 0 ) {
                dv.addKeyedItem( "Decompressed size", cookLeng );
            }
            try {
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
            catch ( final IOException e ) {
                dv.addPane( "Error reading data", new ComponentMaker() {
                    public JComponent getComponent() {
                        return new TextViewer( e );
                    }
                } );
            }
        }
        return fullView;
    }
}
