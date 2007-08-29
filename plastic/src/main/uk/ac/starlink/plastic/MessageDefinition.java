package uk.ac.starlink.plastic;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Describes PLASTIC messages known to this package.
 * Instances of this class describe messages defined by the PLASTIC 
 * message definitions list, currently at 
 * <a href="http://plastic.sourceforge.net/coremessages.html"
 *         >http://plastic.sourceforge.net/coremessages.html</a>,
 * perhaps along with other ones known from elsewhere.
 * The message IDs, argument types and return types are given.
 *
 * @author   Mark Taylor
 * @since    18 Jul 2006
 */
public class MessageDefinition {

    private final URI id_;
    private final ValueType[] argTypes_;
    private final ValueType returnType_;
    private final int requiredArgs_;

    public static final MessageDefinition TEST_ECHO;
    public static final MessageDefinition INFO_GETIVORN;
    public static final MessageDefinition INFO_GETNAME;
    public static final MessageDefinition INFO_GETDESCRIPTION;
    public static final MessageDefinition INFO_GETVERSION;
    public static final MessageDefinition INFO_GETICONURL;
    public static final MessageDefinition HUB_APPREG;
    public static final MessageDefinition HUB_APPUNREG;
    public static final MessageDefinition HUB_STOPPING;
    public static final MessageDefinition VOT_LOAD;
    public static final MessageDefinition VOT_LOADURL;
    public static final MessageDefinition VOT_SHOWOBJECTS;
    public static final MessageDefinition VOT_HIGHLIGHTOBJECT;
    public static final MessageDefinition FITS_LOADLINE;
    public static final MessageDefinition FITS_LOADIMAGE;
    public static final MessageDefinition FITS_LOADCUBE;
    public static final MessageDefinition SKY_POINT;
    public static final MessageDefinition SPECTRUM_LOADURL;
    private static final Map MESSAGE_MAP;

    /** Known message definition list. */
    private static final MessageDefinition[] KNOWN_MESSAGES = {
        TEST_ECHO =
            new MessageDefinition( MessageId.TEST_ECHO.toString(),
                                   new ValueType[] { ValueType.STRING },
                                   ValueType.STRING ),

        INFO_GETIVORN =
            new MessageDefinition( MessageId.INFO_GETIVORN.toString(),
                                   new ValueType[ 0 ],
                                   ValueType.STRING_IVORN ),
        INFO_GETNAME =
            new MessageDefinition( MessageId.INFO_GETNAME.toString(),
                                   new ValueType[ 0 ],
                                   ValueType.STRING ),
        INFO_GETDESCRIPTION =
            new MessageDefinition( MessageId.INFO_GETDESCRIPTION.toString(),
                                   new ValueType[ 0 ],
                                   ValueType.STRING ),
        INFO_GETVERSION =
            new MessageDefinition( MessageId.INFO_GETVERSION.toString(),
                                   new ValueType[ 0 ],
                                   ValueType.STRING ),
        INFO_GETICONURL =
            new MessageDefinition( MessageId.INFO_GETICONURL.toString(),
                                   new ValueType[ 0 ],
                                   ValueType.STRING_URL ),

        HUB_APPREG =
            new MessageDefinition( MessageId.HUB_APPREG.toString(),
                                   new ValueType[] { ValueType.STRING_URI },
                                   ValueType.VOID ),

        HUB_APPUNREG =
            new MessageDefinition( MessageId.HUB_APPUNREG.toString(),
                                   new ValueType[] { ValueType.STRING_URI },
                                   ValueType.VOID ),
        HUB_STOPPING =
            new MessageDefinition( MessageId.HUB_STOPPING.toString(),
                                   new ValueType[ 0 ],
                                   ValueType.VOID ),

        VOT_LOAD =
            new MessageDefinition( MessageId.VOT_LOAD.toString(),
                                   new ValueType[] { ValueType.STRING,
                                                     ValueType.STRING },
                                   ValueType.BOOLEAN ),
        VOT_LOADURL =
            new MessageDefinition( MessageId.VOT_LOADURL.toString(),
                                   new ValueType[] { ValueType.STRING_URL },
                                   ValueType.BOOLEAN ),
        VOT_SHOWOBJECTS =
            new MessageDefinition( MessageId.VOT_SHOWOBJECTS.toString(),
                                   new ValueType[] { ValueType.STRING,
                                                     ValueType.LIST_INTS },
                                   ValueType.BOOLEAN ),
        VOT_HIGHLIGHTOBJECT =
            new MessageDefinition( MessageId.VOT_HIGHLIGHTOBJECT.toString(),
                                   new ValueType[] { ValueType.STRING,
                                                     ValueType.INT },
                                   ValueType.BOOLEAN ),

        FITS_LOADLINE =
            new MessageDefinition( MessageId.FITS_LOADLINE.toString(),
                                   new ValueType[] { ValueType.STRING_URL,
                                                     ValueType.STRING,
                                                     ValueType.INT },
                                   ValueType.BOOLEAN, 1 ),
        FITS_LOADIMAGE =
            new MessageDefinition( MessageId.FITS_LOADIMAGE.toString(),
                                   new ValueType[] { ValueType.STRING_URL,
                                                     ValueType.STRING,
                                                     ValueType.INT },
                                   ValueType.BOOLEAN, 1 ),
        FITS_LOADCUBE =
            new MessageDefinition( MessageId.FITS_LOADCUBE.toString(),
                                   new ValueType[] { ValueType.STRING_URL,
                                                     ValueType.STRING,
                                                     ValueType.INT },
                                   ValueType.BOOLEAN, 1 ),

        SKY_POINT =
            new MessageDefinition( MessageId.SKY_POINT.toString(),
                                   new ValueType[] { ValueType.DOUBLE,
                                                     ValueType.DOUBLE },
                                   ValueType.BOOLEAN ),

        SPECTRUM_LOADURL =
            new MessageDefinition( MessageId.SPECTRUM_LOADURL.toString(),
                                   new ValueType[] { ValueType.STRING_URL,
                                                     ValueType.STRING,
                                                     ValueType.MAP },
                                   ValueType.BOOLEAN ),
    };
    static {
        Map map = new HashMap();
        for ( int i = 0; i < KNOWN_MESSAGES.length; i++ ) {
            MessageDefinition def = KNOWN_MESSAGES[ i ];
            map.put( def.getId(), def );
        }
        MESSAGE_MAP = Collections.unmodifiableMap( map );
    }

