package uk.ac.starlink.startask;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.Iterator;

import net.jini.core.entry.Entry;

/** A class to hold a list of data objects required for a StarTask. The data
*   objects will be 
*   The data objects may be:
*   <ul>
*   <li>A URL of a file. Note that a URL may be given on the command line in
*   the case of NDFs and {@link uk.ac.starlink.jpcs.FilenameParameter}s
*   <li>An array of String to form a simple text file
*   </ul>
*/
public class StarTaskDataPack
 implements Entry, StarTaskDataPacket, Serializable {

/** The StarTaskRequestId to be matched
*/ 
public StarTaskRequestId reqId;

/** The pack
*/
public ArrayList pack = null;

/** Construct an empty StarTaskDataPack
*/
public StarTaskDataPack() {
}

/** Convert all the data objects to local files
*/
public void makeLocal() throws Exception {
System.out.println( "DataPack size " + pack.size() );
   Iterator it = pack.iterator();
   while( it.hasNext() ) {
      ((StarTaskDataPacket)it.next()).makeLocal();
   }
}

/** Convert all the data objects to local files
*/
public StarTaskDataPacket dispose() throws Exception {
   StarTaskDataPack returnPack = new StarTaskDataPack();
   Iterator it = this.iterator();
   while( it.hasNext() ) {
      StarTaskDataPacket dp = (StarTaskDataPacket)it.next();
      StarTaskDataPacket returnPacket = dp.dispose();
      if( returnPacket != null ) {
         returnPack.add( returnPacket );
      }
   }
   
   if( returnPack.size() > 0 ) {
      return returnPack;
   } else {
      return null;
   } 
}

/** Put a {@link StarTaskDataPacket} in this StarTaskDataPack
*   @param packet The data packet to add to the pack
*/
void add( StarTaskDataPacket packet ) {
   if( pack == null ) {
      pack = new ArrayList();
   }
   pack.add( packet );
}

/** Put a {@link StarTaskDataPacket} in this StarTaskDataPack
*   @param type The type of data packet to add to the pack
*   @param pars parameters for creating the StarTaskDataPacket.
*/
void add( String type, String[] pars ) throws Exception {
   StarTaskDataPacket dataPacket;                              
   if( type.equals( "file" ) ) {
System.out.println( "New StarTaskFile" );
       switch( pars.length ) {
          case 1: dataPacket = new StarTaskFile( pars[0] ); break;
          case 2: dataPacket = new StarTaskFile( pars[0], pars[1] ); break;
          case 3:
           dataPacket = new StarTaskFile( pars[0], pars[1], pars[2] ); break;
          default: throw new StarTaskException(
                    "Not 1 - 3 parameters for new StarTaskFile" );
       }
 
   } else if( type.equals( "URL" ) ) {
       switch( pars.length ) {
          case 1: dataPacket = new StarTaskURL( pars[0] ); break;
          case 2: dataPacket = new StarTaskURL( pars[0], pars[1] ); break;
          case 3:
           dataPacket = new StarTaskURL( pars[0], pars[1], pars[2] ); break;
          default: throw new StarTaskException(
                    "Not 1 - 3 parameters for new StarTaskURL" );
       }

   } else {
       throw new StarTaskException(
        "Illegal type '" + type + "' for StarTaskDataPacket" );
   }
   
/*  Add the StarTaskDataPacket to this StarTaskDataPack */
   add( dataPacket );

}

/** Set an ID for this StarTaskDataPack
*   @param id the id
*/
void setId( StarTaskRequestId id ) {
   reqId = id;
}

/** Get the size of the pack.
*   @return the number of packets in the pack
*/
int size() {
   if( pack != null ) {
      return pack.size();
   } else {
      return 0;
   }
}

/** Get an iterator for this StarTaskdataPack
*   @return the iterator
*/
Iterator iterator() {
   if( pack != null ) {
      return pack.iterator();
   } else {
      return null;
   }
}

}
