package uk.ac.starlink.ttools.filter;

import gnu.jel.CompilationException;
import gnu.jel.CompiledExpression;
import gnu.jel.Evaluator;
import gnu.jel.Library;
import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.jel.JELRowReader;
import uk.ac.starlink.ttools.jel.JELUtils;

/**
 * Filter for repeating a table's rows multiple times.
 *
 * @author   Mark Taylor
 * @since    5 Aug 2010
 */
public class RepeatFilter extends BasicFilter {

    /**
     * Constructor.
     */
    public RepeatFilter() {
        super( "repeat", "[-row|-table] <count>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Repeats the rows of a table multiple times to produce",
            "a longer table.",
            "The output table will have <code>&lt;count&gt;</code> times",
            "as many rows as the input table.",
            "</p>",
            "<p>The optional flag determines the sequence of the output rows.",
            "If <code>&lt;count&gt;</code>=2 and there are three rows,",
            "the output sequence will be 112233 for <code>-row</code>",
            "and 123123 for <code>-table</code>.",
            "The default behaviour is currently <code>-table</code>.",
            "</p>",
            "<p>The <code>&lt;count&gt;</code> value will usually",
            "be a constant integer value, but it can be an expression",
            "evaluated in the context of the table,",
            "for instance <code>1000000/$nrow</code>.",
            "If it's a constant, it" + Tables.PARSE_COUNT_MAY_BE_GIVEN,
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        boolean byrow = false;
        String countStr = null;
        while ( argIt.hasNext() && countStr == null ) {
            String arg = argIt.next();
            if ( arg.equals( "-row" ) ) {
                argIt.remove();
                byrow = true;
            }
            else if ( arg.equals( "-table" ) ) {
                argIt.remove();
                byrow = false;
            }
            else if ( arg.startsWith( "-" ) ) {
                argIt.remove();
                throw new ArgException( "Unknown flag " + arg );
            }
            else {
                argIt.remove();
                countStr = arg;
            }
        }
        if ( countStr == null ) {
            throw new ArgException( "No count given" );
        }
        final String countStr0 = countStr;
        final boolean byrow0 = byrow;
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) throws IOException {
                long count = getCount( countStr0, base );
                return new RepeatTable( base, count, byrow0 );
            }
        };
    }

    /**
     * Returns a non-negative long value given by a JEL expression
     * in the context of a given table.
     *
     * @param  countExpr   expression
     * @param  table       evaluation context
     * @return   requested row count
     */
    private long getCount( String countExpr, StarTable table )
            throws IOException {

        /* See if the count can be parsed as a numeric literal. */
        try {
            return Tables.parseCount( countExpr );
        }
        catch ( NumberFormatException e ) {
            // So it's not a numeric literal.
        }

        /* Try parsing it as a JEL expression instead. */
        JELRowReader rdr = JELUtils.createDatalessRowReader( table );
        Library lib = JELUtils.getLibrary( rdr );
        String qexpr = "\"" + countExpr + "\"";
        CompiledExpression compex;
        try {
            compex = Evaluator.compile( countExpr, lib, long.class );
        }
        catch ( CompilationException e ) {
            throw (IOException)
                  new IOException( "Bad expression " + qexpr + ": "
                                 +  e.getMessage() )
                 .initCause( e );
        }
        Object countObj;
        try {
            countObj = rdr.evaluate( compex );
        }
        catch ( Throwable e ) {
            throw (IOException)
                  new IOException( "Evaluation error for " + qexpr )
                 .initCause( e );
        }
        if ( countObj instanceof Number ) {
            long count = ((Number) countObj).longValue();
            if ( count >= 0 ) {
                return count;
            }
            else {
                throw new IOException( "Count " + count + " is negative" );
            }
        }
        else {
            assert false : "Should be a long!";
            throw new IOException( "Not numeric " + qexpr );
        }
    }
}
