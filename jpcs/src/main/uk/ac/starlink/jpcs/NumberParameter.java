package uk.ac.starlink.jpcs;

import java.util.*;
import java.util.regex.*;
import java.lang.reflect.*;
import java.io.*;
import java.text.*;

/**
  * NumberParameter is a subclass of {@link Parameter} for numeric parameters
  * of Starlink applications using the <code>jpcs</code> infrastructure.
  * .
  * It defines fields to describe the parameter's:
  * <ul>
  * <li> name and keyword
  * <li> command line position
  * <li> state
  * <li> value search paths
  * <li> prompt strings
  * <li> global parameter associations
  * <li> value
  * </ul>
  * and methods to set and get them.
  *
  * @author Alan Chipperfield (Starlink)
  * @version 0.0
  */
  

public class NumberParameter extends Parameter {

   static Pattern number = Pattern.compile( 
                               "[+-]?\\d*(\\.\\d*)?([EeDd]?[+-]?\\d*)?" ); 
   
   Number parMin = null;
   Number parMax = null;
   

   public NumberParameter( int position, String name, String prompt,
                            String vpath, String ppath, String keyword )
         throws ParameterException {
      super ( position, name, prompt, vpath, ppath, keyword );
   }
   

   public NumberParameter( String name, String prompt,
                            String vpath, String ppath, String keyword )
         throws ParameterException {
      this ( 0, name, prompt, vpath, ppath, keyword );
   }

   
   public NumberParameter( String name )
         throws ParameterException {
      this( 0, name, "", null, null, name );
   }
   
