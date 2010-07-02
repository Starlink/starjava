package uk.ac.starlink.ttools.task;

/**
 * Joins N tables side-to-side.  Rows are not rearranged.
 *
 * @author   Mark Taylor
 * @since    28 Nov 2006
 */
public class TableJoinN extends MapperTask {
    public TableJoinN() {
        super( "Joins multiple tables side-to-side", new ChoiceMode(), true,
               new JoinMapper(), new VariableTablesInput( true ) );
    }
}
