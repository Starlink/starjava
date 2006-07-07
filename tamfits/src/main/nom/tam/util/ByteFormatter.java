package nom.tam.util;

/** This class provides mechanisms for
  * efficiently formatting numbers and Strings.
  * Data is appended to existing byte arrays. Note
  * that the formatting of real or double values
  * may differ slightly (in the last bit) from
  * the standard Java packages since this routines
  * are optimized for speed rather than accuracy.
  * <p>
  * The methods in this class create no objects.
  * <p>
  * If a number cannot fit into the requested space
  * the truncateOnOverlow flag controls whether the
  * formatter will attempt to append it using the
  * available length in the output (a la C or Perl style
  * formats).  If this flag is set, or if the number
  * cannot fit into space left in the buffer it is 'truncated'
  * and the requested space is filled with a truncation fill
  * character.  A TruncationException may be thrown if the truncationThrow
  * flag is set.
  * <p>
  * This class does not explicitly support separate methods
  * for formatting reals in exponential notation.  Real numbers
  * near one are by default formatted in decimal notation while
  * numbers with large (or very negative) exponents are formatted
  * in exponential notation.  By setting the limits at which these
  * transitions take place the user can force either exponential or
  * decimal notation.
  */

public final class ByteFormatter {

    /** Internal buffers used in formatting fields */
    private byte[] 			tbuf1 = new byte[32];
    private byte[] 			tbuf2 = new byte[32];
    private static final double 	ilog10 = 1./Math.log(10);

    /** Should we truncate overflows or just run over limit */
    private boolean truncateOnOverflow = true;
    
    /** What do we use to fill when we cannot print the number? */
    private byte truncationFill = (byte) '*';  // Default is often used in Fortran

    /** Throw exception on truncations */
    private boolean truncationThrow = 		true;
    
    /** Should we right align? */
    private boolean align =			false;
    
    /** Minimum magnitude to print in non-scientific notation. */
    double simpleMin = 1.e-3;
    
    /** Maximum magnitude to print in non-scientific notation. */
    double simpleMax = 1.e6;

    /** Powers of 10.  We overextend on both sides.
      * These should perhaps be tabulated rather than
      * computed though it may be faster to calculate
      * them than to read in the extra bytes in the class file.
      */
    private static final double tenpow[];
    
    /** What index of tenpow is 10^0 */
    private static final int    zeropow;
    
    static { // Static initializer
	
	int min = (int) Math.floor((int)(Math.log(Double.MIN_VALUE)*ilog10));
	int max = (int) Math.floor((int)(Math.log(Double.MAX_VALUE)*ilog10));
	max += 1;
	
	tenpow = new double[(max - min) + 1];
	
	
	for (int i=0; i<tenpow.length; i += 1) {
	    tenpow[i] = Math.pow(10,i+min);
	}
	zeropow = -min;
    }

    /** Digits.  We could handle other bases
      * by extending or truncating this list and changing
      * the division by 10 (and it's factors) at various
      * locations.
      */
    private static final byte[] digits= {
      (byte)'0', (byte)'1', (byte)'2', (byte)'3', (byte)'4', 
      (byte)'5', (byte)'6', (byte)'7', (byte)'8', (byte)'9'};

    /** Set the truncation behavior.
      * @param val If set to true (the default) then do not
      *        exceed the requested length.  If a number cannot
      *        be sensibly formatted, the truncation fill character
      *        may be inserted.
      */
    public void setTruncateOnOverflow(boolean val) {
        truncateOnOverflow = val;
    }

    /** Should truncations cause a truncation overflow? */
    public void setTruncationThrow(boolean throwException) {
	truncationThrow = throwException;
    }
    
    
    /** Set the truncation fill character.
      * @param val The character to be used in subsequent truncations.
      */
    public void setTruncationFill(char val) {
	truncationFill = (byte) val;
    }

    /** Set the alignment flag.
      * @param val Should numbers be right aligned?
      */
    public void setAlign(boolean val) {
	align = val;
    }
    
    /** Set the range of real numbers that will be formatted in
      * non-scientific notation, i.e., .00001 rather than 1.0e-5.
      * The sign of the number is ignored.
      * @param min  The minimum value for non-scientific notation.
      * @param max  The maximum value for non-scientific notation.
      */
    public void setSimpleRange(double min, double max) {
	simpleMin = min;
	simpleMax = max;
    }
    
