/*
 * 
 * Copyright 2003 CCLRC. All Rights Reserved. 
 * 
  */

package uk.ac.starlink.startask;

import uk.ac.starlink.jpcs.TaskReply;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Defines a simple remote interface for Starlink Dtasks.
 *
 * @author Alan Chipperfield
 * 
 */
public interface StarTask extends Remote {

    /**
     * Runs a task directly as a package Class Method
     *
     * @return the reply from the task
     * @throws RemoteException if a remote communication problem occurs
     * @throws other Exception for other reasons
     */
    TaskReply runTask( String pkg, String task, String[] params )
     throws Exception;

    /**
     * Runs a task via the ShellRunner runPack Method
     *
     * @return the reply from the task
     * @throws RemoteException if a remote communication problem occurs
     * @throws other Exception for other reasons
     */
    TaskReply shellRunTask( String pkg, String task, String[] params)
     throws Exception;
}
