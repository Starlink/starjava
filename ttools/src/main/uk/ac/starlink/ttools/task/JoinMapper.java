package uk.ac.starlink.ttools.task;

import java.io.IOException;
import java.util.logging.Logger;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.JoinStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.ChoiceParameter;
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

    private final ChoiceParameter fixcolParam_;
    private final Parameter suffixParam_;

    private static final String FIX_NONE;
    private static final String FIX_DUPS;
    private static final String FIX_ALL;
    private static final String[] FIXES = new String[] {
        FIX_NONE = "none",
        FIX_DUPS = "dups",
        FIX_ALL = "all",
    };
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.task" );

    /**
     * Constructor.
     */
    public JoinMapper() {
        fixcolParam_ = new ChoiceParameter( "fixcols", FIXES );
        fixcolParam_.setDefault( FIX_DUPS );
        fixcolParam_.setPrompt( "Whether and how to rename input columns" );
        fixcolParam_.setDescription( new String[] {
            "<p>Determines how input columns are renamed before insertion",
            "into the output table.  The choices are:",
            "<ul>",
            "<li><code>" + FIX_NONE + "</code>:",
            "columns are not renamed</li>",
            "<li><code>" + FIX_DUPS + "</code>:",
            "columns which would otherwise have duplicate names in the output",
            "will be renamed to indicate which table they came from</li>",
            "<li><code>" + FIX_ALL + "</code>:",
            "all columns will be renamed to indicate which table they",
            "came from</li>",
            "</ul>",
            "</p>",
        } );

        suffixParam_ = createSuffixParameter( "N" );
    }

    public Parameter[] getParameters() {
        return new Parameter[] {
            fixcolParam_,
            suffixParam_,
        };
    }

    public TableMapping createMapping( Environment env, int nin )
            throws TaskException {

        /* Work out whether and how columns should be renamed for insertion
         * into the output table. */
        String fixcols = fixcolParam_.stringValue( env );
        JoinFixAction[] fixActs = new JoinFixAction[ nin ];
        if ( FIX_ALL.equals( fixcols ) ) {
            String[] suffixes = getColumnSuffixes( env, nin );
            for ( int i = 0; i < nin; i++ ) {
                fixActs[ i ] =
                    JoinFixAction.makeRenameAllAction( suffixes[ i ],
                                                       false, true );
            }
        }
        else if ( FIX_DUPS.equals( fixcols ) ) {
            String[] suffixes = getColumnSuffixes( env, nin );
            for ( int i = 0; i < nin; i++ ) {
                fixActs[ i ] =
                    JoinFixAction.makeRenameDuplicatesAction( suffixes[ i ],
                                                              false, true );
            }
        }
        else {
            assert FIX_NONE.equals( fixcols );
            for ( int i = 0; i < nin; i++ ) {
                fixActs[ i ] = JoinFixAction.NO_ACTION;
            }
        }

        /* Construct and return the mapping. */
        return new JoinMapping( fixActs );
    }

    /**
     * Returns an array of strings to be appended to columns of input tables,
     * if they are being renamed.
     *
     * @param  env  execution environment
     * @param  nin  number of input tables
     * @return <code>nin</code>-element array of strings, one to identify 
     *         each input table
     */
    private String[] getColumnSuffixes( Environment env, int nin )
            throws TaskException {
        String[] suffixes = new String[ nin ];
        for ( int i = 0; i < nin; i++ ) {
            String suff = createSuffixParameter( Integer.toString( i + 1 ) )
                         .stringValue( env );
            if ( suff == null ) {
                suff = "";
            }
            suffixes[ i ] = suff;
        }
        return suffixes;
    }

    /**
     * Returns a new parameter suitable for giving the deduplicating suffix
     * used for the input table with the given (symbolic or real) index.
     *
     * @param   index   index string identifying one of the input tables
     * @return   new suffix parameter
     */
    private Parameter createSuffixParameter( String index ) {
        Parameter param = new Parameter( "suffix" + index );
        param.setDefault( "_" + index );
        param.setNullPermitted( true );
        param.setPrompt( "Deduplicating suffix for columns in table " + index );
        param.setDescription( new String[] {
            "<p>If the <code>" + fixcolParam_.getName() + "</code> parameter",
            "is set so that input columns are renamed for insertion into",
            "the output table, this parameter determines how they are",
            "renamed.  It specifies a suffix which is appended to all renamed",
            "columns from table " + index + ".",
            "</p>",
        } );
        return param;
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
