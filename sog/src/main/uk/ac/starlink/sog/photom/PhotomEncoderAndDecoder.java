/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     06-NOV-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.sog.photom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Double;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;

/**
 * Utility class for AUTOPHOTOM specific encodings and decodings.
 * <p>
 * It saves and restores a PhotomList from a AUTOPHOTOM input and
 * output file and returns a String representation of a
 * PhotometryGlobals suitable for using as a configuration string.
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public class PhotomEncoderAndDecoder 
{
    // Notes: could be refactored into an interface for other
    // photometry systems?

    // Static classes, so no instance.
    private PhotomEncoderAndDecoder() {}

    /**
     * Store the configuration of a PhotomList instance in a file
     * suitable for processing by AUTOPHOTOM.
     */
    public static void save( PhotomList list, File file )
        throws FileNotFoundException
    {
        PrintStream p = new PrintStream( new FileOutputStream( file ) );

        // Assume AnnulusPhotom objects for now, but will need to deal
        // with detached apertures too.
        AnnulusPhotom bp;
        for ( int i = 0; i < list.size(); i++ ) {
            bp = (AnnulusPhotom) list.get( i );
            p.print( bp.getIdent() + " " );
            p.print( bp.getXcoord() + " " );
            p.print( bp.getYcoord() + " " );
            p.print( bp.getMagnitude() + " " );
            p.print( bp.getMagnitudeError() + " " );
            p.print( bp.getSky() + " " );
            p.print( bp.getSignal() + " " );
            p.print( bp.getStatus() + " " );

            // Need semi-major and eccentricity
            double sma = bp.getSemimajor();
            double smi = bp.getSemiminor();
            p.print( sma + " " );
            p.print( Math.sqrt(1.0-(smi*smi)/(sma*sma)) + " " );
            p.print( bp.getAngle() );
            p.print( " annulus circle" );
            p.println();

            p.print( "#ANN " + bp.getIdent() + " " );
            p.print(  bp.getInnerscale() + " " + bp.getOuterscale() );
            p.println();
        }
        p.close();
    }
    
    /**
     * Decode a results file from AUTOPHOTOM and add the new apertures
     * to a PhotomList instance.
     */
    public static void read( File file, PhotomList list ) 
        throws FileNotFoundException, IOException
    {
        System.out.println( "reading file: " + file );
        BufferedReader in = new BufferedReader( new FileReader( file ) );

        // Loop over non-blank lines. If line starts with # it is
        // either a comment or a sky region spec. Sky region specs
        // start with "#SKY" or "#ANN". At present we only deal with #SKY.
        String line = null;
        LinkedHashMap hashMap = new LinkedHashMap();
        AnnulusPhotom ap = null;
        while ( ( line = in.readLine() ) != null ) {

            // Skip blank and comment lines.
            if ( line.length() == 0 ) {
                continue;
            }

            // Split line into space separated words.
            String[] words = line.split( "[\\ ]+" );

            if ( words[0].equals( "#" ) ) {
                continue;
            }

            // Two types of lines expected, aperture and sky
            // (annulus / other regions).
            if ( words[0].equals( "#ANN" ) ) {
                ap = getPhotom( hashMap, words[1] );
                ap.setInnerscale( Double.parseDouble( words[2] ) );
                ap.setOuterscale( Double.parseDouble( words[3] ) );
            }
            else if ( words[0].equals( "#SKY" ) ) {
                // Not supported.
            }
            else {
                //  Ordinary line.
                ap = getPhotom( hashMap, words[0] );

                ap.setXcoord( Double.parseDouble( words[1] ) );
                ap.setYcoord( Double.parseDouble( words[2] ) );

                ap.setMagnitude( Double.parseDouble( words[3] ) );
                ap.setMagnitudeError( Double.parseDouble( words[4] ) );
                ap.setSky( Double.parseDouble( words[5] ) );
                ap.setSignal( Double.parseDouble( words[6] ) );
                ap.setStatus( words[7] );

                // Need semi-major and semi-minor.
                double semimajor = Double.parseDouble( words[8] );
                double eccen = Double.parseDouble( words[9] );
                ap.setSemimajor( semimajor );
                ap.setSemiminor( semimajor*Math.sqrt( 1.0 - eccen * eccen ) );
                ap.setAngle( Double.parseDouble( words[10] ) );
            }
        }

        // Now that all apertures are configured add them to the
        // PhotomList. 
        list.add( hashMap.values() );
        in.close();
    }

    /**
     * Either retrive an existing AnnulusPhotom object from a
     * Map that contains the current set, or create a new one
     * indexed by the given identifier.
     */
    public static AnnulusPhotom getPhotom( Map map, String ident )
    {
        AnnulusPhotom ap = (AnnulusPhotom) map.get( ident );
        if ( ap == null ) {
            ap = new AnnulusPhotom();
            ap.setIdent( Integer.parseInt( ident ) );
            map.put( ident, ap );
        }
        return ap;
    }


    /**
     * Convert a PhotometryGlobals into a String that can be used
     * when running the AUTOPHOTOM application.
     */
    public static String toApplicationString( PhotometryGlobals globals )
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "skymag=" + globals.getZeroPoint() );
        buffer.append( " centro=" + globals.getCentroid() );
        return buffer.toString();
    }
}
