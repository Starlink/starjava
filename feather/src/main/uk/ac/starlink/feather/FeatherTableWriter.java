package uk.ac.starlink.feather;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import uk.ac.bristol.star.feather.ColStat;
import uk.ac.bristol.star.feather.FeatherColumnWriter;
import uk.ac.bristol.star.feather.FeatherWriter;
import uk.ac.bristol.star.feather.FeatherType;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.StreamStarTableWriter;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.util.IntList;

/**
 * StarTableWriter implementation for writing to Feather format files.
 *
 * @author   Mark Taylor
 * @since    26 Feb 2020
 */
public class FeatherTableWriter extends StreamStarTableWriter {

    private final boolean isColumnOrder_;
    private final StoragePolicy storage_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.feather" );

    /**
     * Default constructor.
     */
    public FeatherTableWriter() {
        this( false, StoragePolicy.getDefaultPolicy() );
    }

    /**
     * Constructs a writer with custom configuration.
     * Output may be written either strictly streamed,
     * by acquiring column information as required,
     * or by scanning all the table rows first and caching bytes
     * in byte stores, then dumping them all to output at the end.
     * Differnt pros and cons; row-oriented is likely to be faster
     * (especially for non-column-oriented input table layout)
     * but requires substantial scratch storage.
     *
     * @param  isColumnOrder  true for column-oriented output,
     *                        false for row-oriented output
     * @param  storage   storage policy used if required
     *                   (row-oriented output only)
     */
    public FeatherTableWriter( boolean isColumnOrder, StoragePolicy storage ) {
        isColumnOrder_ = isColumnOrder;
        storage_ = storage;
    }

    public String getFormatName() {
        return "feather";
    }

    public String getMimeType() {
        return "application/octet-stream";
    }

    /**
     * Returns true for files with extension ".fea" or ".feather".
     */
    public boolean looksLikeFile( String loc ) {
        int idot = loc.lastIndexOf( '.' );
        String extension = idot >= 0 ? loc.substring( idot ) : "";
        return extension.equalsIgnoreCase( ".fea" )
            || extension.equalsIgnoreCase( ".feather" );
    }

    public void writeStarTable( StarTable table, OutputStream out )
            throws IOException {
        String description = table.getName();
        String tableMeta = null;

        /* Acquire StarColumnWriter objects for those columns
         * that can be output. */
        int ncol = table.getColumnCount();
        List<StarColumnWriter> cwList = new ArrayList<StarColumnWriter>();
        IntList icList = new IntList();
        for ( int ic = 0; ic < ncol; ic++ ) {
            StarColumnWriter writer =
                StarColumnWriters.createColumnWriter( table, ic );
            if ( writer != null ) {
                icList.add( ic );
                cwList.add( writer );
            }
            else {
                logger_.warning( "Can't encode column "
                               + table.getColumnInfo( ic ) + " to "
                               + getFormatName() + " format" );
            }
        }

        /* Turn them into an array of FeatherColumnWriters. */
        ItemAccumulator[] accumulators = null;
        final FeatherColumnWriter[] colWriters;
        try {
            if ( isColumnOrder_ ) {

                /* For column-oriented output, they can just write their data
                 * without further assistance. */
                colWriters = cwList.toArray( new FeatherColumnWriter[ 0 ] );
            }
            else {

                /* For row-oriented output, we have to scan through the rows
                 * first and accumulate the column data for each column,
                 * then construct a writer for each column using the
                 * accumulated data. */
                int[] ics = icList.toIntArray();
                int nic = ics.length;
                accumulators = new ItemAccumulator[ nic ];
                for ( int jc = 0; jc < nic; jc++ ) {
                    int ic = ics[ jc ];
                    accumulators[ jc ] =
                        cwList.get( jc ).createItemAccumulator( storage_ );
                }
                RowSequence rseq = table.getRowSequence();
                try {
                    while ( rseq.next() ) {
                        Object[] row = rseq.getRow();
                        for ( int jc = 0; jc < nic; jc++ ) {
                            int ic = ics[ jc ];
                            accumulators[ jc ].addItem( row[ ic ] );
                        }
                    }
                }
                finally {
                    rseq.close();
                }
                colWriters = new FeatherColumnWriter[ nic ];
                for ( int jc = 0; jc < nic; jc++ ) {
                    final FeatherColumnWriter cw = cwList.get( jc );
                    final ItemAccumulator acc = accumulators[ jc ];
                    colWriters[ jc ] = new FeatherColumnWriter() {
                        public FeatherType getFeatherType() {
                            return cw.getFeatherType();
                        }
                        public String getName() {
                            return cw.getName();
                        }
                        public String getUserMetadata() {
                            return cw.getUserMetadata();
                        }
                        public ColStat writeColumnBytes( OutputStream out )
                                throws IOException {
                            return acc.writeColumnBytes( out );
                        }
                    };
                }
            }

            /* Write the table based on the column writers. */
            new FeatherWriter( description, tableMeta, colWriters )
               .write( out );
        }
        finally {
            if ( accumulators != null ) {
                for ( ItemAccumulator acc : accumulators ) {
                    acc.close();
                }
            }
        }
    }
}
