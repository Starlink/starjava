package uk.ac.starlink.table;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Describes Uniform Column Descriptors.
 * This class knows about all currently defined UCDs, the 
 * <a href="http://vizier.u-strasbg.fr/doc/UCD.htx">Unified Column
 * Descriptors</a> defined by the CDS.
 * Each UCD has a unique ID by which it is known, and a description 
 * giving a brief explanation of what it means. 
 * <p>
 * UCDs are obtained from the static {@link #getUCD} method; any 
 * two UCDs with the same ID are guaranteed to be the same object.
 *
 * <h3>Source of information</h3>
 * The UCD ids and descriptions are read from a list at the resource
 * {@link #UCD_DEFINITIONS_LOC}.  If this resource is unavailable at
 * runtime, a warning will be written to the logging system.
 * The original source of this text file was
 * the CDS <a href="http://vizier.u-strasbg.fr/viz-bin/UCDs">List of 
 * all UCDs</a>.
 *
 * @author   Mark Taylor (Starlink)
 */
public class UCD {

    private static Map ucdMap;
    private static Logger logger = Logger.getLogger( "uk.ac.starlink.table" );
    public static final String UCD_DEFINITIONS_LOC = 
        "/uk/ac/starlink/table/text/UCDs";

    private final String id;
    private final String description;

    /**
     * Private constructor.  This is only invoked by the <tt>populateMap</tt>
     * method.
     */
    private UCD( String id, String description ) {
        this.id = id;
        this.description = description;
    }

    /**
     * Returns the ID string of this UCD.
     *
     * @return  the ID string (capitals, underscores and numbers only)
     */
    public String getID() {
        return id;
    }

    /**
     * Returns the textual description of the UCD.
     *
     * @return  a few words describing the meaning of this UCD
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the UCD object corresponding to a given UCD ID string.
     * Returns <tt>null</tt> if no UCD with the given name is known.
     *
     * @param  id  the string used to identify the UCD (it will have
     *             surrounding spaces trimmed)
     * @return  the UCD object corresponding to <tt>id</tt>, or 
     *          <tt>null</tt> if none can be found
     */
    public static UCD getUCD( String id ) {
        if ( ucdMap == null ) {
            populateMap();
        }
        UCD ucd = (UCD) ucdMap.get( id.trim() );
        return ucd;
    }

    /**
     * Returns the UCD id string.
     *
     * @return   a string representation of this UCD
     */
    public String toString() {
        return id;
    }

    /**
     * Reads the text list of all defined UCDs to populate the lookup
     * table used by the <tt>getUCD</tt> method.
     */
    private static void populateMap() {
        ucdMap = new HashMap();
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
                    String id = mat.group( 1 );
                    String description = mat.group( 2 );
                    ucdMap.put( id, new UCD( id, description ) );
                }
            }
        }
        catch ( IOException e ) {
            logger.warning( "Trouble reading " + UCD_DEFINITIONS_LOC 
                          + ": " + e );
        }
    }
 
}
