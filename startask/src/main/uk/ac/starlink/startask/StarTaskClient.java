/* This is a client for a {@link StarTask} server
 * 
 * Copyright 2003 CCLRC. 
 * 
 */

package uk.ac.starlink.startask;

import java.io.*;
import java.util.StringTokenizer;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationProvider;
import net.jini.config.NoSuchEntryException;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.discovery.LookupDiscovery;
import net.jini.lookup.ServiceDiscoveryManager;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import uk.ac.starlink.jpcs.TaskReply;
import uk.ac.starlink.jpcs.Msg;

/**
 * Defines an application that makes calls to a remote StarTask server.
 *
 * The first argument is the name of the configuration file.
 *
 * The application uses the following configuration entries, with component
 * uk.ac.starlink.startask.StarTaskClient:
 * <DL>
 * <DT>
 * loginContext
 * </DT>
 * <DD>
 *   type: LoginContext<BR>
 *   default: null
 * <P>
 *   If non-null, specifies the JAAS login context to use for performing a JAAS
 *   login and supplying the Subject to use when running the client. If null,
 *   no JAAS login is performed.
 * </DD>
 * <DT>
 * preparer
 * </DT>
 * <DD>
 *   type: ProxyPreparer<BR>
 *   default: new BasicProxyPreparer()<BR>
 *   Proxy preparer for the server proxy
 * </DD>
 * <DT>
 * serviceDiscovery
 * </DT>
 * <DD>
 *   type: ServiceDiscoveryManager
 *   default: new ServiceDiscoveryManager(
 *                new LookupDiscovery(new String[] { "" }, config),
 *                null, config)
 *   Object used for discovering a server that implement StarTask.
 * </DD>
 * </DL>
 *
 * If there are more that two arguments, the remaining arguments are passed to
 * the server found by the Lookup. They are expected to be:
 * <UL>
 * <LI>The Starlink service e.g. Kappa</LI>
 * <LI>The service task e.g. stats</LI>
 * <LI>The parameter values for the task</LI>
 * </UL>
 * If there are two arguments, the second should be the name of a file
 * containing the commands of the form 'Server task params...' e.g.
 * 'Kappa stats comwest'
 */
public class StarTaskClient {

    static boolean DEBUG = false;

    /**
     * Starts an application that makes calls to a remote StarTask implementation.
     */
    public static void main(String[] args) throws Exception {
	/*
	 * The configuration contains information about constraints to apply to
	 * the server proxy.
	 */
      if( args.length == 0 ) {
         throw new Exception(
          "No config file supplied as argument for StarTaskClient" );
      }
       
      final Configuration config =
          ConfigurationProvider.getInstance( new String[] { args[0] } );
      final String pars[] = args;

	LoginContext loginContext = (LoginContext) config.getEntry(
	    "uk.ac.starlink.startask.StarTaskClient", "loginContext",
	    LoginContext.class, null);
	if (loginContext == null) {
if( DEBUG ) System.out.println( "Null loginContext" );
	    mainAsSubject(config, pars);
	} else {
if( DEBUG ) System.out.println( "login required" );
	    loginContext.login();
	    Subject.doAsPrivileged(
		loginContext.getSubject(),
		new PrivilegedExceptionAction() {
		    public Object run() throws Exception {
			mainAsSubject(config, pars);
			return null;
		    }
		},
		null);
	}
    }

