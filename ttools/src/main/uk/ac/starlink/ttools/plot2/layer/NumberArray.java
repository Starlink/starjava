package uk.ac.starlink.ttools.plot2.layer;

/**
 * Abstracts a readable array of numeric values.
 * Can be used to front various different objects with the same behaviour.
 *
 * @author   Mark Taylor
 * @since    17 Feb 2013
 */
public interface NumberArray {

    /**
     * Returns array length.
     * 
     * @return  number of elements
     */
    int getLength();

    /**
     * Returns a value for a given index.
     *
     * @param   index   array index
     * @return  data value at index
     */
    double getValue( int index );
}
