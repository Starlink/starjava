package uk.ac.starlink.ttools;

import java.beans.IntrospectionException;
import java.io.IOException;
import uk.ac.starlink.table.BeanStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.formats.TextTableWriter;

/**
 * Processing mode for calculating statistics on a table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    16 Mar 2005
 */
public class StatsMode extends ProcessingMode {

    private static final ValueInfo ROWCOUNT_INFO = 
        new DefaultValueInfo( "Total Rows", Long.class );

    public String getName() {
        return "stats";
    }

    public void process( StarTable table ) throws IOException {

        /* Create a table which contains all the statistics, and
         * write it out in the usual way.  This isn't the only way
         * to do it, but since we've got lots of powerful tools lying
         * around for manipulation and output of tables, it turns out
         * to be convenient to use them rather than to write a 
         * statistics outputter from scratch. */
        new TextTableWriter().writeStarTable( makeStatsTable( table ),
                                              getOutputStream() );
    }

    /**
     * Creates a table which is composed of the column-wise statistics
     * of another table.
     *
     * @param   table  table whose stats are to be calculated
     * @return   table containing statistics of <tt>table</tt>
     */
    private static StarTable makeStatsTable( StarTable table )
            throws IOException {

        /* Get an array of objects each of which can accumulate the
         * statistics for one column of the base table. */
        int ncol = table.getColumnCount();
        ColStats[] stats = new ColStats[ ncol ];
        for ( int icol = 0; icol < ncol; icol++ ) {
            stats[ icol ] =
                ColStats.makeColStats( table.getColumnInfo( icol ) );
        }

        /* Accumulate the statistics. */
        RowSequence rseq = table.getRowSequence();
        long nrow = 0;
        try {
            while ( rseq.next() ) {
                nrow++;
                Object[] row = rseq.getRow();
                for ( int icol = 0; icol < ncol; icol++ ) {
                    stats[ icol ].acceptDatum( row[ icol ] );
                }
            }
        }
        finally {
            rseq.close();
        }

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
        ((BeanStarTable) statsTable).setData( stats );

        /* Unfortunately, a BeanTable returns its columns in an unhelpful
         * order (alphabetical by property/column name), so we reorder
         * the columns here. */
        String[] columns = new String[] {
            "column",
            "mean",
            "stdDev",
            "min",
            "max",
            "good",
        };
        statsTable = KeepColumnFilter.keepColumnTable( statsTable, columns );
        statsTable.setParameter( new DescribedValue( ROWCOUNT_INFO,
                                                     new Long( nrow ) ) );

        /* Return the table which contains the statistics. */
        return statsTable;
    }

}
