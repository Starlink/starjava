package uk.ac.starlink.plastic;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.ListModel;

/**
 * Provides facilities for looking at messages which pass through the
 * PLASTIC system and checking whether they obey all the constraints
 * of legality.
 *
 * <p>The first main thing to check is whether
 * the messages are formally legal as PLASTIC messages
 * (a common error is using an argument Java type which is 
 * disallowed since it can't be transmitted using XML-RPC).
 *
 * <p>The second things is checking whether the message matches the
 * usage which is supposed to be associated with its particular message ID. 
 * This can only be done if we know what that usage is supposed to be.
 * The {@link MessageDefinition} class contains descriptions of a list
 * of common messages, but it can't be exhaustive since PLASTIC is
 * extensible by design.  In case of an unrecognised message this 
 * validator will note it the first time it crops up, and attempt to
 * warn in case of later appearances which seem to have different or
 * incompatible usages.  This can't be done in a rigorous way, but it
 * gives it its best shot.  This part is a bit of a work in progress.
 *
 * @author   Mark Taylor
 * @since    19 Jul 2006
 */
class MessageValidator {

    private final Map messageMap_;
    private ListModel appsList_;

    /**
     * Constructor. 
     */
    public MessageValidator() {

        /* Set up a map of known messages, keyed by ID (URI). */
        messageMap_ = new HashMap();
        MessageDefinition[] defs = MessageDefinition.getKnownMessages();
        for ( int i = 0; i < defs.length; i++ ) {
            MessageDefinition def = defs[ i ];
            URI id = def.getId();
            assert ! messageMap_.containsKey( id );
            messageMap_.put( id, def );
            assert messageMap_.containsKey( id );
        }
    }

    /**
     * Returns the MessageDefinition corresponding to a given ID.
     * This may be one of the core messages 
     * ({@link MessageDefinition#getKnownMessages}) or one whose
     * existence and usage this validator has inferred by examining
     * messages it's seen during its lifetime to date.
     *
     * @param  msgId  message ID
     * @return  corresponding message definition
     */
    public MessageDefinition getDefinition( URI msgId ) {
        return (MessageDefinition) messageMap_.get( msgId );
    }

    /**
     * Performs validation on an observed request and returns an array 
     * of strings each of which contains some text describing a worrying
     * thing about the message.  The array will be empty if the message
     * looks legal, decent honest and truthful.  A non-empty array does
     * not necessarily mean the message was strictly illegal though.
     *
     * @param   sender   sender ID
     * @param   message  message ID
     * @param   argList  list of argument objects
     * @return  array of warning messages
     */
    public String[] validateRequest( URI sender, URI message, List argList ) {
        List warningList = new ArrayList();

        /* Check sender is registered. */
        if ( appsList_ != null ) {
            ApplicationItem app =
                getRegisteredApplication( appsList_, sender );
            if ( app == null ) {
                warningList.add( "Sent from unregistered application: " + 
                                 sender );
            }
        }

        /* Check message ID is known. */
        if ( ! messageMap_.containsKey( message ) ) {
            warningList.add( "First sighting of unknown message ID: "
                           + message );
            try {
                MessageDefinition def =
                    inferMessageDefinition( message, argList, null );
                messageMap_.put( message, def );
            }
            catch ( ValueTypeException e ) {
                warningList.add( e.getMessage() );
                ValueType[] anyArgs = new ValueType[ 20 ];
                Arrays.fill( anyArgs, ValueType.ANY );
                MessageDefinition def = 
                    new MessageDefinition( message.toString(),
                                           anyArgs, ValueType.ANY, 0 );
                messageMap_.put( message, def );
            }
        }

        /* Assuming the message ID is known, check the arguments have the
         * expected types. */
        else {
            MessageDefinition msgDef =
                (MessageDefinition) messageMap_.get( message );
            Object[] args = argList.toArray();
            ValueType[] types = msgDef.getArgTypes();
            int nreq = msgDef.getRequiredArgs();
            if ( args.length < nreq ) {
                warningList.add( "Too few arguments"
                               + " (" + args.length + " < " + nreq + ")" );
            }
            for ( int i = 0; i < args.length && i < types.length; i++ ) {
                try {
                    types[ i ].checkJavaValue( args[ i ] );
                }
                catch ( ValueTypeException e ) {
                    warningList.add( "Argument #" + i + " type mismatch: " 
                                   + e.getMessage() );
                }
            }
        }

        /* Returns accumulated list of warning strings. */
        return (String[]) warningList.toArray( new String[ 0 ] );
    }

    /** 
     * Performs validation on the response to an observed message
     * and returns an array of warning strings.  The array will be
     * empty if the response looks OK.
     * Warnings which would have cropped up in a corresponding 
     * {@link validateRequest} call will not be repeated.
     *
     * @param   message  message ID
     * @param   retval   response value to validate
     * @return  array of warning messages
     */
    public String[] validateResponse( URI message, Object retval ) {
        if ( messageMap_.containsKey( message ) ) {
            MessageDefinition msgDef =
                (MessageDefinition) messageMap_.get( message );
            try {
                msgDef.getReturnType().checkJavaValue( retval );
            }
            catch ( ValueTypeException e ) {
                return new String[] { 
                    "Return value type mismatch: " + e.getMessage(),
                };
            }
        }
        return new String[ 0 ];
    }

    /**
     * Locates an application in a ListModel of <code>ApplicationItem</code>s
     * with a given ID.
     *
     * @param   list   ListModel containing {@link ApplicationItem} objects
     * @param   appId  ID of application to look for
     * @return  application item with ID <code>appId</code>, or null if
     *          there isn't one
     */
    private static ApplicationItem getRegisteredApplication( ListModel list,
                                                             URI appId ) {
        for ( int i = 0; i < list.getSize(); i++ ) {
            Object item = list.getElementAt( i );
            ApplicationItem app = (ApplicationItem) item;
            if ( app.getId().equals( appId ) ) {
                return app;
            }
        }
        return null;
    }

    /**
     * Comes up with a message definition object which would describe 
     * an observed message.  Does the best job it can.
     *
     * @param   message  message ID
     * @param   argList  list of actual arguments to the message
     * @param   rtnValue  actual return value from the message
     */
    private static MessageDefinition inferMessageDefinition( URI message,
                                                             List argList,
                                                             Object rtnValue )
            throws ValueTypeException {
        ValueType[] types = new ValueType[ argList.size() ];
        for ( int i = 0; i < types.length; i++ ) {
            types[ i ] = ValueType.inferValueType( argList.get( i ) );
        }
        ValueType rtnType = rtnValue == null
                          ? ValueType.ANY
                          : ValueType.inferValueType( rtnValue );
        return new MessageDefinition( message.toString(), types, rtnType, 0 );
    }
}
