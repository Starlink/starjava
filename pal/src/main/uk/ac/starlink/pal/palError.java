/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;

/** Class for Pal exceptions
 */

public class palError extends Exception {
    int Errcode;
    String ErrMess;

/** Create a new Pal exceptions
 *  @param status Exception number
 *  @param mess Exception message
 */
    public palError ( int status, String mess ) {
        Errcode = status; ErrMess = mess;
    }
/** Create a new Pal exceptions, with zero status
 *  @param mess Exception message 
 */
    public palError ( String mess ) {
        Errcode = 0; ErrMess = mess;
    }
/** Create a new Pal exceptions, with no message
 *  @param status Exception number
 */
//    public palError ( int status ) {
//        Errcode = status; ErrMess = "Unknown";
//    }

/** Get the Pal exceptions as a string
 *  @return The Exception number and message
 */
    public String toString() {
        if ( Errcode == 0 )
            return ErrMess;
        else
            return Errcode + " " + ErrMess;
    }
}
