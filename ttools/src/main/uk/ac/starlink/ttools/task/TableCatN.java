package uk.ac.starlink.ttools.task;

/**
 * Concatenates N tables top to bottom.
 *
 * @author   Mark Taylor
 * @since    5 Oct 2006
 */
public class TableCatN extends MapperTask {
    public TableCatN() {
        super( "Concatenates multiple tables", new ChoiceMode(), true,
               new CatMapper( false ), new VariableTablesInput( true ) );
    }
}
