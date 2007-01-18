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
 * $Id: ServiceManager.java,v 1.8 2007-01-18 23:43:18 arviranga Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.sm;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.EncodeAction;
import java.io.InputStream;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>ServiceManager</code> class provides methods to register/remove
 * services and to list currently registered services. It also provides methods
 * to obtain an instance of <code>ServiceSchemaManager</code> and an instance
 * of <code>ServiceConfigManager</code>.
 *
 * @supported.api
 */
public class ServiceManager {

    // Initialization parameters
    private static boolean initialized;

    private static boolean loadedAuthServices;

    private static SSOTokenManager ssoTokenManager = SMSEntry.tm;

    protected static final String serviceDN = SMSEntry.SERVICES_RDN
            + SMSEntry.COMMA + SMSEntry.baseDN;

    // For realms and co-existance support
    protected static final String COEXISTENCE_ATTR_NAME = "coexistenceMode";

    protected static final String REALM_ATTR_NAME = "realmMode";

    protected static final String REALM_SERVICE = 
        "sunidentityrepositoryservice";

    protected static final String DEFAULT_SERVICES_FOR_REALMS = 
        "serviceNamesForAutoAssignment";

    protected static final String SERVICE_VERSION = "1.0";

    protected static final String REALM_ENTRY = "ou=" + SERVICE_VERSION
            + ",ou=" + REALM_SERVICE + "," + serviceDN;

    protected static final String PLATFORM_SERVICE = "iPlanetAMPlatformService";

    protected static final String ATTR_SERVER_LIST = 
        "iplanet-am-platform-server-list";

    private static boolean realmCache;

    private static boolean coexistenceCache = true;

    private static boolean ditUpgradedCache;

    protected static Set defaultServicesToLoad;

    // constants for IdRepo management
    private static final String SERVICE_OC_ATTR_NAME = "serviceObjectClasses";

    private static final String ALL_SERVICES = "null";

    private static Map serviceNameAndOCs = new CaseInsensitiveHashMap();

    private static Map schemaAndServiceNames = new CaseInsensitiveHashMap();

    // List of sub-services
    protected static SMSEntry smsEntry;

    protected static CachedSubEntries serviceNames;

    protected static HashMap serviceVersions = new CaseInsensitiveHashMap();

    protected static HashMap serviceNameDefaultVersion = 
        new CaseInsensitiveHashMap();

    protected static Set accessManagerServers;

    // SSOToken of the caller
    private SSOToken token;

    private CachedSubEntries subEntries = null;

    // List of service schema managers
    protected HashMap serviceSchemaMgrs = new HashMap();

    // List of service config managers
    protected HashMap serviceConfigMgrs = new CaseInsensitiveHashMap();

    // List of organization config managers
    protected HashMap organizationConfigMgrs = new CaseInsensitiveHashMap();

    // Debug & I18n
    private static Debug debug = SMSEntry.debug;

    /**
     * Creates an instance of <code>ServiceManager</code>.
     * The <code>SSOToken</code> is used to identify the user performing
     * service operations.
     * 
     * @param token
     *            the authenticated single sign on token.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     *
     * @supported.api
     */
    public ServiceManager(SSOToken token) throws SSOException, SMSException {
        // Initilaize the static variables and caches
        initialize(token);

        // Validate SSOToken
        ssoTokenManager.validateToken(token);
        this.token = token;
    }

    /**
     * Returns the <code>ServiceSchemaManager</code> for
     * the given service name and version.
     * 
     * @param serviceName
     *            the name of the service
     * @param version
     *            the version of the service
     * @return the <code>ServiceSchemaManager</code> for the given service
     *         name and version
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     *
     * @supported.api
     */
    public ServiceSchemaManager getSchemaManager(String serviceName,
            String version) throws SMSException, SSOException {
        SMSEntry.validateToken(token);
        String cacheName = getCacheIndex(serviceName, version);
        ServiceSchemaManager ssm = (ServiceSchemaManager) serviceSchemaMgrs
                .get(cacheName);
        if (ssm == null) {
            ssm = new ServiceSchemaManager(token, serviceName, version);
            if (SMSEntry.cacheSMSEntries) {
                serviceSchemaMgrs.put(cacheName, ssm);
            }
        }
        return (ssm);
    }

