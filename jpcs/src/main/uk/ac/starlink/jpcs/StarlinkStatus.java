package uk.ac.starlink.jpcs;

/** A class representing the status at the end of execution of a Starlink
*   Dtask.
*/ 
public class StarlinkStatus {

/** The OK status value
*/
public static final int SAI__OK = 0;

private int value;
private String message;

/** Construct a StarlinkStatus with an associated message
*   @param val The status value.
*   @param message The associated message.
*/
public StarlinkStatus( int val, String mess ) {
   value = val;
   message = mess;
}

/** Construct a StarlinkStatus with a blank associated message
*   @param val The status value.
*/
public StarlinkStatus( int val ) {
   this( val, "" );
}

/** Get the status value
*   @return the status value
*/
public int getValue() {
   return value;
}

/** Get the associated message
*   @return the associated message
*/
public String getMessage() {
   return message;
}

/** Test if this StarlinkStatus is an 'OK' status
*   @return <code>true</code> if the status value is StarlinkStatus.SAI__OK.
*/
public boolean isOK() {
   return value == SAI__OK;
}

}
