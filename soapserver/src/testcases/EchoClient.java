// Copyright (C) 2002 Central Laboratory of the Research Councils

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.apache.axis.utils.Options;

import javax.xml.rpc.ParameterMode;

/**
 * Test client for builtin SOAP services. Sends a String to the
 * EchoServer "echo" method and outputs the response.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @since <today>
 */
public class EchoClient
{
    public static void main( String [] args ) 
        throws Exception 
    {
        if ( args == null || args.length != 1 ) {
            System.err.println("Usage: EchoClient message");
            return;
        }
        
        Service service = new Service();
        Call call = (Call) service.createCall();

        call.setTargetEndpointAddress(new java.net.URL(EchoServer.ENDPOINT));
        call.setOperationName( "echo" );
        call.addParameter( "message", XMLType.XSD_STRING, ParameterMode.IN );
        call.setReturnType( XMLType.XSD_STRING );

        System.out.println( "I say: " + args[0] );
        String ret = (String) call.invoke( args );
        System.out.println( ret );
   }
}
