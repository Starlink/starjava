package uk.ac.starlink.jpcs;

import java.lang.reflect.*;
import java.util.regex.*;
import java.io.*;
import java.text.*;

/**
  * Parameter is the base class for Starlink application parameters.
  * It defines fields to describe the parameter:
  * <ul>
  * <li> name and keyword
  * <li> command line position
  * <li> state
  * <li> value search paths
  * <li> prompt strings
  * <li> global parameter associations
  * <li> value
  * <li> a 'request method' used if the state and vpath indicate 'prompt'.
  * </ul>
  * and methods to set and get them.
  * The parameter value is stored as an Object. 
  * Subclasses may insist upon certain value classes such as Double, Int and so
  * on.
  *
  * @author Alan Chipperfield (Starlink)
  * @version 0.0
  */
 
public class Parameter implements Serializable {

   static final Pattern association =
          Pattern.compile( "(@?-@?)([\\w-]*)\\.([\\w-]*)" );

   static final boolean FROM = true;

   protected static final int MAX_TRIES = 5;
   protected static final int MAX_DIMS = 7;

   private static final int PAR_GROUND = 0;
   private static final int PAR_ACTIVE = 1;
   private static final int PAR_CANCEL = 2;
   
   private static final int PAR_READ = 0;
   private static final int PAR_UPDATE = 1;
   private static final int PAR_WRITE = 2;
   
   private static final String DEFAULT_VPATH = "prompt";
   private static final String DEFAULT_PPATH = "dynamic,default";
   
   private String parName;
   private int access = PAR_UPDATE;
   private int parPosition;
   private String parKeyword;
   private String parPromptString;
   private Vpath parVpath = new Vpath();
   private Ppath parPpath = new Ppath();
   private Object parDefault;
   private Object parDynamic;
   private Object parValue;
   private int parState = PAR_GROUND;
   private String parRequestMethod="getPromptReply";
   private ParameterPrompt parPrompt;
   private String[] parFromGlobal;
   private String[] parToGlobal;
   private ParameterValueList parCurrentList=null;
   private String message=null;
   
