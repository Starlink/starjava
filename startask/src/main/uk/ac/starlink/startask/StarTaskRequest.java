package uk.ac.starlink.startask;

import java.io.*;
import java.lang.reflect.*;
import net.jini.core.entry.*;
import uk.ac.starlink.jpcs.TaskReply;

/** Information required to run a {@link StarTask}
*/
public class StarTaskRequest implements Entry {

/** An identifier for the request
*/
public StarTaskRequestId reqId = null;

/** The name of the Starlink package class.
*   This can be of the form "pkg.Class" where pkg is the name of the Java
*   package (minus the uk.ac.starlink bit) where this is not the class name
*   in lower case. 
*/
public String reqPackage = null;

/** The name of the task method within the package class
*/
public String reqTask = null;

/** The parameters for the task
*/
public String[] reqParameters = null;

/* A flag to indicate if the task should be completed before the client
*  issues the next request.
*/
public Boolean waitFlag = null;

/** Construct a template StarTaskRequest
*/
public StarTaskRequest() {
}

/** Construct a complete StarTaskRequest
 *  @param pkg the Starlink package
 *  @param task the task method within the package
 *  @param parameters the task parameter specifiers
 */
public StarTaskRequest(
 StarTaskRequestId id, String pkg, String task, String[] parameters ) {
   reqId = id;
   reqPackage = pkg;
   reqTask = task;
   reqParameters = parameters;
}

/** A simple test for the StarTaskRequest class.
 *  <p>
 *    % uk.ac.starlink.startask.StarTaskRequest pkg task params...
 *  <p>
 *  For example:<br>
 *    % uk.ac.starlink.startask.StarTaskRequest Kappa stats comwest
 *  <p>
 *  A StarTaskRequest is created from the arguments and its shellRunTask()
 *  and runTask() methods obeyed in turn. The {@link uk.ac.starlink.jpcs.Msg}
 *  component of the returned {@link uk.ac.starlink.jpcs.TaskReply}s are
 *  displayed.
 */ 
public static void main( String[] args ) {
   int npars = args.length;

   try{
      if ( npars < 2 ) {
         throw new Exception( "StarTaskRequest: Insufficient arguments" );
      } else {
         String[] params = new String[ npars - 2 ];
         System.arraycopy( args, 2, params, 0, npars-2 );
         StarTaskRequest str = new StarTaskRequest(
          new StarTaskRequestId(), args[0], args[1], params );
         System.out.println( "\nRunning via ShellRunner" );
         str.shellRunTask().getMsg().flush();;
         System.out.println( "\nRunning directly" );
         str.runTask().getMsg().flush();
      }
   } catch( Throwable e ) {
      System.out.println( e.getMessage() );
   }
}  

/** Set the {@link StarTaskRequestId} for this StarTaskRequest
 *  @param id the required StarTaskRequestId
 */
public void putId( StarTaskRequestId id ) {
   reqId = id;
}


/** Set the package field for this StarTaskRequest
 *  @param pkg the name of the required Starlink application package
 */
public void putPackage( String pkg ) {
   reqPackage = pkg;
}

/** Set the task field for this StarTaskRequest
 *  @param task the name of the required task.
 */

public void putTask( String task ) {
   reqTask = task;
}

/** Set the parameters array for this StarTaskRequest
 *  @param parameters the array of parameters
 */ 
public void putParameters( String[] parameters ) {
   reqParameters = parameters;
}

/** Get the StarTaskRequestID for this StarTaskRequest
*/
public StarTaskRequestId getId() {
   return reqId;
}

/** Get the Starlink package name requested by this StarTaskRequest
*/
public String getPackage() {
   return reqPackage;
}

/** Get the task name requested by this StarTaskRequest
*/
public String getTask() {
   return reqTask;
}

/** Get the parameters array for this StarTaskRequest
*/
public String[] getParameters() {
   return reqParameters;
}

/** Run the requested task using ShellRunner
 */
TaskReply shellRunTask() throws Exception {
   int nPars = reqParameters.length;
   String[] args = new String[ nPars + 2 ];
   args[0] = reqPackage;
   args[1] = reqTask;
   System.arraycopy( reqParameters, 0, args, 2, nPars );
   
   ShellRunner sr = new ShellRunner();

   return TaskReply.readReply( sr.runPack( args ) );
   
}

/** Set the wait flag
 *  @param flag true if the task should be completed before continuing
 */
   void setWait( boolean flag ) {
      waitFlag = new Boolean( flag );
   }
   
/** Get the wait flag
 *  @return true if the task should be completed before continuing
 */
   boolean getWait() {
      return waitFlag.booleanValue();
   }
/** Run the requested task directly
 */
TaskReply runTask() throws Exception {
   Class packageClass = Class.forName(
     "uk.ac.starlink." + reqPackage.toLowerCase() + "." + reqPackage );
   Constructor packageConstructor = packageClass.getConstructor( null );
   Object pkg = packageConstructor.newInstance( null );
   Method task =
    packageClass.getDeclaredMethod( reqTask, new Class[] { String[].class } );
   return TaskReply.readReply( 
   (String[])(task.invoke( pkg, new Object[] { reqParameters } ) ) ); 
}

/** Display the request on System.out
*/
void display() {
   display( System.out );
}

/** Display the request on a specified PrintStream
*/
void display( PrintStream out ) {
   out.println( "Request ID " + getId() );
   out.println(
    getPackage() + ":" + getTask() + " (" + (getWait()?"Wait)":"No wait)") );
   out.println( "Parameters:" );
   String[] params = getParameters();
   if( params != null ) {
      for( int i=0; i<params.length; i++ ) {
         out.println( "   " + params[i] );
      }
   } else {
      out.println( "   " + params );
   }
}
}
