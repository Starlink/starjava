package uk.ac.starlink.startask;

import java.io.*;
import uk.ac.starlink.jpcs.*;
import java.util.StringTokenizer;

/** Generates {@link StarTaskRequest}s from an array of String arguments.
*  (Usually the <code>args</code> array of a main method.)
*  The length of the <code>arguments</code> array with which the
*  StarTaskRequestGenerator is constructed is analysed.
*  <ul>
*  <li>&lt;1 Error
*  <li>=1 The name of a file containing command lines
*  <li>&gt;= 2  A single command line where:
*     <ul>
*     <li>   arguments[0] is the name of the service or package e.g. Kappa
*     <li>   arguments[1] is the name of the task in the service e.g. stats
*     <li>   arguments[2...] Optional are the parameters for the task.
*     </ul>
*  </ul>
*  <p>
*  Where input is from a file,
*  <ul>
*  <li> command lines have the form: <code>Service method parameters...</code>,
*  as above.
*  <li> comment lines begin with #
*  <li> blank lines are ignored
*  <li> command lines may be continued on more than one
*       line by escaping the return with \
*  <li> { introduces a set of commands which may be run
*       in parallel.
*  <li> } ends a set of commands which may be run
*       in parallel.parallel.
*  </ul>
*  <p>
*  ACCEPT and NOPROMPT are automatically added to the specified parameters
*  unless the {@link #allowPrompting} method has been invoked with parameter
*  <code>true</code>.
*/
public class StarTaskRequestGenerator {

private String[] argArray = null;
private int argsOffset;
private boolean isFile = false;
private LineReader in = null;
private boolean waitFlag = true;
private boolean noPrompt = true;

/** Construct a StarTaskRequestGenerator
*   @param arguments the arguments array - usually the arguments from the
*   application invocation command line.
*/
public StarTaskRequestGenerator( String[] arguments ) throws Exception {
   this( arguments, 0 );
}

/** Construct a StarTaskRequestGenerator from offset arguments
*   @param arguments the arguments array - usually the arguments from the
*   application invocation command line.
*   @param offset causes the <code>arguments</code> array to be treated as if
*   it begins at <code>arguments[offset]</code>.
*/
public StarTaskRequestGenerator( String[] arguments, int offset ) throws Exception {
/* Check the arguments to determine the commands to be sent */       
   if ( arguments != null ) {
      argsOffset = offset; 
      if ( arguments.length - offset == 1 ) {
         isFile = true;
         in = new LineReader ( new FileReader( arguments[offset] ) );
      } else if( arguments.length - offset > 0 ) {
         argArray = arguments;
      } else {
         throw new Exception( "Empty arguments array" );
      }
   } else {
      throw new Exception( "Null arguments array" );
   }
}

/** A test of StarTaskRequestGenerator.
 *  <p>
 *  <code>
 *  &nbsp;&nbsp;&nbsp;%uk.ac.starlink.startask.StarTaskRequestGenerator \<br>
 *  &nbsp;&nbsp;&nbsp;package task parameters...
 *  </code>
 *  <br>
 *  or
 *  <br>
 *  <code>
 *  &nbsp;&nbsp;&nbsp;%uk.ac.starlink.startask.StarTaskRequestGenerator filename
 *  </code>
 *  <p>
 *  the extracted components of the StarTaskRequest(s) are displayed. They will
 *  be alternately no prompting and prompting. 
 */
public static void main( String[] args ) throws Exception {
   StarTaskRequestGenerator strg = new StarTaskRequestGenerator( args );
   StarTaskRequest str = strg.nextRequest();
   boolean prompting = false;
   while( str != null ) {
      System.out.println( "Package: " + str.getPackage() );
      System.out.println( "Task:    " + str.getTask() );
      System.out.println( "Parameters:" );
      String[] params = str.getParameters();
      if( params != null ) {
         for( int i=0; i<params.length; i++ ) {
            System.out.println( "   " + params[i] );
         }
      } else {
         System.out.println( "   null" );
      }
      prompting = !prompting;
      strg.allowPrompting( prompting );
      str = strg.nextRequest();
   }
}

/** Get the next command line as a {@link StarTaskRequest}.
*   <p>
*   If a command line read from a file is between { and }, its waitFlag will be
*   set <code>false</code>; otherwise it will be set <code>true</code>.
*   The } will generate a StarTaskRequest with null service, method and
*   parameters but with its waitFlag set <code>true</code>.
*   @return a {@link StarTaskRequest} representing the next command line,
*   or null if there are no more.
*/
public StarTaskRequest nextRequest() throws Exception {

   StarTaskRequest str;
   String[] params;
   int parslen;
   
   if( isFile ) {

      String s = "";
      while( s != null && s.length() == 0 ) {
         s = in.readNonCommentLine( );
         if( s != null ) {
            if( s.equals("{") ) {
               waitFlag = false;
               s = "";
            } else if( s.equals("}") ) {
               waitFlag = true;
            }
         }
      } 


      if( s != null ) {
         if( s.equals("}") ) {
            str = new StarTaskRequest();
            str.setWait( true );
         } else {
            StringTokenizer st = new StringTokenizer( s );
            int n = st.countTokens();
            if ( n > 1 ) {
               String service = st.nextToken();
               String task = st.nextToken();
               n = n - 2;
               parslen = n + (noPrompt?2:0);
               params = new String[parslen];
               for( int i=0;i<n;i++) {
                  params[i] = st.nextToken();
               }

               if( noPrompt ) {               
/* Add ACCEPT NOPROMPT to prevent prompting on the server */
                  params[parslen-2] = "ACCEPT";
                  params[parslen-1] = "NOPROMPT";
               }

/* Create the StarTaskRequest */            
               str = new StarTaskRequest( null, service, task, params );
               str.setWait( waitFlag );

            } else if ( n == 1 && s.equals("end") ) {
               str = null;
                        
            } else {
               throw new Exception(
                "Command read from file has too few tokens" );
            }
         
         }
         
      } else {
/* end of file, close the file and return null */
         in.close();
         str = null;
      }
                     
   } else if( argArray != null ) {
/* A single command line generated from the args array */
      parslen = argArray.length - argsOffset;
      params = new String[ parslen ];
      if ( parslen  > 2 ) {
         System.arraycopy( argArray, 2, params, 0, parslen-2 );
      }

/* Add ACCEPT NOPROMPT to prevent prompting on the server */
      params[parslen-2] = "ACCEPT";
      params[parslen-1] = "NOPROMPT";
      
/* Create the StarTaskRequest */
      str = new StarTaskRequest(
              null, argArray[argsOffset], argArray[argsOffset+1], params );
      str.setWait( true );
      argArray = null;
      
   } else {
/* A single command has already been returned */
      str = null;
   }

   return str;
}

/** Prevent or allow prompting from the task.
*   By default, ACCEPT and NOPROMPT are automatically added to the command line
*   parameters to prevent the task from issuing prompts for any parameter values
*   which are not supplied.
*   @param flag <code>true</code> to allow prompting; <code>false</code> to
*   prevent prompting. 
*/
public void allowPrompting( boolean flag ) {
   noPrompt = !flag;
}

}
