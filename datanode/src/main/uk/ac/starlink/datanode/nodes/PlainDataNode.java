package uk.ac.starlink.datanode.nodes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import javax.swing.JComponent;
import uk.ac.starlink.datanode.viewers.TextViewer;
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
        registerDataObject( DataType.DATA_SOURCE, datsrc );
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
        catch ( IOException e ) {
            dv.addPane( "Error reading text",
                        new ExceptionComponentMaker( e ) );
        }
    }

}
