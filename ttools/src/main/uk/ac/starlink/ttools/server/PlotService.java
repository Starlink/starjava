package uk.ac.starlink.ttools.server;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Defines a behaviour offered to service an HTTP request relating to
 * a PlotSession.
 *
 * @author   Mark Taylor
 * @since    29 Sep 2020
 */
public interface PlotService {

    /**
     * Returns the name of the service.
     * This is both a mnemonic user-directed label,
     * and the path element that identifies an HTTP request as
     * referring to this service.
     *
     * @return  short service name
     */
    String getServiceName();

    /**
     * Indicates whether it makes sense to invoke this service for
     * creating a new session, or whether it has to refer to an existing
     * session.
     * 
     * @return  iff true, this service can initiate a new session
     */
    boolean canCreateSession();

    /**
     * Returns a user-directed XML description of the invocation syntax and
     * behaviour of this service.
     *
     * @return  P-level XML documentation
     */
    String getXmlDescription();

    /**
     * Responds to an HTTP request relating to a given plot session.
     *
     * @param  session  plot session
     * @param  request  HTTP request supplying action details
     * @param  response HTTP response to which output must be written
     */
    void sessionRespond( PlotSession<?,?> session,
                         HttpServletRequest request,
                         HttpServletResponse response )
            throws IOException;
}
