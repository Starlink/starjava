package uk.ac.starlink.jpcs;

/** Requirements for a basic class which will provide a response to a request
 *  for a {@link Parameter} value. The assumption is that the request is in the
 *  form of a displayed prompt string and the response is a
 *  {@link ParameterReply}. The prompt string may contain elements separated by
 *  separating strings. Some of the elements may be specific to the Parameter
 *  being requested. For more details of the assumption see
 *  {@link ParameterPromptAdapter}.
 */
interface ParameterPrompter {

/** Gets a reply from the ParameterPrompter
 *  @return the reply
 */
ParameterReply getReply() throws Exception;

/** Constructs the prompt string for this ParameterPrompter.
 *  @return the prompt string.
 */
String makePromptString( String suggested );

/** Sets the associated {@link Parameter} for this ParameterPrompter.
 *  @param the associated Parameter.
 */
void setParameter( Parameter p );

/** Sets the keyword separator for this ParameterPrompter.
 *  @param the required keyword separator.
 */
void setKeySeparator( String sep );

/** Sets the default separator for this ParameterPrompter.
 *  @param the required default separator.
 */
void setDefaultSeparator( String sep );

/** Sets the final 'invite' string for this ParameterPrompter.
 *  @param the required invite string.
 */
void setInvite( String inv );

} 
