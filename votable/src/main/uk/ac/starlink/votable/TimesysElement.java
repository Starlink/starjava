package uk.ac.starlink.votable;

import java.util.logging.Logger;
import org.w3c.dom.Element;

/**
 * Element subclass for a TIMESYS element in a VOTable.
 * This element was only introduced at VOTable 1.4.
 *
 * @author   Mark Taylor
 * @since    25 Apr 2019
 */
public class TimesysElement extends VOElement {

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.votable" );

    /**
     * Constructor.
     *
     * @param   base  TIMESYS element
     * @param   doc   owner document for new element
     */
    TimesysElement( Element base, VODocument doc ) {
        super( base, doc, "TIMESYS" );
    }

    /**
     * Returns the time origin as a numeric value.
     *
     * @return   time origin as a Julian Date
     *           (days offset from JD-origin)
     */
    public double getTimeOrigin() {
        String attvalue = getAttribute( "timeorigin" );
        try {
            return Timesys.decodeTimeorigin( attvalue );
        }
        catch ( NumberFormatException e ) {
            logger_.warning( "Illegal form for TIMESYS/@timeorigin: "
                           + "\"" + attvalue + "\"" );
            return Double.NaN;
        }
    }
}
