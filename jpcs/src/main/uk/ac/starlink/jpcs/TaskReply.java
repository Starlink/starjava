package uk.ac.starlink.jpcs;
import java.util.*;
import java.io.*;
import org.xml.sax.InputSource;

/** Objects of the class TaskReply are used to return output information
 *  from Starlink Java applications using the Starlink Java Parameter and
 *  Control Subsystem (jpcs).
 *  <P>
 *  The transfer is done as an XML document in the form of an array of Strings.
 *  The TaskReply consists of three possible elements contained in a root
 *  element, TaskReply. The three elements are an {@link Msg}, a
 *  {@link ParameterValueList} and a {@link StarlinkStatus}.
 *  <p>
 *  Methods are provided for setting and getting the TaskReply's Msg,
 *  ParameterValueList and StarlinkStatus and for converting to and from the XML
 *  document form.
 */ 
public class TaskReply implements Serializable {

private Msg msg;
private ParameterValueList pvList;
private StarlinkStatus status;
private boolean inMessage = false;
private StringBuffer message = new StringBuffer(80);
private String lastParam;
private Msg out = new Msg( new String[0] );
private Msg errors = new Msg( new String[0] );
//private boolean internalMsg = true;
private boolean good = true;

public TaskReply() {
}

/** Create a TaskReply from an array of Strings. The array should represent
 *  a TaskReply XML document such as that returned by a Starlink {@link Dtask}.
 *  @param the XML document.
 *  @return the created TaskReply.
 */
public static TaskReply readReply( String[] doc ) throws Exception {
   TaskReply result = new TaskReply();
   if( doc != null ) {
      if( doc.length > 0 ) { 
         TaskReplyHandler handler = new TaskReplyHandler();
         try{
            result =
             handler.readReply( new InputSource( new StringArrayReader(doc) ) );
         } catch( Exception e ) {
   System.out.println( "Error reading TaskReply" );
            Msg errMsg = new Msg( doc );
            errMsg.flush();
            throw e;
         }
      }
   }
   return result;
}

/** Set the {@link Msg} component for this TaskReply.
 *  @param the Msg
 */
public void setMsg( Msg givenMsg ) {
   msg = givenMsg;
}

/** Get the {@link Msg} component of this TaskReply.
 *  @return the Msg.
 */
public Msg getMsg() {
   return msg;
}
  
/** Set the {@link ParameterValueList} component for this TaskReply.
 *  @param the ParameterValueList
 */
public void setPVList( ParameterValueList givenPvl ) {
   pvList = givenPvl;
}

/** Get the {@link ParameterValueList} component of this TaskReply.
 *  @return the ParameterValueList.
 */
public ParameterValueList getPVList() {
   return pvList;
}

/** Set the {@link StarlinkStatus} component for this TaskReply.
 *  @param the StarlinkStatus
 */
public void setStatus( StarlinkStatus givenStatus ) {
   status = givenStatus;
}

/** Get the {@link StarlinkStatus} component of this TaskReply.
 *  @return the StarlinkStatus.
 */
public StarlinkStatus getStatus() {
   return status;
}

/** Produce an array of Strings representing this TaskReply as an XML document.
 *  @return the array of Strings
 */
public String[] toXML() {
   ArrayList al = new ArrayList();

/* The XML root element */
   al.add( "<TaskReply>" );

/* Add the Msg content */
   Iterator it = msg.iterator();
   while( it.hasNext() ) {
      al.add( "<message><![CDATA[" + it.next() + "]]></message>" );
   }

/* Add the ParameterValueList */
   if( pvList != null ) {
      al.addAll( Arrays.asList( pvList.toXML() ) );
   }

/* Terminate the TaskReply element */
   al.add( "</TaskReply>" );

/* Return as an array of String */
   return (String[])al.toArray( new String[0] ); 
}

}

