package uk.ac.starlink.startask;
import net.jini.core.entry.*;

/** A class to serve as a JavaSpace Entry conveying information about the
*   taking of a StarTaskRequest from the space 
*/

public class StarTaskAcceptance implements Entry {

/** The name of the server accepting the request
*/
public String starServer;

/** A copy of the request
*/
public StarTaskRequest starRequest;

/** Construct a template StarTaskAcceptance
*/
public StarTaskAcceptance() {
}

/** Construct a StarTaskAcceptance for a given server and {@link
*   StarTaskRequest}
*/
public StarTaskAcceptance( String server, StarTaskRequest request ) {
   starServer = server;
   starRequest = request;
}

/** Get the server of the StarTaskAcceptance
*/
public String getServer() {
   return starServer;
}

/** Get the StarTaskRequest od the StarTaskAcceptance
*/
public StarTaskRequest getRequest() {
   return starRequest;
}

/** Get the StarTaskRequestId of this StarTaskAcceptance
*/
public StarTaskRequestId getId() {
   if( starRequest != null ) {
      return starRequest.getId();
   } else {
      return null;
   }
}

}
