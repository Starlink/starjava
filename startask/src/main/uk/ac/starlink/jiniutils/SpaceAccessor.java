package uk.ac.starlink.jiniutils;

         /* ----------------------------------------- */
         /*                                           */
         /*               SPACE ACCESSOR              */
         /*                                           */
         /* ----------------------------------------- */

         // Jini core packages

         import net.jini.core.discovery.LookupLocator;
         import net.jini.core.lookup.*;
         import net.jini.core.entry.Entry;

         // Jini extension package

         import net.jini.space.JavaSpace;
         import net.jini.lookup.entry.*;

         // Java core packages

         import java.io.*;
         import java.rmi.*;
         import java.net.*;
         import java.util.*;


         public class SpaceAccessor {

             /* ------ FIELDS ------ */
          
             static String spaceName = null;
             static String jiniURL   = null;
             Properties props;
           
             static final long MAX_LOOKUP_WAIT = 2000L;
             
             JavaSpace space;
     /* ------ CONSTRUCTORS ------ */

             /* GET SPACE */
             
             public SpaceAccessor(String propFileName) {
                 LookupLocator locator = null;
                 ServiceRegistrar registrar = null;
                 
                 // Security manager
                 
                 try {
                     System.setSecurityManager(new RMISecurityManager());
                     }
                 catch (Exception e) {
                     e.printStackTrace();
                     }

                 // Get properties from property file
                 
                 initProperties(propFileName);
                 
                 
                 try {
                     // Get lookup service locator at "jini://hostname"
                     // use default port and register of the locator
                     locator = new LookupLocator(jiniURL);
                     registrar = locator.getRegistrar();

                     // Space name provided in property file
                     ServiceTemplate template;
                     if( spaceName != null ) {
                         // Specify the service requirement, array (length 1) of 
                         // Entry interfaces (such as the Name interface)
                         Entry [] attr = new Entry[1];
                         attr[0] = new Name(spaceName);
                         template = new ServiceTemplate(null,null,attr);
 
                     } else {
                         // Specify the service requirement, array (length 1) of 
                         // instances of Class
                         Class [] types = new Class[] {JavaSpace.class};
                         template = new ServiceTemplate (null, types, null);
                     }
                     
                     // Get space, 10 attempts!
                     for (int i=0; i < 10; i++) {
                         Object obj = registrar.lookup (template);                         

                         if (obj instanceof JavaSpace) {
                             space = (JavaSpace) obj;
                             break;
                             }
                         System.out.println("BasicService. JavaSpace not " +
                                         "available. Trying again...");
                         Thread.sleep(MAX_LOOKUP_WAIT);
                         }
                     }
                 catch(Exception e) {
                     e.printStackTrace();
                     }   
                 }
                 
             /* INITIALISE PROPERTIES. Read property file and assign value to 
             spaceName and jiniURL fields */

             protected void initProperties(String propFileName) {
             
                 // Create instance of Class Properties
                 props = new Properties(System.getProperties());

                 // Try to load property list
                 try {
                     props.load( new BufferedInputStream(new 
                                         FileInputStream(propFileName)));
                     }
                 catch(IOException ex) {
                     ex.printStackTrace();
                     System.out.println( "Exception in SpaceAccessor" );
                     System.exit(-3);
                     }   
             
                 // Output property list (can be ommitted - testing only)
                 
                 System.out.println("jiniURL   = " + 
                                 props.getProperty("jiniURL"));
                 System.out.println("spaceName = " + 
                                 props.getProperty("spaceName"));
        // Assign values to fields
             
                 jiniURL = props.getProperty("jiniURL");
                 spaceName = props.getProperty("spaceName");
                 }       
             
             /* GET SPACE */
              
             public JavaSpace getSpace() {
                 return space;
                 }
             }
