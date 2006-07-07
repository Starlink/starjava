package uk.ac.starlink.ttools.task;

/**
 * Interface which defines additional information available from a Parameter.
 * The intention is that Parameter implementations implement this
 * interface if they have more to say about usage than the one-line
 * text returned by their 
 * {@link uk.ac.starlink.task.Parameter#getUsage} method.
 *
 * @author   Mark Taylor
 * @since    15 Aug 2005
 */
public interface ExtraParameter {

    /**
     * Returns an extended usage message.  This should not repeat the
     * content of the normal usage message.  It should be preformatted,
     * that is it should contain newlines to keep the line length down
     * to less than 80 characters.
     *
     * @param   env  execution envrionment
     * @return  extended usage message
     */
    public String getExtraUsage( TableEnvironment env );
}
