package uk.ac.starlink.ttools.task;

/**
 * Concatenates two tables top to bottom.
 *
 * @author   Mark Taylor
 * @since    27 Sep 2005
 */
public class TableCat2 extends FixedMapperTask {
    public TableCat2() {
        super( "Concatenates 2 tables", new ChoiceMode(), true,
               new CatMapper(), 2, true );
    }
}
