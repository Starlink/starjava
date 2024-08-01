package uk.ac.starlink.ttools.join;

import java.util.Arrays;
import uk.ac.starlink.table.join.PairMode;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;

/**
 * Parameter for choosing table pair match mode.
 *
 * @author   Mark Taylor
 * @since    2 Nov 2007
 */
public class FindModeParameter extends ChoiceParameter<PairMode> {

    /**
     * Constructor.
     *
     * @param  name  parameter name
     */
    @SuppressWarnings("this-escape")
    public FindModeParameter( String name ) {
        super( name, PairMode.values() );
        PairMode[] modes =
            Arrays.asList( getOptions() ).toArray( new PairMode[ 0 ] );
        setDefaultOption( PairMode.BEST );
        setPrompt( "Which pair matches to include" );
        StringBuilder optBuf = new StringBuilder();
        for ( int im = 0; im < modes.length; im++ ) {
            optBuf.append( "<li>" )
                  .append( "<code>" )
                  .append( stringifyOption( modes[ im ] ) )
                  .append( "</code>: " )
                  .append( modes[ im ].getSummary() )
                  .append( ".\n" )
                  .append( getModeDescription( modes[ im ] ) )
                  .append( "</li>" )
                  .append( '\n' );
        }
        String cBest =
            "<code>" + stringifyOption( PairMode.BEST ) + "</code>";
        String cBest1 =
            "<code>" + stringifyOption( PairMode.BEST1 ) + "</code>";
        String cBest2 =
            "<code>" + stringifyOption( PairMode.BEST2 ) + "</code>";
        String cAll =
            "<code>" + stringifyOption( PairMode.ALL ) + "</code>";
        setDescription( new String[] {
            "<p>Determines what happens when a row in one table",
            "can be matched by more than one row in the other table.",
            "The options are:",
            "<ul>",
            optBuf.toString(),
            "</ul>",
            "The differences between",
            cBest + ", " + cBest1 + " and " + cBest2 + " are a bit subtle.",
            "In cases where it's obvious which object in each table",
            "is the best match for which object in the other,",
            "choosing betwen these options will not affect the result.",
            "However, in crowded fields",
            "(where the distance between objects within one or both tables is",
            "typically similar to or smaller than the specified match radius)",
            "it will make a difference.",
            "In this case one of the asymmetric options",
            "(" + cBest1 + " or " + cBest2 + ")",
            "is usually more appropriate than " + cBest + ",",
            "but you'll have to think about which of them suits your",
            "requirements.",
            "The performance (time and memory usage) of the match",
            "may also differ between these options,",
            "especially if one table is much bigger than the other.",
            "</p>",
        } );
    }

    @Override
    public String stringifyOption( PairMode option ) {
        return String.valueOf( option ).toLowerCase();
    }

    /**
     * Returns additional description for each given pair matching mode.
     *
     * @param  mode  mode to describe
     * @return  XML-friendly description
     */
    public static String getModeDescription( PairMode mode ) {
        switch ( mode ) {
            case ALL:
                return "Every match between the two tables is included "
                     + "in the result.\n"
                     + "Rows from both of the input tables may appear "
                     + "multiple times in the result.";
            case BEST:
                return "The best pairs are selected in a way which treats the "
                     + "two tables symmetrically.\n"
                     + "Any input row which appears in one result pair is "
                     + "disqualified from appearing in any other result pair, "
                     + "so each row from both input tables will appear in "
                     + "at most one row in the result.";
            case BEST1:
                return "For each row in table 1, only the best match from "
                     + "table 2 will appear in the result.\n"
                     + "Each row from table 1 will appear a maximum of once "
                     + "in the result, but rows from table 2 may appear "
                     + "multiple times.";
            case BEST2:
                return "For each row in table 2, only the best match from "
                     + "table 1 will appear in the result.\n"
                     + "Each row from table 2 will appear a maximum of once "
                     + "in the result, but rows from table 1 may appear "
                     + "multiple times.";
            default:
                assert false;
                return "???";
        }
    }
}
