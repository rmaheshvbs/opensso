/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SMDataLayer.java,v 1.5 2007-04-09 23:20:22 goodearth Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.sm.ldap;

import java.util.HashMap;
import netscape.ldap.LDAPBind;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSearchConstraints;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerGroup;
import com.iplanet.services.ldap.ServerInstance;

import com.sun.identity.common.LDAPConnectionPool;

/**
 * SMDataLayer (A PACKAGE SCOPE CLASS) to access LDAP or other database
 * 
 * TODO: 1. Needs to subclass and isolate the current implementation of
 * DataLayer as DSLayer for ldap specific operations 2. Improvements needed for
 * _ldapPool: destroy(), initial bind user, tunning for MIN and MAX initial
 * settings etc 3. May choose to extend implementation of _ldapPool from
 * LdapConnectionPool so that there is load balance between connections.Also
 * _ldapPool may be implemented with a HashTable of (host,port) for multiple
 * pools of connections for mulitple (host,port) to DS servers instead of single
 * host and port.
 * 
 */
class SMDataLayer {

    /**
     * Static section to retrieve the debug object.
     */
    private static Debug debug;

    /**
     * Default maximum connections if none is defined in configuration
     */
    static final int MAX_CONN = 20;

    /**
     * Default maximum backlog queue size
     */
    static final int MAX_BACKLOG = 100;

    static final String LDAP_MAXBACKLOG = "maxbacklog";

    static final String LDAP_RELEASECONNBEFORESEARCH = 
        "releaseconnectionbeforesearchcompletes";

    static final String LDAP_REFERRAL = "referral";

    /**
     * SMDataLayer constructor
     */
    private SMDataLayer() {
        initLdapPool();
    }

    /**
     * create the singleton SMDataLayer object if it doesn't exist already.
     */
    protected synchronized static SMDataLayer getInstance() {
        // Obtain the Debug instance.
        debug = Debug.getInstance("amSMSLdap");

        // Make sure only one instance of this class is created.
        if (m_instance == null) {
            m_instance = new SMDataLayer();
        }
        return m_instance;
    }

    /**
     * Get connection from pool, not through LDAPProxy. Reauthenticate if
     * necessary
     * 
     * @return connection that is available to use
     */
    protected LDAPConnection getConnection() {
        if (_ldapPool == null)
            return null;

        if (debug.messageEnabled()) {
            debug.message("SMDataLayer:getConnection()-"
                    + "Invoking _ldapPool.getConnection()");
        }
        LDAPConnection conn = _ldapPool.getConnection();
        if (debug.messageEnabled()) {
            debug.message("SMDataLayer:getConnection()-Got Connection : "
                    + conn);
        }

        return conn;
    }

    /**
     * Just call the pool method to release the connection so that the given
     * connection is free for others to use
     * 
     * @param conn
     *            connection in the pool to be released for others to use
     */
    protected void releaseConnection(LDAPConnection conn) {
        if (_ldapPool == null || conn == null)
            return;

        // reset the original constraints
        // TODO: check with ldapjdk and see if this is appropriate
        // to restore the default constraints.
        //
        conn.setSearchConstraints(_defaultSearchConstraints);

        // A soft close on the connection.
        // Returns the connection to the pool and make it available.
        if (debug.messageEnabled()) {
            debug.message("SMDataLayer:releaseConnection()-"
                    + "Invoking _ldapPool.close(conn) : " + conn);
        }
        _ldapPool.close(conn);
        if (debug.messageEnabled()) {
            debug.message("SMDataLayer:releaseConnection()-"
                    + "Released Connection : " + conn);
        }
    }

