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
    public FindModeParameter( String name ) {
        super( name, PairMode.values() );
        PairMode[] modes = (PairMode[]) Arrays.asList( getOptions() )
                                       .toArray( new PairMode[ 0 ] );
        setDefaultOption( PairMode.BEST );
        setPrompt( "Type of match to perform" );
        StringBuilder optBuf = new StringBuilder();
        for ( int im = 0; im < modes.length; im++ ) {
            optBuf.append( "<li>" )
                  .append( "<code>" )
                  .append( stringifyOption( modes[ im ] ) )
                  .append( "</code>: " )
                  .append( modes[ im ].getSummary() )
                  .append( ".\n" )
                  .append( modes[ im ].getExplanation() )
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
            "<p>Determines which matches appear in the result.", 
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
}
