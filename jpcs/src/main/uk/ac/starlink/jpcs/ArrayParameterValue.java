package uk.ac.starlink.jpcs;

import java.util.*;
import java.lang.reflect.Array;

/** The class of Objects used as Parameter values when the value is an array.
 *  The array may have up to {@link #MAX_DIMS} dimensions but will be stored as
 *  a 1-d array together with its dimensions.
 */
public class ArrayParameterValue {

/** The maximum allowed number of dimensions */
public static final int MAX_DIMS = 7;

private Object arrVal;
private int arrNdims;
private int[] arrDims;

/** Construct an ArrayParameterValue of type double 
 *  @param array the array of doubles
 *  @param ndims the number of dimensions
 *  @param dims the dimensions
 */
ArrayParameterValue( double[] array, int ndims, int[] dims ) { 
  arrVal = array;
  arrNdims = ndims;
  arrDims = dims;
}

/** Construct an ArrayParameterValue of type float 
 *  @param array the array of float
 *  @param ndims the number of dimensions
 *  @param dims the dimensions
 */
ArrayParameterValue( float[] array, int ndims, int[] dims ) { 
  arrVal = array;
  arrNdims = ndims;
  arrDims = dims;
}

/** Construct an ArrayParameterValue of type int 
 *  @param array the array of int
 *  @param ndims the number of dimensions
 *  @param dims the dimensions
 */
ArrayParameterValue( int[] array, int ndims, int[] dims ) { 
  arrVal = array;
  arrNdims = ndims;
  arrDims = dims;
}

/** Construct an ArrayParameterValue of type String
 *  @param array the array of Strings
 *  @param ndims the number of dimensions
 *  @param dims the dimensions
 */
ArrayParameterValue( String[] array, int ndims, int[] dims ) { 
  arrVal = array;
  arrNdims = ndims;
  arrDims = dims;
}

/** Construct an ArrayParameterValue of type boolean 
 *  @param array the array of booleans
 *  @param ndims the number of dimensions
 *  @param dims the dimensions
 */
ArrayParameterValue( boolean[] array, int ndims, int[] dims ) { 
  arrVal = array;
  arrNdims = ndims;
  arrDims = dims;
}

/** Tells if the given String represents an array not enclosed in [] or {}.
  * That is if it has more that one element at the top level.
  * Elements are words separated by space or comma other than in a
  * quoted (") string, or array strings enclosed in [] or {}.
  * Examples:
  * <ul>
  * <li> a b c
  * <li> 1,2,3
  * <li> [1,2][3,4]
  * </ul>
  * @param str the String to be checked.
  * @return <code>true</code> if the given String represents an un-enclosed
  * array; <code>false</code> if it does not.
  */
public static boolean isOpenArray( String str ) {
   ArrayStringTokenizer st = new ArrayStringTokenizer( str );
   String tok = " ";
   int level=-1;
   int comp=0;
   boolean arrayString = false;
               
//System.out.println("ArrayParameterValue.isOpenArray: str is- '" + str + "'" );
   while ( tok != null && comp < 2 ) {
//System.out.println("ArrayParameterValue.isOpenArray: get next token" );
      tok = st.nextToken();
//System.out.println("ArrayParameterValue.isOpenArray: token is- '" + tok + "'"  );
      if ( tok != null ) {
         if ( tok.matches( "[\\{\\[]" ) ) {
            level++;
            if ( level == 0 ) comp++;
                  
         } else if ( tok.matches( "[\\}\\]]" ) ) {
            level--;
              
         } else if ( !tok.equals( "," ) ) {
            if ( level < 0 ) comp++;
        
         }
      }
   
   }

   if ( comp > 1 ) arrayString = true;
//System.out.println("ArrayParameterValue.isOpenArray: returns " + arrayString );
   
   return arrayString;
   
}

/** Tells if the String is a properly constructed representation of an
  * array. That is if it begins with [ or { and ends with matching ] or }.
  * @param str the String to be checked.
  * @return <code>true</code> if the string represents an enclosed array; 
  * <code>false</code> if it does not.
  */
static boolean isArray( String str ) {

   if ( ( str.startsWith( "[" ) && str.endsWith("]") ) ||
        ( str.startsWith( "{" ) && str.endsWith("}") ) ) {
      return true;
   } else {
      return false;
   }
}

/** Parses the given String representation of an array
 *  @param str the String to be parsed
 *  @return an ArrayParameterValue with component type String
 *  @throws ParameterException if the maximum number of dimensions for an
 *  ArrayParameterValue is exceeded or if the array representation is otherwise
 *  illegal.
 */
public static ArrayParameterValue parseArray( String str ) throws ParameterException {
   String tok;
   int level=-1;
   int ndims=1;
   int[] dims = new int[ MAX_DIMS ];
   Arrays.fill( dims, -1 );
   int[] count = new int[ MAX_DIMS ];
   Arrays.fill( count, 0 );
   ArrayList array = new ArrayList( 20 );
   boolean started = false;;
            
//System.out.println("ArrayParameterValue.parseArray: str is-" + str );

/* Create an ArrayStringTokenizer for the array, ensuring that the array is
* properly enclosed in [].
*/
   ArrayStringTokenizer st = new ArrayStringTokenizer( str );
                
/* Now the proper parse 
*/
   while ( st.hasMoreTokens() ) {
      tok = st.nextToken();
//System.out.println("ArrayParameterValue.parseArray: token is-" + tok );
      if ( tok.matches( "[\\{\\[]" ) ) {
         level++;
//System.out.println("ArrayParameterValue.parseArray: Level " + level);
         ndims = level + 1;
         if ( level == MAX_DIMS ) {
            throw new ParameterException( "Maximum number (" +
              MAX_DIMS + ") of dimensions exceeded for array." );
         }
         count[ level] = 0;
                  
      } else if ( tok.matches( "[\\}\\]]" ) ) {
         if ( dims[level] < 0 ) {
            dims[level] = count[level];
         } else if ( dims[level] != count[level] ) {
            throw new ParameterException( "Inconsistent number of " +
              "array elements at same level" );
         }
         level--;
//System.out.println("ArrayParameterValue.parseArray: Level " + level);
         if ( level >= 0 ) count[level]++;
               
      } else if ( !tok.equals( "," ) ) {
        
         array.add( tok );
//System.out.println("ArrayParameterValue.parseArray: array element " + tok
         count[level]++;
      }
   }
            
/*  Now create and save the array value 
*   comp is the number of array elements
*/
   count = new int[ ndims ];
   for ( int i=0; i<ndims; i++ ) {
      count[i] = dims[ndims-i-1];
//System.out.println(
// "ArrayParameterValue.parseArray: dimension " + i + " is " + count[i] );
   }
         
   String[] strarr = (String[])array.toArray( 
     (String[])Array.newInstance( String.class, 1 ) );

   return new ArrayParameterValue( strarr, ndims, count );
}

/** Gets the number of dimensions of this ArrayParameterValue.
 *  @return the number of dimensions
 */
int getNdims() {
   return arrNdims;
}


/** Gets the dimensions of this ArrayParameterValue.
 *  @return the array of dimensions
 */
int[] getDims() {
   return arrDims;
}

/** Gets the array of values of this ArrayParameterValue
 *  @return the array of values
 */
Object getObject() {
   return arrVal;
}

/** Gets the value of this ArrayParameterValue as an array of double.
 *  @return the array of double
 *  @throws ParameterException if the array type is not double[].
 */
double[] getDoubleArray() throws ParameterException {
//System.out.println( "getDoubleArray class: " + getObject().getClass() );
//System.out.println( "getDoubleArray type: " + getType() );
//System.out.println( "getDoubleArray comptype: " + getComponentType() );
   String arrType = getComponentType();
   
   if ( arrType.equals("double") ) {
//System.out.println("getDoubleArray: is double");
      return (double[])arrVal;
         
   } else {
//System.out.println("getDoubleArray: is other");
      throw new ParameterException( "ArrayParameterValue.getDoubleArray(): " +
        "Array type " + arrType + " is not double." );
   }
      
}

/** Gets the value of this ArrayParameterValue as an array of int.
 *  @return the array of int
 *  @throws ParameterException if the array type is not double[].
 */
int[] getIntArray() throws ParameterException {
   String arrType = getComponentType();
   
   if ( arrType.equals("double") ) {
//System.out.println("getIntArray: is double");
      int[] iarr = new int[ getSize() ];
      for ( int i=0; i<getSize(); i++ ) {
         iarr[i] = (int)((double[])arrVal)[i];
      }
      return iarr;
         
   } else {
//System.out.println("getIntArray: is other");
      throw new ParameterException( "ArrayParameterValue.getIntArray(): " +
        "Array type " + arrType + " is not double." );
   }

}


/** Gets the value of this ArrayParameterValue as an array of String.
 *  @return the array of String
 *  @throws ParameterException if the array type is not  double[], int[],
 *  float[], String[] or boolean[].
 */
String[] getStringArray() throws ParameterException {
   String[] retArray;
   String arrType = getComponentType();
   
//System.out.println("getStringArray: component type is: " + arrType );  
   if ( arrType.equals("java.lang.String") ) {
      retArray = (String[])arrVal;
   
   } else if ( arrType.equals("double") ) {
      retArray = (String[])Array.newInstance( java.lang.String.class, getSize() );
      for ( int i=0; i<getSize(); i++ ) {
         retArray[i] = String.valueOf( ((double[])arrVal)[i] );
      }
   
   } else if ( arrType.equals("int") ) {
      retArray = (String[])Array.newInstance( java.lang.String.class, getSize() );
      for ( int i=0; i<getSize(); i++ ) {
         retArray[i] = String.valueOf( ((int[])arrVal)[i] );
      }
   
   } else if ( arrType.equals("float") ) {
      retArray = (String[])Array.newInstance( java.lang.String.class, getSize() );
      for ( int i=0; i<getSize(); i++ ) {
         retArray[i] = String.valueOf( ((float[])arrVal)[i] );
      }
   
   } else if ( arrType.equals("boolean") ) {
      retArray = (String[])Array.newInstance( java.lang.String.class, getSize() );
      for ( int i=0; i<getSize(); i++ ) {
         retArray[i] = String.valueOf( ((boolean[])arrVal)[i] );
      }
      
   } else {
//System.out.println("getStringArray: is String type");
      throw new ParameterException( "ArrayParameterValue.getStringArray(): " +
        "Array type " + arrType + " is non-standard." );
   }
   
   return retArray;
}


/** Gets the value of this ArrayParameterValue as an array of boolean.
 *  @return the array of boolean
 *  @throws ParameterException if the array type is not boolean[].
 */
boolean[] getBoolean() throws ParameterException {
//System.out.println( "getBoolean class: " + getObject().getClass() );
//System.out.println( "getBoolean type: " + getType() );
//System.out.println( "getBoolean comptype: " + getComponentType() );
   
   String arrType = getComponentType();
   if ( arrType.equals("boolean") ) {
//System.out.println("getBoolean: is boolean");
      return (boolean[])arrVal;
         
   } else {
//System.out.println("getBoolean: is other");
      throw new ParameterException( "ArrayParameterValue.getBoolean(): " +
        "Array type " + arrType + " is not boolean." );
   }
      
}

/** Gets the size of the array.
  * @return the total number of elements in the array or <code>-1</code>
  * if no value has been set.
  */
int getSize() {
   int size;
   int ndims = getNdims();
   int[] dims = getDims();
   size = 1;
   for ( int i=0; i<ndims; i++ ) {
      size = size * dims[i];
   }

   return size;
}

/** Gets the type of the array
 *  @return the type of the array
 */
String getType() {
   return this.getObject().getClass().getName();
}

/** Gets the component type of the array
 *  @return the component type of the array
 */
String getComponentType() {
   return this.getObject().getClass().getComponentType().getName();
}
   

/** Converts this ArrayParameterValue to a String representaion of an array
 *  @return the String representation.
 */
public String toString() {
   StringBuffer str = new StringBuffer();

/* Now construct the string to be returned
*/
   try {

/* Get the stored array as an array of Strings and get its dimensions
*/
      String[] array = getStringArray();
      int ndims = getNdims();
      int[] dims = getDims();
      int i;
      int j;
      int div;
      
//System.out.println( "ndims " + ndims );
//for ( i=0;i<ndims;i++) System.out.println( dims[i] );
/* Append the required number of "[" to the string
*/
      for ( i=0; i<ndims; i++ ) str.append( "[" );
      i=0;
      while (  i<array.length ) {
/* Append the String array elements one at a time, followed by ","
*/
         str.append( array[i] + "," );
         i++;
         div = 1;

/* At dimension boundaries, append the required number of "]", deleting
 * the "," before the first one.
*/
         for ( j=0; j<ndims; j++ ) {
            div = div * dims[j];
//System.out.println( "Remainder " + i + " % " + div + " is " + (i%div) );
            if ( i % div == 0 ) {
               if ( j==0 ) str.deleteCharAt( str.length()-1 );  
               str.append( "]" );
            }
         }
/* If there are any elements remaining, at dimension boundaries, append the
 * required number of "[".
*/
         if ( i<array.length ) {
            div = 1;
            for ( j=0; j<ndims; j++ ) {
               div = div * dims[j];
//System.out.println( "Remainder " + i + " % " + div + " is " + (i%div) );
               if ( i % div == 0 ) str.append( "[" );
            }
         }
         
      }
      
   } catch ( ParameterException e ) {
      str.append( "[ " + e.getMessage() + "]" );
      
   }
   
   return str.toString();
   
}

}
  