    /**
     * Initialize the pool shared by all SMDataLayer object(s).
     * 
     * @param host
     *            ldaphost to init the pool from
     * @param port
     *            ldapport to init the pool from
     */
    private synchronized void initLdapPool() {
        // Dont' do anything if pool is already initialized
        if (_ldapPool != null)
            return;

        // Initialize the pool with minimum and maximum connections settings
        // retrieved from configuration
        ServerInstance svrCfg = null;
        String hostName = null;
        HashMap connOptions = new HashMap();

        try {
            DSConfigMgr dsCfg = DSConfigMgr.getDSConfigMgr();
            hostName = dsCfg.getHostName("default");

            // Get "sms" ServerGroup if present
            ServerGroup sg = dsCfg.getServerGroup("sms");
            if (sg != null) {
                _trialConn = dsCfg.getNewConnection("sms",
                        LDAPUser.Type.AUTH_ADMIN);
                svrCfg = sg.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
            } else {
                _trialConn = dsCfg.getNewAdminConnection();
                svrCfg = dsCfg.getServerInstance(LDAPUser.Type.AUTH_ADMIN);
            }
            if (svrCfg == null) {
                debug.error("SMDataLayer:initLdapPool()-"
                        + "Error getting server config.");
            }
        } catch (LDAPServiceException ex) {
            debug.error("SMDataLayer:initLdapPool()-"
                    + "Error initializing connection pool " + ex.getMessage());
            ex.printStackTrace();
        }

        int poolMin = svrCfg.getMinConnections();
        int poolMax = svrCfg.getMaxConnections();
        int maxBackLog = svrCfg.getIntValue(LDAP_MAXBACKLOG, MAX_BACKLOG);
        m_releaseConnectionBeforeSearchCompletes = svrCfg.getBooleanValue(
                LDAP_RELEASECONNBEFORESEARCH, false);
        boolean referrals = svrCfg.getBooleanValue(LDAP_REFERRAL, true);

        if (debug.messageEnabled()) {
            debug.message("SMDataLayer:initLdapPool()-"
                    + "Creating ldap connection pool with :");
            debug.message("SMDataLayer:initLdapPool()-poolMin : " + poolMin);
            debug.message("SMDataLayer:initLdapPool()-poolMax : " + poolMax);
            debug.message("SMDataLayer:getConnection()-maxBackLog : "
                    + maxBackLog);
        }

        try {
            // establish one good connection before the pool
            _trialConn.setOption(LDAPConnection.MAXBACKLOG, new Integer(
                    maxBackLog));
            _trialConn.setOption(LDAPConnection.REFERRALS, Boolean.valueOf(
                    referrals));

            // Default rebind method is to provide the same authentication
            // in the rebind to the server being referred.
            LDAPBind defaultBinder = new LDAPBind() {
                public void bind(LDAPConnection ld) throws LDAPException {
                    // There is possibly a bug in the ldapjdk that the passed in
                    // ld is not carrying the original authentication dn and pwd
                    // Hence, we have to kludge here using the one connection
                    // that we know about:
                    // the connection that we use to initialize the connection
                    // pool.
                    // TODO: need to investigate
                    //
                    String dn = _trialConn.getAuthenticationDN();
                    String pwd = _trialConn.getAuthenticationPassword();
                    String newhost = ld.getHost();
                    int newport = ld.getPort();
                    ld.connect(3, newhost, newport, dn, pwd);
                }
            };
            _trialConn.setOption(LDAPConnection.BIND, defaultBinder);

            // remember the original search constraints
            _defaultSearchConstraints = _trialConn.getSearchConstraints();

            // Construct the pool by cloning the successful connection
            // Set the default options too for failover and fallback features.

            connOptions.put("maxbacklog", new Integer(maxBackLog));
            connOptions.put("referrals", new Boolean(referrals));
            connOptions.put("searchconstraints", _defaultSearchConstraints);

            _ldapPool = new LDAPConnectionPool("SMS", poolMin, poolMax,
                hostName, 389, _trialConn.getAuthenticationDN(),
                _trialConn.getAuthenticationPassword(), connOptions);

        } catch (LDAPException e) {
            debug.error("SMDataLayer:initLdapPool()-"
                    + "Exception in SMDataLayer.initLdapPool:", e);
            e.printStackTrace();

        }
    }

    static private LDAPConnectionPool _ldapPool = null;

    static private LDAPConnection _trialConn = null;

    static private LDAPSearchConstraints _defaultSearchConstraints = null;

    static private SMDataLayer m_instance = null;

    private boolean m_releaseConnectionBeforeSearchCompletes = false;
}