    /**
     * Constructs a message definition which may have some optional arguments.
     *
     * @param   id   message identifier; this must have the form of a URI
     * @param   argTypes  types of each argument
     * @param   returnType  type of return value
     * @param   requiredArgs  minimum number of arguments for a legal
     *                        call of this message
     */
    public MessageDefinition( String id, ValueType[] argTypes,
                              ValueType returnType, int requiredArgs ) {
        try {
            id_ = new URI( id );
        }
        catch ( URISyntaxException e ) {
            throw (IllegalArgumentException)
                  new IllegalArgumentException( "Bad URI: " + id )
                 .initCause( e );
        }
        argTypes_ = argTypes;
        returnType_ = returnType;
        requiredArgs_ = requiredArgs;
    }

    /**
     * Constructs a message definition whose arguments are all required.
     *
     * @param   id   message identifier; this must have the form of a URI
     * @param   argTypes  types of each argument
     * @param   returnType  type of return value
     */
    public MessageDefinition( String id, ValueType[] argTypes,
                              ValueType returnType ) {
        this( id, argTypes, returnType, argTypes.length );
    }

    /**
     * Returns the URI which forms the message ID of this message.
     * This probably starts "<code>ivo://...</code>".
     *
     * @return  message ID
     */
    public URI getId() {
        return id_;
    }

    /**
     * Returns an array of the types of the first N arguments which 
     * must be sent with this message.
     *
     * @return  array of argument value types
     */
    public ValueType[] getArgTypes() {
        return (ValueType[]) argTypes_.clone();
    }

    /**
     * Returns the type of the value which must be returned by this message.
     *
     * @return  return value type
     */
    public ValueType getReturnType() {
        return returnType_;
    }

    /**
     * Returns the number of arguments which are required by this message.
     * This will be less than or equal to the size of the array returned
     * by {@link #getArgTypes}.  A message may legally carry a number
     * of arguments which is larger than either of these values.
     *
     */
    public int getRequiredArgs() {
        return requiredArgs_;
    }

    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( getReturnType().toString() );
        sbuf.append( ' ' );
        sbuf.append( getId() );
        sbuf.append( "(" );
        ValueType[] types = getArgTypes();
        for ( int i = 0; i < types.length; i++ ) {
            if ( i > 0 ) {
                sbuf.append( ',' );
            }
            sbuf.append( types[ i ] );
        }
        sbuf.append( ')' );
        return sbuf.toString();
    }

    /**
     * Returns an array of known standard message definitions.
     * This comprises all the ones defined as public static members
     * of this class.  The returned object is a clone.
     *
     * @return   known messages list
     */
    public static MessageDefinition[] getKnownMessages() {
        return (MessageDefinition[]) KNOWN_MESSAGES.clone();
    }

    /**
     * Returns the message definition corresponding to a given message ID.
     * Returns null if no such message is known.
     *
     * @param   id   message ID
     * @return  definition for message with ID <code>id</code>, or null
     */
    public static MessageDefinition getMessage( URI id ) {
        return (MessageDefinition) MESSAGE_MAP.get( id );
    }
}