    /** Format an int into an array.
      * @param val   The int to be formatted.
      * @param array The array in which to place the result.
      * @return  The number of characters used.
      */
    public int format(int val, byte[] array) throws TruncationException {
        return format(val, array, 0, array.length);
    }
    
    /** Format an int into an existing array.
      * @param  val     Integer to be formatted
      * @param  buf     Buffer in which result is to be stored
      * @param  off     Offset within buffer
      * @param  len     Maximum length of integer
      * @return offset of next unused character in input buffer.
      */
    public int format(int val, byte[] buf,
                                 int off, int len) throws TruncationException {

	// Special case
	if (val == Integer.MIN_VALUE) {
	    if (len > 10 || (!truncateOnOverflow && buf.length-off > 10)) {
	        return format("-2147483648", buf, off, len);
	    } else {
		truncationFiller(buf, off, len);
		return off+len;
	    }
        }
	
        int pos = Math.abs(val);
	
	// First count the number of characters in the result.
	// Otherwise we need to use an intermediary buffer.
	
	int ndig=1;
	int dmax = 10;
	
	while(ndig < 10 && pos >= dmax) {
	    ndig += 1;
	    dmax *= 10;
	}
	
	if (val < 0) {
	    ndig += 1;
        }

        // Truncate if necessary.
        if ( (truncateOnOverflow && ndig > len) || ndig > buf.length-off) {
	    truncationFiller(buf, off, len);
            return off+len;
        }

        // Right justify if requested.
        if (align) {
	    off = alignFill(buf, off,len-ndig);
        }
	
	// Now insert the actual characters we want -- backwards
	// We  use a do{} while() to handle the case of 0.
	
        off += ndig;

        int xoff = off - 1;
	do {
            buf[xoff] = digits[pos%10];
	    xoff -= 1;
            pos /= 10;
	} while (pos > 0);
	
	if (val < 0) {
	    buf[xoff] = (byte)'-';
	}

        return off;
    }

    /** Format a long into an array.
      * @param val   The long to be formatted.
      * @param array The array in which to place the result.
      * @return  The number of characters used.
      */
    public int format(long val, byte[] array) throws TruncationException {
        return format(val, array, 0, array.length);
    }
   
    /** Format a long into an existing array.
      * @param  val     Long to be formatted
      * @param  buf     Buffer in which result is to be stored
      * @param  off     Offset within buffer
      * @param  len     Maximum length of integer
      * @return offset of next unused character in input buffer.
      */
    public int format(long val, byte[] buf,
                                 int off, int len) throws TruncationException {

	// Special case
	if (val == Long.MIN_VALUE) {
	    if (len > 19 || (!truncateOnOverflow && buf.length-off > 19)) {
	        return format("-9223372036854775808", buf, off, len);
	    } else {
		truncationFiller(buf, off, len);
		return off+len;
	    }
        }
	
        long pos = Math.abs(val);
	
	// First count the number of characters in the result.
	// Otherwise we need to use an intermediary buffer.
	
	int ndig=1;
	long dmax = 10;
	
	// Might be faster to try to do this partially in ints
	while (ndig < 19 && pos >= dmax) {
	    
	    ndig += 1;
	    dmax *= 10;
	}
	
	if (val < 0) {
	    ndig += 1;
        }

        // Truncate if necessary.
	
        if ( (truncateOnOverflow && ndig > len) || ndig > buf.length-off) {
	    truncationFiller(buf, off, len);
            return off+len;
        }

        // Right justify if requested.
        if (align) {
	    off = alignFill(buf, off, len-ndig);
        }
	
	// Now insert the actual characters we want -- backwards.
	

        off += ndig;
        int xoff = off - 1;
	
	buf[xoff] = (byte)'0';
	boolean last = (pos == 0);
	
	while (!last) {
	    
	    // Work on ints rather than longs.
	    
	    int giga = (int)(pos % 1000000000L);
	    pos /= 1000000000L;
	    
	    last = (pos == 0);
	    
	    for (int i=0; i<9; i += 1) {
		
		buf[xoff] = digits[giga%10];
		xoff -= 1;
		giga /= 10;
		if (last && giga == 0) {
		    break;
		}
	    }
	}
		
	
	if (val < 0) {
	    buf[xoff] = (byte)'-';
	}

        return off;
    }

