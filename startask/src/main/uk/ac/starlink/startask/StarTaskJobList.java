package uk.ac.starlink.startask;

import java.util.ArrayList;
import java.util.Iterator;

/** A class for handling lists of StarTaskJobs
*/
   class StarTaskJobList extends ArrayList {
   
      StarTaskJobList() {
         super();
      }
   
/** Get a list of the task servers used in this list.
*/
      String[] getServers() {
         ArrayList servers = new ArrayList();
         Iterator it = this.iterator();
         while( it.hasNext() ) {
            StarTaskJob stj = (StarTaskJob)(it.next());
            String server = stj.getAcceptor();
            if( ( server != null ) && !servers.contains( server ) ) {
               servers.add( server );
            }
         }
         return (String[])servers.toArray( new String[] {} );
      }
   
/** Get a list of the jobs in this list handled by the given server.
  * @param server the server name
  * @return A list of StarTaskJobs.
*/
      StarTaskJobList getServerJobs( String server ) {
         StarTaskJobList jobs = new StarTaskJobList();
         Iterator it = this.iterator();
         while( it.hasNext() ) {
            StarTaskJob stj = (StarTaskJob)(it.next());
            String accptr = stj.getAcceptor();
            if( accptr != null ) {
               if( accptr.equals( server ) ) {
                  jobs.add( stj );
               }
            }
         }
         return jobs;
      }
   
/** Get a list of the jobs in this list which are awaiting a server.
  * @return A list of waiting jobs
*/
      StarTaskJobList getWaitingJobs() {
         StarTaskJobList jobs = new StarTaskJobList();
         Iterator it = this.iterator();
         while( it.hasNext() ) {
            StarTaskJob stj = (StarTaskJob)(it.next());
            if( stj.getAcceptor() == null ) {
               jobs.add( stj );
            }
         }
         return jobs;
      }
   
/** Get a list of incomplete jobs in this list.
 *  This includes those awaiting a server and those accepted but not complete.
 *  @return A list of incomplete jobs
*/
      StarTaskJobList getIncompleteJobs() {
         StarTaskJobList jobs = new StarTaskJobList();
         Iterator it = this.iterator();
         while( it.hasNext() ) {
            StarTaskJob stj = (StarTaskJob)(it.next());
            if( !stj.isDone() ) {
               jobs.add( stj );
            }
         }
         return jobs;
   }
   
/** Get a list of jobs in this completed.
*/
      StarTaskJobList getCompletedJobs() {
         StarTaskJobList jobs = new StarTaskJobList();
         Iterator it = this.iterator();
         while( it.hasNext() ) {
            StarTaskJob stj = (StarTaskJob)(it.next());
            if( stj.isDone() ) {
               jobs.add( stj );
            }
         }
         return jobs;
      }
      
/** Display all jobs in the list
*/
      void display() {
         Iterator it = this.iterator();
         while( it.hasNext() ) {
            StarTaskJob stj = (StarTaskJob)(it.next());
            stj.display();
         }
         return;
      }

/** See if this list contains a given StarTaskJob
  */
      boolean contains( StarTaskJob job ) {
         
         boolean reply = false;
         Iterator it = this.iterator();
         while( it.hasNext() ) {
            StarTaskJob stj = (StarTaskJob)(it.next());
            if( job.equals( stj ) ) {
               reply = true;
               break;
            }
         }
         
         return reply;

      }

/** Get job statistics for this list.
*   @return an array containing the numbers of Completed, waiting and
*   in-progress jobs for this job list.
*/ 
      double[] getStats() {
      
         double numbers[] = new double[3];
    
         StarTaskJobList incJobs = this.getIncompleteJobs();
         numbers[0] = this.size() - incJobs.size(); 
         numbers[2] = incJobs.getWaitingJobs().size();
         numbers[1] = incJobs.size() - numbers[2];
         
         return numbers;
         
      }
         
/** Get complete job statistics for this list.
*   The number of completed, in-progress and waiting jobs for this list and for
*   each server referenced by the list is returned.
*   The first subscript indicates the status 0;completed 1;in-progress 2;waiting
*   and the second subscript the server (0 is all jobs) 
*   @return the statistics array.
*/         
      double[][] getCompleteStats() {
         String[] servers = this.getServers();    
         int nServers = servers.length;
         double[][] stats = new double[3][1+nServers];
         double[] jobStats = getStats();
         stats[0][0] = jobStats[0];
         stats[1][0] = jobStats[1];
         stats[2][0] = jobStats[2];
         for( int i=0; i<nServers; i++ ) {
            StarTaskJobList jobList = this.getServerJobs( servers[i] );
            jobStats = jobList.getStats();
            stats[0][1+i] = jobStats[0];
            stats[1][1+i] = jobStats[1];
            stats[2][1+i] = jobStats[2];
         }
    
         return stats;
      }
               
   }         
         
         
         
                 
      
