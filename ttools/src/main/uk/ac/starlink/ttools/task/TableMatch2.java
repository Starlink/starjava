package uk.ac.starlink.ttools.task;

/**
 * Task implementation for pair matching task.
 *
 * @author   Mark Taylor
 * @since    2 Sep 2005
 */
public class TableMatch2 extends FixedMapperTask {
    public TableMatch2() {
        super( "Crossmatches 2 tables", new ChoiceMode(), true,
               new Match2Mapper(), 2, true );
    }
}
