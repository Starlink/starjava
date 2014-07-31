package uk.ac.starlink.gbin;

import java.awt.datatransfer.DataFlavor;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TableFormatException;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.util.DataSource;

public class GbinTableBuilder implements TableBuilder {

    public String getFormatName() {
        return "GBIN";
    }

    public boolean canImport( DataFlavor flavor ) {
        return false;
    }

    public void streamStarTable( InputStream in, TableSink sink, String pos )
            throws IOException {
        GbinTableReader trdr = null;
        try {
            trdr = new GbinTableReader( new BufferedInputStream( in ) );
            sink.acceptMetadata( new GbinStarTable( trdr ) {
                public RowSequence getRowSequence() {
                    throw new UnsupportedOperationException();
                }
            } );
            while ( trdr.next() ) {
                sink.acceptRow( trdr.getRow() );
            }
            sink.endRows();
        }
        finally {
            if ( trdr != null ) {
                trdr.close();
            }
        }
    }

    public StarTable makeStarTable( final DataSource datsrc, boolean wantRandom,
                                    StoragePolicy storage )
            throws IOException {
        if ( ! GbinObjectReader.isMagic( datsrc.getIntro() ) ) {
            throw new TableFormatException( "Not GBIN" );
        }
        GbinTableReader trdr =
            new GbinTableReader(
                new BufferedInputStream( datsrc.getInputStream() ) );
        trdr.close();
        return new GbinStarTable( trdr ) {
            public RowSequence getRowSequence() throws IOException {
                return new GbinTableReader(
                           new BufferedInputStream( datsrc.getInputStream() ) );
            }
        };
    }

    private static abstract class GbinStarTable extends AbstractStarTable {
        private final GbinTableReader trdr_;

        GbinStarTable( GbinTableReader trdr ) {
            trdr_ = trdr;
        }

        public int getColumnCount() {
            return trdr_.getColumnCount();
        }

        public ColumnInfo getColumnInfo( int icol ) {
            return trdr_.getColumnInfo( icol );
        }

        public long getRowCount() {
            return trdr_.getRowCount();
        }
    }
}
