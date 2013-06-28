package uk.ac.starlink.cef;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataSource;

public class CefTableBuilder implements TableBuilder {

    public String getFormatName() {
        return "CEF";
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public void streamStarTable( InputStream in, TableSink sink,
                                 String pos ) throws IOException {
        CefReader rdr = null;
        try {
            rdr = new CefReader( in );
            sink.acceptMetadata( new CefStarTable( rdr ) {
                public RowSequence getRowSequence() {
                    throw new UnsupportedOperationException();
                }
            } );
            while ( rdr.next() ) {
                sink.acceptRow( rdr.getRow() );
            }
            sink.endRows();
        }
        finally {
            if ( rdr != null ) {
                rdr.close();
            }
        }
    }

    public StarTable makeStarTable( final DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storagePolicy )
            throws IOException {
        CefReader rdr =
            new CefReader( new BufferedInputStream( datsrc.getInputStream() ) );
        rdr.close();
        return new CefStarTable( rdr ) {
            public RowSequence getRowSequence() throws IOException {
                return new CefReader(
                    new BufferedInputStream( datsrc.getInputStream() ) );
            }
        };
    }

    private static abstract class CefStarTable extends AbstractStarTable {

        private final CefReader rdr_;
        private final ColumnInfo[] colInfos_;

        CefStarTable( CefReader rdr ) {
            rdr_ = rdr;
            colInfos_ = rdr.getColumnInfos();
            setParameters( new ArrayList<DescribedValue>(
                               Arrays.asList( rdr.getParameters() ) ) );
            String name = rdr.getName();
            if ( name != null ) {
                setName( name );
            }
        }

        public int getColumnCount() {
            return colInfos_.length;
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return colInfos_[ icol ];
        }

        public long getRowCount() {
            return -1L;
        }
    }
}
