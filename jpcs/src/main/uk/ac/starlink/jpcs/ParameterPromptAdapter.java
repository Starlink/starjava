package uk.ac.starlink.jpcs;

import java.io.*;

/** This abstract class provides default methods for subclasses which implement
 *  the {@link ParameterPrompter} interface.
 *  The assumption is that a prompt string is displayed having the following
 *  form:
 *  <p><code>key|k|string|d|suggested|d|invite</code></p>
 *  where:
 *  <ul>
 *  <li><code>key</code> is the {@link Parameter}'s keyword. 
 *  <li><code>|k|</code> is the keyword separator, " - " by default.
 *  <li><code>string</code> is the Parameter's prompt string.
 *  <li><code>|d|</code> is the 'default separator', "/" by default. 
 *  <li><code>suggested</code> is a suggested value for the Parameter.
 *  <li><code>invite</code> is a final string, " > " by default.
 *  </ul>
 *  Methods exist for setting the separators and for constructing the prompt
 *  string.
 */
abstract class ParameterPromptAdapter
 implements ParameterPrompter,Serializable {

Parameter p;
String keysep=" - ";
String defsep="/";
String invite=" > ";

/** Gets a {@link ParameterReply}, normally by issuing a prompt and constructing
 *  the ParameterReply from the user's response
 */
public abstract ParameterReply getReply() throws Exception;

/** Tells this {@link ParameterPrompter} which {@link Parameter} it is
    associated with.
    @param par the Parameter.  
 */
public void setParameter( Parameter par ) {
   p = par;
}

/** Gets the {@link Parameter} with which this {@link ParameterPrompter} is
 *  associated.
 *  @return the associated Parameter
 */
public Parameter getParameter() {
   return p;
}

/** Sets the keyword separator for the prompt string.
 *  @param sep the required keyword separator
 */ 
public void setKeySeparator( String sep ) {
   keysep = sep;
}

/** Sets the 'default separator' for the prompt string.
 *  @param the required 'default separator'
 */
public void setDefaultSeparator( String sep ) {
   keysep = sep;
}

/** Sets the final string for the prompt string
 *  @param inv the required final string.
 */
public void setInvite( String inv ) {
   invite = inv;
}

/** Constructs the prompt string. If the prompt is as a result of a previous bad
 *  value, the prompt string will be preceded by the associated error message.
 *  @param suggested the suggested value as a String.
 */
public String makePromptString( String suggested ) {
   StringBuffer strb = new StringBuffer();
   String keyword;
   String promptString;
   Object defalt;
   
   String errMessage = p.getMessage();
   if ( errMessage != null ) {
      strb.append( errMessage + "\n" );
      p.setMessage( null );
   }
   
   keyword = p.getKeyword();
   if ( keyword != null ) {
      strb.append( keyword );
      strb.append( keysep );
   }
   promptString = p.getPromptString();
   if ( promptString != null ) {
      strb.append( promptString );
   }
   
   if ( suggested != null ) {
//System.out.println( "suggested value is not null" );
      strb.append( defsep );
      strb.append( suggested );
      strb.append( defsep );
   }
   
   strb.append( invite );

   return strb.toString();
}

} 
