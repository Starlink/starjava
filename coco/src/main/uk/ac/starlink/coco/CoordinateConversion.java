/** Conversion of Celestial Coordinates
 *
 * @author Roy Platon
 * @version 1.00 23 Oct 2001
 *
 * <h1>Coco - Conversion of Celestial Coordinates</h1>
 * <p>
 * This package provides an interface between the Coco font end
 * and the pal (Positional Astronomy Library).
 * </p> <p>
 * This routine can be used by either the Applet, a GUI or a text interface.
 * </p>
 **/

package uk.ac.starlink.coco;

import java.io.*;
import java.util.*;
import java.math.*;
import java.text.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/* pal (Positional Astronomy Library) */
import uk.ac.starlink.pal.*;

/**
 * Converts Coordinates between various systems.
 */
public class CoordinateConversion
{
    private final String Version = "Version 1.00";

/*  the Starlink Astronometry Class Library */
    private Pal pal;

/*
 * Debug settings
 */
    private final boolean DEBUG = true;

/*
 * Maths Constants
 */
    private static final double D2R = Math.PI/180.0;
    private static final double R2D = 180.0/Math.PI;
    private static final double S2R = Math.PI/(12*3600.0);
    private static final double AS2R = Math.PI/(180*3600.0);
    private static final double PIBY2 = Math.PI/2.0;

/* Default values */
    private char insys = '4';
    private char inJB = 'B';
    private double inepoch = 1950.0;
    private char inJBeq = 'B';
    private double inequinox = 1950.0;
    private char outsys = '5';
    private char outJB = 'J';
    private double outepoch = 2000.0;
    private char outJBeq = 'J';
    private double outequinox = 2000.0;
    private boolean hours = true;
    private boolean degrees = false;
    private boolean lowprec = false;
    private boolean mediumprec = true;
    private boolean highprec = false;
    private String answer = null;

    private double da = 0.0, db = 0.0, px = 0.0, rv = 0.0;
    private int count = 0, jz = 0;
    private NumberFormat nf = NumberFormat.getNumberInstance();
    private NumberFormat nf2 = NumberFormat.getNumberInstance();
    private NumberFormat nfep = NumberFormat.getNumberInstance();
    private NumberFormat nfpx = NumberFormat.getNumberInstance();
    private NumberFormat nfrv = NumberFormat.getNumberInstance();
    private int pra = 3, prb = 2, prep = 2;
    private int prpx = 3, prrv = 1;
    private int pr1 = 5, pr2 = 4;

/**
 * Start a new instance of the CoordinateConversion Loads the Pal Library.
 */
    public CoordinateConversion(  ) {
        setup();
    }
/**
 * The main constuctor, loads the Pal Library.
 * @param in The input system code
 * @param out The Output System code
 */
    public CoordinateConversion( char in, char out ) {
        insys = Character.toUpperCase(in);
        outsys = Character.toUpperCase(out);
        setup();
    }

