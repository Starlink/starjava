package uk.ac.starlink.ttools.task;

import uk.ac.starlink.ttools.join.Match2Mapper;

/**
 * Task implementation for generic pair matching task.
 *
 * @author   Mark Taylor
 * @since    2 Sep 2005
 */
public class TableMatch2 extends MapperTask {
    public TableMatch2() {
        super( "Crossmatches 2 tables using flexible criteria",
               new ChoiceMode(), true, new Match2Mapper(),
               new FixedTablesInput( 2, true ) );
    }
    @Override
    public Match2Mapper getMapper() {
        return (Match2Mapper) super.getMapper();
    }
}
