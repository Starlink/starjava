package uk.ac.starlink.ttools.join;

import uk.ac.starlink.table.join.JoinType;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;

/**
 * Parameter which can select a {@link uk.ac.starlink.table.join.JoinType}.
 *
 * @author   Mark Taylor
 * @since    8 Sep 2005
 */
public class JoinTypeParameter extends ChoiceParameter<JoinType> {

    @SuppressWarnings("this-escape")
    public JoinTypeParameter( String name ) {
        super( name, JoinType.getPairTypes() );

        StringBuffer items = new StringBuffer();
        JoinType[] joins = JoinType.getPairTypes();
        for ( int i = 0; i < joins.length; i++ ) {
            JoinType join = joins[ i ];
            items.append( "<li><code>" )
                 .append( join.getName() )
                 .append( "</code>: " )
                 .append( join.getDescription() )
                 .append( "</li>\n" );
        }
        setDescription( new String[] {
            "<p>Determines which rows are included in the output table.",
            "The matching algorithm determines which of the rows from",
            "the first table correspond to which rows from the second.",
            "This parameter determines what to do with that information.",
            "Perhaps the most obvious thing is to write out a table",
            "containing only rows which correspond to a row in both of",
            "the two input tables.  However, you may also want to see",
            "the unmatched rows from one or both input tables,",
            "or rows present in one table but unmatched in the other,",
            "or other possibilities.",
            "The options are:",
            "<ul>",
            items.toString(),
            "</ul>",
            "</p>",
        } );
        setPrompt( "Selection criteria for output rows" );
        setDefaultOption( JoinType._1AND2 );
    }

    /**
     * Returns the value of this parameter as a JoinType.
     *
     * @return  join type
     */
    public JoinType joinTypeValue( Environment env ) throws TaskException {
        return objectValue( env );
    }

    @Override
    public String stringifyOption( JoinType type ) {
        return type.getName();
    }
}
