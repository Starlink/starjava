package uk.ac.starlink.jpcs;

import java.lang.reflect.*;
import java.io.*;
import java.text.*;
import java.util.regex.*;

/**
  * BooleanParameter is a subclass of {@link Parameter} for boolean parameters
  * of Starlink applications using the <code>jpcs</code> infrastructure.
  * The normal value Object is a Boolean.
  *
  * @author Alan Chipperfield (Starlink)
  * @version 0.0
  */
  
public class BooleanParameter extends Parameter {

   public BooleanParameter( int position, String name, String prompt,
                            String vpath, String ppath, String keyword )
         throws ParameterException {
      super ( position, name, prompt, vpath, ppath, keyword );
   }
   

   public BooleanParameter( String name, String prompt,
                            String vpath, String ppath, String keyword )
         throws ParameterException {
      this ( 0, name, prompt, vpath, ppath, keyword );
   }

   
   public BooleanParameter( String name )
         throws ParameterException {
      this( 0, name, "", null, null, name );
   }   
  
/**
  * Tells if the given Object is suitable as a value for this parameter.
  * @param obj the Object to be checked.
  * @return true is the Object is valid
  */
   protected boolean checkValidObject( Object obj ) {
      if ( ( obj instanceof Boolean ) ||
           ( obj instanceof String ) ||
           ( obj instanceof ParameterStatusValue ) ||
           ( obj instanceof ArrayParameterValue ) ) {
         return true;
      } else {
         return false;
      }
   }
  
/**
 * Generates a parameter value object for this class from the given String.
 *
 * @param str String to be decoded
 * @return the parameter value
 * @throws ParameterException passed from {@link #getBooleanFromString} or
 * {@link ArrayParameterValue#parseArray}. 
 */
   Object fromString( String str ) throws ParameterException {
/*System.out.println( "String is: " + str );
*/
      Object obj;

/*  If the string represents an array of values not enclosed in [] or {},
*   enclose it in []
*/
      if ( ArrayParameterValue.isOpenArray( str ) ) {
         str = "[" + str + "]";
      }
      
      if( !ArrayParameterValue.isArray( str ) ) {
/*   It's a scalar value
*/
         if ( str.equals( "!" ) ) {
            obj = new NullParameterValue();
         } else if ( str.equals( "!!" ) ) {
            obj = new AbortParameterValue();
         } else {
               boolean bl = getBooleanFromString( str );
               obj = new Boolean( bl );
         }

      } else {
/*    It's an array */
//System.out.println("BooleanParameter.fromString: Creating ArrayParameterValue");
         ArrayParameterValue strParVal = ArrayParameterValue.parseArray( str );
         String[] strarr = strParVal.getStringArray();
         int size = strParVal.getSize();
//System.out.println("BooleanParameter.fromString: size " + size ); 
         boolean[] barr = (boolean[])Array.newInstance( boolean.class, size );
         for ( int i = 0; i<size; i++ ) {
//System.out.println("BooleanParameter.fromString: element - " + strarr[i] );
            barr[i] =
              BooleanParameter.getBooleanFromString( strarr[i] );
//System.out.println("BooleanParameter.fromString: element - " + barr[i] );
         }
         obj = new ArrayParameterValue(
          barr, strParVal.getNdims(), strParVal.getDims() );
//System.out.println("BooleanParameter.fromString: exits" );

      }
        
      return obj;
   }

/**
 * Interprets the given String as a boolean. The the value of the boolean
 * will be <code>true</code> if the String is <code>T,TRUE,Y</code or
 * <code>YES</code> and <code>false</code> if the String is
 * <code>F,FALSE,N</code> or <code>NO</code>  (all case insensitive).
 *
 * @param str the String to be decoded
 * @return the boolean value represented by str.
 * @throws ParameterException if the String is not valid
 */
   static boolean getBooleanFromString( String str ) throws ParameterException {
/* Ignore any leading and trailing spaces */
      String ustr = str.trim().toUpperCase();
      if ( ustr.equals("Y") || ustr.equals("YES") ||
           ustr.equals("T") || ustr.equals("TRUE") ) {
         return true;
      } else if ( ustr.equals("N") || ustr.equals("NO") ||
           ustr.equals("F") || ustr.equals("FALSE") ) {
         return false;
      } else {
//System.out.println( "getBooleanFromString throws ParameterException" );
         throw new ParameterException( "String " + str +
         " does not represent a boolean value" );
      }
   }
  
   
/**
 * Gets this parameter value as boolean. The Parameter is made 'active' if
 * necessary.  This will only work if the value object is Boolean or a suitable
 * String (case insensitive T,F,Y,N.TRUE,FALSE,YES,NO)
 *
 * @return the value object.
 * @throws ParameterNullException if this Parameter value is NullParameterValue.
 * @throws ParameterAbortException if this Parameter value is
 * AbortParameterValue.
 * @throws ParameterException if unable to obtain a valid result after 
 * {@link #MAX_TRIES} attempts.
 * @throws Exception Other Exceptions may result from attempts to obtain the
 * Paramter value.
 */
   public boolean getBoolean() throws Exception {
      Object value=null;
      boolean retval = false;
      boolean done = false;
      int tries = 1;

      while ( !done && (tries <= MAX_TRIES)  ) {
         tries++;
         try {
/* Get value or throw exception */
            this.makeActive();
/* Parameter is active  */
            value = getValue();

            if ( value instanceof Boolean ) {
               retval = ((Boolean)value).booleanValue();
               done = true;
               
            } else if ( value instanceof ParameterStatusValue ) {
               done = true;

            } else {
               setMessage(
                " getBoolean for Parameter " + getKeyword() + 
                ": Can't convert " + value + " to Boolean");
               cancel();
            }
         
         } catch ( Exception e ) {
//System.out.println( "BooleanParameter.getBoolean: caught Exception" );
            setMessage( e.getMessage() );
            cancel();
         }
      }
       
      if ( done ) {
         if ( value instanceof ParameterStatusValue ) {
            throw ((ParameterStatusValue)value).exception( this );
         }

      } else {
         throw new ParameterException( 
            "getBoolean for Parameter " + getKeyword() + ": "
            + String.valueOf( MAX_TRIES )
            + " attempts failed to get a good value" );
      }
      
      return retval;
  }

/**
 * Gets this parameter as a boolean array. The Parameter is made 'active' if
 * necessary. 
 * Array parameter values are stored as instances of class ArrayParameterValue
 * This saves the array values as a vector with supplementary dimension
 * information. Multi-dimensional arrays are returned as a 1-dimensional array.
 * If the parameter value is a scalar, it will be returned as a single element
 * array.
 *
 * @return a boolean array.
 */
public boolean[] getBooleanArray() throws Exception {
/* getObject() will ensure a value has been obtained */
      Object obj = getObject();
      boolean[] retobj = null;
      int arrayLength;
      
      if ( obj instanceof ArrayParameterValue ) {
//System.out.println("getBooleanArray: Is array");
         retobj = ((ArrayParameterValue)obj).getBoolean();
               
      } else {
/*  Scalar */
/*  Create a 1 element array */
//System.out.println("getBooleanArray: Is not array");
         retobj = (boolean[])Array.newInstance( boolean.class, 1 );
//System.out.println("getBooleanArray: call getBoolean()" );
         retobj[0] = getBoolean();
//System.out.println("getBooleanArray: retobj is " + retobj[0]);
      }   
         
      return retobj;
 
   }
   
/**
 * Sets the parameter value to a Boolean object representing the given boolean
 * value. 
 *
 * @param val boolean value to be set
 */
   public void putBoolean( boolean val ) throws ParameterException {
//System.out.println( "Val is: " + val );
      
      set( new Boolean( val ) );
      
      return;
   }
  
/**
 * Returns the parameter value as a String.
 * @param tf - <code>true</code> if the return String for boolean values should
 * be "true" or "false" and <code>false</code> if it should be "yes" or "no"
 * @param single <code>true</code> if the String should be just the initial
 * letter and <code>false</code> if the complete word.
 *
 * @return A string representing the value of the parameter.
 * Note that the parameter value could be an array or a ParameterStatusValue,
 * appropriate strings will be returned if that is the case.
 */
   public String toString( boolean tf, boolean single ) {
      Object value = getValue();
      String str;
      
      if ( value == null ) {
         str = "null";

      } else {
         str = value.toString();
System.out.println( "Value string is: " + str );         
         if ( !(value instanceof ParameterStatusValue) ) {
            String tval="true";
            String fval="false";
         
            if ( !tf ) {
              tval = "yes";
              fval = "no";
            }
         
            if ( single ) {
               tval = tval.substring(0,1);
               fval = fval.substring(0,1);
            }
            
            Pattern patt = Pattern.compile( "true" );
            Matcher matcher = patt.matcher( str );
            str = matcher.replaceAll( tval );
System.out.println( "Replace true string is: " + str );         

            patt = Pattern.compile( "false" );
            matcher = patt.matcher( str );
            str = matcher.replaceAll( fval );
System.out.println( "Replace false string is: " + str );         
         }
      }
       
      return str;
   }

}
