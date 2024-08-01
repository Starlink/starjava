package uk.ac.starlink.ttools.task;

import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.StringParameter;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter which can control how column names are renamed if they would
 * otherwise be duplicated in an output table.
 *
 * @author   Mark Taylor
 * @since    20 Nov 2007
 */
public class JoinFixActionParameter
             extends ChoiceParameter<JoinFixActionParameter.Fixer> {

    /**
     * Constructor.
     *
     * @param  name   parameter name
     */
    @SuppressWarnings("this-escape")
    public JoinFixActionParameter( String name ) {
        super( name, Fixer.class );
        setPrompt( "Whether and how to rename input columns" );
        String dflt;

        /* Option which does not alter column names. */
        addOption( new Fixer( "none", "columns are not renamed" ) {
            public JoinFixAction createAction( Environment env,
                                               StringParameter suffixParam )
                    throws TaskException {
                return JoinFixAction.NO_ACTION;
            }
        } );

        /* Option which appends a suffix to duplicated column names. */
        addOption( new Fixer( dflt = "dups",
                              "columns which would otherwise have duplicate "
                            + "names in the output will be renamed "
                            + "to indicate which table they came from" ) {
            public JoinFixAction createAction( Environment env,
                                               StringParameter suffixParam )
                    throws TaskException {
                String suffix = suffixParam.stringValue( env );
                return suffix == null || suffix.trim().length() == 0
                     ? JoinFixAction.NO_ACTION
                     : JoinFixAction.makeRenameDuplicatesAction( suffix );
            }
        } );

        /* Option which appends a suffix to all column names. */
        addOption( new Fixer( "all",
                              "all columns will be renamed to indicate "
                            + "which table they came from" ) {
            public JoinFixAction createAction( Environment env,
                                               StringParameter suffixParam )
                    throws TaskException {
                String suffix = suffixParam.stringValue( env );
                return suffix == null || suffix.trim().length() == 0
                     ? JoinFixAction.NO_ACTION
                     : JoinFixAction.makeRenameAllAction( suffix );
            }
        } );

        setStringDefault( dflt );
        StringBuffer dbuf = new StringBuffer()
            .append( "<p>Determines how input columns are renamed before\n" )
            .append( "use in the output table.  The choices are:\n" )
            .append( "<ul>\n" );
        Object[] options = getOptions();
        for ( int i = 0; i < options.length; i++ ) {
            Fixer opt = (Fixer) options[ i ];
            dbuf.append( "<li>" )
                .append( "<code>" )
                .append( opt.toString() )
                .append( "</code>" )
                .append( ": " )
                .append( opt.getDescription() )
                .append( "</li>" )
                .append( "\n" );
        }
        dbuf.append( "</ul>\n" )
            .append( "If columns are renamed, the new ones are determined\n" )
            .append( "by <code>" )
            .append( createSuffixParameter( "*" ).getName() )
            .append( "</code> " )
            .append( "parameters.\n" )
            .append( "</p>" );
        setDescription( dbuf.toString() );
    }

    /**
     * Returns the action specified by this parameter for a given label.
     *
     * @param   env  execution environment
     * @param   suffixParam  parameter supplying suffix value
     *          (got from a <code>createSuffixParameter</code> method)
     */
    public JoinFixAction getJoinFixAction( Environment env,
                                           StringParameter suffixParam )
            throws TaskException {
        Fixer fixer = objectValue( env );
        return fixer.createAction( env, suffixParam );
    }

    /**
     * Returns an array of fix actions, one for each of a list of numbered
     * input tables.  This is a convenience method that relies on conventional
     * naming of suffix parameters.
     *
     * @param  env  execution environment
     * @param  nin  size of fix action array 
     * @return  <code>nin</code>-element array of fix actions ("1", "2", ...)
     */
    public JoinFixAction[] getJoinFixActions( Environment env, int nin )
            throws TaskException {
        JoinFixAction[] fixActs = new JoinFixAction[ nin ];
        for ( int i = 0; i < nin; i++ ) {
            StringParameter suffixParam =
                createSuffixParameter( Integer.toString( i + 1 ) );
            fixActs[ i ] = getJoinFixAction( env, suffixParam );
        }
        return fixActs;
    }

    /**
     * Returns a new parameter, identified by a numeric-like label, 
     * by which per-table suffix strings can be specified.
     *
     * @param  numLabel  table identifier such as "1"
     */
    public StringParameter createSuffixParameter( String numLabel ) {
        return createSuffixParameter( "suffix" + numLabel, "table " + numLabel,
                                      "_" + numLabel );
    }

    /**
     * Returns a new parameter by which per-table suffix strings can be
     * specified.
     *
     * @param  name  parameter name
     * @param  descrip   phrase identifying table that this parameter controls
     *                   (such as "table 3")
     * @param  dflt  default value
     */
    public StringParameter createSuffixParameter( String name, String descrip,
                                                  String dflt ) {
        StringParameter param = new StringParameter( name );
        param.setStringDefault( dflt );
        param.setNullPermitted( true );
        param.setUsage( "<label>" );
        param.setPrompt( "Deduplicating suffix for columns in " + descrip );
        param.setDescription( new String[] {
            "<p>If the <code>" + getName() + "</code> parameter",
            "is set so that input columns are renamed for insertion into",
            "the output table, this parameter determines how the",
            "renaming is done.",
            "It gives a suffix which is appended to all renamed columns",
            "from " + descrip + ".",
            "</p>",
        } );
        return param;
    }

    /**
     * Helper class which defines how suffixes are turned into JoinFixActions.
     */
    public abstract class Fixer {

        private final String name_;
        private final String description_;

        /**
         * Constructor.
         *
         * @param  name  object name, for presentation to the user as a 
         *               parameter value
         * @param  description short description of the function
         */
        public Fixer( String name, String description ) {
            name_ = name;
            description_ = description;
        }

        /**
         * Returns a JoinFixAction corresponding to a given label.
         *
         * @param  env  execution environment
         * @param   suffixParam  parameter supplying suffix value
         *          (got from a <code>createSuffixParameter</code> method)
         */
        public abstract JoinFixAction
                createAction( Environment env, StringParameter suffixParam )
                throws TaskException;

        /**
         * Returns a short description of the function of this object.
         *
         * @return  description
         */
        public String getDescription() {
            return description_;
        }

        /**
         * Returns object name.
         */
        public String toString() {
            return name_;
        }
    }
}
