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
 * $Id: WSFederationCOTUtils.java,v 1.2 2007-08-01 21:04:38 superpat7 Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.wsfederation.meta;

import javax.xml.bind.JAXBException;
import java.util.Iterator;
import java.util.List;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.wsfederation.jaxb.entityconfig.AttributeType;
import com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement;
import com.sun.identity.wsfederation.jaxb.entityconfig.ObjectFactory;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;

/**
 * <code>WSFederationCOTUtils</code> provides utility methods to update
 * the WS-Federation Entity Configuration <code>cotlist</code> attributes
 * in the Service and Identity Provider configurations.
 */
public class WSFederationCOTUtils {
    
    private static Debug debug = WSFederationMetaUtils.debug;
    
    /*
     * Private constructor ensure that no instance is ever created
     */
    private WSFederationCOTUtils()  {
        
    }
    
    /**
     * Updates the entity config to add the circle of trust name to the
     * <code>cotlist</code> attribute. The Service Provider and Identity
     * Provider Configuration are updated.
     *
     * @param realm the realm name where the entity configuration is.
     * @param name the circle of trust name.
     * @param entityId the name of the Entity identifier.
     * @throws WSFederationMetaException if there is a configuration error when
     *         updating the configuration.
     * @throws JAXBException is there is an error updating the entity
     *          configuration.
     */
    public static void updateEntityConfig(String realm, String name, 
        String entityId)
    throws WSFederationMetaException, JAXBException {
        String classMethod = "WSFederationCOTUtils.updateEntityConfig: ";
        ObjectFactory objFactory = new ObjectFactory();
        // Check whether the entity id existed in the DS
        FederationElement edes = WSFederationMetaManager.getEntityDescriptor(
                realm, entityId);
        if (edes == null) {
            debug.error(classMethod +"No such entity: " + entityId);
            String[] data = {realm, entityId};
            throw new WSFederationMetaException("entityid_invalid", data);
        }
        FederationConfigElement eConfig = 
            WSFederationMetaManager.getEntityConfig(realm, entityId);
        if (eConfig == null) {
            BaseConfigType bctype = null;
            AttributeType atype = objFactory.createAttributeType();
            atype.setName(SAML2Constants.COT_LIST);
            atype.getValue().add(name);
            // add to eConfig
            FederationConfigElement ele = 
                objFactory.createFederationConfigElement();
            ele.setFederationID(entityId);
            ele.setHosted(false);
            List ll =
                    ele.getIDPSSOConfigOrSPSSOConfig();
            // Decide which role EntityDescriptorElement includes
            // Right now, it is either an SP or an IdP
            // IdP will have a token signing cert
            if (WSFederationMetaManager.getTokenSigningCertificate(edes) != 
                null) {
                bctype = objFactory.createIDPSSOConfigElement();
                bctype.getAttribute().add(atype);
                ll.add(bctype);
            } else {
                bctype = objFactory.createSPSSOConfigElement();
                bctype.getAttribute().add(atype);
                ll.add(bctype);
            }
            WSFederationMetaManager.setEntityConfig(realm,ele);
        } else {
            List elist = eConfig.
                    getIDPSSOConfigOrSPSSOConfig();
            for (Iterator iter = elist.iterator(); iter.hasNext();) {
                BaseConfigType bConfig = (BaseConfigType)iter.next();
                List list = bConfig.getAttribute();
                boolean foundCOT = false;
                for (Iterator iter2 = list.iterator(); iter2.hasNext();) {
                    AttributeType avp = (AttributeType)iter2.next();
                    if (avp.getName().trim().equalsIgnoreCase(
                            SAML2Constants.COT_LIST)) {
                        foundCOT = true;
                        List avpl = avp.getValue();
                        if (avpl.isEmpty() ||!containsValue(avpl,name)) {
                            avpl.add(name);
                            WSFederationMetaManager.setEntityConfig(realm,
                                eConfig);
                            break;
                        }
                    }
                }
                // no cot_list in the original entity config
                if (!foundCOT) {
                    AttributeType atype = objFactory.createAttributeType();
                    atype.setName(SAML2Constants.COT_LIST);
                    atype.getValue().add(name);
                    list.add(atype);
                    WSFederationMetaManager.setEntityConfig(realm, eConfig);
                }
            }
        }
    }
    
    private static boolean containsValue(List list, String name) {
        for (Iterator iter = list.iterator(); iter.hasNext();) {
            if (((String) iter.next()).trim().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Removes the circle of trust name passed from the <code>cotlist</code>
     * attribute in the Entity Config. The Service Provider and Identity
     * Provider Entity Configuration are updated.
     *
     * @param realm the realm of the provider
     * @param name the circle of trust name to be removed.
     * @param entityId the entity identifier of the provider.
     * @throws WSFederationMetaException if there is an error updating the 
     * entity config.
     * @throws JAXBException if there is an error updating the entity config.
     */
    public static void removeFromEntityConfig(String realm, String name,
        String entityId)
    throws WSFederationMetaException, JAXBException {
        String classMethod = "WSFederationCOTUtils.removeFromEntityConfig: ";
        // Check whether the entity id existed in the DS
        FederationElement edes = WSFederationMetaManager.getEntityDescriptor(
                realm, entityId);
        if (edes == null) {
            debug.error(classMethod +"No such entity: " + entityId);
            String[] data = {realm, entityId};
            throw new WSFederationMetaException("entityid_invalid", data);
        }
        FederationConfigElement eConfig = 
            WSFederationMetaManager.getEntityConfig(realm, entityId);
        if (eConfig != null) {
            List elist = eConfig.
                    getIDPSSOConfigOrSPSSOConfig();
            for (Iterator iter = elist.iterator(); iter.hasNext();) {
                BaseConfigType bConfig = (BaseConfigType)iter.next();
                List list = bConfig.getAttribute();
                for (Iterator iter2 = list.iterator(); iter2.hasNext();) {
                    AttributeType avp = (AttributeType)iter2.next();
                    if (avp.getName().trim().equalsIgnoreCase(
                            SAML2Constants.COT_LIST)) {
                        List avpl = avp.getValue();
                        if (avpl != null && !avpl.isEmpty() &&
                                containsValue(avpl,name)) {
                            avpl.remove(name);
                            WSFederationMetaManager.setEntityConfig(realm,
                                eConfig);
                            break;
                        }
                    }
                }
            }
        }
    }
}