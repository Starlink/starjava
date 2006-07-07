package uk.ac.starlink.ttools.task;

import uk.ac.starlink.task.InputStreamParameter;

/**
 * Parameter to hold the location of a script of filter commands.
 *
 * @author   Mark Taylor
 * @since    26 Aug 2005
 */
public class FilterScriptParameter extends InputStreamParameter
                                   implements ExtraParameter {

    public FilterScriptParameter( String name ) {
        super( name );
        setUsage( "<script-file>" );
    }

    public String getExtraUsage( TableEnvironment env ) {
        return FilterParameter.getFiltersUsage( env );
    }
}
