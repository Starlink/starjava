package uk.ac.starlink.ttools.task;

/**
 * Performs a registry query.
 *
 * @author   Mark Taylor
 * @since    6 Jul 2006
 */
public class RegQuery extends MapperTask {
    public RegQuery() {
        super( new RegQueryMapper(), new ChoiceMode(), false, true );
    }
};
