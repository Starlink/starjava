package uk.ac.starlink.topcat;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.MetaCopyStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.task.AbstractInputTableParameter;
import uk.ac.starlink.ttools.task.CredibleString;
import uk.ac.starlink.ttools.task.Credibility;
import uk.ac.starlink.ttools.task.FilterParameter;
import uk.ac.starlink.ttools.task.Setting;
import uk.ac.starlink.ttools.task.StiltsCommand;
import uk.ac.starlink.ttools.task.TableNamer;
import uk.ac.starlink.util.URLUtils;

/**
 * TableNamer implementation for use with TOPCAT.
 *
 * <p>An instance of this class can be used to prepare a DescribedValue
 * to be stashed in the Parameter list of a StarTable, where the
 * value is the name to be used for that table.
 *
 * @author   Mark Taylor
 * @since    17 Sep 2024
 */
public class TopcatTableNamer implements TableNamer {

    private final String name_;
    private final ValueInfo nameInfo_;
    private final boolean hasFormat_;

    /** Namer instance that quotes pathnames. */
    public static final TopcatTableNamer PATHNAME_NAMER;

    /** Namer instance that quotes filenames without directories. */
    public static final TopcatTableNamer FILENAME_NAMER;

    /** Namer instance that uses TopcatModel labels. */
    public static final TopcatTableNamer LABEL_NAMER;

    /** Namer instance that uses topcat table sequence numbers. */
    public static final TopcatTableNamer TNUM_NAMER;

    private static final TopcatTableNamer[] TABLENAMERS = {
        PATHNAME_NAMER = new TopcatTableNamer( "Pathname", true ),
        FILENAME_NAMER = new TopcatTableNamer( "Filename", true ),
        LABEL_NAMER = new TopcatTableNamer( "Label", true ),
        TNUM_NAMER = new TopcatTableNamer( "TNum", false ),
    };
    private static final ValueInfo FORMAT_INFO =
        new DefaultValueInfo( TopcatTableNamer.class.getName() + "TableBuilder",
                              TableBuilder.class );
    private static final Set<String> SCHEME_NAMES =
        ControlWindow.getInstance().getTableFactory().getSchemes().keySet();

    /**
     * Constructor.
     *
     * @param  name   TableNamer user name
     * @param  hasFormat   whether to report table format when available
     */
    public TopcatTableNamer( String name, boolean hasFormat ) {
        name_ = name;
        hasFormat_ = hasFormat;
        String paramName = getClass().getName() + "_" + name;
        nameInfo_ = new DefaultValueInfo( paramName, CredibleString.class );
    }

    public CredibleString nameTable( StarTable table ) {
        Object value = Tables.getValue( table.getParameters(), nameInfo_ );
        return value instanceof CredibleString
             ? (CredibleString) value
             : new CredibleString( "???", Credibility.NO );
    }

