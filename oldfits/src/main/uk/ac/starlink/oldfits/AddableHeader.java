package uk.ac.starlink.oldfits;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;

/**
 * Package-private class to get round a restriction on the nom.tam.fits.Header
 * class - this one just subclasses it and publicises the addLine methods
 * which are (I don't know why) protected in Header.
 */
class AddableHeader extends Header {
    public void addLine( HeaderCard card ) {
        super.addLine( card );
    }
}
