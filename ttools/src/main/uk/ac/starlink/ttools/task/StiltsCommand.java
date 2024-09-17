package uk.ac.starlink.ttools.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.Task;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.Stilts;
import uk.ac.starlink.ttools.filter.ProgressFilter;
import uk.ac.starlink.ttools.filter.SelectFilter;

/**
 * Represents an abstract model of a STILTS command line.
 * A list of parameter-value pairs along with basic parameter
 * grouping information is represented.
 * There is no guarantee that the contents of this object
 * will correspond to a STILTS command that can actually be executed,
 * so care must be taken in assembling it.
 *
 * @author   Mark Taylor
 * @since    17 Sep 2024
 */
public class StiltsCommand {

    private final Task task_;
    private final String taskName_;
    private final SettingGroup[] groups_;

    private static final Collection<String> autoFormatNames_ =
        getAutoFormatNames();

    /**
     * Constructor.
     *
     * @param  task  task
     * @param  taskName  name of the task as used by stilts command line
     * @param  groups   all name-value pairs specifying the configuration
     *                  of the task, grouped for cosmetic purposes
     */
    public StiltsCommand( Task task, String taskName, SettingGroup[] groups ) {
        task_ = task;
        taskName_ = taskName;
        groups_ = groups;
    }

    /**
     * Returns the task corresponding to this object.
     *
     * @return  task object
     */
    public Task getTask() {
        return task_;
    }

    /**
     * Returns the name of this object's task, as used by the
     * stilts command line.
     *
     * @return  task name
     */
    public String getTaskName() {
        return taskName_;
    }

