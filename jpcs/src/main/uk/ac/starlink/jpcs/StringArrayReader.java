package uk.ac.starlink.jpcs;

import java.io.*;
import java.util.*;
import org.xml.sax.*;

/** A class which allows a String array to be treated as a Reader.
 */
public class StringArrayReader extends Reader {

ArrayList aList;
Iterator it;
char[] line;
int linsz = 0;
int ptr = 0;
 
/** Creates a StringArrayReader from the given array of Strings
*/
public StringArrayReader( String[] list ) {
   aList = new ArrayList( Arrays.asList( list ) );
   it = aList.iterator();
}

/** Implements the read() method reuired for a Reader
 *  @param destination buffer
 *  @param offset at which to start storing characters
 *  @param the maximum number of characters to store
 *  @return the number of characters stored, or -1 if the end of the stream has
 *          been reached.
 */ 
public int read( char[] cbuf, int off, int len ) {
   int nchars;

   if( ptr == linsz ) {
// Need new line
      if( it.hasNext() ) {
         String str = (String)it.next();
         linsz = str.length();
         ptr = 0;
         line = new char[linsz];
         str.getChars( 0, linsz, line, 0 );
      
      } else {
// End of Msg
         return -1;
      }

   }
   
// Calculate the number of characters to copy
   if( (linsz - ptr) < len ) {
      nchars = linsz - ptr;
   } else {
      nchars = len;
   }

// Copy the characters
   System.arraycopy( line, ptr, cbuf, off, nchars );
   ptr += nchars;
   off += nchars;
   
// Return the number of characters copied
   return nchars;
   
}

/** Close the stream
*/
public void close() {
}


/** A quick test of the system. The StringArrayReader is converted to an
 *  InputSource which is read by an IfxHandler.
 */
public static void main( String[] args ) throws Exception {

String[] ifx = {
  "<interface name=\"test\">",
  "<parameter name=\"par1\"",
  " type=\"NumberParameter\"",
  " position=\"1\"/>",
  "</interface>"};


StringArrayReader sr = new StringArrayReader( ifx );
IfxHandler handler = new IfxHandler();
handler.printOn(true);
ParameterList plist = handler.readIfx( new InputSource(sr) ); 

}

}   
      
   
