package uk.ac.starlink.ttools.task;

/**
 * Concatenates multiple homogeneous tables top-to-bottom.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2006
 */
public class TableCat extends MapperTask {
    public TableCat() {
        super( "Concatenates multiple similar tables", new ChoiceMode(), true,
               new CatMapper( true ), new HomogeneousTablesInput( true ) );
    }
}
