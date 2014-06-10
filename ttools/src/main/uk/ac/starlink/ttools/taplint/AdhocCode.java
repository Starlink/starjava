package uk.ac.starlink.ttools.taplint;

import java.util.logging.Logger;

/**
 * ReportCode implementation which can be created at runtime.
 * In general, use of this class should be avoided in favour of
 * {@link FixedCode} (create as many new enum constants as you want)
 * where possible, so that static determination of possible codes
 * works as well as it can.
 *
 * @author   Mark Taylor
 * @since    11 Jun 2014
 */
public class AdhocCode implements ReportCode {

    private final ReportType type_;
    private final String label_;

    /** Required length of labels. */
    public static final int LABEL_LENGTH = 4;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.taplint" );

    /**
     * Constructor.
     *
     * @param  type  report type
     * @param  label  4-character label
     */
    public AdhocCode( ReportType type, String label ) {
        type_ = type;
        if ( label.length() != LABEL_LENGTH ) {
            assert false;
            label = createLabelChars( label, LABEL_LENGTH );
            logger_.warning( "Wrong length label: changed to " + label );
        }
        label_ = label;
    }

    public ReportType getType() {
        return type_;
    }

    public String getLabel() {
        return label_;
    }

    @Override
    public int hashCode() {
        return type_.hashCode() * 23 + label_.hashCode();
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof ReportCode ) {
            ReportCode other = (ReportCode) o;
            return this.getType() == other.getType()
                && this.getLabel().equals( other.getLabel() );
        }
        else {
            return false;
        }
    }

    /**
     * Uses some hash function to generate a report code from text.
     * Probably unique, but not guaranteed to be.
     *
     * @param  type  report type
     * @param  text  message text
     * @return   suitable message code
     */
    public static AdhocCode createCodeFromText( ReportType type, String text ) {
        return new AdhocCode( type, createLabelChars( text, LABEL_LENGTH ) );
    }

    /**
     * Uses some hash function to generate a fixed-length character string
     * from a supplied object.
     *
     * @param   id   object to seed character generation
     * @param   nchar  number of characters required
     * @return  nchar-character string
     */
    public static String createLabelChars( Object id, int nchar ) {
        int hash = id.hashCode();
        char[] chrs = new char[ nchar ];
        for ( int i = 0; i < nchar; i++ ) {
            chrs[ i ] = (char) ( 'A' + ( ( hash & 0x1f ) % ( 'Z' - 'A' ) ) );
            hash >>= 5;
        }
        return new String( chrs );
    }
}
