/** RadialVelocity.java
 *
 * @author Roy Platon
 * @version 1.00 26 March 2002
 * Calculate Radial components of Observer's Velocity
 **/

package uk.ac.starlink.rv;

import java.io.*;
import java.util.*;
import java.text.*;

import uk.ac.starlink.pal.*;

/**
 *  Calculates Radial Velocities.
 */
public class RadialVelocity {
    private final String version = "Version 1.00";
    
/*
 *  Debug Flags
 */
    private final boolean DEBUG = false;
    private final boolean DEBUG_FULL = false;

    private Observatory obs = null;
    private int year = 0, month = 0, day = 0, days = 0;
    private char jorb = 'B'; 
    private double equinox, jdate = 0.0, mid = 0.0;
    private Pal pal;
    private Vector Output = null;

/**
 * The Main constuctor to set up default data
 */            
    public RadialVelocity( ) {
        setup();
    }
/**
 *  The constuctor to set up default data for an Observatory
 *  @param observ the Observatory identifier
 */            
    public RadialVelocity ( String observ ) {
        setObservatory( observ );
        setup();
    }
/**
 *  Start Point
 */
    public void start( String observ ) {
        setup();
        obs = pal.Obs( observ );
        System.out.println( "Observatory: " + observ );
        System.out.println( "Observatory: " + obs.getName() );
    }

