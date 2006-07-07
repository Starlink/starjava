package uk.ac.starlink.table.formats;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataSource;

/**
 * A table builder which reads tables in IPAC format.
 * The data format is defined at
 * <a href="http://irsa.ipac.caltech.edu/applications/DDGEN/Doc/ipac_tbl.html"
           >http://irsa.ipac.caltech.edu/applications/DDGEN/Doc/ipac_tbl.html</a>.
 *
 * @author   Mark Taylor
 * @since    7 Feb 2006
 */
public class IpacTableBuilder implements TableBuilder {

    /**
     * Returns "IPAC".
     */
    public String getFormatName() {
        return "IPAC";
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public void streamStarTable( InputStream istrm, TableSink sink,
                                 String pos ) throws IOException {
        IpacReader reader = null;
        try {
            reader = new IpacReader( istrm );
            sink.acceptMetadata( new IpacStarTable( reader ) {
                public RowSequence getRowSequence() throws IOException {
                    throw new UnsupportedOperationException();
                }
            } );
            while ( reader.next() ) {
                sink.acceptRow( reader.getRow() );
            }
            sink.endRows();
        }
        finally {
            if ( reader != null ) {
                reader.close();
            }
        }
    }

    public StarTable makeStarTable( final DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {
        IpacReader reader = 
            new IpacReader(
                new BufferedInputStream( datsrc.getInputStream() ) );
        reader.close();
        return new IpacStarTable( reader ) {
            public RowSequence getRowSequence() throws IOException {
                return new IpacReader(
                    new BufferedInputStream( datsrc.getInputStream() ) );
            }
        };
    }

    /**
     * Partial StarTable implementation based on an IpacReader.
     * This abstract class does not implement <code>getRowSequence()</code>.
     */
    private abstract static class IpacStarTable extends AbstractStarTable {

        private final IpacReader reader_;

        /**
         * Constructor.
         *
         * @param  reader   IPAC reader
         */
        IpacStarTable( IpacReader reader ) {
            reader_ = reader;
            setParameters( new ArrayList( Arrays.asList( reader_
                                                        .getParameters() ) ) );
        }

        public int getColumnCount() {
            return reader_.getColumnCount();
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return reader_.getColumnInfo( icol );
        }

        public long getRowCount() {
            return -1L;
        }
    }
}
