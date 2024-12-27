/*
 * Copyright (C) 2005 Central Laboratory of the Research Councils
 *
 *  History:
 *     29-APR-2005 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.util;

/**
 * Class of physical and astronomical constants. Please add more and check
 * accuracy.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PhysicalConstants
{
    /** Single instance of class */
    private static PhysicalConstants instance = null;

    /** Constructor */
    private PhysicalConstants()
    {
        //  Does nothing.
    }

    /**
     * Get instance of class for aliasing. That is could use:
     * <pre>
     * PhysicalConstants PC = PhysicalConstants.getInstance();
     * System.out.println( "Speed of light = " + PC.SPEED_OF_LIGHT );
     * </pre>
     * to cut down on verbage.
     */
    public static PhysicalConstants getInstance()
    {
        if ( instance == null ) {
            instance = new PhysicalConstants();
        }
        return instance;
    }

    /** Speed of light in a vacuum - m.s^-1 */
    public static final double SPEED_OF_LIGHT = 2.99792458E+8;

    /** Planck constant - J.s */
    public static final double PLANCK = 6.6260693E-34;

    /** Planck constant/2*PI - J.s */
    public static final double PLANCK_BAR  = 1.05457168E-34;

    /** Planck constant - eV.s */
    public static final double PLANCK_EVS = 4.13566743E-15;

    /** Planck constant/2*PI - eV.s */
    public static final double PLANCK_EVS_BAR  = 6.58211915E-16;

    /** Planck length - m */
    public static final double PLANCK_LENGTH = 1.61624E-35;

    /** Planck mass - Kg */
    public static final double PLANCK_MASS = 2.17645E-8;

    /** Planck temperature - K */
    public static final double PLANCK_TEMP = 1.41679E32;

    /** Boltzmann constant - J.K^-1 */
    public static final double BOLTZMANN = 1.3806505E-23;

    /** Gravitational constant - N.m^2.Kg^-2 */
    public static final double GRAVITATION = 6.6742E-11;

    /** Standard acceleration of gravity on Earth m.s^-2 */
    public static final double ACCEL_GRAV = 9.80665;

    /** Electron mass - Kg */
    public static final double ELECTRON_MASS = 9.1093826E-31;

    /** Proton mass - Kg */
    public static final double PROTON_MASS = 1.67262171E-27;

    /** Fine structure constant - unitless */
    public static final double FINE_STRUCTURE = 7.297352568E-3;

    /** Electron volt - J */
    public static final double ELECTRON_VOLT = 1.60217653E-19;

    /** Stefan-Boltzmann constant - W.m^-2.K^-4 */
    public static final double STEFAN_BOLTZMANN = 5.670400E-8;

    /** Rydberg constant - m^-1 */
    public static final double RYDBERG = 10973731.568525;

    /** Wien displacement law constant - m.K */
    public static final double WIEN_DISPLACEMENT = 2.8977685E-3;

    /** Wavelength of H-alpha - Angstroms */
    public static final double WAVE_HALPHA = 6562.8;

    /** 1 Astronomical unit - m */
    public static final double AU = 1.49598E11;

    /** 1 Parsec - m */
    public static final double PARSEC = 3.085677E16;

    /** Mass of Sun - Kg */
    public static final double MASS_OF_SUN = 1.9891E30;

    /** Radius of Sun - m */
    public static final double RADIUS_OF_SUN = 6.96E8;

    /** Solar Luminosity - W */
    public static final double LUMINOSITY_OF_SUN = 3.8E26;

    /** Mass of the Earth - Kg */
    public static final double MASS_OF_EARTH = 5.972E24;

    /** Mass of Jupiter - Kg */
    public static final double MASS_OF_JUPITER = 318.0 * MASS_OF_EARTH;

    /** Solar constant - kW.m^-1 */
    public static final double SOLAR_CONSTANT = 1.37;

    /** 1 Jansky - W.m^-2.Hz^-1 */
    public static final double JANSKY = 1.0E-26;
}