    /** Format a boolean into an existing array.
     */
    public int format(boolean val, byte[] array) {
	return format(val, array, 0, array.length);
    }
    
    /** Format a boolean into an existing array
      * @param val    The boolean to be formatted
      * @param array  The buffer in which to format the data.
      * @param off    The starting offset within the buffer.
      * @param len    The maximum number of characters to use
      *               use in formatting the number.
      * @return	      Offset of next available character in buffer.
      */
    public int format(boolean val, byte[] array, int off,
                             int len) {
	if (align && len > 1) {
	    off = alignFill(array, off, len-1);
        }

        if (len > 0) {
            if (val) {
                array[off] = (byte)'T';
            } else {
                array[off] = (byte)'F';
            }
            off += 1;
        }
        return off;
    }
    
    /** Insert a string at the beginning of an array */
    public int format(String val, byte[] array) {
	return format(val, array, 0, array.length);
    }

    /** Insert a String into an existing character array.
      * If the String is longer than len, then only the
      * the initial len characters will be inserted.
      * @param val   The string to be inserted.  A null string
      *              will insert len spaces.
      * @param array The buffer in which to insert the string.
      * @param off   The starting offset to insert the string.
      * @param len   The maximum number of characters to insert.
      * @return      Offset of next available character in buffer.
      */
    public int format(String val, byte[] array, int off, int len) {
        
        if (val == null) {
            for (int i=0; i<len; i += 1) {
                array[off+i] = (byte)' ';
            }
            return off + len;
        }

        int slen = val.length();

        if ((truncateOnOverflow && slen > len) || (slen > array.length-off) ) {
            val = val.substring(0, len);
            slen = len;
        }

        if (align && (len > slen)) {
	    off = alignFill(array, off, len-slen);
        }
	
        System.arraycopy(val.getBytes(),0,array, off, slen);
        return off+slen;
    } 

    /** Format a float into an array.
      * @param val   The float to be formatted.
      * @param array The array in which to place the result.
      * @return  The number of characters used.
      */
    public int format(float val, byte[] array) throws TruncationException {
        return format(val, array, 0, array.length);
    }
    
    /** Format a float into an existing byteacter array.
      * <p>
      * This is hard to do exactly right...  The JDK code does
      * stuff with rational arithmetic and so forth.
      * We use a much simpler algorithm which may give
      * an answer off in the lowest order bit.
      * Since this is pure Java, it should still be consistent
      * from machine to machine.
      * <p>
      * Recall that the binary representation of
      * the float is of the form <tt>d = 0.bbbbbbbb x 2<sup>n</sup></tt>
      * where there are up to 24 binary digits in the binary
      * fraction (including the assumed leading 1 bit
      * for normalized numbers).
      * We find a value m such that <tt>10<sup>m</su> d</tt> is between 
      * <tt>2<sup>24</sup></tt> and <tt>>2<sup>32</sup></tt>.
      * This product will be exactly convertible to an int
      * with no loss of precision.  Getting the
      * decimal representation for that is trivial (see formatInteger).
      * This is a decimal mantissa and we have an exponent (<tt>-m</tt>).
      * All we have to do is manipulate the decimal point
      * to where we want to see it.  Errors can
      * arise due to roundoff in the scaling multiplication, but
      * should be very small.
      * 
      * @param  val     Float to be formatted
      * @param  buf     Buffer in which result is to be stored
      * @param  off     Offset within buffer
      * @param  len     Maximum length of field
      * @return         Offset of next character in buffer.
      */

