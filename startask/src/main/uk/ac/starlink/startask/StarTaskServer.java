/*
 * 
 * Copyright 2003 CCLRC All Rights Reserved. 
 * 
 */

package uk.ac.starlink.startask;

import java.rmi.RemoteException;
import java.security.PrivilegedExceptionAction;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.config.NoSuchEntryException;
import net.jini.core.lookup.ServiceID;
import net.jini.discovery.DiscoveryManagement;
import net.jini.discovery.LookupDiscovery;
import net.jini.export.Exporter;
import net.jini.export.ProxyAccessor;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.lookup.JoinManager;
import net.jini.security.TrustVerifier;
import net.jini.security.proxytrust.ServerProxyTrust;
import uk.ac.starlink.jpcs.TaskReply;

/**
 * Defines an application server that provides an implementation of the 
 * {@link StarTask} interface.
 * <p>
 * The application uses the following arguments:
 * <p>
 * [all] - All arguments are used as options when getting the configuration
 * <p>
 * The application uses the following configuration entries, with component
 * com.sun.jini.example.hello.Server:
 * <p>
 * <dl>
 * <dt>discoveryManager</dt>
 * <dd>  type: DiscoveryManagement<br>
 *   default: new LookupDiscovery(new String[] { "" }, config)<br>
 *   Object used to discover lookup services to join.
 * </dd>
 * </dt>
 * <dt>exporter</dt>
 * <dd>    type: Exporter<br>
 *   default: none<br>
 *   The object to use for exporting the server
 * </dd>
 * <dt>loginContext</dt>
 * <dd>    type: LoginContext<br>
 *   default: null<br>
 *   If non-null, specifies the JAAS login context to use for performing a JAAS
 *   login and supplying the Subject to use when running the server. If null,
 *   no JAAS login is performed.
 * </dd>
 </dl>
 * @author Alan Chipperfield
 */
public class StarTaskServer implements StarTask, ServerProxyTrust, ProxyAccessor {

    /**
     * If the impl gets GC'ed, then the server will be unexported.
     * Store the instance here to prevent this.
     */
    private static StarTaskServer serverImpl;

    /* The configuration to use for configuring the server */
    protected final Configuration config;

    /** The server proxy, for use by getProxyVerifier */
    protected StarTask serverProxy;

    /**
     * Starts and registers a server that implements the StarTask interface.
     *
     * @param args options to use when getting the Configuration
     * @throws ConfigurationException if a problem occurs with the
     *	       configuration
     * @throws RemoteException if a remote communication problem occurs
     */
    public static void main(String[] args) throws Exception {
	serverImpl = new StarTaskServer(args);
	serverImpl.init();
	System.out.println("StarTask server is ready");
    }

    /**
     * Creates the server.
     *
     * @param configOptions options to use when getting the Configuration
     * @throws ConfigurationException if a problem occurs creating the
     *	       configuration
     */
    protected StarTaskServer(String[] configOptions) throws ConfigurationException {
	config = ConfigurationProvider.getInstance(configOptions);
    }

    /**
     * Initializes the server, including exporting it and storing its proxy in
     * the registry.
     *
     * @throws Exception if a problem occurs
     */
    protected void init() throws Exception {
	LoginContext loginContext = (LoginContext) config.getEntry(
	    "uk.ac.starlink.startask.StarTaskServer", "loginContext",
	    LoginContext.class, null);
	if (loginContext == null) {
	    initAsSubject();
	} else {
	    loginContext.login();
	    Subject.doAsPrivileged(
		loginContext.getSubject(),
		new PrivilegedExceptionAction() {
		    public Object run() throws Exception {
			initAsSubject();
			return null;
		    }
		},
		null);
	}
    }

    /**
     * Initializes the server, assuming that the appropriate subject is in
     * effect.
     */
    protected void initAsSubject() throws Exception {
	/* Export the server */
	Exporter exporter = getExporter();
	serverProxy = (StarTask) exporter.export(this);

	/* Create the smart proxy */
	Proxy smartProxy = Proxy.create(serverProxy);

	/* Get the discovery manager, for discovering lookup services */
	DiscoveryManagement discoveryManager;
	try {
	    discoveryManager = (DiscoveryManagement) config.getEntry(
		"uk.ac.starlink.startask.StarTaskServer", "discoveryManager",
		DiscoveryManagement.class);
	} catch (NoSuchEntryException e) {
            /* Use the public group */
	    discoveryManager = new LookupDiscovery(
		new String[] { "" }, config);
	}

	/* Get the join manager, for joining lookup services */
	JoinManager joinManager =
	    new JoinManager(smartProxy, null /* attrSets */, getServiceID(),
			    discoveryManager, null /* leaseMgr */, config);
    }

    /**
     * Returns the exporter for exporting the server.
     *
     * @throws ConfigurationException if a problem occurs getting the exporter
     *	       from the configuration
     * @throws RemoteException if a remote communication problem occurs
     */
    protected Exporter getExporter()
	throws ConfigurationException, RemoteException
    {
	return (Exporter) config.getEntry(
	    "uk.ac.starlink.startask.StarTaskServer", "exporter", Exporter.class,
	    new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
				  new BasicILFactory()));
    }

    /** Returns the service ID for this server. */
    protected ServiceID getServiceID() {
	return createServiceID();
    }

    /** Creates a new service ID. */
    protected static ServiceID createServiceID() {
	Uuid uuid = UuidFactory.generate();
	return new ServiceID(
	    uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    /** Run a Starlink task directly
    */
    public TaskReply runTask( String pkg, String task, String[] params )
     throws Exception {
      StarTaskRequest str =
       new StarTaskRequest( new StarTaskRequestId(), pkg, task, params );         
	return str.runTask();
    }
    /** Run a Starlink task via the {@link ShellRunner}
    */
    public TaskReply shellRunTask( String pkg, String task, String[] params )
     throws Exception {
      StarTaskRequest str
       = new StarTaskRequest( new StarTaskRequestId(), pkg, task, params );         
	return str.shellRunTask();
    }

    /**
     * Implement the ServerProxyTrust interface to provide a verifier for
     * secure smart proxies.
     */
    public TrustVerifier getProxyVerifier() {
	return new Proxy.Verifier(serverProxy);
    }

    /**
     * Returns a proxy object for this remote object.
     *
     * @return our proxy
     */
    public Object getProxy() {
        return serverProxy;
    }

}
