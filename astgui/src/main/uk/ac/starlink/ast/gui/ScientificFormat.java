/*
 * Copyright (C) 1998 Raymond L. Plante
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Library General Public License as published
 * by the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version. 
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Library
 * General Public License for more details. 
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 675 Mass Ave, Cambridge, MA 02139, USA. 
 *
 *-------------------------------------------------------------------------
 * History: 
 *  98winter rlp  Original version (as an abstract class)
 *  98oct26  rlp  fixed bug in format() when number=0, mode=flex.
 *  99apr06  rlp  fix similar bug as 98oct26 in 2nd version of format;
 *  05jun02  pwd  Import into ASTGUI to replace use of DecimalFormat.
 *                  
 */
//package rplante.text;
package uk.ac.starlink.ast.gui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;
import java.util.Vector;

/**
 * a formatter that uses a format suitable for numeric values in a 
 * scientific context.  <p>
 *
 * The aim of this class is to provide a flexible way to format large 
 * floating-point numbers. Its three modes allow one to format a 
 * fixed-precision decimal format, fixed-precision scientific notation, or 
 * a flexible format. With the flexible mode, scientific notation will be 
 * used if the decimal format would be larger than a specified number of
 * characters (similar to the %g format in the C function printf). <p>
 *
 * The format can be fully configured just as its cousin, 
 * java.text.DecimalFormat, can.  The pattern syntax supported by the 
 * applyPattern() method is a superset of that supported by DecimalFormat. 
 * (In fact, when a pure DecimalFormat pattern is used with this class,
 * the resulting format is identical to that of DecimalFormat.)  Below is 
 * a semi-formal description of the pattern syntax (see also 
 * java.text.DecimalFormat API documentation): <p>
 *
 * Patterns: <br>
 * <pre>
 * pattern       := ('!'exppattern | '~altpattern' | {'@'}decipattern)
 * decipattern   := <em>a DecimalFormat-supported pattern</em> 
 * altpattern    := decipattern{:int}{^intpattern}
 * exppattern    := decipattern{^intpattern}
 * intpattern    := subintpattern{;subintpattern}
 * subintpattern := {prefix}integer{suffix}
 * int           := <em>an integer number</em>
 * prefix,suffix := '\\u0000'..'\\uFFFD'
 * </pre>
 *
 * One uses the !<em>exppattern</em> to force an exponential format, 
 * <code>@</code><em>decipattern</em> for a decimal format, and ~<em>altpattern</em> for a 
 * flexible format.   The ^<em>intpattern</em> specificly controls the 
 * format of the exponent when applicable.  The default pattern is 
 * "<code>~#0.####;-#0.####:9^E+#0;E-#0</code>"; that is, it uses the 
 * flexible mode that results in decimal format when the number can be 
 * formatted to less than 9 characters and exponential otherwise.  <p>
 *
 * This author finds the pattern syntax sufficiently opaque that he 
 * recommends instantiating with the default behavior and then modifying 
 * it via the various set methods.  For example, setMaxWidth() will change
 * the cut-off width between decimal and exponential formats.  The 
 * set...Mode() methods can be used to switch between the different 
 * formatting modes.  <p>
 *
 * @see java.text.DecimalFormat
 * @author Raymond L. Plante
 * @version 1.1
 */
 
 //http://monet.astro.uiuc.edu/~rplante/topics/java/ScientificFormat.html
 //doesn't seem to work, but this is main reference. See also:
 //http://monet.ncsa.uiuc.edu/~rplante/
 
public class ScientificFormat extends NumberFormat {

    protected DecimalFormat efmt = null;
    protected DecimalFormat dfmt = null;

    protected int maxwidth = 0;

    boolean parseMatissaOnly = false;
    boolean flexmode = true;
    boolean expmode  = false;
    boolean decmode  = false;

    protected final static String defdfmt = "#0.####;-#0.####";
    protected final static String defefmt = "E+#0;E-#0";

    public final static int EXPONENT_FIELD = 3;

