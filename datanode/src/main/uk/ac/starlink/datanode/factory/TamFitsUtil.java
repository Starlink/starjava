package uk.ac.starlink.datanode.factory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import uk.ac.starlink.datanode.nodes.DataNode;
import uk.ac.starlink.datanode.nodes.FITSFileDataNode;
import uk.ac.starlink.datanode.nodes.FITSStreamDataNode;
import uk.ac.starlink.datanode.nodes.NdxDataNode;
import uk.ac.starlink.datanode.nodes.NoSuchDataException;
import uk.ac.starlink.util.DataSource;

/**
 * Performs operations dependent on the nom.tam.fits library.
 * Methods from this class must <strong>ONLY</strong> be invoked
 * if {@link uk.ac.starlink.datanode.nodes.NodeUtil#hasTAMFITS} returns true,
 * since nom.tam.fits is not guaranteed to be present at runtime.
 *
 * @author   Mark Taylor
 * @since    9 Mar 2022
 */
public class TamFitsUtil {

    /**
     * Attempts to turn a file into a DataNode based on FITS classes.
     */
    public static DataNode getFitsDataNode( File file, byte[] magic,
                                            DataSource datsrc )
            throws IOException, NoSuchDataException {
        ArrayDataInput istrm = null;
        try {
            InputStream strm1 = datsrc.getInputStream();
            istrm = new BufferedDataInputStream( strm1 );
            Header hdr = new Header( istrm );
            if ( hdr.containsKey( "NDX_XML" ) ) {
                return new NdxDataNode( file );
            }
            else {
                return new FITSFileDataNode( file );
            }
        }
        catch ( FitsException e ) {
            throw new NoSuchDataException( e );
        }
        finally {
            if ( istrm != null ) {
                istrm.close();
            }
        }
    }

    /**
     * Attempts to turn a data source into a DataNode based on FITS classes.
     */
    public static DataNode getFitsStreamDataNode( DataSource datsrc )
            throws NoSuchDataException {
        return new FITSStreamDataNode( datsrc );
    }
}
