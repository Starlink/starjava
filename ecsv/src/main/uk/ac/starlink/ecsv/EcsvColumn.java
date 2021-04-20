package uk.ac.starlink.ecsv;

import java.util.Map;

/**
 * Represents column metadata from an ECSV table.
 *
 * @author   Mark Taylor
 * @since    28 Apr 2020
 */
public interface EcsvColumn<T> {

    /**
     * Returns the column name.
     *
     * @return  name
     */
    String getName();

    /**
     * Returns the column unit string if available.
     *
     * @return  unit, or null
     */
    String getUnit();

    /**
     * Returns the column description text if available.
     *
     * @return  description, or null
     */
    String getDescription();

    /**
     * Returns the column format string if available.
     *
     * @return  printf-style format string, or null
     */
    String getFormat();

    /**
     * Returns the declared datatype for this column.
     *
     * @return  datatype
     */
    String getDatatype();

    /**
     * Returns a decoder that can be used to make sense of cell values
     * in the body of the ECSV file corresponding to this column.
     *
     * @return  decoder, or null if the format for this column is
     *          unknown or unsupported
     */
    EcsvDecoder<T> getDecoder();

    /**
     * Returns a map containing miscellaneous metadata declared for
     * this column, if available.
     *
     * @return   metadata map, may be null or empty
     */
    Map<?,?> getMeta();
}
