package uk.ac.starlink.parquet;

import org.apache.parquet.column.ColumnDescriptor;
import uk.ac.starlink.table.DomainMapper;

/**
 * Provides information about a readable column in a parquet file.
 *
 * @author   Mark Taylor
 * @since    24 Feb 2021
 */
public interface InputColumn<T> {

    /**
     * Class of object that is read from column data.
     *
     * @return   content class
     */
    Class<T> getContentClass();

    /**
     * Parquet column descriptor applying to column.
     *
     * @return  descriptor
     */
    ColumnDescriptor getColumnDescriptor();

    /**
     * Indicates whether null values are a possibility.
     *
     * @return  false if the column is known to contain no null values
     */
    boolean isNullable();

    /**
     * Returns a domain mapper if there is one.
     * 
     * @return  domain mapper, or null
     */
    DomainMapper getDomainMapper();

    /**
     * Returns a decoder that can read column values from the file.
     *
     * @return  decoder
     */
    Decoder<T> createDecoder();
}
