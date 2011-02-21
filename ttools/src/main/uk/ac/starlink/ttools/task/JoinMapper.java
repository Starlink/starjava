package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.logging.Logger;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.TaskException;

/**
 * TableMapper which joins tables side-by-side.  Rows are not rearranged.
 *
 * @author   Mark Taylor
 * @since    28 Nov 2006
 */
public class JoinMapper implements TableMapper {

    private final JoinFixActionParameter fixcolParam_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     */
    public JoinMapper() {
        fixcolParam_ = new JoinFixActionParameter( "fixcols" );
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            fixcolParam_,
            fixcolParam_.createSuffixParameter( VariableTablesInput
                                               .NUM_SUFFIX ),
        };
    }

    public TableMapping createMapping( Environment env, int nin )
            throws TaskException {
        return new JoinMapping( fixcolParam_.getJoinFixActions( env, nin ) );
    }

    /**
     * Mapping which joins tables side by side.
     */
    private static class JoinMapping implements TableMapping {

        private final JoinFixAction[] fixActs_;

        /**
         * Constructor.
         *
         * @param  fixActs  determines how columns will be renamed
         */
        JoinMapping( JoinFixAction[] fixActs ) {
            fixActs_ = fixActs;
        }

        public StarTable mapTables( InputTableSpec[] inSpecs )
                throws IOException, TaskException {
            int nin = inSpecs.length;
            StarTable[] inTables = new StarTable[ nin ];
            long nrow = -1L;
            boolean mismatch = false;
            for ( int i = 0; i < nin; i++ ) {
                inTables[ i ] = inSpecs[ i ].getWrappedTable();
                long nr = inTables[ i ].getRowCount();
                if ( nr >= 0 ) {
                    if ( nrow < 0 ) {
                        nrow = nr;
                    }
                    if ( nr != nrow ) {
                        mismatch = true;
                    }
                }
            }
            if ( mismatch ) {
                logger_.warning( "Tables do not all have the same length; "
                               + "will truncate to shortest" );
            }
            return new JoinStarTable( inTables, fixActs_ );
        }
    }
}
