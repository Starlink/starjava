package uk.ac.starlink.kappa;

import java.lang.reflect.*;
import uk.ac.starlink.jpcs.*;
import uk.ac.starlink.startask.ShellRunner;

public class KAPPA extends Dtask {

public KAPPA() {
}

public static void main( String[] args ) throws Exception {

   KAPPA monolith = new KAPPA();

/* Get the task name and parameters */
   String taskName = args[0];
   int cmdlen = args.length - 1;
   String[] cmdline = new String[ cmdlen ];
   System.arraycopy( args, 1, cmdline, 0, cmdlen );

/* Get the Method to be invoked for this task */
   Method taskMethod = monolith.getTaskMethod( taskName );
//System.out.println("Method is: " + taskMethod.getName() );

/* Invoke the task method with the given command line */
   Object[] arguments = { cmdline };
   String[] s = (String[])taskMethod.invoke( monolith, arguments );
   

/* The task method returns a TaskReply XML document in the array of String.
 * Usually the user is only interested in seeing the messages in the Msg part
 * of the reply but sometimes, notably when using ShellRunner to run the task
 * as a web service, the whole reply should be listed verbatim so that it can
 * be returned to the client. The ShellRunner runPack script will cause the
 * System Property star.ShellRunner to be set true.
 */
   if( Boolean.getBoolean( "star.ShellRunner" ) ) {
      for( int i=0; i<s.length; i++) {
         System.out.println( s[i] );
      }
   } else {
      TaskReply tr = TaskReply.readReply( s );
      tr.getMsg().flush();
   }
   
   System.exit(0);
}

public String[] contour( String[] cmdline ) {
   return runTask( "contour", cmdline, "jnicontour" );
}

public String[] display( String[] cmdline ) {
   return runTask( "display", cmdline, "jnidisplay" );
}
   
public String[] remDisplay( String[] cmdline ) {
   ShellRunner sr = new ShellRunner();
   return sr.rundisplay( cmdline );
}

public String[] stats( String[] cmdline ) {
   return runTask( "stats", cmdline, "jnistats" );
}

private native int jnicontour( ParameterList plist, Msg msg );

private native int jnidisplay( ParameterList plist, Msg msg );

private native int jnistats( ParameterList plist, Msg msg );
   
static {
   System.loadLibrary("KAPPA");
}
   
}
