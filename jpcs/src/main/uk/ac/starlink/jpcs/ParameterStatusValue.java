package uk.ac.starlink.jpcs;
/** This abstract class provides an umbrella class for Parameter values
 *  indicating a special status for the Parameter.
 */
  
abstract public class ParameterStatusValue {

/** A method which will return an Exception appropriate to the particular
 *  Parameter and status.
 *  @param p the Parameter
 *  @return the Exception
 */
abstract public Exception exception( Parameter p );
}

