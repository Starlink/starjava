package uk.ac.starlink.datanode.nodes;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.swing.JComponent;
import uk.ac.starlink.datanode.viewers.TextViewer;
import uk.ac.starlink.util.Compression;
import uk.ac.starlink.util.DataSource;

/**
 * Generic data node representing a stream of data compressed according
 * to a known compression technique.
 */
public class CompressedDataNode extends DefaultDataNode {

    private final DataSource datsrc;
    private final Compression compress;
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
        registerDataObject( DataType.DATA_SOURCE, datsrc );
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

    public void configureDetail( DetailViewer dv ) {
        dv.addKeyedItem( "Compression format", compress );
        long rawLeng = datsrc.getRawLength();
        long cookLeng = datsrc.getLength();
        if ( rawLeng >= 0 ) {
            dv.addKeyedItem( "Compressed size", rawLeng );
        }
        if ( cookLeng >= 0 ) {
            dv.addKeyedItem( "Decompressed size", cookLeng );
        }

        /* Add a text viewer panel for appropriate data. */
        try {
            byte[] intro = datsrc.getIntro();
            if ( intro.length > 0 ) {
                if ( NodeUtil.isASCII( intro ) ) {
                    dv.addPane( "Text view", new ComponentMaker() {
                        public JComponent getComponent() throws IOException {
                            return new TextViewer( datsrc.getInputStream() );
                        }
                    } );
                }
            }
        }
        catch ( final IOException e ) {
            dv.addPane( "Error reading text",
                        new ExceptionComponentMaker( e ) );
        }
    }
}