   public NumberParameter()
         throws ParameterException {
      this( 0, "", "", null, null, "" );
   }
   
/**
 * Generates a parameter value object for the NumberParameter class from the
 * given String. If a number cannot be parsed as a Number or an array, the
 * appropriate {@link ParameterStatusValue} is returned if the given String is
 * "!" (null) or "!!" (abort). The given String will be returned if it is "min"
 * or "max" ignoring case.
 * If the String has the form of a numerical array, the parameter value
 * generated will be an ArrayParameterValue of type double.
 *
 * @param str the String to be decoded
 * @return the parameter value Object 
 */
   Object fromString( String str ) throws Exception {
//System.out.println( "String is: " + str );

      Object obj;

 
/*  If the string represents an array of values not enclosed in [] or {},
*   enclose it in []
*/
      if ( ArrayParameterValue.isOpenArray( str ) ) {
//System.out.println( "Is open array" );
         str = "[" + str + "]";
      }
      
      if( !ArrayParameterValue.isArray( str ) ) {
/*   It's a scalar value
*/

         DecimalFormat df = new DecimalFormat();
         ParsePosition pp = new ParsePosition( 0 );
            
//System.out.println( "Is scalar" );
         String trimmedString = str.trim();
         Matcher m = number.matcher( trimmedString ); 
         if ( m.matches() ) {
//System.out.println( trimmedString + " matches" );
           trimmedString = trimmedString.replaceFirst( "[eDd]", "E" );
           
           obj = df.parse( trimmedString, pp );
//System.out.println( "ParsePosition is " + pp.toString() );
//         if ( pp.getIndex() != trimmedString.length() ) {
         } else if ( str.equals( "!" ) ) {
            obj = new NullParameterValue();
         } else if ( str.equals( "!!" ) ) {
            obj = new AbortParameterValue();
         } else if ( str.equalsIgnoreCase( "min" ) ||
                     str.equalsIgnoreCase( "max" ) ) {
            obj = str;
         } else {
            throw new ParameterException( "Invalid value " + str +
            " for NumberParameter " + getKeyword() );
         }

      } else {
/*    It's an array */
//System.out.println( "Is array" );
//System.out.println("NumberParameter.fromString: Creating ArrayParameterValue");
         String elStr;
         ArrayParameterValue strParVal = ArrayParameterValue.parseArray( str );
         String[] strarr = strParVal.getStringArray();
         int size = strParVal.getSize();
         double[] darr = (double[])Array.newInstance( double.class, size );
         for ( int i = 0; i<size; i++ ) {
            elStr = strarr[i];
            Matcher m = number.matcher( elStr ); 
            if ( m.matches() ) {
               elStr = elStr.replaceFirst( "[eDd]", "E" );
               darr[i] = Double.parseDouble( elStr );
               
            } else {
               throw new ParameterException( "Invalid array element " + elStr +
               " for NumberParameter " + getKeyword() );

            }
         }
         obj = new ArrayParameterValue(
          darr, strParVal.getNdims(), strParVal.getDims() );

      }   

      return obj;
   }
      

/**
 *  Checks if the given Object is a valid value for this Parameter type
 *  The object may be a Number or a String or an ArrayParameterValue with
 *  type double, float or int.
 *  @param the Object to be checked
 *  @return <code>true</code> if the Object is valid; <code>false</code>
 *  otherwise.
 */
   protected boolean checkValidObject( Object obj ) {
      if ( ( obj instanceof Number ) ||
           ( obj instanceof String ) || 
           ( obj instanceof ParameterStatusValue ) ) {
         return true;

      } else if (obj instanceof ArrayParameterValue) {
         String type = ((ArrayParameterValue)obj).getComponentType();
         if( type.equals("double") || type.equals("int") || 
             type.equals("float") ) {
             return true;
         } else {
             return false;
         }
            
      } else {
         return false;
      }
   }

/**
 * Gets this parameter as a Number. The Parameter is made 'active' if
 * necessary. 
 *
 * @return The Parameter value as a Number
 */
   private Number getNumber( String targetType, Method parseType )
      throws Exception {
      Object value=null;
      Number retval;
      boolean done = false;
      int tries = 1;

      retval = null;

/* Loop trying to obtain a value for the parameter
 */
      while ( !done && (tries <= MAX_TRIES)  ) {
         tries++;
         try {
            this.makeActive();
/* Parameter is active - get value or throw exception */
//System.out.println( "Made active. Value " + getValue() );
            value = getValue();

            if ( value instanceof Number ) {
               retval = (Number)value;
               done = true;
            } else if ( value instanceof String ) {
/* If value is a String, check for "min" or "max". If it's neither of those
 * use the supplied parseMethod to parse the String.
*/
               if ( ((String)value).equalsIgnoreCase( "max" ) ) {
                  if ( hasMax() ) {
                     retval = (Number)getMax();
                     done = true;
                  } else {
                     setMessage(
                       targetType + " for Parameter " + getKeyword() + 
                       ": No maximum value set");
                     cancel();
                  }
               } else if ( ((String)value).equalsIgnoreCase( "min" ) ) {
                  if ( hasMin() ) {
                     retval = (Number)getMin();
                     done = true;
                  } else {
                     setMessage(
                       targetType + " for Parameter " + getKeyword() + 
                       ": No minimum value set");
                     cancel();
                  }
               } else {
                  try {
                     Object[] args = new Object[1];
                     args[0] = value;
                     retval = (Number)parseType.invoke( null, args );
                     done = true;
                  } catch ( Exception e ) {
                     setMessage(
                       targetType + " for Parameter " + getKeyword() + ": "
                       + value + " cannot be converted");
                     cancel();
                  }
               }
         
            } else if ( value instanceof ParameterStatusValue ) {
               done = true;
            
            } else {
               setMessage(
                 targetType + " for Parameter " + getKeyword()
                  + " - no conversion possible from class "
                  + value.getClass().getName() );
               cancel();
            }
         
         } catch ( Exception e ) {
//System.out.println( "NumberParameter.getNumber: caught Exception" );
            setMessage( e.getMessage() );
            cancel();
         }
      }
      
/* Now check if a value was obtained.
 * If it was, check for the special status values and throw the appropriate
 * Exception
 */
      if ( done ) {
         if ( value instanceof ParameterStatusValue ) {
            throw ((ParameterStatusValue)value).exception( this );
         }

      } else {
         throw new ParameterException( 
            targetType + " for Parameter " + getKeyword() + ": "
            + String.valueOf( MAX_TRIES )
            + " attempts failed to get a good value" );
      }
      
      return retval;
            
   } 
   
/**
 * Gets this parameter value as a float. The Parameter is made 'active' if
 * necessary. 
 *
 * @return the float value
 */
   public float getFloat() throws Exception {
      Method parseMethod;
      Class pars[] = new Class[1];
      
      pars[0] = Class.forName( "java.lang.String");
      parseMethod =
       Class.forName( "java.lang.Float" ).getMethod( "parseFloat", pars );

      return getNumber( "getFloat", parseMethod ).floatValue();
   }
   
/**
 * Gets this parameter value as a double. The Parameter is made 'active' if
 * necessary.  
 *
 * @return the double value
 */
   public double getDouble() throws Exception {
      Method parseMethod;
      Class pars[] = new Class[1];
      
      pars[0] = Class.forName( "java.lang.String");
      parseMethod =
       Class.forName( "java.lang.Double" ).getMethod( "parseDouble", pars );

      return getNumber( "getDouble", parseMethod ).doubleValue();
   }
   
/**
 * Gets this parameter value as an int. The Parameter is made 'active' if
 * necessary. 
 *
 * @return the int value
 */
   public int getInt() throws Exception {
      Method parseMethod;
      Class pars[] = new Class[1];
      
      pars[0] = Class.forName( "java.lang.String");
      parseMethod =
       Class.forName( "java.lang.Integer" ).getMethod( "parseInt", pars );
      return getNumber( "getInt", parseMethod ).intValue();
   }
   
/**
 * Gets this parameter value as a long. The Parameter is made 'active' if
 * necessary. 
 *
 * @return the long value
 */
   public long getLong() throws Exception {
      Method parseMethod;
      Class pars[] = new Class[1];
      
      pars[0] = Class.forName( "java.lang.String");
      parseMethod =
       Class.forName( "java.lang.Long" ).getMethod( "parseLong", pars );

      return getNumber( "getLong", parseMethod ).longValue();
   }
   
/**
 * Gets this parameter value as a short. The Parameter is made 'active' if
 * necessary. 
 *
 * @return the short value
 */
   public short getShort() throws Exception {
      Method parseMethod;
      Class pars[] = new Class[1];
      
      pars[0] = Class.forName( "java.lang.String");
      parseMethod =
       Class.forName( "java.lang.Short" ).getMethod( "parseShort", pars );

      return getNumber( "getSHORT", parseMethod ).shortValue();
   }
   
/**
 * Get this parameter value as a byte. The Parameter is made 'active' if
 * necessary. 
 *
 * @return the byte value
 */
   public byte getByte() throws Exception {
      Method parseMethod;
      Class pars[] = new Class[1];
      
      pars[0] = Class.forName( "java.lang.String");
      parseMethod =
       Class.forName( "java.lang.Byte" ).getMethod( "parseByte", pars );

      return getNumber( "getByte", parseMethod ).byteValue();
   }

/**
 * Get this parameter as a double array. The Parameter is made 'active' if
 * necessary. 
 * Array parameter values are stored as instances of class ArrayParameterValue
 * This saves the array values as a vector with supplementary dimension
 * information. Multi-dimensional arrays are returned as a 1-dimensional array.
 * If the parameter value is a scalar, it will be returned as a single element
 * array.
 *
 * @return a double array.
 */
   public double[] getDoubleArray() throws Exception {
/* getObject() will ensure a value has been obtained */
      Object obj = getObject();
      double[] retobj = null;
      int arrayLength;
      
      if ( obj instanceof ArrayParameterValue ) {
//System.out.println("getDoubleArray: Is array");
         retobj = ((ArrayParameterValue)obj).getDoubleArray();
               
      } else if (obj instanceof Number ) {
/*  Scalar */
/*  Create a 1 element array */
//System.out.println("getDoubleArray: Is not array");
         retobj = new double[ 1 ];
//System.out.println("getDoubleArray: call getDouble()" );
         retobj[0] = getDouble();
//System.out.println("getDoubleArray: retobj is " + retobj[0]);

      } else if ( obj instanceof ParameterStatusValue ) {
         throw ((ParameterStatusValue)obj).exception( this );
   
      }
         
      return retobj;
 
   }
   
/**
 * Get this parameter as an int array. The Parameter is made 'active' if
 * necessary. 
 * Array parameter values are stored as instances of class ArrayParameterValue
 * This saves the array values as a vector with supplementary dimension
 * information. Multi-dimensional arrays are returned as a 1-dimensional array.
 * If the parameter value is a scalar, it will be returned as a single element
 * array.
 *
 * @return an int array.
 */
   public int[] getIntArray() throws Exception {
/* getObject() will ensure a value has been obtained */
      Object obj = getObject();
      int[] retobj = null;
      int arrayLength;
      
      if ( obj instanceof ArrayParameterValue ) {
//System.out.println("getIntArray: Is array");
         ArrayParameterValue array = (ArrayParameterValue)obj; 
         retobj = new int[ array.getSize() ];
         double[] darr = array.getDoubleArray();
         for ( int i=0; i<array.getSize(); i++ ) {
            retobj[i] = (int)(darr[i]);
         }
               
      } else if (obj instanceof Number ) {
/*  Scalar */
/*  Create a 1 element array */
//System.out.println("getIntArray: Is not array");
         retobj = new int[ 1 ];
//System.out.println("getIntArray: call getInt()" );
         retobj[0] = getInt();
//System.out.println("getIntArray: retobj is " + retobj[0]);

      } else if ( obj instanceof ParameterStatusValue ) {
            throw ((ParameterStatusValue)obj).exception( this );
   
      }   
      return retobj;
 
   }

/**
 * Get this parameter as a float array. The Parameter is made 'active' if
 * necessary. 
 * Array parameter values are stored as instances of class ArrayParameterValue
 * This saves the array values as a vector with supplementary dimension
 * information. Multi-dimensional arrays are returned as a 1-dimensional array.
 * If the parameter value is a scalar, it will be returned as a single element
 * array.
 *
 * @return a float array.
 */
   public float[] getFloatArray() throws Exception {
/* getObject() will ensure a value has been obtained */
      Object obj = getObject();
      float[] retobj = null;
      int arrayLength;
      
      if ( obj instanceof ArrayParameterValue ) {
//System.out.println("getFloatArray: Is array");
         ArrayParameterValue array = (ArrayParameterValue)obj; 
         retobj = new float[ array.getSize() ];
         double[] darr = array.getDoubleArray();
         for ( int i=0; i<array.getSize(); i++ ) {
            retobj[i] = (float)(darr[i]);
         }
               
      } else if (obj instanceof Number ) {
/*  Scalar */
/*  Create a 1 element array */
//System.out.println("getFloatArray: Is not array");
         retobj = new float[ 1 ];
//System.out.println("getFloatArray: call getInt()" );
         retobj[0] = getFloat();
//System.out.println("getFloatArray: retobj is " + retobj[0]);
         

      } else if ( obj instanceof ParameterStatusValue ) {
            throw ((ParameterStatusValue)obj).exception( this );
      }
           
//System.exit(0);        
      return retobj;
  
   }

/**
 * Get this parameter as an ArrayParameterValue. The Parameter is made 'active' if
 * necessary. 
 * Array parameter values are stored as instances of class ArrayParameterValue
 * This saves the array values as a vector with supplementary dimension
 * information. Multi-dimensional arrays are returned as a 1-dimensional array.
 * If the parameter value is a scalar, it will be returned as a single element
 * array.
 *
 * @return an ArrayParameterValue.
 */
   public ArrayParameterValue getArray() throws Exception {
/* getObject() will ensure a value has been obtained */
      Object obj = getObject();
      ArrayParameterValue retobj = null;
      int arrayLength;
      
      if ( obj instanceof ArrayParameterValue ) {
//System.out.println("getFloatArray: Is array");
         retobj = (ArrayParameterValue)obj;
               
      } else if (obj instanceof Double ) {
/*  Create a 1 element ArrayParameterValue */
//System.out.println("getArray: Is Double, not array");
         double[] arr = new double[ 1 ];
         arr[0] = getDouble();
         int[] dims = {1};
         retobj = new ArrayParameterValue( arr, 1, dims );
               
      } else if (obj instanceof Float ) {
/*  Create a 1 element ArrayParameterValue */
//System.out.println("getArray: Is Float, not array");
         float[] arr = new float[ 1 ];
         arr[0] = getFloat();
         int[] dims = {1};
         retobj = new ArrayParameterValue( arr, 1, dims );
               
      } else if (obj instanceof Integer ) {
/*  Create a 1 element ArrayParameterValue */
//System.out.println("getFloatArray: Is Integer, not array");
         int[] arr = new int[ 1 ];
         arr[0] = getInt();
         int[] dims = {1};
         retobj = new ArrayParameterValue( arr, 1, dims );

      } else if ( obj instanceof ParameterStatusValue ) {
            throw ((ParameterStatusValue)obj).exception( this );
      }
           
//System.exit(0);        
      return retobj;
  
   }
   
   
/**
 * Sets the parameter value to a Double object representing the given double
 * value 
 *
 * @param val double value to be set
 */
   public void putDouble( double val ) throws ParameterException {
//   System.out.println( "Val is: " + val );
      
      set( new Double( val ) );
      
      return;
   }
  
/**
 * Sets the parameter value to a Double object representing the given float
 * value. 
 *
 * @param val float value to be set
 */
   public void putFloat( float val ) throws ParameterException {
//   System.out.println( "Val is: " + val );
      
      Float f = new Float( val );
      set( new Float( val ) );
      
      return;
   }
  
/**
 * Sets the parameter value to an Integer object representing the given int
 * value. 
 *
 * @param val int value to be set
 */
   public void putInt( int val ) throws ParameterException {
//   System.out.println( "Val is: " + val );
      
      set( new Integer(val) );
      
      return;
   }
 
/** Gets the maximum allowed value for this Parameter.
  * @return the maximum value.
  */
   private Number getMax() {
      return parMax;
   }
   
/** Gets the minimum allowed value for this Parameter.
  * @return the minimum value.
  */
   private Number getMin() {
      return parMin;
   }
   
/** Sets the maximum allowed value for this Parameter.
  * @param the maximum value.
  */
   public void setMax( Number obj ) {
      parMax = obj;
   }
   
/** Sets the minimum allowed value for this Parameter.
  * @param the minimum value.
  */
   public void setMin( Number obj ) {
      parMin = obj;
   }
   
/** Tells if this Parameter has a maximum value set.
 *  @return <code>true</code> if a maximum value is set; <code>false</code> if
 *  not.
 */
   private boolean hasMax() {
      return parMax != null;
   }
   
/** Tells if this Parameter has a minimum value set.
 *  @return <code>true</code> if a minimum value is set; <code>false</code> if
 *  not.
 */
   private boolean hasMin() {
      return parMin != null;
   }
      
}