    /**
     * Returns the <code>ServiceConfigManager</code> for
     * the given service name and version.
     * 
     * @param serviceName
     *            the name of the service
     * @param version
     *            the version of the service
     * @return the <code>ServiceConfigManager</code> for the given service
     *         name and version.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     *
     * @supported.api
     */
    public ServiceConfigManager getConfigManager(String serviceName,
            String version) throws SMSException, SSOException {
        SMSEntry.validateToken(token);
        String cacheName = getCacheIndex(serviceName, version);
        ServiceConfigManager scm = (ServiceConfigManager) serviceConfigMgrs
                .get(cacheName);
        if (scm == null) {
            scm = new ServiceConfigManager(token, serviceName, version);
            if (SMSEntry.cacheSMSEntries) {
                serviceConfigMgrs.put(cacheName, scm);
            }
        }
        return (scm);
    }

    /**
     * Returns the <code>OrganizationConfigManager</code> for the given
     * organization name. If the <code>orgName</code> either <code>
     * null</code>
     * or empty or "/", the organization configuration for the root organization
     * will be returned.
     * 
     * @param orgName
     *            the name of the organization
     * @return the <code>OrganizationConfigManager</code> for the given
     *         organization name
     * 
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     * @throws SMSException
     *             if an error occurred while performing the operation
     */
    public OrganizationConfigManager getOrganizationConfigManager(
            String orgName)
            throws SMSException, SSOException {

        SMSEntry.validateToken(token);
        OrganizationConfigManager ocm = 
            (OrganizationConfigManager) organizationConfigMgrs.get(orgName);
        if (ocm == null) {
            ocm = new OrganizationConfigManager(token, orgName);
            if (SMSEntry.cacheSMSEntries) {
                organizationConfigMgrs.put(orgName, ocm);
            }
        }
        return (ocm);
    }

    /**
     * Returns all the service names that have been
     * registered.
     * 
     * @return the set of names of services that have been registered
     * @throws SMSException
     *             if an error occurred while performing the operation
     *
     * @supported.api
     */
    public Set getServiceNames() throws SMSException {
        try {
            if (serviceNames == null) {
                serviceNames = CachedSubEntries.getInstance(token, serviceDN);
            }
            return (serviceNames.getSubEntries(token));
        } catch (SSOException s) {
            debug.error("ServiceManager: Unable to get service names", s);
            throw (new SMSException(s, "sms-service-not-found"));
        }
    }

    /**
     * Returns a map of service names and the related object classes
     * 
     * @return Map of service names and objectclasses
     */
    public Map getServiceNamesAndOCs() {
        return (getServiceNamesAndOCs(null));
    }

