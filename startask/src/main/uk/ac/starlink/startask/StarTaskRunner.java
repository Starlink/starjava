     /*                 StarTaskRunner                   */
     /*  Looks in the JavaSpace for StarTaskRequests.   */
     /*  When one is found it is serviced using the      */
     /*  shellRunTask method                             */
     /* ------------------------------------------------ */

     package uk.ac.starlink.startask;
     
     // JavaSpacesUtil package
     import uk.ac.starlink.jiniutils.SpaceAccessor;
     
     // Java net classes
     import java.net.InetAddress;

     // Jini core packages
     import net.jini.core.lease.*;

     // Jini extension package
     import net.jini.space.JavaSpace;
     
     // Starlink PCS
     import uk.ac.starlink.jpcs.*;     

/** A server for {@link StarTaskRequest}s.
*/
     public class StarTaskRunner {

/** Services {@link StarTaskRequest}s from a javaSpace.
*   <p>
*   The JavaSpace to be used is specified by a property file specified by the 
*   System property "uk.ac.starlink.startask.spacePropertyFile" (default
*   <code>star_space.prop</code>).
*   <p>
*   StarTaskRunners look in the JavaSpace and take out the first
*   request they find and execute its shellRun method. This will generally
*   result in a Starlink application being executed in a separate Unix shell.
*   <p> 
*   As the request object is taken, a
*   {@link uk.ac.starlink.startask.StarTaskAcceptance} entry is placed
*   in the space, where it can be read by the client or by independant monitors.
*   The acceptance entry contains information about the request and the server
*   which accepted it.
*   <p>
*   When the StarTaskRunner has finished obeying the StarTaskRequest, it creates
*   a {@link uk.ac.starlink.startask.StarTaskReply} containing the results and
*   places it into the space. At the same time it removes the
*   StarTaskAcceptance entry and then looks for another StarTaskRequest.
*   <p>
*   StarTaskRequests and StarTaskReplys are identified by a
*   {@link uk.ac.starlink.startask.StarTaskRequestId}.  
*/
         public static void main(String[] args) {
         
             System.out.println("START");
             
             Lease acceptLease = null;
          
/* Get the space properties filename */
             String spacePropertyFile =
              System.getProperty( "uk.ac.starlink.startask.spacePropertyFile" );
             if( ( spacePropertyFile == null )
              || ( spacePropertyFile.length() == 0 ) ) {
                spacePropertyFile = "star_space.prop";
            } 
             // try block
             try {

/* Get localhost address */
                 String localHost = InetAddress.getLocalHost().getHostName();
                                  

                 // Get JavaSpace     
System.out.println("Get JavaSpace");
                 SpaceAccessor newSpaceAccessor = new SpaceAccessor(
                    spacePropertyFile );
                 JavaSpace space = newSpaceAccessor.getSpace();
                   
                 // Create a template StarTaskRequest
                 StarTaskRequest reqTemplate = new StarTaskRequest();

                 // Create a template StarTaskDataPack
                 StarTaskDataPack packTemplate = new StarTaskDataPack();
                 
/* Continually look for StarTaskRequests in the Space */
                 while( true ) {
/* Read StarTaskRequest and run it */  
                    StarTaskRequest request =
                      (StarTaskRequest)space.takeIfExists(
                       reqTemplate, null ,Long.MAX_VALUE );
                    if( request != null ) {
/* Display the StarTaskRequest */
                       System.out.println( "\nStarTaskRequest is:" );
                       request.display();
                 
/* Construct a StarTaskAcceptance Object */
                       StarTaskAcceptance acceptance =
                         new StarTaskAcceptance( localHost, request );
System.out.println( "ACCEPTANCE by " + acceptance.starServer );
System.out.println( "  of " +
 acceptance.getId().toString() +" " +
 acceptance.starRequest.reqPackage + ":" + 
 acceptance.starRequest.reqTask );
/* and write it to space */
                       acceptLease =
                        space.write( acceptance, null, 60000 );

/* Get data specified by any associated StarTaskDataPack */
                       packTemplate.setId( request.getId() );
                       StarTaskDataPack dataPack =
                         (StarTaskDataPack)space.takeIfExists(
                           packTemplate, null ,Long.MAX_VALUE );
                       if( dataPack != null ) {
System.out.println( "Making local data" );
                          dataPack.makeLocal();
System.out.println( "Made local data" );
                       } 
                     
/* Now satisfy the request */
                       TaskReply tr = request.shellRunTask();

/* Dispose of any dataPack files */
                       if( dataPack != null ) {
                          StarTaskDataPack returnPack =
                           (StarTaskDataPack)dataPack.dispose();
/* and write any return data pack into space */
                          if( returnPack != null ) {
                             returnPack.setId( request.getId() );
System.out.println( "Write return datapack" );
                             space.write( returnPack, null, Lease.FOREVER );
                          }
                       }
                       
/* Construct a StarTaskReply and send into space */
System.out.println( "Construct reply" );
                       StarTaskReply reply =
                        new StarTaskReply( request.getId(), tr );
                        
System.out.println( "Write reply" );
                       space.write( reply, null, Lease.FOREVER );
                       
/* Take back the StarTaskAcceptance */
                       acceptance =
                        (StarTaskAcceptance)space.take( acceptance, null, 1000 );
                       
                       System.out.println( "\nTask done" );
                   
                    } else {
                       Thread.sleep(500);
                    }
                     
                 }
                    
            
             // Catch block
             } catch(Exception e) {
                 e.printStackTrace();
             }
             
             System.out.println("END");
             System.exit(0);
             }
         }         
