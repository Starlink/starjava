// Copyright (C) 2002 Central Laboratory of the Research Councils

package uk.ac.starlink.soap;

import org.apache.axis.AxisEngine;
import org.apache.axis.AxisFault;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.server.DefaultAxisServerFactory;
import org.apache.axis.server.AxisServer;

import java.util.Map;

/**
 * Helper class for obtaining AxisServers. This implementation avoids
 * creating any external files. Using this you must avoid attachments
 * (which is what I presume go into "attachmentsdirservlet", may want
 * to go back to the default factory, if this choice proves
 * inappropriate).
 * <p>
 * This overiddes the default factory (that it extends) using the
 * "axis.ServerFactory" property.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Plot, PlotConfigurator
 * @since 24-MAY-2002
 */
public class AppAxisServerFactory extends DefaultAxisServerFactory
{
    /**
     * Get an AxisServer.  This factory looks for an "engineConfig" in the
     * environment Map, and if one is found, uses that.  Otherwise it
     * uses the default initialization.
     *
     */
    public AxisServer getServer( Map environment )
        throws AxisFault
    {
        EngineConfiguration config = null;
        try {
            config = (EngineConfiguration)
                environment.get( EngineConfiguration.PROPERTY_NAME );
        }
        catch ( ClassCastException e ) {
            // Just in case, fall through here.
        }

        AxisServer ret = null;
        if ( config == null ) {
            ret = new AxisServer();
        }
        else {
            ret = new AxisServer( config );
        }

        //  Attachments directory is unset (TODO: may not work, perhaps
        //  we should use the Jetty directory in /tmp?).
        //ret.setOption( AxisEngine.PROP_ATTACHMENT_DIR, null );
        return ret;
    }
}
