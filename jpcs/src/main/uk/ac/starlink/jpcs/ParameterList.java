package uk.ac.starlink.jpcs;

import java.io.*;
import java.util.*;

/** A class implementing a list of {@link Parameter}s usually associated with a
  * particular Java ADAM task. Currently ParameterList extends ArrayList but
  * don't rely on this.
  * We put a dummy in the first position to get numbering to old Fortran style
  * required to implement SUBPAR_INDEX. 
*/
public class ParameterList extends ArrayList {

   static final boolean TO = false;

   String taskName = null;
   ParameterValueList currentList = null;

/** Constructs a ParameterList with an initial capacity of 10 and containing
 *  the dummy parameter required by the system.
 *  @throws ParameterException if it fails to create the dummy Parameter.
 */
   public ParameterList() throws ParameterException  {
      super( 10 );
      this.add( new Parameter( "__DUMMY__" ) );
   }
   
/** Sets the taskName associated with this ParameterList
 *  @param name The task name
 */
   public void setTaskName( String name ) {
      taskName = name;
   }
   
/** Gets the name of the task associated with this ParameterList.
 *  @return The task name
 */
   public String getTaskName() {
      return taskName;
   }

/** Sets the list of 'current' parameter values
 *  associated with this ParameterList. The list is a
 *  {@link ParameterValueList} which is read from the task's parameter file if
 *  there is one; otherwise a new, empty ParameterValueList is constructed.
 *  The ParameterValueList obtained is then set as the currentList for all
 *  Parameters in this ParameterList.
 *  @throws Exception if it fails to obtain a ParameterValueList for this
 *  ParameterList.
 */
   public void setCurrentList( ) throws Exception {

      ParameterValueList pvl = getCurrentList();
      if ( pvl != null ) {
         setCurrentList( pvl );
      }
   }
   
/** Sets the list of 'current' parameter values
 *  associated with this {@link ParameterList} and all the 
 *  {@link Parameter Parameters}
 *  within it to the given {@link ParameterValueList}.
 */
   public void setCurrentList( ParameterValueList pvl ) {
      
      currentList = pvl;

/* Now set the current list for each Parameter */
      Iterator it = this.iterator();
      while ( it.hasNext() ) {
         Parameter p = (Parameter)it.next();
         p.setCurrentList( currentList );
      }
         
      return;
   }
   
/** Sets each {@link Parameter} in this ParameterList to its 'current' value.
 *  Actually calls the {@link Parameter#setToCurrentValue()} method for each
 *  Parameter.
 *  @throws ParameterException if the 'current value' is not valid for the
 *  Parameter.
 *  @see Parameter#setToCurrentValue Parameter.setToCurrentValue()
 */
   public void setToCurrentValue() throws ParameterException {
      Iterator it = this.iterator();
      while ( it.hasNext() ) {
         Parameter p = (Parameter)it.next();
         p.setToCurrentValue();
      }
   }

/** Gets the {@link ParameterValueList} associated with this ParameterList.
 *  If one is not currently associated, an attempt is made to create a new
 *  one from the task's parameter file.
 *  @throws Exception if it fails in creating a new ParameterValueList.
 */
   public ParameterValueList getCurrentList() throws Exception {
   ParameterValueList pvl;
      if ( currentList == null ) {
//System.out.println("Reading " + getTaskName() + ".par" );
         pvl = ParameterValueList.readList( getTaskName() + ".par" );
      } else {
         pvl = currentList;
      }     

      return pvl;
   
   }
   
/** Gets the Parameter with the given command-line position. Command-line
 *  positions are normally allocated in the Interface File.
 *  @param pos The command-line position.
 *  @return The Parameter allocated the given position.
 */
   protected Parameter findPosition( int pos ) throws ParameterException {
      for ( int i=1; i<this.size(); i++ ) {
         Parameter p = (Parameter)this.get( i );
         if ( p.getPosition() == pos ) return p;
      }
      throw new ParameterException(
        "Parameter position " + pos + " is not allocated!" );
   }

/** Gets the Parameter with the given keyword.
 *  @param kywrd the keyword to be found.
 *  @return the Parameter with the given keyword.
 */
   protected Parameter findKeyword( String kywrd ) throws ParameterException {
      for ( int i=1; i<this.size(); i++ ) {
         Parameter p = (Parameter)this.get( i );
         if ( p.getKeyword().equalsIgnoreCase( kywrd ) ) {
//System.out.println("Found it");
           return p;
         }
      }
      throw new ParameterException(
         "Parameter with keyword " + kywrd + " not found!" );
   }
   
/** Gets the id for the Parameter with the given name. The id currently is the
 *  index within the ArrayList but this should not be relied upon. Use it only
 *  as an argument of the get() method.
 *  @param The name of the required Parameter
 */
   public Parameter findName( String name ) throws ParameterException {
//System.out.println("findName: List size is " + this.size() );
      for ( int i=1; i<this.size(); i++ ) {
         Parameter p = (Parameter)this.get( i );
//System.out.println("findName: Test against parameter " + p.getName() );
         if ( p.getName().equalsIgnoreCase( name ) ) {
            return p;
         }
      }
      throw new ParameterException(
         "Parameter with name " + name + " not found!" );
   }
   
/** Gets the id for the Parameter with the given name. The id currently is the
 *  index within the ArrayList but this should not be relied upon. Use it only
 *  as an argument of the get() method.
 *  @param The name of the required Parameter
 */
   protected int findID( String name ) throws ParameterException {
//System.out.println("findID: List size is " + this.size() );
      for ( int i=1; i<this.size(); i++ ) {
         Parameter p = (Parameter)this.get( i );
//System.out.println("findID: Test against parameter " + p.getName() );
         if ( p.getName().equalsIgnoreCase( name ) ) return i;
      }
      throw new ParameterException(
         "Parameter with name " + name + " not found!" );
   }

/** Creates a ParameterList populated with the Parameters described in
 *  the named Java ADAM Interface File (.ifx).
 *  @param file the name of the Interface File.
 *  @return a ParameterList corresponding to the Interface File.
 */  
   public static ParameterList readIfx( InputStream file )
        throws Exception {
      ParameterList pl;
      IfxHandler handler = new IfxHandler();
      
      pl = handler.readIfx( file );
      
      return pl;
   }    
  
/** Sets up this ParameterList according to the Parameter values and special
  * keywords specified by the given array of Strings, which will normally be
  * a command line.
  * <P>
  * Parameter values may be positional or of the form
  * <code>keyword=value</code>.
  * For {@link BooleanParameter}s the keyword alone or preceded by 'no' may be
  * given to indicate <code>true</code> or <code>false</code>. Case is not
  * significant for keywords.
  * </p>
  * <p>
  * The special keywords are:
  * <dl>
  * <dt>PROMPT</dt>
  * <dd>Forces a prompt for any Parameter not already given a value when it
  * is required.</dd>
  * <dt>ACCEPT</dt>
  * <dd>Forces acceptance of the suggested value, if there is one, when a prompt
  * would otherwise occur. If there is no suggested value, a prompt will occur
  * anyway.</dd>
  * <dt>RESET</dt>
  * <dd>Causes the 'current' value to be ignored when searching the
  * vpath or ppath for a Parameter value.</dd>
  * <dt>NOPROMPT</dt>
  * <dd>Causes Parameter values to be set to {@link NullParameterValue}
  * if a prompt would otherwise occur.</dd>
  * </dl>
  * @param args the command line.
 */
   protected void parseCommandLine( String[] args ) throws Exception {
      Parameter p=null;
      String arg;
      String key;
      String valstr;
      int pos = 1;
      int j;
      boolean keyEqualsForm=false;
      boolean special = false;
      boolean error = false;
      String errorMessage="";
      
//System.out.println("args length is: " + args.length);      

      for ( int i=0; i<args.length; i++ ) {
         arg = args[i];
//System.out.println("Doing arg " + i + ": " + arg);
         if ( arg.endsWith("=") ||
              ( i < args.length - 1 ) && args[i+1].startsWith("=") ) {
            StringBuffer argBuffer = new StringBuffer( arg );
            argBuffer.append( args[++i] );
            if ( arg.equals("=") ) {
/*           The next arg was a singleton "=" */
               argBuffer.append( args[++i] );
            }
            arg = argBuffer.toString();
         }

         if ( ( j = arg.indexOf('=') ) < 0 ) {
/* Normal word
 * Is it a Keyword of a boolean Parameter
*/
//System.out.println("Normal word");
            if ( Character.isLetter( arg.charAt(0) ) ) {
               key = arg;
               valstr = "TRUE";
               if ( arg.length() > 2 && 
                    arg.substring(0,2).equalsIgnoreCase("NO") ) {
                  key = arg.substring(2);
                  valstr = "FALSE";
               }
               try {
                  p = this.findKeyword( key );
                  if ( !(p instanceof BooleanParameter) ) {
                     valstr = arg;
                  }
               } catch ( ParameterException e ) {
/* Wasn't a logical keyword - was it a special keyword
*/
                  if ( key.equalsIgnoreCase("ACCEPT") ) {
                     special = true;
                     this.setAccept();
                     
                  } else if ( key.equalsIgnoreCase("RESET") ) {
                     special = true;
                     this.setReset();
                     
                  } else if ( key.equalsIgnoreCase("PROMPT") ) {
                     special = true;
                     if ( valstr.equals("TRUE") ) {
                        this.setPrompt();
                     } else {
                        this.setNoPrompt();
                     }
                  } else {
/* Wasn't a singleton keyword - must be a positional parameter
*/
                     special = false;
                     valstr = arg;
                  }
               }
               
            } else {
               key = "";
               valstr = arg;
            }
             
            
         } else {
/* Keyword= form */
//System.out.println("keyword form");
//System.out.println("j is " + j);
            keyEqualsForm = true;
            key = arg.substring( 0, j );
            valstr = arg.substring( j+1 );
         }
//System.out.println("key: " + key );
//System.out.println("valstr: " + valstr );       

         p = null;
/* If it wasn't a special keyword, set the appropriate parameter value */
         if ( !special ) {
/* Get the corresponding Parameter */
            if ( key.length() != 0 ) {
               try {
                  p = this.findKeyword( key );
            
               } catch ( ParameterException e ) {
                  if( keyEqualsForm ) {
//System.out.println( "Was key= form" );
                     error = true;
                     errorMessage = errorMessage + "\n!  " + e.getMessage();
                  } else {
//System.out.println( "Wasn't a keyword - must be positional" );               
//System.out.println( e.getMessage() );
                     valstr = key;
                     try{
                        p = this.findPosition( pos );
                        pos++;
                     } catch( ParameterException e2 ) {
//System.out.println( "Couldn't find position " + (pos) );
                        error = true;
                        errorMessage = errorMessage + "\n!  " + e.getMessage();
                     }
                  }
               }

            } else { 
               try{
                  p = this.findPosition( pos++ );
               } catch( ParameterException e ) {
                  error = true;
                  errorMessage = errorMessage + "\n!  " + e.getMessage();
               }

            }

/* Set the Parameter value */
            if( p != null ) {
               if( !p.isActive() ) {
                  try{
                     p.set( p.fromString( valstr ) );
//System.out.println( "Object set is " + p.getObject().getClass().getName() );
                  } catch( ParameterException e ) {
                     p.cancel();
                     String mess = e.getMessage();
                     p.setMessage(
                      "Error on command line - " + mess );
                     error = true;
                     errorMessage = errorMessage + "\n!  " + mess;
                  }
               
               } else {
/* Parameter value has already been set - must be an error */
                  p.cancel();
                  String mess =  
                   "Attempt to set parameter " + p.getKeyword() + " twice";
                  p.setMessage( "Error on command line - " + mess );
                  error = true;
                  errorMessage = errorMessage + "\n!  " + mess;
               }
            }
         }
      }
      
      if( error ) {
         throw new ParameterException(
          "!! Error on command line -\n" + errorMessage.substring(1) );
      }
          
   }
      
/** De-activates all the Parameters in this ParameterList.
 *  The Parameter state is set to GROUND but the Parameter value is not
 *  overwritten - thus operating as a 'current' value within this session
 *  @param ok <code>true</code> if the Global and current values are to be
 *  updated.
 *  Global parameters are written to the file named in the association field
 *  of the task's Interface File.
 *  Current values are written to a file with the name specified in the taskName
 *  field of this ParameterList.
 */
   public void deActivate( boolean ok ) throws Exception {
      String fileName="";
      String pValue;
      ParameterValueList globalList = null;
      boolean globalUpdate = false;
      boolean currentUpdate = false;

//System.out.println(
//  "ParameterList.deActivate + getTaskName() + " (" + ok + " )" );
      Iterator it = this.iterator();
      while ( it.hasNext() ) {
         Parameter p = (Parameter)it.next();
         if ( ok ) {
/* Get the Parameter value as a String
*/
            pValue = p.toString();
            
/* Check if any Global parameters to update
*/
            String[] globalName = p.getGlobalName( TO );
 
            if ( globalName != null ) {
/* There is an associated Global parameter with write access
*  If the associated filename is not the currently open filename, re-write
*  the opened one and open the new one
*/
//System.out.println(
// "ParameterList.deActivate: globalName " + globalName[0] +"." + globalName[1] );

               if ( !globalName[0].equals(fileName) ) {
                  if( globalList != null ) {
//System.out.println("ParameterList.deActivate: writing file " + fileName );
                     globalList.writeList( fileName + ".PAR" );
                  }
                  fileName = globalName[0];
//System.out.println("ParameterList.deActivate: reading file " + fileName );
                  globalList =
                   ParameterValueList.readList( fileName + ".PAR" );
                  if ( globalList == null ) {
//System.out.println("Creating new globalList");
                     globalList = new ParameterValueList();
                  }
//System.out.println("Global");
//globalList.list();
               }
                           
/* The required ParameterValueList is available - save the name/value pair
*/
//System.out.println(
// "ParameterList.deActivate: setting global " + globalName[1]  + " to " + pValue ); 
               globalList.setValue( globalName[1], pValue );
               globalUpdate = true;            
            }
            
/* Update the current values - currentList will have been set if there is
 * a task parameter file - if not create a new ParameterValueList the first time
 * round.
*/
            if ( currentList == null ) {
                  currentList = new ParameterValueList();
            }
            currentList.setValue( p.getName(), pValue );
            currentUpdate = true;            
         }
         
         p.deActivate();
      }
      
/* Write the final Global file and task Parameter file
*/
//System.out.println("ParameterList.deActivate: writing file " + fileName );
      if ( globalUpdate ) {
         globalList.writeList( fileName + ".PAR" );
      }
      if ( currentUpdate ) {
//System.out.println("ParameterList.deActivate: writing file " + getTaskName() );
         currentList.writeList( getTaskName() + ".par" );
      }
   }

/** Set all Parameters in this ParameterList to ACCEPT mode. If a prompt
 *  would be issued, the suggested value will be used instead.
 */
   private void setAccept() {
      Iterator it = this.iterator();
      while ( it.hasNext() ) {
         Parameter p = (Parameter)it.next();
         p.setAcceptFlag(true);
      }
   }
   
/** Set all Parameters in this ParameterList to RESET mode. Defaults and
 *  suggested values are found as if there is no 'current' value.
 */
   private void setReset() {
      Iterator it = this.iterator();
      while ( it.hasNext() ) {
         Parameter p = (Parameter)it.next();
         p.setResetFlag(true);
      }
   }
   
/** Set all Parameters in this ParameterList to PROMPT mode. A prompt will be
 *  issued for all parameters not defined on the command line.
 */
   private void setPrompt() {
      Iterator it = this.iterator();
      while ( it.hasNext() ) {
         Parameter p = (Parameter)it.next();
         p.setPromptFlag(true);
      }
   }          
   
/** Set all Parameters in this ParameterList to NOPROMPT mode. If a prompt
 *  would be issued, the Null parameter value will be returned instead.
 */
   private void setNoPrompt() {
      Iterator it = this.iterator();
      while ( it.hasNext() ) {
         Parameter p = (Parameter)it.next();
//System.out.println( p.getName() + ": setting NOPROMPT" );
         p.setNoPromptFlag(true);
      }
   }          

/** Outputs a table of appropriate HTML FORM INPUT elements for each input
 *  Parameter in this ParameterList. The output is written to the specified
 *  PrintWriter and is expected to form part of a larger document.
 *  @param out the output destination.
 *  @param outImage the name of an image device parameter, or null.
 *  @param deviceQualifier orientation/overlay for the image, or null.
 *  @return the number of input parameters.
 */
   public int toForm( Writer out, String outImage, String deviceQualifier )
     throws IOException {

     final String[] deviceQualifiers={"landscape","landscape_overlay",
                                 "portrait","portrait_overlay"};
      int nReadParams = 0; 
      Iterator it = this.iterator();
// discard the first (dummy) Parameter
      Parameter p = (Parameter)it.next();

      out.write( "<table>\n" ); 
      while( it.hasNext() ) {
         p = (Parameter)it.next();
         if( p.isRead() ) {
            out.write( "<tr>\n" );
            out.write( "<td>\n" );
            String parName = p.getKeyword();
            String prompt =  p.getPromptString();
            Object suggested = p.getSuggestedValue();
            String sugStr;
            if ( suggested != null ) {
               sugStr = suggested.toString();
            } else {
               sugStr = "";
            }
            String type = "text";
            if( ( outImage != null ) &&
                ( parName.equalsIgnoreCase( outImage ) ) ) {
                  sugStr = deviceQualifier;
               out.write( parName + ": Display type" );
               out.write( "</td><td>" );
               out.write( "<select name=\"" + parName + "\">\n" );
               for( int i=0;i<deviceQualifiers.length;i++) {
                 String selected =
                  deviceQualifier.equals(deviceQualifiers[i])?" selected>":">";
                 out.write( "<option" + selected +
                  deviceQualifiers[i] + "</option>\n" );
               }
               out.write( "</select></td>\n" );
               out.write( "</tr>\n" ); 
               nReadParams++;

            } else if( p.getClass().getName()
                        .equals("uk.ac.starlink.jpcs.BooleanParameter") ) {
               out.write( parName + ": " + prompt );
               out.write( "</td><td>" );
               out.write( "<select name=\"" + parName + "\">\n" );
               if( sugStr.equals("true") ) {
                  out.write( "<option selected>true</option>\n" );
                  out.write( "<option>false</option>\n" );
               } else if( sugStr.equals("false") ) {
                  out.write( "<option selected>false</option>\n" );
                  out.write( "<option>true</option>\n" );
               } else {
                  out.write( "<option selected value=\"\"></option>\n" );
                  out.write( "<option>true</option>\n" );
                  out.write( "<option>false</option>\n" );
               }
               out.write( "</select></td>\n" );
               out.write( "</tr>\n" ); 
               nReadParams++;
            
            } else {
               out.write( parName + ": " + prompt );
               out.write( "</td><td>" );
               out.write( "<input type=\"" + type + "\" "
                     + "name=\"" + parName + "\" "
                     + "value=\"" + sugStr + "\"></td>\n" );
               out.write( "</tr>\n" ); 
               nReadParams++;
            }            
         }   
      } 

      out.write( "</table>\n" );
      return nReadParams; 
   }
   
}
