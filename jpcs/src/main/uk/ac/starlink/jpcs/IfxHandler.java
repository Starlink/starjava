package uk.ac.starlink.jpcs;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import java.io.IOException; 


/** A class providing the methods required to create a {@link ParameterList}
 *  for a Java ADAM task by reading the JPCS-style Interface File. 
*/
public class IfxHandler extends DefaultHandler {

private ParameterList pList;
private boolean print = Boolean.getBoolean( "star.printifx" );
private String lastInterface;
private String lastParam;
private Msg errors = new Msg( new String[0] );
private boolean internalMsg = true;
private boolean good = true;

/** The main program provides a means of checking <jpcs> interface files
 *  for validity.
 *  <p>Invocation:<BR>
 *     <code>   % java uk.ac.starlink.jpcs.IfxHandler ifxfile</code>
 *  </p>
 *     reads the file <code>ifxfile</code> (the <code>.ifx</code> may be
 *     omitted) and lists the Parameter names and attribute values.
*/
public static void main( String[] args ) throws Exception  {
   String ifxName;
   IfxHandler handler = new IfxHandler();
// Switch on printing
   handler.printOn( true );
   
// Obtain file name
   if ( args[0].endsWith( ".ifx" ) ) {
      ifxName = args[0];
   } else {
      ifxName = args[0] + ".ifx";
   }

   System.out.println("\nChecking file " + ifxName );

// Read the file
   ParameterList plist = handler.readIfx( new FileInputStream( ifxName ) );
}

/**
 *  Reads an .ifx file specified as an {@link java.io.InputStream}.
 *  @param file the InputStream to read
 *  @return A {@link ParameterList}.
*/
protected ParameterList readIfx( InputStream file ) throws Exception {       

   return readIfx( new InputSource( file ) );
}

/**
 *  Reads an .ifx file specified as an array of Strings.
 *  @param array the array of Strings to read
 *  @return A {@link ParameterList}.
*/
public ParameterList readIfx( String[] array ) throws Exception {

   return readIfx( new InputSource( new StringArrayReader( array ) ) );
}

/**
 *  Reads an .ifx file.
 *  @param the InputSource to read
 *  @return A {@link ParameterList}.
*/
public ParameterList readIfx( InputSource file ) throws Exception {       
        
// Use the default (non-validating) parser      
//      SaxParser saxParser = ParserFactory.makeParser(
//                      "org.apache.xerces.parsers.SAXParser" );
      SAXParser saxParser = new SAXParser();
      saxParser.setContentHandler( this );                
      saxParser.setErrorHandler( this );                
      boolean checking = Boolean.getBoolean( "star.checkifx" );
      
      try { saxParser.setFeature("http://xml.org/sax/features/validation",
                         checking ); 
      } catch (SAXException e) { 
         System.out.println("error in setting up parser feature "
                            + e.getMessage());
         e.printStackTrace();
      }
      try { saxParser.setFeature("http://apache.org/xml/features/validation/schema",
                         checking ); 
      } catch (SAXException e) { 
         System.out.println("error in setting up parser feature "
                             + e.getMessage() );
      }

// System.out.println("Parse the file");
      saxParser.parse( file );
//System.out.println("Parsed Interface File");         
         
      return pList;
}

public void startElement(String namespaceURI,   
                    String sName, // simple name        
                    String qName, // qualified name     
                    Attributes attrs)   
        throws SAXException {       

// Check for errors already
           if ( lastInterface == null ) {
              if( errors.size() != 0 ) {
                 errors.head( "\n!! IFX ERRORS on or before first interface tag" );
                 errors.flush();
              }
           }

           String eName = sName; // element name        
           if ("".equals(eName)) eName = qName; // not namespaceAware   

           if ( eName.equals("interface") ) {
// interface element
              try {
                 pList = this.interfaceHandler( attrs );
              } catch ( ParameterException e ) {
                 errors.out( "!! Failed to handle interface tag: "
                              + e.getMessage() );
                 good = false;
              }
                 
           } else if ( eName.equals("parameter") ) {
// parameter element
              try {
                 pList.add( this.parameterHandler( attrs ) );
              } catch ( Exception e ) {
                 errors.out( "!! Failed to handle parameter tag: "
                              + e.getMessage() );
                 good = false;
              }
              
           } else if ( eName.equals("helplib") ) {
// helplib element
               if( print ) {
                 System.out.println( "\n   helplib " + attrs.getValue("name") );
               }
               
           } else {
// unknown element
              errors.out("!! Unknown element " + eName );
 
           }
}        

public void endElement(String namespaceURI,   
                    String sName, // simple name        
                    String qName )// qualified name   
        throws SAXException {       
           String eName = sName; // element name        
           if ("".equals(eName)) eName = qName; // not namespaceAware   


           if ( eName.equals("interface") ) {
// end interface element
              lastParam = null;
              if( print ) System.out.println( "\nendinterface" );                 
              if( errors.size() != 0 ) {
                 errors.head( "\n!! IFX ERRORS for interface " + lastInterface );
                 errors.flush();
              }
              
           } else if ( eName.equals("parameter") ) {
// end parameter element
              if( print ) System.out.println( "   endparameter" );                 
              if( errors.size() != 0 ) {
                 errors.head( "\n!! IFX ERRORS for parameter " + lastParam );
                 errors.flush();
              }

           } else {
// end other (unknown?) element
              if( errors.size() != 0 ) {
                 errors.head( "\n!! IFX ERRORS for element " + eName );
                 errors.flush();
              }
           }
}

/** Action at end of document.
  * Checks if there have been any errors since the last element ended and
  * displays them if there are.
  */
public void endDocument() {
           if( errors.size() != 0 ) {
              errors.head( "\n!! IFX ERRORS after last element" );
              errors.flush();
           }
}        

/** Handle an <interface> element
 *  @param attrs The associated element attributes.
 *  @return An empty {@link ParameterList}.
*/
private ParameterList interfaceHandler( Attributes attrs )
        throws ParameterException {

// Get the interface name
   lastInterface = attrs.getValue("name");
   if ( lastInterface == null ) lastInterface = "";

   ParameterList pList = new ParameterList();
   pList.setTaskName( lastInterface );

   if( print ) {
      System.out.println("\nInterface " + lastInterface );
   }

   return pList;
}

/** Handle a <parameter> element.
 *  @param attrs The associated element attributes
 *  @return A {@link Parameter} object reflecting the given attributes.
 */
private Parameter parameterHandler( Attributes attrs )
        throws Exception {

   String attrVal;
   Integer position;

   if (attrs != null) {
   
      String pName =  attrs.getValue("name");
      if ( pName != null ) {
         
// Save the name of the last parameter
         lastParam = pName;

         if ( print ) {
            System.out.println( "\n   Parameter " + pName );
            System.out.println( "      type " + attrs.getValue("type") );
            System.out.println( "      position " + attrs.getValue("position") );
            System.out.println( "      prompt " + attrs.getValue("prompt") );
            System.out.println( "      vpath " + attrs.getValue("vpath") );
            System.out.println( "      ppath " + attrs.getValue("ppath") );
            System.out.println( "      keyword " + attrs.getValue("keyword") );
         }

// Get the class for this parameter type 
         String type = attrs.getValue("type");
         if ( type != null ) {
//System.out.println("Get Class: " + "uk.ac.starlink.jpcs." + type );
            Class pClass = Class.forName(
             "uk.ac.starlink.jpcs." + type );
            Class[] sig = new Class[6];
            sig[0] = int.class;
            Arrays.fill( sig, 1, 6, java.lang.String.class );
            Constructor constructor = pClass.getConstructor( sig );
//System.out.println("Got constructor " );
//Create the basic parameter
            try {
//System.out.println("Constructing Parameter " + pName);
               attrVal = attrs.getValue("position");
               if ( attrVal == null ) {
//System.out.println("No position attribute - set zero");
                  position = new Integer( 0 );
               } else {
                  try{ 
                     position = new Integer( attrVal );
                  } catch ( Exception e ) {
//System.out.println("Conversion failed");
                     position = new Integer( 0 );
                     errors.out( "!  Failed to convert position string '"
                                 + attrVal +"' to int" );
                  }
               }
                      
               
               Object[] args = { position,
                                 pName,
                                 attrs.getValue("prompt"),
                                 attrs.getValue("vpath"),
                                 attrs.getValue("ppath"),
                                 attrs.getValue("keyword") };
                                                   
               Parameter p = (Parameter)constructor.newInstance( args );
//System.out.println("Constructed Parameter " + pName );
// Now fill in other attributes
            
// access
               attrVal = attrs.getValue("access");
               if ( attrVal != null ) {
                  if( print ) {
                     System.out.println( "      access " + attrVal );
                     }
                  try {
                     p.setAccess( attrVal );
                  } catch ( ParameterException e ) {
                     errors.out( "!  Error setting access for Parameter "
                                 + pName + " - " + e.getMessage() );
                  }
               }

// default
               attrVal = attrs.getValue("default");
               if ( attrVal != null ) {
                  if( print ) {
                     System.out.println( "      default " + attrVal );
                  }
                  try {
                     p.setDefault( p.fromString(attrVal) );
                  } catch ( ParameterException e ) {
                     errors.out( "!  Invalid default" 
                     + " - " + e.getMessage() );
                  }
               }
            
// association
               attrVal = attrs.getValue("association");
               if ( attrVal != null ) {
                  if( print ) {
                     System.out.println( "      association " + attrVal );
                     }
                  try {
                     p.setAssociation( attrVal );
                  } catch ( ParameterException e ) {
                     errors.out( "!  Error setting association for Parameter "
                                 + pName + " - " + e.getMessage() );
                  }
               }
               return p;

            } catch( Exception e ) {
               throw new ParameterException( "Problem constructing Parameter "
                                             + pName + " - "
                                             + e.getClass().getName() );
            }
            
               
         } else {
            throw new ParameterException( "Parameter " + pName +
                                      " - type not specified");
         }
         
      } else {
         throw new ParameterException( "No name for Parameter" );
      }
       
   } else {
      throw new ParameterException( "No attributes for Parameter" );
      
   }
   
}

/** Provides a description of where a parsing problem has occurred.
  * The description will usually refer to the previous parameter name.
  * @return the description.
  */
private String getWhere() {
   String description;
   if( lastParam != null ) {
      description = "after parameter " + lastInterface + "." + lastParam;
   } else if( lastInterface == null ) {
      description = "before first interface tag";
   } else {
      description = "after interface " + lastInterface;
   }

   return description;
}
   
/** Parser error handler. Adds location information to standard error report.
  */
public void error( SAXParseException e ) throws SAXException {
   errors.out( "!  " + e.getMessage() );
   good = false;
}

/** Parser fatalError handler. Adds location information to standard fatal
  * error report and re-throws the Exception
  * @param the exception thrown by the parser.
  */
public void fatalError( SAXParseException e ) throws SAXException {
   errors.out( "\n!! IFX FATAL ERROR " + getWhere() );
   errors.out( "!  " + e.getMessage() );
   errors.flush();
   throw( e );
}

/** Parser warning handler. Adds location information to standard warning.
  */
public void warning( SAXParseException e ) throws SAXException {
   errors.out( "!  " + e.getMessage() );
}

/** Switches print flag. The print flag causes the interface file to be
  * displayed or not.
  * @param true if printing is required.
  */
public void printOn( boolean printState ) {
   print = printState;
}

}
   
