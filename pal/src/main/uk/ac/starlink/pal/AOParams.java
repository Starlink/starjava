/*  Starlink Positional Astronomy Library */

package uk.ac.starlink.pal;

import java.lang.*;
import java.text.*;

/** Star-independent apparent-to-observed Parameters
 */
public class AOParams {
    private double geolat;
    private double geolatsin;
    private double geolatcos;
    private double abb;
    private double height;
    private double ambtemp;
    private double pressure;
    private double relhumid;
    private double wavelength;
    private double lapserate;
    private double refractA;
    private double refractB;
    private double longplus;
    private double localtime;

/** Initialise apparent-to-observed Parameters (with zero values)
 */
    public AOParams ( ) {
        geolat = 0.0;
        geolatsin = 0.0;
        geolatcos = 0.0;
        abb = 0.0;
        height = 0;
        ambtemp = 0;
        pressure = 0;
        relhumid = 0;
        wavelength = 0;
        lapserate = 0;
        refractA = 0;
        refractB = 0;
        longplus = 0;
        localtime = 0;
    }

/** Star-independent apparent-to-observed Parameters
 *  @param glat  Geodetic latitude (radians)
 *  @param mag   Magnetude of diurnal aberration vector
 *  @param ht    Height (metres above sea level)
 *  @param temp  Ambient temperature (degrees K)
 *  @param pres  Pressure (millibars)
 *  @param humid Relative humidity (0-1)
 *  @param wavel Wavelength (&mu;m)
 *  @param lapse Lapse rate (degrees K per metre)
 *  @param ra    Refraction constant A (radians)
 *  @param rb    Refraction constant B (radians)
 *  @param longr Longitude + eqn of equinoxes + "sidereal &Delta;UT"
 *  @param loc   Local apparent sidereal time (radians)
 */
    public AOParams ( double glat, double mag, double ht, double temp,
            double pres, double humid, double wavel, double lapse,
            double ra, double rb, double longr, double loc ) {
        geolat = glat;
        geolatsin = Math.sin( glat );
        geolatcos = Math.cos( glat );
        abb = mag;
        height = ht;
        ambtemp = temp;
        pressure = pres;
        relhumid = humid;
        wavelength = wavel;
        lapserate = lapse;
        refractA = ra;
        refractB = rb;
        longplus = longr;
        localtime = loc;
    }

/** Get Geodetic latitude
 *  @return Geodetic latitude (radians)
 */
    public double getLat( ) { return geolat; }

/** Get sine of Geodetic latitude
 *  @return Sine of Geodetic latitude
 */
    public double getLatsin( ) { return geolatsin; }

/** Get cosine of Geodetic latitude
 *  @return Cosine of Geodetic latitude
 */
    public double getLatcos( ) { return geolatcos; }

/** Get height
 *  @return Height
 */
    public double getHeight( ) { return height; }

/** Get magnitude of diurnal aberration vector
 *  @return Magnitude of diurnal aberration vector
 */
    public double getDabb( ) { return abb; }

/** Get ambient temperature
 *  @return Ambient temperature
 */
    public double getTemp( ) { return ambtemp; }

/** Get pressure
 *  @return Pressure
 */
    public double getPressure( ) { return pressure; }

/** Get relative humidity
 *  @return Relative humidity
 */
    public double getHumidity( ) { return relhumid; }

/** Get wavelength
 *  @return Wavelength
 */
    public double getWavelength( ) { return wavelength; }

/** Get lapse rate
 *  @return Lapse rate
 */
    public double getLapserate( ) { return lapserate; }

/** Get refractive index A
 *  @return Refractive index A
 */
    public double getRefractA( ) { return refractA; }

/** Get refractive index B
 *  @return Refractive index B
 */
    public double getRefractB( ) { return refractB; }

/** Get longitude + eqn of equinoxes + "sidereal &Delta;UT"
 *  @return Longitude + eqn of equinoxes + "sidereal &Delta;UT"
 */
    public double getLongplus( ) { return longplus; }

/** Get local apparent sidereal time
 *  @return Local apparent sidereal time (radians)
 */
    public double getLocalTime( ) { return localtime; }

/** Set geodetic latitude
 *  @param g Geodetic latitude (radians)
 */
    public void setLat( double g ) { 
        geolat = g;
        geolatsin = Math.sin( g );
        geolatcos = Math.cos( g );
    }

/** Set magnitude of diurnal aberration vector
 *  @param dabb Magnitude of diurnal aberration vector
 */
    public void setDabb( double dabb ) { abb = dabb; }

/** Set height
 *  @param h Height 
 */
    public void setHeight( double h ) { height = h; }

/** Set ambient temperature
 *  @param a Ambient temperature
 */
    public void setTemp( double a ) { ambtemp = a; }

/** Set Pressure
 *  @param p Pressure
 */
    public void setPressure( double p ) { pressure = p; }

/** Set relative humidity
 *  @param rh Relative humidity
 */
    public void setHumidity( double rh ) { relhumid = rh; }

/** Set wavelength
 *  @param wl Wavelength
 */
    public void setWavelength( double wl ) { wavelength = wl; }

/** Set lapse rate
 *  @param lr Lapse rate
 */
    public void setLapserate( double lr ) { lapserate = lr; }

/** Set refractive index A
 *  @param rfa
 */
    public void setRefractA( double rfa ) { refractA = rfa; }

/** Set refractive index B
 *  @param rfb Refractive index B
 */
    public void setRefractB( double rfb ) { refractB = rfb; }

/** Set longitude + eqn of equinoxes + "sidereal &Delta;UT"
 *  @param lplus
 */
    public void setLongplus( double lplus ) { longplus = lplus; }

/** Set local apparent sidereal time
 *  @param lt Local apparent sidereal time (radians)
 */
    public void setLocalTime( double lt ) { localtime = lt; }

/** Get the parameters as a String
 *  @return Parameter string
 */
    public String toString() {
        return "Parameters: " + geolat + " (" + geolatsin + "," + geolatcos +
                ")" + abb + " " + height  + " " + ambtemp  + " " +
                pressure + " " + relhumid + " " + lapserate + " " + refractA
                + " " + refractB + " " + longplus+ " " + localtime;
    }
}
