// Copyright (C) 2002 Central Laboratory of the Research Councils

package uk.ac.starlink.soap;

import org.apache.axis.EngineConfigurationFactory;
import org.apache.axis.EngineConfiguration;

/**
 * This is a implementation of EngineConfigurationFactory for embedded
 * applications. It does not write out any configuration files or log
 * anything. Overrides default factory using the
 * "axis.EngineConfigFactory" property.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since 24-MAY-2002
 */
public class AppEngineConfigurationFactory
    implements EngineConfigurationFactory
{
    private String clientConfigFile = "client-config.wsdd";
    private String serverConfigFile = "server-config.wsdd";

    /**
     * Constructor.
     */
    public AppEngineConfigurationFactory()
    {
        // Nothing to do. Use the default files.
        String clientProp = System.getProperty( "axis.ClientConfigFile" );
        if ( clientProp != null ) {
            clientConfigFile = clientProp;
        }

        String serverProp = System.getProperty( "axis.ServerConfigFile" );
        if ( serverProp != null ) {
            serverConfigFile = serverProp;
        }
    }

     /**
     * Get a default client engine configuration. Returns a Provider
     * that doesn't save to disk.
     *
     * @return a client EngineConfiguration
     */
    public EngineConfiguration getClientEngineConfig()
    {
        return new AppFileProvider( clientConfigFile );
    }

    /**
     * Get a default server engine configuration.
     *
     * @return a server EngineConfiguration
     */
    public EngineConfiguration getServerEngineConfig()
    {
        return new AppFileProvider( serverConfigFile );
    }

    public static EngineConfigurationFactory newFactory( Object param )
    {
        return null;
    }
}
