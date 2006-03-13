package nom.tam.util;

/** This class provides routines
  *  for efficient parsing of data stored in a byte array.
  *  This routine is optimized (in theory at least!) for efficiency
  *  rather than accuracy.  The values read in for doubles or floats
  *  may differ in the last bit or so from the standard input
  *  utilities, especially in the case where a float is specified
  * as a very long string of digits (substantially longer than
  * the precision of the type).
  * <p>
  * The get methods generally are available with or without a length
  * parameter specified.  When a length parameter is specified only
  * the bytes with the specified range from the current offset will
  * be search for the number.  If no length is specified, the entire
  * buffer from the current offset will be searched.
  * <p>
  * The getString method returns a string with leading and trailing
  * white space left intact.  For all other get calls, leading
  * white space is ignored.  If fillFields is set, then the get
  * methods check that only white space follows valid data and a
  * FormatException is thrown if that is not the case.  If
  * fillFields is not set and valid data is found, then the
  * methods return having read as much as possible.  E.g., for
  * the sequence "T123.258E13", a getBoolean, getInteger and
  * getFloat call would return true, 123, and 2.58e12 when
  * called in succession.
  *  
  */
public class ByteParser {

    /** Array being parsed */
    private byte[] input;

    /** Current offset into input. */
    private int offset;

    /** Length of last parsed value */
    private int numberLength;

    /** Did we find a sign last time we checked? */
    private boolean foundSign;
    
    /** Do we fill up fields? */
    private boolean fillFields = false;

    /** Construct a parser.
      * @param input	The byte array to be parsed.
      *                 Note that the array can be re-used by
      *                 refilling its contents and resetting the offset.
      */
    public ByteParser(byte[] input) {
        this.input    = input;
        this.offset   = 0;
    }
    
    /** Set the buffer for the parser */
    public void setBuffer(byte[] buf) {
        this.input   = buf;
	this.offset  = 0;
    }
    
    /** Get the buffer being used by the parser */
    public byte[] getBuffer() {
        return input;
    }

    /** Set the offset into the array.
      * @param offset	The desired offset from the beginning
      *                  of the array.
      */
    public void setOffset(int offset) {
	    this.offset = offset;
    }
    
    /** Do we require a field to completely fill up the specified
      * length (with optional leading and trailing white space.
      @param flag	Is filling required?
      */
    public void setFillFields(boolean flag) {
        fillFields = flag;
    }

    /** Get the current offset
      @return The current offset within the buffer.
      */
    public int getOffset() {
	    return offset;
    }

    /** Get the number of characters used to parse the previous
      *  number (or the length of the previous String returned).
      */
    public int getNumberLength() {
	    return numberLength;
    }

    /** Read in the buffer until a double is read.  This will read
      * the entire buffer if fillFields is set.
      * @return The value found.
      */
    public double getDouble() throws FormatException {
         return getDouble(input.length-offset);
    }
    
    /** Look for a double in the buffer.
      * Leading spaces are ignored.
      * @param length	The maximum number of characters
      *                  used to parse this number.  If fillFields
      *                  is specified then exactly only whitespace may follow
      *                  a valid double value.
      */
    public double getDouble(int length) throws FormatException {
//	System.out.println("Checking: "+new String(input, offset, length));

        int startOffset = offset;
	
	boolean error = true;

	double number = 0;
	int i = 0;

	// Skip initial blanks.
	length -= skipWhite(length);
	if (length == 0) {
	    return 0;
	}

	double mantissaSign = checkSign();
	if (foundSign) {
	    length -= 1;
	}

	number = getBareInteger(length);   // This will update offset
	length -= numberLength;            // Set by getBareInteger

        if (numberLength > 0) {
	    error = false;
	}
	
	// Check for fractional values after decimal
	if (length > 0 && input[offset] == '.') {

	    offset += 1;
	    length -= 1;

	    double numerator = getBareInteger(length);
	    if (numerator > 0) {
		number += numerator/Math.pow(10.,numberLength);
	    }
	    length -= numberLength;
	    if (numberLength > 0) {
	        error = false;
	    }
	}
	
	if (error) {
	     offset       = startOffset;
	     numberLength = 0;
	     throw new FormatException("Invalid real field");
        }
	
	// Look for an exponent
        if (length > 0) {

	    // Our Fortran heritage means that we allow 'D' for the exponent indicator.
	    if (input[offset] == 'e'  || input[offset] == 'E' ||
		input[offset] == 'd'  || input[offset] == 'D') {

		offset += 1;
		length -= 1;
		if (length > 0) {
		    int sign = checkSign();
		    if (foundSign) {
			length -= 1;
		    }

                    int exponent = (int) getBareInteger(length);
		    number *= Math.pow(10.,exponent*sign);
		    length -= numberLength;
                }
	    }
	}
	
	if (fillFields && length > 0) {
	
	    if (isWhite(length)) {
	        offset += length;
	    } else {
	        numberLength = 0;
		offset = startOffset;
	        throw new FormatException("Non-blanks following real.");
	    }
	}

	numberLength = offset-startOffset;
	return mantissaSign*number;
    }


    /** Get a floating point value from the buffer.  (see getDouble(int())
      */
    public float getFloat() throws FormatException {
	return  (float)getDouble(input.length-offset);
    }

