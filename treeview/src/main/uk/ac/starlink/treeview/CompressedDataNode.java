package uk.ac.starlink.treeview;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.swing.Icon;
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

    public CompressedDataNode( File file ) throws NoSuchDataException {
        this( makeDataSource( file ) );
        setPath( file.getAbsolutePath() );
    }

    public CompressedDataNode( URL url ) throws NoSuchDataException {
        this( new URLDataSource( url ) );
        setPath( url.toString() );
    }

    public CompressedDataNode( String name ) throws NoSuchDataException {
        this( PlainDataNode.makeDataSource( name ) );
    }

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
        this.name = getName( datsrc );
        setLabel( name );
        String path = getPath( datsrc );
        if ( path != null ) {
            setPath( path );
        }
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

    public Icon getIcon() {
        return IconFactory.getInstance().getIcon( IconFactory.COMPRESSED );
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
            try {
                if ( datsrc.isASCII() ) {
                    dv.addPane( "Text view", new ComponentMaker() {
                        public JComponent getComponent() throws IOException {
                            return new TextViewer( datsrc.getInputStream() );
                        }
                    } );
                }
                else {
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
        return fullView;
    }
}
