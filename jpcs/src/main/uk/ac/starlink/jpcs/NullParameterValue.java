package uk.ac.starlink.jpcs;

/** The class of Object used as the {@link Parameter} value when the null value
 *  (!) has been supplied.
 *  Note that this is not the same as the Parameter value being 'null',
 *  which occurs when the Parameter does not have a value (is not 'active').
 */
public class NullParameterValue extends ParameterStatusValue {

/** Encodes the NullParameterValue as a String.
 *  @return the String "!".
 */
   public String toString() {
      return "!";
   }
 
/** Returns a {@link ParameterNullException} whose message names the given
 *  Parameter.
 *  @param p the Parameter to be named
 *  @return A ParameterNullException
 */
   public Exception exception( Parameter p ) {
      return new ParameterNullException( p.getKeyword() );
   }
   
}

