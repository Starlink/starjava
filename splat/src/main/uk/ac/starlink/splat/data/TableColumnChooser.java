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
 * extended as needed. We prefer columns that have recognised model utypes
 * (SSAP or SLAP) over picking a column just using the name.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class TableColumnChooser
{
    /** The instance of this class */
    private static TableColumnChooser instance = null;

    /** Patterns for the names of column containing the coordinates */
    private static ArrayList coordNamePatterns = null;

    /** Patterns for the utypes of column containing the coordinates */
    private static ArrayList coordUtypePatterns = null;

    /** Patterns for the names of column containing the data values */
    private static ArrayList dataNamePatterns = null;

    /** Patterns for the utypes of column containing the data values */
    private static ArrayList dataUtypePatterns = null;

    /** Patterns for the names of column containing the data errors */
    private static ArrayList errorNamePatterns = null;

    /** Patterns for the utypes of column containing the data errors */
    private static ArrayList errorUtypePatterns = null;

    /** Patterns for the names of column containing the line identifier labels */
    private static ArrayList labelNamePatterns = null;

    /** Patterns for the utypes of column containing the line identifier labels */
    private static ArrayList labelUtypePatterns = null;

    /** Flags used for any pattern matching */
    private static final int flags = Pattern.CASE_INSENSITIVE;

    /** Singleton class */
    private TableColumnChooser()
    {
        //  Default patterns for column names and utypes.
        coordNamePatterns = new ArrayList();
        addCoordNamePattern( "wave.*" );
        addCoordNamePattern( "freq.*" );
        addCoordNamePattern( "velo.*" );
        addCoordNamePattern( "redshift.*" );
        addCoordNamePattern( "pos.*" );
        addCoordNamePattern( "x*." );
        addCoordNamePattern( "time*." );

        coordUtypePatterns = new ArrayList();
        addCoordUtypePattern( ".*spectralaxis.*" );       // SSAP
        addCoordUtypePattern( ".*timeaxis.*" );       // LightCurves
        addCoordUtypePattern( ".*line\\.wavelength" );     // SLAP

        dataNamePatterns = new ArrayList();
        addDataNamePattern( "flux.*" );
        addDataNamePattern( "inten.*" );
        addDataNamePattern( "temp.*" );
        addDataNamePattern( "mag.*" );
        addDataNamePattern( "energy.*" );
        addDataNamePattern( "y.*" );

        dataUtypePatterns = new ArrayList();
        addDataUtypePattern( ".*fluxaxis.*" );           // SSAP

        errorNamePatterns = new ArrayList();
        addErrorNamePattern( "error.*" );
        addErrorNamePattern( "sigma.*" );
        addErrorNamePattern( "stddev.*" );

        errorUtypePatterns = new ArrayList();
        addErrorUtypePattern( ".*fluxaxis\\.accuracy.*" ); // SSAP

        labelNamePatterns = new ArrayList();
        addLabelNamePattern( "identifiers.*" );
        addLabelNamePattern( "label.*" );
        addLabelNamePattern( "name.*" );
       

        labelUtypePatterns = new ArrayList();
        addLabelUtypePattern( ".*line\\.title" );             // SLAP
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
        //  Check for utypes then column names.
        int column = matchInfo( infos, coordUtypePatterns, null );
        if ( column == -1 ) {
            column = matchName( coordNamePatterns, names );
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
        //  Check for utypes then column names.
        int column = matchInfo( infos, dataUtypePatterns, errorUtypePatterns );
        if ( column == -1 ) {
            column = matchName( dataNamePatterns, names );
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
        //  Check for utypes then column names.
        int column = matchInfo( infos, errorUtypePatterns, null );
        if ( column == -1 ) {
            column = matchName( errorNamePatterns, names );
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
        //  Check for utypes then column names.
        int column = matchInfo( infos, labelUtypePatterns, null );
        if ( column == -1 ) {
            column = matchName( labelNamePatterns, names );
        }
        return column;
    }

    /**
     * Match the column info utypes against the recognised utype patterns
     * optionally excluding another set of patterns, and retaining the
     * column with UCD meta.main when there are more than one match.
     * Returns the index of the matched column or -1.
     */
    protected int matchInfo( ColumnInfo[] infos, ArrayList utypes,
                             ArrayList notutypes )
    {
        int result = -1;
        if ( utypes == null ) {
            //  Nothing to match, so nothing to do.
            return result;
        }

        String utype = null;
        String ucd = null;
        Pattern p = null;
        int size = utypes.size();
        ColumnInfo ns[] = new ColumnInfo[1];
        for ( int i = 0; i < size; i++ ) {
            p = (Pattern) utypes.get( i );
            for( int k = 0; k < infos.length; k++ ) {
                utype = infos[k].getUtype();
                if ( utype != null ) {
                    utype = utype.toLowerCase();
                    if ( p.matcher( utype ).matches() ) {

                        //  Exclude notutypes if given.
                        ns[0] = infos[k];
                        if ( matchInfo( ns, notutypes, null ) == -1 ) {

                            // Possible match. Keep this one if this has a UCD
                            // that includes "meta.main", that supercedes
                            // previous matches.
                            ucd = infos[k].getUCD();
                            if ( result == -1 ) {
                                result = k;
                            }
                            else {
                                if ( ucd != null && ucd.indexOf( "meta.main" ) > 0 ) {
                                    result = k;
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Compares the Patterns stored in list against the strings stored in
     * names and returns the index of the first match in names, otherwise
     * returns -1.
     */
    protected int matchName( ArrayList list, String[] names )
    {
        int size = list.size();
        Pattern p = null;
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

    public void addCoordNamePattern( String pattern )
    {
       coordNamePatterns.add( Pattern.compile( pattern, flags ) );
    }

    public void addCoordUtypePattern( String pattern )
    {
       coordUtypePatterns.add( Pattern.compile( pattern, flags ) );
    }

    public void addDataNamePattern( String pattern )
    {
       dataNamePatterns.add( Pattern.compile( pattern, flags ) );
    }

    public void addDataUtypePattern( String pattern )
    {
       dataUtypePatterns.add( Pattern.compile( pattern, flags ) );
    }

    public void addErrorNamePattern( String pattern )
    {
        errorNamePatterns.add( Pattern.compile( pattern, flags ) );
    }

    public void addErrorUtypePattern( String pattern )
    {
        errorUtypePatterns.add( Pattern.compile( pattern, flags ) );
    }

    public void addLabelNamePattern( String pattern )
    {
        labelNamePatterns.add( Pattern.compile( pattern, flags ) );
    }

    public void addLabelUtypePattern( String pattern )
    {
        labelUtypePatterns.add( Pattern.compile( pattern, flags ) );
    }
}
