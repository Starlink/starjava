package uk.ac.starlink.tfcat;

import java.util.List;

/**
 * Utility class to represent the validity status of a TFCat text.
 * The {@link #getStatus} method parses a given text to return an instance
 * of this class.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public class TfcatStatus {

    private final Code code_;
    private final String message_;

    private static final WordChecker ucdChecker_ = TfcatUtil.getUcdChecker();
    private static final WordChecker unitChecker_ = TfcatUtil.getUnitChecker();

    /**
     * Constructor.
     *
     * @param   code  validity code
     * @param   message   message giving error reports, or null for valid
     */
    protected TfcatStatus( Code code, String message ) {
        code_ = code;
        message_ = message;
    }

    /**
     * Returns a code summarising the validity status.
     *
     * @return  code
     */
    public Code getCode() {
        return code_;
    }

    /**
     * Returns a message containing information about compliancy issues
     * during parsing.
     *
     * @return  error report message, or null for compliant text
     */
    public String getMessage() {
        return message_;
    }

    /**
     * Parses a given TFCat text and returns a status.
     *
     * @param  tfcatTxt   TFCat text; this should be a JSON Object or null
     * @return  parse status, or null for null/empty input
     */
    public static TfcatStatus getStatus( String tfcatTxt ) {
        if ( tfcatTxt == null || tfcatTxt.length() == 0 ) {
            return null;
        }
        boolean isDebug = false;
        BasicReporter reporter =
            new BasicReporter( isDebug, ucdChecker_, unitChecker_ );
        TfcatObject tfcat = TfcatUtil.parseTfcat( tfcatTxt, reporter );
        List<String> msgs = reporter.getMessages();
        int nmsg = msgs.size();
        final String msgTxt;
        switch ( nmsg ) {
            case 0:
                msgTxt = null;
                break;
            case 1:
                msgTxt = msgs.get( 0 );
                break;
            default:
                msgTxt = String.join( "; ", msgs );
        }
        final Code code;
        if ( tfcat == null ) {
            code = Code.FAIL;
        }
        else if ( nmsg == 0 ) {
            code = Code.OK;
        }
        else {
            code = Code.INVALID;
        }
        return new TfcatStatus( code, msgTxt );
    }

    /**
     * Parse result code.
     */
    public enum Code {

        /** Parse successful, no errors. */
        OK,

        /** Validity errors were encountered during parse. */
        INVALID,

        /** Fatal errors encounted during parse, no TFCat object constructed. */
        FAIL;
    }
}
