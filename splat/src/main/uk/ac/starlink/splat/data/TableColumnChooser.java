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

/**
 * Centralising class for making the choices about what are the coordinates,
 * data values and data errors columns in tables.
 * <p>
 * The choices are controlled by a series of regular expressions that can be
 * extended as needed.
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

    /** Flags used for any pattern matching */
    private static final int flags = Pattern.CASE_INSENSITIVE;

    /** Singleton class */
    private TableColumnChooser()
    {
        //  Default patterns... Could try matching to things like data
        //  source. Any joy from UCDs and IVOA work?
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
        addErrorPattern( "stddev.*" );
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
     * Return the index of the String that most matches a coordinate.
     * Returns -1 if no match is located.
     */
    public int getCoordMatch( String[] names )
    {
        return match( coordPatterns, names );
    }
    
    /**
     * Return the index of the String that most matches a data value.
     * Returns -1 if no match is located.
     */
    public int getDataMatch( String[] names )
    {
        return match( dataPatterns, names );
    }
    
    /**
     * Return the index of the String that most matches a data error.
     * Returns -1 if no match is located.
     */
    public int getErrorMatch( String[] names )
    {
        return match( errorPatterns, names );
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
}
