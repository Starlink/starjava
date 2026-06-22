package uk.ac.starlink.parquet;

import java.util.Map;

/**
 * Characterises metadata that can be held in a MAML header.
 *
 * <p>MAML support in STIL is currently on a best efforts basis.
 * This does not do an extremely faithful job of modelling the MAML
 * metadata format.
 *
 * @author   Mark Taylor
 * @since    22 Jun 2026
 * @see   <a href="https://github.com/asgr/MAML-Format"
 *                >https://github.com/asgr/MAML-Format</a>
 */
public interface MamlMetadata {

    /**
     * Returns the table name.
     *
     * @return   value of the "table" key, if any
     */
    String getName();

    /**
     * Returns a map of metadata items with single String values.
     *
     * @return  string-valued items suitable for use as table parameters
     */
    Map<String,String> getParameters();

    /**
     * Returns basic information about table columns, keyed by column name.
     *
     * @return  map of column name to column metadata
     */
    Map<String,Field> getFields();

    /**
     * Contains metadata about a column.
     */
    interface Field {

        /**
         * Returns column unit.
         *
         * @return  value of "unit" entry in field
         */
        String getUnit();

        /**
         * Returns column description.
         *
         * @return  value of "info" entry in field
         */
        String getInfo();

        /**
         * Returns column UCD.
         * Although MAML permits multiple UCDs per field,
         * this interface does not.
         *
         * @return  value of "ucd" entry in field, if it's a scalar
         */
        String getUcd();
    }
}