    /**
     * Create a ScientificFormat using the default pattern and symbols 
     * for the default locale.  
     */
    public ScientificFormat() { 
	this(Locale.ENGLISH);
    }

    /**
     * Create a ScientificFormat using the default pattern for the 
     * given locale.  
     */
    public ScientificFormat(Locale locale) {
	DecimalFormatSymbols dfs = new DecimalFormatSymbols(locale);
	dfmt = new DecimalFormat(defdfmt, dfs);
	efmt = new DecimalFormat(defefmt, dfs);
//	dfmt.applyPattern(defdfmt);
    }

    /**
     * Creates a ScientificFormat using a given pattern
     */
    public ScientificFormat(String pattern) {
	this();
	applyPattern(pattern);
    }

    /**
     * Creates a ScientificFormat using a given pattern for the specified
     * locale
     */
    public ScientificFormat(Locale locale, String pattern) {
        this(locale);
	applyPattern(pattern);
    }

    /**
     * apply this pattern
     */
    public void applyPattern(String pattern) throws IllegalArgumentException {
	int p, l;
	String sub;

	// remove quoted material
	Object[] fmtquotes = extractQuotedStrings(pattern);
	String pat = (String) fmtquotes[0];
	Vector quoteds = (Vector) fmtquotes[1];
	Vector qpos = (Vector) fmtquotes[2];

	// get exponential part of pattern
	p = pat.indexOf('^');
	l = pat.lastIndexOf('^');
	if (p != l) 
	    throw new IllegalArgumentException("Bad formatting string: " +
		                               "too many ^s");
	try {
	    sub = pat.substring(p+1).trim();
	    pat = pat.substring(0, p).trim();
	    if (sub != null && sub.length() > 0) {
		if (sub.indexOf('.') >= 0) throw new 
		    IllegalArgumentException("Bad formatting string: " +
					     "integer exponents only");
		String epat = replaceQuotedStrings(quoteds, qpos, sub, p+1);
		try {
		    efmt.applyPattern(epat);
		} catch (IllegalArgumentException ex) {
		    throw new IllegalArgumentException("Bad formatting " +
						       "string: bad exponent " +
						       "portion: " + epat);
		}
	    }
	} catch (StringIndexOutOfBoundsException ex) { }

	// get the max-width part of pattern
	p = pat.indexOf(':');
	l = pat.lastIndexOf(':');
	if (p != l) 
	    throw new IllegalArgumentException("Bad formatting string: " +
		                               "too many :s");
	maxwidth = 0;
	try {
	    sub = pat.substring(p+1).trim();
	    pat = pat.substring(0, p).trim();
	    if (sub != null && sub.length() > 0) 
		maxwidth = Integer.parseInt(sub);
	} 
	catch (NumberFormatException ex) {
	    throw new IllegalArgumentException("Bad formatting string: " +
					       "bad max-width value");
	}	
	catch (StringIndexOutOfBoundsException ex) { }

	// Now determine mode
	p = 1;
	if (pat.charAt(0) == '!') {
	    setExponentialMode();
	}
	else if (pat.charAt(0) == '~') {
	    setFlexibleMode();
	}
	else {
	    setDecimalMode();
	    if (pat.charAt(0) != '@') p = 0;
	}
	try {
	    sub = (p > 0) ? pat.substring(p) : pat;
	    pat = replaceQuotedStrings(quoteds, qpos, sub, p);
	    try {
		dfmt.applyPattern(pat);
	    } catch (IllegalArgumentException ex) {
		throw new IllegalArgumentException("Bad formatting string: " +
						   "bad decimal portion: " + 
		                                   pat);
	    }
	}
	catch (StringIndexOutOfBoundsException ex) { }
    }

