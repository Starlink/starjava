package uk.ac.starlink.jpcs;
import java.util.*;

/** Objects of the class Msg are used to handle output from Starlink Java
 *  applications, particularly those using existing ADAM Fortran/C code as
 *  native methods. All output from such code is supposed to be routed through
 *  a single routine, SUBPAR_WRITE.
 *  <p>
 *  Each invocation of a Java PCS task creates an Object of class Msg and the
 *  replacement SUBPAR library with which tasks are built contains a version of
 *  SUBPAR_WRITE which calls the <code>out</code> method in the task's Msg
 *  object.
 *  <p>
 *  An Msg may be 'buffered', in which case messages posted with Msg.out() are
 *  saved until they are 'flushed'; otherwise messages will be written
 *  immediately to System.out.
 */ 
public class Msg extends ArrayList { 

/** Incremental capacity size
 */
private static final int INC_SIZE = 20;
private boolean buffered = true;

/** Constructs an Msg. If the System property uk.ac.starlink.jpcs.msgBuffer is
 *  <code>"false"</code> (ignoring case) when the Msg is constructed, any
 *  output will be displayed immediately; otherwise the Msg will be buffered.
 */
   public Msg() {
      super( INC_SIZE );
      this.setBuffered(
       !(System.getProperty(
        "uk.ac.starlink.jpcs.msgBuffer", "true" ).equalsIgnoreCase("false")) ); 
   }

/** Constructs a buffered Msg containing messages held, one per String, in the
 *  given array of Strings.
 */
   public Msg( String[] array ) {
      super( Arrays.asList( array ) ); 
   }

/** Prints a single message.
 *  @param str the String to be printed
 */
   protected void write( String str ) {
      System.out.println( str );
   }
   
/** Outputs a message intended for the user. If the System property
 *  <code>MsgBuffer</code> was <code>"true"</code> when this Msg was created,
 *  the message will be buffered. Buffered messages may be displayed with the
 *  {@link #flush} method.
 *  @param the message to output.
 */
   public void out( String str ) {
      if ( buffered ) {
         add( (Object)str );
      } else {
         write( str );
      }
   }

/** Add a message to a set of buffered message. If this Msg is not buffered,
 *  <code>head()</code> is equivalent to {@link #out}.
 *  @param the message to output.
 */
   public void head( String str ) {
      if ( buffered ) {
         add( 0, str );
      } else {
         write( str );
      }
   }
   
/** Flushes any buffered messages. The messages are written using {@link #write}
 *  and remove from the Msg. It is not an error to call this method when
 *  there are no buffered messages.
 */
   public void flush() {
      Iterator it = iterator();
      while ( it.hasNext() ) {
         write( (String)it.next() );
         it.remove();
      }
   }
   
/** Lists any buffered messages. The messages are written using {@link #write}
 *  but are not removed from the Msg. It is not an error to call this method
 *  when there are no buffered messages.
 */
   public void list() {
      Iterator it = iterator();
      while ( it.hasNext() ) {
         write( (String)it.next() );
      }
   }

/** Sets message buffering on or off.
 *  @param true if buffering is required.
 */
   public void setBuffered( boolean buffer ) {
      buffered = buffer;
   }
   
}
  

