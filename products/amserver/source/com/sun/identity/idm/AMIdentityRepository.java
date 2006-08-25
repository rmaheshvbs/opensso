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
 * $Id: AMIdentityRepository.java,v 1.10 2006-08-25 21:20:46 veiming Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.idm;

import com.iplanet.am.sdk.AMHashMap;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.DNUtils;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.ServiceManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.security.auth.callback.Callback;
import netscape.ldap.LDAPDN;
import netscape.ldap.util.DN;

/**
 * The class <code> AMIdentityRepository </code> represents an object to access
 * the repositories in which user/role/group and other identity data is
 * configured. This class provides access to methods which will search, create
 * and delete identities. An instance of this class can be obtained in the
 * following manner:
 * <p>
 * 
 * <PRE>
 * 
 * AMIdentityRepository = new AMIdentityRepository(ssoToken, realmName);
 * 
 * </PRE>
 * 
 */
public final class AMIdentityRepository {
    private SSOToken token;

    private String org;

    private IdRepo pluginClass;

    public static Debug debug = Debug.getInstance("amIdm");

    public static Map listeners = new CaseInsensitiveHashMap();

    /**
     * iPlanet-PUBLIC-METHOD
     * 
     * Constructor for the <code>AMIdentityRepository</code> object. If a null
     * is passed for the organization identifier <code>realmName</code>, then
     * the "root" realm is assumed.
     * 
     * @param ssotoken
     *            Single sign on token of the user
     * @param realmName
     *            Name of the realm (can be a Fully qualified DN)
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public AMIdentityRepository(SSOToken ssotoken, String realmName)
            throws IdRepoException, SSOException {
        token = ssotoken;
        org = DNMapper.orgNameToDN(realmName);
    }

    /**
     * iPlanet-PUBLIC-METHOD
     * 
     * Returns the set of supported object types <code>IdType</code> for this
     * deployment. This is not realm specific.
     * 
     * @return Set of supported <code> IdType </code> objects.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public Set getSupportedIdTypes() throws IdRepoException, SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Set res = idServices.getSupportedTypes(token, org);
        res.remove(IdType.REALM);
        return res;
    }

    /**
     * iPlanet-PUBLIC-METHOD
     * 
     * Returns the set of Operations for a given <code>IdType</code>,
     * <code>IdOperations</code> that can be performed on an Identity. This
     * varies for each organization (and each plugin?).
     * 
     * @param type
     *            Type of identity
     * @return Set of <code>IdOperation</code> objects.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public Set getAllowedIdOperations(IdType type) throws IdRepoException,
            SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return idServices.getSupportedOperations(token, type, org);

    }

    /**
     * iPlanet-PUBLIC-METHOD
     * 
     * Return the special identities for this realm for a given type. These
     * identities cannot be deleted and hence have to be shown in the admin
     * console as non-deletable.
     * 
     * @param type
     *            Type of the identity
     * @return IdSearchResult
     * @throws IdRepoException
     *             if there is a datastore exception
     * @throws SSOException
     *             if the user's single sign on token is not valid.
     */
    public IdSearchResults getSpecialIdentities(IdType type)
            throws IdRepoException, SSOException {

        IdSearchResults results = getSpecialIdentities(token, type, org);

        if (type.equals(IdType.USER)) {
            // Iterating through to get out the names and remove only amadmin
            // anonymous as per AM console requirement.

            IdSearchResults newResults = new IdSearchResults(type, org);
            Set identities = results.getSearchResults();
            if ((identities != null) && !identities.isEmpty()) {
                for (Iterator i = identities.iterator(); i.hasNext();) {
                    AMIdentity amid = ((AMIdentity) i.next());
                    String remUser = amid.getName().toLowerCase();
                    if (!remUser.equalsIgnoreCase(IdConstants.AMADMIN_USER)
                            && !remUser.equalsIgnoreCase(
                                    IdConstants.ANONYMOUS_USER)) 
                    {
                        newResults.addResult(amid, amid.getAttributes());
                    }
                }
                results = newResults;
            }
        }
        return results;
    }

