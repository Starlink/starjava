package uk.ac.starlink.jpcs;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.net.URL;

/** An abstract class which is inherited by Starlink application classes wishing
 *  to use the Java Parameter and Communications (PCS) infrastructure.
 *  It provides methods to set up the environment for the 'task' methods in the
 *  subclass and to run them.
 **/    
public abstract class Dtask {

public static final int DTASK_OK = 0;
public static final int DTASK_ERROR = 1;

private ArrayList taskNames = new ArrayList();
private ArrayList pLists = new ArrayList();

private Class clss = this.getClass();
private ClassLoader classLoader = clss.getClassLoader();
private String packDir;
/**
 *  Invokes a task method of the subclass with the supplied parameters.
 *  This signature is convenient for use in the main method of a
 *  subclass as the <code>args</code> array may be passed directly to it.
 *  @param args An array of Strings, the first of which must be the name of the
 *  task method to be invoked. The remainder represent the task
 *  {@link Parameter} value specifiers, as may be supplied on the command line.
 *  @throws Exception
 */
public String[] runTask( String taskName, String[] cmdline, String engineName ){ 
//System.out.println("Runtask");
   Msg msg = new Msg();
   TaskReply tr = new TaskReply();
   try{
//System.out.println( "Get Method " + engineName );;
      Method taskMethod = getTaskEngineMethod( engineName );

//System.out.println( "Get ParameterList" );
      ParameterList plist = getParameterList( taskName );

//System.out.println( "Parse the command line" );
      try {
         plist.parseCommandLine( cmdline );
      } catch( ParameterException e ) {
         msg.out( e.getMessage() );
      }
         
//System.out.println( "Invoking " + engineName );
      Object[] args = { plist, msg };
      int status = ((Integer)taskMethod.invoke( this, args )).intValue();
   
      if( plist != null ) {
         plist.deActivate( status==0 );
      }

   } catch ( InvocationTargetException e ) {
      msg.out( "InvocationTargetException" );
      msg.out( e.getCause().toString() );
//      e.printStackTrace();

   } catch ( Exception e ) {
      msg.out( "Dtask action failed" );
      msg.out( e.toString() );
//      e.printStackTrace();
   }

//System.out.println("Listing Msg");
//msg.list();
//System.out.println("End listing Msg");
   tr.setMsg( msg );
   try{
      tr.setPVList( getPVList( taskName ) );
   } catch( Exception e ) {
      msg.out( e.getMessage() );
   }   
   return tr.toXML();

}


/** Get the ParameterList for a task. If the ParameterList is not registered
 *  in the taskNames/pLists pair of ArrayLists, a new ParameterList is created
 *  by reading the task's Interface File. The new ParameterList is then
 *  registered.
 *  If a ParameterList is already registerd for the task, that ParameterList is
 *  returned.
 *  
 *  @param The name of the task.
 *  @return The ParameterList for the task.
 */
public ParameterList getParameterList( String taskName ) throws Exception {
   int taskNumber;
   ParameterList pList;
   
/* See if a ParameterList for this task is already registered */
   if ( (taskNumber = taskNames.indexOf( taskName )) < 0 ) {

/* Not registered - find the Interface File */
/* and override the taskName in case it is wrong in the interface file */
      pList = ParameterList.readIfx( findIfx( taskName ) );
      pList.setTaskName( taskName );

/* Register the newly-created ParameterList */
      registerPList( taskName, pList );
      
/* Set up the current value list for this task */
      pList.setCurrentList();

   } else {
/* ParameterList already registered */
      pList = (ParameterList)pLists.get( taskNumber );
      
   }
   
   return pList;
     
}

/** Find the Interface File for the given task.
 *  The Interface File, named <code>&lt;taskName&gt;.ifx</code> is expected to
 *  be in the current working directory or, failing that, in subdirectory
 *  'support' of the the package directory as determined by the
 *  ClassLoader.getResource() method for the sub-class. In this way, the
 *  standard Interface file may be overidden by the user's preferred version.
 *  @param the name of the task
 */
protected InputStream findIfx( String taskName ) throws Exception {

   InputStream ifx;
   String ifxName = taskName + ".ifx";
   
/* See if there's a taskname.ifx file in the current directory */
   try{
      ifx = new FileInputStream( ifxName );

   } catch ( Exception e ) {
/* There isn't a local taskname.ifx - look elsewhere */

/* Ensure correct package name */
      if( packDir == null ) {
         Package pack = clss.getPackage();
         if( pack != null ) {
            packDir = pack.getName().replace( '.', File.separator.charAt(0) );
         } else {
            packDir = "";
         }
      }
 
/* Get the interface file using the ClassLoader */
      if( classLoader != null ) {
         URL url = classLoader.getResource(
          packDir + File.separator + "support" + File.separator + ifxName );
         if( url != null ) {
            ifx = url.openStream();
         } else {
            throw new ParameterException(
             "Can't find interface file " + ifxName );
         }
         
      } else {
         throw new ParameterException( "Can't find interface file " + ifxName
                                       + " - no ClassLoader" );
      }
   }
   
   return ifx;
}

/** Register the taskName/ParameterList pair in this Dtask
  * @param the name of the task
  * @param the ParameterList
  */ 
private void registerPList( String taskName, ParameterList pList ) {
   taskNames.add( taskName );
   pLists.add( pList );
}

/** Get a task Parameter value. The Parameter will not be made active.
  * @param the name of the task
  * @param the name of the parameter
  * @return the parameter value
 */ 
public String[] getParameter( String taskName, String parameterKeyword ) {

   String[] retval = new String[1];
  
   try {
      ParameterList pList = getParameterList( taskName );
      Parameter p = pList.findKeyword( parameterKeyword );
      retval[0] = p.toString();
     
   } catch ( Exception e ) {
      retval[0] = e.toString();
   }
   return retval;

} 

/** Returns the current value for a given task parameter.
 *  Gets the ParameterList for the task, using getParameterList(), and obtains
 *  the ParameterValueList for it, by reading the task's parameter file if
 *  necessary.
*/
public String[] getCurrent( String taskName, String parameterKeyword ) {

   String[] retval = new String[1];

   try {
      ParameterList plist = getParameterList( taskName );
      Parameter p = plist.findKeyword( parameterKeyword );
      retval[0] = p.getCurrent().toString();
     
   } catch ( Exception e ) {
      retval[0] = e.toString();
   }
   return retval;
}

/** Returns the current values for a given task's parameters. The
 *  ParameterValueList is in the returned {@link TaskReply} document.
 *  @param the name of the task
 *  @return a TaskReply XML document in an array of Strings.
*/
public String[] getCurrent( String taskName ) {
   ParameterValueList pvlist;
   TaskReply tr = new TaskReply();;
   Msg out = new Msg();

   try {
      pvlist = getPVList( taskName );
      tr.setPVList( pvlist );
     
   } catch ( Exception e ) {
      out.out( e.toString() );
   }
      
   tr.setMsg( out );
   return tr.toXML();
}

/** Gets the current ParameterValueList for a given task
 *  Gets the ParameterList for the task, using getParameterList(), and obtains
 *  the ParameterValueList for it, by reading the task's parameter file if
 *  necessary.
 *  @param the task name
 *  @return the list of current values for the task's parameters.
 */
protected ParameterValueList getPVList( String taskName ) throws Exception {
   ParameterList plist = getParameterList( taskName );
   return plist.getCurrentList();
}

/** Returns the Interface File for a task.
 *  @param the name of the task
 *  @return the Interface File in the form of an array of String.
*/
public String[] getIfx( String taskName ) {
   String line;
   Msg msg = new Msg();
   msg.setBuffered( true );
   try{
      BufferedReader br =
        new BufferedReader( new InputStreamReader( findIfx(taskName) ) );
      line = br.readLine();
      while( line != null ) {
         msg.add( line );
         line = br.readLine();
      }
   
   } catch( Exception e ) {
      msg.out( "getIfx failed" );
      msg.out( e.toString() );
//      e.printStackTrace();
   }
   
   return (String[])msg.toArray( new String[0] );
}

/** Gets the Method to be invoked for a given taskName.
  * The Method will have the given name and a single String[] argument
  * comprising the command line parameters. 
  * @param the name of the method to be used.
  * @param whether the name should be prefixed with <code>jni</code>
  * @return the Method to be invoked
  */
public Method getTaskMethod( String taskName )
 throws NoSuchMethodException {
//System.out.println( "This class is " + this.getClass().getName() );   
   Class[] sig = {java.lang.String[].class};
   Method taskMethod = this.getClass().getDeclaredMethod( taskName, sig );
//System.out.println("Method is: " + taskMethod.getName() );

   return taskMethod;
}

/** Gets the Method which will do the work for a given taskName.
  * The Method will have the given name and two arguments, the associated
  * {@link ParameterList} and a {@link Msg} to receive output messages.  
  * @param the name of the method to be used.
  * @param whether the name should be prefixed with <code>jni</code>
  * @return the Method to be invoked
  */
public Method getTaskEngineMethod( String taskName )
 throws NoSuchMethodException {
//System.out.println( "This class is " + this.getClass().getName() );   
   Class[] sig = {uk.ac.starlink.jpcs.ParameterList.class,
                  uk.ac.starlink.jpcs.Msg.class };
   Method taskMethod = this.getClass().getDeclaredMethod( taskName, sig );
//System.out.println("Method is: " + taskMethod.getName() );

/* Allow the task engine method to be invoked from Dtask */
   taskMethod.setAccessible( true );

   return taskMethod;
}

}
