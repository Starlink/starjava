package uk.ac.starlink.topcat;

import uk.ac.starlink.table.ColumnData;

/**
 * Aggregates a ColumnData and an identifier for the same.
 *
 * <p>The identifier can be used for assessment of equality.
 * Instances referring to different columns should have different values,
 * and the identifier for a single instance should change if the content
 * of its columns is expected to have changed.
 *
 * @author   Mark Taylor
 * @since    27 Jun 2025
 */
public interface IdentifiedColumnData {

    /**
     * Returns the ColumnData.
     *
     * @return  column data
     */
    ColumnData getColumnData();

    /**
     * Returns the identifier, which can be used to assess equality
     * of the content of the column data at construction time of this object.
     *
     * @return  identifier
     */
    String getId();
}
