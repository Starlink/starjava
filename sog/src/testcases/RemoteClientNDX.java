// Copyright (C) 2002 Central Laboratory of the Research Councils

package uk.ac.starlink.sog;

import java.net.URL;
import java.util.List;

import javax.xml.rpc.ParameterMode;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;

import org.w3c.dom.Node;

////import uk.ac.starlink.hdx.HdxContainer;
////import uk.ac.starlink.hdx.HdxContainerFactory;
////import uk.ac.starlink.hdx.Ndx;
import javax.xml.transform.Source;
import uk.ac.starlink.ndx.Ndx;
import uk.ac.starlink.ndx.NdxIO;
import uk.ac.starlink.array.AccessMode;
import uk.ac.starlink.util.SourceReader;

/**
 * Test client for builtin SOAP services.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see Plot, PlotConfigurator
 * @since <today>
 */
public class RemoteClientNDX
{
    //  Define end point for SOG SOAP services.
    private static String endpoint = 
        "http://localhost:8082/services/SOGRemoteServices";
    
    public static void main( String [] args ) 
        throws Exception 
    {
        //  Get the NDX.
        NdxIO ndxIO = new NdxIO();
        Ndx ndx = null;
        try {
            URL url = new URL( new URL( "file:." ), args[0] );
            System.out.println( url );
            ndx = ndxIO.makeNdx( url, AccessMode.READ );
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if ( ndx == null ) {
            throw new RuntimeException( "Document contains no NDXs" );
        }

        //  Transmit it to SOG.
        transmitNDX( ndx, endpoint, "showNDX" );
    }

    public static void transmitNDX( Ndx ndx, String endpoint, String method ) 
    {
        //  Transform NDX to DOM (should check persistence?).
        Source ndxSource = ndx.toXML( null );
        Node ndxElement = null;
        try {
            ndxElement = new SourceReader().getDOM( ndxSource );
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Object[] elements = new Object[1];
        elements[0] = ndxElement;
        
        // Create a call to the application services.
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress( new java.net.URL( endpoint ) );
            
            // Configure call to appropriate method that receives a DOM Element.
            call.setOperationName( method );
            call.addParameter( "element", XMLType.SOAP_ELEMENT, ParameterMode.IN );
            call.setReturnType( XMLType.AXIS_VOID );
            
            // Send the NDX DOM Element.
            System.out.println( "Sending NDX" );
            call.invoke( elements );
        }
        catch (Exception e) {
            System.out.println( "Failed to transmit NDX" );
            e.printStackTrace();
        }
    }
}
