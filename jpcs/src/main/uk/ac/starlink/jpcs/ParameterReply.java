package uk.ac.starlink.jpcs;

/** This class is used to hold a String resulting from a prompt for a
 *  {@link Parameter} value. It provides methods to test the String for
 *   specific values indicating status values or special actions
 */
class ParameterReply {

String reply;

/** Constructs a ParameterReply from the given String
 *  @param str the reply string.
 */
ParameterReply( String str) {
   reply = str;
}

/** Tests if this ParameterReply is the 'null' response.
 *  @return <code>true</code> if the reply string is "!"; <code>false</code>
 *  otherwise.
 */
boolean isNull() {
   return reply.equals("!");
}

/** Tests if this ParameterReply is the 'abort' response.
 *  @return <code>true</code> if the reply string is "!!"; <code>false</code>
 *  otherwise.
 */
boolean isAbort() {
   return reply.equals("!!");
}

/** Tests if this ParameterReply is the 'help' response.
 *  @return <code>true</code> if the reply string is "?"; <code>false</code>
 *  otherwise.
 */
boolean isHelp() {
   return reply.equals("?");
}

/** Tests if this ParameterReply is the 'fullhelp' response.
 *  @return <code>true</code> if the reply string is "??"; <code>false</code>
 *  otherwise.
 */
boolean isFullHelp() {
   return reply.equals("??");
}

/** Gets this ParameterReply as a String
 *  @return the String from which this ParameterReply was constructed.
 */  
public String toString() {
   return reply;
}

}