    protected static Object[] extractQuotedStrings(String text) { 
	Object[] out = new Object[3];
	Vector qstrs = new Vector();
	Vector qpos = new Vector();
	StringBuffer ostr = new StringBuffer();
	out[1] = qstrs;
	out[2] = qpos;

	int ppos = 0;
	int bp, al, nl=0, opp;
	while (ppos < text.length() && (bp = text.indexOf("'", ppos)) >= 0) {
	    al = bp - ppos;
	    opp = ppos;
	    ppos = text.indexOf("'", bp+1);
	    if (ppos < 0) break;

	    nl += al;
	    ostr.append(text.substring(ppos, bp-ppos));
	    qstrs.addElement(text.substring(bp, 1+ppos-bp));
	    qpos.addElement(Integer.valueOf(nl));
	}

	ostr.append(text.substring(ppos));
	out[0] = ostr.toString();

	return out;
    }

    protected static String 
    replaceQuotedStrings(Vector strings, Vector positions, 
			 String input, int start) 
    {
	if (strings == null || strings.size() == 0) return input;

	int p, b=0;
	StringBuffer out = new StringBuffer();
	int n = Math.min(strings.size(), positions.size());
	for(int i=0; i < n; i++) {
	    p = ((Integer) positions.elementAt(i)).intValue() - start;
	    if (start >= 0 && p < input.length()) {
		out.append(input.substring(b, p-b));
		out.append((String) strings.elementAt(i));
		b = p;
	    }
	}
	out.append(input.substring(b));

	return out.toString();
    }

    public void applyLocalizedPattern(String pattern) {
	applyPattern(pattern);
    }

    public void applyMantissaPattern(String pattern) {
	dfmt.applyPattern(pattern);
    }

    public void applyExponentPattern(String pattern) {
	efmt.applyPattern(pattern);
    }

    /** 
     * the the maximum width of the decimal format.  This is used only 
     * while in flexible mode; if the decimal is greater than this width,
     * exponential mode will be used.  A negative value means use the 
     * default width.
     */
    public void setMaxWidth(int width) {
	maxwidth = (width > 0) ? width : 0;
    }

    public int getMaxWidth() { 
	if (maxwidth > 0) return maxwidth; 

	return (Math.max(getPositivePrefix().length(), 
			 getNegativePrefix().length()) +
		Math.max(getPositiveSuffix().length(), 
			 getNegativeSuffix().length()) +
		getMaximumFractionDigits() + 
		Math.max(getMinimumIntegerDigits(), 3) + 1);
    }

    public String getPositivePrefix() { return dfmt.getPositivePrefix(); }
    public void setPositivePrefix(String newValue) { 
	dfmt.setPositivePrefix(newValue);
    }

    public String getNegativePrefix() { return dfmt.getNegativePrefix(); }
    public void setNegativePrefix(String newValue) { 
	dfmt.setNegativePrefix(newValue);
    }

    public String getPositiveSuffix() { return dfmt.getPositiveSuffix(); }
    public void setPositiveSuffix(String newValue) { 
	dfmt.setPositiveSuffix(newValue);
    }

    public String getNegativeSuffix() { return dfmt.getNegativeSuffix(); }
    public void setNegativeSuffix(String newValue) { 
	dfmt.setNegativeSuffix(newValue);
    }

    public String getExpPositivePrefix() { return efmt.getPositivePrefix(); }
    public void setExpPositivePrefix(String newValue) { 
	efmt.setPositivePrefix(newValue);
    }

    public String getExpNegativePrefix() { return efmt.getNegativePrefix(); }
    public void setExpNegativePrefix(String newValue) { 
	efmt.setNegativePrefix(newValue);
    }

    public String getExpPositiveSuffix() { return efmt.getPositiveSuffix(); }
    public void setExpPositiveSuffix(String newValue) { 
	efmt.setPositiveSuffix(newValue);
    }

    public String getExpNegativeSuffix() { return efmt.getNegativeSuffix(); }
    public void setExpNegativeSuffix(String newValue) { 
	efmt.setNegativeSuffix(newValue);
    }

    public void setExponentialMode() {
	expmode = true;
	decmode = flexmode = false;
    }
    public boolean isExponentialMode() { return expmode; }

