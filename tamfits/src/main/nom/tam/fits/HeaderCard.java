package nom.tam.fits;

 /*
  * Copyright: Thomas McGlynn 1997-1999.
  * This code may be used for any purpose, non-commercial
  * or commercial so long as this copyright notice is retained
  * in the source code or included in or referred to in any
  * derived software.
  * Many thanks to David Glowacki (U. Wisconsin) for substantial
  * improvements, enhancements and bug fixes -- including
  * this class.
  */


/** This class describes methods to access and manipulate the individual
  * cards for a FITS Header.
  */
public class HeaderCard
{
    /** The keyword part of the card (set to null if there's no keyword) */
    private String key;

    /** The value part of the card (set to null if there's no value) */
    private String value;

    /** The comment part of the card (set to null if there's no comment) */
    private String comment;

    /** A flag indicating whether or not this is a string value */
    private boolean isString;

    /** Maximum length of a FITS keyword field */
    public static final int MAX_KEYWORD_LENGTH = 8;

    /** Maximum length of a FITS value field */
    public static final int MAX_VALUE_LENGTH = 70;

    /** padding for building card images */
    private static String space80 = "                                                                                ";

    /** Create a HeaderCard from its component parts
      * @param key keyword (null for a comment)
      * @param value value (null for a comment or keyword without an '=')
      * @param comment comment
      * @exception HeaderCardException for any invalid keyword
      */
    public HeaderCard(String key, double value, String comment)
	              throws HeaderCardException {
	this(key, String.valueOf(value), comment);
	isString = false;
    }
    
    /** Create a HeaderCard from its component parts
      * @param key keyword (null for a comment)
      * @param value value (null for a comment or keyword without an '=')
      * @param comment comment
      * @exception HeaderCardException for any invalid keyword
      */
    public HeaderCard(String key, boolean value, String comment)
	              throws HeaderCardException {
	this(key, value? "T":"F", comment);
	isString = false;
    }
    
    public HeaderCard(String key, int value, String comment) 
      throws HeaderCardException {
	this(key,String.valueOf(value), comment);
	isString = false;
    }
    public HeaderCard(String key, long value, String comment) 
      throws HeaderCardException {
	this(key,String.valueOf(value), comment);
	isString = false;
    }
       
    /** Create a HeaderCard from its component parts
      * @param key keyword (null for a comment)
      * @param value value (null for a comment or keyword without an '=')
      * @param comment comment
      * @exception HeaderCardException for any invalid keyword or value
      */
    public HeaderCard(String key, String value, String comment)
	  throws HeaderCardException
    {
        if (key == null && value != null) {
            throw new HeaderCardException("Null keyword with non-null value");
        }

        if (key != null && key.length() > MAX_KEYWORD_LENGTH) {
	    if (!FitsFactory.getUseHierarch() ||
		!key.substring(0,9).equals("HIERARCH.")) {
                throw new HeaderCardException("Keyword too long");
	    }
        }

	if (value != null) {
            value = value.trim();

            // MBT (16-NOV-2006): Quote processing modified.
            // Comment out the following:
            //
         // if (value.charAt(0) == '\'') {
	 //     if (value.charAt(value.length()-1) != '\'') {
	 //         throw new HeaderCardException("Missing end quote in string value");
	 //     }
         //
	 //     value = value.substring(1,value.length()-1).trim();
         //
         // }

            // If first and last characters are single quotes, strip them.
            // I'm not sure this is the best thing to do, but it's here to
            // provide behaviour which is as compatible as possible with
            // the previous behaviour (see commented-out code above),
            // while fixing the bugs that it exhibited (see code below).
            if (value.length() > 1 && value.charAt(0) == '\''
                                   && value.charAt(value.length()-1) == '\'') {
                value = value.substring(1,value.length()-1).trim();
            }

            if (value.length() > MAX_VALUE_LENGTH) {
	        throw new HeaderCardException("Value too long");
            }
        }

        this.key     = key;
        this.value   = value;
        this.comment = comment;
	isString = true;
    }

