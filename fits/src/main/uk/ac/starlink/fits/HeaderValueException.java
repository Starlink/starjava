package uk.ac.starlink.fits;

import java.io.IOException;

/**
 * Indicates a problem with a FITS header card.
 *
 * @author   Mark Taylor
 * @since    4 Mar 2022
 */
public class HeaderValueException extends IOException {

    /**
     * No-arg constructor.
     */
    public HeaderValueException() {
        super();
    }

    /**
     * Constructor.
     *
     * @param  msg   error message
     */
    public HeaderValueException( String msg ) {
        super( msg );
    }
}