    public int format(float val, byte[] buf,
                             int off, int len) throws TruncationException {

        float pos = (float) Math.abs(val);

        int minlen, actlen;

	// Special cases
        if (pos == 0.) {
	    return format("0.0", buf, off, len);
        } else if (Float.isNaN(val))  {
	    return format("NaN", buf, off, len);
        } else if (Float.isInfinite(val)) {
            if (val > 0) {
		return format("Infinity",buf, off, len); 
            } else {
		return format("-Infinity", buf, off, len);
            }
	}

        int power = (int) Math.floor( (Math.log(pos)*ilog10) );
        int shift = 8 - power;
        float scale;
        float scale2 = 1;

        // Scale the number so that we get a number ~ n x 10^8.
        if (shift < 30) {
            scale = (float) tenpow[shift+zeropow];
        } else {
            // Can get overflow if the original number is
            // very small, so we break out the shift
            // into two multipliers.
            scale2 = (float)tenpow[30+zeropow];
            scale = (float)tenpow[shift-30+zeropow];
        }


        pos = (pos*scale)*scale2;

        // Parse the float bits.

        int bits = Float.floatToIntBits(pos);

        // The exponent should be a little more than 23
        int exp = ((bits&0x7F800000) >> 23) - 127;
        
        int numb = (bits & 0x007FFFFF);

        if (exp > -127) {
            // Normalized....
            numb |= (0x00800000);
        } else {
            // Denormalized
            exp += 1;
	}

             
        // Multiple this number by the excess of the exponent
        // over 24.  This completes the conversion of float to int
	// (<<= did not work on Alpha TruUnix)
	    
        numb = numb << ( exp - 23L);

        // Get a decimal mantissa.
	boolean oldAlign = align;
        align = false;
        int ndig = format(numb, tbuf1, 0, 32);
	align = oldAlign;
		  

        // Now format the float.

        return combineReal(val, buf, off, len, tbuf1, ndig, shift);
    }

    /** Format a double into an array.
      * @param val   The double to be formatted.
      * @param array The array in which to place the result.
      * @return  The number of characters used.
      */
    public int format(double val, byte[] array) throws TruncationException {
        return format(val, array, 0, array.length);
    }
    
	
    /** Format a double into an existing character array.
      * <p>
      * This is hard to do exactly right...  The JDK code does
      * stuff with rational arithmetic and so forth.
      * We use a much simpler algorithm which may give
      * an answer off in the lowest order bit.
      * Since this is pure Java, it should still be consistent
      * from machine to machine.
      * <p>
      * Recall that the binary representation of
      * the double is of the form <tt>d = 0.bbbbbbbb x 2<sup>n</sup></tt>
      * where there are up to 53 binary digits in the binary
      * fraction (including the assumed leading 1 bit
      * for normalized numbers).
      * We find a value m such that <tt>10<sup>m</su> d</tt> is between 
      * <tt>2<sup>53</sup></tt> and <tt>>2<sup>63</sup></tt>.
      * This product will be exactly convertible to a long
      * with no loss of precision.  Getting the
      * decimal representation for that is trivial (see formatLong).
      * This is a decimal mantissa and we have an exponent (<tt>-m</tt>).
      * All we have to do is manipulate the decimal point
      * to where we want to see it.  Errors can
      * arise due to roundoff in the scaling multiplication, but
      * should be no more than a single bit.
      * 
      * @param  val     Double to be formatted
      * @param  buf     Buffer in which result is to be stored
      * @param  off     Offset within buffer
      * @param  len     Maximum length of integer
      * @return offset of next unused character in input buffer.
      */

    public int format(double val, byte[] buf,
                             int off, int len) throws TruncationException {

        double pos = Math.abs(val);

        int minlen, actlen;

	// Special cases -- It is OK if these get truncated.
        if (pos == 0.) {
	    return format("0.0", buf, off, len);
        } else if (Double.isNaN(val))  {
	    return format("NaN", buf, off, len);
        } else if (Double.isInfinite(val)) {
            if (val > 0) {
		return format("Infinity", buf, off, len);
            } else {
		return format("-Infinity", buf, off, len);
	    }
	}

        int power = (int) (Math.log(pos)*ilog10);
        int shift = 17 - power;
        double scale;
        double scale2 = 1;

        // Scale the number so that we get a number ~ n x 10^17.
        if (shift < 200) {
            scale = tenpow[shift+zeropow];
        } else {
            // Can get overflow if the original number is
            // very small, so we break out the shift
            // into two multipliers.
            scale2 = tenpow[200+zeropow];
            scale = tenpow[shift-200+zeropow];
        }


        pos = (pos*scale)*scale2;

        // Parse the double bits.

        long bits = Double.doubleToLongBits(pos);

        // The exponent should be a little more than 52.
        int exp = (int) (((bits&0x7FF0000000000000L) >> 52) - 1023);
        
        long numb = (bits & 0x000FFFFFFFFFFFFFL);

        if (exp > -1023) {
            // Normalized....
            numb |= (0x0010000000000000L);
        } else {
            // Denormalized
            exp += 1;
	}

             
        // Multiple this number by the excess of the exponent
        // over 52.  This completes the conversion of double to long.
        numb = numb << (exp - 52);

        // Get a decimal mantissa.
	boolean oldAlign = align;
	align = false;
        int ndig = format(numb, tbuf1, 0, 32);
	align = oldAlign;

        // Now format the double.

        return combineReal(val, buf, off, len, tbuf1, ndig, shift);
   }


