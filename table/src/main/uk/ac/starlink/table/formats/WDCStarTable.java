package uk.ac.starlink.table.formats;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.ReaderRowSequence;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.util.DataSource;

class WDCStarTable extends AbstractStarTable {

    private WDCReader wknow;
    private DataSource datsrc;
    private ColumnInfo[] columnInfos;

    public WDCStarTable( WDCReader wknow, DataSource datsrc ) {
        this.wknow = wknow;
        this.datsrc = datsrc;
        this.columnInfos = wknow.getColumnInfos();
    }

    public int getColumnCount() {
        return columnInfos.length;
    }

    public long getRowCount() {
        return -1L;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return columnInfos[ icol ];
    }

    public RowSequence getRowSequence() throws IOException {

        /* Get an input stream for the lines. */
        InputStream istrm = datsrc.getInputStream();
        final BufferedInputStream strm = 
            ( istrm instanceof BufferedInputStream ) 
                ? (BufferedInputStream) istrm 
                : new BufferedInputStream( istrm );

        /* Eat up the header bytes (discard the result). */
        WDCReader wknow1 = new WDCReader( strm );

        /* Return a RowSequence based on the format. */
        return new ReaderRowSequence() {
            public Object[] readRow() throws IOException {
                String line = WDCTableBuilder.readLine( strm );
                return ( line == null ) ? null : wknow.decodeLine( line );
            }
            public void close() throws IOException {
                strm.close();
            }
        };
    }

}
