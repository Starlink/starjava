//
// Convert.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2000 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

//package visad.browser;

package jsky.util;

/**
 * Utility methods for various conversions between primitive data types.
 */
public class Convert {

    /**
     * Converts an array of ints to an array of bytes. Each integer is cut into
     * four byte-size pieces, making the resulting byte array four times the
     * length of the input int array.
     *
     * @param ints The array of ints to be converted to a byte array
     *
     * @return An array of bytes corresponding to the original int array.
     */
    public static byte[] intToBytes(int[] ints) {
        int len = ints.length;
        byte[] bytes = new byte[4 * len];
        for (int i = 0; i < len; i++) {
            int q = ints[i];
            bytes[4 * i] = (byte) (q & 0x000000ff);
            bytes[4 * i + 1] = (byte) ((q & 0x0000ff00) >> 8);
            bytes[4 * i + 2] = (byte) ((q & 0x00ff0000) >> 16);
            bytes[4 * i + 3] = (byte) ((q & 0xff000000) >> 24);
        }
        return bytes;
    }

    /**
     * Converts an array of bytes to an array of ints. Each group of four bytes
     * form a single int, making the resulting int array one fourth the length
     * of the input byte array. Note that trailing elements of the bytes array
     * will be ignored.
     *
     * @param bytes The array of bytes to be converted to an int array
     *
     * @return An array of ints corresponding to the original byte array.
     */
    public static int[] bytesToInt(byte[] bytes) {
        int len = bytes.length / 4;
        int[] ints = new int[len];
        for (int i = 0; i < len; i++) {
            // This byte decoding method is not very good; is there a better way?
            int q3 = bytes[4 * i + 3] << 24;
            int q2 = bytes[4 * i + 2] << 16;
            int q1 = bytes[4 * i + 1] << 8;
            int q0 = bytes[4 * i];
            if (q2 < 0) q2 += 16777216;
            if (q1 < 0) q1 += 65536;
            if (q0 < 0) q0 += 256;
            ints[i] = q3 | q2 | q1 | q0;
        }
        return ints;
    }

    /**
     * Escape value for an RLE encoded sequence.
     */
    private static final int RLE_ESCAPE = Integer.MIN_VALUE;

    /**
     * Encodes the given array of ints using a run-length scheme.
     *
     * @param array The array of ints to RLE-encode
     *
     * @return An RLE-encoded array of ints.
     */
    public static int[] encodeRLE(int[] array) {
        int len = array.length;
        int[] temp = new int[len];
        int p = 0;

        for (int i = 0; i < len;) {
            int q = array[i];
            int count = 0;
            while (i < len && q == array[i]) {
                count++;
                i++;
            }

            if (count < 4) {
                // no gain from RLE; save values directly
                for (int z = 0; z < count; z++) temp[p++] = q;
            }
            else {
                // compress data using RLE
                temp[p++] = RLE_ESCAPE;
                temp[p++] = q;
                temp[p++] = count;
            }
        }

        // trim encoded array
        int[] encoded = new int[p];
        System.arraycopy(temp, 0, encoded, 0, p);
        return encoded;
    }

    /**
     * Decodes the given array of ints from a run-length encoding.
     *
     * @param array The RLE-encoded array of ints to decode
     *
     * @return A decoded array of ints.
     */
    public static int[] decodeRLE(int[] array) {
        // compute size of decoded array
        int count = 0;
        int i = 0;
        while (i < array.length) {
            if (array[i] == RLE_ESCAPE) {
                count += array[i + 2];
                i += 3;
            }
            else {
                count++;
                i++;
            }
        }

        // allocate decoded array
        int[] decoded = new int[count];
        int p = 0;

        // decode RLE sequence
        for (i = 0; i < array.length; i++) {
            int q = array[i];
            if (q == RLE_ESCAPE) {
                int val = array[++i];
                int cnt = array[++i];
                for (int z = 0; z < cnt; z++) decoded[p++] = val;
            }
            else
                decoded[p++] = q;
        }
        return decoded;
    }

    /**
     * Extracts a double from a string.
     */
    public static double getDouble(String s) {
        double d = Double.NaN;
        try {
            d = Double.valueOf(s).doubleValue();
        }
        catch (NumberFormatException exc) {
        }
        return d;
    }

    /**
     * Extracts a float from a string.
     */
    public static float getFloat(String s) {
        float f = Float.NaN;
        if (s != null) {
            try {
                f = Float.valueOf(s).floatValue();
            }
            catch (NumberFormatException exc) {
            }
        }
        return f;
    }

    /**
     * Extracts a boolean from a string.
     */
    public static boolean getBoolean(String s) {
        if (s == null) return false;
        char c = s.trim().charAt(0);
        return c == 'T' || c == 't';
    }

    /**
     * Extracts an integer from a string.
     */
    public static int getInt(String s) {
        int i = 0;
        if (s != null) {
            try {
                i = Integer.parseInt(s);
            }
            catch (NumberFormatException exc) {
            }
        }
        return i;
    }

    /**
     * Number of significant digits after the decimal point.
     */
    public static final int PLACES = 3;

    /**
     * Gets a reasonably short string representation of a double
     * for use in a graphical user interface.
     */
    public static String shortString(double val) {
        // remember whether or not the number is negative
        boolean negative = (val < 0.0);

        double orig_val = val;

        // now we only need to deal with a positive number
        val = Math.abs(val);

        if (val < 0.001) {
            for (int i = 1; i < 30; i++) {
                val *= 10.0;
                orig_val *= 10.0;
                if (val >= 1.0) {
                    return shortString(orig_val) + "E-" + i;
                }
            }
        }

        // build multiplier for saving significant digits
        // also build value used to round up insignificant digits
        int mult = 1;
        float round = 0.5f;
        for (int p = PLACES; p > 0; p--) {
            mult *= 10;
            round /= 10;
        }

        // break into digits before (preDot) and after (postDot) the decimal point
        long l = (long) ((val + round) * mult);
        long preDot = l / mult;
        int postDot = (int) (l % mult);

        // format the pre-decimal point number
        // Integer.toString() is faster than Long.toString(); use it if possible
        String num;
        if (preDot <= Integer.MAX_VALUE) {
            num = Integer.toString((int) preDot);
        }
        else {
            num = Long.toString(preDot);
        }

        // if there's nothing after the decimal point, use the whole number
        if (postDot == 0) {

            // make sure we don't return "-0"
            if (negative && preDot != 0) {
                return "-" + num;
            }

            return num;
        }

        // start building the string
        StringBuffer buf = new StringBuffer(num.length() + 5);

        // add sign (if necessary), pre-decimal point digits and decimal point
        if (negative) {
            buf.append('-');
        }
        buf.append(num);
        buf.append('.');

        // format the post-decimal point digits
        num = Integer.toString(postDot);

        // add leading zeros if necessary
        int nlen = num.length();
        for (int p = PLACES; p > nlen; p--) {
            buf.append('0');
        }

        // add actual digits
        buf.append(num);

        // return the final string
        return buf.toString();
    }

}
