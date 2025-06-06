package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.IntegerParameter;

/**
 * Performs a TAP query.
 *
 * @author   Mark Taylor
 * @since    21 Feb 2011
 */
public class TapQuerier extends MapperTask {

    @SuppressWarnings("this-escape")
    public TapQuerier() {
        super( "Queries a Table Access Protocol server", new ChoiceMode(),
               true, new TapMapper(),
               new VariableTablesInput( true, "upload", "upload" ) );
        IntegerParameter countParam = getTablesInput().getCountParam();
        countParam.setMinimum( 0 );
        countParam.setIntDefault( 0 );
    }

    @Override
    public TapMapper getMapper() {
        return (TapMapper) super.getMapper();
    }

    @Override
    public VariableTablesInput getTablesInput() {
        return (VariableTablesInput) super.getTablesInput();
    }
}
