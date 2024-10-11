package uk.ac.starlink.ttools.task;

import uk.ac.starlink.ttools.join.SkyMatch2Mapper;

/**
 * Task implementation for simplified sky matching task.
 *
 * @author    Mark Taylor
 * @since     2 Nov 2007
 */
public class SkyMatch2 extends MapperTask {
    public SkyMatch2() {
        super( "Crossmatches 2 tables on sky position", new ChoiceMode(),
               false, new SkyMatch2Mapper(), new FixedTablesInput( 2, false ) );
    }
    @Override
    public SkyMatch2Mapper getMapper() {
        return (SkyMatch2Mapper) super.getMapper();
    }
}