    public void setDecimalMode() {
	decmode = true;
	expmode = flexmode = false;
    }
    public boolean isDecimalMode() { return decmode; }

    public void setFlexibleMode() {
	flexmode = true;
	decmode = expmode = false;
    }
    public boolean isFlexibleMode() { return flexmode; }

    public int getGroupingSize() { return dfmt.getGroupingSize(); }
    public void setGroupingSize(int newValue) {
	dfmt.setGroupingSize(newValue);
    }

    public int getExpGroupingSize() { return dfmt.getGroupingSize(); }
    public void setExpGroupingSize(int newValue) {
	dfmt.setGroupingSize(newValue);
    }

    public boolean isDecimalSeparatorAlwaysShown() { 
	return dfmt.isDecimalSeparatorAlwaysShown();
    }
    public void setDecimalSeparatorAlwaysShown(boolean newValue) {
	dfmt.setDecimalSeparatorAlwaysShown(newValue);
    }

    public boolean equals(Object obj) {
	if (! (obj instanceof ScientificFormat)) return false;

	ScientificFormat that = (ScientificFormat) obj;
	if  (this.decmode != that.decmode ||
	     this.expmode != that.expmode ||
	     this.flexmode != that.flexmode) return false;

	if (! this.dfmt.equals(that.dfmt)) return false;

	if (! decmode && ! this.efmt.equals(that.efmt)) return false;
	if (flexmode && this.maxwidth != that.maxwidth) return false;

	return true;
    }

    public Object clone() {
	ScientificFormat out = (ScientificFormat) super.clone();
	out.dfmt = (DecimalFormat) dfmt.clone();
	out.efmt = (DecimalFormat) efmt.clone();
	return out;
    }

    public String toPattern() {
	StringBuffer out = new StringBuffer();

	if (expmode) out.append("!");
	else if (flexmode) out.append("~");
	else out.append("@");

	out.append(dfmt.toPattern());

	if (flexmode) out.append(":" + maxwidth);

	if (! decmode) out.append("^" + efmt.toPattern());

	return out.toString();
    }

    public StringBuffer format(double number, StringBuffer result, 
			       FieldPosition fieldPosition) {
	if (expmode) {
	    return exponentialFormat(number, result, fieldPosition);
	}
	else if (flexmode) {
	    int sign = (number < 0) ? -1 : 1;
	    int mxwd = getMaxWidth();
	    double mantissa = Math.abs(number),
		   exponent = 0;

	    if (mantissa != 0) {
		for(; mantissa > 10; mantissa /= 10, exponent++);
		for(; mantissa <  1; mantissa *= 10, exponent--);
	    }

	    if (exponent < (1 - dfmt.getMaximumFractionDigits()) ||
		exponent - dfmt.getMaximumFractionDigits() > mxwd - 2) 
	    {
		return exponentialFormat(sign*mantissa, exponent,
					 result, fieldPosition);
	    }
	    else {
		StringBuffer tmp = new StringBuffer();
		dfmt.format(number, tmp, fieldPosition);
		if (tmp.length() > mxwd) {
		    return exponentialFormat(sign*mantissa, exponent, 
					     result, fieldPosition);
		}
		else {
		    result.append(tmp.toString());
		    return result;
		}
	    }
	}
	else {
	    return dfmt.format(number, result, fieldPosition);
	}
	    
    }

    public StringBuffer format(long number, StringBuffer result, 
			       FieldPosition fieldPosition) {
	if (expmode) {
	    return exponentialFormat(number, result, fieldPosition);
	}
	else if (flexmode) {
	    int sign = (number < 0) ? -1 : 1;
	    int mxwd = getMaxWidth();
	    double mantissa = Math.abs(number),
		   exponent = 0;

	    if (mantissa != 0) {
		for(; mantissa > 10; mantissa /= 10, exponent++);
		for(; mantissa <  1; mantissa *= 10, exponent--);
	    }

	    if (exponent < (1 - dfmt.getMaximumFractionDigits()) ||
		exponent - dfmt.getMaximumFractionDigits() > mxwd - 2) 
	    {
		return exponentialFormat(sign*mantissa, exponent,
					 result, fieldPosition);
	    }
	    else {
		StringBuffer tmp = new StringBuffer();
		dfmt.format(number, tmp, fieldPosition);
		if (tmp.length() > mxwd) {
		    return exponentialFormat(sign*mantissa, exponent, 
					     result, fieldPosition);
		}
		else {
		    result.append(tmp.toString());
		    return result;
		}
	    }
	}
	else {
	    return dfmt.format(number, result, fieldPosition);
	}
	    
    }

