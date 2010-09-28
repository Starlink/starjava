package uk.ac.starlink.table.gui;

import java.io.IOException;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableSequence;
import uk.ac.starlink.table.ValueInfo;

/**
 * Interface defining an object which can load tables.
 *
 * @author   Mark Taylor
 * @since    13 Sept 2010
 */
public interface TableLoader {

    /**
     * Table parameter key which may be used to provide information about
     * the source of a loaded table.
     * This is a string-type info with the name "LOAD_SOURCE".
     * If present it should be used in preference to the result of 
     * {@link #getLabel} to label the table for users.
     */
    public static final ValueInfo SOURCE_INFO =
        new DefaultValueInfo( "LOAD_SOURCE", String.class,
                              "Short label indicating table source" );

    /**
     * Returns a short textual label describing what is being loaded.
     * This may be presented to a waiting user.
     *
     * @return   load label
     */
    String getLabel();

    /**
     * Loads one or more tables.
     * If this loader wishes to label the tables in the returned sequence
     * to describe their source, it may set a table parameter with the
     * {@link #SOURCE_INFO} key, for instance:
     * <pre>
     *    table.setParameter(new DescribedValue(TableLoader.SOURCE_INFO,
     *                                          "Foo protocol query #1"))
     * </pre>
     * This is optional; for instance if a table name is set that may
     * provide sufficient description.
     *
     * <p>This method may be time-consuming, and should not be called on
     * the event dispatch thread.
     *
     * @param  tfact   table factory
     * @return   loaded tables
     */
    TableSequence loadTables( StarTableFactory tfact ) throws IOException;
}
