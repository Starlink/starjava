/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: FITSKeywordProvider.java,v 1.2 2002/08/05 10:57:21 brighton Exp $
 */

package jsky.image.fits;

import jsky.coords.WCSKeywordProvider;
import jsky.image.fits.codec.FITSImage;

import nom.tam.fits.Header;



/**
 * A simple class accessing FITS/WCS keywords, where keywords are inherited
 * from an empty primary FITS extension, if present.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class FITSKeywordProvider implements WCSKeywordProvider {

    private Header _primary;
    private Header _header;

    public FITSKeywordProvider(FITSImage fitsImage) {
	_header = fitsImage.getHeader();
	if (_header == null)
	    throw new IllegalArgumentException("No FITS header");
	
        if (fitsImage.getNumHDUs() > 1) {
	    _primary = fitsImage.getHDU(0).getHeader();
	    if (_primary == null)
		throw new IllegalArgumentException("No primary FITS extension header");
	    if (_primary.getIntValue("NAXIS") != 0) {
		_primary = new Header(); // ignore if not an empty primary extension
	    }
	}
	else {
	    _primary = new Header();
	}
    }

    /** Return true if the given keyword was found */
    public boolean findKey(String key) {
	return _header.findKey(key) != null || _primary.findKey(key) != null;
    }


    /** Return the value of the given keyword as a String, or null if not found. */
    public String getStringValue(String key) {
	if (_header.findKey(key) != null)
	    return _header.getStringValue(key);
	return _primary.getStringValue(key);
    }

    /** Return the value of the given keyword as a String, or null if not found. */
    public String getStringValue(String key, String defaultValue) {
	if (_header.findKey(key) != null)
	    return _header.getStringValue(key);
	if (_primary.findKey(key) != null)
	    return _primary.getStringValue(key);
	return defaultValue;
    }


    /** Return the value of the given keyword as a double, or 0.0 if not found. */
    public double getDoubleValue(String key) {
	if (_header.findKey(key) != null)
	    return _header.getDoubleValue(key);
	return _primary.getDoubleValue(key);
    }

    /** Return the value of the given keyword as a double, or 0.0 if not found. */
    public double getDoubleValue(String key, double defaultValue) {
	if (_header.findKey(key) != null)
	    return _header.getDoubleValue(key, defaultValue);
	return _primary.getDoubleValue(key, defaultValue);
    }


    /** Return the value of the given keyword as a double, or 0.0 if not found. */
    public float getFloatValue(String key) {
	if (_header.findKey(key) != null)
	    return _header.getFloatValue(key);
	return _primary.getFloatValue(key);
    }

    /** Return the value of the given keyword as a double, or 0.0 if not found. */
    public float getFloatValue(String key, float defaultValue) {
	if (_header.findKey(key) != null)
	    return _header.getFloatValue(key, defaultValue);
	return _primary.getFloatValue(key, defaultValue);
    }


    /** Return the value of the given keyword as an int, or 0 if not found. */
    public int getIntValue(String key) {
	if (_header.findKey(key) != null)
	    return _header.getIntValue(key);
	return _primary.getIntValue(key);
    }

    /** Return the value of the given keyword as an int, or 0 if not found. */
    public int getIntValue(String key, int defaultValue) {
	if (_header.findKey(key) != null)
	    return _header.getIntValue(key, defaultValue);
	return _primary.getIntValue(key, defaultValue);
    }
}
