package uk.ac.starlink.ecsv;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the part of an ECSV file preceding the actual data lines.
 *
 * @author   Mark Taylor
 * @since    28 Apr 2020
 */
public class EcsvHeader {

    private final String[] yamlLines_;
    private final String namesLine_;
    private static final Charset UTF8 = Charset.forName( "UTF-8" );
    private static final Pattern MAGIC_REGEX = 
        Pattern.compile( "#\\s+%ECSV\\s+[0-9]+[.][0-9]+\\s*" );
    private static final Pattern YAMLSTART_REGEX =
        Pattern.compile( "---\\s*" );

    /**
     * Constructor.
     *
     * @param   yamlLines   lines of YAML containing header information;
     *                      these lines exclude the leading "# " characters
     *                      found in the ECSV file
     * @param   namesLine   first non-YAML line in the ECSV file,
     *                      which is supposed to contain a redundant list
     *                      of column names
     */
    public EcsvHeader( String[] yamlLines, String namesLine ) {
        yamlLines_ = yamlLines;
        namesLine_ = namesLine;
    }

    /**
     * Returns the lines of YAML containing header information.
     * Leading "# " characters are not included.
     *
     * @return  YAML text as array of lines
     */
    public String[] getYamlLines() {
        return yamlLines_;
    }

    /**
     * Returns the CSV-formatted line supposed to contain column names.
     *
     * @return  column names line
     */
    public String getNamesLine() {
        return namesLine_;
    }

    /**
     * Constructs an EcsvHeader instance by reading lines from a file.
     *
     * @param  rdr   line reader returning input lines
     * @return   header instance
     */
    public static EcsvHeader readHeader( LineReader rdr )
            throws IOException, EcsvFormatException {

        /* Read first line and check it's the ECSV magic number. */
        String line0 = rdr.readLine();
        if ( ! isMagic( line0.getBytes( UTF8 ) ) ) {
            throw new EcsvFormatException( "No ECSV identifier line" );
        }

        /* Read and store lines until we find one without a leading "# ". */
        boolean isStarted = false;
        List<String> yamlLines = new ArrayList<>();
        for ( String line; ( line = rdr.readLine() ) != null; ) {

            /* Comment line - continue to parse header. */
            if ( line.length() >= 2 &&
                 line.charAt( 0 ) == '#' && line.charAt( 1 ) == ' ' ) {
                String content = line.substring( 2 );

                /* If we're accumulating lines, store this one. */
                if ( isStarted ) { 
                    yamlLines.add( content );
                }

                /* Otherwise tread water until we find the "---" intro sequence.
                 * It probably should be immediately after the magic number,
                 * but allow intermediate comment lines if there are some. */
                else if ( YAMLSTART_REGEX.matcher( content ).matches() ) {
                    yamlLines.add( content );
                    isStarted = true;
                }
            }

            /* Single '#' only - not strictly legal since it ought to
             * have a space after it, but if it's there just tolerate
             * and ignore it. */
            else if ( line.length() == 1 && line.charAt( 0 ) == '#' ) {
            }

            /* Non-comment line: return based on what we've found so far. */
            else {
                if ( isStarted ) {
                    return new EcsvHeader( yamlLines.toArray( new String[ 0 ] ),
                                           line );
                }
                else {
                    throw new EcsvFormatException( "No YAML --- delimiter" );
                }
            }
        }
        throw new EcsvFormatException( "No post-YAML ECSV content" );
    }

    /**
     * Indicates whether a byte sequence indicates the start of an ECSV file.
     *
     * @param   intro  first few bytes of a file
     * @return  true iff file looks like ECSV
     */
    public static boolean isMagic( byte[] intro ) {
        return MAGIC_REGEX.matcher( new String( intro, UTF8 ) ).lookingAt();
    }
}
