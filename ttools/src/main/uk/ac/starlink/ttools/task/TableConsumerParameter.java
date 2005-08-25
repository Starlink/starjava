package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.TableConsumer;

/**
 * Defines an object (probably a parameter) which can return a 
 * TableConsumer.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public interface TableConsumerParameter {

    /**
     * Returns a TableConsumer which corresponds to the value of this
     * parameter.
     *
     * @param  env  execution environment
     */
    TableConsumer consumerValue( Environment env ) throws TaskException;

    /**
     * Sets the value of this parameter directly from a TableConsumer.
     *
     * @param  consumer  consumer
     */
    void setValueFromConsumer( TableConsumer consumer );
}
