package uk.ac.starlink.tfcat;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard reporter implementation.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public class BasicReporter implements Reporter {

    private final boolean isDebug_;
    private final String context_;
    private final List<String> messages_; 
    private final WordChecker ucdChecker_;
    private final WordChecker unitChecker_;

    /**
     * Constructs a BasicReporter with no UCD or VOUnit validation.
     *
     * @param   isDebug   if true, reports will be trigger a stack trace
     *                    on standard error
     */
    public BasicReporter( boolean isDebug ) {
        this( isDebug, null, null );
    }

    /**
     * Constructs a BasicReporter with configurable UCD and VOUnit validation.
     *
     * @param   isDebug   if true, reports will be trigger a stack trace
     *                    on standard error
     * @param  ucdChecker   checks UCD strings
     * @param  unitChecker  checks unit strings
     */
    public BasicReporter( boolean isDebug,
                          WordChecker ucdChecker, WordChecker unitChecker ) {
        this( isDebug, ucdChecker, unitChecker,
              (String) null, new ArrayList<String>() );
    }

    /**
     * Copy constructor for internal use.
     *
     * @param  template  template reporter
     * @param  context   full context string
     */
    private BasicReporter( BasicReporter template, String context ) {
        this( template.isDebug_, template.ucdChecker_, template.unitChecker_,
              context, template.messages_ );
    }

    /**
     * Internal constructor used by all others.
     *
     * @param   isDebug   if true, reports will be trigger a stack trace
     *                    on standard error
     * @param  ucdChecker   checks UCD strings
     * @param  unitChecker  checks unit strings
     * @param  context   full context string
     * @param  messages  list of accumulated report messages
     */
    private BasicReporter( boolean isDebug,
                           WordChecker ucdChecker, WordChecker unitChecker,
                           String context, List<String> messages ) {
        isDebug_ = isDebug;
        ucdChecker_ = ucdChecker;
        unitChecker_ = unitChecker;
        context_ = context;
        messages_ = messages;
    }

    /**
     * Returns a list of all the messages accumulated by this reporter
     * and its sub-reporters.
     *
     * @return  message list; empty for fully valid input
     */
    public List<String> getMessages() {
        return messages_;
    }

    public BasicReporter createReporter( String subContext ) {
        String context = context_ == null ? subContext
                                          : context_ + "/" + subContext;
        return new BasicReporter( this, context );
    }

    public BasicReporter createReporter( int subContext ) {
        String context = ( context_ == null ? "" : context_ )
                       + "[" + subContext + "]";
        return new BasicReporter( this, context );
    }

    public void report( String message ) {
        StringBuffer sbuf = new StringBuffer();
        if ( context_ != null ) {
            sbuf.append( context_ )
                .append( ": " );
        }
        sbuf.append( message );
        String txt = sbuf.toString();
        messages_.add( txt );
        if ( isDebug_ ) {
            System.err.println( txt );
            Thread.dumpStack();
            System.err.println();
        }
    }

    public void checkUcd( String ucd ) {
        if ( ucdChecker_ != null && ucd != null ) {
            String report = ucdChecker_.checkWord( ucd );
            if ( report != null ) {
                report( report );
            }
        }
    }

    public void checkUnit( String unit ) {
        if ( unitChecker_ != null && unit != null ) {
            String report = unitChecker_.checkWord( unit );
            if ( report != null ) {
                report( report );
            }
        }
    }
}
