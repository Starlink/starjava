/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: DMS.java,v 1.4 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.coords;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Class representing a value of the form "deg:min:sec".
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public class DMS implements Serializable {

    /** number of degrees */
    private int degrees;

    /** number of minutes */
    private int min;

    /** number of seconds */
    private double sec;

    /** value converted to decimal */
    private double val;

    /** set to 1 or -1 */
    private byte sign = 1;

    /** Used to format values as strings. */
    private static NumberFormat nf = NumberFormat.getInstance(Locale.US);

    static {
        nf.setMinimumIntegerDigits(2);
        nf.setMaximumFractionDigits(2);
    }

    /** On the handling of -0: from the javadoc for Double.equals():
     * "If d1 represents +0.0 while d2 represents -0.0, or vice versa,
     * the equal test has the value false, even though +0.0==-0.0 has the
     * value true."
     * The test for 0.0 != -0.0 only works with Double.equals(minusZero).
     * This case shows up in DMS values with zero degrees and negative values,
     * such as "-00 24 32"
     */
    private static final Double minusZero = new Double(-0.0);

    /* true if value has been initialized */
    private boolean initialized = false;


    /** Default constructor: initialize to null values */
    public DMS() {
    }

    /**
     * Initialize with the given degrees, minutes and seconds.
     */
    public DMS(double degrees, int min, double sec) {
        set(degrees, min, sec);
    }

    /**
     * Initialize from a decimal value and calculate H:M:S.sss.
     */
    public DMS(double val) {
        setVal(val);
    }

    /**
     * Copy constructor
     */
    public DMS(DMS hms) {
        setVal(hms.val);
    }

    /**
     * Initialize from a string value, in format H:M:S.sss, hh, d.ddd, or
     * H M S.
     */
    public DMS(String s) {
        double[] vals = {0.0, 0.0, 0.0};
        StringTokenizer tok = new StringTokenizer(s, ": ");
        int n = 0;
        while (n < 3 && tok.hasMoreTokens()) {
            vals[n++] = Double.valueOf(tok.nextToken()).doubleValue();
        }

        if (n >= 2) {
            set(vals[0], (int) vals[1], vals[2]);
        }
        else if (n == 1) {
            setVal(vals[0]);
        }
        else {
            throw new RuntimeException("Expected a string of the form hh:mm:ss.sss, but got: '" + s + "'");
        }
    }


    /**
     * Set the degrees, minutes and seconds.
     */
    public void set(double degrees, int min, double sec) {
        this.degrees = (int) degrees;
        this.min = min;
        this.sec = sec;

        val = (sec / 60.0 + min) / 60.0;

        if (degrees < 0.0 || new Double(degrees).equals(minusZero)) {
            val = degrees - val;
            this.degrees = -this.degrees;
            sign = -1;
        }
        else {
            val = this.degrees + val;
            sign = 1;
        }
        initialized = true;
    }

    /**
     * Set from a decimal value and calculate H:M:S.sss.
     */
    public void setVal(double val) {
        this.val = val;

        double v = val; // check also for neg zero
        if (v < 0.0 || new Double(v).equals(minusZero)) {
            sign = -1;
            v = -v;
        }
        else {
            sign = 1;
        }

        double dd = v + 0.0000000001;
        degrees = (int) dd;
        double md = (dd - degrees) * 60.;
        min = (int) md;
        sec = (md - min) * 60.;
        initialized = true;
    }

    /**
     * Return the value as a String in the form hh:mm:ss.sss.
     * Seconds are formatted with leading zero if needed.
     * The seconds are formatted with 2 digits precission.
     */
    public String toString() {
        String secs = nf.format(sec);

        // sign
        String signStr;
        if (sign == -1)
            signStr = "-";
        else
            signStr = "+";

        return signStr
                + nf.format(degrees)
                + ":"
                + nf.format(min)
                + ":"
                + secs;
    }

    /** Return true if this object has been initialized with a valid value */
    public boolean isInitialized() {
        return initialized;
    }

    /** Return the number of degrees (not including minutes or seconds) */
    public int getDegrees() {
        return degrees;
    }

    /** Return the number of minutes (not including degrees or seconds) */
    public int getMin() {
        return min;
    }

    /** Return the number of seconds (not including degrees and minutes) */
    public double getSec() {
        return sec;
    }

    /** Return the value (fractional number of degrees) as a double */
    public double getVal() {
        return val;
    }

    /** Return the sign of the value */
    public byte getSign() {
        return sign;
    }

    /** Define equality based on the value */
    public boolean equals(Object obj) {
        return (val == ((DMS) obj).val);
    }


    /**
     * Test cases
     */
    public static void main(String[] args) {

        DMS d = new DMS(3, 19, 48.23);
        System.out.println("DMS(3, 19, 48.23) == " + d + " == " + d.getVal());

        if (!(d.equals(new DMS(d.getVal()))))
            System.out.println("Equality test failed: " + d + " != " + new DMS(d.getVal()));

        d = new DMS(41, 30, 42.2);
        System.out.println("41 30 42.2 = " + d + " = " + d.getVal() + " = " + new DMS(d.getVal()));

        d = new DMS(-41, 30, 2.2);
        System.out.println("-41 30 2.2 = " + d + " = " + d.getVal() + " = " + new DMS(d.getVal()));

        d = new DMS("-41 30 42.2");
        System.out.println("-41 30 42.2 = " + d + " = " + d.getVal() + " = " + new DMS(d.getVal()));

        d = new DMS("1:01:02.34567");
        System.out.println("1:01:02.34567 = " + d + " = " + d.getVal() + " = " + new DMS(d.getVal()));

        d = new DMS("1:01:02.34567");
        System.out.println("1:01:02.34567 = " + d + " = " + d.getVal() + " = " + new DMS(d.getVal()));

        d = new DMS(-0., 15, 33.3333);
        System.out.println("-0 15 33.3333 = " + d + " = " + d.getVal() + " = " + new DMS(d.getVal()));

        d = new DMS(-0.0001);
        System.out.println("-0.0001 = " + d + " = " + d.getVal() + " = " + new DMS(d.getVal()));

        d = new DMS(121.39583332 / 15.);
        System.out.println("121.39583332/15. = " + d + " = " + d.getVal() + " = " + new DMS(d.getVal()));

        d = new DMS(121.09583332 / 15.);
        System.out.println("121.09583332/15. = " + d + " = " + d.getVal() + " = " + new DMS(d.getVal()));

        d = new DMS(-121.39583332 / 15.);
        System.out.println("-121.39583332/15. = " + d + " = " + d.getVal() + " = " + new DMS(d.getVal()));

        d = new DMS(-121.09583332 / 15.);
        System.out.println("-121.09583332/15. = " + d + " = " + d.getVal() + " = " + new DMS(d.getVal()));
    }
}
