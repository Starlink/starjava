package uk.ac.starlink.startask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Iterator;

/** Class to transfer a file in a distributed processing system.
*/
class StarTaskFile implements StarTaskDataPacket, Serializable {

/* The name of the file on the client */
String clientName = null;

/* The name of the file on the server */
String  serverName = null;

/* The diposal method */
int dispose;

/* The file body */
ArrayList body = new ArrayList();

/** Construct a StarTaskFile from a given file. The pathname of the file created
*   on the server will be the same. The disposal method will be 'DELETE'.
*   @param localName The name of the original file.
*/
StarTaskFile( String localName ) throws Exception {
   this( localName, localName, StarTaskDataPacket.DELETE );
}

/** Construct a StarTaskFile from a given file and specifying the pathname of
*   the file to be created on the server. The disposal method will be 'DELETE'.
*   @param localName The name of the original file
*   @param dispose The disposal method.
*/
StarTaskFile( String localName, String disp ) throws Exception {
   this( localName, localName, disp );
}

/** Construct a StarTaskFile from a given file specifying the pathname of the
*   file to be created on the server and the disposal method as a string.
*   @param localName The name of the original file 
*   @param remoteName The name of the file to be created.
*   @param dispose The disposal method.
*/
StarTaskFile( String localName, String remoteName, String disp )
 throws Exception {
   
   this( localName, remoteName, StarTaskFile.disposeMethod( disp )  );
}

/** Construct a StarTaskFile from a given file specifying the pathname of the
*   file to be created on the server and the disposal method.
*   @param localName The name of the original file 
*   @param remoteName The name of the file to be created.
*   @param dispose The disposal method code
*/
StarTaskFile( String localName, String remoteName, int disp )
 throws Exception {
   final int bsz = 512; 
   if( !localName.equals( "-" ) ) {
      clientName = localName;
   }
   if( !remoteName.equals( "-" ) ) {
      serverName = remoteName;
   }
   dispose = disp;
   
   if( ( dispose != StarTaskDataPacket.RETURN ) &&
       ( dispose != StarTaskDataPacket.FTP ) ) {
      FileInputStream fis = new FileInputStream( localName );
      int nbytes = 0;
      int n = bsz;
      int off = 0;
      byte[] b = new byte[ bsz ];

/* Until the end of file */
      while( true ) {
/* repeatedly fill the buffer */
         nbytes = fis.read( b, off, n );
         if( nbytes == n ) {
/* the byte array is full */
            body.add( b );
            b = new byte[ bsz ];
            off = 0;
            n = bsz;
         } else if( nbytes >= 0 ) {
/* byte array not yet full */
            off = off + nbytes;
            n = n - nbytes;
         } else if( nbytes < 0 ) {
/* end of file - save remnant */
            if( off > 0 ) {
               byte[] br = new byte[ off ];
               System.arraycopy( b, 0, br, 0, off );
               body.add( br );
            }
            break;
         }
      }
      fis.close();
   }
}

/** A simple test of StarTaskFile.
*   <p align="center">
*   <code>
*   % java uk.ac.starlink.startask.StarTaskFile infile outfile [dispose]
*   </code>
*   </p>
*   <p>
*   Constructs a StarTaskFile from <code>infile</code> and uses the makeLocal
*   method to produce <code>outfile</code>, then calls its
*   <code>dispose()</code> method.in effect copying
*   <code>infile</code> to <code>outfile</code>.
*   </p> 
*   <code>dispose</code> is optional and defaults to KEEP in effect copying
*   <code>infile</code> to <code>outfile</code>.
*   UPDATE and RETURN don't make much sense (the same copy is just done twice)
*   and DELETE will delete <code>outfile</code> before it can be seen.
*   FTP will cause a URL to be displayed.
*/
public static void main( String[] args ) throws Exception {
   StarTaskFile stf;    
   switch( args.length ) {
   case 0:
   case 1:
      throw new StarTaskException(
       "usage: StarTaskFile infile outfile [dispose]" );
   case 2:
      stf = new StarTaskFile( args[0], args[1], StarTaskDataPacket.KEEP );
      break;
   default:   
      stf = new StarTaskFile( args[0], args[1], args[2] );
   }
   
   stf.makeLocal();

   StarTaskDataPacket ret = stf.dispose();
   if( ret != null ) {
      System.out.println( "Make return local" );
      ret.makeLocal();
   }
   
}

/** Make the local file for this StarTaskFile.
*/
public void makeLocal() throws Exception {
   
   switch( dispose ) {
      case StarTaskDataPacket.DELETE:
      case StarTaskDataPacket.KEEP:
      case StarTaskDataPacket.UPDATE:
         System.out.println( "Making " + serverName + " from " + clientName );
         Iterator it = body.iterator();
         File serverFile = new File( serverName );
         File directory = serverFile.getParentFile();
         if( !directory.exists() ) {
            if( !directory.mkdirs() ) {
               throw new StarTaskException(
                "StarTaskFile: failed to create container directory" );
            }
         } 
         FileOutputStream file = new FileOutputStream( serverName );
         while( it.hasNext() ) {
            byte[] b = (byte[])it.next();
            file.write( b, 0, b.length );
         }
         file.close();
         break;
      case StarTaskDataPacket.RETURN:
      case StarTaskDataPacket.FTP:
         break;
      default:
         throw new StarTaskException( "StarTaskFile: illegal disposal method" ); 
   }   
}

/** Dispose of the local file for this StarTaskFile.
*   DELETE causes the file to be deleted
*   UPDATE and RETURN cause a new StarTaskFile to be returned with disposal
*   method KEEP.
*   FTP causes the file to be copied to the FTP area defined by the System
*   property "uk.ac.starlink.startask.ftpDirectory" and a {@link StarTaskURL}
*   for it returned - the URL is constructed from the URL for the FTP area
*   as defined by the System property "uk.ac.starlink.startask.ftpLocator"
*   with the file pathname appended. 
*   @return a StarTaskDataPacket to return, or null
*/
public StarTaskDataPacket dispose() throws Exception {
   StarTaskDataPacket dataPacket = null;
   switch( dispose ) {
      case StarTaskDataPacket.UPDATE:
         dataPacket =
          new StarTaskFile( serverName, clientName, StarTaskDataPacket.KEEP );
      case StarTaskDataPacket.RETURN:
         dataPacket =
          new StarTaskFile( clientName, serverName, StarTaskDataPacket.KEEP );
      case StarTaskDataPacket.DELETE:
         File file = new File( serverName );
         file.delete();
         break;
      case StarTaskDataPacket.FTP:
         String ftpDirectory = System.getProperty(
          "uk.ac.starlink.startask.ftpDirectory",
          "/home/starlink/ftp/pub/ajc" );
         String ftpLocator = System.getProperty(
          "uk.ac.starlink.startask.ftpLocator",
          "ftp://ftp.starlink.rl.ac.uk/pub/users-ftp/ajc" );
/*     Copy the local file to the FTP area */
         File inFile = new File( serverName );  
         FileInputStream is = new FileInputStream( inFile );
         FileOutputStream os = new FileOutputStream(
          new File( ftpDirectory + File.separator + serverName  ) );
         byte[] b = new byte[512];
         int nbytes;

         while( ( nbytes = is.read( b ) ) >= 0 ) {
            os.write( b, 0, nbytes );
         }

/*    Close the Streams and delete the input file */    
         is.close();
         os.close();
         inFile.delete();
          
/*     Construct the URL */
/*     and construct the StarTaskURL */
        dataPacket = new StarTaskURL(
         ftpLocator + "/" + serverName, clientName );
        break;
      default: dataPacket = null;
   }
   
   return dataPacket;
}

/** Convert a disposal method in the form of a String to the corresponding int
  * code as defined in the {@link StarTaskDataPacket} class.
  * <p>
  * "d" or "delete" - DELETE<br>
  * "k" or "keep"   - KEEP<br>
  * "r" or "return" - RETURN<br>
  * "f" or "ftp"    - FTP<br>
  * "u" or "update" - UPDATE
  * </p>
  */
private static int disposeMethod( String key ) throws StarTaskException {
   int disp;
   String ldisp = key.toLowerCase();
   if( ldisp.equals( "delete" ) || ldisp.equals( "d" ) ) {
      disp = StarTaskDataPacket.DELETE;
   } else if( ldisp.equals( "keep" ) || ldisp.equals( "k" ) ) {
      disp = StarTaskDataPacket.KEEP;
   } else if( ldisp.equals( "return" ) || ldisp.equals( "r" ) ) {
      disp = StarTaskDataPacket.RETURN;
   } else if( ldisp.equals( "ftp" ) || ldisp.equals( "f" ) ) {
      disp = StarTaskDataPacket.FTP;
   } else if( ldisp.equals( "update" ) || ldisp.equals( "u" ) ) {
      disp = StarTaskDataPacket.UPDATE;
   } else {
      throw new StarTaskException(
       "StarTaskFile: Invalid dispose string " + key );
   }
   
   return disp;
}
 

}
