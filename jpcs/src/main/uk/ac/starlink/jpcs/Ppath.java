package uk.ac.starlink.jpcs;

import java.util.*;

/** The class defining search path of possible sources from which a suggested
 *  {@link Parameter} value may be obtained, for display in a prompt. Each
 *  possible source is tried in turn until a valid value is obtained. Each
 *  Parameter has its own Ppath.
 */
final class Ppath extends ValuePath {

private static final String[] srces =
            {"current", "default", "dynamic", "global"};

/** Constructs a Ppath but does not set the search path.
 */
Ppath() {
      super();
}



/** Gets the list of allowed sources.
 *  @return a List of Strings
 */ 
List allowedSources() {
   return Arrays.asList( srces );
}

/** Sets the search path for this Ppath.
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
      throw new ParameterException( e.getMessage() + " in Ppath" );
   }
}

}