    /** Create a HeaderCard from a FITS card image
      * @param card the 80 character card image
      */
    public HeaderCard(String card)
    {
        key      = null;
        value    = null;
        comment  = null;
        isString = false;
	
	if (FitsFactory.getUseHierarch() 
	       && card.length() > 9 
	       && card.substring(0,9).equals("HIERARCH ")) {
	    hierarchCard(card);
	    return;
	}


        // We are going to assume that the value has no blanks in
        // it unless it is enclosed in quotes.  Also, we assume that
        // a / terminates the string (except inside quotes)

        // treat short lines as special keywords
        if (card.length() < 9) {
            key = card;
            return;
        }

        // extract the key
        key = card.substring(0, 8).trim();

        // if it is an empty key, assume the remainder of the card is a comment
        if (key.length() == 0) {
            key = "";
            comment = card.substring(8);
            return;
        }

        // Non-key/value pair lines are treated as keyed comments
        if (!card.substring(8,10).equals("= ")) {
            comment = card.substring(8).trim();
            return;
        }

        // extract the value/comment part of the string
        String valueAndComment = card.substring(10).trim();

        // If there is no value/comment part, we are done.
        if (valueAndComment.length() == 0) {
            value = "";
            return;
        }

        int vend = -1;
        boolean quote = false;

        // If we have a ' then find the matching  '.
        if (valueAndComment.charAt(0) == '\'') {

            int offset = 1;
            while (offset < valueAndComment.length()) {

	        // look for next single-quote character
	        vend = valueAndComment.indexOf("'", offset);;

	        // if the quote character is the last character on the line...
	        if (vend == valueAndComment.length()-1) {
	            break;
	        }

	        // if we did not find a matching single-quote...
	        if (vend == -1) {
	            // pretend this is a comment card
	            key = null;
	            comment = card;
	            return;
	        }

	        // if this is not an escaped single-quote, we are done
	        if (valueAndComment.charAt(vend+1) != '\'') {
	            break;
	        }

	        // skip past escaped single-quote
	        offset = vend+2;
            }

            // break apart character string
            // MBT (16-NOV-2006): unquote quotes 16-NOV-2006
            value = valueAndComment.substring(1, vend)
                                   .replaceAll( "''", "'" )
                                   .trim();
	   
	    if (vend+1 >= valueAndComment.length()) {
		comment = null;
	    } else {
		
	        comment = valueAndComment.substring(vend+1).trim();
	        if (comment.charAt(0) == '/') {
		    if (comment.length() > 1) {
		        comment = comment.substring(1);
		    } else {
			comment = "";
		    }
	        }
		
		if (comment.length() == 0) {
		    comment = null;
		}
		
	    }
            isString = true;
	    
	    
        } else {

            // look for a / to terminate the field.
            int slashLoc = valueAndComment.indexOf('/');
            if (slashLoc != -1) {
                comment = valueAndComment.substring(slashLoc+1).trim();
                value = valueAndComment.substring(0, slashLoc).trim();
	    } else {
		value = valueAndComment;
	    }
        }
    }
    
    /** Process HIERARCH style cards...
     *  HIERARCH LEV1 LEV2 ...  value / comment
     *  The keyword for the card will be "HIERARCH.LEV1.LEV2..."
     *  The value will be the first token which starts with a non-alphabetic
     *  character (i.e., not A-Z or _).
     *  A '/' is assumed to start a comment.
     */
    
