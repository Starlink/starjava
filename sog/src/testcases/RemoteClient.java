// Copyright (C) 2002 Central Laboratory of the Research Councils

package uk.ac.starlink.sog;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.apache.axis.utils.Options;

import javax.xml.rpc.ParameterMode;


/**
 * Test client for builtin SOAP services.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Plot, PlotConfigurator
 * @since <today>
 */
public class RemoteClient
{
    private static String endpoint = 
        "http://localhost:8082/services/SOGRemoteServices";
    
    public static void main( String [] args ) 
        throws Exception 
    {
        Service service = new Service();
        Call call = (Call) service.createCall();

        call.setTargetEndpointAddress( new java.net.URL( endpoint ) );
        call.setOperationName( "showImage" );
        call.addParameter( "fileOrURL", XMLType.XSD_STRING, ParameterMode.IN );
        call.setReturnType( XMLType.AXIS_VOID );

        System.out.println( "I say: " + args[0] );
        call.invoke( args );
   }
}
