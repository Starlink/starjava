package uk.ac.starlink.startask;

import java.io.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.net.URL;
import java.lang.reflect.Method;
import uk.ac.starlink.jpcs.Msg;
import uk.ac.starlink.jpcs.TaskReply;

/**  Enables specific shell commands to be run as a Java methods.
 *  The '<code>runxxx</code>' methods
 *    will execute a script of the same name found in the <code>util</code>
 *    package directory. The 'run' method will execute script
 *    <code>runxxx</code> where <code>xxx</code> is given as the first argument.
 *    <p>
 *    Scripts must be installed in the correct place and must
 *    specify the shell to be used on the first line. They must not require
 *    any input from the user.
 *    <p>
 *    The methods (apart from <code>main</code>) all return an array of Strings
 *    representing a {@link TaskReply} in XML form. 
 *    The {@link uk.ac.starlink.jpcs.Msg Msg} component of the TaskReply will
 *    be the output from the script intended for the user to see. Depending
 *    upon the script, the TaskReply may or may not contain a 
 *    {@link uk.ac.starlink.jpcs.ParameterValueList ParameterValueList} and 
 *    {@link uk.ac.starlink.jpcs.StarlinkStatus StarlinkStatus} component.
 */
public class ShellRunner {

ClassLoader classLoader = ShellRunner.class.getClassLoader();

/** Run a ShellRunner method from the command line.
 *  This is probably only useful for testing purposes.
 *  <p>
 *  Invocation:
 *  <p>
 *  <code>&nbsp;&nbsp;&nbsp;% java ShellRunner method arguments...</code>
 *  <p>
 *  method will be one of the methods of this class.
 *  <p>
 *  Output from the method intended for the user will be displayed.
 */
public static void main( String[] args ) throws Exception {
   ShellRunner shr = new ShellRunner();

/* Separate the method and its arguments */
   String task = args[0];
   int nargs = args.length-1;
   String[] params = new String[nargs];
   System.arraycopy( args, 1, params, 0, nargs ); 
   
   
/* Get the method and arguments to be used */
   Class[] sig = { String[].class };
   Method method = shr.getClass().getDeclaredMethod( task, sig );
   
/* Invoke the method.
 * Most methods will overwrite the first argument with the command to be
 * run in the shell
 */
   Object[] methodArgs = { params };
   String[] reply = (String[])method.invoke( shr, methodArgs );

/* Deal with the reply
 * If it has the form of a TaskReply as an XML document, parse it;
 * then extract the components; if not, handle as a normal array of Strings to be
 * displayed
 */
//for(int i=0;i<reply.length;i++)System.out.println(reply[i]);
   TaskReply tr = TaskReply.readReply( reply );
   tr.getMsg().flush();
}

/** Executes a shell command in a separate process.
 *  @param Array containing the command to run in a sub-process and its
 *  arguments.
 *  @return an array of Strings representing a {@link TaskReply} in XML form.
*/
private String[] executeArray( String command, String[] args ) {
   int status;
   Msg msg = new Msg();
   msg.setBuffered( true );
   
/* Get the script to be obeyed */
/* This allows it to be extracted from a jar file, for example */
   try{
      String scriptName = getScript( command );
      String line;
      int nargs = args.length;
      String[] arguments = new String[nargs + 1];
      arguments[0] = scriptName;
      System.arraycopy( args, 0, arguments, 1, nargs ); 

/* Inform of command being run */
//System.out.println( "ShellRunner executeArray:" );  
//for(int i=0;i<arguments.length;i++) System.out.println(arguments[i]);
      try {
/* Run up a process to execute the command and get the output and error streams
 * as buffered readers */   
         Runtime runt = Runtime.getRuntime();
         Process run = runt.exec( arguments, null );
         BufferedReader ir = new BufferedReader(
           new InputStreamReader( run.getInputStream() ) );
         BufferedReader er = new BufferedReader(
           new InputStreamReader( run.getErrorStream() ) );

/* Read the output stream and output each line */
         line = ir.readLine();
         while ( line != null ) {
            msg.out( line );
            line = ir.readLine();
         }    

/* Now check the standard output for TaskReply + appendages.
         
/* Read the error stream and output each line */
         line = er.readLine();
         while ( line != null ) {
            msg.out( line );
            line = er.readLine();
         }    

/* Wait for process completion and report result */
         status = run.waitFor();
         if( status != 0 ) {
            msg.out( "ShellRunner executeArray completed with status " + status );
         }

//throw ( new Exception( "test exception" ) );

      } catch( Exception e ) {   
         msg.out( "ShellRunner.executeArray failed" );
         msg.out( e.toString() );
//      e.printStackTrace();

      }

   } catch( Exception e ) {
      msg.out( "ShellRunner failed" );
      msg.out( e.toString() );
   }

/* Deal with the reply
 * If it has the form of a TaskReply as an XML document, it can be returned
 * directly; if not, form a TaskReply containing only a Msg part.
 */
   String[] reply;
   TaskReply taskReply;
   if( msg.contains( "<TaskReply>" ) ) {
      if( ( msg.get( 0 ).equals( "<TaskReply>" ) )
        && ( msg.get( msg.size() - 1 ).equals( "</TaskReply>" ) ) ) {
/* It is a  pure TaskReply - can be returned as is */       
          reply = (String[])msg.toArray(new String[] {});
          
      } else {
/* There is a TaskReply within it - we must add any heading or trailing message
 * message to the msg.
 */
         Msg headMsg = new Msg();
         headMsg.setBuffered( true );
         Msg trailMsg = new Msg();
         trailMsg.setBuffered( true );
         int i;
         
         i = 0;    
         while( i<msg.indexOf( "<TaskReply>" ) ) {
            headMsg.out( (String)msg.get(i) );
            msg.remove( i );
            i++;
         }

         i = msg.indexOf( "</TaskReply>" ) + 1;
         while( i<msg.size() ) {
            trailMsg.out( (String)msg.get(i) );
            msg.remove( i );
            i++;
         }
         
/*  Now get the TaskReply */
         reply = (String[])msg.toArray( new String[] {} );
         try{
            taskReply = TaskReply.readReply( reply );

/*  Add header and trailer to the TaskReply Msg */          
            headMsg.addAll( taskReply.getMsg() );
            headMsg.addAll( trailMsg );
            taskReply.setMsg( headMsg );

         } catch ( Exception e ) {
            taskReply = new TaskReply();
            msg = new Msg();
            msg.setBuffered( true );
            msg.addAll( headMsg );
            msg.out( "Failed to parse embedded TaskReply" );
            msg.addAll( trailMsg );
            msg.out( e.getMessage() );
            taskReply.setMsg( msg );
         }
                 
         reply = taskReply.toXML();
      }    
 
//for(int i=0;i<reply.length;i++)System.out.println(reply[i]);
   } else {
/* No TaskReply in reply - create one containing only a Msg part */
      taskReply = new TaskReply();
      taskReply.setMsg( msg );
      reply = taskReply.toXML();
   }

   return reply;

}

/** Runs <code>Kappa:display</code>. The <code>rundisplay</code> script is run
 *  in a separate process with arguments as specified by the
 *  <code>arguments</code> array. The first argument is used to set the value
 *  of environment variable <code>DISPLAY</code>. The remaining arguments
 *  are passed to the Java JNI version of <code>Kappa:display</code>.
 *  @param arguments the argument array for the rundisplay script
 *  @return an array of Strings representing a {@link TaskReply} in XML form.
 */
public String[] rundisplay( String[] arguments ) {
   return executeArray( "rundisplay", arguments );
}

/** Runs <code>Extractor</code>. The <code>runextractor</code> script is run in
 *  a separate process. This runs the Java JNI version of
 *  <code>Extractor</code>, using the given argument string.
 *  @param arguments the argument array for the rundisplay script
 *  @return an array of Strings representing a {@link TaskReply} in XML form.
 */
public String[] runextractor( String[] arguments ) {
   return executeArray( "runextractor", arguments );
}

/** Runs <code>PISA:pisaplot</code>. The <code>runpisaPlot</code> script is run
 *  in a separate process with arguments as specified by the
 *  <code>arguments</code> array. The first argument is used to set the value
 *  of environment variable <code>DISPLAY</code>. The remaining arguments are
 *  passed to <code>pisaplot</code>.
 *  @param arguments the argument array for the rundisplay script
 *  @return an array of Strings representing a {@link TaskReply} in XML form.
 */
public String[] runpisaPlot( String[] arguments ) {
   return executeArray( "runpisaPlot", arguments );
}

/** Runs a method of Starlink Java application class in a separate process with
 *  arguments as specified by the <code>arguments</code> array.
 *  The first argument specifies the application class (Kappa, for example); the
 *  second argument the class method (stats, for example).
 *  The remaining arguments are passed to the method.
 *  @param arguments the argument string
 *  @return the standard and error output
 */
public String[] runPack( String[] arguments ) {
   return executeArray( "runPack", arguments );
}

/** Convert an <code>Extractor</code> output catalogue to one suitable for input
 *  to <code>pisaplot</code>. The <code>runrotator</code> script is run in a
 *  separate process with arguments as specified by the
 *  <code>arguments</code> array. The script assumes that the THETA_IMAGE
 *  angle is in column 8 (as produced by the  <code>pisa.sex</code>
 *  configuration file), 90degrees is subtracted from the angle.
 *  @param arguments the argument array for the rundisplay script
 *  @return an array of Strings representing a {@link TaskReply} in XML form.
 */
public String[] runrotator( String[] arguments ) {
   return executeArray( "runrotator", arguments );
}

/** Run a script in a separate process. The script is named <code>runxxx</code>
 *  where <code>xxx</code> is the first argument.
 *  The remaining arguments are passed to the script.
 *  Known scripts are:
 *  <ul>
 *  <li> The <code>runxxx</code> scripts mentioned above.
 *  <li> <code>runstats</code> - runs the JNI version of
 *   <code>Kappa:stats</code>
 *  <li> <code>runfindObj/findOff/register/tranNDF/makeMos</code> run the normal Starlink
 *   versions of the relevant CCDPACK application.
 *  <li> <code>runlist</code> - cats the files specified by the
 *   <code>arguments</code> array.
 *  </ul>
 *  @param arguments the argument array for the rundisplay script
 *  @return an array of Strings representing a {@link TaskReply} in XML form.
 */
public String[] run( String[] arguments ) {
   int nargs = arguments.length-1;
   String[] newArguments = new String[ nargs ];
   System.arraycopy( arguments, 1, newArguments, 0, nargs );
   return executeArray( "run" + arguments[0], newArguments );
}
 
/** Executes an <code>echo</code> command. The command is executed in a separate
 *  process with arguments as specified by the <code>message</code> array.
 *  We remove trailing ACCEPT NOPROMPT arguments, assuming they have been added
 *  by a front end expecting to run a Starlink Dtask.
 *  @param arguments the argument array for the rundisplay script
 *  @return an array of Strings representing a {@link TaskReply} in XML form.
 */
public String[] echo( String[] arguments ) {
   int nargs = arguments.length;
   if( nargs>1 ) {
      if( arguments[nargs-1].equals("NOPROMPT") ) {
         arguments[nargs-1] = "";
      }
      if( arguments[nargs-2].equals("ACCEPT") ) {
         arguments[nargs-2] = "";
      }
   }
   return executeArray( "echo", arguments );
}

/** Executes an <code>ls</code> command. The command is executed in a separate
 *  process with arguments as specified by the <code>arguments</code> array.
 *  We remove trailing ACCEPT NOPROMPT arguments, assuming they have been added
 *  by a front end expecting to run a Starlink Dtask.
 *  @param arguments the argument array for the rundisplay script
 *  @return an array of Strings representing a {@link TaskReply} in XML form.
 */
public String[] ls( String[] arguments ) {
   String[] newArguments;
   int nargs = arguments.length;
   if( nargs>1 ) {
      if( arguments[nargs-1].equals("NOPROMPT") ) {
         nargs--;
      }
      if( arguments[nargs-1].equals("ACCEPT") ) {
         nargs--;
      }
   }
   if( nargs < arguments.length ) {
      newArguments = new String[ nargs ];
      System.arraycopy( arguments, 0, newArguments, 0, nargs );
   } else {
      newArguments = arguments;
   }
   return executeArray( "ls", newArguments );
}

/** Get the name of the script to be executed. In most cases this will be the
 *  name of a temporary file in the working directory. This allows the script to
 *  be found using the getResource() method
 *  @param script the name of the required script
 *  @param arguments the script arguments
 *  @return the name of the script to be executed
 */
private String getScript( String script ) throws Exception {
   String useName;

/* If the 'script' name is 'run...', we obtain it from the classpath */
/* This method works OK even if the script is in a jar file */
   if( script.startsWith( "run" ) ) {

/* Add the startask path to the script name */
      String scriptName = "uk/ac/starlink/startask/support/" + script;
/* Get a URL for it */
      URL url =
       classLoader.getResource( scriptName );

      if( url != null ) {
/* If OK, fetch the script to a file in the current directory */
         useName = (new File( url.getPath() )).getName();
         InputStream is = url.openStream();
         FileOutputStream os = new FileOutputStream( useName );
         byte[] b = new byte[512];
         int nbytes;
         while( ( nbytes = is.read( b ) ) >= 0 ) {
            os.write( b, 0, nbytes );
         }  
         is.close();
         os.close();

/* Ensure temporary file is deleted on exit */
         File file = new File( useName );
         file.deleteOnExit();

/* Ensure execute permission on the created file */
         Runtime runt = Runtime.getRuntime();
         Process chmod = runt.exec( "chmod 755 " + useName );
/* Wait for process completion and report result */
         int status = chmod.waitFor();
         if( status != 0 ) {
            throw new Exception(
             "ShellRunner chmod " + useName + " failed" );
         }
         
/* Add ./ to name so . not required on PATH */
         useName = "./" + useName;

      } else {
/* Failed to get URL */
         throw new Exception( "Unable to find script " + scriptName );
      }
      
   } else {
/* Not a 'run...' script - return as is (ls or echo) */
      useName = script;
      
   }
   
   return useName;
   
}   

   
   

}
