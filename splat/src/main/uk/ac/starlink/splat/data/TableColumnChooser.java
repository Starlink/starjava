/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-FEB-2004 (Peter W. Draper):
 *        Original version.
 */

package uk.ac.starlink.splat.data;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import uk.ac.starlink.table.ColumnInfo;

/**
 * Centralising class for making the choices about what are the coordinates,
 * data values and data errors columns in tables.
 * <p>
 * The choices are controlled by a series of regular expressions that can be
 * extended as needed, or by looking for the standard set of utypes defined
 * by the IVOA spectral data model (1.0).
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class TableColumnChooser
{
    /** The instance of this class */
    private static TableColumnChooser instance = null;

    /** Patterns for the column containing the coordinates */
    private static ArrayList coordPatterns = null;

    /** Patterns for the column containing the data values */
    private static ArrayList dataPatterns = null;

    /** Patterns for the column containing the data errors */
    private static ArrayList errorPatterns = null;

    /** Patterns for the column containing the line identifier labels */
    private static ArrayList labelPatterns = null;

    /** Flags used for any pattern matching */
    private static final int flags = Pattern.CASE_INSENSITIVE;

    /** Singleton class */
    private TableColumnChooser()
    {
        //  Default patterns.
        coordPatterns = new ArrayList();
        addCoordPattern( "wavelength.*" );
        addCoordPattern( "freq.*" );
        addCoordPattern( "velo.*" );
        addCoordPattern( "redshift.*" );
        addCoordPattern( "pos.*" );
        addCoordPattern( "x*." );

        dataPatterns = new ArrayList();
        addDataPattern( "flux.*" );
        addDataPattern( "inten.*" );
        addDataPattern( "temp.*" );
        addDataPattern( "mag.*" );
        addDataPattern( "energy.*" );
        addDataPattern( "y.*" );

        errorPatterns = new ArrayList();
        addErrorPattern( "error.*" );
        addErrorPattern( "sigma.*" );
        addErrorPattern( "stddev.*" );

        labelPatterns = new ArrayList();
        addLabelPattern( "identifiers.*" );
        addLabelPattern( "label.*" );
        addLabelPattern( "name.*" );
   }

    /**
     * Get reference to the single instance of this class.
     */
    public static TableColumnChooser getInstance()
    {
        if ( instance == null ) {
            instance = new TableColumnChooser();
        }
        return instance;
    }

    /**
     * Return the index of the coordinate column of the table.
     * Matches either the spectral data model utype, or the current set of 
     * patterns.
     * Returns -1 if no match is located.
     */
    public int getCoordMatch( ColumnInfo[] infos, String[] names )
    {
        //  First check column infos for the IVOA spectral data model utype.
        int column = matchUtype( infos, "data.spectralaxis.value" );
        if ( column == -1 ) {

            // SLAP utype.
            column = matchUtype( infos, "line.wavelength" );
            if ( column == -1 ) {
                column = match( coordPatterns, names );
            }
        }
        return column;
    }
    
    /**
     * Return the index of the data values column of the table.
     * Matches either the spectral data model utype, or the current set of 
     * patterns.
     * Returns -1 if no match is located.
     */
    public int getDataMatch( ColumnInfo[] infos, String[] names )
    {
        //  First check column infos for the IVOA spectral data model utype.
        int column = matchUtype( infos, "data.fluxaxis.value" );
        if ( column == -1 ) {
            column = match( dataPatterns, names );
        }
        return column;
    }
    
    /**
     * Return the index of the data errors column of the table.
     * Matches either the spectral data model utypes, or the current set of 
     * patterns.
     * Returns -1 if no match is located.
     */
    public int getErrorMatch( ColumnInfo[] infos, String[] names )
    {
        //  First check column infos for the IVOA spectral data model utypes.
        int column = matchUtype( infos, "data.fluxaxis.accuracy.staterror" );
        if ( column == -1 ) {
            column = matchUtype( infos, "data.fluxaxis.accuracy.staterrlow" );
            if ( column == -1 ) {
                column = matchUtype( infos, 
                                     "data.fluxaxis.accuracy.staterrhigh" );
                if ( column == -1 ) {
                    column = match( errorPatterns, names );
                }
            }
        }
        return column;
    }

    /**
     * Return the index of the line identifier labels column.
     * Matches either the spectral line data model utypes? 
     * or the current set of patterns.
     * Returns -1 if no match is located.
     */
    public int getLabelMatch( ColumnInfo[] infos, String[] names )
    {
        //  First check column infos for the IVOA spectral data model utypes.
        int column = matchUtype( infos, "Line.title" );
        if ( column == -1 ) {
            column = match( labelPatterns, names );
        }
        return column;
    }

    /** 
     * Match the utype to that of the spectral data model. Returns the index
     * of the matched column or -1. Note modelUtype should be lower case and
     * only have the trailing context (no ssa: or Spectrum.).
     */
    protected int matchUtype( ColumnInfo[] infos, String modelUtype )
    {
        String utype;
        for( int k = 0; k < infos.length; k++ ) {
            utype = infos[k].getUtype();
            if ( utype != null ) {
                utype = utype.toLowerCase();
                if ( utype.endsWith( modelUtype ) ) {
                    return k;
                }
            }
        }
        return -1;
    }

    
    /**
     * Compares the Patterns stored in list against the strings stored in
     * names and returns the index of the first match in names, otherwise
     * returns -1.
     */
    protected int match( ArrayList list, String[] names )
    {
        int size = list.size();
        Pattern p = null;
        Matcher m = null;
        for ( int i = 0; i < size; i++ ) {
            p = (Pattern) list.get( i );
            for ( int j = 0; j < names.length; j++ ) {
                if ( p.matcher( names[j] ).matches() ) {
                    return j;
                }
            }
        }
        return -1;
    }

    /** Add a new regular expression pattern for matching coordinates.*/
    public void addCoordPattern( String pattern ) 
    {
       coordPatterns.add( Pattern.compile( pattern, flags ) );
    }

    /** Insert a new regular expression pattern for matching coordinates.*/
    public void addCoordPattern( String pattern, int index ) 
    {
       coordPatterns.add( index, Pattern.compile( pattern, flags ) );
    }

    /** Add a new regular expression pattern for matching data values.*/
    public void addDataPattern( String pattern ) 
    {
       dataPatterns.add( Pattern.compile( pattern, flags ) );
    }

    /** Insert a new regular expression pattern for matching data values.*/
    public void addDataPattern( String pattern, int index ) 
    {
       dataPatterns.add( index, Pattern.compile( pattern, flags ) );
    }

    /** Add a new regular expression pattern for matching data errors.*/
    public void addErrorPattern( String pattern ) 
    {
        errorPatterns.add( Pattern.compile( pattern, flags ) );
    }

    /** Insert a new regular expression pattern for matching data errors.*/
    public void addErrorPattern( String pattern, int index ) 
    {
        errorPatterns.add( index, Pattern.compile( pattern, flags ) );
    }

    /** Add a new regular expression pattern for matching labels.*/
    public void addLabelPattern( String pattern ) 
    {
        labelPatterns.add( Pattern.compile( pattern, flags ) );
    }

    /** Insert a new regular expression pattern for matching labels.*/
    public void addLabelPattern( String pattern, int index ) 
    {
        labelPatterns.add( index, Pattern.compile( pattern, flags ) );
    }
}
