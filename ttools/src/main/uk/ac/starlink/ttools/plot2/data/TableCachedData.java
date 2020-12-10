package uk.ac.starlink.ttools.plot2.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import uk.ac.starlink.table.RowCollector;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.RowSplittable;
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
     * from a given table in a sequential fashion.
     *
     * @param  table  input table
     * @param  maskSpecs  mask specifications relating to table
     * @param  coordSpecs  coord specifications relating to table
     * @param  colFact   creates storage
     * @return  populated TableCachedData
     */
    @Slow
    public static TableCachedData readDataSeq( StarTable table,
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

    /**
     * Populates and returns a TableCachedData instance by reading
     * from a given table in a parallel fashion.
     *
     * <p><strong>Use with care:</strong>  The ordering of the elements
     * in the result is not guaranteed to be the same as the input
     * iteration ordering.
     * Also, the returned object is less efficient for iteration than that
     * returned by the sequential implementation.
     *
     * @param  table  input table
     * @param  maskSpecs  mask specifications relating to table
     * @param  coordSpecs  coord specifications relating to table
     * @param  colFact   creates storage
     * @param   rowRunner row runner
     * @return  populated TableCachedData
     */
    @Slow
    public static TableCachedData readDataPar( StarTable table,
                                               final MaskSpec[] maskSpecs,
                                               final CoordSpec[] coordSpecs,
                                              final CachedColumnFactory colFact,
                                               RowRunner rowRunner )
            throws IOException {
        final int nm = maskSpecs.length;
        final int nc = coordSpecs.length;
        RowCollector<List<TableCachedData>> collector =
                new RowCollector<List<TableCachedData>>(){
            public List<TableCachedData> createAccumulator() {
                return new ArrayList<TableCachedData>();
            }
            public List<TableCachedData> combine( List<TableCachedData> tcds1,
                                                  List<TableCachedData> tcds2 ){
                tcds1.addAll( tcds2 );
                return tcds1;
            }
            public void accumulateRows( RowSplittable rseq,
                                        List<TableCachedData> tcds )
                    throws IOException {
                long leng = rseq.splittableSize();
                CachedColumn[] maskCols = new CachedColumn[ nm ];
                CachedColumn[] coordCols = new CachedColumn[ nc ];
                MaskSpec.Reader[] maskRdrs = new MaskSpec.Reader[ nm ];
                CoordSpec.Reader[] coordRdrs = new CoordSpec.Reader[ nc ];
                for ( int im = 0; im < nm; im++ ) {
                    maskRdrs[ im ] = maskSpecs[ im ].flagReader( rseq );
                    maskCols[ im ] =
                        colFact.createColumn( StorageType.BOOLEAN, leng );
                }
                for ( int ic = 0; ic < nc; ic++ ) {
                    coordRdrs[ ic ] = coordSpecs[ ic ].valueReader( rseq );
                    coordCols[ ic ] =
                        colFact.createColumn( coordSpecs[ ic ]
                                             .getStorageType(), leng );
                }
                long nr = 0;
                LongSupplier rowIndex = rseq.rowIndex();
                while ( rseq.next() ) {
                    long irow = rowIndex == null ? -1 : rowIndex.getAsLong();
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
                tcds.add( new TableCachedData( nr, maskDatas, coordDatas ) );
            }
        };
        List<TableCachedData> tcds = rowRunner.collect( collector, table );
        return tcds.size() > 1 ? toMulti( tcds ) : tcds.get( 0 );
    }

    /**
     * Aggregates a list of compatible TableCachedDatas into a single one.
     * Iterating over the output will be the same as iterating over
     * each of the input ones in turn.
     *
     * @param  tcds  input data storage
     * @return  aggregated data storage
     */
    private static TableCachedData toMulti( final List<TableCachedData> tcds ) {
        TableCachedData tcd0 = tcds.get( 0 );
        int nm = tcd0.getMaskColumns().size();
        int nc = tcd0.getCoordColumns().size();
        long[] nrows = new long[ tcds.size() ];
        int isub = 0;
        long nrow = 0;
        for ( TableCachedData tcd : tcds ) {
            long nr = tcd.getRowCount();
            nrows[ isub++ ] = nr;
            nrow += nr;
        }
        List<Supplier<CachedReader>> multiMaskCols = new ArrayList<>();
        List<Supplier<CachedReader>> multiCoordCols = new ArrayList<>();
        for ( int im = 0; im < nm; im++ ) {
            final int im0 = im;
            List<Supplier<CachedReader>> subCols =
                tcds.stream()
                    .map( tcd -> tcd.getMaskColumns().get( im0 ) )
                    .collect( Collectors.toList() );
            multiMaskCols.add( () -> new MultiCachedReader( subCols, nrows ) );
        }
        for ( int ic = 0; ic < nc; ic++ ) {
            final int ic0 = ic;
            List<Supplier<CachedReader>> subCols =
                tcds.stream()
                    .map( tcd -> tcd.getCoordColumns().get( ic0 ) )
                    .collect( Collectors.toList() );
            multiCoordCols.add( () -> new MultiCachedReader( subCols, nrows ) );
        }
        return new TableCachedData( nrow, multiMaskCols, multiCoordCols );
    }

    /**
     * Aggregates a list of compatible CachedReaders into a single one.
     * Iterating over the output will be the same as iterating over
     * each of the input ones in turn.
     */
    private static class MultiCachedReader implements CachedReader {
        final List<Supplier<CachedReader>> subCols_;
        final long[] nrows_;
        final CachedReader[] subRdrs_;
        long ixlo_;
        long ixhi_;
        long jx_;
        CachedReader rdr_;

        /**
         * Constructor.
         *
         * @param  subCols  array of input reader suppliers
         * @param  ncols   per-subCol array giving the number of elements
         *                 in each one
         */
        MultiCachedReader( List<Supplier<CachedReader>> subCols, long[] nrows ){
            subCols_ = subCols;
            nrows_ = nrows;
            subRdrs_ = new CachedReader[ nrows_.length ];
            ixlo_ = -1;
            ixhi_ = -1;
        }

        /**
         * Prepares for reading an element at a given index.
         * This method updates the <code>jx_</code> and
         * <code>rdr_</code> members.
         *
         * @param  ix  global index to which subsequent reads will apply
         */
        private void update( long ix ) {
            if ( ix >= ixlo_ && ix < ixhi_ ) {
                jx_ = ix - ixlo_;
            }
            else {
                long ixlo;
                long ixhi = 0;
                for ( int isub = 0; isub < nrows_.length; isub++ ) {
                    ixlo = ixhi;
                    ixhi += nrows_[ isub ];
                    if ( ix >= ixlo && ix < ixhi ) {
                        ixlo_ = ixlo;
                        ixhi_ = ixhi;
                        jx_ = ix - ixlo;
                        if ( subRdrs_[ isub ] == null ) {
                            subRdrs_[ isub ] = subCols_.get( isub ).get();
                        }
                        rdr_ = subRdrs_[ isub ];
                        return;
                    }
                }
                throw new IllegalArgumentException( "Index out of range: " + ix
                                                  + " > " + ixhi );
            }
        }
        public boolean getBooleanValue( long ix ) {
            update( ix );
            return rdr_.getBooleanValue( jx_ );
        }
        public int getIntValue( long ix ) {
            update( ix );
            return rdr_.getIntValue( jx_ );
        }
        public long getLongValue( long ix ) {
            update( ix );
            return rdr_.getLongValue( jx_ );
        }
        public double getDoubleValue( long ix ) {
            update( ix );
            return rdr_.getDoubleValue( jx_ );
        }
        public Object getObjectValue( long ix ) {
            update( ix );
            return rdr_.getObjectValue( jx_ );
        }
    }
}