    /**
     * Performs the main operations of the application with the specified
     * configuration and assuming that the appropriate subject is in effect.
     */
    static void mainAsSubject(Configuration config, String[] args)
     throws Exception {
     
      String file = "commands.lst";
      String service;
      String task;
      String[] params;
      int parslen;

	/* Get the service discovery manager, for discovering other services */
	ServiceDiscoveryManager serviceDiscovery;
	try {
	    serviceDiscovery = (ServiceDiscoveryManager) config.getEntry(
		"uk.ac.starlink.startask.StarTaskClient", "serviceDiscovery",
		ServiceDiscoveryManager.class);
if ( DEBUG ) System.out.println( "Got ServiceDiscoveryManager " );
	} catch (NoSuchEntryException e) {
	    /* Default to search in the public group */
if ( DEBUG ) System.out.println( "Searching the public group" );
	    serviceDiscovery = new ServiceDiscoveryManager(
		new LookupDiscovery(new String[] { "" }, config),
		null, config);
	}

/* Now check the arguments to determine the commands to be sent */       
      if ( args != null ) {
         try {
            if ( args.length > 2 ) {
                     
/* Use the command given on the RunWS command line */
               service = args[1];
               task = args[2];
                
               parslen = args.length - 1;
               params = new String[ parslen ];
               if ( args.length > 3 ) {
                  System.arraycopy( args, 3, params, 0, parslen-2 );
               }
/* Add ACCEPT NOPROMPT to prevent prompting on the server */
               params[parslen-2] = "ACCEPT";
               params[parslen-1] = "NOPROMPT";
                
/* Look up the remote server */
               ServiceItem serviceItem = serviceDiscovery.lookup(
	          new ServiceTemplate(null, new Class[] { StarTask.class }, null),
	          null, Long.MAX_VALUE);
if ( DEBUG ) System.out.println( "Got serviceItem" );
	         StarTask server = (StarTask) serviceItem.service;
if ( DEBUG ) System.out.println( "Got server" );

/* Prepare the server proxy */
               ProxyPreparer preparer = (ProxyPreparer) config.getEntry(
                 "uk.ac.starlink.startask.StarTaskClient",
                 "preparer", ProxyPreparer.class, new BasicProxyPreparer() );
if ( DEBUG ) System.out.println( "Got preparer" + preparer );

               server = (StarTask) preparer.prepareProxy(server);
if ( DEBUG ) System.out.println( "Got server proxy" + server );

/* Use the server */
               TaskReply tr = server.shellRunTask( service, task, params );
               Msg msg = tr.getMsg();
               if( msg != null ) {
                  msg.flush();
               }

            } else {
/* Get the commands file - commands.lst by default */
               if( args.length == 2 ) file = args[1];
               LineReader in =
                 new LineReader ( new FileReader( file ) );
               String s = in.readNonCommentLine( );
               while( s != null ) {
                  if ( DEBUG ) System.out.println( "New line: " + s );
                 
                  StringTokenizer st = new StringTokenizer( s );
                  int n = st.countTokens();
                  if ( n > 1 ) {
                     service = st.nextToken();
                     task = st.nextToken();
                     parslen = n;
                     params = new String[parslen];
                     for( int i=0;i<parslen-2;i++) {
                        params[i] = st.nextToken();
                     }
/* Add ACCEPT NOPROMPT to prevent prompting on the server */
                     params[parslen-2] = "ACCEPT";
                     params[parslen-1] = "NOPROMPT";
                     
/* Look up the remote server */
                     ServiceItem serviceItem = serviceDiscovery.lookup(
                       new ServiceTemplate(
                         null, new Class[] { StarTask.class }, null),
                         null, Long.MAX_VALUE);
                     StarTask server = (StarTask) serviceItem.service;

/* Prepare the server proxy */
                     ProxyPreparer preparer = (ProxyPreparer) config.getEntry(
                       "uk.ac.starlink.startask.StarTaskClient",
                       "preparer", ProxyPreparer.class,
                       new BasicProxyPreparer());
                     server = (StarTask) preparer.prepareProxy(server);

/* Use the server */
                     System.out.print( "\nRunning: " + service + ":" + task );
                     int i = 0;
                     while( i<params.length ) {
                        System.out.print( " " + params[i++] );
                     }
                     System.out.println();
                     
                     TaskReply tr =
                      server.shellRunTask( service, task, params );
	               Msg msg = tr.getMsg();
                     if( msg != null ) {
                        msg.flush();
                     }

                  } else if ( n == 1 && s.equals("end") ) {
                     break;
                        
                  } else {
                     throw new Exception(
                      "Command read from file has too few tokens" );
                  }
                  s = in.readNonCommentLine( );
               };

               in.close();

            }
            
         } catch ( Exception e ) {
            System.out.println ( "Exception raised: " + e );
         }
      }        

/* Exit to close any thread created by the callback handler's GUI */
if ( DEBUG ) System.out.println( "Calling System.exit()" );
	System.exit(0);
      
    }
}
