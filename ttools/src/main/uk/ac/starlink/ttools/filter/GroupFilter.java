package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.RowRunner;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.task.TableGroup;

/**
 * Filter version of TGROUP task.
 *
 * @author   Mark Taylor
 * @since    18 Nov 2022
 */
public class GroupFilter extends BasicFilter {

    private final char delimChr_ = '@';

    /**
     * Constructor.
     */
    public GroupFilter() {
        super( "group", "[-[no]parallel] <key> [<key> ...] [<aggcol> ...]" );
    }

    public String[] getDescriptionLines() {
        return new String[] {
            "<p>Calculates aggregate functions on groups of rows.",
            "This does the same job as a",
            "<code>SELECT ... GROUP BY</code> statement",
            "with aggregate functions in ADQL/SQL.",
            "</p>",
            "<p>The functionality is identical to that of the",
            "<ref id='tgroup'><code>tgroup</code></ref> command,",
            "and the meaning and syntax of the <code>&lt;key&gt;</code> and",
            "<code>&lt;aggcol&gt;</code> words are identical too,",
            "except that the <code>&lt;aggcol&gt;</code> delimiter character",
            "is " + describeChar( delimChr_ ),
            "rather than " + describeChar( TableGroup.AGGCOL_DELIM ) + ",",
            "so a group entry count can be added for instance using",
            "an <code>&lt;aggcol&gt;</code> like",
            "\"<code>null" + delimChr_ + "count</code>\".",
            "The <code>&lt;aggcol&gt;</code> arguments",
            "are distinguished by the fact that they contain",
            "at least one delimiter (\"<code>" + delimChr_ + "</code>\").",
            "See the <ref id='tgroup'><code>tgroup</code></ref> documentation",
            "for a full explanation of the syntax and functionality.",
            "</p>",
            "<p>The syntax here is rather cramped,",
            "so in many cases it will be more comfortable to use",
            "<code>tgroup</code> instead,",
            "but this filter is provided for cases where that may be",
            "more convenient.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {

        /* Parse arguments. */
        List<String> keyList = new ArrayList<>();
        List<TableGroup.AggSpec> aggList = new ArrayList<>();
        boolean isParallel = true;
        while ( argIt.hasNext() ) {
            String arg = argIt.next();
            if ( "-parallel".equals( arg ) ) {
                isParallel = true;
                argIt.remove();
            }
            else if ( "-noparallel".equals( arg ) ) {
                isParallel = false;
                argIt.remove();
            }
            else if ( arg.indexOf( delimChr_ ) >= 0 ) {
                try {
                    aggList.add( TableGroup.parseAggSpec( arg, delimChr_ ) );
                }
                catch ( UsageException e ) {
                    throw new ArgException( "Bad aggcol entry: "
                                          + e.getMessage() );
                }
                argIt.remove();
            }
            else {
                keyList.add( arg );
                argIt.remove();
            }
        }
        final String[] keys = keyList.toArray( new String[ 0 ] );
        final TableGroup.AggSpec[] aggs =
            aggList.toArray( new TableGroup.AggSpec[ 0 ] );
        if ( keys.length == 0 ) {
            throw new ArgException( "No keys specified" );
        }
        final RowRunner runner = isParallel ? RowRunner.DEFAULT
                                            : RowRunner.SEQUENTIAL;
        final boolean isCache = true;
        final boolean isSort = true;

        /* Return wrapped table. */
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) throws IOException {
                try {
                    return TableGroup
                          .aggregateRows( base, keys, aggs, runner,
                                          isSort, isCache );
                }
                catch ( TaskException e ) {
                    throw new IOException( e.getMessage(), e );
                }
            }
        };
    }

    /**
     * Returns a user-directed XML string describing a character.
     * Throws a RuntimeException if the character is not one of the
     * expected ones, which is just supposed to catch problems at
     * package build time.
     *
     * @param  chr   character
     * @return   XML text starting with lower case indefinite article
     */
    private static String describeChar( char chr ) {
        final String words;
        switch ( chr ) {
            case ';':
                words = "a semicolon";
                break;
            case '@':
                words = "an at sign";
                break;
            default:
                throw new IllegalArgumentException( "Unknown char: " + chr );
        }
        return words + " (\"<code>" + chr + "</code>\")";
    }
}
