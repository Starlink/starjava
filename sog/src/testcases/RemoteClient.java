// Copyright (C) 2002 Central Laboratory of the Research Councils

package uk.ac.starlink.sog;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.apache.axis.utils.Options;
import org.apache.axis.AxisFault;

import javax.xml.rpc.ParameterMode;

import java.rmi.RemoteException;


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
    {
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            
            call.setTargetEndpointAddress( new java.net.URL( endpoint ) );
            call.setOperationName( "showImage" );
            call.addParameter( "fileOrURL", XMLType.XSD_STRING, ParameterMode.IN );
            call.setReturnType( XMLType.AXIS_VOID );
            
            System.out.println( "I say: " + args[0] );
            call.invoke( args );
        }
        catch (AxisFault f) {
            System.out.println( "AxisFault" );
            System.out.println( f );
        }
        catch (RemoteException e) {
            System.out.println( "RemoteException" );
            System.out.println( e.getMessage() );
        }
        catch (Exception e) {
            System.out.println( "GeneralException" );
            System.out.println( e );
        }
   }
}
