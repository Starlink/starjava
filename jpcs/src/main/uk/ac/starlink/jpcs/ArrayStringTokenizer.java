package uk.ac.starlink.jpcs;

import java.util.StringTokenizer;
import java.util.NoSuchElementException;

/** A class for tokenizing strings representing {@link ArrayParameterValue}s.
 */
class ArrayStringTokenizer {

private StringTokenizer st;

/** Construct an ArrayStringTokenizer for the given String
 *  @param the string to be tokenized.
 */
ArrayStringTokenizer( String str ) {
   st = new StringTokenizer( str,  "{}[], ", true );
}

/** Tells if this ArrayStringTokenizer has more tokens
 *  @return <code>true</code> if there are more tokens; <code>false</code>
 *  otherwise.
 */
protected boolean hasMoreTokens() {
   return st.hasMoreTokens();
}

/** Gets the next token. Tokens are separated by {}[], or space. The separators
 *  other than space are themselves returned as tokens. Separators within "" are
 *  not treated as separators and the quoted string is returned as a single,
 *  unquoted token.
 *  @return the next token or separator. 
 */
protected String nextToken() {
   String tok;
   
/* Ignore spaces between tokens
*/
   if ( st.hasMoreTokens() ) {
//System.out.println( "ArrayStringTokenizer.nextToken: getting token" );
      tok = st.nextToken("{}[], ");
//System.out.println( "ArrayStringTokenizer.nextToken: Token: '" + tok + "'");
      try {
         while ( tok.equals( " " ) ) {
            tok = st.nextToken("{}[], ");
         }
         
      } catch ( NoSuchElementException e ) {
         tok = null;
      }
      
/* If the token begins with " or ' it is a quoted string - get the remainder. 
*/
      if ( tok != null ) {
         if ( tok.startsWith( "\"" ) || tok.startsWith( "\'" ) ) {
            String quote = tok.substring( 0, 1 );
//System.out.println("ArrayStringTokenizer.nextToken: Quoted string - "  + quote);
            StringBuffer strb = new StringBuffer( tok.substring(1) );
      
/* Handle case of quoted string starting with one of the delimiters so that the
*  first token returned is just ".
*/
            if ( tok.length() == 1 ) {
               tok = " ";
            }

            while ( !tok.endsWith( quote ) ) {
/* Get remainder of quoted string - loop terminated when the delimiter " is
*  returned.
*/
//System.out.println("ArrayStringTokenizer.nextToken: Get remaider of quoted string");
               if ( st.hasMoreTokens() ) {
                  tok = st.nextToken( quote );
//System.out.println( "ArrayStringTokenizer.nextToken: Token: " + tok );
               } else {
//System.out.println(
//   "ArrayStringTokenizer.nextToken: Unclosed quoted string - assumed closed" );
                  tok = quote;
               }
               strb.append( tok );
            }
//System.out.println( "ArrayStringTokenizer.nextToken: Remove terminating quote" );
            strb.deleteCharAt( strb.length() - 1 );
//System.out.println( "ArrayStringTokenizer.nextToken: Convert StringBuffer to String" );
            tok = strb.toString();
//System.out.println( "ArrayStringTokenizer.nextToken: UnQuoted Token: " + tok );

         } else if ( tok.matches( "\\w*\\(\\d*:\\d*" ) ) {
/* It looks like a filename with slice information
*/
            StringBuffer strb = new StringBuffer( tok );
            while ( !tok.endsWith( ")" ) ) {
/* Get remainder of filename string - loop terminated when the delimiter ) is
*  returned.
*/
//System.out.println("ArrayStringTokenizer.nextToken: Get remaider of filename string");
               if ( st.hasMoreTokens() ) {
                  tok = st.nextToken( ")" );
//System.out.println( "ArrayStringTokenizer.nextToken: Token: " + tok );
                  strb.append( tok );
               } else {
//System.out.ptintln( "ArrayStringTokenizer.nextToken: Unclosed brackets string - assumed closed" );
                  strb.append( ")" );
               }
            }
//System.out.println( "ArrayStringTokenizer.nextToken: Convert StringBuffer to String" );
            tok = strb.toString();
//System.out.println( "ArrayStringTokenizer.nextToken: Full filename " + tok );
         }
      }
   
   } else {
      tok = null;
   
   }

   return tok;

}

}
