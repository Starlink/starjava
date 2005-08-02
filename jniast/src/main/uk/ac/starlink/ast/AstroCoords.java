package uk.ac.starlink.ast;

import uk.ac.starlink.ast.Region;

/**
 * Represents an <code>AstroCoords</code> element from the Space-Time
 * Coordinate (STC) metadata.
 * This class is used in place of AST's {@link uk.ac.starlink.ast.KeyMap}
 * for user-visible cases where the <code>KeyMap</code>'s purpose is
 * to represent an <code>AstroCoords</code> element, since it is 
 * easier for programmers and more typesafe.
 * Note however that it does not have all the complexity of an STC
 * <code>AstroCoords</code> element, only those parts which are required
 * for its use with the AST <code>Stc*</code> classes.
 *
 * @author   Mark Taylor
 */
public class AstroCoords {

    private String[] name_;
    private Region value_;
    private Region error_;
    private Region resolution_;
    private Region size_;
    private Region pixSize_;

    /**
     * Constructor.
     */
    public AstroCoords() {
    }

    /**
     * Constructor which allows specification of axis names.
     *
     * @param  axisNames  array of names for each axis in the coordinate
     *         system of the corresponding <code>Region</code>
     */
    public AstroCoords( String[] axisNames ) {
        setName( axisNames );
    }

    /**
     * Sets the axis names.
     *
     * @param  axisNames  array of names for each axis in the coordinate
     *         system of the corresponding <code>Region</code>
     */
    public void setName( String[] axisNames ) {
        name_ = axisNames; 
    }

    /**
     * Returns the axis names.
     *
     * @return  array of names for each axis in the coordinate system of the
     *          corresponding <code>Region</code>
     */
    public String[] getName() {
        return name_;
    }

    /**
     * Sets the value of this coordinate.
     *
     * @param  value  value region
     */
    public void setValue( Region value ) {
        value_ = value;
    }

    /**
     * Returns the value of this coordinate.
     *
     * @return  value region
     */
    public Region getValue() {
        return value_;
    }
    
    /**
     * Sets the error region for this coordinate.
     *
     * @param   error  error region
     */
    public void setError( Region error ) {
        error_ = error;
    }

    /**
     * Returns the error region for this coordinate.
     *
     * @return   error region
     */
    public Region getError() {
        return error_;
    }

    /**
     * Sets the resolution region for this coordinate.
     *
     * @param  resolution  resolution region
     */
    public void setResolution( Region resolution ) {
        resolution_ = resolution;
    }

    /**
     * Returns the resolution region for this coordinate.
     *
     * @return  resolution region
     */
    public Region getResolution() {
        return resolution_;
    }

    /**
     * Sets the size region for this coordinate.
     *
     * @param  size  size region
     */
    public void setSize( Region size ) {
        size_ = size;
    }

    /**
     * Returns the size region for this coordinate.
     *
     * @return  size region
     */
    public Region getSize() {
        return size_;
    }

    /**
     * Sets the pixel size for this coordinate.
     *
     * @param  pixSize  pixel size
     */
    public void setPixSize( Region pixSize ) {
        pixSize_ = pixSize;
    }

    /**
     * Returns the pixel size for this coordinate.
     *
     * @return  pixSize  pixel size
     */
    public Region getPixSize() {
        return pixSize_;
    }
    
}
