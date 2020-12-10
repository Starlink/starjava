package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.ttools.plot2.Slow;

/**
 * Supplies selected mask and coordinate data relating to a single table.
 *
 * @author   Mark Taylor
 * @since    10 Dec 2020
 */
public class TableCachedData {

    private final long nrow_;
    private final List<Supplier<CachedReader>> maskCols_;
    private final List<Supplier<CachedReader>> coordCols_;

    /**
     * Constructor.  Note all the mask and coord specs are assumed to
     * refer to the same table.
     *
     * @param  nrow   number of rows available from readers
     * @param  maskCols  list of mask column reader suppliers
     * @param  coordCols  list of coordinate column reader suppliers
     */
    public TableCachedData( long nrow,
                            List<Supplier<CachedReader>> maskCols,
                            List<Supplier<CachedReader>> coordCols ){
        nrow_ = nrow;
        maskCols_ = maskCols;
        coordCols_ = coordCols;
    }

    /**
     * Returns the row count for this object's table.
     *
     * @return   row count
     */
    public long getRowCount() {
        return nrow_;
    }

    /**
     * Returns a list of objects supplying mask data.
     *
     * @return  list of mask column reader suppliers
     */
    public List<Supplier<CachedReader>> getMaskColumns() {
        return maskCols_;
    }

    /**
     * Returns a list of objects supplying coordinate data.
     *
     * @return  list of coord column reader suppliers
     */
    public List<Supplier<CachedReader>> getCoordColumns() {
        return coordCols_;
    }

    /**
     * Populates and returns a TableCachedData instance by reading
     * from a given table.
     *
     * @param  table  input table
     * @param  maskSpecs  mask specifications relating to table
     * @param  coordSpecs  coord specifications relating to table
     * @param  colFact   creates storage
     * @return  populated TableCachedData
     */
    @Slow
    public static TableCachedData readData( StarTable table,
                                            MaskSpec[] maskSpecs,
                                            CoordSpec[] coordSpecs,
                                            CachedColumnFactory colFact )
            throws IOException, InterruptedException {
        int nm = maskSpecs.length;
        int nc = coordSpecs.length;
        final long leng = table.getRowCount();
        CachedColumn[] maskCols = new CachedColumn[ nm ];
        CachedColumn[] coordCols = new CachedColumn[ nc ];
        MaskSpec.Reader[] maskRdrs = new MaskSpec.Reader[ nm ];
        CoordSpec.Reader[] coordRdrs = new CoordSpec.Reader[ nc ];
        RowSequence rseq = table.getRowSequence();
        for ( int im = 0; im < nm; im++ ) {
            maskRdrs[ im ] = maskSpecs[ im ].flagReader( rseq );
            maskCols[ im ] =
                colFact.createColumn( StorageType.BOOLEAN, leng );
        }
        for ( int ic = 0; ic < nc; ic++ ) {
            coordRdrs[ ic ] = coordSpecs[ ic ].valueReader( rseq );
            coordCols[ ic ] =
                colFact.createColumn( coordSpecs[ ic ].getStorageType(), leng );
        }
        long nr = 0;
        try {
            for ( long irow = 0; rseq.next(); irow++ ) {
                if ( Thread.currentThread().isInterrupted() ) {
                    throw new InterruptedException();
                }
                for ( int im = 0; im < nm; im++ ) {
                    boolean include = maskRdrs[ im ].readFlag( irow );
                    maskCols[ im ].add( Boolean.valueOf( include ) );
                }
                for ( int ic = 0; ic < nc; ic++ ) {
                    Object value = coordRdrs[ ic ].readValue( irow );
                    coordCols[ ic ].add( value );
                }
                nr++;
            }
        }
        finally {
            rseq.close();
        }
        List<Supplier<CachedReader>> maskDatas = new ArrayList<>();
        List<Supplier<CachedReader>> coordDatas = new ArrayList<>();
        for ( int im = 0; im < nm; im++ ) {
            maskCols[ im ].endAdd();
            maskDatas.add( maskCols[ im ]::createReader );
        }
        for ( int ic = 0; ic < nc; ic++ ) {
            coordCols[ ic ].endAdd();
            coordDatas.add( coordCols[ ic ]::createReader );
        }
        return new TableCachedData( nr, maskDatas, coordDatas );
    }
}
