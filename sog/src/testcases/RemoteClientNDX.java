// Copyright (C) 2002 Central Laboratory of the Research Councils

package uk.ac.starlink.sog;

import java.net.URL;
import java.util.List;

import javax.xml.rpc.ParameterMode;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;

import org.w3c.dom.Element;

import uk.ac.starlink.hdx.HdxContainer;
import uk.ac.starlink.hdx.HdxContainerFactory;
import uk.ac.starlink.hdx.Ndx;

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
        List ndxs = null;
        HdxContainerFactory hdxf = HdxContainerFactory.getInstance();
        HdxContainer hdx;
        try {
            URL url = new URL( new URL( "file:." ), args[0] );
            hdx = hdxf.readHdx( url );
            ndxs = hdx.getNdxList();
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if ( ndxs != null && ndxs.size() == 0 ) {
            throw new RuntimeException( "Document contains no NDXs" );
        }
        Ndx ndx = (Ndx) ndxs.get( 0 );
        
        //  Transmit it to SOG.
        transmitNDX( ndx, endpoint, "showNDX" );
    }

    public static void transmitNDX( Ndx ndx, String endpoint, String method ) 
    {
        //  Transform NDX to DOM (should check persistence?).
        Element ndxElement = ndx.toDOM();
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