    /**
     * Returns an array of objects that together contain all the parameter
     * settings required to specify this task to stilts.
     * They are grouped for cosmetic purposes.
     *
     * @return  settings
     */
    public SettingGroup[] getGroups() {
        return groups_;
    }

    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( "stilts " )
            .append( taskName_ )
            .append( " \\\n" );
        for ( SettingGroup group : groups_ ) {
            Setting[] settings = Arrays.stream( group.getSettings() )
                                       .filter( s -> ! s.isDefaultValue() )
                                       .toArray( n -> new Setting[ n ] );
            if ( settings.length > 0 ) {
                sbuf.append( Stream.generate( () -> "   " )
                                   .limit( group.getLevel() )
                                   .collect( Collectors.joining() ) )
                    .append( Arrays.stream( settings )
                                   .map( s -> s.toString() )
                                   .collect( Collectors.joining( " " ) ) )
                    .append( " \\\n" );
            }
        }
        return sbuf.toString();
    }

    /**
     * Creates an instance of this class with an automatically determined
     * task name.
     *
     * @param  task  task
     * @param  groups   all name-value pairs specifying the configuration
     *                  of the task, grouped for cosmetic purposes
     */
    public static StiltsCommand createCommand( Task task,
                                               SettingGroup[] groups ) {
        String taskName =
            Stilts.getTaskFactory().getNickName( task.getClass() );
        return new StiltsCommand( task, taskName, groups );
    }

    /**
     * Returns settings associated with an InputTableParameter and table.
     *
     * @param   inParam  input table parameter
     * @param   table    table associated with this parameter
     * @param   selection  row selection expression, or null
     * @param   namer    table namer capable of naming the supplied table
     * @return   list of settings associated with table input
     */
    public static List<Setting>
            createInputTableSettings( AbstractInputTableParameter<?> inParam,
                                      StarTable table, TableNamer namer,
                                      FilterParameter filterParam,
                                      CredibleString selection ) {
        List<Setting> settings = new ArrayList<>();
        if ( table != null ) {

            /* Table name */
            CredibleString naming = namer.nameTable( table );
            Credibility nameCred = naming.getCredibility();
            Setting tableSetting =
                new Setting( inParam.getName(), naming.getValue(), null );
            tableSetting.setObjectValue( table );
            tableSetting.setCredibility( nameCred );
            settings.add( tableSetting );

            /* Input format. */
            if ( nameCred == Credibility.YES ||
                 nameCred == Credibility.MAYBE ) {
                Parameter<String> fmtParam = inParam.getFormatParameter();
                TableBuilder tfmt = namer.getTableFormat( table );
                final Setting tfmtSetting;
                if ( tfmt != null ) {
                    String fmtName = tfmt.getFormatName();
                    tfmtSetting = autoFormatNames_.contains( fmtName )
                                ? createDefaultParamSetting( fmtParam )
                                : createParamSetting( fmtParam, fmtName );
                }
                else {
                    tfmtSetting = createDefaultParamSetting( fmtParam );
                    tfmtSetting.setCredibility( Credibility.MAYBE );
                }
                settings.add( tfmtSetting );
            }

            /* Row selection. */
            if ( selection != null ) {
                String filterCmd = new SelectFilter().getName()
                                 + " "
                                 + argQuote( selection.getValue() );
                Setting selectSetting =
                    new Setting( filterParam.getName(), filterCmd, null );
                selectSetting.setCredibility( selection.getCredibility() );
                settings.add( selectSetting );
            }
        }
        return settings;
    }

    /**
     * Creates a Setting corresponding to a given task parameter.
     *
     * @param   param  task parameter
     * @param   tval   typed value for parameter
     * @return   setting object
     */
    public static <T> Setting createParamSetting( Parameter<T> param, T tval ) {
        String key = param.getName();
        String value;
        try {
            value = param.objectToString( new MapEnvironment(), tval );
        }
        catch ( TaskException e ) {
            assert false;
            throw new RuntimeException();
        }
        String dflt = param.getStringDefault();
        return new Setting( key, value, dflt );
    }

    /**
     * Creates a Setting corresponding to a given task parameter,
     * set to its default value.
     *
     * @param   param  task parameter
     * @return   setting object
     */
    public static Setting createDefaultParamSetting( Parameter<?> param ) {
        String dflt = param.getStringDefault();
        return new Setting( param.getName(), dflt, dflt );
    }

    /**
     * Returns a setting that corresponds to adding a progress filter
     * to a filter parameter.
     *
     * @param   filterParam  filter parameter
     * @return  setting that adds this parameter-less filter
     */
    public static Setting createProgressSetting( FilterParameter filterParam ) {
        return new Setting( filterParam.getName(),
                            new ProgressFilter().getName(), null );
    }

    /**
     * Returns unique parameter of given type.
     */
    public static <T> Parameter<T> getParameterByType( Task task,
                                                       Class<T> ptype ) {
        List<Parameter<?>> params =
            Arrays.stream( task.getParameters() )
                  .filter( p -> ptype.isAssignableFrom( p.getValueClass() ) )
                  .collect( Collectors.toList() );
        if ( params.size() == 1 ) {
            @SuppressWarnings("unchecked")
            Parameter<T> tparam = (Parameter<T>) params.get( 0 );
            return tparam;
        }
        else {
            return null;
        }
    }

    /**
     * Groups a list of settings into zero or more SettingGroups.
     * A group is created for each non-empty run of Settings in the
     * input list that does not contain a null value; nulls are
     * effectively recognised as group terminators.
     *
     * @param  level  level for all returned groups
     * @param  settings  input list of settings
     * @return   list of groups containing all the input settings
     */
    public static List<SettingGroup> toGroups( int level,
                                               List<Setting> settings ) {
        List<Setting> inList = new ArrayList<>( settings );
        inList.add( null );
        List<SettingGroup> glist = new ArrayList<>();
        List<Setting> slist = new ArrayList<>();
        for ( Setting s : inList ) {
            if ( s != null ) {
                slist.add( s );
            }
            else if ( slist.size() > 0 ) {
                Setting[] line = slist.toArray( new Setting[ 0 ] );
                glist.add( new SettingGroup( level, line ) );
                slist = new ArrayList<>();
            }
        }
        return glist;
    }

    /**
     * Quotes a string as required for use as one of the arguments within
     * a STILTS filter command.
     *
     * @param  txt  string to quote
     * @return   text suitable for use within a stilts parameter value;
     *           some quoting may have been added if required
     */
    private static String argQuote( String txt ) {
        boolean hasSquot = txt.indexOf( '\'' ) >= 0;
        boolean hasDquot = txt.indexOf( '"' ) >= 0;
        boolean hasSpace = txt.indexOf( ' ' ) >= 0;
        if ( hasSquot || hasDquot || hasSpace ) {
            return "\"" + txt.replaceAll( "\"", "\\\\\"" ) + "\"";
        }
        else {    
            return txt;
        }
    }   

    /**
     * Returns the list of table input handlers that correspond to
     * input formats which can be auto-detected.
     * For these, the default "ifmt=(auto)" setting will work.
     *
     * @return  auto-detected format name list
     */
    private static Collection<String> getAutoFormatNames() {
        Collection<String> list = new HashSet<String>();
        for ( Object obj : new StarTableFactory().getDefaultBuilders() ) {
            if ( obj instanceof TableBuilder ) {
                list.add( ((TableBuilder) obj).getFormatName() );
            }
            else {
                assert false;
            }
        }
        return list;
    }
}
