/*
 * Copyright (C) 2007 Science and Technology Facilities Council
 *
 *  History:
 *     05-JUN-2007 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

/**
 * Utility class for containing three associated strings.
 * <p>
 * The first string represents a unique key when used in a List.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class Triple
{
    private String s1;
    private String s2;
    private String s3;

    /**
     * Create an instance.
     */
    public Triple( String s1, String s2, String s3 )
    {
        this.s1 = s1;
        this.s2 = s2;
        this.s3 = s3;
    }

    public String toString()
    {
        return s1;
    }

    /**
     * Retrieve the first string.
     */
    public String gets1()
    {
        return s1;
    }

    /**
     * Retrieve the second string.
     */
    public String gets2()
    {
        return s2;
    }

    /**
     * Retrieve the third string.
     */
    public String gets3()
    {
        return s3;
    }
}
