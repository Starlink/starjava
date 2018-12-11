package uk.ac.starlink.topcat;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.TablePreparation;
import uk.ac.starlink.table.ValueInfo;

/**
 * TablePreparation implementation that can store and retrieve information
 * about the serialization format from which each table was loaded.
 *
 * @author   Mark Taylor
 * @since    28 Sep 2017
 */
public class TopcatPreparation implements TablePreparation {

    private final TablePreparation prePrep_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat" );

    /** Metadata key for storing input handler object. */
    public static final ValueInfo FORMAT_INFO =
        new DefaultValueInfo( "STIL_FORMAT", TableBuilder.class,
                              "STIL input handler that was used to read "
                            + "the table" );

    /**
     * Constructs an instance that executes a prior preparation before
     * doing its own work.
     *
     * @param  prePrep   preparation to chain; exeucuted before this one
     */
    public TopcatPreparation( TablePreparation prePrep ) {
        prePrep_ = prePrep;
    }
    
    /**
     * Constructs an instance with no prior preparation.
     */
    public TopcatPreparation() {
        this( null );
    }

    public StarTable prepareLoadedTable( StarTable table,
                                         TableBuilder builder ) {
        if ( prePrep_ != null ) {
            table = prePrep_.prepareLoadedTable( table, builder );
        }
        if ( builder != null ) {
            table.getParameters()
                 .add( new DescribedValue( FORMAT_INFO, builder ) );
        }
        return table;
    }

    /**
     * Operates on a table that was loaded using this preparation,
     * and pulls out the table input handler that this preparation put there.
     * The handler is returned, and the corresponding table parameter
     * is removed from its list, if possible.
     */
    public static TableBuilder removeFormatParameter( StarTable table ) {
        TableBuilder tformat = null;
        for ( Iterator<DescribedValue> it = table.getParameters().iterator();
              it.hasNext(); ) {
            DescribedValue dval = it.next();
            if ( FORMAT_INFO.equals( dval.getInfo() ) ) {
                Object val = dval.getValue();
                if ( val instanceof TableBuilder ) {
                    tformat = (TableBuilder) val;
                }
                try {
                    it.remove();
                }
                catch ( UnsupportedOperationException e ) {
                    logger_.log( Level.WARNING,
                                 "Failed to remove info " + dval
                               + " from immutable param list", e );
                }
            }
        }
        return tformat;
    }

    /**
     * Utility method to create a StarTableFactory using this preparation.
     *
     * @return   new table factory
     */
    public static StarTableFactory createFactory() {
        StarTableFactory tfact = new StarTableFactory( true );
        tfact.setPreparation( new TopcatPreparation( tfact.getPreparation() ) );
        return tfact;
    }
}
