// Copyright (C) 2002 Central Laboratory of the Research Councils

package uk.ac.starlink.soap;

import org.apache.axis.configuration.FileProvider;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.ConfigurationException;
import org.apache.axis.deployment.wsdd.WSDDDeployment;
import org.apache.axis.AxisEngine;
import org.apache.axis.Handler;
import org.apache.axis.handlers.soap.SOAPService;
import org.apache.axis.encoding.TypeMappingRegistry;

import javax.xml.namespace.QName;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Iterator;


/**
 * A simple ConfigurationProvider that uses the Admin class to read
 * XML files for clients and servers, but does write them, so is
 * suitable for embedded applications.
 * <p>
 * See org.apache.axis.configuration.FileProvider for documentation.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see org.apache.axis.configuration.FileProvider
 * @since 24-MAY-2002
 *
 */
public class AppFileProvider extends FileProvider
{
    public AppFileProvider( String filename )
    {
        super( filename );
    }

    public AppFileProvider( String basepath, String filename )
        throws ConfigurationException
    {
        super( basepath, filename );
    }

    public AppFileProvider( InputStream is )
    {
        super( is );
    }

    /**
     * Save the engine configuration. Do nothing for this class.
     */
    public void writeEngineConfig( AxisEngine engine )
        throws ConfigurationException 
    {
        // Do nothing.
    }
}