   private boolean parAcceptFlag=false;
   private boolean parResetFlag=false;
   private boolean parPromptFlag=false;
   private boolean parNoPromptFlag=false;
        
/** Constructs a Parameter with the specified fields set.
  * @param position the command line position
  * @param name the parameter name
  * @param prompt the prompt string
  * @param vpath the value path
  * @param ppath the suggeste value path
  * @param keyword the parameter keyword
  * @throws ParameterException
  */
   public Parameter( int position, String name, String prompt,
                     String vpath, String ppath, String keyword )
     throws ParameterException {
      parPosition = position;
      parName = name;
      parPromptString = prompt;
      if ( vpath == null ) {
         getVpath().setPath( DEFAULT_VPATH );
      } else if ( vpath.length() == 0 ) {
         getVpath().setPath( DEFAULT_VPATH );
      } else {
         getVpath().setPath( vpath );
      }
      if ( ppath == null ) {
//System.out.println("Parameter: ppath is null - set to " + DEFAULT_PPATH );
         getPpath().setPath( DEFAULT_PPATH );
      } else if ( ppath.length() == 0 ) {
         getPpath().setPath( DEFAULT_PPATH );
      } else {
         getPpath().setPath( ppath );
      }
      if ( keyword == null ) {
         parKeyword = name;
      } else if ( keyword.length() == 0 ) {
         parKeyword = name;
      } else {
         parKeyword = keyword;
      }
/* Use the utility method in ValuePath to find the required request Method */
//      parRequestMethod = ValuePath.getMethod( this.getClass(), "getPromptReply" ); 
/* Create a new parameter prompt for this Parameter */
      parPrompt = new ParameterPrompt( this );
//System.out.println( "Position: " + parPosition );
//System.out.println( "Name: " + parName );
//System.out.println( "Keyword: " + parKeyword );
//System.out.println( "Prompt: " + parPromptString );
//System.out.println( "Vpath: " + parVpath );
//System.out.println( "Ppath: " + parPpath );
   }
   
/** Constructs a Parameter with the specified fields set.
  * No command line position is allocated.
  * @param name the parameter name
  * @param prompt the prompt string
  * @param vpath the value path
  * @param ppath the suggested value path
  * @param keyword the parameter keyword
  * @throws ParameterException
 */
   public Parameter( String name, String prompt,
                     String vpath, String ppath, String keyword )
     throws ParameterException {
      this( 0, name, prompt, vpath, ppath, keyword );
   }

/** Constructs a StringParameter with the given name. Other fields will take
  * default values.
  * @param name the parameter name
  */  
   public Parameter( String name ) throws ParameterException {
      this( 0, name, "", null, null, name );
   }
   
/**
  * Tells if the given Object is suitable as a value for this parameter.
  * @param obj the Object to be checked
  * @return true - A basic Parameter may take any Object as its value.
  * @throws ParameterException
  */
   protected boolean checkValidObject( Object obj ) {
      return true;
   }
  
/**
 * Generates a parameter value object for the general Parameter class from the
 * given String. If the string cannot be interpreted as a Number, an attempt
 * is made to interpret it as a Starlink status value (Null or Abort) and, if
 * that fails, as a boolean encoding. If it is none of those, the given string
 * is returned.
 * @param str String to be decoded
 * @return the parameter value 
 * @throws Exception
 */
   Object fromString( String str ) throws Exception {
//   System.out.println( "String is: " + str );
      
      Object obj;
/* See if it parses as a Number */
      DecimalFormat df = new DecimalFormat();

/* Set ParsePosition after any leading spaces */
      ParsePosition pp = new ParsePosition( 0 );
      
      String trimmedString = str.trim();
      obj = df.parse( str, pp );
      if ( pp.getIndex() != trimmedString.length() ) {
         if ( str.equals( "!" ) ) {
/* If not, is it null or abort response */
            obj = new NullParameterValue();
         } else if ( str.equals( "!!" ) ) {
            obj = new AbortParameterValue();
         } else {
/* or a Starlink boolean value */
            try {
               obj = new Boolean( BooleanParameter.getBooleanFromString( str ) );
            } catch ( Exception e ) {
/* If all else has failed, make it a String */
               obj = str;
            }
         }
      }
         
      return obj;
   }
   
/** Puts the value of this Parameter to the appropriate interpretation of the
 *  given String.
 *  @param str the String to be interpreted.
  * @throws Exception
 */
   public void putString( String str ) throws Exception {
      set( fromString( str ) );
      return;
   }
   
/**
 * Tells if this parameter is active - that is if it has been given a value.
 *
 * @return <code>true</code> if the parameter is active; <code>false</code>
 * otherwise.
 */
   protected boolean isActive() {
      return parState == PAR_ACTIVE;
   }
   
   
/**
 * Tells if this parameter is at 'ground' level - that is the vpath will be
 * searched to find a value.
 *
 * @return <code>true</code> if the parameter is at ground level;
 * <code>false</code> otherwise.
 */
   protected boolean isGround() {
      return parState == PAR_GROUND;
   }
   
   
/**
 * Tells if this parameter is 'cancelled' - that is a prompt will be issued for
 * any requested new value.
 *
 * @return <code>true</code> if the parameter is active; <code>false</code>
 * otherwise
 */
   protected boolean isCancelled() {
      return parState == PAR_CANCEL;
   }
   
/**
 * Tells if this parameter is NullParameterValue.
 *
 * @return <code>true</code> if the parameter value is NullParameter;
 * <code>false</code> otherwise.
 */
   protected boolean isNull() {
      return getValue() instanceof NullParameterValue;
   }
   
/**
 * Tells if this parameter is AbortParameterValue.
 *
 * @return <code>true</code> if the parameter value is NullParameter;
 * <code>false</code> otherwise.
 */
   protected boolean isAbort() {
      return getValue() instanceof AbortParameterValue;
   }

/**
 * Ensures that the Parameter has a value. If the Parameter state is 'ground',
 * the Parameter's vpath is searched; if the state is 'cancelled', a new value
 * is requested (usually with a prompt). The value obtained may be a
 * {@link ParameterStatusValue}, that is {@link NullParameterValue} (!) or
 * {@link AbortParameterValue} (!!).
  * @throws ParameterException
 */
   protected void makeActive() throws Exception {
      Object val = null;
      try {
         if ( isGround() ) {
//System.out.println("Parameter.makeActive: isGround");
/* If the Parameter is in the ground state, we first check if the Vpath has
 * been overridden by the Prompt flag.
*/
            if ( getPromptFlag() ) {
/* Prompt is set - use the RequestMethod.
*/
//System.out.println("PromptFlag set");
               val = requestValue();
               setValue( val );
             
            } else {
                          
/* Otherwise we construct a Vpath and search along it for a value.
 * The Object returned by Vpath.findValue() is valid; else an Exception is
 * thrown
 */
               val = getVpath().findValue(this);
               setValue( val );
         
            }
      

         } else if ( isCancelled() ) {

//System.out.println("Parameter.makeActive: isCancelled");
/* If the Parameter is in the cancelled state, we request a value using this
 * Parameter's requestValue() method. The Object returned by requestValue() is
 * valid; else an Exception is thrown
 */
            val = requestValue();
            setValue( val );
         
         }

      } catch ( InvocationTargetException e ) {
//System.out.println( "Parameter.makeActive: throws ParameterException " +
//ParameterUtility.getBaseCause( e ) );  
            throw new ParameterException( 
              ParameterUtility.getBaseCause(e).getMessage() );
      }

      return;
   }
   
/** Sets the Parameter to the GROUND state. The Parameter value is left in place
  * and thus can act as a 'current' value in this session.
 */
   protected void deActivate() {
//System.out.println( "deActivate " + getName() );
      setState( PAR_GROUND );
      setAcceptFlag( false );
      setResetFlag( false );
      setPromptFlag( false );
      setNoPromptFlag( false );
      parDynamic = null;
      return;
   }
 
/**
 * Sets a valid parameter value.  Checks first (with {@link #checkValidObject})
 * that the given Object is valid for this Parameter. If it is valid, the
 * Parameter value is set and its state is set to 'active'. No check is made on
 * the current state of the Parameter. If the Object is null, no changes are
 * made.
 *
 * @param obj an object to set as the parameter value.
 * @throws ParameterException if the object is not valid.
 * @throws ParameterException
 */
   public void setValue( Object obj ) throws ParameterException {
      if ( checkValidObject( obj ) ) {
         set( obj );
      } else {
         throw new ParameterException( 
           "Attempt to set invalid object " + obj +
           " as value of parameter " + getKeyword() );
      }
      return;
   } 
   
   
/**
 * Sets the parameter value. No check is made on the validity of the given
 * Object or the current state of the Parameter. The Parameter state is set to
 * 'active'.
 *
 * @param obj an Object to set as the parameter value.
 */
   protected void set( Object obj ) {
      parValue = obj;
      setState ( PAR_ACTIVE );
      return;
   }
   
   
/**
 * Gets the parameter value Object, or null is the Parameter is not active.
 *
 * @return The Object set as the parameter value.
 */
   protected Object getValue() {
      return parValue;
   }
   
/** Gets the 'current' value of the Parameter. The 'current' value is the
 *  last-used-successfully value. 
 *  @return an Object created from the String saved for this Parameter in the
 *  ParameterValues file associated with this task, or null if no value has
 *  been saved.
 */
   protected Object getCurrent() {
      Object retval = null;
      if ( !getResetFlag() ) {
//System.out.println("RESET is false");
         ParameterValueList pvList = getCurrentList();
         if ( pvList != null ) {
//System.out.println( "Got the pvlist" );
//pvList.list();
            try {
               retval = fromString( pvList.getValue( getName() ) );
            } catch ( Exception e ) {
//System.out.println("Error getting value from the pvlist");
//e.printStackTrace();
               retval = null;
            }
         }
      }
      
      return retval;
   }
   
/** Sets this Parameter's value to its 'current' value. If there is no
 *  associated {@link ParameterValueList} or if the list does not contain 
 *  a value for this Parameter, the value is set to null.
 */
   protected void setToCurrentValue() {
      set( getCurrent() );
   }
  

/** Sets the 'static default' value for this Parameter. Checks first (with 
 *  {@link #checkValidObject}) that the given Object is valid for this
 *  Parameter.
 *  @param obj the default Object.
 *  @throws ParameterException
 */
   protected void setDefault( Object obj ) throws ParameterException {
      if ( checkValidObject( obj ) ) {
         parDefault = obj;
      } else {
         throw new ParameterException( 
           "Attempt to set invalid object " + obj.toString() +
           " as default value for parameter " + getKeyword() );
      }
      return;
   }
   
/** Tells if this Parameter has a 'static default' value.
 *  @return <code>true</code> if this Parameter has a default value.
 */
   protected boolean hasDefault() {
      return parDefault != null;
   }
   
/** Gets the 'static default' value Object for this Parameter.
 *  @return the default value
 */
   protected Object getDefault() {
      return parDefault;
   }
   
  
/**
 * Set the parameter value to the given ArrayParameterValue.
 * @param The ArrayParameterValue
 */
   public void putArray( ArrayParameterValue arr ) throws ParameterException {
      String type = arr.getComponentType();
      if ( checkValidObject( arr ) ) {
         set( arr );
      } else {
         throw new ParameterException( "Invalid ArrayParameterValue " + 
            " for NumberParameter " + getKeyword() );
      }
   }
 
/** Sets the 'dynamic default' value for this Parameter. Checks first (with 
 *  {@link #checkValidObject}) that the given Object is valid for this
 *  Parameter.
 *  @param obj the dynamic default Object.
 *  @throws ParameterException
 */
   public void setDynamic( Object obj ) throws ParameterException {
//System.out.println( "setDynamic value: " + obj );
      if ( checkValidObject( obj ) ) {
//System.out.println( "setDynamic: Object checked OK" );
         parDynamic = obj;
      } else {
         throw new ParameterException( 
           "Attempt to set invalid object " + obj.toString() +
           " as value of parameter " + getKeyword() );
      }
      return;
   }
      
/** Sets the 'dynamic default' value to a Double object representing the given
 *  double argument, if permitted for this Parameter.
 *  @param v the dynamic default value.
 *  @throws ParameterException
 */
   public void setDynamic( double v ) throws ParameterException {
      setDynamic( new Double( v ) );
      return;
   }
   
/** Sets the 'dynamic default' value to an Integer object representing the given
 *  int argument, if permitted for this Parameter.
 *  @param v the dynamic default value.
 */
   public void setDynamic( int v ) throws ParameterException {
      setDynamic( new Integer( v ) );
      return;
   }
   
/** Sets the 'dynamic default' value to a Double object representing the given
 *  float argument, if permitted for this Parameter.
 *  @param v the dynamic default value.
 *  @throws ParameterException
 */
   public void setDynamic( float v ) throws ParameterException {
//      setDynamic( new Double( (double)v ) );
      setDynamic( new Float( v ) );
      return;
   }
   
/** Sets the 'dynamic default' value for this Parameter to a Boolean object
 *  representing the given boolean argument, if permitted for this Parameter.
 *  @param v the dynamic default value
 *  @throws ParameterException
 */
   public void setDynamic( boolean v ) throws ParameterException {
      setDynamic( new Boolean( v ) );
      return;
   }
   
/** Gets the 'dynamic default' value for this Parameter. The value is guaranteed
 *  to be valid for this Parameter.
 *  @param obj the dynamic default Object.
 */
   protected Object getDynamic() {
//System.out.println( "getDynamic: value is " + parDynamic );
      return parDynamic;
   }

/** Set the association for this Parameter.
 *  @param assoc the association string of the form "<->GLOBAL.NAME
 *         where &lt; and &gt; indicate read and/or write to parameter NAME
 *         in file GLOBAL.PAR. The filename and parameter name will always be
 *         cast to upper case.
 *  @throws ParameterException
 */
   protected void setAssociation( String assoc ) throws ParameterException {
      Matcher m = association.matcher( assoc );
      if( m.matches() ) {
         boolean noRead = false;
//System.out.println( "Association groups" );
//System.out.println( m.group(1) );
//System.out.println( m.group(2) );
//System.out.println( m.group(3) );
         String access = m.group(1);
         String fileName = m.group(2).toUpperCase();
         String paramName = m.group(3).toUpperCase();
         if ( access.charAt(0) == '@' ) {
            parFromGlobal = new String[2];
            parFromGlobal[0] = fileName;
            parFromGlobal[1] = paramName;
         } else {
            noRead = true;
         }
         
         if ( access.endsWith("@") ) {
            parToGlobal = new String[2];
            parToGlobal[0] = fileName;
            parToGlobal[1] = paramName;

         } else if ( noRead ) {
            throw new ParameterException( "Parameter " + getKeyword() +
              " Illegal association string - " + assoc + " no @'s." );
         }
          
      } else {
         throw new ParameterException( "Parameter " + getKeyword() +
           " Illegal association string - " + assoc );
      }
      
   } 

/** Gets  the Global Parameter value associated with this Parameter.
 *  @return the Global Parameter value.
 */
   protected Object getGlobal() {
      Object retval=null;

      String[] globalName = getGlobalName( FROM );
//System.out.println(
//  "getGlobal: globalName " + globalName[0] + "." + globalName[1] );
      if ( globalName != null ) {
         try {
            ParameterValueList pvList =
              ParameterValueList.readList( globalName[0] + ".PAR" );
            if ( pvList != null ) {
               String globalVal = pvList.getValue( globalName[1] );
//System.out.println("getGlobal: globalVal " + globalVal );
               if ( globalVal != null ) {
                  retval = fromString( globalVal );
               }
            }
         
         } catch ( Exception e ) {
//System.out.println("getGlobal: Exception " + e.getMessage() );
            retval = null;

         }
      }

      return retval;
   }
   
/** Gets the name of the associated global parametername for read or write
 *  access.
 *  @param from <code>true</code> if read acces is required;
 *  <code>false</code> if write access is required.
 */
   protected String[] getGlobalName( boolean from ) {
      if( from ) {
         return parFromGlobal;
         
      } else {
         return parToGlobal;

      }
   }
      
   
/** Sets the request Method for this Parameter. The request method is a method
  * implementing the <code>ParameterPrompter</code> interface. It is used to obtain
  * a Parameter value if a 'prompt' is indicated on the vpath or the
  * Parameter is in the 'cancelled' state when a value is requested. The default
  * value for a Parameter's request method is the protected class
  * <code>ParameterPrompt</code>.
  * @param meth the required request method. 
  */
   public void setRequestMethod( String meth ) {
      parRequestMethod = meth;
      return;
   }
   
/** Gets the request Method for this Parameter.
 *  @return the request method.
 */
   public String getRequestMethod() {
      return parRequestMethod;
   }
   

/** Requests a value for this Parameter. The value will normally be found by
 *  invoking the Parameter's request Method but this may be overridden if
 *  the Parameter's Accept or NoPrompt flags are set. If the Accept flag is set,
 *  the Parameter's suggested (prompt) value will be returned if there is one;
 *  otherwise the request will proceed normally. If the NoPrompt flag is set,
 *  the NullParameterValue will be returned. Accept takes precedence over
 *  NoPrompt
 *  @return the Parameter value.
 *  @throws Exception
 */
   protected Object  requestValue() throws Exception {
      Object retval=null;

/* If in accept mode, get suggested value
*/
      if ( getAcceptFlag() ) {
         retval = getSuggestedValue();
      }
      
/* If still no value,
*/ 
      if ( retval == null ) {
            
/*  if in NoPrompt mode, return NullParameterValue;
*/
         if ( getNoPromptFlag() ) {
            retval = new NullParameterValue();

/*  otherwise invoke the requestMethod
*/
         } else {
/* Use the utility method in ValuePath to find the required request Method */
            retval = ValuePath.getMethod(
              this.getClass(), this.getRequestMethod() ).invoke(this,null);
         
         }
      }
      
      return retval; 
   }
   
/** Supplies an appropriate value if no request (prompt) is allowed for this
  * Parameter.
  * @return a {@link NullParameterValue} object.
 */
 
