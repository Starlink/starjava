package uk.ac.starlink.startask;

import uk.ac.starlink.startask.StarTaskRequestGenerator;
import uk.ac.starlink.startask.StarTaskRequest;

import uk.ac.starlink.jpcs.Msg;
import uk.ac.starlink.jpcs.TaskReply;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;

import javax.xml.rpc.ParameterMode;
import javax.xml.namespace.QName;
import java.io.*;
import java.util.*;

/** A client for Starlink web services. 
 *  The required service(s) may be invoked by:
 *  <p align="center"><code>% java uk.ac.starlink.jpcs.RunWS
 *  arguments</code></p>
 *  <p>
 *  If a single argument is given, it is assumed to be the name of a file
 *  containing a sequence of service requests. If more than one argument is
 *  given, the arguments form a single service request.</p>
 *  <p>A service request has the form:</p>
 *  <p align="center"><code>   Service method parameters...</code>
 *  </p>
 *  <p>For example:</p>
 *  <p align="center"><code>   Kappa stats comwest</code></p>
 *  <p>
 *  Parameters '<code>ACCEPT</code> and <code>NOPROMPT</code> are automatically
 *  added to prevent prompts being issued on the server.</p>
 *  <p>
 *  Service request files by convention have extension <code>.lst</code>. Blank
 *  lines and lines starting with <code>#</code>, <code>{</code> and
 *  <code>}</code> are ignored.</p>
 *  <p>
 *  The results from the service are returned as an array of strings. The array
 *  is checked to see if it has the form of a Starlink Java Parameter and
 *  Control Subsystem (jpcs) TaskReply. If it has, the Msg component is
 *  extracted and displayed; if it has not, the array of strings is displayed
 *  verbatim.</p>
 *  <p>
 *  By default service requests are sent to port 8080 on rlspc14.bnsc.rl.ac.uk
 *  but this may be overridden by properties <code>server</code> and
 *  <code>port</code> set in a file named <code>.starserver.properties</code>
 *  Properties from any such file found in the user's working directory will
 *  override any found in the user's home directory.
 */
 
public class RunWS {

static final String DEFAULTSERVER = "rlspc14.bnsc.rl.ac.uk";
static final String DEFAULTPORT = "8080";
static final String PROPFILENAME = ".starserver.properties";

   public static void main(String [] args) {

      boolean DEBUG = false;                  
      String propFileName;
      int propsource;
      String service = null;        
      String command = null;        
      String[] params = null;
      int parslen;        
      String file = "commands.lst";        
      String results[] = null;
      Msg msg;

/* Set up the starservice default properties. */
      propsource = 1;
      Properties props = new Properties();
      props.setProperty( "server", DEFAULTSERVER );
      props.setProperty( "port", DEFAULTPORT );

/* Override with any in the user's home directory */
      String home = System.getProperty( "user.home" );
      String sep = System.getProperty( "file.separator" );
      propFileName = home + sep + PROPFILENAME;
      try {
         FileInputStream propFile = new FileInputStream( propFileName );
         props.load( propFile );
         propsource = 2;
      } catch ( Exception e ) {
//         System.out.println( "No user's properties file found" );
      }
      
/* Then override with any in the local directory */
      propFileName = PROPFILENAME;
      try {
         FileInputStream propFile = new FileInputStream( propFileName );
         props.load( propFile );
         propsource = 3;
      } catch ( Exception e ) {
//         System.out.println( "No local properties file found" );
      }
      
      switch( propsource ) {
         case 1: System.out.println( "Default server used" );break;
         case 2: System.out.println( "User's default server used" );break;
         case 3: System.out.println( "Locally specified server used" );break;
      }
 
 
/* Get the required server and port */
      String server = props.getProperty( "server" );
      int port = Integer.parseInt( props.getProperty("port") );
      System.out.println( "Server: " + server + "; port: " + port );
            
      try{
/* Create the request generator */
         StarTaskRequestGenerator strg = new StarTaskRequestGenerator( args );

/* Get the first request */
         StarTaskRequest str = strg.nextRequest();       
                
/* Continue until no more requests */
         while( str != null ) {
      
/* Get the required service */
            service = str.getPackage();

/* Ignore dummy requests */
            if( service != null ) {
               StarlinkService connection =
                 new StarlinkService( server, port, service );
               command = str.getTask();
               params = str.getParameters();
               results = connection.runTask ( command, params );
//System.out.println("Form Msg from results " + results.length );
//for(int i=0;i<results.length;i++)System.out.println(results[i]);
               if( results.length > 0 ) {
                  if( results[0].startsWith( "<TaskReply" ) ) {
                     msg = TaskReply.readReply( results ).getMsg();
                  } else {
                     msg = new Msg( results );
                  }
                  msg.flush();
               }
            }
         
/* Get the next request */
            str = strg.nextRequest();
         
         } // repeat for this request
            
      } catch ( Exception e ) {
         System.out.println ( "Exception raised: " + e );
      }
   }
}
