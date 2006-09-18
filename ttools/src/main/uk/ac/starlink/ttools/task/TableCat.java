package uk.ac.starlink.ttools.task;

/**
 * Concatenates multiple homogeneous tables top-to-bottom.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2006
 */
public class TableCat extends HomogeneousMapperTask {
    public TableCat() {
        super( new CatMapper(), "Concatenates multiple tables",
               new ChoiceMode(), true, true );
    }
}
