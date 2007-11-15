package uk.ac.starlink.ttools.join;

import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for choosing table match mode.  Either BEST or ALL may be chosen.
 *
 * @author   Mark Taylor
 * @since    2 Nov 2007
 */
public class FindModeParameter extends ChoiceParameter {

    /** Value for best match only mode. */
    public static final String BEST = "best";

    /** Value for all metches mode. */
    public static final String ALL = "all";

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    public FindModeParameter( String name ) {
        super( name, new String[] { BEST, ALL, } );
        setDefault( BEST );
        setPrompt( "Type of match to perform" );
        setDescription( new String[] {
            "<p>Determines which matches are retained.", 
            "If <code>" + BEST + "</code> is selected,",
            "then only the best match",
            "between the two tables will be retained; in this case",
            "the data from a row of either input table will appear in",
            "at most one row of the output table.",
            "If <code>" + ALL + "</code> is selected, then all pairs of rows",
            "from the two input tables which match the input criteria",
            "will be represented in the output table.",
            "</p>",
        } );
    }

    /**
     * Returns the value of this parameter as a flag indicating whether it's
     * only the best matches or all that are required.
     *
     * @param  env  execution environment
     * @return   true for best matches only, false for all matches
     */
    public boolean bestOnlyValue( Environment env ) throws TaskException {
        String mode = stringValue( env );
        if ( BEST.equalsIgnoreCase( mode ) ) {
            return true;
        }
        else if ( ALL.equalsIgnoreCase( mode ) ) {
            return false;
        }
        else {
            throw new TaskException( "Unknown value \"" + mode + "\" of "
                                   + getName() + " (shouldn't happen)" );
        }
    }
}