    /** This method formats a double given
      * a decimal mantissa and exponent information.
      * @param val   The original number
      * @param buf   Output buffer
      * @param off   Offset into buffer
      * @param len   Maximum number of characters to use in buffer.
      * @param mant  A decimal mantissa for the number.
      * @param lmant The number of characters in the mantissa
      * @param shift The exponent of the power of 10 that
      *              we shifted val to get the given mantissa.
      * @return      Offset of next available character in buffer.
      */


    int combineReal(double val, byte[] buf, int off, int len,
		    byte[] mant, int lmant, int shift) throws TruncationException {
	
	// First get the minimum size for the number
	
	double pos = Math.abs(val);
	boolean simple = false;
	int minSize;
	int maxSize;
	
	if (pos >= simpleMin && pos <= simpleMax) {
	    simple = true;
	}
	
	int exp = lmant - shift - 1;
	int lexp = 0;
			
	if (!simple) {
	    
	    boolean oldAlign = align;
	    align = false;
	    lexp = format(exp, tbuf2, 0, 32);
	    align = oldAlign;
	    
	    minSize = lexp + 2; // e.g., 2e-12
	    maxSize = lexp + lmant + 2;  // add in "." and e
	} else {
	    if (exp >= 0) {
		minSize = exp+1;     // e.g. 32
		
		// Special case.  E.g., 99.9 has
		// minumum size of 3.
		int i;
		for (i=0; i < lmant && i <= exp; i += 1) {
		    if (mant[i] != (byte)'9') {
			break;
		    }
		}
		if (i > exp && i < lmant && mant[i] >= (byte)'5') {
		    minSize += 1;
		}
		
		maxSize = lmant + 1; // Add in "."
		if (maxSize <= minSize) {   // Very large numbers.
		    maxSize = minSize + 1;
		}
	    } else {
		minSize = 2;
		maxSize = 1 + Math.abs(exp) + lmant;
	    }
	}
	if (val < 0) {
	    minSize += 1;
	    maxSize += 1;
	}
	
	// Can the number fit?
	if ((truncateOnOverflow && minSize > len) || 
	    (minSize > buf.length-off )) {
	    truncationFiller(buf, off, len);
	    return off+len;
	}
	
	// Do we need to align it?
	if (maxSize < len && align) {
	    int nal = len-maxSize;
	    off = alignFill(buf, off, nal);
	    len -= nal;
	} 
	

	int off0 = off;
	
	// Now begin filling in the buffer.
	if (val < 0) {
	    buf[off] = (byte)'-';
	    off += 1;
	    len -= 1;
	}
	
	
	if (simple) {
	    return Math.abs(mantissa(mant, lmant, exp, simple, buf, off, len));
	} else {
	    off = mantissa(mant, lmant, 0, simple, buf, off, len-lexp-1);
	    if (off < 0) {
		off = -off;
		len -= off;
		// Handle the expanded exponent by filling
		if (exp == 9 || exp == 99) {
		    // Cannot fit...
		    if (off+len == minSize) {
			truncationFiller(buf, off, len);
			return off+len;
		    } else {
			// Steal a character from the mantissa.
			off -= 1;
		    }
		}
		exp += 1;
	        lexp = format(exp, tbuf2, 0, 32);
	    }
	    buf[off]=(byte)'E';
	    off += 1;
	    System.arraycopy(tbuf2, 0, buf, off, lexp);
	    return off + lexp;
	}
    }

