/*
 * 
 * Copyright 2003 CCLRC. 
 * 
 * 
 */

package uk.ac.starlink.startask;

import uk.ac.starlink.jpcs.TaskReply;
import java.io.Serializable;
import java.rmi.RemoteException;
import net.jini.core.constraint.MethodConstraints;
import net.jini.core.constraint.RemoteMethodControl;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ProxyTrustIterator;
import net.jini.security.proxytrust.SingletonProxyTrustIterator;
import net.jini.security.proxytrust.TrustEquivalence;

/** Define a smart proxy for the server. */
class Proxy implements Serializable, StarTask {
    /** The server proxy */
    final StarTask serverProxy;

    /**
     * Create a smart proxy, using an implementation that supports constraints
     * if the server proxy does.
     */
    static Proxy create(StarTask serverProxy) {
	return (serverProxy instanceof RemoteMethodControl)
	    ? new ConstrainableProxy(serverProxy)
	    : new Proxy(serverProxy);
    }

    Proxy(StarTask serverProxy) {
	this.serverProxy = serverProxy;
    }

    public boolean equals(Object o) {
	return getClass() == o.getClass()
	    && serverProxy.equals(((Proxy) o).serverProxy);
    }

    public int hashCode() {
	return serverProxy.hashCode();
    }

    /** Implement StarTask. */
    public TaskReply runTask( String pkg, String task, String[] params )
     throws Exception {
	System.out.println("Calling runTask in smart proxy");
	return serverProxy.runTask( pkg, task, params );
    }
    public TaskReply shellRunTask( String pkg, String task, String[] params )
     throws Exception {
	System.out.println("Calling shellRunTask in smart proxy");
	return serverProxy.shellRunTask( pkg, task, params );
    }

    /** A constrainable implementation of the smart proxy. */
    private static final class ConstrainableProxy extends Proxy
	implements RemoteMethodControl
    {
	ConstrainableProxy(StarTask serverProxy) {
	    super(serverProxy);
	}

	/** Implement RemoteMethodControl */

	public MethodConstraints getConstraints() {
	    return ((RemoteMethodControl) serverProxy).getConstraints();
	}

	public RemoteMethodControl setConstraints(MethodConstraints mc) {
	    return new ConstrainableProxy(
		(StarTask) ((RemoteMethodControl) serverProxy).setConstraints(
		    mc));
	}

	/*
	 * Provide access to the underlying server proxy to permit the
	 * ProxyTrustVerifier class to verify the proxy.
	 */
	private ProxyTrustIterator getProxyTrustIterator() {
	    return new SingletonProxyTrustIterator(serverProxy);
	}
    }

    /** A trust verifier for secure smart proxies. */
    final static class Verifier implements TrustVerifier, Serializable {
	private final RemoteMethodControl serverProxy;
    
	/**
	 * Create the verifier, throwing UnsupportedOperationException if the
	 * server proxy does not implement both RemoteMethodControl and
	 * TrustEquivalence.
	 */
	Verifier(StarTask serverProxy) {
	    if (serverProxy instanceof RemoteMethodControl &&
		serverProxy instanceof TrustEquivalence)
	    {
		this.serverProxy = (RemoteMethodControl) serverProxy;
	    } else {
		throw new UnsupportedOperationException();
	    }
	}

	/** Implement TrustVerifier */
	public boolean isTrustedObject(Object obj, TrustVerifier.Context ctx)
	    throws RemoteException
	{
	    if (obj == null || ctx == null) {
		throw new NullPointerException();
	    } else if (!(obj instanceof ConstrainableProxy)) {
		return false;
	    }
	    RemoteMethodControl otherServerProxy =
		(RemoteMethodControl) ((ConstrainableProxy) obj).serverProxy;
	    MethodConstraints mc = otherServerProxy.getConstraints();
	    TrustEquivalence trusted =
		(TrustEquivalence) serverProxy.setConstraints(mc);
	    return trusted.checkTrustEquivalence(otherServerProxy);
	}
    }
}
