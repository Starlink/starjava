package uk.ac.starlink.jpcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.io.*;

/** Implements a tokenizer for Starlink-style application command lines.
 *  Quoted strings and arrays enclosed are treated as single tokens, and
 *  / is treated as a word character.
 */
public class CommandLineTokenizer extends StreamTokenizer {

static final String startquote = "\"";
static final String endquote = "\"";

public CommandLineTokenizer( Reader reader ) {
   super( reader );
   super.resetSyntax();
   super.wordChars( '\u0000', '\u00FF');
   super.wordChars( '/', '/' );
   super.whitespaceChars( ' ', ' ' );
   super.quoteChar( '"');
   super.quoteChar( '\'');
   super.ordinaryChar( '[' );
   super.ordinaryChar( ']' );
}

public static void main( String args[] ) {

   CommandLineTokenizer clt = new CommandLineTokenizer(
     new StringReader( args[0] ) );
     
   try{
      while( clt.nextToken() != clt.TT_EOF ) {
         System.out.println( "Token: " + clt.sval );
      }
   } catch( Throwable e ) {
      e.printStackTrace();
      System.exit(1);
   }
}

public int nextToken() throws IOException {
   int level=0;
   StringBuffer array = new StringBuffer();    

   int ttype = super.nextToken();      
   boolean finished = false;
   while( (ttype != TT_EOF) && !finished ) {
      switch ( ttype ) {
         case StreamTokenizer.TT_WORD:
//System.out.println( "CommandLineTokenizer - Word: " + sval );
            if( level > 0 ) array.append( sval + "," );
            break;
         case StreamTokenizer.TT_EOL:
//System.out.println("CommandLineTokenizer - EOL");
            break;
         case '"':
         case '\'':
            sval =  startquote + this.sval + endquote;
//System.out.println( "CommandLineTokenizer - Quoted String: " + sval );
            if( level > 0 ) array.append( sval + "," );
            break;
         case '[':
            level++;
            array.append( "[" );
            break;
         case ']':
            level--;
            array.setCharAt( array.length()-1, ']' );
            if( level == 0 ) {
               sval = array.toString();
               array.delete(0,array.length());
               ttype = TT_WORD;
//System.out.println( "CommandLineTokenizer - Array: " + sval );
            }
            break;    
         default:
//System.out.println( "CommandLineTokenizer - Default: " + ttype );
      }
   
      if( level == 0 ) {
         finished = true;
      } else {
         ttype = super.nextToken();
      }
   
   }
      
//System.out.println( "CommandLineTokenizer - Return: " + ttype );
   return ttype;      
   
}

} 