    private void setup(  ) {
        printDebug( "Radial Velocity Components - " + version );
        System.out.println( "Radial Velocity Components - " + version );
        pal = new Pal();
    }

/**
 * Sets up an Observatory
 * @param observatory  The Observatory identifier
 */            
    public void setObservatory( String observatory ) {
        obs = pal.Obs( observatory );
    }
/**
 * Sets up an Observatory
 * @param id  The Observatory number
 */            
    public void setObservatory( int id ) {
        obs = pal.Obs( id );
    }

/**
 * Sets a Date
 * @param date The date expressed as a string( yyyy mm dd days
 */            
    public void setDate( String date  ) throws palError {
        StringTokenizer st = new StringTokenizer( date );
        if ( st.countTokens() != 3 )
             throw new palError( "Needs 3 Integers in Date" );
        try {
            year = Integer.parseInt( st.nextToken() );
            month = Integer.parseInt( st.nextToken() );
            day = Integer.parseInt( st.nextToken() );
        }
        catch ( NumberFormatException e ) {
            throw new palError ( "Illegal Number in Date: " + date );
        }
        try {
           jdate = pal.Caldj( year, month, day );
        }
        catch ( palError e ) {
            throw new palError ( "Illegal Date: " + date );
        }
    }

/**
 * Sets a Date
 * @param date The date expressed as a string( yyyy mm dd days
 */            
    public void setDays( String day  ) throws palError {
        try {
            days = Integer.parseInt( day );
        }
        catch ( NumberFormatException e ) {
            throw new palError ( "Illegal Number in Days: " + day );
        }
    }

/**
 * Sets an equinox
 * @param Equinox The equinox expressed as a String XYYYY,
 * where X is either J or B and YYYY is a year
 */            
    public void setEquinox ( String Equinox ) throws palError {
        int pos = 0;
        int jb = 0;
        jorb = Equinox.charAt( 0 );
        try {
            if ( jorb == 'B' ) jb = 1;
            if ( jorb == 'J' ) jb = 2;
            if ( jb > 0 )  pos = 1;
            else jorb = 'B';
            equinox = Double.parseDouble( Equinox.substring( pos ) );
        }
        catch ( NumberFormatException e ) {
            throw new palError ( "Illegal Equinox: " + Equinox );
        }
        if ( equinox < 1900.0 || equinox > 2100.0 ) {
            throw new palError ( "Equinox " + equinox + " out of range" );
        }       
    }

/**
 * Starts the calculation
 * @param hms The coordinates expessed as h m s
 * @param dms The coordinates expessed as d m s
 * @return The Radial velocities as an Array of Strings
 */            
    public String[] calculate( String hms, String dms ) throws palError {
        Vector v = calculateRV( hms, dms );
        int len = v.size();
        String s[] = new String[len];
        for ( int i = 0; i < len; i++ ) s[i] = (String) v.elementAt(i);
        return s;    
    }

/**
 * Calculate the Radial Velocities
 * @param hms The coordinates expessed as h m s
 * @param dms The coordinates expessed as d m s
 * @return The Radial velocities as a Vector of Strings
 */            
    public Vector calculateRV( String hms, String dms ) throws palError {
        double date = 0.0, deltat, mid;
        int h, m, d, mm;
        double s = 0.0, ss = 0.0, rr, dd, da;
        int p1 = 0, p2, p3;
        boolean negr, negd;
        Output = new Vector(10);
        AngleDR r, r2, ra, rd, rw, rm;
        String data = hms;

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumIntegerDigits( 2 );
        nf.setMaximumIntegerDigits( 4 );
        nf.setGroupingUsed( false );

        try {
            StringTokenizer stok = new StringTokenizer( hms );
            if ( stok.countTokens() != 3 ) throw
                new palError( "Not enough values in R.A., needs 3: ");
            String str = stok.nextToken();
            negr = ( str.indexOf( '-' ) >= 0 );
            h = Integer.parseInt( str );
            m = Integer.parseInt( stok.nextToken() );
            s = Double.parseDouble( stok.nextToken() );
            rr = pal.Dtf2r( h, m, s );

            stok = new StringTokenizer( dms );
            data = dms;
            if ( stok.countTokens() != 3 ) throw
                new palError( "Not enough values in Dec, needs 3: " );
            str = stok.nextToken();
            negd = ( str.indexOf( '-' ) >= 0 );
            d = Integer.parseInt( str );
            mm = Integer.parseInt( stok.nextToken() );
            ss = Double.parseDouble( stok.nextToken() );
            dd = pal.Daf2r( Math.abs( d ), mm, ss );
            if ( negr ) rr = -rr;
            if ( negd ) dd = -dd;
            r = new AngleDR ( rr, dd );
        }
        catch ( NumberFormatException e ) {
            throw new palError ( "Illegal Number in: " + data ) ;
        }
        catch ( palError e ) {
            throw new palError ( "Illegal value in data (" + e + ")" );
        }

/* Correction from UTC to TDT (days) */
        deltat = pal.Dtt( jdate ) / pal.D2S;

/* TDB for middle of report */
        mid = jdate + deltat + days / 2.0;

/* Mean J2000 Place */
        r2 = r;
        if ( jorb == 'J' ) {
            r2 = pal.Preces( "FK5", equinox, 2000.0, r2 );
        } else {
            rw = pal.Preces( "FK4", equinox, 1950.0, r2 );
            r2 = pal.Fk45z ( rw, pal.Epj( mid ) );
        }
        double v2[] = pal.Dcs2c( r2 );

/* Geocentric Apparent Place */
        double pm[] = { 0.0, 0.0 };
        Stardata sd = new Stardata( r2, pm, 0.0, 0.0 );
        ra = pal.Map( sd, 2000.0, mid );
        da = ra.getDelta();
        double sind = Math.sin( da );
        double cosd = Math.cos( da );

/* Galactic and Ecliptic coordinates */
        Galactic gl = pal.Eqgal( r2 );
        AngleDR el = pal.Eqecl( r2, mid );

/* Slowly changing velocity components */
        double vclsrd = pal.Drvlsrd( r2 );
        double vclsrk = pal.Drvlsrk( r2 );
        double vcgalc = pal.Drvgalc( r2 );
        double vclg = pal.Drvlg( r2 );

/* Produce report Headings */
        print ( " Radial Component of observer's velocity\n" );

        print ( " Observatory:   " + obs );
        print ( spaces(16) + obs.printPosition( 1 ) + "\n" );

        double jd = jdate + 2400000.5;
        print ( " Starting UTC date:   " +
                   year + "/" + nf.format(month) + "/" + nf.format(day) +
                   "  =  JD " + jd + "\n" );

        palTime sr = pal.Dr2tf( r.getAlpha() );
        palTime srd = pal.Dr2af( r.getDelta() );
        print ( " Equatorial coordinates:   " + sr.toString(2) +
                 "  " + srd.printSign() + srd.toString(1) 
                 + "   " + jorb + equinox ); 

/* Mean J2000 place if necessary */
        if ( jorb != 'J' || equinox != 2000.0 ) {
            palTime sr2 = pal.Dr2tf( r2.getAlpha() );
            palTime sd2 = pal.Dr2af( r2.getDelta() );
            print ( spaces(27) + sr2.toString(2) +
                 "  " + sd2.printSign() + sd2.toString(1) 
                 + "   J2000.0" );
        }

/* Geocentric Apparent Place */
        palTime sra = pal.Dr2tf( ra.getAlpha() ); 
        palTime sda = pal.Dr2af( ra.getDelta() ); 
        print ( spaces(27) + sra.toString(2) +
                 "  " + sda.printSign() + sda.toString(1) 
                 + "   geocentric apparent\n" );

        print ( " Galactic coordinates:   L2 = " + 
                            format( gl.getLongitude()*pal.R2D, 8, 4 ) +
                            "  B2 = " +
                            formats( gl.getLatitude()*pal.R2D, 8, 4 ) +
                            "\n");
        print ( " Ecliptic coordinates:   L  = " + 
                            format( el.getAlpha()*pal.R2D, 8, 4 ) +
                            "  B  = " + 
                            formats( el.getDelta()*pal.R2D, 8, 4 ) +
                            "  (mean equinox of date)\n\n" );

/* Print Heading */
        print ( spaces(7) + "UTC" + spaces(12) + "ZD     EARTH" + spaces(9) +
               "SUN" + spaces(12) + "LSR (K)   LSR (D)" + spaces(5) +
               "GALAXY    LOCAL GROUP\n" );
        for ( int i = 0; i < days; i++ ) {
            double djm1 = jdate + i;
            mjDate iymdf;
            try {
                iymdf = pal.Djcal( djm1 );
            }
            catch ( palError e ) {
                printError ( "Date Error" );
                return null;
            }
            for ( int j = 0; j < 48; j++ ) {
                double ut = djm1 + ( j / 48.0 );
                double w = obs.getLong();
                double p = obs.getLat();
                double st = pal.Gmst( ut ) - w;
                double ha = st - ra.getAlpha();
                double cosp = Math.cos( p );
                double sinp = Math.sin( p );
                double cosz = sinp*sind + cosp*cosd*Math.cos( ha );
                double zd = pal.R2D * Math.atan2( Math.sqrt( 1.0 -
                                   Math.min( 1.0, Math.pow(cosz,2) ) ), cosz );
                if ( zd < 90.0 ) {
                    int nuth = j / 2;
                    int nutm = ( j != (2 * nuth) ? 30 : 0 );
                    double tdb = ut + deltat;
                    double dvb[] = new double[3], dpb[] = new double[3],
                           dvh[] = new double[3], dph[] = new double[3];
                    pal.Evp( tdb, 2000.0, dvb, dpb, dvh, dph );
                    double vcrot = pal.Drverot( p, ra, st );
                    double vcorb = - pal.Dvdv( v2, dvh ) * pal.AUKM;
                    double rvtot2 = vcrot + vcorb;
                    double rvtot3 = rvtot2 + vclsrd;
                    double rvtot4 = rvtot3 + vcgalc;
                    double rvtot5 = rvtot2 + vclg;
                    double tl = - pal.Dvdv( v2, dph ) * pal.AUSEC;
                    print ( " " + nf.format( iymdf.getYear() ) + " " +
                            nf.format( iymdf.getMonth() )+ " " +
                            nf.format( iymdf.getDay() )+ " " +
                            nf.format( nuth ) + ":" + nf.format( nutm ) +
                            format( zd, 8, 1 ) +  
                            formats( vcrot, 9, 2 ) + formats( rvtot2, 10, 2 )
                            + " (" + formats( tl, 6, 1 ) + ") " +
                            formats( rvtot2 + vclsrk, 10, 2 ) + 
                            formats( rvtot3, 10, 2 )  + 
                            formats( rvtot4, 12, 2 ) + formats( rvtot5, 13, 2 ) );
                }
            }
        }
        Output.trimToSize();
        return Output;
    }

    private void print( String message )
    {
 //       System.out.println( message );
        Output.add( message );
        return;
    }

    private void printError( String message )
    {
        System.err.println( message );
        return;
    }

    private String format( double number, int fieldsize, int ndp )
    {
        return format ( number, fieldsize, ndp, false );   
    }

    private String formats( double number, int fieldsize, int ndp )
    {
        return format ( number, fieldsize, ndp, true );   
    }

    private String format( double number, int fieldsize, int ndp, boolean sign )
    {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits( ndp );
        nf.setMinimumFractionDigits( ndp );
        String num = nf.format( number );
        if ( sign && number >= 0.0 ) num = "+" + num;
        if ( num.length() < fieldsize ) {
            num = spaces( fieldsize - num.length() ).concat( num );
        }
        return num;
    }

    private String spaces( int num )
    {
        char sp[] = new char[num];
        for ( int i=0; i < num; i++ ) sp[i] = ' ';
        return new String( sp );
    }

    private void printDebug( String message )
    {
        if ( DEBUG ) System.err.println( "[rv Debug]: " + message );
        return;
    }
}