    /**
     * Returns a map of service names and the related object classes for the
     * given <code>schemaType</code>.
     * 
     * @param schemaType
     *            name of the schema
     * @return Map of service names and objectclasses
     */
    public Map getServiceNamesAndOCs(String schemaType) {
        if (schemaType == null) {
            schemaType = ALL_SERVICES;
        } else if (schemaType.equalsIgnoreCase("realm")) {
            schemaType = "filteredrole";
        }
        Map answer = (Map) serviceNameAndOCs.get(schemaType);
        if (answer == null) {
            try {
                answer = new HashMap();
                Set serviceNames = getServiceNames();
                if (serviceNames != null && !serviceNames.isEmpty()) {
                    Iterator it = serviceNames.iterator();
                    while (it.hasNext()) {
                        try {
                            String service = (String) it.next();
                            ServiceSchemaManagerImpl ssm = 
                                ServiceSchemaManagerImpl.getInstance(
                                        token, service,
                                            serviceDefaultVersion(token,
                                                    service));
                            if (ssm != null) {
                                // Check if service has schemaType
                                if (schemaType != ALL_SERVICES
                                        && ssm.getSchema(new SchemaType(
                                                schemaType)) == null) {
                                    // If the schema type is "User"
                                    // check for "Dynamic" also
                                    if (schemaType.equalsIgnoreCase(
                                            SMSUtils.USER_SCHEMA)
                                         && ssm.getSchema(SchemaType.DYNAMIC) 
                                         == null) 
                                    {
                                        continue;
                                    }
                                    // If the schema type is "Role:
                                    // check for "Dynamic" also
                                    if (schemaType.toLowerCase()
                                            .indexOf("role") != -1
                                            && ssm.getSchema(SchemaType.DYNAMIC)
                                            == null) 
                                    {
                                        continue;
                                    }
                                }
                                ServiceSchemaImpl ss = ssm
                                        .getSchema(SchemaType.GLOBAL);
                                if (ss != null) {
                                    Map attrs = ss.getAttributeDefaults();
                                    if (attrs.containsKey(SERVICE_OC_ATTR_NAME))
                                    {
                                        answer.put(service, attrs
                                                .get(SERVICE_OC_ATTR_NAME));
                                    }
                                }
                            }
                        } catch (SMSException smse) {
                            // continue with next service. Best effort to get
                            // all service names.
                            if (debug.messageEnabled()) {
                                debug.message(
                                        "ServiceManager.getServiceNamesandOCs"
                                          + " caught SMSException ", smse);
                            }
                        }
                    }
                }
                serviceNameAndOCs.put(schemaType, answer);
            } catch (SMSException smse) {
                // ignore
                if (debug.messageEnabled()) {
                    debug.message("ServiceManager.getServiceNamesandOCs"
                            + " caught SMSException ", smse);
                }
            } catch (SSOException ssoe) {
                // ignore
                if (debug.messageEnabled()) {
                    debug.message("ServiceManager.getServiceNamesandOCs"
                            + " caught SSOException ", ssoe);
                }
            }
        }
        return (SMSUtils.copyAttributes(answer));
    }

    /**
     * Returns all versions supported by the service.
     * 
     * @param serviceName
     *            service name.
     * @return the set of versions supported by the service
     * @throws SMSException
     *             if an error occurred while performing the operation
     *
     * @supported.api
     */
    public Set getServiceVersions(String serviceName) throws SMSException {
        try {
            return (getVersions(token, serviceName));
        } catch (SSOException s) {
            debug.error("ServiceManager: Unable to get service versions", s);
            throw (new SMSException(s, "sms-version-not-found"));
        }
    }

    /**
     * Registers one or more services, defined by the XML
     * input stream that follows the SMS DTD.
     * 
     * @param xmlServiceSchema
     *            the input stream of service metadata in XML conforming to SMS
     *            DTD.
     * @return set of registered service names.
     * @throws SMSException
     *             if an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     *
     * @supported.api
     */
    public Set registerServices(InputStream xmlServiceSchema)
            throws SMSException, SSOException {
        // Validate SSO Token
        SMSEntry.validateToken(token);
        Set sNames = new HashSet();
        // Get the XML document and get the list of service nodes
        Document doc = SMSSchema.getXMLDocument(xmlServiceSchema);

        if (!validSMSDtdDocType(doc)) {
            throw new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_xml_invalid_doc_type, null);
        }

