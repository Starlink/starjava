package uk.ac.starlink.vo;

/**
 * Describes the information about a UWS Job which can be retrieved
 * from a UWS server by retrieving the job's &lt;uws:job&gt; element.
 *
 * @author   Mark Taylor
 * @since    4 May 2011
 * @see  <a href="http://www.ivoa.net/Documents/UWS/index.html">UWS 1.0</a>
 */
public interface UwsJobInfo {

    /**
     * Returns this job's ID, unique for this UWS service.
     *
     * @return  job ID
     */
    String getJobId();

    /**
     * Returns this job's run ID, often left blank.
     *
     * @return  run ID
     */
    String getRunId();

    /**
     * Returns this job's owner ID, blank unless authentication is in use.
     *
     * @return  owner authorization information
     */
    String getOwnerId();

    /**
     * Returns this job's current phase.
     *
     * @return  phase
     * @see  UwsStage
     */
    String getPhase();

    /**
     * Returns this job's quoted completion time, if any.
     *
     * @return  quote as an ISO-8601 time
     */
    String getQuote();

    /**
     * Returns this job's start time, if it's started.
     *
     * @return  start time in ISO-8601 format
     */
    String getStartTime();

    /**
     * Returns this job's end time, if it's ended.
     *
     * @return  end time in ISO-8601 format
     */
    String getEndTime();

    /**
     * Returns the maximum wall-clock time that this job is permitted to
     * run for, in seconds.  Zero indicates unlimited.
     *
     * @return  max duration in seconds
     */
    String getExecutionDuration();

    /**
     * Returns this job's destruction time.
     *
     * @return  time after which the job will be removed in ISO-8601 format
     */
    String getDestruction();

    /**
     * Returns the list of parameter objects associated with this job.
     *
     * @return  parameter list
     */
    Parameter[] getParameters();

    /**
     * Returns the list of result objects associted with this job.
     *
     * @return   result list
     */
    Result[] getResults();

    /**
     * Returns any error information associated with this job.
     *
     * @return   error object
     */
    Error getError();

    /**
     * Represents a parameter associated with a UWS job.
     */
    public interface Parameter {

        /**
         * Returns this parameter's ID (name).
         *
         * @return  name
         */
        String getId();

        /**
         * Returns this parameter's value.
         *
         * @return  value
         */
        String getValue();

        /**
         * Indicates whether this parameter's value is a URL or not.
         *
         * @return   true for direct content, false for a URL
         */
        boolean isByReference();

        /**
         * Indicates whether this parameter represents posted data.
         *
         * @return   true iff posted
         */
        boolean isPost();
    }

    /**
     * Represents a result associated with a UWS job.
     */
    public interface Result {

        /**
         * Returns this result's ID (name).
         *
         * @return  name
         */
        String getId();

        /**
         * Returns this result's value.
         *
         * @return  value
         */
        String getValue();

        /**
         * Returns the optional xlink:href attribute for this result.
         *
         * @return  link reference
         */
        String getHref();

        /**
         * Returns the optional xlink:type attribute for this result.
         *
         * @return  link type
         */
        String getType();
    }

    /**
     * Represents error information associated with a UWS job.
     */
    public interface Error {

        /**
         * Indicates whether this error is fatal.
         *
         * @return  true iff fatal
         */
        boolean isFatal();

        /**
         * Indicates whether a more detailed error message is available.
         * If so it can be found at the jobid/error endpoint.
         *
         * @return  true  iff detail is available
         */
        boolean hasDetail();

        /**
         * Returns the error message.
         *
         * @return  message
         */
        String getMessage();
    }
}
