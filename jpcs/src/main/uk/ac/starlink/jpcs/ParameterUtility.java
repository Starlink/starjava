package uk.ac.starlink.jpcs;
import java.lang.reflect.*;

/** A class containing static utility methods used by the Starlink parameter
 *system.
 */
class ParameterUtility {

/** Gets the base cause of an arbitrary depth of InvocationTargetExceptions
 *  @param e the Throwable whose cause is to be found
 *  @return the cause of e or null.
 */       
protected static Throwable getBaseCause( Throwable e ) {
   Throwable cause = e.getCause(); 
   if ( cause == null ) {
      return e;
   } else {
      return getBaseCause( cause );
   }
}

}
