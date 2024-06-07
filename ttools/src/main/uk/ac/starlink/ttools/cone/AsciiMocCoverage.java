package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import cds.moc.HealpixMoc;

/**
 * MOC coverage implementation which uses the ASCII serialization.
 *
 * @author   Mark Taylor
 * @since    29 Nov 2023
 * @see  <a href="https://www.ivoa.net/documents/MOC/">MOC v2.0 sec 4.2.3</a>
 */
public class AsciiMocCoverage extends MocCoverage {

    private final String asciiMoc_;

    /**
     * Constructor.
     *
     * @param  asciiMoc  MOC encoded using the ASCII MOC serialization
     */
    public AsciiMocCoverage( String asciiMoc ) {
        asciiMoc_ = asciiMoc;
    }

    @Override
    protected HealpixMoc createMoc() throws IOException {
        try {
            return new HealpixMoc( asciiMoc_ );
        }
        catch ( Exception e ) {
            throw new IOException( "MOC ASCII format error", e );
        }
    }

    /**
     * Test whether a string is apparently a (spatial) ASCII MOC
     * as described in sec 4.3.2 of MOC 2.0.
     *
     * @param  txt  string to test
     * @return  true if text looks like it could be a spatial MOC
     * @see  <a href="https://www.ivoa.net/documents/MOC/20220727/REC-moc-2.0-20220727.html#tth_sEc4.3.2"
     *          >MOC 2.0 sec 4.3.2</a>
     */
    public static boolean looksLikeAsciiMoc( String txt ) {
        if ( txt == null || txt.length() == 0 ) {
            return false;
        }

        /* I initially tried to use a single regular expression for matching
         * against a test MOC string to implement this functionality.
         * Although I could write a regex that was correct, it was prone to
         * generate StackOverflowErrors when confronted with a long MOC string.
         * It might be possible to avoid this using e.g. possessive quantifiers
         * in the regex, but I couldn't get that working. */

        /* This implementation is pretty inefficient; the test string
         * might easily be hundreds of tokens long or more, and I'm
         * splitting it up into lots of little strings.  The patterns
         * are not pre-compiled.  An iterator over tokens e.g. using
         * Matcher.find would be more efficient. */
        String[] regions = txt.replaceFirst( "^\\s*s\\s*", "" )
                              .split( "\\s*[0-9]+/\\s*", 0 );
        for ( String region : regions ) {
            if ( region.length() > 0 ) {
                String[] words = region.split( "\\s+", 0 );
                for ( String word : words ) {
                    if ( word.length() > 0 &&
                         ! word.matches( "[0-9]+(-[0-9]+)?" ) ) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