    private StringBuffer exponentialFormat(double mantissa, double exponent, 
					   StringBuffer result, 
					   FieldPosition fieldPosition) 
    {
	FieldPosition intfp = new FieldPosition(0);
	FieldPosition ruse, euse;

	if (fieldPosition.getField() == EXPONENT_FIELD) {
	    ruse = intfp;
	    euse = fieldPosition;
	}
	else {
	    ruse = fieldPosition;
	    euse = intfp;
	}

	dfmt.format(mantissa, result, ruse);
	efmt.format(exponent, result, euse);

	return result;
    }

    private StringBuffer exponentialFormat(double number, StringBuffer result, 
					   FieldPosition fieldPosition) {
	int exp=0;
	int sign = (number < 0) ? -1 : 1;
	number = Math.abs(number);

	for(; number > 10; number /= 10, exp++);
	for(; number < 1; number *= 10, exp--);

	return exponentialFormat(sign*number, exp, result, fieldPosition);
    }

    private StringBuffer exponentialFormat(long number, StringBuffer result, 
					   FieldPosition fieldPosition) {

	int exp=0;
	FieldPosition intfp = new FieldPosition(0);
	FieldPosition ruse, euse;

	for(; number > 10; number /= 10, exp++);
	for(; number < 1; number *= 10, exp--);

	if (fieldPosition.getField() == EXPONENT_FIELD) {
	    ruse = intfp;
	    euse = fieldPosition;
	}
	else {
	    ruse = fieldPosition;
	    euse = intfp;
	}

	dfmt.format(number, result, ruse);
	efmt.format(exp, result, euse);

	return result;
    }

    /**
     * Parse out a number from the given text and return it as a Long if 
     * possible; otherwise, return it as a Double.  If parseIntegerOnly is
     * set, parsing will stop at a decimal point (or equivalent).  How the 
     * string is interpreted (i.e. as a normal decimal or as exponential
     * notation) depends on the currently set mode.
     */
    public Number parse(String text, ParsePosition parsePosition) {
	if (expmode) {
	    return parseAsExponential(text, parsePosition);
	}
	else if (flexmode) {
	    return parseAsFlexible(text, parsePosition);
	}
	else {
	    return parseAsDecimal(text, parsePosition);
	}
    }

    /** 
     * parse the input string, interpreting it as exponential format.
     * If parseIntegerOnly is set, return only the integer portion of 
     * the mantissa.  (To return the decimal portion of the mantissa, 
     * use parseAsDecimal()).
     */
    public Number parseAsExponential(String text, ParsePosition parsePosition) {
	Number out = null;
	int p;
	int start = parsePosition.getIndex();

	Number mantissa = dfmt.parse(text, parsePosition);
	if (mantissa == null) return null;
	if (isParseIntegerOnly()) return mantissa;

	// this is a work-around for a JDK 1.1.6
	p = parsePosition.getIndex()-1;
	if (p >= 0 && text.charAt(p) == 'E') 
	    parsePosition.setIndex(p);

	Number expo = efmt.parse(text, parsePosition);
	if (expo == null) {
	    parsePosition.setIndex(start);
	    return null;
	}

	double val = mantissa.doubleValue() * 
	    Math.pow(10, expo.doubleValue());
	if (mantissa instanceof Long && expo instanceof Long) 
	    out = Long.valueOf(Math.round(val));
	else 
	    out = Double.valueOf(val);

	return out;
    }

