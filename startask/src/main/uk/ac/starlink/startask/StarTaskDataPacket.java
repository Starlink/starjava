package uk.ac.starlink.startask;

/** A packet of data required by the server in a Starlink distributed system
*/
interface StarTaskDataPacket {

static final int DELETE = 1;
static final int KEEP = 2;
static final int RETURN = 3;
static final int FTP = 4;
static final int UPDATE = 5;

/** Create a local version of the data.
*/
void makeLocal() throws Exception;

/** Dispose of the local version of the data.
*   Options might be: delete, retain, return to sender or move to and ftp area.
*   A packet to be returned may be generated.
*   @return a packet to be returned, or null.
*/
StarTaskDataPacket dispose() throws Exception;

}
