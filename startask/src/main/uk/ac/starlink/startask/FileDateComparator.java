package uk.ac.starlink.startask;

import java.util.Comparator;
import java.io.File;
import java.io.FileNotFoundException;

/** A Comparator for comparing the date last modified of files represented
 *  by instances of File.
 */
public class FileDateComparator implements Comparator {

/** Compare the date last modified of the two files
 *  @param f1 the first File
 *  @param f2 the second File
 *  @return a negative integer if f1 was last modified earlier than f2
 *          a positive integer if f1 was last modified later than f2
 *          0 if the two dates are equal
 *  @throws ClassCastException if the given Objects are not Files.
 */
public int compare( Object f1, Object f2 )
 throws ClassCastException {
   int retVal;
   long date1 = ((File)f1).lastModified();
   long date2 = ((File)f2).lastModified();
   
   if( date1 < date2 ) {
      retVal = -1;
   } else if( date1 > date2 ) {
      retVal = 1;
   } else {
      retVal = 0;
   }
   
   return retVal;
}

}
