/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     14-FEB-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

/**
 * This class defines a boolean-like value that can have three states,
 * rather than two. These are meant to represent, false, maybe and true.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class FuzzyBoolean 
{
    /**
     * The current value.
     */
    protected int value = FALSE;

    /**
     * The false value.
     */
    public static final int FALSE = 0;

    /**
     * The true value.
     */
    public static final int TRUE = 1;

    /**
     * The maybe true value.
     */
    public static final int MAYBE = -1;

    /**
     * Create an instance.
     *
     * @param value the value of this object.
     */
    public FuzzyBoolean( int value )
    {
        if ( validateValue( value ) ) {
            this.value = value;
        }
        else {
            this.value = FALSE;
        }
    }

    /**
     * Create an instance initialised from a primitive boolean.
     *
     * @param value the value of this object.
     */
    public FuzzyBoolean( boolean value )
    {
        if ( value ) { 
            this.value = TRUE;
        }
        else {
            this.value = FALSE;
        }
    }

    /**
     * Returns the value of this object as an int.
     *
     * @return the value of this object.
     */
    public int intValue() 
    {
        return value;
    }

    /**
     * Returns the value of this object as a boolean.
     *
     * @return true for TRUE and MAYBE, FALSE otherwise.
     */
    public boolean booleanValue() 
    {
        return ( value == TRUE || value == MAYBE );
    }
    
    /**
     * Returns a String object representing the value.
     *
     * @return  a string representation of this object. 
     */
    public String toString() 
    {
        if ( value == FALSE ) {
            return "false";
        } 
        else if ( value == TRUE ) {
            return "true";
        }
        return "maybe";
    }

    /**
     * Validate a given value to see if it is a permitted one.
     */
    protected boolean validateValue( int value )
    {
        if ( value == FALSE || value == TRUE || value == MAYBE ) {
            return true;
        }
        return false;
    }

    /**
     * Returns if the value is TRUE.
     */
    public boolean isTrue() 
    {
        return ( value == TRUE );
    }

    /**
     * Returns if the value is FALSE.
     */
    public boolean isFalse() 
    {
        return ( value == FALSE );
    }

    /**
     * Returns if the value is MAYBE.
     */
    public boolean isMaybe() 
    {
        return ( value == MAYBE );
    }

    /**
     * Return if a FuzzyBoolean has the same value as this object.
     */
    public boolean equals( FuzzyBoolean fb )
    {
        return ( fb.intValue() == value );
    }
}

