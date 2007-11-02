package uk.ac.starlink.ttools.task;

/**
 * Task implementation for simplified sky matching task.
 *
 * @author    Mark Taylor
 * @since     2 Nov 2007
 */
public class SkyMatch2 extends FixedMapperTask {
    public SkyMatch2() {
        super( "Crossmatches 2 tables on sky position", new ChoiceMode(),
               false, new SkyMatch2Mapper(), 2, false );
    }
}
