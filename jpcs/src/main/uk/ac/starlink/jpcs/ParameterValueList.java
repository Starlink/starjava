package uk.ac.starlink.jpcs;

import java.io.*;
import java.util.*;

import uk.ac.starlink.jpcs.Utilities;

/** This class serves to save Parameter values as Strings for use as 'current'
  * or 'global' Parameter values.
 */
public class ParameterValueList extends Properties {
   
static final String HEADING = "Starlink Application ParameterValueList";
   
/** Creates an empty ParameterValueList.
 *  The size will be increased as necessary.
 */
   public ParameterValueList(){
       super();
   }


/** Lists the parameter/value pairs saved in the file specified by the argument.
 *  <p>
 *      <code>% java uk.ac.starlink.jpcs.ParameterValueList filename</code>
 *  </p>
 *  <p>
 *  The <code>.par</code> extension may be omitted from the filename. If
 *  filename is omitted, <code>GLOBAL.PAR</code> is assumed.
 *
 */
   public static void main ( String[] args ) {
      String fileName = "GLOBAL.PAR";
      if ( args.length > 0 ) {
//System.out.println("args[0] is " + args[0] );
         if ( !args[0].equals("GLOBAL") ) {
            if ( args[0].endsWith( ".par" ) || args[0].endsWith( ".PAR" ) ) {
               fileName = args[0];
            } else {
               fileName = args[0] + ".par";
            }
         }
      }
   
      System.out.println( "\nListing ParameterValueList " + fileName );
      try{
         ParameterValueList pvList = ParameterValueList.readList( fileName );
         pvList.remove( "__DUMMY__" );
         pvList.list( System.out );
      
      } catch ( Exception e ) {
           e.printStackTrace();
        
      }
      
   }        
   
/** Sets a name/value pair in this ParameterValueList.
 *  @param name The name of the parameter
 *  @param value The value to be set for the named parameter.
 */
   protected void setValue( String name, String value ) {
      setProperty( name, value );       
      return;
      
   }
   
/** Gets the value stored in this ParameterValueList for the named Parameter.
 *  @param name The parameter name
 *  @return the String stored for the named Parameter, or null if no value is
 *  stored.
 */
   protected String getValue( String name ) {
      
      return getProperty( name );
   }
   
/** Reads a ParameterValueList from a file written by {@link #writeList}()
 *  @param fileName The name of the file (conventionally extension 
 *  <code>.par</code>).
 *  @return the saved ParameterValueList
 *  @throws Exception
 */
   protected static ParameterValueList readList( String fileName )
    throws Exception {
//System.out.println("Reading " + fileName );
      ParameterValueList retval = new ParameterValueList();
      
      File list = Utilities.getConfigFile( fileName );
      try {
         BufferedInputStream bis = new BufferedInputStream(
                                    new FileInputStream( list ) );
         BufferedReader br = new BufferedReader( 
                              new InputStreamReader( bis ) );
         bis.mark( 50 );
         String line1 = br.readLine();
         try{
            if( line1 != null ) {
               if( line1.equals( "#" + HEADING ) ) {
                  bis.reset();
                  retval.load( bis );
                  bis.close();
               } else {
                  throw new ParameterException( "Invalid first line" );
               }
               
            } else {
               throw new ParameterException( "File is empty" );
            }

         } catch ( Exception e ) {
            throw new ParameterException(
             "Invalid Parameter value file " + list.toString() + "\n"
             + e.toString() );
         }
      } catch( FileNotFoundException e ) {
         retval = null;
      }
      
      return retval;
   }
   

/** Writes this ParameterValueList to a file
 *  @param fileName the file to be written (conventionally extension 
 *  <code>.par</code>).
 *  @throws Exception
 */
   protected void writeList( String fileName ) throws Exception {
      File list = Utilities.getConfigFile( fileName );
      FileOutputStream ostream = new FileOutputStream( list );
      remove( "__DUMMY__" );
      store( ostream, HEADING );
      ostream.close();
      return;
   }
   
/** Gets the name/value pairs in this ParameterValueList as an array of Strings
 *  of the form: <code>name=value</code>.
 */
   protected String[] toArray() {
      ArrayList al = new ArrayList();
      remove( "__DUMMY__" );
      Enumeration names = propertyNames();
      while ( names.hasMoreElements() ) {
         String name = (String)names.nextElement();
//System.out.println("ParameterValueList: name " + name );
         al.add( name + " = " + getValue(name) );
      }
      
      return (String[])al.toArray( new String[0] );
   }
   
/** Sets the name/value pairs in this ParameterValueList from an array of
 *   Strings of the form: <code>name=value</code>.
 */
   protected static ParameterValueList fromArray( String[] pairs ) {
      ParameterValueList pvl = new ParameterValueList();
      
      for( int i=0; i<pairs.length; i++ ) {
         String[] pair = pairs[i].split( " = ", 2 );
         pvl.setValue( pair[0], pair[1] );
      }
      
      return pvl;
   }     

/** Converts this ParameterValueList into XML format.
 */
   public String[] toXML() {
      ArrayList al = new ArrayList();
      al.add("<ParameterValueList>");
      remove( "__DUMMY__" );
      Enumeration names = propertyNames();
      while ( names.hasMoreElements() ) {
         String name = (String)names.nextElement();
//System.out.println("ParameteValueList: name " + name );
         al.add( "<parameter name=\"" + name + "\">" );
         al.add( "<![CDATA[" + getValue(name) + "]]>" );
         al.add( "</parameter>" );
      }
      
      al.add( "</ParameterValueList>" );
      
      return (String[])al.toArray( new String[0] );
   }
  
}   
      
