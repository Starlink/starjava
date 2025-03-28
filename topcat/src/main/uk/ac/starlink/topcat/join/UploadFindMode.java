package uk.ac.starlink.topcat.join;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StoragePolicy;
import uk.ac.starlink.table.TableSink;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.storage.MonitorStoragePolicy;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.Scheduler;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TopcatUtils;
import uk.ac.starlink.ttools.cone.ColumnPlan;
import uk.ac.starlink.ttools.cone.QuerySequenceFactory;
import uk.ac.starlink.ttools.cone.RowMapper;
import uk.ac.starlink.ttools.cone.ServiceFindMode;
import uk.ac.starlink.ttools.cone.BlockUploader;
import uk.ac.starlink.ttools.task.UserFindMode;
import uk.ac.starlink.util.Bi;

/**
 * Mode for upload crossmatches corresponding to the user options.
 * This is related to the ServiceFindMode, but not in a 1:1 fashion.
 *
 * @author   Mark Taylor
 * @since    6 Jun 2014
 */
public abstract class UploadFindMode {

    private final String name_;
    private final ServiceFindMode serviceMode_;
    private final UserFindMode userMode_;
    private final boolean oneToOne_;

    /** All matches. */
    public static final UploadFindMode ALL =
        new AddTableMode( "All", ServiceFindMode.ALL, UserFindMode.ALL, false );

    /** Best match only. */
    public static final UploadFindMode BEST =
        new AddTableMode( "Best", ServiceFindMode.BEST, UserFindMode.BEST,
                          false );

    /** Best match in local table for each remote row. */
    public static final UploadFindMode BEST_REMOTE =
        new AddTableMode( "Best Remote", ServiceFindMode.BEST_REMOTE,
                          UserFindMode.BEST_REMOTE, false );

    /** One output row per local table row, best match or blank. */
    public static final UploadFindMode EACH =
        new AddTableMode( "Each", ServiceFindMode.BEST, UserFindMode.EACH,
                          true );

    /** Just adds a match subset to the table. */
    public static final UploadFindMode ADD_SUBSET =
        new AddSubsetMode( "Add Subset" );

    /** Useful instances of this class. */
    private static final UploadFindMode[] INSTANCES = {
        BEST, ALL, EACH, BEST_REMOTE, ADD_SUBSET,
    };

