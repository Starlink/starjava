package uk.ac.starlink.datanode.nodes;

import java.io.IOException;
import java.io.InputStream;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import uk.ac.starlink.util.DataSource;

/**
 * A DataNode representing a FITS file whose data comes from a stream.
 *
 * @author   Mark Taylor (Starlink)
 */
public class FITSStreamDataNode extends FITSDataNode {

    DataSource datsrc;

    public FITSStreamDataNode( DataSource datsrc ) throws NoSuchDataException {
        super( datsrc );
        this.datsrc = datsrc;
    }

    protected ArrayDataMaker getArrayData( final long start, long size ) {
        return new ArrayDataMaker() {
            public ArrayDataInput getArrayData() throws IOException {
                InputStream istrm = datsrc.getInputStream();
                for ( long pos = 0L; pos < start; 
                      pos += istrm.skip( start - pos ) );
                return new BufferedDataInputStream( istrm );
            }
            public DataSource getDataSource() {
                return datsrc;
            }
            public long getOffset() {
                return start;
            }
        };
    }

}
