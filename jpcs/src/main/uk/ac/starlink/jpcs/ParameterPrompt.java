package uk.ac.starlink.jpcs;

import javax.swing.*;        
import java.awt.*;
import java.awt.event.*;

/** Basic prompting for {@link Parameter} values.
 */
class ParameterPrompt extends ParameterPromptAdapter {

/** Construct a ParameterPrompt object
 */
ParameterPrompt() {
}

/** Construct a ParameterPrompt object and associate a {@link Parameter} with it
 *  @param p the Parameter to be associated.
 */
ParameterPrompt( Parameter p ) {
   setParameter( p );
}

/** Gets a {@link ParameterReply} in response to a prompt for a value for the
 *  associated {@link Parameter}. The prompt string is constructed using the
 *  makePromptString() method and displayed in a {@link JOptionPane} - it may
 *  contain a suggested value for the Parameter.
 *  @return a ParameterReply constructed from the user's input or, if
 *  that is an empty String, from the suggested value if there is one. If the
 *  'cancel' button on the JOptionPane, the 'abort' reply is returned.
 */
public ParameterReply getReply() throws Exception {
   Parameter p = getParameter();
   String reply = null; 
   String suggStr;
   Object suggested = p.getSuggestedValue();
   if ( suggested != null ) {
      suggStr = suggested.toString();
   } else {
      suggStr = null;
   }
   
   reply = JOptionPane.showInputDialog( makePromptString( suggStr ) );
//System.out.println("ParameterReply: reply is " + reply );
   if ( reply == null ) {
/* Cancel button pressed */
      reply = "!!";
      
   } else if ( reply.length() == 0 ) {
/* If zero-length reply, use suggested value if there is one:
 * otherwise return the zero-length string
 */
      if ( suggStr != null ) {
//System.out.println("ParameterReply: set reply to " + suggStr );
         reply = suggStr;
      }
   }
   
/*   reply = JOptionPane.showInputDialog( makePromptString(),
                 getParameter().getDefault().toString() );*/

   return new ParameterReply( reply ); 
}
}
