package uk.ac.starlink.jpcs;

/** The class of Object used as the {@link Parameter} value when the 'abort'
 *  value (!!) has been supplied.
 */
public class AbortParameterValue extends ParameterStatusValue {

/** Encodes the NullParameterValue as a String.
 *  @return the String "!!".
 */
   public String toString() {
      return "!!";
   }
 
/** Returns a {@link ParameterAbortException} whose message names the given
 *  Parameter.
 *  @param p the Parameter to be named
 *  @return A ParameterAbortException
 */
   public Exception exception( Parameter p ) {
      return new ParameterAbortException( p.getKeyword() );
   }

}