    private void hierarchCard(String card) {
	
	String name = "";
	String token = null;
	String separator = "";
	int[] tokLimits;
	int posit = 0;
	int commStart = -1;
	
	// First get the hierarchy levels
	while ((tokLimits = getToken(card,posit)) != null) {
	    token = card.substring(tokLimits[0],tokLimits[1]);
	    if (!token.equals("=")) {
		name += separator + token;
	        separator = ".";
	    } else {
		tokLimits = getToken(card, tokLimits[1]);
		if (tokLimits != null) {
		    token = card.substring(tokLimits[0], tokLimits[1]);
		} else {
		    key = name;
		    value = null;
		    comment = null;
		    return;
		}
		break;
	    }
	    posit = tokLimits[1];
	}
	key = name;
	
	
	// At the end?
	if (tokLimits == null) {
	    value    = null;
	    comment  = null;
	    isString = false;
	    return;
	}
	
	
	if (token.charAt(0) == '\'') {
	    // Find the next undoubled quote...
	    isString = true;
	    if (token.length() > 1 && token.charAt(1) == '\'' &&
		(token.length() == 2 || token.charAt(2) != '\'')) {
		value = "";
		commStart = tokLimits[0]+2;
	    } else if (card.length() < tokLimits[0]+2) {
		value = null;
		comment = null;
		isString= false;
		return;
	    } else {
		int i;
		for (i=tokLimits[0]+1; i<card.length(); i += 1) {
		    if (card.charAt(i) == '\'') { 
			if (i == card.length() - 1) {
			    value = card.substring(tokLimits[0]+1, i);
			    commStart = i+1;
			    break;
			} else if (card.charAt(i+1) == '\'') {
			    // Doubled quotes.
			    i += 1;
			    continue;
			} else {
			    value = card.substring(tokLimits[0]+1, i);
			    commStart = i+1;
			    break;
			}
		    }
		}
	    }
	    if (commStart < 0) {
		value = null;
		comment = null;
		isString = false;
		return;
	    }
	    for (int i=commStart; i<card.length(); i += 1) {
		if (card.charAt(i) == '/') {
		    comment = card.substring(i+1).trim();
		    break;
		} else if (card.charAt(i) != ' ') {
		    comment = null;
		    break;
		}
	    }
	} else  {
	    isString = false;
	    int sl = token.indexOf('/');
	    if (sl == 0) {
		value  = null;
		comment= card.substring(tokLimits[0]+1);
	    } else if (sl > 0) {
		value = token.substring(0,sl);
		comment = card.substring(tokLimits[0]+sl+1);
	    } else {
	        value    = token;
	    
	        for (int i=tokLimits[1]; i<card.length(); i += 1) {
		    if (card.charAt(i) == '/') {
		        comment = card.substring(i+1).trim();
		        break;
		    } else if (card.charAt(i) != ' ') {
		        comment = null;
		        break;
		    }
	        }
	    }
	}
    }
	
    /** Get the next token.  Can't use StringTokenizer
     *  since we sometimes need to know the position within
     *  the string.
     */
	    
    private int[] getToken(String card, int posit) {
	
	int i;
	for (i=posit; i<card.length(); i += 1) {
	    if (card.charAt(i) != ' ') {
		break;
	    }
	}
	
	if (i >= card.length()) {
	    return null;
	}
	
	if (card.charAt(i) == '=') {
	    return new int[]{i,i+1};
	}
	
	int j;
	for (j=i+1; j < card.length(); j += 1) {
	    if (card.charAt(j) == ' ' || card.charAt(j) == '=') {
		break;
	    }
	}
	return new int[]{i,j};
    }
	
	
	

    /** Does this card contain a string value?
      */
    public boolean isStringValue()
    {
        return isString;
    }

    /** Is this a key/value card?
      */
    public boolean isKeyValuePair()
    {
        return (key != null && value != null);
    }

    /** Set the key.
     */
    void setKey(String newKey) {
	key = newKey;
    }
    
    /** Return the keyword from this card
      */
    public String getKey()
    {
        return key;
    }

    /** Return the value from this card
      */
    public String getValue()
    {
        return value;
    }

    /** Return the comment from this card
      */
    public String getComment()
    {
        return comment;
    }

