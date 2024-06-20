package uk.ac.starlink.ttools.mode;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.PrintStream;
import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowCollector;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.RowSplittable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.formats.TextTableWriter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.filter.KeepColumnFilter;
import uk.ac.starlink.ttools.filter.StatsFilter;

/**
 * Processing mode for calculating statistics on a table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Mar 2005
 */
public class StatsMode implements ProcessingMode {

    private final boolean isParallel_;

    private static final ValueInfo ROWCOUNT_INFO = 
        new DefaultValueInfo( "Total Rows", Long.class );

    /**
     * Default constructor.
     */
    public StatsMode() {
        this( true );
    }

    /**
     * Constructs an instance with optional parallel processing.
     * Parallel execution can be much faster, but it depends on the
     * underlying data; if the multithreaded execution ends up
     * requesting simultaneous access to many different parts of
     * a mapped file, it can end up being much slower than
     * sequential execution.
     *
     * @param   isParallel  whether statistics calculations are
     *                      done using multiple threads
     */
    public StatsMode( boolean isParallel ) {
        isParallel_ = isParallel;
    }

    public Parameter<?>[] getAssociatedParameters() {
        return new Parameter<?>[ 0 ];
    }

    public String getDescription() {
        return DocUtils.join( new String[] {
            "<p>Calculates and displays univariate statistics for each",
            "of the numeric columns in the table.",
            "The following entries are shown for each column as appropriate:",
            "<ul>",
            "<li>mean</li>",
            "<li>population standard deviation</li>",
            "<li>minimum</li>",
            "<li>maximum</li>",
            "<li>number of non-null entries</li>",
            "</ul>",
            "</p>",
            "<p>See the " + DocUtils.filterRef( new StatsFilter() ) + " filter",
            "for more flexible statistical calculations.",
            "</p>",
        } );
    }

    public TableConsumer createConsumer( Environment env ) {
        final PrintStream out = env.getOutputStream();
        final RowRunner runner = isParallel_ ? RowRunner.DEFAULT
                                             : RowRunner.SEQUENTIAL;
        return new TableConsumer() {
            public void consume( StarTable table ) throws IOException {
                /* Create a table which contains all the statistics, and
                 * write it out in the usual way.  This isn't the only way
                 * to do it, but since we've got lots of powerful tools lying
                 * around for manipulation and output of tables, it turns out
                 * to be convenient to use them rather than to write a 
                 * statistics outputter from scratch. */
                new TextTableWriter()
                   .writeStarTable( makeStatsTable( table, runner ), out );
            }
        };
    }

    /**
     * Creates a table which is composed of the column-wise statistics
     * of another table.
     *
     * @param   table  table whose stats are to be calculated
     * @param   runner  handles row-based execution
     * @return   table containing statistics of <code>table</code>
     */
    private static StarTable makeStatsTable( StarTable table, RowRunner runner )
            throws IOException {

        /* Calculate the statistics. */
        StatsCollector collector =
            new StatsCollector( Tables.getColumnInfos( table ) );
        TableStats tstats = runner.collect( collector, table );
        long nrow = tstats.nrow_;

        /* Turn the array of ColStats objects into a StarTable. */
        StarTable statsTable;
        try {
            statsTable = new BeanStarTable( ColStats.class );
        }
        catch ( IntrospectionException e ) {
            throw (AssertionError) 
                  new AssertionError( "Introspection Error???" )
                 .initCause( e );
        }
        ((BeanStarTable) statsTable).setData( tstats.colStats_ );

        /* Unfortunately, a BeanTable returns its columns in an unhelpful
         * order (alphabetical by property/column name), so we reorder
         * the columns here. */
        String columns =
            "column " +
            "mean " +
            "stdDev " +
            "min " +
            "max " +
            "good " +
            "";
        statsTable = KeepColumnFilter.keepColumnTable( statsTable, columns );
        statsTable.setParameter( new DescribedValue( ROWCOUNT_INFO,
                                                     Long.valueOf( nrow ) ) );

        /* Return the table which contains the statistics. */
        return statsTable;
    }

    /**
     * Accumulator for table column statistics.
     */
    private static class TableStats {
        final int nc_;
        final ColStats[] colStats_;
        long nrow_;

        /**
         * Constructor.
         *
         * @param  infos   metadata object for each column
         *                 that will be accumulated
         */
        TableStats( ColumnInfo[] infos ) {
            nc_ = infos.length;
            colStats_ = new ColStats[ nc_ ];
            for ( int ic = 0; ic < nc_; ic++ ) {
                colStats_[ ic ] = ColStats.makeColStats( infos[ ic ] );
            }
        }

        /**
         * Accumulate the contents of a second stats object into this one.
         *
         * @param  other  other table stats
         */
        void addStats( TableStats other ) {
            this.nrow_ += other.nrow_;
            for ( int ic = 0; ic < nc_; ic++ ) {
                this.colStats_[ ic ].addStats( other.colStats_[ ic ] );
            }
        }
    }

    /**
     * Collector for accumulating column statitics.
     */
    private static class StatsCollector extends RowCollector<TableStats> {
        private final ColumnInfo[] infos_;
        private final int nc_;
     
        /**
         * Constructor.
         *
         * @param  infos   metadata object for each column
         *                 that will be accumulated
         */
        StatsCollector( ColumnInfo[] infos ) {
            infos_ = infos;
            nc_ = infos.length;
        }
        public TableStats createAccumulator() {
            return new TableStats( infos_ );
        }
        public TableStats combine( TableStats tstats1, TableStats tstats2 ) {
            tstats1.addStats( tstats2 );
            return tstats1;
        }
        public void accumulateRows( RowSplittable rseq, TableStats tstats )
                throws IOException {
            ColStats[] cstats = tstats.colStats_;
            while ( rseq.next() ) {
                Object[] row = rseq.getRow();
                for ( int ic = 0; ic < nc_; ic++ ) {
                    cstats[ ic ].acceptDatum( row[ ic ] );
                }
                tstats.nrow_++;
            }
        }
    }
}