   protected Object noRequestValue() {
     return new NullParameterValue();
   }

/**
 * Obtain a value Object by invoking the ParameterPrompter specified for this
 * parameter. Note that a ParameterPrompter need not actually prompt the user.
 * @return the value Object. The Object is guaranteed to be a valid object for
 * this Parameter.
 * @throws Exception
 */
   protected Object getPromptReply() throws Exception {
      ParameterReply reply;
      Object obj;
//System.out.println("getPromptReply");
      reply =  parPrompt.getReply();
      if ( reply.isNull() ) {
//System.out.println("Parameter.getPromptReply: Reply is Null Value");
         obj = new NullParameterValue();

      } else if ( reply.isAbort() ) {
//System.out.println("Parameter.getPromptReply: Reply is Abort Value");
        obj = new AbortParameterValue();

      } else {
//System.out.println("Parameter.getPromptReply: Convert Value from String '" +
//reply.toString() + "'" );
         obj = this.fromString( reply.toString() );

      }
      return obj;
   }

/**
 * Sets the parameter into the 'cancelled' state. Future attempts to get a value
 * will ignore the value search path and use the ParameterPrompter for this
 * parameter. The parameter value is left in place so it can be used as the
 * current value.
 */   
   public void cancel() {
      setState( PAR_CANCEL );
      setAcceptFlag( false );
      setResetFlag( false );
   }
   

/**
 * Gets the command-line position for this parameter.
 *
 * @return the command-line position or null
 */
    protected int getPosition() {
      return parPosition;
   }
   
/**
 * Gets the type of the value of this parameter
 *
 * @return the name of the class of this parameter value
 */
   protected String getType() {
      if (  parState == PAR_GROUND ) {
         return "unknown";
      } else {
         return parValue.getClass().getName();
      } 
   }
   
/**
 * Gets the name for this parameter. 
 *
 * @return the name
 */
   public String getName() {
      return parName;
   }
   
/**
 * Gets the keyword for this parameter. The keyword will be used in all
 * communication with the user and normally defaults to the parameter name.
 *
 * @return the keyword
 */
   public String getKeyword() {
      return parKeyword;
   }
   
/**
 * Sets the String which may be used to expand on the keyword in prompts or
 * elsewhere. A null String is allowed.
 *
 * @param str the required 'prompt' string
 */
   public void setPromptString( String str ) {
      parPromptString = str;
   }
   
/**
 * Gets the String which may be used to expand on the keyword in prompts or
 * elsewhere. A null String is allowed.
 *
 * @return the 'prompt' string
 */
   public String getPromptString() {
      return parPromptString;
   }
   
/**
 * Gets the value path for this parameter. The value path is a
 * comma-separated list of places to look for a parameter value. List items may
 * be:
 * <UL>
 * <LI> current - the last-used value
 * <LI> global - the associated Global parameter value
 * <LI> default - the 'static' default value
 * <LI> dynamic - the 'dynamic' default value
 * <LI> prompt - the value obtained from this.{@link #requestValue}
 * <LI> noprompt - return {@link NullParameterValue} (prevents a prompt). 
 * </UL>
 * @return the value path
 */
   private Vpath getVpath() {
      return parVpath;
   }
   
/**
 * Gets the prompt path for this Parameter. The prompt path is a
 * comma-separated list of places to look for a suggested value to be displayed
 * in any parameter prompts.  List items may be:
 * <UL>
 * <LI> current - the last-used value
 * <LI> global - the associated Global parameter value
 * <LI> default - the 'static' default value
 * <LI> dynamic - the 'dynamic' default value
 * </UL>
 *
 * @return the prompt path
 */
   protected Ppath getPpath() {
      return parPpath;
   }
   
/**
 * Gets this Parameter's value as an Object. The Parameter is made 'active' if
 * necessary. 
 * In most cases the object type will be known so a more specific method may
 * be used to get the value.
 *
 * @return the value object
 * @throws Exception
 */
    public Object getObject() throws Exception {
      this.makeActive();
      return getValue();
   }

/**
 * Gets this Parameter's value as a String. The Parameter is made 'active' if
 * necessary. 
 *
 * @return the String value
 */
    public String getString() throws Exception {
      Object value=null;
      String retval = "";
      boolean done = false;
      int tries = 1;

      while ( !done && (tries <= MAX_TRIES)  ) {
         tries++;
//System.out.println("getString: Make active");
         this.makeActive();
/* Parameter is active - get value or throw exception */
//System.out.println("getString: Is active");
         value = getValue();
            
         if ( value instanceof String ) {
//System.out.println("getString: Is String");
            retval = (String)value;
            done = true;

         } else if ( value instanceof ParameterStatusValue ) {
//System.out.println("getString: Is ParameterStatusValue");
            done = true;
               
         } else {
//System.out.println("getString: Is other");
            retval = value.toString();
//System.out.println("getString: Converted to String");
            done = true;

         }
      }
       
      if ( done ) {
         if ( value instanceof ParameterStatusValue ) {
            throw ((ParameterStatusValue)value).exception( this );
         }
         
      } else {
         throw new ParameterException( 
            "getString for Parameter " + getKeyword() + ": "
            + String.valueOf( MAX_TRIES )
            + " attempts failed to get a good value" );
      }
      
      return retval;
  }
     
/**
 * Sets the parameter state:
 * <ul>
 * <li> PAR_GROUND - Initial state no value has been set and the value path will
 * be followed to find one.
 * <li> PAR_ACTIVE - A value has been set.
 * <li> PAR_CANCEL - The parameter has been cancelled - the specified
 * ParameterPrompter will be invoked if another value is requested.
 * </ul>
 *
 * @param istat the required state
 */ 
   void setState( int istat ) {
      parState = istat;
   }
   
/**
 * Gets the parameter state:
 * <ul>
 * <li> PAR_GROUND - Initial state no value has been set and the value path will
 * be followed to find one.
 * <li> PAR_ACTIVE - A value has been set.
 * <li> PAR_CANCEL - The parameter has been cancelled - the specified
 * ParameterPrompter will be invoked if another value is requested.
 * </ul>
 *
 * @param the required state
 */ 
   private int getState() {
      return parState;
   }
   
/** Sets a message to be output before prompting
  * @param the message to be displayed
  */
   protected void setMessage( String mess ) {
      message = mess;
   }
   
/** Gets any message to be output before prompting.
  * @return the message to be displayed or null is there is no message.
  */
   protected String getMessage() {
      return message;
   }
     
/**  Gets the suggested value for this Parameter. The suggested value is found
  *  by searching the Ppath for the parameter. The search may be modified by
  *  setting RESET mode, which causes any 'current' value to be ignored.
  *  @return The suggested value.
  */
   public Object getSuggestedValue() {
//System.out.println("getting Suggested Value for Parameter " + getName() );
      Object suggested = null;
      try {
//System.out.println("Searching Ppath");
         suggested = getPpath().findValue( this );
      } catch ( Exception e ) {
//System.out.println("Error Searching Ppath");
         suggested = null;
      }
      return suggested; 
   }
   
/**  Returns the Parameter value as a String
  *  @return the String representaion of the Parameter value.
  */
   public String toString() {
      String retString;
      Object val = getValue();
      if ( val == null ) {
         retString = "null";
      } else {
         retString = val.toString();
      }
      
      return retString;
   }

/**  Sets this Parameter's access mode.
 *   @param the required mode "READ", "UPDATE" or "WRITE".
 */
   protected void setAccess( String mode ) throws ParameterException {
      if( mode.equalsIgnoreCase( "READ" ) ) {
         access = PAR_READ;
      } else if( mode.equalsIgnoreCase( "UPDATE" ) ) {
         access = PAR_UPDATE;
      } else if( mode.equalsIgnoreCase( "WRITE" ) ) {
         access = PAR_WRITE;
      } else {
         throw new ParameterException( "Illegal access mode '" + mode + "'" );
      }
   }
      
/**  Tells if this Parameter has read access.
 *   @return true if the access mode is READ or UPDATE; false if it is WRITE.
 */ 
   public boolean isRead() {
      if( access < 2 ) {
         return true;
      } else {
         return false;
      }
   }
      
/**  Tells if this Parameter has write access.
 *   @return true if the access mode is WRITE or UPDATE; false if it is READ.
 */ 
   public boolean isWrite() {
      if( access < 2 ) {
         return true;
      } else {
         return false;
      }
   }
      
/**  Sets this Parameter's ACCEPT mode. If the Accept flag is true, if a prompt
 *  would be issued, the suggested value will be used instead.
 *  @param the required ACCEPT mode.
 */
   protected void setAcceptFlag( boolean val ) {
//System.out.println( "setting AcceptFlag for Parameter " + this.getName() );
      parAcceptFlag = val;
   }
   
/** Sets this Parameter's RESET mode. If Reset flag is true, defaults and
 *  suggested values are found as if there is no 'current' value.
 *  @param val the required RESET mode
 */
   protected void setResetFlag( boolean val ) {
//System.out.println( "setting ResetFlag for Parameter " + this.getName() );
      parResetFlag = val;
   }
   
/** Sets this Parameter to PROMPT mode. If the Prompt flag is true, a prompt
 *  will be issued for all parameters not defined on the command line.
 *  @param val the required PROMPT mode 
 */
   protected void setPromptFlag( boolean val ) {
//System.out.println( "setting PromptFlag for Parameter " + this.getName() );
      parPromptFlag = val;
   }
   
/**  Sets this Parameter to NOPROMPT mode. If the NoPrompt flag is true and a
 *   prompt would be issued, a Null value is returned instead, 
 *   @param val the required NOPROMPT mode
 */
   protected void setNoPromptFlag( boolean val ) {
//System.out.println( "setting NoPromptFlag for Parameter " + this.getName() );
      parNoPromptFlag = val;
   }
   
/**  Gets this Parameter's ACCEPT flag.
 *   @return true if this Parameter is in ACCEPT mode.
 */
   protected boolean getAcceptFlag() {
      return parAcceptFlag;
   }
   
/**  Gets this Parameter's RESET flag. 
 *   @return true if this Parameter is in RESET mode.
 */
   protected boolean getResetFlag() {
      return parResetFlag;
   }
   
/**  Gets this Parameter's PROMPT flag. 
 *   @return true if this Parameter is in PROMPT mode.
 */
   protected boolean getPromptFlag() {
      return parPromptFlag;
   }
   
/**  Gets this Parameter's NOPROMPT flag. 
 *   @return true if this Parameter is in NOPROMPT mode.
 */
   protected boolean getNoPromptFlag() {
      return parNoPromptFlag;
   }
   
/**  Sets the current value list for this Parameter.
 *   @param currentList The ParameterValueList in which this Parameter's
 *   'current' value may be found.
 */
   protected void setCurrentList( ParameterValueList currentList ) {
      parCurrentList = currentList;
   }
   
/**  Gets the current value list for this Parameter.
 *   @param The ParameterValueList in which this Parameter's 'current' value may
 *   be found.
 */
   protected ParameterValueList getCurrentList() {
      return parCurrentList;
   }
      
}