    /** Takes an arbitrary String object and turns it into a string with
      * characters than can be harmlessly output to a FITS header.
      * The FITS standard excludes certain characters; moreover writing
      * non-7-bit characters can end up producing multiple bytes per
      * character in some text encodings, leading to a corrupted header.
      * @param str input string
      */
    private static String sanitize( String str ) {
        int nc = str.length();
        char[] cbuf = new char[ nc ];
        for ( int ic = 0; ic < nc; ic++ ) {
            char c = str.charAt( ic );
            cbuf[ ic ] = ( c >= 0x20 && c <= 0x7e ) ? c : '?';
        }
        return new String( cbuf );
    }

    /** Return the 80 character card image
      */
    public String toString()
    {
        StringBuffer buf = new StringBuffer(80);

        // start with the keyword, if there is one
        if (key != null) {
	    if (key.length() > 9 && key.substring(0,9).equals("HIERARCH.")){
                // MBT (01-APR-2011): sanitize output string
		return sanitize(hierarchToString());
	    }
            buf.append(key);
	    if (key.length() < 8) {
                buf.append(space80.substring(0, 8-buf.length()));
            }
	}

        if (value != null) {
            String val = value.replaceAll( "'", "''" );
            buf.append("= ");

            if (isString) {
	        // left justify the string inside the quotes
	        buf.append('\'');
	        buf.append(val);
		if (buf.length() < 19) {
		  
		    buf.append(space80.substring(0, 19-buf.length()));
		}
	        buf.append('\'');
		// Now add space to the comment area starting at column 40
		if (buf.length() < 30) {
		    buf.append(space80.substring(0, 30-buf.length()));
		}
		
            } else {
		
	        int offset = buf.length();
		if (val.length() < 20) {
		    buf.append(space80.substring(0, 20-val.length()));
		}
		
	        buf.append(val);

            }

            // if there is a comment, add a comment delimiter
            if (comment != null) {
	        buf.append(" / ");
            }
	    
        } else if (comment != null && comment.startsWith("= ")) {
            buf.append("  ");
        }

        // finally, add any comment
        if (comment != null) {
            buf.append(comment);
        }

        // make sure the final string is exactly 80 characters long
        if (buf.length() > 80) {
            buf.setLength(80);
	    
        } else {

            if (buf.length() < 80) {
	        buf.append(space80.substring(0, 80-buf.length()));
            }
        }

        // MBT (01-APR-2011): sanitize output string
        return sanitize(buf.toString());
    }
    private String hierarchToString() {
	
	
	StringBuffer b = new StringBuffer(80);
	int p=0;
	String space = "";
	while (p < key.length()) {
	    int q = key.indexOf('.', p);
	    if (q < 0) {
		b.append(space+key.substring(p));
		break;
	    } else {
		b.append(space+key.substring(p,q));
	    }
	    space = " ";
	    p = q+1;
	}
	if (value != null) {
	    // Try to align values
	    int avail = 78 - (b.length() + value.length());
	    if (isString) {
		avail -= 2;
	    }
	    if (comment != null) {
		avail -= 3+comment.length();
	    }
	    
	    if (avail > 0 && b.length() < 29) {
	        b.append(space80.substring(0,Math.min(avail, 29-b.length())));
	    }
	    
	    b.append("= ");
	    if (isString) {
		b.append('\'');
	    } else if (avail > 0 && value.length() < 10) {
		b.append(space80.substring(0,Math.min(avail, 10-value.length())));
	    }
	    b.append(value);
	    if (isString) {
		b.append('\'');
	    }
	}
	
        if (comment != null) {
	    b.append(" / "+comment);
	}
	if (b.length() < 80) {
	    b.append(space80.substring(0,80-b.length()));
	}
	String card = new String(b);
	if (card.length() > 80) {
	    card = card.substring(0,80);
	}
	return card;
    }
}
