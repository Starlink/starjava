package uk.ac.starlink.table;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Describes Uniform Column Descriptors.
 * This class knows about all currently defined UCDs, the 
 * <a href="https://vizier.cds.unistra.fr/doc/UCD.htx">Unified Column
 * Descriptors</a> defined by the CDS.
 * Each UCD has a unique ID by which it is known, and a description 
 * giving a brief explanation of what it means. 
 * <p>
 * UCDs are obtained from the static {@link #getUCD} method; any 
 * two UCDs with the same ID are guaranteed to be the same object.
 *
 * <h2>Source of information</h2>
 * The UCD ids and descriptions are read from a list at the resource
 * {@link #UCD_DEFINITIONS_LOC}.  If this resource is unavailable at
 * runtime, a warning will be written to the logging system.
 * The original source of this text file was
 * the CDS <a href="https://vizier.cds.unistra.fr/viz-bin/UCDs">List of 
 * all UCDs</a>.
 *
 * @author   Mark Taylor (Starlink)
 * @see   <a href="https://vizier.cds.unistra.fr/doc/UCD.htx">Unified 
 *        Content Descriptors</a>
 */
public class UCD implements Comparable<UCD> {

    private static Map<String,UCD> ucdMap;
    private static List<UCD> ucdList;
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.table" );
    public static final String UCD_DEFINITIONS_LOC = 
        "/uk/ac/starlink/table/text/UCDs";

    private final String id_;
    private final String description_;

    /**
     * Private constructor.  This is only invoked by the
     * {@link #ensureInitialised} method.
     */
    private UCD( String id, String description ) {
        id_ = id;
        description_ = description;
    }

    /**
     * Returns the ID string of this UCD.
     *
     * @return  the ID string (capitals, underscores and numbers only)
     */
    public String getID() {
        return id_;
    }

    /**
     * Returns the textual description of the UCD.
     *
     * @return  a few words describing the meaning of this UCD
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Implements the {@link java.lang.Comparable} interface, comparing
     * alphabetically by ID.
     */
    public int compareTo( UCD other ) {
        return id_.compareTo( other.id_ );
    }

    /**
     * Returns the UCD object corresponding to a given UCD ID string.
     * Returns <code>null</code> if no UCD with the given name is known.
     *
     * @param  id  the string used to identify the UCD (it will have
     *             surrounding spaces trimmed)
     * @return  the UCD object corresponding to <code>id</code>, or 
     *          <code>null</code> if none can be found
     */
    public static UCD getUCD( String id ) {
        ensureInitialised();
        UCD ucd = ucdMap.get( id.trim() );
        return ucd;
    }

    /**
     * Returns an iterator over all the known UCDs.
     * The iterator returns the UCDs in their natural order (alphabetic
     * by ID).
     *
     * @return  an Iterator which iterates over all the existing
     *          <code>UCD</code> objects
     */
    public static Iterator<UCD> getUCDs() {
        ensureInitialised();
        return ucdList.iterator();
    }

    /**
     * Returns the UCD id string.
     *
     * @return   a string representation of this UCD
     */
    public String toString() {
        return id_;
    }

    /**
     * Reads the text list of all defined UCDs to populate the lookup
     * table used by the <code>getUCD</code> method.
     */
    private static void ensureInitialised() {

        /* If this has already been done, no work is now required. */
        if ( ucdMap != null ) {
            return;
        }

        /* Otherwise, populate the map. */
        ucdMap = new HashMap<String,UCD>();
        ucdList = new ArrayList<UCD>();
        InputStream strm = UCD.class.getResourceAsStream( UCD_DEFINITIONS_LOC );
        if ( strm == null ) {
            logger.warning( "No resource " + UCD_DEFINITIONS_LOC 
                          + " was found" );
            return;
        }
        try {
            BufferedReader rdr = 
                new BufferedReader( new InputStreamReader( strm ) );
            Pattern pat = 
                Pattern.compile( "^\\s*([A-Z][^<>\\s]+)\\s+(.*?)\\s*$" );
            for ( String line; ( line = rdr.readLine() ) != null; ) {
                Matcher mat = pat.matcher( line );
                if ( mat.matches() ) {
                    String id = unEscape( mat.group( 1 ) );
                    String description = unEscape( mat.group( 2 ) );
                    UCD ucd = new UCD( id, description );
                    ucdMap.put( id, ucd );
                    ucdList.add( ucd );
                }
            }
            Collections.sort( ucdList );
        }
        catch ( IOException e ) {
            logger.warning( "Trouble reading " + UCD_DEFINITIONS_LOC 
                          + ": " + e );
        }
    }

    /**
     * Returns the plain text version of an HTML string.  This does not
     * attempt to be comprehensive, but copes with the characters we
     * expect to get.
     *
     * @param  text  the HTML-like input string
     * @return  a plain-text equivalent of <code>text</code>
     */
    private static String unEscape( String text ) {
        return text
              .replaceAll( "&lt;", "<" )
              .replaceAll( "&gt;", ">" );
    }
 
}
