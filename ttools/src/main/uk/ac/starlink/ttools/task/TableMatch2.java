package uk.ac.starlink.ttools.task;

/**
 * Task implementation for pair matching task.
 *
 * @author   Mark Taylor
 * @since    2 Sep 2005
 */
public class TableMatch2 extends MapperTask {
    public TableMatch2() {
        super( new Match2Mapper(), false );
    }
}
