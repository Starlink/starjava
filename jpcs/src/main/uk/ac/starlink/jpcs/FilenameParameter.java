package uk.ac.starlink.jpcs;

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.URL;


/** This class is use for parameters representing filenames. It is the same as
 *  StringParameter except that if the value of the parameter has the form
 *  of a URL, the URL is used to get the file into the current working directory
 *  and the value supplied by getString() changed to point to the local file.
  * @author Alan Chipperfield (Starlink)
  * @version 0.0
 */
public class FilenameParameter extends Parameter {

/* localName holds the local filename where a URL has been fetched */
   String localName = null;
   File file;

/** Constructs a StringParameter with the specified fields set.
  * @param position the command line position
  * @param name the parameter name
  * @param prompt the prompt string
  * @param vpath the value path
  * @param ppath the suggeste value path
  * @param keyword the parameter keyword
  */
   public FilenameParameter( int position, String name, String prompt,
                            String vpath, String ppath, String keyword )
         throws ParameterException {
      super ( position, name, prompt, vpath, ppath, keyword );
   }
   

/** Constructs a StringParameter with the specified fields set.
  * No command line position is allocated.
  * @param name the parameter name
  * @param prompt the prompt string
  * @param vpath the value path
  * @param ppath the suggeste value path
  * @param keyword the parameter keyword
  */
   public FilenameParameter( String name, String prompt,
                            String vpath, String ppath, String keyword )
         throws ParameterException {
      this ( 0, name, prompt, vpath, ppath, keyword );
   }

/** Constructs a StringParameter with the given name. Other fields will take
  * default values.
  * @param nam the parameter name
  */
   public FilenameParameter( String nam )
         throws ParameterException {
      this( 0, nam, "", null, null, nam );
   }
   
  
/**
  * Return true if the given Object is suitable as a value for this parameter.
  * Suitable Objects are:
  * <UL>
  * <LI> A String
  * <LI> A {@link ParameterStatusValue}
  * </UL>
  * @param obj the Object to be checked
  * @return <code>true</code> if the Object is valid; <code>false</code>
  * otherwise.
  */
   protected boolean checkValidObject( Object obj ) {
      if ( obj instanceof String || 
           obj instanceof ParameterStatusValue ) {
         return true;
      
      } else {
         return false;

      }
   }
  

/**
  * Return the filename to be used. If the name has the form of a URL get the
  * referenced file into the current directory (as URL_filename) and return the
  * local filename.
  *
  * @return the String value
  */
    public String getString() throws Exception {
      Object value=null;
      String retval = "";
      boolean done = false;
      int tries = 1;

/* If localName is set, it was a URL and the file has been fetched. Just check
 * it is still there.
 */
      if( localName != null ) {
         if( file.exists() ) {
            return localName;
         }
      }
      
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
            if( retval.matches( ".*://.*" ) ) {
               file = new File( retval );
               String localName = "URL_" + file.getName();
               try{
                  URL url = new URL( retval );;
                  InputStream is = url.openStream();
                  FileOutputStream file =
                   new FileOutputStream( localName );
                  byte[] b = new byte[512];
                  int nbytes;

                  while( ( nbytes = is.read( b ) ) >= 0 ) {
                     file.write( b, 0, nbytes );
                  }
   
                  is.close();
                  file.close();
               
                  retval = localName;
                  done = true;
                  
               } catch ( Throwable e ) {
// Failed to get URL into localName - try again
                  done = false;
               }
               
            } else {
// Wasn't URL - assume normal filename 
               done = true;
            }
            
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
   
   public void cancel() {
      if( localName != null ) {
         file.delete();
         localName = null;
         file = null;
      }
      super.cancel();
   }
  
}
       
