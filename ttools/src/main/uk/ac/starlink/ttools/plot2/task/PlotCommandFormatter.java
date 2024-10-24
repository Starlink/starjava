package uk.ac.starlink.ttools.plot2.task;

import java.util.ArrayList;
import java.util.List;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.ttools.task.CommandFormatter;
import uk.ac.starlink.ttools.task.CredibleString;
import uk.ac.starlink.ttools.task.InputTableParameter;
import uk.ac.starlink.ttools.task.LineEnder;

/**
 * Handles export of PlotStiltsCommand objects to external
 * serialization formats.
 *
 * @author   Mark Taylor
 * @since    15 Sep 2017
 */
public class PlotCommandFormatter extends CommandFormatter {

    /**
     * Constructor.
     *
     * @param  invocation     display text to introduce the STILTS command
     * @param  includeDflts   if true, all parameters are included;
     *                        if false, only those with non-default values
     * @param  lineEnder      line end presentation policy
     * @param  levelIndent    number of spaces per indentation level
     * @param  cwidth         nominal formatting width in characters;
     *                        this affects line wrapping, but actual
     *                        wrapping may depend on other factors too
     */
    public PlotCommandFormatter( CredibleString invocation,
                                 boolean includeDflts, LineEnder lineEnder,
                                 int levelIndent, int cwidth ) {
        super( invocation, includeDflts, lineEnder, levelIndent, cwidth,
               false );
    }

    @Override
    protected String[] stripExpectedUnused( Task task, String[] words ) {
        InputTableParameter inParam =
            AbstractPlot2Task.createTableParameter( "" );
        String fmtName = inParam.getFormatParameter().getName();
        String strmName = inParam.getStreamParameter().getName();
        List<String> list = new ArrayList<>();
        for ( String word : words ) {
            if ( ! word.startsWith( fmtName ) &&
                 ! word.startsWith( strmName ) ) {
                list.add( word );
            }
        }
        return list.toArray( new String[ 0 ] );
    }
}
