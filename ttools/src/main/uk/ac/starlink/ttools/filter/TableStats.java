package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.RowCollector;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.IntList;

/**
 * Aggregates column statistics for a table.
 * Factory methods are provided to accumulate statistics from table data.
 *
 * @author   Mark Taylor
 * @since    3 Dec 2020
 */
public class TableStats {

    /** Value by which Median Absolute Deviation is scaled to estimate SD. */
    public static final double MAD_SCALE = 1.4826;

    private final UnivariateStats[] colStats_;
    private long nrow_;

    /**
     * Constructor.
     *
     * @param colStats  array of per-column statistics accumulators
     */
    public TableStats( UnivariateStats[] colStats ) {
        colStats_ = colStats;
    }

    /**
     * Returns the array of per-column statistics accumulators.
     *
     * @return  column stats
     */
    public UnivariateStats[] getColumnStats() {
        return colStats_;
    }

    /**
     * Returns the number of rows for which statistics have been
     * accumulated.
     *
     * @return   row count
     */
    public long getRowCount() {
        return nrow_;
    }

    /**
     * Calculates statistics from a given table.
     *
     * @param  table  table supplying data
     * @param  runner   controls sequential/parallel processing
     * @param  qSupplier  supplies quantile accumulators if quantiles are
     *                    required, otherwise null
     * @param  doCard   whether to count distinct values
     */
    public static TableStats calculateStats( StarTable table, RowRunner runner,
                                             Supplier<Quantiler> qSupplier,
                                             boolean doCard )
            throws IOException {
        ColumnInfo[] colInfos = Tables.getColumnInfos( table );
        StatsCollector collector =
            new StatsCollector( colInfos, qSupplier, doCard );
        return runner.collect( collector, table );
    }

    /**
     * Calculates Median Absolute Deviations for each table column.
     *
     * @param  table  input table
     * @param  runner   controls sequential/parallel processing
     * @param  qSupplier  supplies quantile accumulators
     * @param  medians  per-column array of median values; 
     *                  MADs will be calculated only for elements with
     *                  non-null values
     * @return   per-column array of MADs
     */
    public static double[] calculateMads( StarTable table, RowRunner runner,
                                          Supplier<Quantiler> qSupplier,
                                          final double[] medians )
            throws IOException {
        final int ncol = medians.length;
        IntList madIcolList = new IntList();
        for ( int ic = 0; ic < ncol; ic++ ) {
            if ( ! Double.isNaN( medians[ ic ] ) ) {
                madIcolList.add( ic );
            }
        }
        final int[] madIcols = madIcolList.toIntArray();
        final int njc = madIcols.length;
        RowCollector<Quantiler[]> madCollector =
                new RowCollector<Quantiler[]>() {
            public Quantiler[] createAccumulator() {
                Quantiler[] quantilers = new Quantiler[ ncol ];
                for ( int jc = 0; jc < njc; jc++ ) {
                    int ic = madIcols[ jc ];
                    quantilers[ ic ] = qSupplier.get();
                }
                return quantilers;
            }
            public Quantiler[] combine( Quantiler[] qs1, Quantiler[] qs2 ) {
                for ( int jc = 0; jc < njc; jc++ ) {
                    int ic = madIcols[ jc ];
                    qs1[ ic ].addQuantiler( qs2[ ic ] );
                }
                return qs1;
            }
            public void accumulateRows( RowSplittable rseq, Quantiler[] qs )
                    throws IOException {
                while ( rseq.next() ) {
                    for ( int jc = 0; jc < njc; jc++ ) {
                        int ic = madIcols[ jc ];
                        Object value = rseq.getCell( ic );
                        if ( value instanceof Number ) {
                            double dval = ((Number) value).doubleValue();
                            if ( !Double.isNaN( dval ) ) {
                                double dm = Math.abs( dval - medians[ ic ] );
                                qs[ ic ].acceptDatum( dm );
                            }
                        }
                    }
                }
            }
        };
        Quantiler[] madQuantilers = runner.collect( madCollector, table );
        double[] mads = new double[ ncol ];
        for ( int ic = 0; ic < ncol; ic++ ) {
            Quantiler quantiler = madQuantilers[ ic ];
            if ( quantiler != null ) {
                quantiler.ready();
                mads[ ic ] = quantiler.getValueAtQuantile( 0.5 );
            }
            else {
                mads[ ic ] = Double.NaN;
            }
        }
        return mads;
    }

    /**
     * Collector implementation for table statistics.
     */
    private static class StatsCollector extends RowCollector<TableStats> {
        private final ColumnInfo[] colInfos_;
        private final Supplier<Quantiler> qSupplier_;
        private final boolean doCard_;
        private final int ncol_;

        /**
         * Constructor.
         *
         * @param   colInfos  column metadata
         * @param   qSupplier  quantiler supplier, or null for no quantiles
         * @param   doCard   whether to count distinct values
         */
        public StatsCollector( ColumnInfo[] colInfos,
                               Supplier<Quantiler> qSupplier,
                               boolean doCard ) {
            colInfos_ = colInfos;
            qSupplier_ = qSupplier;
            doCard_ = doCard;
            ncol_ = colInfos.length;
        }

        public TableStats createAccumulator() {
            UnivariateStats[] colStats = new UnivariateStats[ ncol_ ];
            for ( int icol = 0; icol < ncol_; icol++ ) {
                Class<?> clazz = colInfos_[ icol ].getContentClass();
                colStats[ icol ] =
                    UnivariateStats.createStats( clazz, qSupplier_, doCard_ );
            }
            return new TableStats( colStats );
        }

        public TableStats combine( TableStats ts1, TableStats ts2 ) {
            ts1.nrow_ += ts2.nrow_;
            for ( int icol = 0; icol < ncol_; icol++ ) {
                ts1.colStats_[ icol ].addStats( ts2.colStats_[ icol ] );
            }
            return ts1;
        }

        public void accumulateRows( RowSplittable rseq, TableStats tstats )
                throws IOException {
            UnivariateStats[] colStats = tstats.colStats_;
            LongSupplier rowIndex = rseq.rowIndex();
            long nr = 0;
            while ( rseq.next() ) {
                nr++;
                Object[] row = rseq.getRow();
                long irow = rowIndex == null ? -1 : rowIndex.getAsLong();
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    colStats[ icol ].acceptDatum( row[ icol ], irow );
                }
            }
            tstats.nrow_ += nr;
        }
    }
}
