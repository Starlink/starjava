package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.Task;

/**
 * Task subinterface for tasks having an <em>Operational</em> purpose
 * as defined by the
 * <a href="https://www.ivoa.net/documents/Notes/softid/">SoftID IVOA Note</a>.
 *
 * @author   Mark Taylor
 * @since    5 Jan 2026
 */
public interface OperationalTask extends Task {

    /**
     * Returns an operational purpose string appropriate for this task.
     * This will usually be one of the values
     *  {@link  uk.ac.starlink.vo.UserAgentUtil#PURPOSE_TEST}
     * ({@value uk.ac.starlink.vo.UserAgentUtil#PURPOSE_TEST}) or
     *  {@link  uk.ac.starlink.vo.UserAgentUtil#PURPOSE_COPY}
     * ({@value uk.ac.starlink.vo.UserAgentUtil#PURPOSE_COPY}).
     * A null or empty return value is treated the same as not implementing
     * this interface.
     *
     * @return  operational purpose string, excluding "IVOA-"
     */
    String getOperationalPurpose();
}
