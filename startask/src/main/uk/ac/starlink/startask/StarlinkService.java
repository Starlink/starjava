package uk.ac.starlink.startask;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;

import javax.xml.rpc.ParameterMode;
import javax.xml.namespace.QName;
import java.io.*;
import java.util.*;
/** A class implementing SOAP communications for a client program running
 *  Starlink web services.
 */
public class StarlinkService {
   boolean DEBUG = false;
   String site = "rlspc14.bnsc.rl.ac.uk";
   int port = 8080;
   String application = null;
   String endpoint = null;
   Service service;
   Call call;

/** Set up a connection with a service on the default site and port.
 *  @param the name of the service
 */
   public StarlinkService ( String app ) throws Exception {
      application = app;
      newService();
   }
    
/** Set up a connection with a service at a given site and port.
 *  @param the required site
 *  @param the required port
 *  @param the name of the service
 */
   public StarlinkService ( String s, int p, String app ) throws Exception {
      site = s;
      port = p;
      application = app;
      if ( port == 0 ) port = 8080;
      newService();
   }

/** Set up a call to this service
 */
   private void newService ( ) throws Exception { 
      service = new Service();
      call    = (Call) service.createCall();
      endpoint =
         "http://" + site + ":" + port + "/axis/services/" + application;
      call.setTargetEndpointAddress( new java.net.URL(endpoint) );
      if ( DEBUG ) System.out.println( "\nConnecting to service: " + endpoint );       
   }

/** Run the given method on this service with the given parameters.
 *  The interpretation of the return array of Strings will depend on the task.
 *  For normal Starlink JPCS tasks the Strings will be a
 *  {@link uk.ac.starlink.jpcs.TaskReply} in the form of an XML document.
 *  @param the name of the method
 *  @param the parameters for the method
 *  @return the TaskReply document.
 */
   public String[] runTask ( String method, String[] params ) throws Exception {
      call.setOperationName( method );
      call.removeAllParameters( );
      call.addParameter( "params", XMLType.SOAP_ARRAY, ParameterMode.IN );
      call.setReturnType( XMLType.SOAP_ARRAY );
      if ( DEBUG ) {
        String cmdline = "";
        for( int i=0; i<params.length; i++ ) cmdline = cmdline + params[i] + " ";
        System.out.println("Invoke '" + method + "' '" + cmdline + "'" );
      }
      return (String[]) call.invoke( new Object [] { params } );
    }

/** Run the given method on this service with the given parameters.
 *  The interpretation of the return array of Strings will depend on the task.
 *  @param the name of the method
 *  @param the parameter for the method
 *  @return the TaskReply document.
 */
   public String[] runTask ( String method, String param ) throws Exception {
      call.setOperationName( method );
      call.removeAllParameters( );
      call.addParameter( "param", XMLType.XSD_STRING, ParameterMode.IN );
      call.setReturnType( XMLType.SOAP_ARRAY );
      if ( DEBUG ) {
        System.out.println("Invoke '" + method + "' '" + param + "'" );
      }
      return (String[]) call.invoke( new Object [] { param } );
    }

/** Run the given method on this service with the given two parameters.
 *  The interpretation of the return array of Strings will depend on the task.
 *  @param the name of the method
 *  @param the first parameter for the method
 *  @param the first parameter for the method
 *  @return the result strings.
 */
   public String[] runTask ( String method, String param1, String param2 )
    throws Exception {
      call.setOperationName( method );
      call.removeAllParameters( );
      call.addParameter( "param1", XMLType.XSD_STRING, ParameterMode.IN );
      call.addParameter( "param2", XMLType.XSD_STRING, ParameterMode.IN );
      call.setReturnType( XMLType.SOAP_ARRAY );
      if ( DEBUG ) {
        System.out.println("Invoke '" + method + "' '" +
         param1 + " " + param2 + "'" );
      }
      return (String[]) call.invoke( new Object [] { param1, param2 } );
    }
    
}