    private void setup () {
        printDebug( "setup: Conversion of Celestial Coordinates (" + Version + ")" );
        pal = new Pal();
    }

/**
 * Convert the coordinates
 * @param data The Coordinates to convert
 * @return The converted coordinates as a string
 */
    public String convert ( String data ) {
        StringTokenizer s = new StringTokenizer( data );
        String answer = null;
        int num = s.countTokens();
        if ( num < 6 ) return "Insufficient data " + data;

        setInEpoch( s.nextToken() );
        setOutEpoch( s.nextToken() );
        String str = s.nextToken();
        if ( str.length() < 4 ) return "Insufficient data in " + str;
        setPrecision( str.charAt(0) );
        setUnits( str.charAt(1) );
        setInSystem( str.charAt(2) );
        setOutSystem( str.charAt(3) );
        String coords = s.nextToken();
        for ( num -= 4; num > 0; num-- ) coords = coords + " " + s.nextToken();
        System.out.println( "Coords = " + coords );

        AngleDR r = validate( coords );
        if ( r != null ) {
            answer = convert( r );
        } else {
            answer = "Invalid Input data: " + data;
        }
        return answer;
    }

/**
 * Set the input System.
 * @param in The input system.
 * <p>Allowable systems are:
 * <dl>
 * <dt>4</dt> <dd>Mean [alpha, delta], old system, with E-terms
 *                (loosely FK4, usually B1950) (default)</dd>
 * <dt>5</dt> <dd>Mean [alpha, delta], old system, no E-terms
 *                (some radio positions)</dd>
 * <dt>A</dt> <dd>Mean [alpha, delta], new system
 *                (loosely FK5, usually J2000)</dd>
 * <dt>B</dt> <dd>Geocentric apparent [alpha, delta], new system</dd>
 * <dt>E</dt> <dd>Ecliptic coordinates [lamda, beta], new system (mean of date)</dd>
 * <dt>G</dt> <dd>Galactic coordinates [l, b], IAU 1958 system</dd>
 * </dl>
 * </p>
 */
    public void setInSystem ( char in ) {
        insys = Character.toUpperCase( in );
        switch ( insys ) {
            case '4':
            case 'B':
            case 'G':
                inJB = inJBeq = 'B';
                inepoch = inequinox = 1950.0;
                break;
            case '5':
                inJB = inJBeq = 'J';
                inepoch = inequinox = 2000.0;
            case 'A':
            case 'E':
                break;
        }
    }

/**
 * Set the input Epoch.
 * @param ep The epoch, eg. 1984.53 or 1983 2 26.4
 */
    public void setInEpoch ( String ep ) {
        if ( ep == null ) return;
        inJB = Character.toUpperCase( ep.charAt(0) );
        if ( inJB == 'B' || inJB == 'J' )
            inepoch = Double.parseDouble( ep.substring( 1 ) );
        else {
            inJB = ' ';
            inepoch = Double.parseDouble( ep.substring( 0 ) );
        }
    }

/**
 * Set the input Equinox.
 * @param ep The epoch, eg. 1950 (optional B or J prefix)
 */
    public void setInEquinox ( String eq ) {
        if ( eq == null ) return;
        inJBeq = Character.toUpperCase( eq.charAt(0) );
        if ( inJBeq == 'B' || inJBeq == 'J' )
            inequinox = Double.parseDouble( eq.substring( 1 ) );
        else
            inequinox = Double.parseDouble( eq.substring( 0 ) );
    }

/**
 * Set the output System.
 * @param out The output system.
 * <p>Allowable systems are:
 * <dl>
 * <dt>4</dt> <dd>Mean [alpha, delta], old system, with E-terms
 *                (loosely FK4, usually B1950)</dd>
 * <dt>5</dt> <dd>Mean [alpha, delta], old system, no E-terms
 *                (some radio positions)</dd>
 * <dt>A</dt> <dd>Mean [alpha, delta], new system
 *                (loosely FK5, usually J2000) (default)</dd>
 * <dt>B</dt> <dd>Geocentric apparent [alpha, delta], new system</dd>
 * <dt>E</dt> <dd>Ecliptic coordinates [lamda, beta], new system (mean of date)</dd>
 * <dt>G</dt> <dd>Galactic coordinates [l, b], IAU 1958 system</dd>
 * </dl>
 * </p>
 */
    public void setOutSystem ( char out ) {
        outsys = Character.toUpperCase( out );
        switch ( outsys ) {
            case '4':
            case 'B':
            case 'G':
                outJB = outJBeq = 'B';
                outepoch = outequinox = 1950.0;
                break;
            case '5':
                outJB = outJBeq = 'J';
                outepoch = outequinox = 2000.0;
            case 'A':
            case 'E':
                break;
        }
    }

/**
 * Set the output Epoch.
 * @param ep The epoch, eg. 1984.53 or 1983 2 26.4
 */
    public void setOutEpoch ( String ep ) {
        if ( ep == null ) return;
        outJB = Character.toUpperCase( ep.charAt(0) );
        if ( outJB == 'B' || outJB == 'J' )
            outepoch = Double.parseDouble( ep.substring( 1 ) );
        else
            outepoch = Double.parseDouble( ep.substring( 0 ) );
    }

/**
 * Set the output Equinox.
 * @param ep The epoch, eg. 1950 (optional B or J prefix)
 */
    public void setOutEquinox ( String eq ) {
        if ( eq == null ) return;
        outJBeq = Character.toUpperCase( eq.charAt(0) );
        if ( outJBeq == 'B' || outJBeq == 'J' )
            outequinox = Double.parseDouble( eq.substring( 1 ) );
        else
            outequinox = Double.parseDouble( eq.substring( 0 ) );
    }

/**
 * Set High precision results.
 */
    public void setHigh ( ) { setPrecision ('H'); }
/**
 * Set Medium precision results.
 */
    public void setMedium ( ) { setPrecision ('M'); }
/**
 * Set Low precision results.
 */
    public void setLow ( ) { setPrecision ('L'); }
/**
 * Set the precision.
 * @param prec Precision of results, either 'H', 'M' (default) or 'L'
 */
    public void setPrecision ( String prec ) {
        setUnits( prec.charAt(0) );
    }
    public void setPrecision ( char prec ) {
        lowprec = false; mediumprec = false; highprec = false;
        switch ( Character.toUpperCase( prec ) ) {
            case 'H':
                highprec = true;
                pra = 5; prb = 4; prep = 3;
                prpx = 5; prrv = 3;
                pr1 = 8; pr2 = 7;
                break;
            case 'L':
                lowprec = true;
                pra = 1; prb = 0; prep = 1;
                prpx = 2; prrv = 1;
                pr1 = 2; pr2 = 1;
                break;
            case 'M':
            default:
                mediumprec = true;
                pra = 3; prb = 2; prep = 2;
                prpx = 3; prrv = 1;
                pr1 = 5; pr2 = 4;
                break;
        }
        nfep.setMinimumFractionDigits( prep );
        nfep.setMaximumFractionDigits( prep );
        nfep.setGroupingUsed( false );
        nf.setMaximumFractionDigits( pr1 );
        nf.setMinimumFractionDigits( pr1 );
        nf.setGroupingUsed( false );
        nf2.setMaximumFractionDigits( pr2 );
        nf2.setMinimumFractionDigits( pr2 );
        nf2.setGroupingUsed( false );
        nfpx.setMaximumFractionDigits( prpx );
        nfpx.setMinimumFractionDigits( prpx );
        nfpx.setGroupingUsed( false );
        nfrv.setMaximumFractionDigits( prrv );
        nfrv.setMinimumFractionDigits( prrv );
        nfrv.setGroupingUsed( false );
    }

/**
 * Set input and output angles to degrees format.
 */
    public void setDegrees ( ) { setUnits('D'); }
/**
 * Set input and output angles to hours format ( ie H M S ).
 */
    public void setHours ( ) { setUnits('H'); }
/**
 * Set the RA mode.
 * @param unit The RA Mode, either 'D' or 'H' (default).
 */
    public void setUnits ( String unit ) {
        setUnits( unit.charAt(0) );
    }
    public void setUnits ( char unit ) {
        if ( Character.toUpperCase( unit ) == 'D' ) {
            degrees = true;
            hours = false;
        } else if ( Character.toUpperCase( unit ) == 'H' ) {
            degrees = false;
            hours = true;
        }
    }

/**
 * Validate a coordinate string.
 * @param value The coordinate string.
 * @return The angle in the FK5 system (null if invalid)
 */
    public AngleDR validate( String value ) {
        String text = null;
        AngleDR ra, rb, rw;
        double a, b, w;
        printDebug( "validate: Coord: " + value);
        printDebug( "   System in: " + insys + " " + inJBeq + inequinox );
        printDebug( "   System out: " + outsys + " " + outJBeq + outequinox );
        if ( value.equals( "" ) ) {
            answer = null;
            return null;
        } else {
            text = value;
        }
        palString val = new palString( value + "\n");
        double d[] = new double[10];
        int jf[] = new int[10];

        if ( lowprec ) {
            setPrecision( 'L' );
        } else if ( mediumprec ) {
            setPrecision( 'M' );
        } else if ( highprec ) {
            setPrecision( 'H' );
        }

        if ( insys == '4' || insys == 'B' || insys == '5' || insys == 'A' ) {
            for ( int i=0; i < 10; i++ ) {
                double f = 0.0;
                f = pal.Dfltin( val, f );
                int j = pal.Status;
                d[i] = f;
                if ( i == 6 || i == 7 )
                    if ( val.getChar() == '"' ) {
                        j = -2; val.incrChar();
                    }
                jf[i] = j;
                if ( j > 1 ) {
                    printDebug( "   Flag " + i + " = " + j );
                    return null;
                }
                else if ( j != 1 ) {
                    count = i+1;
                }
            }
            printDebug ( "   Number of fields = " + count );
            if ( count < 2 ) return null;
            if ( count == 2 ) {
                jf[5] = 0;
                jf[4] = 0;
                d[3] = d[1];
                jf[3] = jf[1];
                d[1] = 0.0;
                jf[1] = 0;
                d[2] = 0.0;
                jf[2] = 0;
                count = 6;
            } else if ( degrees ) {
                return null;
            } else if ( count == 3 ) {
                return null;
            } else if ( count == 4 ) {
                jf[5] = 0;
                d[4] = d[3];
                jf[4] = jf[3];
                d[3] = d[2];
                jf[3] = jf[2];
                d[2] = 0.0;
                jf[2] = 0;
                count = 6;
            } else if ( count == 5 ) {
                jf[5] = 0;
                count = 6;
            }
            printDebug( "   Coarse Validity Check" );

/* Coarse Validity check */
            if ( jf[0] != 0 || jf[1] != 0 || jf[2] != 0 ||
                 jf[3] > 0 || jf[4] != 0 || jf[5] != 0 ||count == 7 ||
                 Math.abs( d[6] ) >= 15.0 || Math.abs( d[7] ) >= 15.0 ||
                 jf[8] < 0 || d[8] >= 1.0 || Math.abs( d[9] ) >= 200.0 ||
                 ( insys == 'A' && count > 6 ) ) return null;

            a = ( 60.0 * (60.0*d[0] + d[1] ) + d[2] ) * S2R;
            b = ( 60.0 * (60.0*Math.abs( d[3] ) + d[4] ) + d[5] ) * AS2R;
            if ( jf[3] < 0 ) b = -b;
            jz = jf[6];
            da = d[6] * S2R;
            db = d[7] * AS2R;
            px = d[8];
            rv = d[9];
            if ( degrees ) a = a / 15.0;
            if ( jz == -2 ) {
                ra = new AngleDR( da/15.0, db );
                rb = new AngleDR( 0.0, b );
                ra = pal.Dtp2s( ra, rb );
                da = pal.Drange( ra.getAlpha() );
                db = ra.getDelta() - b;
            } else {
                printDebug( "   Angle > 15.0" );
                if ( Math.cos(a) * Math.abs(da) / AS2R >= 15.0 ) return null;
            }
        } else {
/*           Decode ecliptic or galactic coordinates */
            w = 0.0;
            w = pal.Dfltin( val, w );
            if ( pal.Status > 0 ) return null;
            a = w * D2R;
            w = pal.Dfltin( val, w );
            if ( pal.Status > 0 ) return null;
            b = w * D2R;
            if ( Math.abs( w ) > 90.0 ) return null;
        }
/* Check nothing left in record */
        printDebug ( "   Check end of String " + val.getPos() + " " +
                      val.length() );
        if ( val.getPos() < val.length() ) return null;

/*        Normalise */
        ra = new AngleDR( a, b );
        double v[] = pal.Dcs2c( ra );
        ra = pal.Dcc2s( v );
        ra.setAlpha( pal.Dranrm ( ra.getAlpha() ) );

        ra = normalize ( ra );
        return ra;
    }

/**
 * Normalize the input angle to FK5.
 * @param ra The angle in specified input system.
 * @return The angle in the FK5 system
 */
    public AngleDR normalize( AngleDR ra ) {
/*        Report & convert to J2000 FK5 at specified epoch */
        double aw = ra.getAlpha();
        double bw = ra.getDelta();
        double dd[] = { da, db };
        double epb = pal.Epco( 'B', inJB, inepoch );
        double epb1 = pal.Epco( 'B', outJB, outepoch );
        double eqb = pal.Epco( 'B', inJBeq, inequinox );
        double epj = pal.Epco( 'J', inJB, inepoch );
        double epj1 = pal.Epco( 'J', outJB, outepoch );
        double eqj = pal.Epco( 'J', inJBeq, inequinox );
        String system;
        AngleDR rw, rb;
        printDebug ("normalize: Angle ra =" + ra );
        printDebug ("   inepoch = " + inepoch );
        printDebug ("   outepoch = " + outepoch );
        printDebug ("   inquinox = " + inequinox );

        if ( insys == '4' ) {
            system = "FK4";
            printDebug ( "   input system = " + system );
            if ( hours ) {
                printDebug ("   Hours");
                palTime ifA = pal.Dr2tf( aw );
                palTime ifB = pal.Dr2af( bw );
                WRITE( "  = " + ifA.toString( pra ) + " " +
                       ifB.printSign( ) + ifB.toString( prb ) + " "
                       + inJB + nfep.format(inepoch) + " "
                       + inJBeq + nfep.format(inequinox) + " "
                       + system + " " + nfpx.format(px) + " " + nfrv.format(rv) );
                if ( jz != 1 ) {
//                    DEBUG("Proper Motion");
                    WRITE( nf.format( da/S2R ) + " " + nf2.format( db/AS2R ) );
                }
            } else {
                printDebug ("   Degrees");
                WRITE( " =   " + nf.format(r2d(aw)) + " " + nf2.format(bw/D2R) + " " +
                       inJB + nfep.format(inepoch) + " " +
                       inJBeq + nfep.format(inequinox) + " " + system );
            }
            if ( jz != 1 ) {
/* Proper motion supplied */
                printDebug ("   Proper Motion");
                rw = pal.Pm( ra, dd, px, rv, epb, epb1 );
                ra = pal.Subet( rw, eqb );
                ra = pal.Preces( system, eqb, 1950.0, ra );
                rw = pal.Addet( ra, 1950.0 );
                ra = pal.Fk45z( rw, epb1 );
            } else {
                rb = pal.Subet( ra, eqb );
                ra = pal.Preces( system, eqb, 1950.0, rb );
                rb = pal.Addet( ra, 1950.0 );
                ra = pal.Fk45z( rb, epb );
            }

        } else if ( insys == 'B' ) {
            system = "FK4 (no E-terms)";
            printDebug ( "   Input system = " + system );
            if ( hours ) {
                printDebug ("   Hours");
                palTime ifA = pal.Dr2tf( aw );
                palTime ifB = pal.Dr2af( bw );
                WRITE( "  = " + ifA.toString( pra ) + " " +
                       ifB.printSign( ) + ifB.toString( prb ) + " " +
                       inJB + nfep.format(inepoch) + " " +
                       inJBeq + nfep.format(inequinox) + " " + system + " " +
                       nfpx.format(px) + " " + nfrv.format(rv) );
                if ( jz != 1 ) {
//                    DEBUG("Proper Motion");
                    WRITE( nf.format( da/S2R ) + " " + nf2.format( db/AS2R ) );
                }
            } else {
                printDebug ("   Degrees");
                WRITE( " =   " + nf.format(r2d( aw)) + " " + nf2.format(bw/D2R) + " " +
                       inJB + nfep.format(inepoch) + " " +
                       inJBeq + nfep.format(inequinox) + " " + system );
            }
            if ( jz != 1 ) {
/* Proper motion supplied */
                printDebug ("   Proper Motion");
                rw = pal.Pm(ra, dd, px, rv, epb, epb1);
//                ra = pal.Subet( rw, epb );
                ra = pal.Preces("FK4", eqb, 1950.0, rw );
                rw = pal.Addet( ra, 1950.0 );
                ra = pal.Fk45z( rw, epb1 );
            } else {
//                rb = pal.Subet( ra, epb );
                rw = pal.Preces("FK4", eqb, 1950.0, ra );
                rb = pal.Addet( rw, 1950.0 );
                ra = pal.Fk45z( rb, epb );
            }

        } else if ( insys == '5' ) {
            system = "FK5";
            printDebug ( "   Input system = " + system );
            if ( hours ) {
                printDebug ("   Hours");
                palTime ifA = pal.Dr2tf( aw );
                palTime ifB = pal.Dr2af( bw );
                WRITE( "  = " + ifA.toString( pra ) + " " +
                       ifB.printSign( ) + ifB.toString( prb ) + " " +
                       inJB + nfep.format(inepoch) + " " +
                       inJBeq + nfep.format(inequinox) + " " + system + " " +
                       nfpx.format(px) + " " + nfrv.format(rv) );
                if ( jz != 1 ) {
//                    DEBUG("Proper Motion");
                    WRITE( nf.format( da/S2R ) + " " + nf2.format( db/AS2R ) );
                }
            } else {
                printDebug ("   Degrees");
                WRITE( " =   " + nf.format(r2d(aw)) + " " + nf2.format(bw/D2R)
                       + " " + inJB + nfep.format(inepoch) + " " +
                       inJBeq + nfep.format(inequinox) + " " + system );
            }
/* Proper motion initial to final epoch */
            printDebug ("   Proper Motion");
            epj = pal.Epco( 'J', inJB, inepoch );
            eqj = pal.Epco( 'J', inJBeq, inequinox );
            rw = pal.Pm( ra, dd, px, rv, epj, epj1 );
            ra = pal.Preces( system, eqj, 2000.0, rw );

        } else if ( insys == 'A' ) {
            system = "geocentric apparent";
            printDebug ( "   Input system = " + system );
            mjDate ifD = null;
            double mjd = pal.Epj2d( epj );
            try {
                ifD = pal.Djcal( mjd );
            }
            catch ( palError e ) { }
            palTime ifA = pal.Dr2tf( aw );
            palTime ifB = pal.Dr2af( bw );
            if ( hours ) {
                printDebug ("   Hours");
                WRITE( "  = " + ifA.toString( pra ) + " " +
                       ifB.printSign( ) + ifB.toString( prb ) + " " +
                       ifD.toString( prrv ) + " " + system );
            } else {
                printDebug ("   Degrees");
                WRITE( " =   " + nf.format(r2d(aw)) + " " + nf2.format(bw/D2R)
                       + " " + ifD.toString( prrv ) + " " + system );
            }
/* Convert to J2000 FK5 */
            printDebug ( "   Convert to J2000 FK5: " + ra + mjd );
            ra = pal.Amp( ra, mjd, 2000.0 );
            printDebug ( "   Result " + ra );
        } else if ( insys == 'E' ) {
            system = "ecliptic";
            printDebug ( "   Input system = " + system );
            WRITE( "  = " + nf.format(r2d(aw)) + " " + nf2.format(bw/D2R) +
                   " " + inJB + nfep.format(inepoch) + " " + system );
/* Convert to J2000 FK5 */
            double dw = pal.Epj2d( epj );
            ra = pal.Ecleq( ra, dw );
        } else {
            system = "galactic (II)";
            printDebug ( "   Input system = " + system );
            WRITE( "  = " + nf.format(r2d(aw)) + " " + nf2.format(bw/D2R) +
                   " " + inJB + nfep.format(inepoch) + " " + system );
/* Convert to J2000 FK5 */
            Galactic g = new Galactic( ra.getAlpha(), ra.getDelta() );
            ra = pal.Galeq( g );
        }

        return ra;
    }

/**
 * Convert the angle into the output system.
 * @param ra The angle in the FK5 system.
 * @return The coordinates in the requested output system
 */
    public String convert( AngleDR ra )
    {
        if ( ra == null ) return "No Coordinate specified";
        double epb = pal.Epco('B', outJB, outepoch);
        double epj = pal.Epco('J', outJB, outepoch);
        double eqb = pal.Epco('B', outJBeq, outequinox);
        double eqj = pal.Epco('J', outJBeq, outequinox);


/*        Convert to B1950 FK4 without proper motion */
        String system;
        AngleDR rw;
        Stardata sd= pal.Fk54z( ra, epb );
        printDebug ( "convert: Angle = " + ra );
        printDebug ( "   inepoch = " + inepoch );
        printDebug ( "   outepoch = " + outepoch );
        printDebug ( "   outequinox = " + outequinox );


/*        Convert from J2000 FK5 to output system & report */
        if ( outsys == '4' ) {
            system = "FK4";
            printDebug ("   Output system = " + system);
/* Remove E-terms */
            ra = pal.Subet( sd.getAngle(), 1950.0 );
/* Precess to final epoch */
            rw = pal.Preces( "FK4", 1950.0, eqb, ra );
/* Add E-terms */
            ra = pal.Addet( rw, eqb );
        } else if ( outsys == 'B' ) {
            system = "FK4 (no E-terms)";
            printDebug ("   Output system = " + system);
/* Remove E-terms */
            rw = pal.Subet( sd.getAngle(), 1950.0 );
/* Precess to final epoch */
            ra = pal.Preces( "FK4", 1950.0, eqb, rw );
        } else if ( outsys == '5' ) {
            system = "FK5";
            printDebug ("   Output system = " + system);
/*        Convert to J2000 FK5 to output system & report */
            ra = pal.Preces( "FK5", 2000.0, eqj, ra );
        } else if ( outsys == 'A' ) {
            system = "geocentric apparent";
            printDebug ("   Ouput system = " + system);
            double pm[] = { 0.0, 0.0 };
            sd = new Stardata( ra, pm, px, 0.0 );
            ra = pal.Map( sd, 2000.0, pal.Epj2d( epj ) );
        } else if ( outsys == 'E' ) {
            system = "ecliptic";
            printDebug ("   Output system = " + system);
            ra = pal.Eqecl( ra, pal.Epj2d( epj ) );
        } else if ( outsys == 'G' ) {
/*           Galactic */
            system = "galactic (II)";
            printDebug ("   Output system = " + system);
            Galactic g = pal.Eqgal( ra );
            ra = new AngleDR ( g.getLongitude(), g.getLatitude() );
        } else
            return "Unknown Output System";

        printDebug ( "   Angle = " + ra );
        if ( hours ) {
            palTime ifA = pal.Dr2tf( ra.getAlpha() );
            palTime ifB = pal.Dr2af( ra.getDelta() );
            answer = ifA.toString( pra ) + " " +
                     ifB.printSign( ) + ifB.toString( prb );
            if ( outsys == '4' || outsys == '5' || outsys == 'B' ) {
                answer = answer  + " " + outJB + nfep.format(outepoch) + " " +
                     outJBeq + nfep.format(outequinox) + " " + system;
            } else if ( outsys == 'A' ) {
                try {
                    mjDate ifD = pal.Djcal( pal.Epj2d( epj ) );
                    answer = answer + " " + ifD.toString(prrv) + " " + system;
                }
                catch ( palError e ) { answer = "Invalid date"; }
            } else if ( outsys == 'E' || outsys == 'G' ) {
                rw = new AngleDR ( ra.getAlpha()/D2R, ra.getDelta()/D2R );
                answer = rw.toString( pr1 ) + " " + outJB +
                     nfep.format(outepoch) + " " + system;
            }
        } else {
            answer = nf.format( ra.getAlpha()/D2R ) +
                     " " + nf.format( ra.getDelta()/D2R ) +
                     " " + outJB + nfep.format(outepoch) +
                     " " + outJBeq + nfep.format(outequinox) + " " + system;
        }

        return answer;
    }

    private double r2d ( double angle ) {
        double u = (double) Math.pow(10, pr1);
        double c = 360.0*u;
        double v = Math.IEEEremainder( (int) (angle*R2D*u) , c);
        if ( v < 0.0 ) v += c;

        return v/u;
    }
    private void WRITE ( String s ) {
        if ( DEBUG ) System.out.println( s );
    }

    private void printDebug( String message ) {
        if ( DEBUG ) System.err.println( "[Coco Debug] " + message );
        return;
    }

}
