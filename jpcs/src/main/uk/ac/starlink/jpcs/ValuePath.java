package uk.ac.starlink.jpcs;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

/** An abstract class providing the mechanics for defining and searching 
  * a search path to be used when a value is needed for a {@link Parameter}.
  * The search path consists of a comma-separated selection keywords which
  * cause appropriate methods of Parameter to be invoked in turn until a valid
  * Parameter value is found. 
  *
  * The keywords and associated methods which may be selected are:
  * <ul>
  * <li>default - getDefault()
  * <li>dynamic - getDynamic()
  * <li>global - getGlobal()
  * <li>prompt - requestValue()
  * <li>noprompt - noRequestValue()
  * </ul>
  * Subclasses of ValuePath define a list of allowed sources, selected from the
  * above list and may set a path consisting of a selection from the allowed
  * sources.
  *
  * If 'prompt' is one of the allowed sources, it is automatically added to the
  * end of the search path so that normally a prompt is issued if all else
  * fails. Note that unless they throw an Exception, requestValue() and
  * noRequestValue() should always provide a result (possibly a 
  * {@link ParameterStatusValue}) so noprompt may be used to prevent the
  * default prompt. 
 */
abstract class ValuePath implements Serializable {

abstract List allowedSources();

ArrayList sources;

private static String[] methods = { "getCurrent",
                     "getDefault",
                     "getDynamic",
                     "getGlobal",
                     "noRequestValue",
                     "requestValue"
                   };

private int[] vpath = { 0, 0, 0, 0, 0, 0 };
private int length;

ValuePath() {
   sources = new ArrayList( allowedSources() );
}

/** Sets the Parameter value search path. Sources in the given String are
 *  checked against the list of allowed sources for this ValuePath
 *  @param A comma-separated list of sources
 *  @throws ParameterException if an illegal source is specifed.
 */ 
void setPath ( String str ) throws ParameterException {
   StringTokenizer st = new StringTokenizer( str, ", ", false );
//System.out.println("setPath: " + str);
   int i = 0;
   while ( st.hasMoreTokens() ) { 
      String tok = st.nextToken().toLowerCase();
//System.out.println("Token: " + tok);
      vpath[i] = sources.indexOf( tok ) + 1;
      if ( vpath[i] > 0 ) {
         i++;
      } else {
         throw new ParameterException( "Illegal source (" + tok + ")" );
      }     
   }
   
/* Add 'prompt' to the path if it is one of the permitted sources.
 * Note that if 'noprompt' is on the path, it will never reach the 'prompt'.   
 */
   vpath[i] = sources.indexOf( "prompt" ) + 1;
   if ( vpath[i] > 0 ) {
      i++;
   }

/* Set the length of the ValuePath
 */
   length = i;
        
}

/** Gets the ValuePath as a String
 *  @return a comma-separated string of sources
 */ 
public String toString() {
   StringBuffer strb = new StringBuffer(40);
   for ( int i=0; i<length; i++ ) {
      strb.append( (String)sources.get(vpath[i]-1) + "," );
   }
   strb.deleteCharAt( strb.length()-1 );
   return strb.toString();
}


/** Searches the sources in this ValuePath until a potential value for the 
 *  specified Parameter is found.
 *  @param p The Parameter for which a value is required
 *  @return The potential Parameter value
 */
Object findValue ( Parameter p ) throws Exception {
   Object object = null;
   int i = 0;
   Method getVal;
   Class cls = ((Parameter)p).getClass();
//System.out.println("Class is: " + cls.getName() );
   try {
      while ( ( i < length ) && ( object == null ) ) {
         String method = methods[vpath[i]-1];
//System.out.println("Method is: " + method );
         getVal = ValuePath.getMethod( cls, method );
         if ( getVal != null ) {
            object = getVal.invoke( (Object)p, null );
//System.out.println( "Value is " + object );
         }
         i++;
      }
   } catch( InvocationTargetException e ) {
//System.out.println( "ValuePath.findValue: throws ParameterException " +
//ParameterUtility.getBaseCause( e ) );  
      throw new ParameterException(
        ParameterUtility.getBaseCause((Throwable)e).getMessage() );
      
   }
      
   return object;
}

/** Finds a method of the given Class requiring no arguments. Intended for
 *  use in searching a {@link ValuePath}, it may be useful in other contexts.
 *  {@link Class#getMethod} is first used on the given Class. If that is not
 *  successful, (@link Class#getDeclaredMethod} is used on the superclass of
 *  the given Class. This allows public or protected methods of the superclass
 *  to be found.
 *  @param cls - the Class in which to look for the method
 *  @param method - the name of the required method
 *  @return  the required Method, or null is the method is not found.
 */
protected static Method getMethod( Class cls, String method ) {
   Method gotMethod;
   try {
//System.out.println( "Trying to get method from " + cls );
       gotMethod =
                cls.getDeclaredMethod( method, null );
   } catch ( NoSuchMethodException e ) {
      try {
//System.out.println( "Trying to get method from " + cls.getSuperclass() );
         gotMethod =
           cls.getSuperclass().getDeclaredMethod( method, null );
      } catch ( Exception e2 ) {
         e2.printStackTrace( System.out );
         gotMethod = null;
      }
            
   } catch ( Exception e ) {
      e.printStackTrace( System.out );
      gotMethod = null;
   }
   
   return gotMethod;

}

}

