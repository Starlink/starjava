package uk.ac.starlink.topcat.func;

/**
 * Activation methods which interact with the Sloane Digital Sky Server.
 *
 * <p>At time of writing this is not installed in the available set of
 * activation methods by default.
 *
 * @author   Mark Taylor (Starlink)
 * @since    25 Aug 2004
 */
public class Sdss {

    /**
     * Displays a colour cutout image of a specified size from the 
     * SDSS around a given sky position.  The displayed image is square, 
     * a given number of (0.4arcsec) pixels on each side.
     *
     * @param  ra  Right Ascension in radians
     * @param  dec Declination in radians
     * @param  pixels  size of displayed image in SDSS pixels
     * @return  short log message
     */
    public static String sdssCutout( double ra, double dec, int pixels ) {
        String query = new Query( "http://skyservice.pha.jhu.edu/" +
                                  "dr2/ImgCutout/getjpeg.aspx" )
             .addArgument( "ra", Math.toDegrees( ra ) )
             .addArgument( "dec", Math.toDegrees( dec ) )
             .addArgument( "height", pixels )
             .addArgument( "width", pixels )
             .toString();
        return Display.sog( query );
    }

    /**
     * Displays a colour cutout image of a default size from the SDSS
     * around a given sky position.  The displayed image is 128 pixels
     * square - a pixel is 0.4arcsec.
     *
     * @param  ra  Right Ascension in radians
     * @param  dec Declination in radians
     * @return  short log message
     */
    public static String sdssCutout( double ra, double dec ) {
        return sdssCutout( ra, dec, 128 );
    }

    /** 
     * Helper class for forming CGI queries.
     */
    private static class Query {
        StringBuffer sbuf_ = new StringBuffer();
        Query( String endpoint ) {
            sbuf_.append( endpoint )
                 .append( '?' );
        }
        Query addArgument( String name, double value ) {
            return addArgument( name, Double.toString( value ) );
        }
        Query addArgument( String name, long value ) {
            return addArgument( name, Long.toString( value ) );
        }
        Query addArgument( String name, String value ) {
            sbuf_.append( '&' )
                 .append( name )
                 .append( '=' );
            for ( int i = 0; i < value.length(); i++ ) {
                char c = value.charAt( i );
                switch ( c ) {
                    case ' ':
                    case '%':
                        sbuf_.append( '%' )
                             .append( Integer.toHexString( (int) c ) );
                        break;
                    default:
                        sbuf_.append( c );
                }
            }
            return this;
        }
        public String toString() {
            return sbuf_.toString();
        }
    }
}