    /**
     * Searches for identities of a certain type. The iterator returns
     * AMIdentity objects for use by the application.
     * 
     * @deprecated This method is deprecated. Use
     *             {@link #searchIdentities(IdType type,String pattern,
     *             IdSearchControl ctrl)}
     * @param type
     *            Type of identity being searched for.
     * @param pattern
     *            Search pattern, like "a*" or "*".
     * @param avPairs
     *            Map of attribute-values which can further help qualify the
     *            search pattern.
     * @param recursive
     *            If true, then the search is performed on the entire subtree
     *            (if applicable)
     * @param maxResults
     *            Maximum number of results to be returned. A -1 means no limit
     *            on the result set.
     * @param maxTime
     *            Maximum amount of time after which the search should return
     *            with partial results.
     * @param returnAttributes
     *            Set of attributes to be read when performing the search.
     * @param returnAllAttributes
     *            If true, then read all the attributes of the entries.
     * @return results containing <code>AMIdentity</code> objects.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public IdSearchResults searchIdentities(IdType type, String pattern,
            Map avPairs, boolean recursive, int maxResults, int maxTime,
            Set returnAttributes, boolean returnAllAttributes)
            throws IdRepoException, SSOException {
        // DelegationEvaluator de = new DelegationEvaluator();
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        IdSearchControl crtl = new IdSearchControl();
        crtl.setSearchModifiers(IdSearchOpModifier.OR, avPairs);
        crtl.setRecursive(recursive);
        crtl.setMaxResults(maxResults);
        crtl.setTimeOut(maxTime);
        crtl.setReturnAttributes(returnAttributes);
        crtl.setAllReturnAttributes(returnAllAttributes);
        
        // Call search method that takes IdSearchControl
        return searchIdentities(type, pattern, crtl);
    }

    /**
     * iPlanet-PUBLIC-METHOD
     * 
     * Searches for identities of certain types from each plugin and returns a
     * combined result
     * 
     * @param type
     *            Type of identity being searched for.
     * @param pattern
     *            Pattern to be used when searching.
     * @param ctrl
     *            IdSearchControl which can be used to set up various search
     *            controls on the search to be performed.
     * @return Returns the combines results in an object IdSearchResults.
     * @see com.sun.identity.idm.IdSearchControl
     * @see com.sun.identity.idm.IdSearchResults
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public IdSearchResults searchIdentities(IdType type, String pattern,
            IdSearchControl ctrl) throws IdRepoException, SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return idServices.search(token, type, pattern, ctrl, org);
    }

    /**
     * iPlanet-PUBLIC-METHOD
     * 
     * Returns a handle of the Identity object representing this realm for
     * services related operations only. This <code> AMIdentity
     * </code> object
     * can be used to assign and unassign services containing dynamic attributes
     * to this realm
     * 
     * @return a handle of the Identity object.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public AMIdentity getRealmIdentity() throws IdRepoException, SSOException {
        String univId = "id=ContainerDefaultTemplateRole,ou=realm," + org;
        return IdUtils.getIdentity(token, univId);
    }

    /**
     * iPlanet-PUBLIC-METHOD
     * 
     * Creates a single object of a type. The object is created in all the
     * plugins that support creation of this type of object.
     * 
     * This method is only valid for IdType Agent, Realm, and User.
     * 
     * @param type
     *            Type of object to be created.
     * @param idName
     *            Name of object
     * @param attrMap
     *            Map of attribute-values to be set when creating the entry.
     * @return Identity object representing the newly created entry.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public AMIdentity createIdentity(IdType type, String idName, Map attrMap)
            throws IdRepoException, SSOException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return idServices.create(token, type, idName, attrMap, org);
    }

    /**
     * iPlanet-PUBLIC-METHOD
     * 
     * Creates multiple objects of the same type. The objects are created in all
     * the <code>IdRepo</code> plugins that support creation of these objects.
     * 
     * This method is only valid for IdType Agent, Realm, and User.
     * 
     * @param type
     *            Type of object to be created
     * @param identityNamesAndAttrs
     *            Names of the identities and their
     * @return Set of created Identities.
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public Set createIdentities(IdType type, Map identityNamesAndAttrs)
            throws IdRepoException, SSOException {
        Set results = new HashSet();

        if (identityNamesAndAttrs == null || identityNamesAndAttrs.isEmpty()) {
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }

        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Iterator it = identityNamesAndAttrs.keySet().iterator();

        while (it.hasNext()) {
            String name = (String) it.next();
            Map attrMap = (Map) identityNamesAndAttrs.get(name);
            AMIdentity id = idServices.create(token, type, name, attrMap, org);
            results.add(id);
        }

        return results;
    }

    /**
     * iPlanet-PUBLIC-METHOD
     * 
     * Deletes identities. The Set passed is a set of AMIdentity objects.
     * 
     * This method is only valid for IdType Agent, Realm, and User.
     * 
     * @param type
     *            Type of Identity to be deleted.
     * @param identities
     *            Set of AMIdentity objects to be deleted
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     * @deprecated As of release AM 7.1, replaced by
     *             {@link #deleteIdentities(Set identities)}
     */
    public void deleteIdentities(IdType type, Set identities)
            throws IdRepoException, SSOException {
        deleteIdentities(identities);
    }