    public TableBuilder getTableFormat( StarTable table ) {
        if ( hasFormat_ ) {
            Object fmt = Tables.getValue( table.getParameters(),
                                          FORMAT_INFO );
            return fmt instanceof TableBuilder
                 ? (TableBuilder) fmt
                 : null;
        }
        else {
            return null;
        }
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns an object to be stashed in a table's parameter list
     * giving the table name.
     *
     * @param  credStr  value
     * @return  described value
     */
    private DescribedValue createNameParam( CredibleString credStr ) {
        return new DescribedValue( nameInfo_, credStr );
    }

    /**
     * Returns an object to be stashed in a table's parameter list
     * giving the table name.
     *
     * @param  str   table name string
     * @param  cred  table name credibility
     * @return  described value
     */
    private DescribedValue createNameParam( String str, Credibility cred ) {
        return createNameParam( new CredibleString( str, cred ) );
    }

    /**
     * Returns a list of TableNamer objects that give the user options for
     * referencing TopcatModels by a text string in generated stilts commands.
     *
     * @return  table namer user options
     */
    public static TopcatTableNamer[] getTableNamers() {
        return TABLENAMERS.clone();
    }

    /**
     * Returns a table corresponding to the current apparent table of
     * a topcat model, intended to be used with an instance of this class.
     *
     * <p>Its parameter list also contains parameters
     * giving various naming options corresponding to the FileNamer
     * instances defined by this class.
     *
     * @param   tcModel  topcat model
     * @return   table view for use with TopcatTableNamer instance
     */
    public static StarTable getTable( TopcatModel tcModel ) {
        if ( tcModel == null ) {
            return null;
        }
        List<DescribedValue> params = new ArrayList<>();
        params.add( new DescribedValue( FORMAT_INFO,
                                        tcModel.getTableFormat() ) );
        params.add( TNUM_NAMER
                   .createNameParam( "T" + tcModel.getID(), Credibility.NO ) );
        params.add( LABEL_NAMER
                   .createNameParam( tcModel.getLabel(), Credibility.MAYBE ) );
        String loc = tcModel.getLocation();
        URL url;
        try {
            url = URLUtils.newURL( loc );
        }
        catch ( MalformedURLException e ) {
            url = null;
        }
        File file = url == null ? null
                                : URLUtils.urlToFile( url.toString() );
        if ( file == null ) {
            file = new File( loc );
        }
        String[] schemePair = StarTableFactory.parseSchemeLocation( loc );
        final CredibleString filename;
        final CredibleString pathname;
        if ( url != null ) {
            filename = new CredibleString( file.getName(), Credibility.NO );
            pathname = new CredibleString( loc, Credibility.YES );
        }
        else if ( file.exists() ) {
            filename = new CredibleString( file.getName(), Credibility.MAYBE );
            pathname = new CredibleString( file.getAbsolutePath(),
                                           Credibility.YES );
        }
        else if ( schemePair != null ) {
            Credibility cred = SCHEME_NAMES.contains( schemePair[ 0 ] )
                             ? Credibility.YES
                             : Credibility.MAYBE;
            filename = new CredibleString( loc, cred );
            pathname = filename;
        }
        else {
            filename = new CredibleString( loc, Credibility.NO );
            pathname = filename;
        }
        params.add( FILENAME_NAMER.createNameParam( filename ) );
        params.add( PATHNAME_NAMER.createNameParam( pathname ) );
        StarTable table =
            new MetaCopyStarTable( tcModel.getViewModel().getSnapshot() );
        table.getParameters().addAll( params );
        return table;
    }

    /**
     * Returns a list of settings for use with a stilts command
     * that characterise a TopcatModel as an input table.
     *
     * @param  inParam  stilts table input parameter
     * @param  filterParam  stilts table input filter parameter, or null
     * @param  tcModel   topcat model to represent as input
     * @return  list of settings
     */
    public List<Setting>
            createInputTableSettings( AbstractInputTableParameter<?> inParam,
                                      FilterParameter filterParam,
                                      TopcatModel tcModel ) {
        StarTable table = getTable( tcModel );
        CredibleString selection =
              filterParam == null
            ? null
            : getSelectExpression( tcModel.getSelectedSubset() );
        return StiltsCommand
              .createInputTableSettings( inParam, getTable( tcModel ), this,
                                         filterParam, selection );
    }

    /**
     * Returns a best effort at an expression indicating row selection
     * corresponding to a given RowSubset.
     * In some cases, for instance a subset defined by a bitmap,
     * there's no way to do this that will result in an evaluatable
     * expression, so in those cases just return the subset name or something.
     *
     * @param  rset  row subset
     * @return   attempt at expression giving row inclusion, not null
     */
    public static CredibleString getSelectExpression( RowSubset rset ) {
        if ( rset == null ) {
            return null;
        }
        else if ( rset.equals( RowSubset.ALL ) ) {
            return null;
        }
        else if ( rset.equals( RowSubset.NONE ) ) {
            return new CredibleString( "false", Credibility.YES );
        }
        else if ( rset instanceof SyntheticRowSubset ) {
            return new CredibleString( ((SyntheticRowSubset) rset)
                                       .getExpression(), Credibility.MAYBE );
        }
        else if ( rset instanceof BooleanColumnRowSubset ) {
            BooleanColumnRowSubset cset = (BooleanColumnRowSubset) rset;
            String expr = cset.getTable()
                         .getColumnInfo( cset.getColumnIndex() ).getName();
            return new CredibleString( expr, Credibility.YES );
        }
        else if ( rset instanceof SingleRowSubset ) {
            SingleRowSubset sset = (SingleRowSubset) rset;
            return new CredibleString( "$0==" + sset.getRowIndex(),
                                       Credibility.YES );
        }
        else if ( rset instanceof InverseRowSubset ) {
            CredibleString invResult =
                getSelectExpression( ((InverseRowSubset) rset)
                                    .getInvertedSubset() );
            return new CredibleString( "!(" + invResult.getValue() + ")",
                                       invResult.getCredibility() );
        }
        else {
            return new CredibleString( "<" + rset.getName() + ">",
                                       Credibility.NO );
        }
    }
}