        // Create service schema
        NodeList nodes = doc.getElementsByTagName(SMSUtils.SERVICE);
        for (int i = 0; (nodes != null) && (i < nodes.getLength()); i++) {
            Node serviceNode = nodes.item(i);
            String name = XMLUtils.getNodeAttributeValue(serviceNode,
                    SMSUtils.NAME);
            String version = XMLUtils.getNodeAttributeValue(serviceNode,
                    SMSUtils.VERSION);

            // Obtain the SMSSchema for Schema and PluginSchema
            SMSSchema smsSchema = new SMSSchema(name, version, doc);

            // Check if the schema element exists
            if (XMLUtils.getChildNode(serviceNode, SMSUtils.SCHEMA) != null) {
                // Before validating service schema, we need to check
                // for AttributeSchema having the syntax of "password"
                // and if present, encrypt the DefaultValues if any
                checkAndEncryptPasswordSyntax(doc);
                validateServiceSchema(serviceNode);
                ServiceSchemaManager.createService(token, smsSchema);

                // Update the service name and version cached SMSEntry
                if (serviceNames == null) {
                    serviceNames = CachedSubEntries.getInstance(token,
                            serviceDN);
                }
                serviceNames.add(name);
                CachedSubEntries sVersions = (CachedSubEntries) serviceVersions
                        .get(name);
                if (sVersions == null) {
                    // Not present, hence create it and add it
                    sVersions = CachedSubEntries.getInstance(token,
                            getServiceNameDN(name));
                    serviceVersions.put(name, sVersions);
                }
                sVersions.add(version);
                sNames.add(name);
            }

            // Check if PluginSchema nodes exists
            for (Iterator pluginNodes = XMLUtils.getChildNodes(serviceNode,
                    SMSUtils.PLUGIN_SCHEMA).iterator(); pluginNodes.hasNext();)
            {
                Node pluginNode = (Node) pluginNodes.next();
                PluginSchema.createPluginSchema(token, pluginNode, smsSchema);
            }

            // Check if configuration element exists
            Node configNode;
            if ((configNode = XMLUtils.getChildNode(serviceNode,
                    SMSUtils.CONFIGURATION)) != null) {
                // Store the configuration, will throw exception if
                // the service configuration already exists
                CreateServiceConfig.createService(this, name, version,
                        configNode);
            }
        }
        return sNames;
    }

    private boolean validSMSDtdDocType(Document doc) {
        boolean valid = false;
        DocumentType docType = doc.getDoctype();

        if (docType != null) {
            String dtdPath = docType.getSystemId();
            if (dtdPath != null) {
                int idx = dtdPath.lastIndexOf('/');
                if (idx != -1) {
                    dtdPath = dtdPath.substring(idx + 1);
                }
                valid = dtdPath.equals("sms.dtd");
            }
        }

        return valid;
    }

    /**
     * Removes the service schema and configuration for
     * the given service name.
     * 
     * @param serviceName
     *            the name of the service
     * @param version
     *            the version of the service
     * @throws SMSException
     *             if an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     *
     * @supported.api
     */
    public void removeService(String serviceName, String version)
            throws SMSException, SSOException {
        // Find all service entries that have the DN
        // Search for (&(ou=<serviceName>)(objectclass=top))
        // construct the rdn with the given version, look for the entry
        // in iDS and if entry exists(service with that version), delete.
        SMSEntry.validateToken(token);
        String[] objs = { serviceName };
        Iterator results = SMSEntry.search(
            MessageFormat.format(SMSEntry.FILTER_PATTERN, (Object[])objs))
            .iterator();
        while (results.hasNext()) {
            String dn = (String) results.next();
            String configdn = SMSEntry.PLACEHOLDER_RDN + SMSEntry.EQUALS
                    + version + SMSEntry.COMMA + dn;
            CachedSMSEntry configsmse = CachedSMSEntry.getInstance(token,
                    configdn, null);
            SMSEntry confige = configsmse.getClonedSMSEntry();
            if (!confige.isNewEntry()) {
                confige.delete(token);
                configsmse.refresh(confige);
            }
            // If there are no other service version nodes for that service,
            // delete that node(schema).
            CachedSMSEntry smse = CachedSMSEntry.getInstance(token, dn, null);
            SMSEntry e = smse.getSMSEntry();
            Iterator versions = 
                e.subEntries(token, "*", 0, false, false).iterator();
            if (!versions.hasNext()) {
                e.delete(token);
                smse.refresh(e);
            }
        }
    }

    /**
     * Deletes only the schema for the given service name. This is provided only
     * for backward compatibility for DSAME 5.0 and will be deprecated in the
     * future release. Alternative is to use
     * <code>ServiceSchemaManager.replaceSchema()</code>.
     * 
     * @param serviceName
     *            Name of service to be deleted.
     * @throws SMSException
     *             if an error occurred while performing the operation
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired
     */
    public void deleteService(String serviceName) throws SMSException,
            SSOException {
        Iterator versions = getServiceVersions(serviceName).iterator();
        while (versions.hasNext()) {
            String version = (String) versions.next();
            CachedSMSEntry ce = CachedSMSEntry.getInstance(token,
                    getServiceNameDN(serviceName, version), null);
            SMSEntry e = ce.getClonedSMSEntry();
            String[] values = { SMSSchema.getDummyXML(serviceName, version) };
            e.setAttribute(SMSEntry.ATTR_SCHEMA, values);
            e.save(token);
            ce.refresh(e);
        }
    }

    /**
     * Returns the base DN (or root DN) that was set in
     * <code>serverconfig.xml</code> at install time.
     */
    public static String getBaseDN() {
        return (SMSEntry.baseDN);
    }

    /**
     * Returns all AM Server instance. Read the configured servers from platform
     * service's <code>iplanet-am-platform-server-list</code>
     */
    public static Set getAMServerInstances() {
        // Check cache
        if (accessManagerServers == null) {
            // Get AdminToken
            try {
                SSOToken token = (SSOToken) AccessController
                        .doPrivileged(AdminTokenAction.getInstance());
                ServiceSchemaManagerImpl ssmi = ServiceSchemaManagerImpl
                        .getInstance(token, PLATFORM_SERVICE, SERVICE_VERSION);
                ServiceSchemaImpl ssi = ssmi.getSchema(SchemaType.GLOBAL);
                AttributeSchemaImpl as = ssi
                        .getAttributeSchema(ATTR_SERVER_LIST);
                Set values = as.getDefaultValues();
                Set answer = new HashSet();
                for (Iterator items = values.iterator(); items.hasNext();) {
                    String value = (String) items.next();
                    int index = value.indexOf("|");
                    if (index != -1) {
                        value = value.substring(0, index);
                    }
                    answer.add(value);
                }
                accessManagerServers = answer;
                if (debug.messageEnabled()) {
                    debug.message("ServiceManager.getAMServerInstances: "
                        + "server list: " + answer);
                }
            } catch (SMSException e) {
                if (debug.warningEnabled()) {
                    debug.warning("ServiceManager.getAMServerInstances: " +
                        "Unable to get server list", e);
                }
            } catch (SSOException e) {
                if (debug.warningEnabled()) {
                    debug.warning("ServiceManager.getAMServerInstances: " +
                        "Unable to get server list", e);
                }
            }
        }
        return (accessManagerServers == null ? new HashSet() : new HashSet(
                accessManagerServers));
    }

    /**
     * Returns organization names that match the given attribute name and
     * values. Only exact matching is supported, and if more than one value is
     * provided the organization must have all these values for the attribute.
     * Basically an AND is performed for attribute values for searching.
     * 
     * @param serviceName
     *            service name under which the attribute is to be sought.
     * @param attrName
     *            name of the attribute to search.
     * @param values
     *            set of attribute values to search.
     * @return organizations that match the attribute name and values.
     * @throws SMSException
     *             if an error occurred while performing the operation.
     * @throws SSOException
     *             if the user's single sign on token is invalid or expired.
     */
    public Set searchOrganizationNames(String serviceName, String attrName,
            Set values) throws SMSException, SSOException {

        try {
            if (subEntries == null) {
                subEntries = CachedSubEntries.getInstance(token,
                        SMSEntry.SERVICES_RDN + SMSEntry.COMMA
                                + SMSEntry.baseDN);
            }
            return (subEntries.searchOrgNames(token, serviceName.toLowerCase(),
                    attrName, values));
        } catch (SSOException ssoe) {
            debug.error("OrganizationConfigManagerImpl: Unable to "
                    + "get sub organization names", ssoe);
            throw (new SMSException(SMSEntry.bundle
                    .getString("sms-INVALID_SSO_TOKEN"),
                    "sms-INVALID_SSO_TOKEN"));
        }
    }

    /**
     * Removes all the SMS cached entries. This method
     * should be called to clear the cache for example, if ACIs for the SMS
     * entries are changed in the directory. Also, this clears the SMS entries
     * only in this JVM instance. If multiple instances (of JVM) are running
     * this method must be called within each instance.
     *
     * @supported.api
     */
    public synchronized void clearCache() {
        // Clear the local caches
        serviceNameAndOCs = new CaseInsensitiveHashMap();
        schemaAndServiceNames = new CaseInsensitiveHashMap();
        serviceVersions = new CaseInsensitiveHashMap();
        serviceNameDefaultVersion = new CaseInsensitiveHashMap();
        serviceSchemaMgrs = new HashMap();
        serviceConfigMgrs = new HashMap();

        // Call respective Impl classes
        CachedSMSEntry.clearCache();
        CachedSubEntries.clearCache();
        ServiceSchemaManagerImpl.clearCache();
        PluginSchemaImpl.clearCache();
        ServiceInstanceImpl.clearCache();
        ServiceConfigImpl.clearCache();
        OrgConfigViaAMSDK.clearCache();

        // Re-initialize the flags
        try {
            checkFlags(token);
            OrganizationConfigManager.initializeFlags();
            DNMapper.clearCache();
        } catch (Exception e) {
            debug.error("ServiceManager::clearCache unable to " +
                "re-initialize global flags", e);
        }
    }

    /**
     * Returns the flag which lets IdRepo and SM know that we are running in the
     * co-existence mode.
     * 
     * @return true or false depending on if the coexistence flag is enabled or
     *         not.
     */
    public static boolean isCoexistenceMode() {
        isRealmEnabled();
        return (coexistenceCache);
    }

    /**
     * Returns <code>true</code> if current service
     * configuration uses the realm model to store the configuration data.
     * 
     * @return <code>true</code> is realm model is used for storing
     *         configuration data; <code>false</code> otherwise.
     *
     * @supported.api
     */
    public static boolean isRealmEnabled() {
        if (!initialized) {
            try {
                initialize((SSOToken) AccessController
                        .doPrivileged(AdminTokenAction.getInstance()));
            } catch (Exception ssme) {
                debug.error("ServiceManager::isRealmEnabled unable to "
                        + "initialize", ssme);
            }
        }
        return (realmCache);
    }

    /**
     * Returns <code>true</code> if configuration data has been migrated to
     * Access Manager 7.0. Else <code>false</code> otherwise.
     * 
     * @return <code>true</code> if configuration data has been migrated to AM
     *         7.0; <code>false</code> otherwise
     */
    public static boolean isConfigMigratedTo70() {
        isRealmEnabled();
        return (ditUpgradedCache);
    }

    // ------------------------------------------------------------
    // Protected methods
    // ------------------------------------------------------------

    // Called by CreateServiceConfig.java to create LDAP entries
    SSOToken getSSOToken() {
        return (token);
    }

    protected static String getCacheIndex(String serviceName, String version) {
        StringBuffer sb = new StringBuffer(20);
        return (
            sb.append(serviceName).append(version).toString().toLowerCase());
    }

    protected static String getServiceNameDN(String serviceName) {
        StringBuffer sb = new StringBuffer(100);
        sb.append(SMSEntry.PLACEHOLDER_RDN).append(SMSEntry.EQUALS).append(
                serviceName).append(SMSEntry.COMMA).append(serviceDN);
        return (sb.toString());
    }

    protected static String getServiceNameDN(String serviceName, String version)
    {
        StringBuffer sb = new StringBuffer(100);
        sb.append(SMSEntry.PLACEHOLDER_RDN).append(SMSEntry.EQUALS).append(
                version).append(SMSEntry.COMMA).append(
                getServiceNameDN(serviceName));
        return (sb.toString());
    }

    protected static Set getVersions(SSOToken token, String serviceName)
            throws SMSException, SSOException {
        CachedSubEntries sVersions = (CachedSubEntries) serviceVersions
                .get(serviceName);
        if (sVersions == null) {
            sVersions = CachedSubEntries.getInstance(token,
                    getServiceNameDN(serviceName));
            if (sVersions == null || sVersions.getSMSEntry().isNewEntry()
                    || sVersions.getSubEntries(token).isEmpty()) {
                String[] msgs = { serviceName };
                throw (new ServiceNotFoundException(
                        IUMSConstants.UMS_BUNDLE_NAME,
                        IUMSConstants.SMS_service_does_not_exist, msgs));
            }
            serviceVersions.put(serviceName, sVersions);
        }
        return (sVersions.getSubEntries(token));
    }

    protected static String serviceDefaultVersion(SSOToken token,
            String serviceName) throws SMSException, SSOException {
        String version = (String) serviceNameDefaultVersion.get(serviceName);
        if (version == null) {
            Iterator iter = getVersions(token, serviceName).iterator();
            if (iter.hasNext()) {
                version = (String) iter.next();
            } else {
                String msgs[] = { serviceName };
                throw (new ServiceNotFoundException(
                        IUMSConstants.UMS_BUNDLE_NAME,
                        IUMSConstants.SMS_service_does_not_exist, msgs));
            }
            serviceNameDefaultVersion.put(serviceName, version);
        }
        return (version);
    }

    protected static void checkServiceNameAndVersion(
        SSOToken t,
        String serviceName,
        String version
    ) throws SMSException, SSOException {
        Set versions = getVersions(t, serviceName);
        if ((versions == null) || !versions.contains(version)) {
            String[] msgs = { serviceName };
            throw (new ServiceNotFoundException(IUMSConstants.UMS_BUNDLE_NAME,
                    IUMSConstants.SMS_service_does_not_exist, msgs));
        }
    }

    protected static void checkAndEncryptPasswordSyntax(Document doc)
            throws SMSException {
        // Get the node list of all AttributeSchema
        NodeList nl = doc.getElementsByTagName(SMSUtils.SCHEMA_ATTRIBUTE);
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            // Check if the "syntax" attribute is "password"
            String syntax = XMLUtils.getNodeAttributeValue(node,
                    SMSUtils.ATTRIBUTE_SYNTAX);
            if (syntax.equals(AttributeSchema.Syntax.PASSWORD.toString())) {
                if (debug.messageEnabled()) {
                    debug.message("ServiceManager: encrypting password syntax");
                }
                // Get the DefaultValues and encrypt then
                Node defaultNode;
                if ((defaultNode = XMLUtils.getChildNode(node,
                        SMSUtils.ATTRIBUTE_DEFAULT_ELEMENT)) != null) {
                    // Get NodeList of "Value" nodes and encrypt them
                    for (Iterator items = XMLUtils.getChildNodes(defaultNode,
                            SMSUtils.ATTRIBUTE_VALUE).iterator(); items
                            .hasNext();) {
                        Node valueNode = (Node) items.next();
                        String value = XMLUtils.getValueOfValueNode(valueNode);
                        // Encrypt it
                        String encValue = (String) AccessController
                                .doPrivileged(new EncodeAction(value));
                        // Construct the encrypted "Value" node
                        StringBuffer sb = new StringBuffer(100);
                        sb.append(AttributeSchema.VALUE_BEGIN).append(encValue)
                                .append(AttributeSchema.VALUE_END);
                        Document newDoc = SMSSchema.getXMLDocument(sb
                                .toString(), false);
                        Node newValueNode = XMLUtils.getRootNode(newDoc,
                                SMSUtils.ATTRIBUTE_VALUE);
                        // Replace the node
                        Node nValueNode = doc.importNode(newValueNode, true);
                        defaultNode.replaceChild(nValueNode, valueNode);
                    }
                }
            }
        }
    }

    protected static boolean validateServiceSchema(Node serviceNode)
            throws SMSException {
        Node schemaRoot = XMLUtils.getChildNode(serviceNode, SMSUtils.SCHEMA);
        String[] schemaNames = { SMSUtils.GLOBAL_SCHEMA, SMSUtils.ORG_SCHEMA,
                SMSUtils.DYNAMIC_SCHEMA, SMSUtils.USER_SCHEMA,
                SMSUtils.POLICY_SCHEMA, SMSUtils.GROUP_SCHEMA,
                SMSUtils.DOMAIN_SCHEMA };
        for (int i = 0; i < schemaNames.length; i++) {
            Node childNode = XMLUtils.getChildNode(schemaRoot, schemaNames[i]);
            if (childNode != null) {
                ServiceSchemaImpl ssi = new ServiceSchemaImpl(null, childNode);
                Map attrs = ssi.getAttributeDefaults();
                ssi.validateAttributes(attrs, false);
            }
        }
        return (true);
    }

    // Gets called by OrganizationConfigManager when service schema has changed
    protected static void schemaChanged() {
        // Reset the service names and OCs used by IdRepo
        serviceNameAndOCs = new CaseInsensitiveHashMap();
        // Reset the schema types and service names
        schemaAndServiceNames = new CaseInsensitiveHashMap();
        // Reset the service names
        serviceNames = null;
    }

    /**
     * Returns service names that will be assigned to a realm during creation.
     */
    public static Set servicesAssignedByDefault() {
        if (!loadedAuthServices) {
            AuthenticationServiceNameProvider provider = 
                AuthenticationServiceNameProviderFactory.getProvider();
            defaultServicesToLoad.addAll(provider
                    .getAuthenticationServiceNames());
            if (debug.messageEnabled()) {
                debug.message("ServiceManager::servicesAssignedByDefault:"
                        + "defaultServicesToLoad = " + defaultServicesToLoad);
            }
            loadedAuthServices = true;
            defaultServicesToLoad = Collections
                    .unmodifiableSet(defaultServicesToLoad);
        }
        return (defaultServicesToLoad);
    }

    static void initialize(SSOToken token) throws SMSException, SSOException {
        // Validate SSOToken
        SMSEntry.validateToken(token);

        // Check if already initialized
        if (initialized)
            return;

        // Initilaize the parameters
        try {
            // Get the service names and cache it
            serviceNames = CachedSubEntries.getInstance(token, serviceDN);
            if (serviceNames.getSMSEntry().isNewEntry()) {
                if (debug.warningEnabled()) {
                    debug.warning("SeviceManager:: Root service node "
                            + "does not exists: " + serviceDN);
                }
                String[] msgs = new String[1];
                msgs[0] = serviceDN;
                throw (new SMSException(IUMSConstants.UMS_BUNDLE_NAME,
                        IUMSConstants.SMS_services_node_does_not_exist, msgs));
            }
        } catch (SMSException e) {
            debug.error("ServiceManager::unable to get " + "services node: "
                    + serviceDN, e);
            throw (e);
        }

        // Check if realm is enabled and set appropriate flags
        checkFlags(token);
        initialized = true;
    }

    static void checkFlags(SSOToken token) throws SMSException, SSOException {
        try {
            CachedSMSEntry entry = CachedSMSEntry.getInstance(token,
                    REALM_ENTRY, null);
            if (!entry.isNewEntry()) {
                ditUpgradedCache = true;
                ServiceConfigManagerImpl ssm = ServiceConfigManagerImpl
                        .getInstance(token, REALM_SERVICE, SERVICE_VERSION);
                ServiceConfigImpl sc = null;
                Map map = null;
                if (ssm == null
                        || (sc = ssm.getGlobalConfig(token, null)) == null
                        || (map = sc.getAttributes()) == null) {
                    return;
                }
                Set coexistEntry = (Set) map.get(COEXISTENCE_ATTR_NAME);
                if (coexistEntry != null && coexistEntry.contains("false")) {
                    coexistenceCache = false;
                }
                Set realmEntry = (Set) map.get(REALM_ATTR_NAME);
                if (realmEntry != null && realmEntry.contains("true")) {
                    realmCache = true;
                }
                // Get the default services to be loaded
                defaultServicesToLoad = (Set) map
                        .get(DEFAULT_SERVICES_FOR_REALMS);
            }
            if (debug.messageEnabled()) {
                debug.message("ServiceManager::checkFlags:realmEnabled="
                        + realmCache);
                debug.message("ServiceManager::checkFlags:coexistenceMode="
                        + coexistenceCache);
            }
        } catch (SMSException e) {
            debug.error("ServiceManager::unable to check "
                    + "if Realm is enabled: ", e);
            throw (e);
        }
    }
}
