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
 * $Id: LDAPv3Repo.java,v 1.3 2006-01-12 18:06:11 kenwho Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.idm.plugins.ldapv3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import netscape.ldap.LDAPAttribute;
import netscape.ldap.LDAPAttributeSet;
import netscape.ldap.LDAPCache;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPControl;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPModification;
import netscape.ldap.LDAPModificationSet;
import netscape.ldap.LDAPObjectClassSchema;
import netscape.ldap.LDAPRebind;
import netscape.ldap.LDAPRebindAuth;
import netscape.ldap.LDAPReferralException;
import netscape.ldap.LDAPSchema;
import netscape.ldap.LDAPSearchConstraints;
import netscape.ldap.LDAPSearchResults;
import netscape.ldap.LDAPUrl;
import netscape.ldap.LDAPv2;
import netscape.ldap.LDAPv3;
import netscape.ldap.controls.LDAPPasswordExpiredControl;
import netscape.ldap.controls.LDAPPasswordExpiringControl;
import netscape.ldap.controls.LDAPPersistSearchControl;
import netscape.ldap.factory.JSSESocketFactory;
import netscape.ldap.util.ConnectionPool;
import netscape.ldap.util.DN;

import com.iplanet.am.sdk.AMCommonUtils;
import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.am.sdk.IdRepoListener;
import com.iplanet.am.util.Debug;
import com.iplanet.services.naming.ServerEntryNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoFatalException;
import com.sun.identity.idm.IdRepoUnsupportedOpException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.RepoSearchResults;
import com.sun.identity.jaxrpc.SOAPClient;
import com.sun.identity.sm.SchemaType;

public class LDAPv3Repo extends IdRepo {

    private Map supportedOps = new HashMap();

    // config is part of IdRepo.java superclass and
    // set in superclass's initialize method.
    // private Map configMap = new AMHashMap();
    private Map myConfigMap = null;

    private Map myServiceMap = null;

    private IdRepoListener myListener = null;

    private static SOAPClient mySoapClient = new SOAPClient("dummy");

    private String ldapServerName = null;

    private String firstHostAndPort = "";

    private int version = 3;

    // password control  states
    private final static int NO_PASSWORD_CONTROLS = 0;

    private final static int PASSWORD_EXPIRED = -1;

    private String orgDN = "";

    private Debug debug;

    private ConnectionPool connPool;

    private int connNumRetry = 3;

    private int connRetryInterval = 1000;

    private int timeLimit = 5000;

    private int defaultMaxResults = 100;

    private int roleSearchScope = LDAPv2.SCOPE_SUB;

    private String userSearchFilter = null;

    private String groupSearchFilter = null;

    private String roleSearchFilter = 
	"(&(objectclass=ldapsubentry)(objectclass=nsmanagedroledefinition))";

    private String filterroleSearchFilter =
	"(&(objectclass=ldapsubentry)(objectclass=nsfilteredroledefinition))";

    private String agentSearchFilter = null;
	
    private String userSearchNamingAttr = null;

    private String agentSearchNamingAttr = null;

    private String groupSearchNamingAttr = null;

    private String roleSearchNamingAttr = null;

    private String peopleCtnrNamingAttr = null;

    private String agentCtnrNamingAttr = null;

    private String groupCtnrNamingAttr = null;

    private String peopleCtnrValue = null;

    private String agentCtnrValue = null;

    private String groupCtnrValue = null;

    private String nsRoleAttr = null;

    private String nsRoleDNAttr = null;

    private String nsRoleFilterAttr = null;

    private String memberOfAttr = null;

    private String uniqueMemberAttr = null;

    private String memberURLAttr = null;

    private String isActiveAttrName = null;

    private boolean alwaysActive = false;

    private Set filterroleObjClassSet = null;

    private Set roleObjClassSet = null;

    private Set groupObjClassSet = null;

    private Set userObjClassSet = null;

    private Set agentObjClassSet = null;

    private Map createUserAttrMap = null;

    private Set userAtttributesAllowed = null;

    private Set groupAtttributesAllowed = null;

    private Set agentAtttributesAllowed = null;

    private Set filteredroleAtttributesAllowed = null;

    private Set userSpecifiedOpsSet = null;

    private Set authenticatableSet = null;

    private boolean authenticationEnabled = false;

    private String authNamingAttr = null;

    private boolean cacheEnabled = false;

    private long cacheTTL = 600; // in seconds

    private long cacheSize = 10240; // in bytes

    private LDAPCache ldapCache = null;

    private final int MIN_CONNECTION_POOL_SIZE = 1;

    private final int MAX_CONNECTION_POOL_SIZE = 10;

    private final int DEFAULTPORT = 389;

    // access to the _eventsMgr and _eventsMgr needs to be sync.
    protected static Hashtable _eventsMgr = new Hashtable();

    protected static Hashtable _numRequest = new Hashtable();

    private boolean hasListener = false;

    private String dsType = "";

    static final String LDAP_OBJECT_CLASS = "objectclass";

    static final String LDAP_SCOPE_BASE = "SCOPE_BASE";

    static final String LDAP_SCOPE_ONE = "SCOPE_ONE";

    static final String LDAP_SCOPE_SUB = "SCOPE_SUB";

    private static final String LDAPv3Config_LDAPV3GENERIC =
	"sun-idrepo-ldapv3-ldapv3Generic";

    private static final String LDAPv3Config_LDAPV3AMDS =
	"sun-idrepo-ldapv3-ldapv3AMDS";

    private static final String LDAPv3Config_LDAPV3AD =
	"sun-idrepo-ldapv3-ldapv3AD";

    private static final String LDAPv3Config_LDAP_SERVER = 
        "sun-idrepo-ldapv3-config-ldap-server";

    private static final String LDAPv3Config_LDAP_PORT = 
        "sun-idrepo-ldapv3-config-ldap-port";

    private static final String LDAPv3Config_AUTHID =
        "sun-idrepo-ldapv3-config-authid";

    private static final String LDAPv3Config_AUTHPW = 
        "sun-idrepo-ldapv3-config-authpw";

    private static final String LDAPv3Config_LDAP_SSL_ENABLED = 
        "sun-idrepo-ldapv3-config-ssl-enabled";

    private static final String LDAPv3Config_LDAP_CONNECTION_POOL_MIN_SIZE = 
        "sun-idrepo-ldapv3-config-connection_pool_min_size";

    private static final String LDAPv3Config_LDAP_CONNECTION_POOL_MAX_SIZE = 
        "sun-idrepo-ldapv3-config-connection_pool_max_size";

    private static final String LDAPv3Config_ORGANIZATION_NAME = 
        "sun-idrepo-ldapv3-config-organization_name";

    private static final String LDAPv3Config_LDAP_GROUP_SEARCH_FILTER =
"sun-idrepo-ldapv3-config-groups-search-filter";

    private static final String LDAPv3Config_LDAP_USERS_SEARCH_FILTER =
        "sun-idrepo-ldapv3-config-users-search-filter";

    private static final String LDAPv3Config_LDAP_ROLES_SEARCH_FILTER =
        "sun-idrepo-ldapv3-config-roles-search-filter";

    private static final String LDAPv3Config_LDAP_AGENT_SEARCH_FILTER =
        "sun-idrepo-ldapv3-config-agent-search-filter";

    private static final String LDAPv3Config_LDAP_ROLES_SEARCH_ATTRIBUTE =
        "sun-idrepo-ldapv3-config-roles-search-attribute";

    private static final String LDAPv3Config_LDAP_GROUPS_SEARCH_ATTRIBUTE = 
        "sun-idrepo-ldapv3-config-groups-search-attribute";

    private static final String LDAPv3Config_LDAP_USERS_SEARCH_ATTRIBUTE = 
        "sun-idrepo-ldapv3-config-users-search-attribute";

    private static final String LDAPv3Config_LDAP_AGENT_SEARCH_ATTRIBUTE =
        "sun-idrepo-ldapv3-config-agent-search-attribute";

    private static final String LDAPv3Config_LDAP_ROLES_SEARCH_SCOPE = 
        "sun-idrepo-ldapv3-config-role-search-scope";

    private static final String LDAPv3Config_LDAP_GROUP_CONTAINER_NAME = 
        "sun-idrepo-ldapv3-config-group-container-name";

    private static final String LDAPv3Config_LDAP_AGENT_CONTAINER_NAME = 
        "sun-idrepo-ldapv3-config-agent-container-name";

    private static final String LDAPv3Config_LDAP_PEOPLE_CONTAINER_NAME = 
        "sun-idrepo-ldapv3-config-people-container-name";

    private static final String LDAPv3Config_LDAP_GROUP_CONTAINER_VALUE =
        "sun-idrepo-ldapv3-config-group-container-value";

    private static final String LDAPv3Config_LDAP_PEOPLE_CONTAINER_VALUE = 
        "sun-idrepo-ldapv3-config-people-container-value";

    private static final String LDAPv3Config_LDAP_AGENT_CONTAINER_VALUE =
        "sun-idrepo-ldapv3-config-agent-container-value";

    private static final String LDAPv3Config_LDAP_TIME_LIMIT = 
        "sun-idrepo-ldapv3-config-time-limit";

    private static final String LDAPv3Config_LDAP_MAX_RESULT = 
        "sun-idrepo-ldapv3-config-max-result";

    private static final String LDAPv3Config_REFERRALS = 
        "sun-idrepo-ldapv3-config-referrals";

    private static final String LDAPv3Config_ROLE_OBJECT_CLASS = 
        "sun-idrepo-ldapv3-config-role-objectclass";

    private static final String LDAPv3Config_FILTERROLE_OBJECT_CLASS =
	"sun-idrepo-ldapv3-config-filterrole-objectclass";

    private static final String LDAPv3Config_GROUP_OBJECT_CLASS = 
        "sun-idrepo-ldapv3-config-group-objectclass";

    private static final String LDAPv3Config_USER_OBJECT_CLASS = 
        "sun-idrepo-ldapv3-config-user-objectclass";

    private static final String LDAPv3Config_AGENT_OBJECT_CLASS = 
        "sun-idrepo-ldapv3-config-agent-objectclass";

    private static final String LDAPv3Config_GROUP_ATTR =
        "sun-idrepo-ldapv3-config-group-attributes";

    private static final String LDAPv3Config_USER_ATTR = 
        "sun-idrepo-ldapv3-config-user-attributes";

    private static final String LDAPv3Config_AGENT_ATTR = 
        "sun-idrepo-ldapv3-config-agent-attributes";

    private static final String LDAPv3Config_FILTERROLE_ATTR =
	"sun-idrepo-ldapv3-config-filterrole-attributes";

    private static final String LDAPv3Config_NSROLE = 
        "sun-idrepo-ldapv3-config-nsrole";

    private static final String LDAPv3Config_NSROLEDN = 
        "sun-idrepo-ldapv3-config-nsroledn";

    private static final String LDAPv3Config_NSROLEFILTER = 
        "sun-idrepo-ldapv3-config-nsrolefilter";

    private static final String LDAPv3Config_MEMBEROF =
        "sun-idrepo-ldapv3-config-memberof";

    private static final String LDAPv3Config_UNIQUEMEMBER = 
        "sun-idrepo-ldapv3-config-uniquemember";

    private static final String LDAPv3Config_MEMBERURL =
        "sun-idrepo-ldapv3-config-memberurl";

    private static final String LDAPv3Config_LDAP_IDLETIMEOUT = 
        "sun-idrepo-ldapv3-config-idletimeout";

    private static final String LDAPv3Config_LDAP_PSEARCHBASE =
        "sun-idrepo-ldapv3-config-psearchbase";

    private static final String LDAPv3Config_LDAP_ISACTIVEATTRNAME =
        "sun-idrepo-ldapv3-config-isactive";

    private static final String LDAPv3Config_LDAP_CREATEUSERMAPPING =
        "sun-idrepo-ldapv3-config-createuser-attr-mapping";

    private static final String LDAPv3Config_LDAP_AUTHENABLED =
	"sun-idrepo-ldapv3-config-auth-enabled";

    private static final String LDAPv3Config_LDAP_AUTHENTICATABLE =
	"sun-idrepo-ldapv3-config-authenticatable-type";

    private static final String LDAPv3Config_LDAP_AUTHNAMING =
	"sun-idrepo-ldapv3-config-auth-naming-attr";

    private static final String LDAPv3Config_LDAP_CACHEENABLED =
	"sun-idrepo-ldapv3-config-cache-enabled";

    private static final String LDAPv3Config_LDAP_CACHETTL =
	"sun-idrepo-ldapv3-config-cache-ttl";

    private static final String LDAPv3Config_LDAP_CACHESIZE =
	"sun-idrepo-ldapv3-config-cache-size";

    private static SSOToken internalToken = null;

    private static final String SCHEMA_BUG_PROPERTY = 
        "com.netscape.ldap.schema.quoting";

    private static final String VAL_STANDARD = "standard";

    private static final String CLASS_NAME = 
        "com.sun.identity.idm.plugins.ldapv3.LDAPv3Repo";

    public LDAPv3Repo() {
        debug = Debug.getInstance("LDAPv3Repo");
        loadSupportedOps();
    }

    private String getLDAPServerName(Map configParams) {
        String siteID = "";
        String serverID = "";
        try {
            serverID = WebtopNaming.getAMServerID();
            siteID = WebtopNaming.getSiteID(serverID);
        } catch (ServerEntryNotFoundException senf) {
            if (debug.messageEnabled()) {
                debug.warning("ServerEntryNotFoundException error: siteID="
                        + siteID + "; serverID=" + serverID);
                senf.printStackTrace();
            }
        }

        Set ldapServerSet = new HashSet((Set) configParams
                .get(LDAPv3Config_LDAP_SERVER));
        String ldapServer = "";
        String endOfList = "";
        // put ldapServer from list into a string seperated by space for
        // failover purposes. LDAPConnection will automatcially handle failover.
        // hostname:portnumber | severID | siteID
        // serverID is optional. if omitted, it means any(don't care).
        // siteID is optional. if omitted, it means any(don't care).
        // host whose siteID and serverID matches webtop naming's serverid and
        // siteid are put in the front of the list as well as those host which
        // did not specify a siteid/serverid because of backward compatibliity.
        // otherwise it will go to the end of the list.
        Iterator it = ldapServerSet.iterator();
        while (it.hasNext()) {
            String curr = (String) it.next();
            StringTokenizer tk = new StringTokenizer(curr, "|");
            String hostAndPort = tk.nextToken().trim();
            String hostServerID = "";
            if (tk.hasMoreTokens()) {
                hostServerID = tk.nextToken();
                hostServerID = hostServerID.trim();
            }
            String hostSiteID = "";
            if (tk.hasMoreTokens()) {
                hostSiteID = tk.nextToken();
                hostSiteID = hostSiteID.trim();
            }

            if (hostSiteID.length() == 0) {
                hostSiteID = siteID;
            }
            if (hostServerID.length() == 0) {
                hostServerID = serverID;
            }
            if (siteID.equals(hostSiteID) && serverID.equals(hostServerID)) {
                if (ldapServer.length() == 0) {
                    ldapServer = hostAndPort;
                } else {
                    ldapServer = ldapServer + " " + hostAndPort;
                }
            } else {
                if (endOfList.length() == 0) {
                    endOfList = hostAndPort;
                } else {
                    endOfList = endOfList + " " + hostAndPort;
                }
            }
        } // end of while
        if (ldapServer.length() == 0) {
            ldapServer = endOfList;
        } else {
            if (endOfList.length() != 0) {
                ldapServer = ldapServer + " " + endOfList;
            }
        }

        if (debug.messageEnabled()) {
            debug.message("getLDAPServerName:LDAPv3Config_LDAP_SERVER"
                    + "; ldapServer:" + ldapServer + "; endOfList:" + endOfList
                    + "; siteID:" + siteID + "; serverID:" + serverID
                    + "; ldapServerSet:" + ldapServerSet);
        }
        return ldapServer;
    }

    private void initConnectionPool(Map configParams) {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: initConnectionPool ");
        }

        if (ldapServerName == null) {
            ldapServerName = getLDAPServerName(configParams);
        }
        // ldapServerName is list of server names seperated by sapce for
        // failover purposes. LDAPConnection will automatcially handle failover.