    /**
     * iPlanet-PUBLIC-METHOD
     * 
     * Deletes identities. The Set passed is a set of AMIdentity objects.
     * 
     * This method is only valid for IdType Agent, Realm, and User.
     * 
     * @param identities
     *            Set of AMIDentity objects to be deleted
     * @throws IdRepoException
     *             if there are repository related error conditions.
     * @throws SSOException
     *             if user's single sign on token is invalid.
     */
    public void deleteIdentities(Set identities) throws IdRepoException,
            SSOException {
        if (identities == null || identities.isEmpty()) {
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "201", null);
        }

        IdServices idServices = IdServicesFactory.getDataStoreServices();
        Iterator it = identities.iterator();
        while (it.hasNext()) {
            AMIdentity id = (AMIdentity) it.next();
            idServices.delete(token, id.getType(), id.getName(), org, id
                    .getDN());
        }
    }

    /**
     * Non-javadoc, non-public methods Returns <code>true</code> if the data
     * store has successfully authenticated the identity with the provided
     * credentials. In case the data store requires additional credentials, the
     * list would be returned via the <code>IdRepoException</code> exception.
     * 
     * @param credentials
     *            Array of callback objects containing information such as
     *            username and password.
     * 
     * @return <code>true</code> if data store authenticates the identity;
     *         else <code>false</code>
     */
    public boolean authenticate(Callback[] credentials) throws IdRepoException,
            com.sun.identity.authentication.spi.AuthLoginException {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        return (idServices.authenticate(org, credentials));
    }

    /**
     * iPlanet-PUBLIC-METHOD
     * 
     * Adds a listener, which should receive notifications for all changes that
     * occurred in this organization.
     * 
     * This method is only valid for IdType User and Agent.
     * 
     * @param listener
     *            The callback which implements <code>AMEventListener</code>.
     * @return Integer identifier for this listener.
     */
    public int addEventListener(IdEventListener listener) {
        ArrayList listOfListeners = (ArrayList) listeners.get(org);
        if (listOfListeners == null) {
            listOfListeners = new ArrayList();
        }
        synchronized (listeners) {
            listOfListeners.add(listener);
            listeners.put(org, listOfListeners);
        }
        return (listOfListeners.size() - 1);
    }

    /**
     * iPlanet-PUBLIC-METHOD
     * 
     * Removes listener as the application is no longer interested in receiving
     * notifications.
     * 
     * @param identifier
     *            Integer identifying the listener.
     */
    public void removeEventListener(int identifier) {
        ArrayList listOfListeners = (ArrayList) listeners.get(org);
        if (listOfListeners != null) {
            synchronized (listeners) {
                listOfListeners.remove(identifier);
            }
        }
    }

    /**
     * iPlanet-PUBLIC-METHOD
     * 
     * Clears the cache.
     */
    public static void clearCache() {
        IdServices idServices = IdServicesFactory.getDataStoreServices();
        idServices.reinitialize();
        IdUtils.initialize();
    }


    public IdSearchResults getSpecialIdentities(SSOToken token, IdType type,
            String orgName) throws IdRepoException, SSOException {
        Set pluginClasses = new OrderedSet();
        IdRepo thisPlugin = null;
        if (ServiceManager.isConfigMigratedTo70()
                && ServiceManager.getBaseDN().equalsIgnoreCase(orgName)) {
            // add the "SpecialUser plugin
            String p = IdConstants.SPECIAL_PLUGIN;
            if (pluginClass == null) {
                try {

                    Class thisClass = Class.forName(p);
                    thisPlugin = (IdRepo) thisClass.newInstance();
                    thisPlugin.initialize(new HashMap());
                    Map listenerConfig = new HashMap();
                    listenerConfig.put("realm", orgName);
                    IdRepoListener lter = new IdRepoListener();
                    lter.setConfigMap(listenerConfig);
                    thisPlugin.addListener(token, lter);
                    pluginClass = thisPlugin;
                    Set opSet = thisPlugin.getSupportedOperations(type);
                    if (opSet != null && opSet.contains(IdOperation.READ)) {
                        pluginClasses.add(thisPlugin);
                    }
                } catch (Exception e) {
                    // Throw an Exception !!
                    debug.error("Unable to instantiate plugin: " + p, e);
                }
            } else {
                Set opSet = pluginClass.getSupportedOperations(type);
                if (opSet != null && opSet.contains(IdOperation.READ)) {
                    pluginClasses.add(pluginClass);
                }
            }
        }
        if (pluginClasses.isEmpty()) {
            return new IdSearchResults(type, orgName);
        } else {
            IdRepo specialRepo = (IdRepo) pluginClasses.iterator().next();
            RepoSearchResults res = specialRepo.search(token, type, "*", 0, 0,
                    Collections.EMPTY_SET, false, 0, Collections.EMPTY_MAP,
                    false);
            Object obj[][] = new Object[1][2];
            obj[0][0] = res;
            obj[0][1] = Collections.EMPTY_MAP;
            return combineSearchResults(token, obj, 1, type, orgName, false,
                    null);
        }
    }
    
    /**
     * Return String representation of the <code>AMIdentityRepository
     * </code> object. It returns realm name.
     *
     * @return String representation of <code>AMIdentityRepository</code>
     * object.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(100);
        sb.append("AMIdentityRepository object: ")
            .append(org);
        return (sb.toString());
    }

    // TODO:
    // FIXME: Move these utilities to a util class
    private Map reverseMapAttributeNames(Map attrMap, Map configMap) {
        if (attrMap == null || attrMap.isEmpty()) {
            return attrMap;
        }
        Map resultMap;
        Map[] mapArray = getAttributeNameMap(configMap);
        if (mapArray == null) {
            resultMap = attrMap;
        } else {
            resultMap = new CaseInsensitiveHashMap();
            Map reverseMap = mapArray[1];
            Iterator it = attrMap.keySet().iterator();
            while (it.hasNext()) {
                String curr = (String) it.next();
                if (reverseMap.containsKey(curr)) {
                    resultMap.put((String) reverseMap.get(curr), (Set) attrMap
                            .get(curr));
                } else {
                    resultMap.put(curr, (Set) attrMap.get(curr));
                }
            }
        }
        return resultMap;
    }

    private IdSearchResults combineSearchResults(SSOToken token,
            Object[][] arrayOfResult, int sizeOfArray, IdType type,
            String orgName, boolean amsdkIncluded, Object[][] amsdkResults) {
        Map amsdkDNs = new CaseInsensitiveHashMap();
        Map resultsMap = new CaseInsensitiveHashMap();
        int errorCode = IdSearchResults.SUCCESS;
        if (amsdkIncluded) {
            RepoSearchResults amsdkRepoRes = (RepoSearchResults) 
                amsdkResults[0][0];
            Set results = amsdkRepoRes.getSearchResults();
            Map attrResults = amsdkRepoRes.getResultAttributes();
            Iterator it = results.iterator();
            while (it.hasNext()) {
                String dn = (String) it.next();
                String name = LDAPDN.explodeDN(dn, true)[0];
                amsdkDNs.put(name, dn);
                Set attrMaps = new HashSet();
                attrMaps.add((Map) attrResults.get(dn));
                resultsMap.put(name, attrMaps);
            }
            errorCode = amsdkRepoRes.getErrorCode();
        }
        for (int i = 0; i < sizeOfArray; i++) {
            RepoSearchResults current = (RepoSearchResults) arrayOfResult[i][0];
            Map configMap = (Map) arrayOfResult[i][1];
            Iterator it = current.getSearchResults().iterator();
            Map allAttrMaps = current.getResultAttributes();
            while (it.hasNext()) {
                String m = (String) it.next();
                String mname = DNUtils.DNtoName(m);
                Map attrMap = (Map) allAttrMaps.get(m);
                attrMap = reverseMapAttributeNames(attrMap, configMap);
                Set attrMaps = (Set) resultsMap.get(mname);
                if (attrMaps == null) {
                    attrMaps = new HashSet();
                }
                attrMaps.add(attrMap);
                resultsMap.put(mname, attrMaps);
            }
        }
        IdSearchResults results = new IdSearchResults(type, orgName);
        Iterator it = resultsMap.keySet().iterator();
        while (it.hasNext()) {
            String mname = (String) it.next();
            Map combinedMap = combineAttrMaps((Set) resultsMap.get(mname), 
                    true);
            AMIdentity id = new AMIdentity(token, mname, type, orgName,
                    (String) amsdkDNs.get(mname));
            results.addResult(id, combinedMap);
        }
        results.setErrorCode(errorCode);
        return results;
    }

    private Map[] getAttributeNameMap(Map configMap) {
        Set attributeMap = (Set) configMap.get(IdConstants.ATTR_MAP);

        if (attributeMap == null || attributeMap.isEmpty()) {
            return null;
        } else {
            Map returnArray[] = new Map[2];
            int size = attributeMap.size();
            returnArray[0] = new CaseInsensitiveHashMap(size);
            returnArray[1] = new CaseInsensitiveHashMap(size);
            Iterator it = attributeMap.iterator();
            while (it.hasNext()) {
                String mapString = (String) it.next();
                int eqIndex = mapString.indexOf('=');
                if (eqIndex > -1) {
                    String first = mapString.substring(0, eqIndex);
                    String second = mapString.substring(eqIndex + 1);
                    returnArray[0].put(first, second);
                    returnArray[1].put(second, first);
                } else {
                    returnArray[0].put(mapString, mapString);
                    returnArray[1].put(mapString, mapString);
                }
            }
            return returnArray;
        }
    }

    private Map combineAttrMaps(Set setOfMaps, boolean isString) {
        // Map resultMap = new CaseInsensitiveHashMap();
        Map resultMap = new AMHashMap();
        Iterator it = setOfMaps.iterator();
        while (it.hasNext()) {
            Map currMap = (Map) it.next();
            if (currMap != null) {
                Iterator keyset = currMap.keySet().iterator();
                while (keyset.hasNext()) {
                    String thisAttr = (String) keyset.next();
                    if (isString) {
                        Set resultSet = (Set) resultMap.get(thisAttr);
                        Set thisSet = (Set) currMap.get(thisAttr);
                        if (resultSet != null) {
                            resultSet.addAll(thisSet);
                        } else {
                            /*
                             * create a new Set so that we do not alter the set
                             * that is referenced in setOfMaps
                             */
                            resultSet = new HashSet((Set) 
                                    currMap.get(thisAttr));
                            resultMap.put(thisAttr, resultSet);
                        }
                    } else { // binary attributes

                        byte[][] resultSet = (byte[][]) resultMap.get(thisAttr);
                        byte[][] thisSet = (byte[][]) currMap.get(thisAttr);
                        int combinedSize = thisSet.length;
                        if (resultSet != null) {
                            combinedSize = resultSet.length + thisSet.length;
                            byte[][] tmpSet = new byte[combinedSize][];
                            for (int i = 0; i < resultSet.length; i++) {
                                tmpSet[i] = (byte[]) resultSet[i];
                            }
                            for (int i = 0; i < thisSet.length; i++) {
                                tmpSet[i] = (byte[]) thisSet[i];
                            }
                            resultSet = tmpSet;
                        } else {
                            resultSet = (byte[][]) thisSet.clone();
                        }
                        resultMap.put(thisAttr, resultSet);

                    }

                }
            }
        }
        return resultMap;
    }
}
