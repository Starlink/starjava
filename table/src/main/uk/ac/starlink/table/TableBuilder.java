package uk.ac.starlink.table;

import java.io.IOException;
import uk.ac.starlink.util.DataSource;

/**
 * Interface for objects which can construct a <tt>StarTable</tt> from
 * a data resource.
 *
 * @author   Mark Taylor (Starlink)
 */
public interface TableBuilder {

    /**
     * Constructs a {@link StarTable} based on a given <tt>DataSource</tt>.
     * If the source is not recognised or this builder does not know
     * how to construct a table from it, then <tt>null</tt> should be
     * returned.
     * If this builder thinks it should be able to handle the source
     * but an error occurs during processing, an <tt>IOException</tt>
     * can be thrown.
     *
     * @param  datsrc  the DataSource containing the table resource
     * @return  a StarTable made out of <tt>datsrc</tt>, or <tt>null</tt>
     *          if this handler can't handle it
     */
    public StarTable makeStarTable( DataSource datsrc ) throws IOException;

}
