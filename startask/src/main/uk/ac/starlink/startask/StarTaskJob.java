package uk.ac.starlink.startask;

/** A class to keep track of the progress of a {@link StarTaskRequest}.
*/
   class StarTaskJob {
   
/* The request */
      StarTaskRequest request;
/* The name of the server issuing a StarTaskAcceptance for this request */
      String acceptor = null;
/* true if the request is completed; false otherwise. */
      boolean reqDone = false;
   
/** Construct a StarTaskJob for a given {@link StarTaskRequest}.
  * the acceptor will be set null and the 'completed' flag <code>false</code>.  
  * @param req the StarTaskRequest.
  */
      StarTaskJob( StarTaskRequest req ) {
         request = req;
      }
      
/** Get the {@link StarTaskRequest}
*/
      StarTaskRequest getRequest() {
         return request;
      }
   
/** Set the acceptor (server) for this StarTaskJob.
  * If the server for this is not null, it cannot be reset to anything other
  * than null. 
  * @param the accepting server name or null.
  * @return true if the server was set (or reset to null); false otherwise.
  */ 
      boolean setAcceptor( String server ) {
         boolean reply;
         if( ( acceptor == null ) || ( server == null ) ) {
            acceptor = server;
            reply = true;
         } else {
            reply = false;
         }
         return reply; 
      }
   
   
/** Get the acceptor (server) for this StarTaskJob
  * @return the accepting server name; or null is the request has not yet been
  * accepted.
  */ 
      String getAcceptor() {
         return acceptor;
      }
   
/** Set the 'completed' flag for this StarTaskJob
  * @param done the required state: <code>true</code> if the job is completed;
  * <code>false</code> otherwise.
  */
      void setDone( boolean done ) {
         reqDone = done;
      }
   
/** Test if the job is completed
  * @return <code>true</code> if the job is completed; <code>false</code>
  * otherwise.
  */
      boolean isDone() {
         return reqDone;
      }
      
/** Display the job.
  * A line is printed consisting of the task name, the request id sequence
  * number the status of the job and the server (if any) involved
  */
      void display() {
         String server;
         String status;
         if( acceptor != null ) {
            server = acceptor;
            if( isDone() ) {
               status = " completed by ";
            } else {
               status = " being run on ";
            }
         } else {
            server = " ";
            status = " awaiting server";
         }

         StarTaskRequest str = getRequest();
         System.out.println(
          "  " + str.getTask() + " " + str.getId().getId() + status + server );
      }
      
/** See if this StarTaskJob is the same as another.
  * Two StarTaskJobs are the same if they refer to {@link StarTaskRequest}s
  * with equal {@link StarTaskRequestId}s.
  */
      public boolean equals( Object obj ) {
     
         boolean reply = false;
         if( ( obj != null ) && obj instanceof StarTaskJob ) {
            if( this.getRequest().getId().equals(
             ((StarTaskJob)obj).getRequest().getId() ) ) {
               reply = true;
            }
         }
         
         return reply;
         
      }
   }