    /** Write the mantissa of the number.  This method addresses
     *  the subtleties involved in rounding numbers.
     */
    int mantissa(byte[] mant, int lmant, int exp, boolean simple, 
		 byte[] buf, int off, int len) {
	
	// Save in case we need to extend the number.
	int off0 = off;
	int pos = 0;
	
	if (exp < 0) {
	    buf[off] = (byte) '0';
	    len -= 1;
	    off += 1;
	    if (len > 0) {
		buf[off] = (byte) '.';
		off += 1;
		len -= 1;
	    }
	    // Leading 0s in small numbers.
	    int cexp = exp;
	    while (cexp < -1 && len > 0) {
	        buf[off] = (byte) '0';
	        cexp += 1;
	        off  += 1;
	        len  -= 1;
	    }
	    
	} else {
	
	    // Print out all digits to the left of the decimal.
	    while (exp >= 0  && pos < lmant) {
	        buf[off] = mant[pos];
	        off  += 1;
	        pos  += 1;
	        len  -= 1;
	        exp  -= 1;
	    }
	    // Trust we have enough space for this.
	    for (int i=0; i<= exp; i += 1) {
		buf[off] = (byte)'0';
		off += 1;
		len -= 1;
	    }
	    
	    // Add in a decimal if we have space.
	    if (len > 0) {
	        buf[off] = (byte)'.';
		len -= 1;
	        off += 1;
	    }
	}

	// Now handle the digits to the right of the decimal.
	while (len > 0 && pos < lmant) {
	    buf[off] = mant[pos];
	    off   += 1;
	    exp   -= 1;
	    len   -= 1;
	    pos   += 1;
	}
	
	// Now handle rounding.
	
	if (pos < lmant && mant[pos] >= (byte)'5') {
	    int i;
	    
	    // Increment to the left until we find a non-9
	    for (i=off-1; i >= off0; i -= 1) {
		
		
		if (buf[i] == (byte)'.' || buf[i] == (byte)'-') {
		    continue;
		}
		if (buf[i] == (byte)'9') {
		    buf[i] = (byte)'0';
		} else {
		    buf[i] += 1;
		    break;
		}
	    }
	    
	    // Now we handle 99.99 case.  This can cause problems
	    // in two cases.  If we are not using scientific notation
	    // then we may want to convert 99.9 to 100., i.e.,
	    // we need to move the decimal point.  If there is no
	    // decimal point, then we must not be truncating on overflow
	    // but we should be allowed to write it to the
	    // next character (i.e., we are not at the end of buf).
	    // 
	    // If we are printing in scientific notation, then we want
	    // to convert 9.99 to 1.00, i.e. we do not move the decimal.
	    // However we need to signal that the exponent should be
	    // incremented by one.
	    // 
	    // We cannot have aligned the number, since that requires
	    // the full precision number to fit within the requested
	    // length, and we would have printed out the entire
	    // mantissa (i.e., pos >= lmant)
	    
	    if (i < off0) {
		
		buf[off0] = (byte)'1';
		boolean foundDecimal = false;
		for (i=off0+1; i < off; i += 1) {
		    if (buf[i] == (byte)'.') {
			foundDecimal = true;
			if (simple) {
			    buf[i] =  (byte)'0';
			    i += 1;
			    if (i < off) {
			        buf[i] = (byte)'.';
			    }
			}
			break;
		    }
		}
		if (simple && !foundDecimal) {
		    buf[off+1] = (byte)'0';   // 99 went to 100
		    off += 1;
		}
		
		off = -off;  // Signal to change exponent if necessary.
	    }
	    
        }
	
	return off;
    }

    /** Fill the buffer with truncation characters.  After filling
      * the buffer, a TruncationException will be thrown if the
      * appropriate flag is set.
      */
    void truncationFiller(byte[] buffer, int offset, int length)
      throws TruncationException {
       
        for (int i=offset; i<offset+length; i += 1) {
	    buffer[i] = truncationFill;
	}
	if (truncationThrow) {
	    throw new TruncationException();
	}
	return;
    }

    /** Fill the buffer with blanks to align
      * a field.
      */
    public int alignFill(byte[] buffer, int offset, int len) {
	for (int i=offset; i<offset+len; i += 1) {
	    buffer[i] = (byte)' ';
	}
	return offset+len;
    }
    

}
