package uk.ac.starlink.startask;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/** Handles a sequence of filenames in the specified directory. Filenames in
 *  the sequence match a regular expression containing '(\\d+)' 
 *  to indicate where a sequence of digits should appear in the name.
 */

public class FilenameSequencer implements FilenameFilter {

File directory;
String nameTemplate;
Pattern pattern;
int max;

/** Creates a FilenameSequencer referring to the specified directory.
 *  @param dir the directory in which the files are required.
 *  @param template a String defining the required filenames.
 */
public FilenameSequencer( File dir, String template ) {
   directory = dir;
   nameTemplate = template;
   pattern = Pattern.compile( template );
}

/** Creates a FilenameSequencer referring to the specified directory.
 *  @param dir the directory in which the files are required.
 *  @param template a String defining the required filenames.
 */
public FilenameSequencer( String dir, String template ) {
   this( new File( dir ), template );
   
}

/** Creates a FilenameSequencer referring to the current directory.
 *  @param a String defining the required filenames.
 */
public FilenameSequencer( String template ) {
   this( new File( System.getProperty( "user.dir" ) ), template );
}

/** Runs a test of FilenameSequencer. Each run should produce in the current
 *  working directory the next file in the sequence image0.gif, image1.gif etc.,
 *  deleting the previous files and re-cycling after image5.gif.
 */
public static void main( String[] args ) throws Exception {
   String directory = "images";
   File dirF = new File( directory );
   dirF.mkdir();
   
   FilenameSequencer fs =
    new FilenameSequencer( dirF, "image(\\d+)\\.gif" );
   String name = fs.nextName( 5, false );
   File file = new File( dirF, name );
   if( file.createNewFile() ) {
      System.out.println( "\nCreated: " + fs.getAbsoluteDirectoryPath() +
      File.separator + name );
   } else {
      System.out.println( "\nFailed to create: " + 
        fs.getAbsoluteDirectoryPath() + File.separator + name );
   }
}

/** Obtain a list of existing filenames in the sequence
 */
public String[] list() {
   return directory.list( this );
}

/** Obtain a list of existing files in the sequence
 */
public File[] listFiles() {
   return directory.listFiles( this );
}

/** Obtain a list of existing filenames in the sequence sorted into 
 *  into date-last-modified order
 */
public File[] sortedFileList() {
   File[] list = listFiles();
   Arrays.sort( list, new FileDateComparator() );
   return list;
}

/** Finds the last name in the sequence
 *  @param retain True if existing files in the sequence are to be kept;
 *  false if they are to be deleted.
 *  @return the next filename in the sequence.
 */
public String lastName( boolean retain ) {
   File file;
   int filenum;
   File[] files = sortedFileList();
   int nfiles = files.length;
   String name;

   if( nfiles == 0 ) {
      name = null;
      
   } else {
// Delete old files if requested
      if( !retain ) {
         for( int i=0; i<nfiles; i++ ) {
            files[i].delete();
         }
      }
      
      name = files[nfiles-1].getName();
   }
   
   return name;
}

/** Finds the next name in the sequence.
 *  @param max the highest number in the sequence. After max, numbering will
 *  return to 0. 
 *  @param retain True if existing files in the sequence are to be kept;
 *  false if they are to be deleted.
 *  @return the next filename in the sequence.
 */
public String nextName( int max, boolean retain ) {

   int fileNumber;
// Obtain the name of the latest file, deleting existing files if requested.
   String lastname = lastName( retain );

// Obtain the next number in the sequence
   if( lastname == null ) {
      fileNumber = 0;
   } else {
      Matcher m = pattern.matcher(lastname);
      boolean match = m.matches();
      fileNumber = Integer.parseInt( m.group(1) );
// Get the next number in sequence
      fileNumber = fileNumber + 1;
      fileNumber = ( fileNumber > max ) ? 0 : fileNumber;
   }

// Construct the new filename
   String newName =  nameTemplate.replaceFirst(
            "\\(\\\\d\\+\\)\\\\", String.valueOf( fileNumber ) );

// and return it.
   return newName;
}


/** Finds the next name in the sequence.
 *  Existing files in the sequence will be deleted.
 *  @param max the highest number in the sequence. After max, numbering will
 *  return to 0. 
 *  @return the next filename in the sequence.
 */
public String nextName( int max ) throws Exception {
   return nextName( max, false );
}

/** Finds the next name in the sequence. Numbering will re-cycle after 9999.
 *  @param retain True if existing files in the sequence are to be kept;
 *  false if they are to be deleted.
 *  @return the next filename in the sequence.
 */
public String nextName( boolean retain ) {
   return nextName( 9999, retain );
}

/** Finds the next name in the sequence. Numbering will re-cycle after 9999 and
 *  existing files will be deleted.
 *  @return the next filename in the sequence.
 */
public String nextName() throws Exception {
   return nextName( 9999, false );
} 

/** Makes a FilenameSequencer a FilenameFilter
 */
public boolean accept( File dir, String name ) {
   Matcher m = pattern.matcher( name );
   return m.matches();
}

/** Gets the directory containing the filename sequence.
 *  @return the directory as a File.
 */
public File getDirectory() {
   return directory;
}

/** Gets the path of the directory containing the filename sequence.
 *  @return the path.
 */ 
public String getDirectoryPath() {
   return directory.getPath();
}

/** Gets the absolute path of the directory containing the filename sequence.
 *  @return the absolute path.
 */ 
public String getAbsoluteDirectoryPath() {
   return directory.getAbsolutePath();
}

}