    /**
     * parse the input string, interpreting it as regular decimal format.
     * This behaves just as DecimalFormat.parse().
     */
    public Number parseAsDecimal(String text, ParsePosition parsePosition) {
	return dfmt.parse(text, parsePosition);
    }

    /**
     * parse the input string, interpreting it as either regular decimal
     * format or exponential format.  Exponential format is tried first;
     * failing that decimal format is tried.  
     */
    public Number parseAsFlexible(String text, ParsePosition parsePosition) {

	// parse as if it is an exponential type
	Number out = parseAsExponential(text, parsePosition);
	if (out != null) return out;

	// it's a decimal type after all
	return parseAsDecimal(text, parsePosition);
    }

    public void setParseIntegerOnly(boolean value) {
	super.setParseIntegerOnly(value);
	dfmt.setParseIntegerOnly(value);
    }

    public boolean isGroupingUsed() { return dfmt.isGroupingUsed(); }
    public void setGroupingUsed(boolean newValue) { 
	dfmt.setGroupingUsed(newValue); 
    }

    public int getMaximumIntegerDigits() { 
	return dfmt.getMaximumIntegerDigits(); 
    }
    public void setMaximumIntegerDigits(int newValue) {
	dfmt.setMaximumIntegerDigits(newValue);
    }

    public int getMinimumIntegerDigits() { 
	return dfmt.getMinimumIntegerDigits(); 
    }
    public void setMinimumIntegerDigits(int newValue) {
	dfmt.setMinimumIntegerDigits(newValue);
    }

    public int getMaximumFractionDigits() { 
	return dfmt.getMaximumFractionDigits(); 
    }
    public void setMaximumFractionDigits(int newValue) {
	dfmt.setMaximumFractionDigits(newValue);
    }

    public int getMinimumFractionDigits() { 
	return dfmt.getMinimumFractionDigits(); 
    }
    public void setMinimumFractionDigits(int newValue) {
	dfmt.setMinimumFractionDigits(newValue);
    }

    public int getExpMaximumIntegerDigits() { 
	return efmt.getMaximumIntegerDigits(); 
    }
    public void setExpMaximumIntegerDigits(int newValue) {
	efmt.setMaximumIntegerDigits(newValue);
    }

    public int getExpMinimumIntegerDigits() { 
	return efmt.getMinimumIntegerDigits(); 
    }
    public void setExpMinimumIntegerDigits(int newValue) {
	efmt.setMinimumIntegerDigits(newValue);
    }

    public static void main(String[] args) {
	if ( args == null || args.length == 0 || args[0].equals( "" ) ) {
	    args = new String[1];
	    args[0] = "5.428E+2";
	}

	ScientificFormat scifmt = new ScientificFormat();

	StringBuffer buf;
	ParsePosition ppos = new ParsePosition(0);
	Number num;
	double value;

	System.out.println("Maximum width for Flexible Mode: " + 
			   scifmt.getMaxWidth());
	System.out.println("Input\t\tFlexible\tDecimal\tExponential");

	for(int i=0; i < args.length; i++) {
	    buf = new StringBuffer(args[i]);
            System.out.println( "Parsing: " + buf.toString() );

	    // parse out a number from the input string 
	    ppos.setIndex(0);
	    scifmt.setFlexibleMode();      // input as decimal or exponential
	    num = scifmt.parse(args[i], ppos);

	    if (ppos.getIndex() == 0) {
		buf.append("\tUnable to parse out number");
		System.out.println(buf.toString());
		continue;
	    }
	    value = num.doubleValue();

	    // print the number in each mode.
	    // we're already in Flexible mode
	    buf.append("\t\t").append(scifmt.format(value));

	    scifmt.setDecimalMode();
	    buf.append("\t").append(scifmt.format(value));

	    scifmt.setExponentialMode();
	    buf.append("\t").append(scifmt.format(value));

	    System.out.println(buf.toString());
	}
    }


}
