package uk.ac.starlink.jpcs;

import java.lang.reflect.*;
import java.io.*;
import java.text.*;

/** StringParameter is a subclass of {@link Parameter} for string parameters
  * of Starlink applications using the <code>jpcs</code> infrastructure.
  *
  * StringParameters are used where the required value is a String or array of
  * Strings. StringParameters are also used for filenames. 
  * @author Alan Chipperfield (Starlink)
  * @version 0.0
  */
  
public class StringParameter extends Parameter {

/** Constructs a StringParameter with the specified fields set.
  * @param position the command line position
  * @param name the parameter name
  * @param prompt the prompt string
  * @param vpath the value path
  * @param ppath the suggeste value path
  * @param keyword the parameter keyword
  */
   public StringParameter( int position, String name, String prompt,
                            String vpath, String ppath, String keyword )
         throws ParameterException {
      super ( position, name, prompt, vpath, ppath, keyword );
   }
   

/** Constructs a StringParameter with the specified fields set.
  * No command line position is allocated.
  * @param name the parameter name
  * @param prompt the prompt string
  * @param vpath the value path
  * @param ppath the suggeste value path
  * @param keyword the parameter keyword
  */
   public StringParameter( String name, String prompt,
                            String vpath, String ppath, String keyword )
         throws ParameterException {
      this ( 0, name, prompt, vpath, ppath, keyword );
   }

/** Constructs a StringParameter with the given name. Other fields will take
  * default values.
  * @param nam the parameter name
  */
   public StringParameter( String nam )
         throws ParameterException {
      this( 0, nam, "", null, null, nam );
   }
   
  
/**
  * Return true if the given Object is suitable as a value for this parameter.
  * Suitable Objects are:
  * <UL>
  * <LI> A String
  * <LI> An {@link ArrayParameterValue} with String components.
  * <LI> A {@link ParameterStatusValue}
  * </UL>
  * @param obj the Object to be checked
  * @return <code>true</code> if the Object is valid; <code>false</code>
  * otherwise.
  */
   protected boolean checkValidObject( Object obj ) {
      if ( ( obj instanceof String ) ||
           ( obj instanceof ArrayParameterValue ) ||
           ( obj instanceof ParameterStatusValue ) ) {
         return true;
      } else {
         return false;
      }
   }
  
/**
 * Generate a parameter value object for this class from the given String.
 * The String may be in the form of an array of strings enclosed in [] or {},
 * or unenclosed. For example:
 * <code>[abc,"d e f","g,h,i"]</code>
 * or <code>abc,def,ghi</code>
 * @param str String to be decoded
 * @return the parameter value 
 */
   Object fromString( String str ) throws ParameterException {
//System.out.println( "StringParameter.fromString: String is: " + str );

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
         } else if( str.matches( "\".*\"|'.*'" ) ) {
            obj = str.substring( 1, str.length()-1 );
         } else { 
            obj = str;
         }

      } else {
/*    It's an array */
//System.out.println("StringParameter.fromString: Creating ArrayParameterValue");
         obj = ArrayParameterValue.parseArray( str );

      }
         
      return obj;
   } 
   
   
/**
 * Gets this Parameter value as String array. The Parameter is made 'active' if
 * necessary. Its value is expected to be an {@link ArrayParameterValue}, if it is
 * a simple String, a one-element array of String will be returned.
 * @return the array of Strings.
 */
   public String[] getStringArray() throws Exception {
/* getObject() will ensure a value has been obtained */
      Object obj = getObject();
      String[] retobj = null;
      int arrayLength;
      
      if ( obj instanceof ArrayParameterValue ) {
//System.out.println("getStringArray: Is array");
         retobj = ((ArrayParameterValue)obj).getStringArray();
               
      } else {
/*  Scalar */
/*  Create a 1 element array */
//System.out.println("getStringArray: Is not array");
         retobj = (String[])Array.newInstance( java.lang.String.class, 1 );
//System.out.println("getStringArray: call getString()" );
         retobj[0] = getString();
//System.out.println("getStringArray: retobj is " + retobj[0]);
      }   
         
      return retobj;
   }
    
}