    /** Get a floating point value in a region of the buffer */
    public float getFloat(int length) throws FormatException {
	return (float) getDouble(length);
    }
   
   /** Convert a region of the buffer to an integer */
    public int getInt(int length) throws FormatException  {
        int startOffset = offset;
	
	length -= skipWhite(length);
	
	int number = 0;
	boolean error = true;
	    
	int sign = checkSign();
	if (foundSign) {
	    length -= 1;
	}
	
        while (length > 0 && input[offset] >= '0'  && input[offset] <= '9') {
	    number = number*10 + input[offset]-'0';
	    offset += 1;
	    length -= 1;
	    error = false;
	}
	
	if (error) {
	    numberLength = 0;
	    offset = startOffset;
	    throw new FormatException("Invalid Integer");
	}
	
	if (length > 0 && fillFields) {
	    if (isWhite(length)) {
	        offset += length;
	    } else {
	        numberLength = 0;
		offset = startOffset;
	        throw new FormatException("Non-white following integer");
	    }
	}
	
	numberLength = offset-startOffset;
	return sign*number;
    }

    /** Look for an integer at the beginning of the buffer */
    public int getInt() throws FormatException {
        return getInt(input.length-offset);
    }


    /** Look for a long in a specified region of the buffer */
    public long getLong(int length) throws FormatException {

	int startOffset = offset;

	// Skip white space.
	length -= skipWhite(length);

	long number = 0;
	boolean error=true;
	    
	long sign = checkSign();
	if (foundSign) {
	    length -= 1;
	}
	
        while (length > 0  && input[offset] >= '0'  && input[offset] <= '9') {
	    number = number*10 + input[offset]-'0';
	    error = false;
	    offset += 1;
	    length -= 1;
	}
	
	if (error) {
	    numberLength = 0;
	    offset = startOffset;
	    throw new FormatException("Invalid long number");
	}
	
	if (length > 0 && fillFields) {
            if (isWhite(length)) {
	        offset += length;
	    } else {
	        offset = startOffset;
		numberLength = 0;
		throw new FormatException("Non-white following long");
	    }
	}
	numberLength = offset - startOffset;
	return sign*number;
    }

    /** Get a string
      * @param length  The length of the string.
      */
    public String getString(int length) {

        String s = new String(input, offset, length);
	offset += length;
	numberLength = length;
	return s;
    }
    
    /** Get a boolean value from the beginning of the buffer */
    public boolean getBoolean() throws FormatException {
         return getBoolean(input.length-offset);
    }

    /** Get a boolean value from a specified region of the buffer */
    public boolean getBoolean(int length) throws FormatException  {

        int startOffset = offset;
        length -= skipWhite(length);
	if (length == 0) {
	    throw new FormatException("Blank boolean field");
	}
	
	boolean value = false;
	if (input[offset] == 'T'  || input[offset] == 't') {
	    value = true;
	} else if (input[offset] != 'F' && input[offset] != 'f') {
            numberLength = 0;
	    offset = startOffset;
	    throw new FormatException("Invalid boolean value");
	}
	offset += 1;
	length -= 1;
	
	if (fillFields && length > 0) {
	    if (isWhite(length)) {
	        offset += length;
	    } else {
	        numberLength = 0;
		offset = startOffset;
	        throw new FormatException("Non-white following boolean");
	    }
	}
	numberLength = offset-startOffset;
	return value;
    }
    
    /** Skip bytes in the buffer */
    public void skip(int nBytes) {
        offset += nBytes;
    }


    /** Get the integer value starting at the current position.
      * This routine returns a double rather than an int/long
      * to enable it to read very long integers (with reduced
      * precision) such as 111111111111111111111111111111111111111111.
      * Note that this routine does set numberLength.
      *
      * @param length	The maximum number of characters to use.
      */
    private double getBareInteger(int length) {

        int startOffset = offset;
	double number = 0;

	while (length > 0 && input[offset] >= '0' && input[offset] <= '9') {

	    number *= 10;
	    number += input[offset] - '0';
	    offset += 1;
	    length -= 1;
	}
	numberLength = offset-startOffset;
        return number;
    }

    /** Skip white space.  This routine skips with space in
      * the input and returns the number of character skipped.
      * White space is defined as ' ', '\t', '\n' or '\r'
      *
      * @param length The maximum number of characters to skip.
      */
    public int skipWhite(int length) {

	int i;
	for (i=0; i<length; i += 1) {
	    if (input[offset+i] != ' '   && input[offset+i] != '\t'  &&
	        input[offset+i] != '\n'  && input[offset+i] != '\r') {
		break;
	    }
        }

	offset += i;
	return i;

    }

    /** Find the sign for a number .
      * This routine looks for a sign (+/-) at the current location
      * and return +1/-1 if one is found, or +1 if not.
      * The foundSign boolean is set if a sign is found and offset is
      * incremented.
      */
    private int checkSign () {

	foundSign = false;

        if (input[offset] == '+') {
	    foundSign = true;
	    offset += 1;
	    return 1;
	} else if (input[offset] == '-') {
	    foundSign = true;
	    offset += 1;
	    return -1;
	}

	return 1;
    }


    /** Is a region blank?
      * @param length The length of the region to be tested
      */
    public boolean isWhite(int length) {
        int oldOffset = offset;
        boolean value= skipWhite(length) == length;
	offset = oldOffset;
	return value;
    }
	
}
