package uk.ac.starlink.topcat;

import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.task.Credibility;
import uk.ac.starlink.ttools.task.CredibleString;

/**
 * Defines a way of invoking the STILTS command.
 *
 * @author   Mark Taylor
 * @since    22 Sep 2017
 */
public class StiltsInvoker {

    private final String name_;
    private final CredibleString invocation_;

    /** Instance using "stilts" command. */
    public static final StiltsInvoker STILTS;

    /** Instance using "topcat" command. */
    public static final StiltsInvoker TOPCAT;

    /** Instance using "java" command. */
    public static final StiltsInvoker CLASSPATH;

    /** List of all known invokers. */
    public static final StiltsInvoker[] INVOKERS = {
        STILTS =
            new StiltsInvoker( "stilts",
                                new CredibleString( "stilts",
                                                    Credibility.MAYBE ) ),
        TOPCAT =
            new StiltsInvoker( "topcat",
                               new CredibleString( "topcat -stilts",
                                                   Credibility.MAYBE ) ),
        CLASSPATH =
            new StiltsInvoker( "Class-path",
                               new CredibleString( getClasspathInvocation(),
                                                   Credibility.YES ) ),
    };

    /**
     * Constructor.
     *
     * @param  name  invoker name for presentation to the user
     * @param  invocation  invocation text
     */
    public StiltsInvoker( String name, CredibleString invocation ) {
        name_ = name;
        invocation_ = invocation;
    }

    /**
     * Returns the invocation string.
     *
     * @return  invocation string, including trailing whitespace
     */
    public CredibleString getInvocation() {
        return invocation_;
    }

    /**
     * Returns name for presentation to user.
     */
    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a string invoking using the current JVM classpath.
     *
     * @return  invocation string
     */
    private static String getClasspathInvocation() {
        try {
            return new StringBuffer()
                .append( "java " )
                .append( "-classpath " )
                .append( System.getProperty( "java.class.path" ) )
                .append( ' ' )
                .append( Stilts.class.getName() )
                .toString();
         }
         catch ( Throwable e ) {
             return "???";
         }
    }
}
