package uk.ac.starlink.startask;

import java.io.Serializable;
import java.util.Date;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/** A class to represent the ID of a StarTaskRequest. The ID is expected to be
  * unique within a JavaSpace.
  * Currently it contains a string (possibly the username of the user), an
  * integer id number and the creation Date.
*/
public class StarTaskRequestId implements Serializable {

private String reqUsername;
private int reqId;
private Date reqDate;
private String timeString;

/** Construct a null StarTaskRequestId
*/
StarTaskRequestId() {
   reqUsername = null;
   reqId = -1;
   reqDate = null;
   timeString = null;
}

/** Construct a full StarTaskRequestId
*   @param username An identification string
*   @param id An identifying integer
*/
public StarTaskRequestId( String username, int id ) {
   reqUsername = username;
   reqId = id;
   reqDate = new Date();
   timeString = DateFormat.getTimeInstance().format( reqDate );
}

/** Get the Id number for this StarTaskRequestId
 *  @return the Id number.
 */
public int getId() {
   return reqId;
}

/** Get the 'username' string of this StarTaskRequestId
 *  @return the username
 */
public String getName() {
   return reqUsername;
}

/** Get the date of this StarTaskRequestId
 *  @return the date
 */
public Date getDate() {
   return reqDate;
}

/** Convert this StarTaskRequestId to a human-readable String
 *  @return a human-readable String
*/
public String toString() {
   String retString = reqUsername + ":" + Integer.toString( reqId );
   if( timeString != null ) {
      retString = retString + " " + timeString;
   }
   return retString;
}

/** See if this StarTaskRequestId is the same as another
  * @param str the StarTaskRequestId to be compared with this.
  * @return true if the two request ids are equal
  */
   public boolean equals( StarTaskRequestId id ) {
  
      boolean reply = false;
     
      if( id != null ) {
         if( ( id.getId() == reqId )
          && ( id.getName().equals( reqUsername ) )
          && ( id.getDate().equals( reqDate ) ) ) {
            reply = true;
         }
      }
      
      return reply;
   }
}