    /**
     * Constructor.
     *
     * @param  name  mode name
     * @param  serviceMode   ServiceFindMode instance underlying this fucntion
     * @param  userMode   UserFindMode instance matching this function
     * @param  oneToOne   true iff output rows match 1:1 with input rows
     */
    private UploadFindMode( String name, ServiceFindMode serviceMode,
                            UserFindMode userMode, boolean oneToOne ) {
        name_ = name;
        serviceMode_ = serviceMode;
        userMode_ = userMode;
        oneToOne_ = oneToOne;
        if ( oneToOne && ! serviceMode.supportsOneToOne() ) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Returns the service mode associated with this user mode.
     *
     * @return   service mode
     */
    public ServiceFindMode getServiceMode() {
        return serviceMode_;
    }

    /**
     * Returns the stilts mode corresponding to this mode.
     *
     * @return  user mode
     */
    public UserFindMode getUserMode() {
        return userMode_;
    }

    /**
     * Indicates whether this mode describes a match for which the count
     * and sequence of the output table rows are in one to one correspondence
     * with the input table rows.
     *
     * @return  true iff output rows match 1:1 with input rows
     */
    public boolean isOneToOne() {
        return oneToOne_;
    }

    /**
     * Performs an upload match and consumes the result in some appropriate
     * way.
     *
     * @param  blocker  block uploader
     * @param  inTable  input table, correspoinding to <code>qsFact</code>
     * @param  qsFact   sequence of positional query specifications,
     *                  with a row sequence corresponding to that of
     *                  <code>inTable</code>
     * @param  storage  storage policy for storing result table
     * @param  scheduler   object for conditionally scheduling operations
     *                     on the EDT
     * @param  tcModel   topcat model from which the input data comes
     * @param  rowMap    maps tcModel row indices to view indices
     */
    public abstract void runMatch( BlockUploader blocker, StarTable inTable,
                                   QuerySequenceFactory qsFact,
                                   StoragePolicy storage, Scheduler scheduler,
                                   TopcatModel tcModel, int[] rowMap );

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns an array of useful instances of this class.
     *
     * @return  instances
     */
    public static UploadFindMode[] getInstances() {
        return INSTANCES.clone();
    }

    /**
     * UploadFindMode subclass that loads the output table into the
     * topcat application as a new table.
     */
    private static class AddTableMode extends UploadFindMode {

        /**
         * Constructor.
         *
         * @param  name  mode name
         * @param  serviceMode   service upload mode
         * @param  userMode   UserFindMode instance matching this function
         * @param  oneToOne   true iff output rows match 1:1 with input rows
         */
        AddTableMode( String name, ServiceFindMode serviceMode,
                      UserFindMode userMode, boolean oneToOne ) {
            super( name, serviceMode, userMode, oneToOne );
        }

        public void runMatch( BlockUploader blocker, StarTable inTable,
                              QuerySequenceFactory qsFact,
                              StoragePolicy storage, Scheduler scheduler,
                              TopcatModel tcModel, int[] rowMap ) {
            CountSink countSink = new CountSink();
            StoragePolicy countStorage =
                new MonitorStoragePolicy( storage, countSink );
            final Bi<StarTable,BlockUploader.BlockStats> result;
            try {
                result = blocker.runMatch( inTable, qsFact, countStorage );
            }
            catch ( Exception e ) {
                scheduler.scheduleError( "Upload Match Error", e );
                return;
            }
            catch ( OutOfMemoryError e ) {
                scheduler.scheduleMemoryError( e );
                return;
            }
            StarTable outTable = result.getItem1();
            BlockUploader.BlockStats stats = result.getItem2();
            final int nBlock = stats.getBlockCount();
            final int nTrunc = stats.getTruncatedBlockCount();
            final long nMatch = countSink.getRowCount();
            if ( nMatch == 0 ) {
                scheduler.scheduleMessage( "No rows matched",
                                           "Empty match",
                                           JOptionPane.ERROR_MESSAGE );
            }
            else {
                final ControlWindow controlWin = ControlWindow.getInstance();
                final JComponent parent = scheduler.getParent();
                scheduler.schedule( new Runnable() {
                    public void run() {
                        TopcatModel outTcModel =
                            controlWin.addTable( outTable, outTable.getName(),
                                                 true );
                        StringBuffer sbuf = new StringBuffer()
                            .append( "New table created by upload crossmatch" )
                            .append( ": " )
                            .append( outTcModel )
                            .append( " (" );
                        if ( isOneToOne() ) {
                            sbuf.append( nMatch )
                                .append( " matches" );
                        }
                        else {
                            sbuf.append( outTable.getRowCount() )
                                .append( " rows" );
                        }
                        sbuf.append( ")" );
                        if ( nTrunc > 0 ) {
                            sbuf.append( "\n" )
                                .append( "WARNING: " )
                                .append( nTrunc )
                                .append( "/" )
                                .append( nBlock )
                                .append( " blocks were truncated by service" )
                                .append( " - consider reducing block size" );
                        }
                        String msg = sbuf.toString();
                        JOptionPane
                       .showMessageDialog( parent, msg, "Upload Match Success",
                                           JOptionPane.INFORMATION_MESSAGE );
                    } 
                } );
            }
        }
    }

    /**
     * UploadFindMode subclass that adds a subset for matched rows to the
     * topcat model.
     */
    private static class AddSubsetMode extends UploadFindMode {

        /**
         * Constructor.
         *
         * @param  name  mode name
         */
        AddSubsetMode( String name ) {
            super( name, ServiceFindMode.BEST_SCORE, UserFindMode.BEST, false );
        }

        public void runMatch( BlockUploader blocker, StarTable inTable,
                              QuerySequenceFactory qsFact,
                              StoragePolicy storage, Scheduler scheduler,
                              final TopcatModel tcModel, int[] rowMap ) {

            /* Prepare an input table containing just row indices. */
            long nRow = inTable.getRowCount();
            ColumnStarTable rowTable =
                ColumnStarTable.makeTableWithRows( nRow );
            ColumnData rowColumn =
                    new ColumnData( new DefaultValueInfo( "INDEX", Long.class,
                                                          null ) ) {
                public Object readValue( long irow ) {
                    return Long.valueOf( irow );
                }
            };
            rowTable.addColumn( rowColumn );

            /* Run a BEST match, counting the results. */
            CountSink countSink = new CountSink();
            StoragePolicy countStorage =
                new MonitorStoragePolicy( storage, countSink );
            final Bi<StarTable,BlockUploader.BlockStats> result;
            try {
                result = blocker.runMatch( rowTable, qsFact, countStorage );
            }
            catch ( Exception e ) {
                scheduler.scheduleError( "Upload Match Error", e );
                return;
            }
            catch ( OutOfMemoryError e ) {
                scheduler.scheduleMemoryError( e );
                return;
            }
            final StarTable outTable = result.getItem1();
            final BlockUploader.BlockStats stats = result.getItem2();
            final int nBlock = stats.getBlockCount();
            final int nTrunc = stats.getTruncatedBlockCount();
            final long nMatch = countSink.getRowCount();

            /* The result table has just two columns: input row index and
             * match score.  The match score must be non-null, since this
             * is a BEST match; ignore it and just set a flag true for each
             * row referenced in the result. */
            assert outTable.getColumnCount() == 2;
            assert outTable.getColumnInfo( 0 ).getContentClass() == Long.class;
            int icolIndex = 0;
            int icolScore = 1;
            int nrow =
                Tables.checkedLongToInt( tcModel.getDataModel().getRowCount() );
            final BitSet matchMask = new BitSet();
            RowSequence rseq = null;
            try {
                rseq = outTable.getRowSequence();
                while ( rseq.next() ) {
                    long irow = ((Long) rseq.getCell( icolIndex )).longValue();
                    assert ! Tables.isBlank( rseq.getCell( icolScore ) );
                    if ( irow < Integer.MAX_VALUE ) {
                        int jrow = rowMap == null ? (int) irow
                                                  : rowMap[ (int) irow ];
                        matchMask.set( jrow );
                    }
                }
            }
            catch ( IOException e ) {
                scheduler.scheduleError( "Result Read Error", e );
                return;
            }
            finally {
                if ( rseq != null ) {
                    try {
                        rseq.close();
                    }
                    catch ( IOException e ) {
                        // never mind
                    }
                }
            }

            /* Take the list of per-row flags and turn it into a subset
             * under user control. */
            final JComponent parent = scheduler.getParent();
            final String dfltName = "xmatch";
            final String title = "Upload Match Success";
            List<String> msgLines = new ArrayList<>();
            msgLines.add( "Upload crossmatch successful; matches found for "
                        + nMatch + "/" + nRow + " rows." );
            if ( nTrunc > 0 ) {
                msgLines.add( "WARNING: " + nTrunc + "/" + nBlock
                            + " blocks were truncated by the service"
                            + " - consider reducing block size" );
            }
            msgLines.add( " " );
            msgLines.add( "Define new subset for matched rows:" );
            scheduler.schedule( new Runnable() {
                public void run() {
                    TopcatUtils.addSubset( parent, tcModel, matchMask, dfltName,
                                           msgLines.toArray( new String[ 0 ] ),
                                           title );
                }
            } );
        }
    }

    /**
     * TableSink implementation that counts the number of rows written to it.
     */
    private static class CountSink implements TableSink {
        private volatile long count_;
        private volatile boolean ended_;

        public void acceptMetadata( StarTable meta ) {
            count_ = 0;
            ended_ = false;
        }

        public void acceptRow( Object[] row ) {
            count_++;
        }

        public void endRows() {
            ended_ = true;
        }

        /**
         * Returns the number of rows written to the completed table,
         * or -1 if not completed.
         *
         * @return   final row count or -1
         */
        public long getRowCount() {
            return ended_ ? count_ : -1;
        }
    }
}
