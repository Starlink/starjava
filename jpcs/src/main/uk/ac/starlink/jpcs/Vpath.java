package uk.ac.starlink.jpcs;

import java.util.*;

/** The class defining search path of possible sources from which a
 *  {@link Parameter} value may be obtained. Each possible source is tried in
 *  turn until a valid value is obtained. Each Parameter has its own Vpath.
 */
final class Vpath extends ValuePath {
private static final String[] srces =
            {"current", "default", "dynamic", "global", "noprompt", "prompt"};

/** Constructs a Vpath but does not set the search path.
 */
Vpath() {
      super();
}

/** Gets the list of allowed sources
 *  @return a List of Strings
 */ 
protected List allowedSources() {
   return Arrays.asList( srces );
}

/** Sets the search path for this Vpath.
 *  The search path is defined as a comma-separated list of sources. Possible
 *  sources are:
 *  <dl>
 *  <dt>current</dt>
 *  <dd>The value from the last successful invocation of the task.</dd>
 *  <dt>default</dt>
 *  <dd>The static default defined in the Interface File.</dd>
 *  <dt>global</dt>
 *  <dd>The value of the global parameter associated with this Parameter by
 *  an 'association' field in the Interface File.</dd>
 *  <dt>prompt</dt>
 *  <dd>A request to the user</dd>
 *  <dt>noprompt</dt>
 *  <dd>A NullParameterValue - prevents drop-through to the implied 'prompt'
 *  if all else fails.</dd>
 *  </dl>
 *  The above list describes the intended effects but the system is implemented
 *  by invoking methods of the Parameter (see {@link ValuePath} for details)
 *  so the actual effect will be determined by those methods.
 *  @param str the search path
 */
void setPath( String str ) throws ParameterException {
   try {
      super.setPath( str );
   } catch ( ParameterException e ) {
      throw new ParameterException( e.getMessage() + " in Vpath" );
   }
}


   

}
