package uk.ac.starlink.ttools.task;

/**
 * Performs a cone search query for each row of an input table, 
 * and concatenates the result as one big output table.
 *
 * @author   Mark Taylor
 * @since    4 Jul 2006
 */
public class MultiCone extends MapperTask {
    public MultiCone() {
        super( new MultiConeMapper(), new ChoiceMode(), true, true );
    }
}
