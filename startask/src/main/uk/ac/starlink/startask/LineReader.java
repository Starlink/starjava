/*
 */

package uk.ac.starlink.startask;

import java.io.*;

/** A class to read logical lines from an input source.
 *  A logical line may comprise a number of lines where all but the last has
 *  its terminator immediately preceded by '\'. Blank lines and lines beginning
 *  with '#' may be ignored.
 */
public class LineReader extends BufferedReader {

public LineReader( FileReader fr ) {
   super( fr );
}

/** Read the next line from the input source.
 *  A line is as desribed for {@link java.io.BufferedReader} except that a
 *  continuation is indicated if the line terminator is immediately preceded by
 *  '\'
 */ 
public String readLine() throws IOException {
   String s;
   
// Get the next physical line
   s = super.readLine();
   if( s != null ) {
      s = s.trim();
// Check for continuations - assemble logical line
      while( s.endsWith( "\\" ) ) {
         String continuation = super.readLine();
         if( continuation == null ) {
            s = s.substring( 0, s.length()-1 );
         } else {
            s = s.substring( 0, s.length()-1 ) + " " + continuation.trim();
         }
      }
   }
   
   return s;
}

/* Reads the next non-comment line.
 * A comment line is one which starts with '#' or is blank.
 */
public String readNonCommentLine() throws IOException {
   boolean gotLine = false;
   String s;

// Get next line
   s = readLine();
   while( !gotLine ) {
      if( s != null ) {
         s = s.trim();
// Ignore comments and blank lines
         if( s.startsWith( "#" ) || s.length() == 0 ) {
            s = readLine();
         } else {
            gotLine = true;
         }
      } else {
         gotLine = true;
      }
   }
   
   return s;
}

}