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


/** A class providing the methods required to interpret the Array of strings
 *  returned by a Java ADAM task. 
*/
class TaskReplyHandler extends DefaultHandler {

private TaskReply taskReply;
private Msg out = new Msg( new String[0] );
private ParameterValueList pvList;
private StarlinkStatus status;
private boolean print = Boolean.getBoolean( "star.printTaskReply" );
private boolean inMessage = false;
private boolean inParameter = false;
private StringBuffer message = new StringBuffer(80);
private StringBuffer pValue = new StringBuffer(80);
private String pName;
private String lastParam;
private Msg errors = new Msg( new String[0] );
private boolean good = true;

/** List the components of a {@link TaskReply} saved in a file.
 *  Invocation: java uk.ac.starlink.jpcs.TaskReplyHandler filename
*/
public static void main( String[] args ) throws Exception {
TaskReplyHandler handler = new TaskReplyHandler();
TaskReply taskReply;
ParameterValueList pvl;
String[] doc = {
"<TaskReply>",
"<message><![CDATA[Message 1]]></message>",
"<message><![CDATA[Message <2>]]></message>",
"<message><![CDATA[Message 3&4]]></message>",
"</TaskReply>"};

if( args.length == 0 ) {
/* Use a simple, preset test reply */
   taskReply = handler.readReply(
     new InputSource(
       new StringArrayReader( doc ) ) );

} else {
/* Read the TaskReply from a file */
   taskReply = handler.readReply( new FileInputStream( args[0] ) );
}

/* Now display the components of the TaskReply */
   System.out.println( "\nTaskReply components read from file:" );

/* The messages */
/* List them without removing them from the Msg */
/* There will always be a Msg for the TaskReply but it may be empty */
/* - that's OK. */
   taskReply.getMsg().list();

/* The ParameterValueList - if there is one */
   pvl = taskReply.getPVList();
   if( pvl != null ) {
      pvl.list( System.out );
   } else {
      System.out.println( "No ParameterValueList in TaskReply." );
   }

/* Now convert the TaskReply to an array of String and repeat the process */
   doc = taskReply.toXML();
   taskReply = handler.readReply(
     new InputSource(
       new StringArrayReader( doc ) ) );

/* Now display the components of the TaskReply */
   System.out.println( "\nTaskReply components read from generated XML:" );
//for( int i=0; i<doc.length; i++ ) {
//System.out.println( doc[i] );
//}
/* The messages */
/* List them without removing them from the Msg */
/* There will always be a Msg for the TaskReply but it may be empty */
/* - that's OK. */
   taskReply.getMsg().list();


/* The ParameterValueList - if there is one */
   pvl = taskReply.getPVList();
   if( pvl != null ) {
      pvl.list( System.out );
   } else {
      System.out.println( "No ParameterValueList in TaskReply." );
   }
   

}

/**
 *  Reads a TaskReply document
 *  @param the InputStream to read
 *  @return A {@link ParameterList}.
*/
public TaskReply readReply( InputStream file ) throws Exception {       

      return readReply( new InputSource( file ) );
}

/**
 *  Reads a TaskReply document.
 *  @param the InputSource to read
 *  @return A {@link ParameterList}.
*/
public TaskReply readReply( InputSource file ) throws Exception {       
        
// Use the default (non-validating) parser      
//      SaxParser saxParser = ParserFactory.makeParser(
//                      "org.apache.xerces.parsers.SAXParser" );
      SAXParser saxParser = new SAXParser();
      saxParser.setContentHandler( this );                
      saxParser.setErrorHandler( this );                
      boolean checking = Boolean.getBoolean( "star.checkTaskReply" );
      
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

// System.out.println("Parse the TaskReply");
      saxParser.parse( file );
//System.out.println("Parsed TaskReply File");         
         
      return taskReply;
}

public void startElement(String namespaceURI,   
                    String sName, // simple name        
                    String qName, // qualified name     
                    Attributes attrs)   
        throws SAXException {       

// Check for errors already
           if( taskReply == null ) {
              if( errors.size() > 1 ) {
                 errors.head( "\n!! ERRORS on or before TaskReply tag" );
                 errors.flush();
              }
           }
           
           String eName = sName; // element name        
           if ("".equals(eName)) eName = qName; // not namespaceAware   

//System.out.println( "Element start " + eName );
           if ( eName.equals("message") ) {
// message element
               inMessage = true;
              
           } else if ( eName.equals("ParameterValueList") ) {
// parametervaluelist element
// System.out.println("parameterValueList");
              pvList = new ParameterValueList();
                 
           } else if ( eName.equals("parameter") ) {
// parameter element
              inParameter = true;
              pName = attrs.getValue("name");
              lastParam = pName;
              if( lastParam == null ) {
                 errors.out( "!! No name in parameter tag." );
                 good = false;
              }
              
           } else if ( eName.equals("StarlinkStatus") ) {
               this.statusHandler( attrs );

           } else if ( eName.equals("TaskReply") ) {
// TaskReply element
//System.out.println("Create TaskReply");
              taskReply = new TaskReply();
              out = new Msg( new String[0] );
                              
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

//System.out.println( "Element end " + eName );

           if ( eName.equals("message") ) {
// end interface element
              String str = message.toString();
              message.delete( 0, message.length() );
              out.out( str );
              if( print ) System.out.println( str );                 
              inMessage = false;
              lastParam = null;
              if( print ) System.out.println( "\n</message>" );                 
              if( errors.size() > 1 ) {
                 errors.head( "\n!! ERRORS for TaskReply " );
                 errors.flush();
              }
              
           } else if ( eName.equals("parameter") ) {
// end parameter element
              if( pName != null ) {
                 if ( print ) {
                    System.out.println( pName + " = " + pValue );
                 }
                 pvList.setValue( pName, pValue.toString() );
                 pValue.delete(0,pValue.length());
                 pName = null;
                 inParameter = false;
      }
              if( print ) System.out.println( "   </parameter>" );                 
              if( errors.size() > 1 ) {
                 errors.head( "\n!! ERRORS for parameter " + lastParam );
                 errors.flush();
              }
              
           } else if ( eName.equals("ParameterValueList") ) {
// end parameter element
              if( print ) System.out.println( "   </ParameterValueList>" );                 
              if( errors.size() > 1 ) {
                 errors.head( "\n!! ERRORS for ParameterValueList" );
                 errors.flush();
              }
              
           } else if ( eName.equals("status") ) {
// end parameter element
              if( print ) System.out.println( "   </status>" );                 
              if( errors.size() > 1 ) {
                 errors.head( "\n!! ERRORS for status" );
                 errors.flush();
              }

           } else if ( eName.equals("TaskReply") ) {
// end interface element
              taskReply.setMsg( out );
              taskReply.setPVList( pvList );
              taskReply.setStatus( status );
              lastParam = null;
              if( print ) System.out.println( "\n</TaskReply>" );                 
              if( errors.size() > 1 ) {
                 errors.head( "\n!! ERRORS for TaskReply " );
                 errors.flush();
              }

           } else {
// end other (unknown?) element
              if( errors.size() > 1 ) {
                 errors.head( "\n!! ERRORS for element " + eName );
                 errors.flush();
              }
           }
}

/** Action at end of document.
  * Checks if there have been any errors since the last element ended and
  * displays them if there are.
  */
public void endDocument() {
           if( errors.size() > 1 ) {
              errors.head( "\n!! ERRORS after last element" );
              errors.flush();
           }
}

public void characters( char buf[], int offset, int len ) throws SAXException {
   if( inMessage ) {
      message.append( buf, offset, len );
   } else if ( inParameter ) {
      pValue.append( buf, offset, len );
   } else {
// Ignore other characters (we get newlines etc. from between elements).
   }      
     
}        

/** Handle a <parameter> element.
 *  @param attrs The associated element attributes
 *  @return A {@link Parameter} object reflecting the given attributes.
 */
private void parameterHandler( Attributes attrs )
        throws Exception {

   String attrVal;
   Integer position;

   if (attrs != null) {
   
      String pName =  attrs.getValue("name");
      String pValue = attrs.getValue("value");
      if ( pName != null ) {
         
// Save the name of the last parameter
         lastParam = pName;

      }
   }
}

/** Handles the status element
 */
private void statusHandler( Attributes attrs ) {
   status = new StarlinkStatus( Integer.parseInt( attrs.getValue("value") ),
                    attrs.getValue("message") );
}

/** Provides a description of where a parsing problem has occurred.
  * The description will usually refer to the previous parameter name.
  * @return the description.
  */
private String getWhere() {
   String description;
   if( lastParam != null ) {
      description = "after parameter " + lastParam;
   } else if( taskReply == null ) {
      description = "before TaskReply tag";
   } else {
      description = "after /TaskReply";
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
   errors.out( "\n!! FATAL ERROR " + getWhere() );
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
   