        // port will not be used since ldapserver is in the following format:
        // nameOfLDAPhost:portNumber.
        int ldapPort = DEFAULTPORT;
        String authid = getPropertyStringValue(configParams,
                LDAPv3Config_AUTHID);
        String authpw = getPropertyStringValue(configParams,
                LDAPv3Config_AUTHPW);
        String referrals = getPropertyStringValue(configParams,
                LDAPv3Config_REFERRALS);
        String ssl = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_SSL_ENABLED);

        int minPoolSize = getPropertyIntValue(configParams,
                LDAPv3Config_LDAP_CONNECTION_POOL_MIN_SIZE,
                MIN_CONNECTION_POOL_SIZE);
        int maxPoolSize = getPropertyIntValue(configParams,
                LDAPv3Config_LDAP_CONNECTION_POOL_MAX_SIZE,
                MAX_CONNECTION_POOL_SIZE);
        if (minPoolSize < 1) {
            minPoolSize = MIN_CONNECTION_POOL_SIZE;
        }
        if (maxPoolSize < 1) {
            maxPoolSize = MAX_CONNECTION_POOL_SIZE;
        }

        LDAPConnection ldc = null;
        try {
            if (ssl != null && ssl.equalsIgnoreCase("true")) {
                ldc = new LDAPConnection(new JSSESocketFactory(null));
            } else {
                ldc = new LDAPConnection();
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: initConnectionPool "
                        + "LDAPConnection failed", e);
            }
            connPool = null;
        }

        try {
            ldc.setOption(LDAPv3.PROTOCOL_VERSION, new Integer(3));
            ldc.setOption(LDAPv2.REFERRALS, new Boolean(referrals));
            ldc.setOption(LDAPv2.TIMELIMIT, new Integer(timeLimit));
            ldc.setOption(LDAPv2.SIZELIMIT, new Integer(defaultMaxResults));
            setDefaultReferralCredentials(ldc);
            LDAPSearchConstraints constraints = ldc.getSearchConstraints();
            constraints.setMaxResults(defaultMaxResults);
            constraints.setServerTimeLimit(timeLimit);
            ldc.setSearchConstraints(constraints);
	    if (cacheEnabled) {
		ldapCache = new LDAPCache(cacheTTL, cacheSize);
		ldc.setCache(ldapCache);
		if (debug.messageEnabled()) {
		    debug.message("LDAPv3Repo: cacheTTL=" + cacheTTL
			+ "; cacheSize=" + cacheSize );
		}
	    }
        } catch (LDAPException lde) {
            int resultCode = lde.getLDAPResultCode();
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: initConnectionPool setOption " +
                        "failed: " + resultCode);
            }
        }

        try {
            ldc.connect(ldapServerName, ldapPort, authid, authpw);
            connPool = new ConnectionPool(minPoolSize, maxPoolSize, ldc);
        } catch (LDAPException lde) {
            int resultCode = lde.getLDAPResultCode();
            debug.error("LDAPv3Repo: initConnectionPool ConnectionPool failed: "
                           + resultCode + "; ldapServerName:" + ldapServerName);
            connPool = null;
        }
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: exit initConnectionPool ");
        }
    }

    protected void setDefaultReferralCredentials(LDAPConnection conn) {
        final LDAPConnection mConn = conn;
        LDAPRebind reBind = new LDAPRebind() {
            public LDAPRebindAuth getRebindAuthentication(String host, int port)
            {
                return new LDAPRebindAuth(mConn.getAuthenticationDN(), mConn
                        .getAuthenticationPassword());
            }
        };
        LDAPSearchConstraints cons = conn.getSearchConstraints();
        cons.setRebindProc(reBind);
        conn.setSearchConstraints(cons);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#initialize(java.util.Map)
     */
    public void initialize(Map configParams) {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: Initializing configuration()");
        }
        super.initialize(configParams);
        myConfigMap = configParams;
        String myServiceStr = getPropertyStringValue(configParams,
                IdConstants.SERVICE_ATTRS);
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: initialize: myServiceStr: "
                    + myServiceStr);
        }
        if ((myServiceStr != null) && (myServiceStr.length() != 0)) {
            myServiceMap = new HashMap(mySoapClient.decodeMap(myServiceStr));
        } else {
            myServiceMap = new HashMap();
        }
        if (debug.messageEnabled()) {
            if (myServiceMap != null) {
                debug.message("LDAPv3Repo: initialize: myServiceMap: "
                        + myServiceMap);
            } else {
                debug.message("LDAPv3Repo: initialize: myServiceMap = null");
            }
        }

	// find out which configuration/DS this is: AMDS, AD, generic DS
	setDSType(configParams);

	// get the organization name
	orgDN = getPropertyStringValue(configParams,
		LDAPv3Config_ORGANIZATION_NAME);

        timeLimit = getPropertyIntValue(configParams,
                LDAPv3Config_LDAP_TIME_LIMIT, timeLimit) * 1000;
        defaultMaxResults = getPropertyIntValue(configParams,
                LDAPv3Config_LDAP_MAX_RESULT, defaultMaxResults);

        cacheEnabled = getPropertyBooleanValue(configParams,
		LDAPv3Config_LDAP_CACHEENABLED);

        cacheTTL = getPropertyIntValue(configParams,
		LDAPv3Config_LDAP_CACHETTL, 600);  // in seconds

        cacheSize = getPropertyIntValue(configParams,
		LDAPv3Config_LDAP_CACHESIZE, 10240); // in bytes

        String scope = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_ROLES_SEARCH_SCOPE);
        if (scope != null && scope.equalsIgnoreCase(LDAP_SCOPE_BASE)) {
            roleSearchScope = LDAPv2.SCOPE_BASE;
        } else if (scope != null && scope.equalsIgnoreCase(LDAP_SCOPE_ONE)) {
            roleSearchScope = LDAPv2.SCOPE_ONE;
        } else {
            roleSearchScope = LDAPv2.SCOPE_SUB;
        }

        userSearchFilter = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_USERS_SEARCH_FILTER);
        groupSearchFilter = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_GROUP_SEARCH_FILTER);
        roleSearchFilter = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_ROLES_SEARCH_FILTER);
        agentSearchFilter = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_AGENT_SEARCH_FILTER);
        userSearchNamingAttr = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_USERS_SEARCH_ATTRIBUTE);
        agentSearchNamingAttr = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_AGENT_SEARCH_ATTRIBUTE);
        groupSearchNamingAttr = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_GROUPS_SEARCH_ATTRIBUTE);
        roleSearchNamingAttr = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_ROLES_SEARCH_ATTRIBUTE);
        peopleCtnrNamingAttr = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_PEOPLE_CONTAINER_NAME);
        agentCtnrNamingAttr = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_AGENT_CONTAINER_NAME);
        groupCtnrNamingAttr = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_GROUP_CONTAINER_NAME);
        peopleCtnrValue = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_PEOPLE_CONTAINER_VALUE);
        agentCtnrValue = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_AGENT_CONTAINER_VALUE);
        groupCtnrValue = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_GROUP_CONTAINER_VALUE);

        Set tmpOC = (Set) configParams.get(LDAPv3Config_ROLE_OBJECT_CLASS);
	if (tmpOC == null) {
	    roleObjClassSet = Collections.EMPTY_SET;
	} else {
	    roleObjClassSet = new HashSet((Set) tmpOC);
	}
        
	tmpOC = (Set) configParams.get(LDAPv3Config_FILTERROLE_OBJECT_CLASS);
	if (tmpOC == null) {
	    filterroleObjClassSet = Collections.EMPTY_SET;
	} else {
	    filterroleObjClassSet = new HashSet((Set) tmpOC);
	}

	tmpOC = (Set) configParams.get(LDAPv3Config_GROUP_OBJECT_CLASS);
	if (tmpOC == null) {
	    groupObjClassSet = Collections.EMPTY_SET;
        } else {
	    groupObjClassSet = new HashSet((Set) tmpOC);
        }

	tmpOC = (Set) configParams.get(LDAPv3Config_USER_OBJECT_CLASS);
	if (tmpOC == null) {
	    userObjClassSet = Collections.EMPTY_SET;
	} else {
	    userObjClassSet = new HashSet((Set) tmpOC);
        }

	tmpOC = (Set) configParams.get(LDAPv3Config_AGENT_OBJECT_CLASS);
	if (tmpOC == null) {
	    agentObjClassSet = Collections.EMPTY_SET;
        } else {
	    agentObjClassSet = new HashSet((Set) tmpOC);
        }

	nsRoleAttr = getPropertyStringValue(configParams,
		LDAPv3Config_NSROLE, "nsrole");
        nsRoleDNAttr = getPropertyStringValue(configParams,
		LDAPv3Config_NSROLEDN, "nsRoleDN");
        nsRoleFilterAttr = getPropertyStringValue(configParams,
		LDAPv3Config_NSROLEFILTER, "nsRoleFilter");
        memberOfAttr = getPropertyStringValue(configParams,
                LDAPv3Config_MEMBEROF);
        uniqueMemberAttr = getPropertyStringValue(configParams,
                LDAPv3Config_UNIQUEMEMBER);
        memberURLAttr = getPropertyStringValue(configParams,
                LDAPv3Config_MEMBERURL);
        userAtttributesAllowed = new CaseInsensitiveHashSet();
	Set allowAttr = (Set) configParams.get(LDAPv3Config_USER_ATTR);
	if (allowAttr != null) {
	    userAtttributesAllowed.addAll(allowAttr);
	}
        groupAtttributesAllowed = new CaseInsensitiveHashSet();
	allowAttr = (Set) configParams.get(LDAPv3Config_GROUP_ATTR);
	if (allowAttr != null) {
	    groupAtttributesAllowed.addAll(allowAttr);
        }
        agentAtttributesAllowed = new CaseInsensitiveHashSet();
	allowAttr = (Set) configParams.get(LDAPv3Config_AGENT_ATTR);
	if (allowAttr != null) {
	    agentAtttributesAllowed.addAll(allowAttr);
	}
	filteredroleAtttributesAllowed = new CaseInsensitiveHashSet();
	allowAttr = (Set) configParams.get(LDAPv3Config_FILTERROLE_ATTR);
	if (allowAttr != null) {
	    filteredroleAtttributesAllowed.addAll(allowAttr);
	}
        userSpecifiedOpsSet = new HashSet((Set) configParams
                .get(IdConstants.SUPPORTED_OP));
        parsedUserSpecifiedOps(userSpecifiedOpsSet);
        isActiveAttrName = getPropertyStringValue(configParams,
                LDAPv3Config_LDAP_ISACTIVEATTRNAME);
        if (isActiveAttrName == null || isActiveAttrName.length() == 0) {
            alwaysActive = true;
        }
        createUserAttrMap = getCreateUserAttrMapping(configParams);

	authenticationEnabled = getPropertyBooleanValue(configParams,
		LDAPv3Config_LDAP_AUTHENABLED);

        authNamingAttr = getPropertyStringValue(configParams,
	    LDAPv3Config_LDAP_AUTHNAMING);
        if (authNamingAttr == null) {
	    authNamingAttr = userSearchNamingAttr;
	}

	Set tmpAuthSet = (Set)
	        configParams.get(LDAPv3Config_LDAP_AUTHENTICATABLE);
        if (tmpAuthSet == null) {
	    authenticatableSet = Collections.EMPTY_SET;
	} else {
	    authenticatableSet = new HashSet(tmpAuthSet);
	}

        initConnectionPool(configParams);

	if (debug.messageEnabled()) {
	    debug.message("    userObjClassSet: " + userObjClassSet);
	    debug.message("    agentObjClassSet: " + agentObjClassSet);
	    debug.message("    groupObjClassSet:" + groupObjClassSet);
	    debug.message("    roleObjClassSet:" + roleObjClassSet);
	    debug.message("    filterroleObjClassSet: "
		    + filterroleObjClassSet);
            debug.message("    userAtttributesAllowed: "
		    + userAtttributesAllowed);
            debug.message("    groupAtttributesAllowed: "
		    + groupAtttributesAllowed);
            debug.message("    agentAtttributesAllowed: "
		    +  agentAtttributesAllowed);
            debug.message("    filteredroleAtttributesAllowed: "
		    +  filteredroleAtttributesAllowed);
            debug.message( "LDAPv3Repo: exit Initializing. "
		    + "timeLimit =" + timeLimit
		    + "; maxResults =" + defaultMaxResults
		    + "; roleSearchScope=" + roleSearchScope
		    + "; orgDN=" + orgDN
		    + "; createUserAttrMap=" + createUserAttrMap);
        }

    }

    public void shutdown() {
        debug.message("LDAPv3Repo: shutdown");
        super.shutdown();
        connPool.destroy();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getSupportedOperations(
     *      com.iplanet.am.sdk.IdType)
     */
    public Set getSupportedOperations(IdType type) {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getSupportedOps on " + type + " called");
            debug.message("  cont LDAPv3Repo: supportedOps Map = "
                    + supportedOps.toString());
        }
        return (Set) supportedOps.get(type);
    }

    /*
     * (non-Javadoc
     * 
     * @see com.sun.identity.idm.IdRepo#getSupportedTypes()
     */

    public Set getSupportedTypes() {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getSupportedTypes: supportedOps.keySet="
                    + supportedOps.keySet());
        }
        return supportedOps.keySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#isActive(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public boolean isActive(SSOToken token, IdType type, String name)
            throws IdRepoUnsupportedOpException, SSOException {
        /*
         * not every ldap has user user status attribute for active/inactive If
         * an attribute is configured, we check for "active/inactive". if the
         * attribute is not configured or attribute is configured but does not
         * exist in user entry or does not contain the word "active" then it is
         * assume active.
         */
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: isActive called: type:" + type
                    + "; name:" + name);
        }
        if (!type.equals(IdType.USER)) {
            Object[] args = { CLASS_NAME, IdOperation.READ.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }

        if (alwaysActive) {
            try {
                boolean found = isExists(token, type, name);
                return found;
            } catch (IdRepoException ide) {
                return false;
            }
        }
        Map attrMap = null;
        HashSet attrNameSet = new HashSet();
        attrNameSet.add(isActiveAttrName);
        try {
            attrMap = getAttributes(token, type, name, attrNameSet);
        } catch (IdRepoException idrepoerr) {
            if (debug.messageEnabled()) {
                debug.message("  LDAPv3Repo: isActive idrepoerr=" + idrepoerr);
            }
            return (false); // we can't determine user status.
        }

        if (debug.messageEnabled()) {
            debug.message("  LDAPv3Repo: isActive attrMap=" + attrMap);
        }
        Set attrSet = (Set) (attrMap.get(isActiveAttrName));
        String attrValue = null;

        if ((attrSet != null) && (attrSet.size() == 1)) {
            attrValue = (String) attrSet.iterator().next();
            return !attrValue.equalsIgnoreCase("inactive");
        } else {
            return (true);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#isExists(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String)
     */
    public boolean isExists(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: isExists called " + type + ": " + name);
        }

        String dn;
	LDAPEntry foundEntry = null;
        try {
            dn = getDN(type, name);
        } catch (IdRepoUnsupportedOpException ide) {
            return false;
        } catch (IdRepoException idrepoerr) {
            return false;
        }

        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        try {
            foundEntry = ld.read(dn);
        } catch (LDAPException e) {
            switch (e.getLDAPResultCode()) {
            case LDAPException.NO_SUCH_OBJECT:
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: isExists: The specified entry " +
                            "does not exist.");
                }
                break;
            case LDAPException.LDAP_PARTIAL_RESULTS:
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: isExists: Entry served by a " +
                            "different LDAP server.");
                }
                break;
            case LDAPException.INSUFFICIENT_ACCESS_RIGHTS:
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: isExists: You do not have the " +
                            "access rights to perform this operation.");
                }
                break;
            default:
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: isExists: Error number: "
                            + e.getLDAPResultCode());
                    debug.message("LDAPv3Repo: isExists: "
                            + "Could not read the specified entry.");
                }
                break;
            }
            connPool.close(ld);
            int resultCode = e.getLDAPResultCode();
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82))
            {
                Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "306",
                        args);
            }
            return false;
        }
        connPool.close(ld);
        return true;
    }

    public synchronized void removeListener() {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: removeListener called ");
        }

        // keep a count with eventsMgr.ldapServerName of number of listener.
        // when the count reaches 0. remove the ldapServerName entry.
        // see if we already have an event service for this server.
        if (ldapServerName == null) {
            ldapServerName = getLDAPServerName(myConfigMap);
        }
        if (ldapServerName == null) {
            debug.error("LDAPv3Repo: removeListener failed. missing ldap " +
                    "server name.");
        }

        LDAPv3EventService eventService = (LDAPv3EventService) _eventsMgr
                .get(ldapServerName);
        if (eventService != null) {
            if (hasListener) {
                eventService.removeListener(this);
                Integer requestNum = (Integer) _numRequest.get(ldapServerName);
                if (requestNum != null) {
                    int requestInt = requestNum.intValue();
                    if (requestInt <= 1) {
                        _eventsMgr.remove(ldapServerName);
                        _numRequest.remove(ldapServerName);
                    } else {
                        _numRequest.remove(ldapServerName);
                        _numRequest.put(ldapServerName, new Integer(
                                requestInt - 1));
                    }
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo: removeListener. requestInt="
                                        + requestInt);
                    }
                }
            } else { // listener was not added
                Integer requestNum = (Integer) _numRequest.get(ldapServerName);
                if (requestNum == null) {
                    _eventsMgr.remove(ldapServerName);
                }
            }
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#addListener(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.AMObjectListener, java.util.Map)
     */
    public synchronized int addListener(SSOToken token, IdRepoListener listener)
            throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: addListener called"
                    + "  LDAPv3Config_LDAP_SERVER="
                    + configMap.get(LDAPv3Config_LDAP_SERVER)
                    + "; LDAPv3Config_LDAP_PSEARCHBASE="
                    + configMap.get(LDAPv3Config_LDAP_PSEARCHBASE)
                    + "; LDAPv3Config_LDAP_IDLETIMEOUT="
                    + configMap.get(LDAPv3Config_LDAP_IDLETIMEOUT));
        }
        // TODO Auto-generated method stub
        // listener.setConfigMap(configMap);
        myListener = listener;

        // see if we already have an event service for this server.
        if (ldapServerName == null) {
            ldapServerName = getLDAPServerName(myConfigMap);
        }
        if (ldapServerName == null) {
            debug.error("LDAPv3Repo: addListener failed. missing ldap server " +
                    "name.");
            return 0;
        }

        String searchBase = getPropertyStringValue(myConfigMap,
                LDAPv3Config_LDAP_PSEARCHBASE);
        if (ldapServerName == null) {
            debug.error("LDAPv3Repo: addListener failed. "
                    + "missing persistence search base.");
            return 0;
        }

        LDAPv3EventService eventService = (LDAPv3EventService) _eventsMgr
                .get(ldapServerName);
        if (eventService == null) {
            int idleTimeOut = getPropertyIntValue(myConfigMap,
                    LDAPv3Config_LDAP_IDLETIMEOUT, 0);
            try {
                if (idleTimeOut == 0) {
                    eventService = new LDAPv3EventService(myConfigMap,
                            ldapServerName);
                } else {
                    eventService = new LDAPv3EventServicePolling(myConfigMap,
                            ldapServerName);
                }
                _eventsMgr.put(ldapServerName, eventService);
            } catch (LDAPException le) {
                debug.error("LDAPv3Repo: addListener failed. "
                        + "new eventService failed. LDAPException=", le);
                Object[] args = { CLASS_NAME };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "218", args);
            }
        }
        String filter = "(objectclass=*)";
        int op = LDAPPersistSearchControl.ADD | LDAPPersistSearchControl.MODIFY
                | LDAPPersistSearchControl.DELETE
                | LDAPPersistSearchControl.MODDN;
        try {
            eventService.addListener(token, listener, searchBase,
                    LDAPv3.SCOPE_SUB, filter, op, myConfigMap, this,
                    ldapServerName);
            Integer numRequest = (Integer) _numRequest.get(ldapServerName);
            int requestNum;
            if (numRequest == null) {
                requestNum = 1;
            } else {
                requestNum = numRequest.intValue() + 1;
            }
            _numRequest.put(ldapServerName, new Integer(requestNum));
        } catch (IdRepoException idrepoex) {
            debug.error("LDAPv3Repo: addListener failed. persistant search " +
                    "not supported");
        } catch (LDAPException ldapex) {
            debug.error("LDAPv3Repo: addListener failed. "
                    + "eventService.addListener.LDAPException", ldapex);
            Object[] args = { CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "218", args);
        }
        // probably should save the reqID with our listener.
        // once we have our listener we can get at our config listener.getconfig
        // will this id change? what happens it timeout and have to be
        // restarted.
        hasListener = true;
        return 0;
    }

    /*
     * given a Map of attributes(attrMap), return a Map with only attributes
     * that were predefined to be permited or allowed.
     */
    private Map getAllowedAttrs(IdType type, Map attrMap) {
        Set predefinedAttr = Collections.EMPTY_SET;
        if (type.equals(IdType.USER)) {
            predefinedAttr = userAtttributesAllowed;
        } else if (type.equals(IdType.AGENT)) {
            predefinedAttr = agentAtttributesAllowed;
        } else if (type.equals(IdType.GROUP)) {
            predefinedAttr = groupAtttributesAllowed;
        } else if (type.equals(IdType.ROLE)) {
	    return (new HashMap(attrMap));
        } else if (type.equals(IdType.FILTEREDROLE)) {
            predefinedAttr = filteredroleAtttributesAllowed;
        } else {
	    return (new HashMap(attrMap));
	}

        Map allowedAttr = new HashMap();
        Iterator itr = predefinedAttr.iterator();
        while (itr.hasNext()) {
            String attrName = (String) itr.next();
            Set attrNameValue = (Set) attrMap.get(attrName);
            if (attrNameValue != null) {
                allowedAttr.put(attrName, attrNameValue);
            }
        }
        return (allowedAttr);
    }

    private Map addAttrMapping(IdType type, String name, Map attrMap) {
        // add missing required atttr and its default value.
        if (debug.messageEnabled()) {
            debug.message("enter addAttrMapping: createUserAttrMap="
                    + createUserAttrMap);
            prtAttrMap(attrMap);
        }
        if (type.equals(IdType.USER)) {
            Iterator itr = createUserAttrMap.keySet().iterator();
            while (itr.hasNext()) {
                String attrName = (String) itr.next();
                if (!attrMap.containsKey(attrName)) {
                    String mapAttrName = (String) createUserAttrMap
                            .get(attrName);
                    // if the attrname is same as the attrvalue. see
                    // getCreateUserAttrMapping
                    // special case, use the username as the value of the
                    // attribute..
                    if (mapAttrName.equalsIgnoreCase(attrName)) {
                        Set mapAttrValue = new HashSet();
                        mapAttrValue.add(name);
                        attrMap.put(attrName, mapAttrValue);
                    } else {
                        Set mapAttrValue = (Set) attrMap.get(mapAttrName);
                        if (mapAttrValue != null) {
                            attrMap.put(attrName, mapAttrValue);
                        }
                    }
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("exit addAttrMapping: ");
            prtAttrMap(attrMap);
        }
        return attrMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#create(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Map)
     */
    public String create(SSOToken token, IdType type, String name, Map attrMap)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: Create called on " + type + ": " + name);
            prtAttrMap(attrMap);
        }

        String eDN = getDN(type, name);
        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        Set theOC = null;
        if (type.equals(IdType.USER)) {
            theOC = userObjClassSet;
        } else if (type.equals(IdType.AGENT)) {
            theOC = agentObjClassSet;
        } else if (type.equals(IdType.GROUP)) {
            theOC = groupObjClassSet;
        } else if (type.equals(IdType.ROLE)) {
            theOC = roleObjClassSet;
        } else if (type.equals(IdType.FILTEREDROLE)) {
	    theOC = filterroleObjClassSet;
        } else {
            Object[] args = { CLASS_NAME, IdOperation.CREATE.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }

        attrMap = getAllowedAttrs(type, attrMap);
        attrMap = addAttrMapping(type, name, attrMap);

        LDAPAttributeSet ldapAttrSet = new LDAPAttributeSet();
        if (attrMap != null && !attrMap.isEmpty()) {
            // add the default objectclass to the attrMap passed in.
            boolean addedOC = false;
            Map privAttrMap = new HashMap();

            Iterator itr = attrMap.keySet().iterator();
            while (itr.hasNext()) {
                String attrName = (String) itr.next();
                if (attrName.equalsIgnoreCase(LDAP_OBJECT_CLASS)) {
                    Set attrNameValue = ((Set) attrMap.get(attrName));
                    HashSet newNameValue = new HashSet(attrNameValue);
                    newNameValue.addAll(theOC);
                    privAttrMap.put(attrName, newNameValue);
                    addedOC = true;
                } else {
                    Set attrNameValue = (Set) attrMap.get(attrName);
                    // ignore empty set because some servers can't handle it.
                    if (!attrNameValue.isEmpty()) {
                        privAttrMap.put(attrName, attrNameValue);
                    }
                }
            }

            if (!addedOC) { // object class not in attrMap passed in, add it.
                privAttrMap.put(LDAP_OBJECT_CLASS, theOC);
            }

            itr = privAttrMap.keySet().iterator();
            while (itr.hasNext()) {
                String attrName = (String) itr.next();
                Set set = (Set) (privAttrMap.get(attrName));
                String attrValues[] = (set == null ? null : (String[]) set
                        .toArray(new String[set.size()]));
                if (debug.messageEnabled()) {
                    if (attrName.equalsIgnoreCase("userpassword")) {
                        debug.message("    : attrName= " + attrName);
                    } else {
                        debug.message("    : attrName= " + attrName + " set:"
                                + set);
                    }
                }
                ldapAttrSet.add(new LDAPAttribute(attrName, attrValues));
            } // null
        } else {
            String attrValues[] = (theOC == null ? null : (String[]) theOC
                    .toArray(new String[theOC.size()]));
            ldapAttrSet.add(new LDAPAttribute(LDAP_OBJECT_CLASS, attrValues));

        }
        if (debug.messageEnabled()) {
            debug.message("    : before ld.add: eDN=" + eDN);
        }
        try {
            LDAPEntry theEntry = new LDAPEntry(eDN, ldapAttrSet);
            ld.add(theEntry);
            connPool.close(ld);
        } catch (LDAPException lde) {
            debug.error("LDAPv3Repo.create failed. errorCode="
                    + lde.getLDAPResultCode() + "  "
                    + lde.getLDAPErrorMessage());
            if (debug.messageEnabled()) {
		debug.message("LDAPv3Repo.create failed", lde);
            }
            connPool.close(ld);
            int resultCode = lde.getLDAPResultCode();
            Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82))
            {
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "306",
                        args);
            } else {
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "306", args);
            }
        }
        return eDN;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#delete(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String)
     */
    public void delete(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: delete called on " + type + ": " + name);
        }

        String eDN = getDN(type, name);
        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        try {
            ld.delete(eDN);
        } catch (LDAPException lde) {
            int resultCode = lde.getLDAPResultCode();
            String ldeErrMsg = lde.getLDAPErrorMessage();
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: delete, error: " + resultCode
                        + "errmsg=" + ldeErrMsg, lde);
            }
            Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82)) 
            {
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "306",
                        args);
            } else if (resultCode == LDAPException.NO_SUCH_OBJECT) {
                args[0] = CLASS_NAME;
                args[1] = eDN;
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "220", args);
            } else {
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "306", args);
            }
        } finally {
            connPool.close(ld);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Set)
     */
    public Map getAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getAttributes called" + ": " + type
                    + ": " + name + " ; attrName=" + attrNames);
        }

        Map myMap = getAttributes(token, type, name, attrNames, true);
        return myMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Set)
     */
    private Map getAttributes(SSOToken token, IdType type, String name,
            Set attrNames, boolean isString) throws IdRepoException,
            SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getAttributes called" + ": " + type
                    + ": " + name + " ; attrName=" + attrNames);
        }

        Map theAttrMap = new HashMap();
        if (type.equals(IdType.REALM)) {
            if ((attrNames != null) && attrNames.contains("objectclass")) {
                return theAttrMap;
            }
        }

        String dn = getDN(type, name);
        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        LDAPSearchConstraints constraints = ld.getSearchConstraints();
        constraints.setMaxResults(defaultMaxResults);
        constraints.setServerTimeLimit(timeLimit);

        Set predefinedAttr = null;
        if (type.equals(IdType.USER)) {
            predefinedAttr = userAtttributesAllowed;
        } else if (type.equals(IdType.AGENT)) {
            predefinedAttr = agentAtttributesAllowed;
        } else if (type.equals(IdType.GROUP)) {
            predefinedAttr = groupAtttributesAllowed;
        } else if (type.equals(IdType.FILTEREDROLE)) {
	    predefinedAttr = filteredroleAtttributesAllowed;
	}

        if (debug.messageEnabled()) {
            debug.message("  LDAPv3Repo: predefinedAttr=" + predefinedAttr
                    + "; attrNames=" + attrNames);
        }

        try {
            LDAPEntry foundEntry = null;
            if (attrNames == null) {
                foundEntry = ld.read(dn);
            } else {
                if (predefinedAttr != null) {
                    Set allowedAttrNames = new HashSet();
                    Iterator itr = attrNames.iterator();
                    while (itr.hasNext()) {
                        String attrName = (String) itr.next();
                        if (predefinedAttr.contains(attrName)) {
                            allowedAttrNames.add(attrName);
                        }
                    }
                    attrNames = allowedAttrNames;
                }
                if (debug.messageEnabled()) {
                    debug.message("  LDAPv3Repo: before read: attrNames="
                            + attrNames);
                }
                foundEntry = ld.read(dn, (String[]) attrNames
                        .toArray(new String[attrNames.size()]));
            }
            if (foundEntry == null) {
                debug.error("getAttributes: unable to find dn:" + dn
                        + " to retrieve its attributes.");
                connPool.close(ld);
                Object[] args = { CLASS_NAME, dn };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "211", args);
            }
            // convert from LDAPAttributeSet to Map.
            LDAPAttributeSet ldapAttrSet = foundEntry.getAttributeSet();
            int size = ldapAttrSet.size();
            for (int i = 0; i < size; i++) {
                LDAPAttribute ldapAttr = ldapAttrSet.elementAt(i);
                if (ldapAttr != null) {
                    String attrName = ldapAttr.getName();

                    if (debug.messageEnabled()) {
                        debug.message("  LDAPv3Repo: after read: attrName="
                                + attrName);
                    }

                    if ((predefinedAttr != null)
                            && !predefinedAttr.contains(attrName.toLowerCase()))
                    {
                        continue;
                    }
                    Set attrValueSet = new HashSet();
                    if (isString) {
                        Enumeration enumVals = ldapAttr.getStringValues();
                        while ((enumVals != null) && enumVals.hasMoreElements())
                        {
                            String value = (String) enumVals.nextElement();
                            attrValueSet.add(value);
                        }
                        theAttrMap.put(attrName.toLowerCase(), attrValueSet);
                    } else {
                        byte[][] values = ldapAttr.getByteValueArray();
                        theAttrMap.put(attrName, values);
                        if (debug.messageEnabled()) {
                            debug.message("   getAttribute binary: values="
                                    + values);
                        }
                    }
                }
            }
        } catch (LDAPException lde) {
            String ldeErrMsg = lde.getLDAPErrorMessage();
            if (debug.messageEnabled()) {
		debug.warning("LDAPv3Repo.getAttributes failed. errorCode="
			+ lde.getLDAPResultCode() + "  " + ldeErrMsg);
            }
            connPool.close(ld);
            int resultCode = lde.getLDAPResultCode();
            Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82))
            {
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "306",
                        args);
            } else if (resultCode == LDAPException.NO_SUCH_OBJECT) {
                args[0] = CLASS_NAME;
                args[1] = dn;
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "220", args);
            } else {
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "306", args);
            }
        }
        connPool.close(ld);
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getAttributes returns theAttrMap: "
                    + theAttrMap);
        }
        return (theAttrMap);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String)
     */
    public Map getAttributes(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getAttributes called" + ": " + type
                    + ": " + name);
        }

        return (getAttributes(token, type, name, null));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getBinaryAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Set)
     */
    public Map getBinaryAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("getBinaryAttributes: ...");
        }
        return (getAttributes(token, type, name, attrNames, false));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#setBinaryAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map,
     *      boolean)
     */
    public void setBinaryAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd) throws IdRepoException, SSOException 
    {

        if (debug.messageEnabled()) {
            debug.message("setBinaryAttributes: type:" + type + "; name="
                    + name + "; attributes=" + attributes + "; isAdd:" + isAdd);
        }
        setAttributes(token, type, name, attributes, isAdd, false);
    }

    /**
     * Finds the dynamic group member DNs
     * 
     * @param url
     *            the url to be used for the group member search
     * @return the set of group member DNs satisfied the search url
     */

    private Set findDynamicGroupMembersByUrl(LDAPUrl url)
            throws IdRepoException {

        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        LDAPSearchConstraints constraints = ld.getSearchConstraints();
        constraints.setMaxResults(defaultMaxResults);
        constraints.setServerTimeLimit(timeLimit);
        Set groupMemberDNs = new HashSet();
        try {
            if (debug.messageEnabled()) {
                debug.message("search filter in LDAPGroups : "
                        + url.getFilter());
            }
            LDAPSearchResults res = ld.search(url.getDN(), url.getScope(), url
                    .getFilter(), null, false, constraints);
            while (res.hasMoreElements()) {
                try {
                    LDAPEntry entry = res.next();
                    if (entry != null) {
                        groupMemberDNs.add(entry.getDN());
                    }
                } catch (LDAPReferralException lre) {
                    // ignore referrals
                    continue;
                } catch (LDAPException le) {
                    throw new IdRepoException(le.errorCodeToString());
                }
            }
        } catch (LDAPException lde) {
            int resultCode = lde.getLDAPResultCode();
	    String ldeErrMsg = lde.getLDAPErrorMessage();
            debug.error("LDAPv3Repo: findDynamicGroupMembersByUrl. "
                    + "ld.search error: " + resultCode);
            if (debug.messageEnabled()) {
		debug.error("LDAPv3Repo: findDynamicGroupMembersByUrl failed",
		        lde);
                lde.printStackTrace();
            }
            Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82)) 
            {
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "306",
                        args);
            } else {
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "306", args);
            }
        } finally {
            // release the ldap connection back to the pool
            connPool.close(ld);
        }

        return groupMemberDNs;
    }

    private Set getGroupMembers(SSOToken token, IdType type, String name,
            IdType membersType) throws IdRepoException, SSOException {

        // returns all members of the group named name.
        Set resultSet = new HashSet();
        String dn = null;
        try {
            dn = getDN(type, name);
        } catch (IdRepoUnsupportedOpException ide) {
            return null;
        } catch (IdRepoException idrepoerr) {
            return null;
        }

        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        LDAPEntry groupEntry = null;
        try {
            groupEntry = ld.read(dn);
        } catch (LDAPException e) {
            debug.error("LDAPGroups: invalid group name " + name);
            int resultCode = e.getLDAPResultCode();
            if (debug.messageEnabled()) {
		debug.message("LDAPGroups: invalid group name " + name, e);
            }
            Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82))
            {
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "306",
                        args);
            }
            return null;
        } finally {
            connPool.close(ld);
        }
        LDAPAttribute attribute = groupEntry.getAttribute(uniqueMemberAttr);
        if (attribute != null) {
            Enumeration enumVals = attribute.getStringValues();
            while ((enumVals != null) && enumVals.hasMoreElements()) {
                String memberDNStr = (String) enumVals.nextElement();
                resultSet.add(memberDNStr);
            }
        } else { // see if this is a dynamic group.
            attribute = groupEntry.getAttribute(memberURLAttr);
            if (attribute != null) {
                Enumeration enumVals = attribute.getStringValues();
                while ((enumVals != null) && enumVals.hasMoreElements()) {
                    String memberUrl = (String) enumVals.nextElement();
                    try {
                        LDAPUrl ldapUrl = new LDAPUrl(memberUrl);
                        Set dynMembers = findDynamicGroupMembersByUrl(ldapUrl);
                        resultSet.addAll(dynMembers);
                    } catch (java.net.MalformedURLException e) {
                        throw (new IdRepoException("MalformedURLException"));
                    }
                }
            }
        }
        return resultSet;
    }

    private Set getManagedRoleMembers(SSOToken token, IdType type, String name,
            IdType membersType) throws IdRepoException, SSOException {

        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        LDAPSearchConstraints constraints = ld.getSearchConstraints();
        constraints.setMaxResults(defaultMaxResults);
        constraints.setServerTimeLimit(timeLimit);
        Set roleMemberDNs = new HashSet();
        LDAPSearchResults res = null;
        try {
            String filter = "(" + nsRoleDNAttr + "=" + getDN(type, name) + ")";
            if (debug.messageEnabled()) {
                debug.message("search filter in getManagedRoleMembers: "
                        + filter);
            }
            // all entries which has nsRoleDN=managedRoleName
            res = ld.search(orgDN, roleSearchScope, filter, null, false,
                    constraints);

            while (res.hasMoreElements()) {
                try {
                    LDAPEntry entry = res.next();
                    if (entry != null) {
                        roleMemberDNs.add(entry.getDN());
                    }
                } catch (LDAPReferralException lre) {
                    // ignore referrals
                    continue;
                } catch (LDAPException le) {
                    connPool.close(ld);
		    int resultCode = le.getLDAPResultCode();
		    // If time or size limit has reached, return the results
		    if (resultCode == LDAPException.TIME_LIMIT_EXCEEDED ||
			resultCode == LDAPException.SIZE_LIMIT_EXCEEDED) {
			if (debug.messageEnabled()) {
			    debug.message("LDAPv3Plugin: getManagedRoleMembers"
				+ "search iteration size/time limit reached: "
				+ le.getMessage());
			}
			return (roleMemberDNs);
		    }
                    if (debug.messageEnabled()) {
			debug.message("LDAPv3Plugin: getManagedRoleMembers "
			    + "search iteration exception", le);
                    }
                    Object[] args = { CLASS_NAME, Integer.toString(resultCode)};
                    if ((resultCode == 80) || (resultCode == 81)
                            || (resultCode == 82)) {
                        throw new IdRepoFatalException(
                                IdRepoBundle.BUNDLE_NAME, "306", args);
                    } else {
                        throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                                "306", args);
                    }
                }
            }
        } catch (LDAPException lde) {
            connPool.close(ld);
            int resultCode = lde.getLDAPResultCode();
            debug.error("LDAPv3Repo: getManagedRoleMembers, ld.search error"
                    + resultCode);
            if (debug.messageEnabled()) {
		debug.error("LDAPv3Repo: getManagedRoleMembers, " 
		        + "ld.search error", lde);
            }
            Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82)) 
            {
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "306",
                        args);
            } else {
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "306", args);
            }
        }
        connPool.close(ld);
        return roleMemberDNs;
    }

    private Set getFilteredRoleMembers(SSOToken token, IdType type,
            String name, IdType membersType) throws IdRepoException,
            SSOException {

        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	   ld.setCache(ldapCache);
	}
        LDAPSearchConstraints constraints = ld.getSearchConstraints();
        constraints.setMaxResults(defaultMaxResults);
        constraints.setServerTimeLimit(timeLimit);
        String getAttrs[] = { nsRoleFilterAttr };
        Set roleMemberDNs = new HashSet();
        String dn = getDN(type, name);
        try {
            LDAPEntry foundEntry = ld.read(dn, getAttrs);
            LDAPAttribute ldapAttr = foundEntry.getAttribute(nsRoleFilterAttr);
            if (ldapAttr != null) {
                Enumeration enumVals = ldapAttr.getStringValues();
                while ((enumVals != null) && enumVals.hasMoreElements()) {
                    String roleFilter = (String) enumVals.nextElement();
                    LDAPSearchResults res = ld.search(orgDN, roleSearchScope,
                            roleFilter, null, false, constraints);
                    while (res.hasMoreElements()) {
                        try {
                            LDAPEntry entry = res.next();
                            if (entry != null) {
                                roleMemberDNs.add(entry.getDN());
                            }
                        } catch (LDAPReferralException lre) {
                            // ignore referrals
                        } catch (LDAPException le) {
                            connPool.close(ld);
                            int resultCode = le.getLDAPResultCode();
			    // If time or size limit has reached, 
			    // return the results
			    if (resultCode == LDAPException.TIME_LIMIT_EXCEEDED
			        || resultCode ==
				    LDAPException.SIZE_LIMIT_EXCEEDED) {
                                if (debug.messageEnabled()) {
				    debug.message("LDAPv3Plugin: " 
					+ "getManagedRoleMembers search "
					+ "iteration size/time limit reached: "
					+ le.getMessage());
				}
				return (roleMemberDNs);
                            }

                            if (debug.messageEnabled()) {
				debug.message("LDAPv3Repo: "
				    + "getFilteredRoleMembers iteration"
				    + " exception", le);
                                le.printStackTrace();
                            }
                            Object[] args = { CLASS_NAME,
                                    Integer.toString(resultCode) };
                            if ((resultCode == 80) || (resultCode == 81)
                                    || (resultCode == 82)) {
                                throw new IdRepoFatalException(
                                        IdRepoBundle.BUNDLE_NAME, "306", args);
                            } else {
                                throw new IdRepoException(
                                        IdRepoBundle.BUNDLE_NAME, "306", args);
                            }
                        }
                    } // inner while
                } // outer while
            }
        } catch (LDAPException lde) {
            connPool.close(ld);
            int resultCode = lde.getLDAPResultCode();
            debug.error("LDAPv3Repo: getFilteredRoleMembers, ld.read"
                    + resultCode);
            if (debug.messageEnabled()) {
		debug.message("LDAPv3Repo: getFilteredRoleMembers, ld.read",
			lde);
            }
            Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82))
            {
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "306",
                        args);
            } else {
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "306", args);
            }
        }
        connPool.close(ld);
        return roleMemberDNs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getMembers(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String,
     *      com.iplanet.am.sdk.IdType)
     */
    public Set getMembers(SSOToken token, IdType type, String name,
            IdType membersType) throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getMembers called" + type + ": " + name
                    + ": " + membersType);
        }
        Set results = null;
        if (type.equals(IdType.USER) || type.equals(IdType.AGENT)) {
            debug.error("AMSDKRepo: Membership operation is not supported "
                    + " for Users or Agents");
            throw new IdRepoException(IdRepoBundle.getString("203"), "203");
        } else if (type.equals(IdType.GROUP)) {
            if (membersType.equals(IdType.USER)) {
                results = getGroupMembers(token, type, name, membersType);
            } else {
                debug.error("AMSDKRepo: Groups do not supported membership for "
                                + membersType.getName());
                Object[] args = { CLASS_NAME, membersType.getName(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
            }
        } else if (type.equals(IdType.ROLE)) {
            if (membersType.equals(IdType.USER)) {
                results = getManagedRoleMembers(token, type, name, membersType);
            } else {
                Object[] args = { CLASS_NAME, membersType.getName(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
            }
        } else if (type.equals(IdType.FILTEREDROLE)) {
            if (membersType.equals(IdType.USER)) {
                results = getFilteredRoleMembers(
                                token, type, name, membersType);
            } else {
                Object[] args = { CLASS_NAME, membersType.getName(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
            }
        } else {
            Object[] args = { CLASS_NAME, IdOperation.READ.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        return results;
    }

    private Set getGroupMemberShips(SSOToken token, IdType type, String name,
            IdType membershipType) throws IdRepoException, SSOException {

        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        LDAPSearchConstraints constraints = ld.getSearchConstraints();
        constraints.setMaxResults(defaultMaxResults);
        constraints.setServerTimeLimit(timeLimit);
        String getAttrs[] = { memberOfAttr };
        Set groupDNs = new HashSet();
        String dn = getDN(type, name);
        try {
            LDAPEntry foundEntry = ld.read(dn, getAttrs);
            LDAPAttribute ldapAttr = foundEntry.getAttribute(memberOfAttr);
            if (ldapAttr != null) {
                Enumeration enumVals = ldapAttr.getStringValues();
                while ((enumVals != null) && enumVals.hasMoreElements()) {
                    String groupDN = (String) enumVals.nextElement();
                    groupDNs.add(groupDN);
                }
            }
        } catch (LDAPException lde) {
            int resultCode = lde.getLDAPResultCode();
            debug.error("LDAPv3Repo: getGroupMemberShips. ld.read error: "
                    + resultCode);
            if (debug.messageEnabled()) {
		debug.message("LDAPv3Repo: getGroupMemberShips. ld.read error",
		       lde);
                lde.printStackTrace();
            }
            Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82))
            {
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "306",
                        args);
            } else {
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "306", args);
            }
        } finally {
            connPool.close(ld);
        }
        return groupDNs;
    }

    private Set getManagedRoleMemberShips(SSOToken token, IdType type,
            String name, IdType membershipType) throws IdRepoException,
            SSOException {

        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        LDAPSearchConstraints constraints = ld.getSearchConstraints();
        constraints.setMaxResults(defaultMaxResults);
        constraints.setServerTimeLimit(timeLimit);
        String getAttrs[] = { nsRoleDNAttr };
        Set roleDNs = new HashSet();
        String dn = getDN(type, name);
        try {
            LDAPEntry foundEntry = ld.read(dn, getAttrs);
            LDAPAttribute ldapAttr = foundEntry.getAttribute(nsRoleDNAttr);
            if (ldapAttr != null) {
                Enumeration enumVals = ldapAttr.getStringValues();
                while ((enumVals != null) && enumVals.hasMoreElements()) {
                    String roleDN = (String) enumVals.nextElement();
                    roleDNs.add(roleDN);
                }
            }
        } catch (LDAPException lde) {
            int resultCode = lde.getLDAPResultCode();
            debug.error("LDAPv3Repo: getManagedRoleMemberShips. ld.read error"
                    + resultCode);
            if (debug.messageEnabled()) {
		debug.message("LDAPv3Repo: getManagedRoleMemberShips. " +
			"ld.read error", lde);
            }
            Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82))
            {
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "306",
                        args);
            } else {
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "306", args);
            }
        } finally {
            connPool.close(ld);
        }
        return roleDNs;
    }

    private Set getFilteredRoleMemberShips(SSOToken token, IdType type,
            String name, IdType membershipType) throws IdRepoException,
            SSOException {

        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        LDAPSearchConstraints constraints = ld.getSearchConstraints();
        constraints.setMaxResults(defaultMaxResults);
        constraints.setServerTimeLimit(timeLimit);
        String getAttrs[] = { nsRoleAttr };
        Set allRoleDNs = new HashSet();
        String dn = getDN(type, name);
        // nsRole returns both managedRole and filteredRole.
        // there is no way to just get the filtererRole.
        // so get all the roles(managedRole and filteredRole) then
        // remove managedRole from all the roles to get the filteredRole.
        try {
            LDAPEntry foundEntry = ld.read(dn, getAttrs);
            LDAPAttribute ldapAttr = foundEntry.getAttribute(nsRoleAttr);
            if (ldapAttr != null) {
                Enumeration enumVals = ldapAttr.getStringValues();
                while ((enumVals != null) && enumVals.hasMoreElements()) {
                    String roleDN = (String) enumVals.nextElement();
                    allRoleDNs.add(roleDN);
                }
            }
            Set managedRoleDNs = getManagedRoleMemberShips(token, type, name,
                    membershipType);
            allRoleDNs.removeAll(managedRoleDNs);
        } catch (LDAPException lde) {
            int resultCode = lde.getLDAPResultCode();
            debug.error("LDAPv3Repo: getFilteredRoleMemberShips: ld.read: error"
                            + resultCode);
            if (debug.messageEnabled()) {
		debug.error("LDAPv3Repo: getFilteredRoleMemberShips: " +
			"ld.read: error", lde);
            }
            Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82))
            {
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "306",
                        args);
            } else {
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "306", args);
            }
        } finally {
            connPool.close(ld);
        }
        return allRoleDNs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#getMemberships(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String,
     *      com.iplanet.am.sdk.IdType)
     */
    public Set getMemberships(SSOToken token, IdType type, String name,
            IdType membershipType) throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getMemberships called" + type + ": "
                    + name + ": " + membershipType);
        }

        Set result = null;

	if (!type.equals(IdType.USER) && !type.equals(IdType.AGENT)) {
            debug.error("AMSDKRepo: Membership for identities other than "
                    + " Users is not allowed ");
            Object[] args = { CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "206", args);
        } else {
            if (membershipType.equals(IdType.GROUP)) {
                result = getGroupMemberShips(token, type, name, membershipType);
            } else if (membershipType.equals(IdType.ROLE)) {
                result = getManagedRoleMemberShips(token, type, name,
                        membershipType);
            } else if (membershipType.equals(IdType.FILTEREDROLE)) {
                result = getFilteredRoleMemberShips(token, type, name,
                        membershipType);
            } else { // Memberships of any other types not supported for
                debug.error("AMSDKRepo: Membership for other types of "
                        + "entities not supported for Users");
                Object args[] = { CLASS_NAME, type.getName(),
                        membershipType.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "204", args);
            }
        }
        return result;
    }

    private void modifyGroupMembership(SSOToken token, IdType type,
            String name, Set usersSet, IdType membersType, int operation)
            throws IdRepoException, SSOException {

        String groupDN = getDN(type, name);
        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        Iterator it = usersSet.iterator();
        while (it.hasNext()) {
            String userDN = (String) it.next();
            LDAPAttribute mbr1 = new LDAPAttribute(uniqueMemberAttr, userDN);
            LDAPAttribute mbrOf = new LDAPAttribute(memberOfAttr, groupDN);
            LDAPModification mod = null;
            LDAPModification modMemberOf = null;
            switch (operation) {
            case ADDMEMBER:
                mod = new LDAPModification(LDAPModification.ADD, mbr1);
                modMemberOf = new LDAPModification(LDAPModification.ADD, mbrOf);
                break;
            case REMOVEMEMBER:
                mod = new LDAPModification(LDAPModification.DELETE, mbr1);
                modMemberOf = new LDAPModification(LDAPModification.DELETE,
                        mbrOf);
            }
            try {
                ld.modify(groupDN, mod);
                ld.modify(userDN, modMemberOf);
            } catch (LDAPException lde) {
                int resultCode = lde.getLDAPResultCode();
                debug.error("LDAPv3Repo: modifyGroupMembership ld.modify: "
                        + resultCode + " groupDN = " + groupDN + " userDN= "
                        + userDN);
                connPool.close(ld);
                if (debug.messageEnabled()) {
		    debug.error("LDAPv3Repo: modifyGroupMembership ld.modify",
			    lde);
                    lde.printStackTrace();
                }
                Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
                if ((resultCode == 80) || (resultCode == 81)
                        || (resultCode == 82)) {
                    throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME,
                            "306", args);
                } else {
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "306",
                            args);
                }
            }
        }
        connPool.close(ld);
    }

    private void modifyRoleMembership(SSOToken token, IdType type, String name,
            Set usersSet, IdType membersType, int operation)
            throws IdRepoException, SSOException {

        // to add just put nsRoleDN into the user entry.
        // there is nothing we can for filtered role since membership
        // is controlled by a filtered.
        String roleDN = getDN(type, name);
        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        Iterator it = usersSet.iterator();
        while (it.hasNext()) {
            LDAPModification mod = null;
            String userDN = (String) it.next();
            LDAPAttribute mbr1 = new LDAPAttribute(nsRoleDNAttr, roleDN);
            switch (operation) {
            case ADDMEMBER:
                mod = new LDAPModification(LDAPModification.ADD, mbr1);
                break;
            case REMOVEMEMBER:
                mod = new LDAPModification(LDAPModification.DELETE, mbr1);
            }
            try {
                ld.modify(userDN, mod);
            } catch (LDAPException lde) {
                int resultCode = lde.getLDAPResultCode();
                debug.error("LDAPv3Repo: modifyRoleMembership ld.modify: "
                        + resultCode + " userDN= " + userDN + " roleDN= "
                        + roleDN);
                if (debug.messageEnabled()) {
		    debug.error("LDAPv3Repo: modifyRoleMembership ld.modify",
			    lde);
                }
                connPool.close(ld);
                Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
                if ((resultCode == 80) || (resultCode == 81)
                        || (resultCode == 82)) {
                    throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME,
                            "306", args);
                } else {
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "306",
                            args);
                }
            }
        }
        connPool.close(ld);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#modifyMemberShip(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Set,
     *      com.iplanet.am.sdk.IdType, int)
     */
    public void modifyMemberShip(SSOToken token, IdType type, String name,
            Set members, IdType membersType, int operation)
            throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: modifyMemberShip called " + type
                    + "; name= " + name + "; members= " + members
                    + "; membersType= " + membersType + "; operation= "
                    + operation);
        }
        if (members == null || members.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo.modifyMemberShip: Members set " +
                        "is empty");
            }
            throw new IdRepoException(IdRepoBundle.getString("201"), "201");
        }
        if (type.equals(IdType.USER) || type.equals(IdType.AGENT)) {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo.modifyMembership: Memberhsip " +
                        "to users and agents is not supported");
            }
            throw new IdRepoException(IdRepoBundle.getString("203"), "203");
        }
        if (!membersType.equals(IdType.USER)) {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo.modifyMembership: A non-user " +
                        "type cannot  be made a member of any identity"
                                + membersType.getName());
            }
            Object[] args = { CLASS_NAME };
            throw new IdRepoException(IdRepoBundle.getString("206", args),
                    "206", args);
        }

        Set usersSet = new HashSet();
        Iterator it = members.iterator();
        while (it.hasNext()) {
            String curr = (String) it.next();
            String dn = getDN(membersType, curr);
            usersSet.add(dn);
        }

        if (type.equals(IdType.GROUP)) {
            modifyGroupMembership(token, type, name, usersSet, membersType,
                    operation);
        } else if (type.equals(IdType.ROLE)) {
            modifyRoleMembership(token, type, name, usersSet, membersType,
                    operation);
        } else {
            debug.error("AMSDKRepo.modifyMembership: Memberships cannot be"
                    + "modified for type= " + type.getName());
            Object[] args = { CLASS_NAME, type.getName() };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "209", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#removeAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Set)
     */
    public void removeAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: removeAttributes called " + type + ": "
                    + name + attrNames);
        }
        if (attrNames == null || attrNames.isEmpty()) {
            throw new IdRepoException(IdRepoBundle.getString("201"), "201");
        }

        Set predefinedAttr = null;
        if (type.equals(IdType.USER)) {
            predefinedAttr = userAtttributesAllowed;
        } else if (type.equals(IdType.AGENT)) {
            predefinedAttr = agentAtttributesAllowed;
        } else if (type.equals(IdType.GROUP)) {
            predefinedAttr = groupAtttributesAllowed;
        } else if (type.equals(IdType.FILTEREDROLE)) {
	    predefinedAttr = filteredroleAtttributesAllowed;
        }

        String eDN = getDN(type, name);
        if (attrNames != null && attrNames.isEmpty()) {
            LDAPModificationSet ldapModSet = new LDAPModificationSet();
            Iterator itr = attrNames.iterator();
            while (itr.hasNext()) {
                String attrName = (String) (itr.next());
                if ((predefinedAttr != null)
                        && (!predefinedAttr.contains(attrName))) {
                    continue;
                }
                LDAPAttribute theAttr = new LDAPAttribute(attrName);
                ldapModSet.add(LDAPModification.REPLACE, theAttr);
            } // while
            LDAPConnection ld = connPool.getConnection();
	    if (cacheEnabled) {
		ld.setCache(ldapCache);
	    }
            try {
                ld.modify(eDN, ldapModSet);
            } catch (LDAPException lde) {
                int resultCode = lde.getLDAPResultCode();
                debug.error("LDAPv3Repo: setAttributes, ld.modify error: "
                        + resultCode);
                if (debug.messageEnabled()) {
		    debug.error("LDAPv3Repo: setAttributes, ld.modify error",
			   lde);
                }
                Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
                if ((resultCode == 80) || (resultCode == 81)
                        || (resultCode == 82)) {
                    throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME,
                            "306", args);
                } else if (resultCode == LDAPException.NO_SUCH_OBJECT) {
                    args[0] = CLASS_NAME;
                    args[1] = eDN;
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "220",
                            args);
                } else {
                    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "306",
                            args);
                }
            } finally {
                connPool.close(ld);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#search(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, int, int,
     *      java.util.Set, boolean, int, java.util.Map)
     */
    public RepoSearchResults search(SSOToken token, IdType type,
            String pattern, int maxTime, int maxResults, Set returnAttrs,
            boolean returnAllAttrs, int filterOp, Map avPairs, 
            boolean recursive)
            throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: new search called:" + "type:" + type
                    + " ;pattern:" + pattern + " ;avPairs: " + avPairs);
            debug.message("  cont LDAPv3Repo: search: " + "maxTime:" + maxTime
                    + " ;maxResults:" + maxResults + " ;returnAttrs: "
                    + returnAttrs);
            debug.message("  cont LDAPv3Repo: search:" + "returnAllAttrs:"
                    + returnAllAttrs + " ;filterOp:" + filterOp
                    + " ;recursive:" + recursive + " ;returnAttrs: "
                    + returnAttrs);
        }

        // String base = orgDN;
        String base = getBaseDN(type);
        int scope = LDAPv2.SCOPE_SUB;
	if (!recursive) {
	    scope = LDAPv2.SCOPE_ONE;
	}
        boolean attrsOnly = false;

        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        LDAPSearchConstraints searchConstraints = new LDAPSearchConstraints();
        if (maxResults < 1) {
            searchConstraints.setMaxResults(defaultMaxResults);
        } else {
            searchConstraints.setMaxResults(maxResults);
        }

        if (maxTime < 1) {
            searchConstraints.setServerTimeLimit(timeLimit);
        } else {
            searchConstraints.setServerTimeLimit(maxTime * 1000);
        }

        String namingAttr = getNamingAttr(type);
        String[] theAttr = null;
        if (returnAllAttrs) {
            theAttr = new String[] { "*" };
        } else if (returnAttrs != null && !returnAttrs.isEmpty()) {
	    returnAttrs.add(namingAttr);
            theAttr = (String[]) returnAttrs.toArray(new String[returnAttrs
                    .size()]);
        } else { // don't return any attr it will be faster.
	    // Need to get back the naming attribute
	    theAttr = new String[] { namingAttr };
        }

        LDAPSearchResults myResults = null;
        String objectClassFilter = getObjClassFilter(type);

        StringBuffer filterSB = new StringBuffer();

        if (filterOp == IdRepo.OR_MOD) {
            filterSB.append("(|");
        } else if (filterOp == IdRepo.AND_MOD) {
            filterSB.append("(&");
        } // do nothing for IdRepo.NO_MOD

        filterSB.append("(&").append(
                constructFilter(
                        namingAttr, objectClassFilter, pattern)); // note A

        if ((avPairs != null) && (avPairs.size() > 0)) {
            filterSB.append(constructFilter(avPairs));
        }

        filterSB.append(")"); // matches "(" in note A above

        if ((filterOp == IdRepo.AND_MOD) || (filterOp == IdRepo.OR_MOD)) {
            filterSB.append(")");
        }

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: before ld.search call:" + "filterSB:"
                    + filterSB + " ; base:" + base);
            if (theAttr != null) {
                debug.message("          theAttr[0]: " + theAttr[0]);
            } else {
                debug.message("          theAttr[0]:=null");
            }
        }
        try {
            myResults = ld.search(base, scope, filterSB.toString(), theAttr,
                    attrsOnly, searchConstraints);
        } catch (LDAPException lde) {
            int resultCode = lde.getLDAPResultCode();
            connPool.close(ld);
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: search, ld.search error: "
                        + resultCode);
            }
            Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82))
            {
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "306",
                        args);
            } else if (resultCode == 32) {
                // return empty set for entry not found error.
                return (new RepoSearchResults(new HashSet(),
                      RepoSearchResults.SUCCESS, Collections.EMPTY_MAP, type));
            } else {
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "306", args);
            }
        }

        int errorCode = RepoSearchResults.SUCCESS;
        Map allEntryMap = new HashMap();
        Set allEntries = new HashSet();
        try {
            while (myResults.hasMoreElements()) {
                LDAPEntry entry = myResults.next();
                String entryDN = entry.getDN();
                Map attrEntryMap = new HashMap();

                if (returnAllAttrs) {
                    // return all the attributes
                    LDAPAttributeSet ldapAttrSet = entry.getAttributeSet();
                    int size = ldapAttrSet.size();
                    for (int i = 0; i < size; i++) {
                        LDAPAttribute ldapAttr = ldapAttrSet.elementAt(i);
                        if (ldapAttr != null) {
                            String attrName = ldapAttr.getName();
                            Set attrValueSet = new HashSet();
                            Enumeration enumVals = ldapAttr.getStringValues();
                            while ((enumVals != null)
                                    && enumVals.hasMoreElements()) {
                                String value = (String) enumVals.nextElement();
                                attrValueSet.add(value);
                            }
                            attrEntryMap.put(attrName, attrValueSet);
                        }
                    }
		    // Get the naming attribute value
		    Set idNameValue = (Set) attrEntryMap.get(namingAttr);
		    String idName = entryDN;
		    if (idNameValue != null && !idNameValue.isEmpty()) {
			idName = (String) idNameValue.iterator().next();
		    }
		    allEntries.add(idName);
		    allEntryMap.put(idName, attrEntryMap);
                } else if (returnAttrs != null && !returnAttrs.isEmpty()) {
                    // return the attributes specified by caller.
                    Iterator itr = returnAttrs.iterator();
                    while (itr.hasNext()) {
                        String attrName = (String) itr.next();
                        LDAPAttribute ldapAttr = entry.getAttribute(attrName);
                        // return empty set if attribute does not exist.
                        Set attrValueSet = new HashSet();
                        if (ldapAttr != null) {
                            Enumeration enumVals = ldapAttr.getStringValues();
                            while ((enumVals != null)
                                    && enumVals.hasMoreElements()) {
                                String value = (String) enumVals.nextElement();
                                attrValueSet.add(value);
                            }
                        }
                        attrEntryMap.put(attrName, attrValueSet);
                    }
		    // Get the naming attribute value
		    Set idNameValue = (Set) attrEntryMap.get(namingAttr);
		    String idName = entryDN;
		    if (idNameValue != null && !idNameValue.isEmpty()) {
			idName = (String) idNameValue.iterator().next();
		    }
		    allEntries.add(idName);
		    allEntryMap.put(idName, attrEntryMap);
                } else {
		    // returnAllAttrs is false and list of attr to return is null
		    // do not return any attribute  
		    // Get the naming attribute for results
		    // return entry DN if empty
		    String idName = entryDN;
		    LDAPAttribute ldapAttr = entry.getAttribute(namingAttr);
		    if (ldapAttr != null ) { 
			Enumeration enumVals = ldapAttr.getStringValues();
			if ((enumVals != null) && enumVals.hasMoreElements()) {
			    idName = (String) enumVals.nextElement();
			}
		    }
		    allEntries.add(idName);
                }
            } // while

        } catch (LDAPException e) {
            int ldapErrCode = e.getLDAPResultCode();
            switch (errorCode) {
            case LDAPException.TIME_LIMIT_EXCEEDED: {
                errorCode = RepoSearchResults.TIME_LIMIT_EXCEEDED;
                break;
            }
            case LDAPException.SIZE_LIMIT_EXCEEDED: {
                errorCode = RepoSearchResults.SIZE_LIMIT_EXCEEDED;
                break;
            }
            default:
                errorCode = ldapErrCode;
            }
        }

        connPool.close(ld);
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: exit search " + "allEntryDN:"
                    + allEntries + " ;allEntries:" + allEntryMap);
        }

        return new RepoSearchResults(allEntries, errorCode, allEntryMap, type);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#search(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Map, boolean,
     *      int, int, java.util.Set)
     */
    public RepoSearchResults search(SSOToken token, IdType type,
            String pattern, Map avPairs, boolean recursive, int maxResults,
            int maxTime, Set returnAttrs) throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: old search called" + type + ": "
                    + pattern + ": " + avPairs);
        }
        return search(token, type, pattern, maxTime, maxResults, returnAttrs,
                true, IdRepo.NO_MOD, avPairs, recursive);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#setAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Map, boolean)
     */
    public void setAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd) throws IdRepoException, SSOException
            {

        setAttributes(token, type, name, attributes, isAdd, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.IdRepo#setAttributes(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdType, java.lang.String, java.util.Map, boolean)
     */
    public void setAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd, boolean isString)
            throws IdRepoException, SSOException {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: setAttributes called: " + type + ": "
                    + name);
            prtAttrMap(attributes);
        }
        if (attributes == null || attributes.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: setAttributes. Attributes " +
                        "are empty");
            }
            throw new IdRepoException(IdRepoBundle.getString("201"), "201");
        }
        String eDN = getDN(type, name);

        Set predefinedAttr = null;
        if (type.equals(IdType.USER)) {
            predefinedAttr = userAtttributesAllowed;
        } else if (type.equals(IdType.AGENT)) {
            predefinedAttr = agentAtttributesAllowed;
        } else if (type.equals(IdType.GROUP)) {
            predefinedAttr = groupAtttributesAllowed;
        } else if (type.equals(IdType.FILTEREDROLE)) {
	    predefinedAttr = filteredroleAtttributesAllowed;
	}

        LDAPModificationSet ldapModSet = new LDAPModificationSet();
        Iterator itr = attributes.keySet().iterator();
        while (itr.hasNext()) {
            LDAPAttribute theAttr = null;
            String attrName = (String) itr.next();

            if ((predefinedAttr != null)
                    && (!predefinedAttr.contains(attrName))) {
                continue;
            }

            if (isString) {
                Set set = (Set) (attributes.get(attrName));
                String attrValues[] = (set == null ? null : (String[]) set
                        .toArray(new String[set.size()]));
                if (set == null || set.isEmpty()) {
                    // delete the attribute from entry by setting value to
                    // empty.
                    theAttr = new LDAPAttribute(attrName);
                    ldapModSet.add(LDAPModification.REPLACE, theAttr);
                } else {
                    theAttr = new LDAPAttribute(attrName, attrValues);
                    if (isAdd) {
                        ldapModSet.add(LDAPModification.ADD, theAttr);
                    } else {
                        ldapModSet.add(LDAPModification.REPLACE, theAttr);
                    }
                }
            } else {
                byte[][] attrBytes = (byte[][]) (attributes.get(attrName));
                theAttr = new LDAPAttribute(attrName);
                int size = attrBytes.length;
                for (int i = 0; i < size; i++) {
                    if (debug.messageEnabled()) {
                        debug.message("setAttributes binary:" + attrBytes[i]);
                    }
                    theAttr.addValue(attrBytes[i]);
                }
                if (isAdd) {
                    ldapModSet.add(LDAPModification.ADD, theAttr);
                } else {
                    ldapModSet.add(LDAPModification.REPLACE, theAttr);
                }
                if (debug.messageEnabled()) {
                    debug.message("setAttribute binary attrBytes:" + attrBytes);
                }
            }
        } // while
        // Check if LdapModSet is empty
        if (ldapModSet.size() == 0) {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: setAttributes. LdapModSet is empty");
            }
            throw new IdRepoException(IdRepoBundle.getString("201"), "201");
        }

        // For user objects, need to check if all objectclasses are present
        // If not, they must be added (for account lockout atleast)
        if (type.equals(IdType.USER)) {
            Set ocsToBeAdded = new HashSet();
            Set ocAttrName = new HashSet();
            ocAttrName.add(LDAP_OBJECT_CLASS);
            Map attrs = getAttributes(token, type, name, ocAttrName);
            if (attrs != null && !attrs.isEmpty()) {
                Set ocValues = (Set) attrs.values().iterator().next();
                if (ocValues != null && !ocValues.isEmpty()) {
                    for (Iterator items = userObjClassSet.iterator(); items
                            .hasNext();) {
                        String oc = (String) items.next();
                        boolean found = false;
                        // Check if present in ocValues
                        for (Iterator ocs = ocValues.iterator(); ocs.hasNext();)
                        {
                            String occ = (String) ocs.next();
                            if (oc.equalsIgnoreCase(occ)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            ocsToBeAdded.add(oc);
                        }
                    }
                }
            }
            if (!ocsToBeAdded.isEmpty()) {
                // Add to ldapModSet
                ldapModSet.add(LDAPModification.ADD, new LDAPAttribute(
                        LDAP_OBJECT_CLASS, (String[]) ocsToBeAdded
                                .toArray(new String[ocsToBeAdded.size()])));
            }
        }
        LDAPConnection ld = connPool.getConnection();
	if (cacheEnabled) {
	    ld.setCache(ldapCache);
	}
        try {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: setAttributes. Calling ld.modify");
            }
            ld.modify(eDN, ldapModSet);
        } catch (LDAPException lde) {
            int resultCode = lde.getLDAPResultCode();
            if (debug.warningEnabled()) {
                debug.warning("LDAPv3Repo: setAttributes, ld.modify error: "
                        + resultCode, lde);
            }
            Object[] args = { CLASS_NAME, Integer.toString(resultCode) };
            if ((resultCode == 80) || (resultCode == 81) || (resultCode == 82))
            {
                throw new IdRepoFatalException(IdRepoBundle.BUNDLE_NAME, "306",
                        args);
            } else if (resultCode == LDAPException.NO_SUCH_OBJECT) {
                args[0] = CLASS_NAME;
                args[1] = eDN;
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "220", args);
            } else {
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "306", args);
            }
        } finally {
            connPool.close(ld);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#assignService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Map)
     */
    public void assignService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType sType, Map attrMap)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: assignService called. IdType=" + type
                    + "; name=" + name + "; serviceName=" + serviceName
                    + "; SchemaType=" + sType + "; attrMap=" + attrMap);
        }

        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)
                || type.equals(IdType.ROLE) 
                || type.equals(IdType.FILTEREDROLE)) {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        } else if (type.equals(IdType.USER)) {
            Set OCs = (Set) attrMap.get("objectclass");
            Set attrName = new HashSet(1);
            attrName.add("objectclass");
            Map tmpMap = getAttributes(token, type, name, attrName);
            Set oldOCs = (Set) tmpMap.get("objectclass");
            OCs = AMCommonUtils.combineOCs(OCs, oldOCs);
            attrMap.put("objectclass", OCs);
            if (sType.equals(SchemaType.USER)) {
                setAttributes(token, type, name, attrMap, false);
            } else if (sType.equals(SchemaType.DYNAMIC)) {
                // setAttributes(token, type, name, attrMap, false);
                return;
            }
        } else if (type.equals(IdType.REALM)) {
            // add the serviceName and attrMap to myServiceMap
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: assignService: before myServiceMap:"
                        + myServiceMap);
            }
            if ((serviceName != null) && (serviceName.length() > 0)
                    && (attrMap != null)) {
                Map myAttrMap = new HashMap(attrMap);
                myServiceMap.put(serviceName, myAttrMap);
            } else {
                debug.message("LDAPv3Repo: assignService: not stored. " +
                        "null or 0");
            }
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: assignService: after myServiceMap:"
                        + myServiceMap);
            }
            if (myListener != null) {
                myListener.setServiceAttributes(serviceName, myServiceMap);
            }
        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
        if (debug.messageEnabled()) {
            debug.message("  exit assignService.  myServiceMap:"
                            + myServiceMap);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#unassignService(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Set)
     */
    public void unassignService(SSOToken token, IdType type, String name,
            String serviceName, Map attrMap) throws IdRepoException,
            SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: unassignService called. IdType=" + type
                    + "; name=" + name + "; serviceName=" + serviceName
                    + "; attrMap=" + attrMap);
        }
        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)
                || type.equals(IdType.ROLE) || type.equals(IdType.FILTEREDROLE))
        {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        } else if (type.equals(IdType.REALM)) {
            // remove the serviceName and attrMap from myServiceMap
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: unassignService: before " +
                        "myServiceMap:" + myServiceMap);
            }
            if ((serviceName != null) && (serviceName.length() > 0)) {
                myServiceMap.remove(serviceName);
            } else {
                debug.message("LDAPv3Repo: unassignService: serviceName is " +
                        "null or 0");
            }
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: unassignService: after myServiceMap:"
                                + myServiceMap);
            }
            if (myListener != null) {
                myListener.setServiceAttributes(serviceName, myServiceMap);
            }
        } else if (type.equals(IdType.USER)) {
            // Get the object classes that need to be remove from Service Schema
            Set removeOCs = (Set) attrMap.get("objectclass");
            Set attrNameSet = new HashSet();
            attrNameSet.add("objectclass");
            Map objectClassesMap = getAttributes(
                    token, type, name, attrNameSet);
            Set OCValues = (Set) objectClassesMap.get("objectclass");
            removeOCs = AMCommonUtils.updateAndGetRemovableOCs(OCValues,
                    removeOCs);
            // Get the attributes that need to be removed
            Set removeAttrs = new HashSet();
            Iterator iter1 = removeOCs.iterator();
            while (iter1.hasNext()) {
                String oc = (String) iter1.next();
                Set attrs = null;
                try {
                    attrs = new HashSet(getOCAttributes(oc));
                } catch (LDAPException lde) {
                    int resultCode = lde.getLDAPResultCode();
                    debug.error("LDAPv3Repo: unassignService. "
                            + "get Object Attributes failed: " + resultCode);
                    if (debug.messageEnabled()) {
			debug.error("LDAPv3Repo: unassignService.", lde);
                    }
                    Object[] args = { CLASS_NAME, Integer.toString(resultCode)};
                    if ((resultCode == 80) || (resultCode == 81)
                            || (resultCode == 82)) {
                        throw new IdRepoFatalException(
                                IdRepoBundle.BUNDLE_NAME, "306", args);
                    } else {
                        throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
                                "306", args);
                    }
                }
                Iterator iter2 = attrs.iterator();
                while (iter2.hasNext()) {
                    String attrName = (String) iter2.next();
                    removeAttrs.add(attrName.toLowerCase());
                }
            }

            Map avPair = getAttributes(token, type, name);
            Iterator itr = avPair.keySet().iterator();
            while (itr.hasNext()) {
                String attrName = (String) itr.next();
                if (removeAttrs.contains(attrName)) {
                    try {
                        // remove attribute one at a time, so if the first
                        // one fails, it will keep continue to remove
                        // other attributes.
                        Map tmpMap = new AMHashMap();
                        tmpMap.put(attrName, Collections.EMPTY_SET);
                        setAttributes(token, type, name, tmpMap, false);
                    } catch (Exception ex) {
                        if (debug.messageEnabled()) {
                            debug.message("unassignService failed. error " +
                                    "occurred while removing attribute: "
                                            + attrName);
                        }
                    } // catch
                } // if
            } // while

            // Now update the object class attribute
            Map tmpMap = new AMHashMap();
            tmpMap.put("objectclass", OCValues);
            setAttributes(token, type, name, tmpMap, false);
        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getAssignedServices(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public Set getAssignedServices(SSOToken token, IdType type, String name,
            Map mapOfServiceNamesandOCs) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getAssignedServices. IdType=" + type
                    + "; Name=" + name + "; mapOfServiceNamesandOCs="
                    + mapOfServiceNamesandOCs);
            debug.message("     getAssignedServices. myServiceMap="
                    + myServiceMap);
        }
        Set resultsSet = new HashSet();

        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)
                || type.equals(IdType.ROLE) || type.equals(IdType.FILTEREDROLE))
        {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        } else if (type.equals(IdType.USER)) {
            Set OCs = readObjectClass(token, type, name);
            OCs = convertToLowerCase(OCs);
            Iterator iter = mapOfServiceNamesandOCs.keySet().iterator();
            while (iter.hasNext()) {
                String sname = (String) iter.next();
                Set ocSet = (Set) mapOfServiceNamesandOCs.get(sname);
                ocSet = convertToLowerCase(ocSet);
                if ((OCs != null) && OCs.containsAll(ocSet)) {
                    resultsSet.add(sname);
                }
            }
            if (myServiceMap != null) {
                resultsSet.addAll(myServiceMap.keySet());
            }
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: getAssignedServices returns " +
                        "resultsSet: " + resultsSet);
            }
        } else if (type.equals(IdType.REALM)) {
            resultsSet = myServiceMap.keySet();
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: getAssignedServices: resultsSet: "
                        + resultsSet + "; myServiceMap:" + myServiceMap);
            }
        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
        return resultsSet;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getServiceAttributes(
     *      com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Set)
     */
    public Map getServiceAttributes(SSOToken token, IdType type, String name,
            String serviceName, Set attrNames) throws IdRepoException,
            SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: getServiceAttributes. IdType=" + type
                    + "; Name=" + name + "; serviceName=" + serviceName
                    + "; attrNames=" + attrNames);
        }

        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)
                || type.equals(IdType.ROLE) || type.equals(IdType.FILTEREDROLE))
        {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        } else if (type.equals(IdType.USER)) {
            // get the user attributes from ldap.
            Map userAttrs = getAttributes(token, type, name, attrNames);

            // find the attributes in service map.
            if ((serviceName == null) || (serviceName.length() == 0)) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: getServiceAttribute. userAttrs="
                            + userAttrs);
                }
                return (userAttrs);
            }
            Map srvCfgAttrMap = (Map) myServiceMap.get(serviceName);
            Map mySrvAttrMap = new HashMap();
            if ((srvCfgAttrMap == null) || (srvCfgAttrMap.isEmpty())) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: getServiceAttribute: return " +
                            "userAttrs:" + userAttrs);
                }
                return (userAttrs);
            } else {
                Iterator itr = srvCfgAttrMap.keySet().iterator();
                while (itr.hasNext()) {
                    String attrName = (String) itr.next();
                    if (attrNames.contains(attrName)) {
                        mySrvAttrMap.put(attrName, srvCfgAttrMap.get(attrName));
                    }
                }
                if (debug.messageEnabled()) {
                    debug.message("    mySrvAttrMap=" + mySrvAttrMap);
                    debug.message("    srvCfgAttrMap=" + srvCfgAttrMap);
                    debug.message("    userAttrs=" + userAttrs);
                }
            }

            // merge the attributes found from user and service map.
            Set userAttrsNameSet = userAttrs.keySet();
            Iterator itr = mySrvAttrMap.keySet().iterator();
            while (itr.hasNext()) {
                String attrName = (String) itr.next();
                if (userAttrsNameSet.contains(attrName)) {
                    // merge the set and add it.
                    Set userEntrySet = (Set) userAttrs.get(attrName);
                    Set srvEntrySet = (Set) mySrvAttrMap.get(attrName);
                    userEntrySet.addAll(srvEntrySet);
                    userAttrs.put(attrName, userEntrySet);
                } else {
                    userAttrs.put(attrName, mySrvAttrMap.get(attrName));
                }
            }
            if (debug.messageEnabled()) {
                debug.message("    on exit: userAttrs= " + userAttrs);
            }
            return (userAttrs);

        } else if (type.equals(IdType.REALM)) {
            Map srvCfgAttrMap = (Map) myServiceMap.get(serviceName);
            if ((srvCfgAttrMap == null) || srvCfgAttrMap.isEmpty()) {
                debug.message("LDAPv3Repo: getServiceAttributes. REALM " +
                        "returns empty");
                return (new HashMap());
            }
            if ((attrNames == null) || attrNames.isEmpty()) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: getServiceAttributes. REALM: "
                            + "attrNames is null or empty. srvCfgAttrMap="
                            + srvCfgAttrMap);
                }
                return (new HashMap(srvCfgAttrMap));
            } else {
                Map resultMap = new HashMap();
                Set srvCfgAttrNameSet = srvCfgAttrMap.keySet();
                Iterator itr = attrNames.iterator();
                while (itr.hasNext()) {
                    String attrName = (String) itr.next();
                    if (srvCfgAttrNameSet.contains(attrName)) {
                        resultMap.put(attrName, srvCfgAttrMap.get(attrName));
                    }
                }
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo: getServiceAttributes. " +
                            "REALM resultMap=" + resultMap);
                }
                return (resultMap);
            }

        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#modifyService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      java.util.Map)
     */
    public void modifyService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType sType, Map attrMap)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: modifyService. IdType=" + type
                    + "; Name=" + name + "; serviceName=" + serviceName
                    + "; SchemaType=" + sType + "; attrMap=" + attrMap);
        }

        if (type.equals(IdType.AGENT) || type.equals(IdType.GROUP)
                || type.equals(IdType.ROLE) || type.equals(IdType.FILTEREDROLE))
        {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        } else if (type.equals(IdType.REALM)) {
            // modify my map by doing an add and replace of existing value.
            // call listener.
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: modifyService. REALM before. " +
                        "myServiceMap" + myServiceMap);
            }
            Map srvCfgAttrMap = (Map) myServiceMap.get(serviceName);
            if ((srvCfgAttrMap == null) || (srvCfgAttrMap.isEmpty())) {
                myServiceMap.put(serviceName, new HashMap(attrMap));
            } else {
                Set myServiceNameSet = srvCfgAttrMap.keySet();
                Iterator itr = attrMap.keySet().iterator();
                while (itr.hasNext()) {
                    String attrName = (String) itr.next();
                    if (myServiceNameSet.contains(attrName)) {
                        Set attrNamedSet = (Set) attrMap.get(attrName);
                        Set srvCfgAttrNamedSet = (Set) srvCfgAttrMap
                                .get(attrName);
                        srvCfgAttrNamedSet.clear();
                        srvCfgAttrNamedSet.addAll(attrNamedSet);
                        srvCfgAttrMap.put(attrName, srvCfgAttrNamedSet);
                    } else {
                        srvCfgAttrMap.put(attrName, attrMap.get(attrName));
                    }
                }
                myServiceMap.put(serviceName, srvCfgAttrMap);
            }
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: modifyService. REALM after. " +
                        "myServiceMap" + myServiceMap);
            }
            if (myListener != null) {
                myListener.setServiceAttributes(serviceName, myServiceMap);
                debug.message("LDAPv3Repo: modifyService calls " +
                        "setServiceAttributes:" + myServiceMap);
            }
        } else if (type.equals(IdType.USER)) {
            if (sType.equals(SchemaType.DYNAMIC)) {
                Object args[] = { this.getClass().getName(), sType.toString(),
                        type.getName() };
                throw new IdRepoException(
                        IdRepoBundle.BUNDLE_NAME, "214", args);
            } else {
                setAttributes(token, type, name, attrMap, false);
            }
        } else {
            Object args[] = { this.getClass().getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "213", args);
        }
    }

    
    /* (non-Javadoc)
     * @see com.sun.identity.idm.IdRepo#getFullyQualifiedName(
     *     com.iplanet.sso.SSOToken, com.sun.identity.IdType, 
     *     java.lang.String)
     */
    public String getFullyQualifiedName(SSOToken token,
	IdType type, String name) 
	throws IdRepoException, SSOException {
	// given the idtype and the name, we will do search to get its FDN.
	if (debug.messageEnabled()) {
	    debug.message("LDAPv3Repo: getFullyQualifiedName. IdType=" + type
		+ ";  name=" + name);
        }

	if ((name == null) || (name.length() == 0)) {
	    Object[] args = { CLASS_NAME, "" };
	    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, 
		"220", args);
	}
	if ((type != IdType.USER) && (type != IdType.AGENT) 
	    && (type != IdType.GROUP)) {
	    Object[] args = { CLASS_NAME , type};
	    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME,
		"210", args);
	}
	String userDN = searchForName(type, name);
	if (firstHostAndPort.length() == 0) {
	    StringTokenizer tk = new StringTokenizer(ldapServerName);
	    firstHostAndPort = tk.nextToken();
	}

	return ("ldap://" +  firstHostAndPort + "/" + userDN);
    }


    private String searchForName(IdType type, String name) 
	throws IdRepoException, SSOException {
        return (searchForName(type, name, false));
    }
    

    /* 
     *  search for the "name" in DS and return DN if found 
     *  ruturns empty string otherwise.
     */
    private String searchForName(IdType type, String name, boolean auth) 
	throws IdRepoException, SSOException {
	// given the idtype and the name, we will do search to get its FDN.
	if (debug.messageEnabled()) {
	    debug.message("LDAPv3Repo: searchForName. IdType=" + type
		+ ";  name=" + name);
        }
        String userDN = "";
	String baseDN = orgDN;
	int searchScope = LDAPv2.SCOPE_SUB;
	String searchFilter = "";
	String [] attrs = new String[2];
	attrs[0] = "dn";
	String namingAttr = getNamingAttr(type, auth);
	String objectClassFilter = getObjClassFilter(type);
	searchFilter = constructFilter(namingAttr,objectClassFilter, name);
	attrs[1] = namingAttr;
	int userMatches = 0;
        
	LDAPConnection ldc = connPool.getConnection();
	if (cacheEnabled) {
	    ldc.setCache(ldapCache);
	}
        try {
	    if (debug.messageEnabled()) {
		debug.message("Connecting to " + firstHostAndPort + ":" +
		"\nSearching " + baseDN + " for " +
		searchFilter + "\nscope = " + searchScope);
	    }
	    LDAPSearchResults results = ldc.search(baseDN, searchScope,
		    searchFilter, attrs, false);
	    LDAPEntry entry = null;
	    boolean userNamingValueSet=false;
	    while (results.hasMoreElements()) {
	        try {
		    entry = results.next();
		    userDN = entry.getDN();
		    userMatches ++;
		    if (debug.messageEnabled()) {
			debug.message("searchForName: userDN=" + 
			    userDN + "; entry=" + entry);
		    }
		} catch (LDAPReferralException refe) {
		    debug.message("LDAPReferral Detected.");
		    continue;
		}
	    } 
        } catch (LDAPException e) {
	    int ldapResultCode = e.getLDAPResultCode();
	    if (debug.messageEnabled()) {
                debug.message("Search for User error: ", e);
	        debug.message("resultCode: " + ldapResultCode);
	    }
	    Object[] args = { CLASS_NAME, Integer.toString(ldapResultCode) };
	    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "306", args);
	} finally {
	    if (ldc != null) {
		connPool.close(ldc);
	    }
	}
        if (userMatches > 1) {
	    if (debug.messageEnabled()) {
	        debug.message("LDAPv3Repo: searchForName return "
		    + " found more than match.");
	    }
	    Object[] args = { CLASS_NAME };
	    throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "222", args);
	}
	return userDN;
    }
    

    /* (non-Javadoc)
     * @see com.sun.identity.idm.IdRepo#supportsAuthentication()
     */
    public boolean supportsAuthentication() {
	if (debug.messageEnabled()) {
	    debug.message("LDAPv3Repo: supportsAuthentication." +
		" authenticationEnabled=" + authenticationEnabled);
        }
	return (authenticationEnabled);
    }

    
    /* (non-Javadoc)
     * @see com.sun.identity.idm.IdRepo#authenticate(
     *     javax.security.auth.callback.Callback[])
     */
    public boolean authenticate(Callback[] credentials) throws IdRepoException,
	com.sun.identity.authentication.spi.AuthLoginException {
	debug.message("LDAPv3Repo: authenticate. ");

	if (!authenticationEnabled) {
	    debug.message("LDPv3Repo:authenticate. authentication disabled.");
            return (false); 
	}

	// Obtain user name and password from credentials and authenticate
	String username = null;
	String password = null;
	for (int i = 0; i < credentials.length; i++) {
	    if (credentials[i] instanceof NameCallback) {
	        username = ((NameCallback) credentials[i]).getName();
	        if (debug.messageEnabled()) {
		    debug.message("LDPv3Repo:authenticate username: " +
				  username);
		}
	    } else if (credentials[i] instanceof PasswordCallback) {
                char[] passwd = ((PasswordCallback) credentials[i])
		    .getPassword();
                if (passwd != null) {
		    password = new String(passwd);
		    debug.message("LDAPv3Repo:authN passwd present: " +
			    password);
		}
	    }
	}
	if (username == null || password == null) {
	    Object args[] = { CLASS_NAME };
	    throw new IdRepoException(
		IdRepoBundle.BUNDLE_NAME, "221", args);
	}
	boolean success = false;
	if (debug.messageEnabled()) {
	    debug.message("LDAPv3.authenticate: username="
		     + username);
	}
	String userDN = "";
	if (DN.isDN(username)) {
	    userDN = username;
	} else {
	    // see if it is a user.
	    if (authenticatableSet.contains("User")) {
	        try {
	            userDN = searchForName(IdType.USER, username, true);
                } catch (IdRepoException repoerr) {
		    if (debug.messageEnabled()) {
		        debug.message("LDAPv3.authenticate: " +
			    repoerr.getMessage() + "" +
			    " search failed IdType.USER,  username=" 
			    + username);
                    }
	        } catch (SSOException ssoerr) {
		    if (debug.messageEnabled()) {
		        debug.message("LDAPv3.authenticate: sso error = " 
			    + ssoerr.getL10NMessage() + 
			    " search failed IdType.USER  username=" 
			    + username);
                    }
	        }
	    } 
	    if (authenticatableSet.contains("Agent")) {
	    // see if it is a agent.
	        if (userDN.length() == 0) {
		    try {
		        userDN = searchForName(IdType.AGENT, username, true);
                    } catch (IdRepoException repoerr) {
		        if (debug.messageEnabled()) {
		            debug.message("LDAPv3.authenticate: " +
			        repoerr.getMessage() + "" +
			        " search failed IdType.AGENT, username=" 
			        + username);
                        }
	            } catch (SSOException ssoerr) {
		        if (debug.messageEnabled()) {
	                    debug.message("LDAPv3.authenticate: sso error=" 
			        + ssoerr.getL10NMessage() + 
			        " search failed IdType.AGENT username=" 
			        + username);
                        }
	            }
	        }
	    }
	    if (authenticatableSet.contains("Group")) {
	    // see if it is a agent.
	        if (userDN.length() == 0) {
		    try {
		        userDN = searchForName(IdType.GROUP, username, true);
                    } catch (IdRepoException repoerr) {
		        if (debug.messageEnabled()) {
		            debug.message("LDAPv3.authenticate: " +
			        repoerr.getMessage() + "" +
			        " search failed IdType.GROUP, username=" 
			        + username);
                        }
	            } catch (SSOException ssoerr) {
		        if (debug.messageEnabled()) {
	                    debug.message("LDAPv3.authenticate: sso error=" 
			        + ssoerr.getL10NMessage() + 
			        " search failed IdType.GROUP, username=" 
			        + username);
                        }
	            }
	        }
	    }
	}
	if (debug.messageEnabled()) {
	    debug.message("LDAPv3.authenticate: userDN=" + userDN);
	}
	LDAPConnection ldc = null;
	if (userDN.length() > 0) {
	    try {
		ldc = connPool.getConnection();
	        if (cacheEnabled) {
	            ldc.setCache(ldapCache);
	        }
		ldc.authenticate(version, userDN, password);
		if ( ldc != null ) {
		    int seconds = checkControls(ldc);
		    switch(seconds) {
			case NO_PASSWORD_CONTROLS: 
			    debug.message("No controls returned");
			    success = true;
			    break;
			case PASSWORD_EXPIRED:
			    debug.message("Password expired and must be reset");
			    break;
			default:
			    debug.message("password will expire");
			    success = true;
		    }
                } 
	    } catch (LDAPException e) {
		success = false;
		debug.error("LDAPv3.authenticate: " +
			"username=" + username +
			"; userDN=" + userDN +
			"; ldap error message=" +
			e.getLDAPErrorMessage());
	    } finally {
		if (ldc != null) {
		    connPool.close(ldc);
		}
	    }
        } else {
	    // not an authenticable type or user not found.
	    debug.message("LDAPv3.authenticate: userDN is null or zero length.");
	}

	return (success);
    }

    /*
     * returns the LDPACache handle for this instance of the plugin.
     */
    public LDAPCache GetCache() {
	return(ldapCache);
    }

    /*
     * flush the entire cache starting from orgDN.
     */
    public void clearCache() {
       if (debug.messageEnabled()) {
	   debug.message("clearCache");
       }
       if ((!cacheEnabled) || (ldapCache == null)) {
	   return;
       }
       boolean status = ldapCache.flushEntries(null, LDAPv2.SCOPE_SUB);
       if (debug.messageEnabled()) {
	   debug.message("clearCache: flushed return " + status);
       }
    }

    /*
     * removed the dn from the cache.
     */
    public void objectChanged(String dn, int changeType) {
	if (debug.messageEnabled()) {
	    debug.message("objectChanged:  dn=" + dn);
	}
	boolean flushStatus;

        if ((!cacheEnabled) || (ldapCache == null)) {
	    return;
	}

	if (changeType == LDAPPersistSearchControl.ADD) {
	    DN fqdn = new DN(dn);
	    DN parentDN = fqdn.getParent();

	    do {  
		flushStatus = ldapCache.flushEntries(parentDN.toString(), LDAPv2.SCOPE_ONE);
		if (debug.messageEnabled()) {
		    debug.message("objectChanged LDAPPersistSearchControl.ADD: " +
			"parent  scope_one flushStatus= " +flushStatus);
		}
	    } while (flushStatus);

	    do {  // did not work by itself. still in cache while open subject after add user.
		flushStatus = ldapCache.flushEntries(parentDN.toString(), LDAPv2.SCOPE_BASE);
		if (debug.messageEnabled()) {
		    debug.message("objectChanged LDAPPersistSearchControl.ADD: " +
			"parent  scope_base flushStatus= " +flushStatus);
		}
	    } while (flushStatus);
	} else if (changeType == LDAPPersistSearchControl.MODIFY) {
	    do {
		flushStatus = ldapCache.flushEntries(dn, LDAPv2.SCOPE_BASE);
                if (debug.messageEnabled()) {
		    debug.message("objectChanged LDAPPersistSearchControl.MODIFY " +
			"dn scope_base flushStatus= " +flushStatus);
		}
	    } while (flushStatus);
	} else if (changeType == LDAPPersistSearchControl.MODDN) {
	    DN fqdn = new DN(dn);
	    DN parentDN = fqdn.getParent();
	    String parent = parentDN.toString();
	    do {
		flushStatus = ldapCache.flushEntries(parent, LDAPv2.SCOPE_ONE); // this includes self.
		if (debug.messageEnabled()) {
		    debug.message("objectChanged LDAPPersistSearchControl.MODDN " +
			"parent scope_one: flushStatus= " +flushStatus);
		}
	    } while (flushStatus);

	    do {
		flushStatus = ldapCache.flushEntries(parent, LDAPv2.SCOPE_BASE);
		if (debug.messageEnabled()) {
		    debug.message("objectChanged LDAPPersistSearchControl.MODDN " +
			"parent scope_base: flushStatus= " +flushStatus);
		}
	    } while (flushStatus);
	} else { // assume LDAPPersistSearchControl.DELETE is the only one left.
	    DN fqdn = new DN(dn);
	    DN parentDN = fqdn.getParent();
	    String parent = parentDN.toString();
	    do {
		flushStatus = ldapCache.flushEntries(parent, LDAPv2.SCOPE_SUB);
		if (debug.messageEnabled()) {
		    debug.message("objectChanged. other; parent, scope_sub " +
			"parent scope_sub flushStatus= " +flushStatus);
		}
	    } while (flushStatus);

	    do {
		flushStatus = ldapCache.flushEntries(parent, LDAPv2.SCOPE_BASE);
		if (debug.messageEnabled()) {
		    debug.message("objectChanged. other2; parent, scope_base " +
			"parent scope_base flushStatus= " +flushStatus);
		}
	    } while (flushStatus);
	}
    }


    /**
     * checks for  an LDAP v3 server whether the  control has returned
     * if a password has expired or password is expiring and password
     * policy is enabled on the server.
     * @return PASSWOR_EXPIRED if password has expired
     * @return number of seconds until expiration if password is going to expire
     */

    private int checkControls(LDAPConnection ld) {
        LDAPControl[] controls = ld.getResponseControls();
        int status = NO_PASSWORD_CONTROLS;
        if ((controls != null) && (controls.length >= 1)) {
            LDAPPasswordExpiringControl expgControl = null;
            for (int i = 0; i < controls.length; i++) {
                if (controls[i] instanceof LDAPPasswordExpiredControl) {
                    return PASSWORD_EXPIRED;
                }
                if (controls[i] instanceof LDAPPasswordExpiringControl) {
                    expgControl = (LDAPPasswordExpiringControl)controls[i];
                }
            }
            if (expgControl != null) {
                try {
                        /* Return the number of seconds until expiration */
                    return expgControl.getSecondsToExpiration();
                } catch(NumberFormatException e) {
                    if (debug.messageEnabled()) {
                        debug.message( "Unexpected message <" +
                        expgControl.getMessage() +
                        "> in password expiring control" );
                    }
                }
            }
        }
        return NO_PASSWORD_CONTROLS;
    }


    private Collection getOCAttributes(String objClassName)
            throws LDAPException {
        Collection attributes = getRequiredAttributes(objClassName);
        attributes.addAll(getOptionalAttributes(objClassName));
        return attributes;
    }

    private Collection getRequiredAttributes(String objClassName)
            throws LDAPException {
        Collection attributeNames = new ArrayList();
        LDAPObjectClassSchema objClass = getLDAPSchema().getObjectClass(
                objClassName);
        if (objClass != null) {
            Enumeration en = objClass.getRequiredAttributes();
            while (en.hasMoreElements()) {
                attributeNames.add( (String)en.nextElement());
            }
        }
        return attributeNames;
    }

    private Collection getOptionalAttributes(String objClassName)
            throws LDAPException {
        Collection attributeNames = new ArrayList();
        LDAPObjectClassSchema objClass = getLDAPSchema().getObjectClass(
                objClassName);
        if (objClass != null) {
            Enumeration en = objClass.getOptionalAttributes();
            while (en.hasMoreElements()) {
                attributeNames.add( (String)en.nextElement());
            }
        }
        return attributeNames;
    }

    private LDAPSchema getLDAPSchema() throws LDAPException {
        LDAPSchema dirSchema = new LDAPSchema();
        LDAPConnection conn = connPool.getConnection();
        if (cacheEnabled) {
            conn.setCache(ldapCache);
        }
        Object previousProp = null;

        try {
            // disable the checking of attribute syntax quoting and the read on
            // ""
            previousProp = conn.getProperty(SCHEMA_BUG_PROPERTY);
            conn.setProperty(SCHEMA_BUG_PROPERTY, VAL_STANDARD);

            int retry = 0;
            while (retry <= connNumRetry) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPv3Repo.getLDAPSchema retry: " + retry);
                }
                try {
                    // after connection is down, fetchSchema will not try to
                    // reconnect. So use read to force it to reconnect
                    if (retry > 0) {
                        try {
                            conn.read("fake=fake");
                        } catch (Exception ex) {
                        }
                    }
                    dirSchema.fetchSchema(conn, "cn=schema");
                    return dirSchema;
                } catch (LDAPException e) {
                    retry++;
                    try {
                        Thread.currentThread().sleep(connRetryInterval);
                    } catch (InterruptedException ex) {
                    }
                } // catch
            } // while
        } catch (LDAPException e) {
            debug.error("LDAPv3Repo.getLDAPSchema Exception: ", e);
            throw e;
        } finally {
            if (previousProp != null) {
                conn.setProperty(SCHEMA_BUG_PROPERTY, previousProp);
            }
            connPool.close(conn);
        }
        return dirSchema;
    }

    private Set readObjectClass(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        Set attrNameSet = new HashSet();
        attrNameSet.add("objectclass");
        Map objectClassesMap = getAttributes(token, type, name, attrNameSet);
        Set OCValues = (Set) objectClassesMap.get("objectclass");
        return OCValues;
    }

    private Set convertToLowerCase(Set vals) {
        if (vals == null || vals.isEmpty()) {
            return vals;
        } else {
            Set tSet = new HashSet();
            Iterator it = vals.iterator();
            while (it.hasNext()) {
                tSet.add(((String) it.next()).toLowerCase());
            }
            return tSet;
        }
    }

    private Set parseInputedOps(StringTokenizer st, boolean supportService) {
        // read op from st.
        Set opsReadSet = new HashSet();
        while (st.hasMoreTokens()) {
            String idOpToken = st.nextToken();
            if (idOpToken.equalsIgnoreCase("read")) {
                opsReadSet.add(IdOperation.READ);
            } else if (idOpToken.equalsIgnoreCase("edit")) {
                opsReadSet.add(IdOperation.EDIT);
            } else if (idOpToken.equalsIgnoreCase("create")) {
                opsReadSet.add(IdOperation.CREATE);
            } else if (idOpToken.equalsIgnoreCase("delete")) {
                opsReadSet.add(IdOperation.DELETE);
            } else if (idOpToken.equalsIgnoreCase("service")) {
                if (supportService) {
                    opsReadSet.add(IdOperation.SERVICE);
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("parseInputedOps exit: opsReadSet:" + opsReadSet);
        }
        return opsReadSet;
    }

    private void parsedUserSpecifiedOps(Set userSpecifiedOpsSet) {
        // FIXME Is the field this.userSpecifiedOpsSet to be used in this?

        // parse each entry, string, based syntax:
        // idType=idOperation,idOperation ...
        // if the idType is within my type and op then add it.
        if (debug.messageEnabled()) {
            debug.message("parsedUserSpecifiedOps entry: userSpecifiedOpsSet:"
                    + userSpecifiedOpsSet);
        }
        IdType idTypeRead = null;
	Set opsREAD = null;
	Map oldSupportedOps = new HashMap(supportedOps);
	supportedOps.clear();
	Iterator it = userSpecifiedOpsSet.iterator();
        while (it.hasNext()) {
            idTypeRead = null;
            Set opsRead = null;
            String curr = (String) it.next();
            StringTokenizer st = new StringTokenizer(curr, "= ,");
            if (st.hasMoreTokens()) {
                String idtypeToken = st.nextToken(); // read the type.
                if (debug.messageEnabled()) {
                    debug.message("    idtypeToken:" + idtypeToken);
                }
                if (idtypeToken.equalsIgnoreCase("user")) {
                    idTypeRead = IdType.USER;
                    opsRead = parseInputedOps(st, true);
                } else if (idtypeToken.equalsIgnoreCase("group")) {
                    idTypeRead = IdType.GROUP;
                    opsRead = parseInputedOps(st, false);
                } else if (idtypeToken.equalsIgnoreCase("agent")) {
                    idTypeRead = IdType.AGENT;
                    opsRead = parseInputedOps(st, false);
                } else if (idtypeToken.equalsIgnoreCase("role")) {
		    idTypeRead = IdType.ROLE;
		    opsRead = parseInputedOps(st, false);
		} else if (idtypeToken.equalsIgnoreCase("filteredrole")) {
		    idTypeRead = IdType.FILTEREDROLE;
		    opsRead = parseInputedOps(st, false);                    
                } else if (idtypeToken.equalsIgnoreCase("realm")) {
                    idTypeRead = IdType.REALM;
                    opsRead = parseInputedOps(st, true);
                } else {
                    idTypeRead = null; // unknown or unsupported type.
                }
            } // else a blank line.

            if ((idTypeRead != null) && (opsRead != null)
                    && (!opsRead.isEmpty())) {
                supportedOps.put(idTypeRead, opsRead);
                if (debug.messageEnabled()) {
                    debug.message("parsedUserSpecifiedOps called supportedOps:"
                            + supportedOps + "; idTypeRead:" + idTypeRead
                            + "; opsRead:" + opsRead);
                }
            }

        } // while

    }

    private void loadSupportedOps() {
        Set opSet = new HashSet();
        opSet.add(IdOperation.CREATE);
        opSet.add(IdOperation.DELETE);
        opSet.add(IdOperation.EDIT);
        opSet.add(IdOperation.READ);
        opSet.add(IdOperation.SERVICE);

        supportedOps.put(IdType.USER, Collections.unmodifiableSet(opSet));
        supportedOps.put(IdType.REALM, Collections.unmodifiableSet(opSet));

        Set op2Set = new HashSet(opSet);
        op2Set.remove(IdOperation.SERVICE);
        supportedOps.put(IdType.GROUP, Collections.unmodifiableSet(op2Set));
        supportedOps.put(IdType.AGENT, Collections.unmodifiableSet(op2Set));
	supportedOps.put(IdType.ROLE, Collections.unmodifiableSet(op2Set));
	supportedOps.put(IdType.FILTEREDROLE, 
	        Collections.unmodifiableSet(op2Set));
        if (debug.messageEnabled()) {
            debug.message("loadSupportedOps: supportedOps: " + supportedOps);
        }
    }

    private String getObjClassFilter(IdType type) {
        String objClassFilter = null;
        if (type.equals(IdType.USER)) {
            objClassFilter = userSearchFilter;
        } else if (type.equals(IdType.GROUP)) {
            objClassFilter = groupSearchFilter;
        } else if (type.equals(IdType.ROLE)) {
            objClassFilter = roleSearchFilter;
        } else if (type.equals(IdType.FILTEREDROLE)) {
	    objClassFilter = filterroleSearchFilter;
        } else if (type.equals(IdType.AGENT)) {
            objClassFilter = agentSearchFilter;
        } else {
            // should we just throw an exception
            objClassFilter = userSearchFilter;
        }
        if (debug.messageEnabled()) {
            debug.message("getObjClassFilter returns: objClassFilter="
                    + objClassFilter);
        }
        return objClassFilter;
    }

    private String getNamingAttr(IdType type) {
	return (getNamingAttr(type, false)); 
    }

    private String getNamingAttr(IdType type, boolean auth) {
	String namingAttr = null; 

        if (auth) {
	    return(authNamingAttr);
	}
        if (type.equals(IdType.USER)) {
            namingAttr = userSearchNamingAttr;
        } else if (type.equals(IdType.GROUP)) {
            namingAttr = groupSearchNamingAttr;
	} else if (type.equals(IdType.ROLE) ||
	    type.equals(IdType.FILTEREDROLE)) {
            namingAttr = roleSearchNamingAttr;
        } else if (type.equals(IdType.AGENT)) {
            namingAttr = agentSearchNamingAttr;
        } else {
            // should we just throw an exception
            namingAttr = userSearchNamingAttr;
        }

        return namingAttr;
    }

    private String getDN(IdType type, String name) throws IdRepoException {
        String dn;

        String origName = name;
        if (name == null) {
            name = "";
        } else if (name.length() > 0) {
            name = name + ",";
        }

        if (type.equals(IdType.USER)) {
            if ((peopleCtnrValue == null) || (peopleCtnrValue.length() == 0)
                    || (peopleCtnrNamingAttr == null)
                    || (peopleCtnrNamingAttr.length() == 0)) {
                // Since people container is not specified, do a sub-tree
                // search to find the user DN
                // Auto-construct the DN in case search failed
                dn = userSearchNamingAttr + "=" + name + orgDN;
                String filter = constructFilter(userSearchNamingAttr,
                        getObjClassFilter(IdType.USER), origName);
                LDAPConnection ld = null;
                try {
                    ld = connPool.getConnection();
                    if (cacheEnabled) {
                        ld.setCache(ldapCache);
                    }
                    LDAPSearchResults results = ld.search(orgDN,
                            LDAPv2.SCOPE_SUB, filter, null, false);
                    if (results != null && results.hasMoreElements()) {
                        // Take the first DN
                        dn = results.next().getDN();
                    }
                } catch (Exception lde) {
                    // Debug the exception and return the auto-constructed DN
                    if (debug.messageEnabled()) {
                        debug.message("LDAPv3Repo: getDN user search", lde);
                    }
                } finally {
                    if (ld != null) {
                        connPool.close(ld);
                    }
                }
            } else {
                dn = userSearchNamingAttr + "=" + name + peopleCtnrNamingAttr
                        + "=" + peopleCtnrValue + "," + orgDN;
            }
        } else if (type.equals(IdType.AGENT)) {
            if ((agentCtnrValue == null) || (agentCtnrValue.length() == 0)
                    || (agentCtnrNamingAttr == null)
                    || (agentCtnrNamingAttr.length() == 0)) {
                dn = agentSearchNamingAttr + "=" + name + orgDN;
            } else {
                dn = agentSearchNamingAttr + "=" + name + agentCtnrNamingAttr
                        + "=" + agentCtnrValue + "," + orgDN;
            }
        } else if (type.equals(IdType.GROUP)) {
            if ((groupCtnrValue == null) || (groupCtnrValue.length() == 0)
                    || (groupCtnrNamingAttr == null)
                    || (groupCtnrNamingAttr.length() == 0)) {
                dn = groupSearchNamingAttr + "=" + name + orgDN;
            } else {
                dn = groupSearchNamingAttr + "=" + name + groupCtnrNamingAttr
                        + "=" + groupCtnrValue + "," + orgDN;
            }
        } else if (type.equals(IdType.ROLE)  ||
            type.equals(IdType.FILTEREDROLE)) {
            dn = roleSearchNamingAttr + "=" + name + orgDN;
        } else {
            Object[] args = { CLASS_NAME, IdOperation.READ.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        return dn;
    }

    private String getBaseDN(IdType type) throws IdRepoException {
        String dn;

        if (type.equals(IdType.USER)) {
            if ((peopleCtnrValue == null) || (peopleCtnrValue.length() == 0)
                    || (peopleCtnrNamingAttr == null)
                    || (peopleCtnrNamingAttr.length() == 0)) {
                dn = orgDN;
            } else {
                dn = peopleCtnrNamingAttr + "=" + peopleCtnrValue + "," + orgDN;
            }
        } else if (type.equals(IdType.AGENT)) {
            if ((agentCtnrValue == null) || (agentCtnrValue.length() == 0)
                    || (agentCtnrNamingAttr == null)
                    || (agentCtnrNamingAttr.length() == 0)) {
                dn = orgDN;
            } else {
                dn = agentCtnrNamingAttr + "=" + agentCtnrValue + "," + orgDN;
            }
        } else if (type.equals(IdType.GROUP)) {
            if ((groupCtnrValue == null) || (groupCtnrValue.length() == 0)
                    || (groupCtnrNamingAttr == null)
                    || (groupCtnrNamingAttr.length() == 0)) {
                dn = orgDN;
            } else {
                dn = groupCtnrNamingAttr + "=" + groupCtnrValue + "," + orgDN;
            }
	} else if (type.equals(IdType.ROLE) || 
	    type.equals(IdType.FILTEREDROLE)) {
            dn = orgDN;
        } else {
            dn = orgDN;
        }
        return dn;
    }

    private String constructFilter(Map avPairs) {
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: constructFilter: avPairs=" + avPairs);
        }
        StringBuffer filterSB = new StringBuffer();
        boolean appendedAmp = false;

        Iterator iter = avPairs.keySet().iterator();
        if (iter.hasNext()) {
            filterSB.append("(&");
            appendedAmp = true;
        }

        while (iter.hasNext()) {
            String attributeName = (String) iter.next();
            Iterator iter2 = ((Set) (avPairs.get(attributeName))).iterator();

            while (iter2.hasNext()) {
                String attributeValue = (String) iter2.next();
                filterSB.append("(").append(attributeName).append("=").append(
                        attributeValue).append(")");
            }
        }

        if (appendedAmp == true) {
            filterSB.append(")");
        }
        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: exit constructFilter: " + "filterSB= "
                    + filterSB);
        }
        return filterSB.toString();
    }

    private String constructFilter(String namingAttr, String objectClassFilter,
            String wildcard) {

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: constructFilter: "
                    + "objectClassFilter=" + objectClassFilter + "; wildcard="
                    + wildcard + "; namingAttr=" + namingAttr);
        }
        StringBuffer filterSB = new StringBuffer();
        int index = objectClassFilter.indexOf("%U");
        int vIndex = objectClassFilter.indexOf("%V");

        if ((index == -1) && (vIndex == -1)) {
            if ((namingAttr == null) || (namingAttr.length() == 0)) {
                filterSB.append(objectClassFilter);
            } else {
                filterSB.append("(&(").append(namingAttr).append("=").append(
                        wildcard).append(")").append(objectClassFilter).append(
                        ")");
            }
            objectClassFilter = filterSB.toString();

            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo: exit 1 constructFilter . "
                        + "objectClassFilter=" + objectClassFilter);
            }
            return (objectClassFilter);
        } else {
            String uPart;
            String vPart;
            int indexat = wildcard.indexOf("@");

            if (indexat == -1) {
                uPart = wildcard;
                vPart = "*";
            } else {
                uPart = wildcard.substring(0, indexat);
                vPart = wildcard.substring(indexat + 1);
            }
        
            while (index != -1) {
                filterSB.append(objectClassFilter.substring(0, index)).append(
                        wildcard)
                        .append(objectClassFilter.substring(index + 2));
                objectClassFilter = filterSB.toString();
                filterSB = new StringBuffer();
                index = objectClassFilter.indexOf("%U");
            }

            // int index2 = objectClassFilter.indexOf("%V");
            while (vIndex != -1) {
                filterSB.append(objectClassFilter.substring(0, vIndex)).append(
                        wildcard).append(
                        objectClassFilter.substring(vIndex + 2));
                objectClassFilter = filterSB.toString();
                filterSB = new StringBuffer();
                vIndex = objectClassFilter.indexOf("%V");
            }
        }

        if (debug.messageEnabled()) {
            debug.message("LDAPv3Repo: exit constructFilter. "
                    + "objectClassFilter=" + objectClassFilter);
        }

        return objectClassFilter;
    }

    private Map getCreateUserAttrMapping(Map configParams) {
        Set createUserAttrMappingSet = ((Set) configParams
                .get(LDAPv3Config_LDAP_CREATEUSERMAPPING));

        Map createAttrMap;
        if (createUserAttrMappingSet == null
                || createUserAttrMappingSet.isEmpty()) {
            createAttrMap = Collections.EMPTY_MAP;
        } else {
            if (debug.messageEnabled()) {
                debug.message("in getCreateUserAttrMapping: "
                        + "createUserAttrMappingSet="
                        + createUserAttrMappingSet);
            }
            int size = createUserAttrMappingSet.size();
            createAttrMap = new CaseInsensitiveHashMap(size);
            Iterator it = createUserAttrMappingSet.iterator();
            while (it.hasNext()) {
                String mapString = (String) it.next();
                int eqIndex = mapString.indexOf('=');
                if (eqIndex > -1) {
                    String first = mapString.substring(0, eqIndex);
                    String second = mapString.substring(eqIndex + 1);
                    createAttrMap.put(first, second);
                } else {
                    // this is a special case to denote use the user name for
                    // attr value.
                    createAttrMap.put(mapString, mapString);
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("exit getCreateUserAttrMapping: createAttrMap="
                    + createAttrMap);
        }
        return createAttrMap;
    }

    private void prtAttrMap(Map attrMap) {
        if (attrMap.containsKey("userpassword")) {
            AMHashMap removedPasswd = new AMHashMap();
            removedPasswd.copy(attrMap);
            removedPasswd.remove("userpassword");
            removedPasswd.put("userpassword", "xxx...");
            debug.message("    attrs: " + removedPasswd);
        } else {
            debug.message("    attrs: " + attrMap);
        }
    }

    private void setDSType(Map configParams) {
	if (configParams.containsKey(LDAPv3Config_LDAPV3AD)) {
	    dsType = LDAPv3Config_LDAPV3AD;
	} else if (configParams.containsKey(LDAPv3Config_LDAPV3AMDS)) {
	    dsType = LDAPv3Config_LDAPV3AMDS;
	} else {
	    dsType = LDAPv3Config_LDAPV3GENERIC;
	}
    }

    private int getPropertyIntValue(Map configParams, String key,
            int defaultValue) {
        int value = defaultValue;
        try {
            Set valueSet = (Set) configParams.get(key);
            if (valueSet != null && !valueSet.isEmpty()) {
                value = Integer.parseInt((String) valueSet.iterator().next());
            }
        } catch (NumberFormatException nfe) {
            value = defaultValue;
        }
        if (debug.messageEnabled()) {
            debug.message("    LDAPv3Repo.getPropertyIntValue(): " + key
                    + " = " + value);
        }
        return value;
    }

    private String getPropertyStringValue(Map configParams, String key, String defaultVal) {
	String value = getPropertyStringValue(configParams, key);
	if (value == null) {
	    value = defaultVal;
	}
	return value;
    }

    private String getPropertyStringValue(Map configParams, String key) {
        String value = null;
        Set valueSet = (Set) configParams.get(key);
        if (valueSet != null && !valueSet.isEmpty()) {
            value = (String) valueSet.iterator().next();
        } else {
            if (debug.messageEnabled()) {
                debug.message("LDAPv3Repo.getPropertyStringValue failed:"
                                + key);
            }
        }
        if (debug.messageEnabled()) {
            if (!key.equals(LDAPv3Config_AUTHPW)) {
                debug.message("    LDAPv3Repo.getPropertyStringValue(): " + key
                        + " = " + value);
            } else {
                if ((value == null) || (value.length() == 0)) {
                    debug.message("    LDAPv3Repo.getPropertyStringValue(): "
                            + key + " = NULL or ZERO LENGTH");
                } else {
                    debug.message("    LDAPv3Repo.getPropertyStringValue(): "
                            + key + " = has value XXX");
                }
            }
        }
        return value;
    }

    private boolean getPropertyBooleanValue(Map configParams, String key) {
	String value = getPropertyStringValue(configParams, key);
	if ((value != null) && (value.equalsIgnoreCase("true"))) {
	    return (true);
	} else {
	    return (false);
	}
    }

}
