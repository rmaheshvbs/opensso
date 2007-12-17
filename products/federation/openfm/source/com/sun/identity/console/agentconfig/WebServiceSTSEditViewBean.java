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
 * $Id: WebServiceSTSEditViewBean.java,v 1.1 2007-12-17 19:42:48 veiming Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.console.agentconfig;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.agentconfig.model.AgentsModel;
import com.sun.identity.console.agentconfig.model.WSSAttributeNames;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.web.ui.view.html.CCDropDownMenu;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Customized STS View Bean.
 */
public class WebServiceSTSEditViewBean 
    extends WebServiceEditViewBean {
    private static final String PAGE_NAME = "WebServiceSTSEdit";
    static final String CHILD_NAME_USERTOKEN_NAME = "usernametokenname";
    static final String CHILD_NAME_USERTOKEN_PASSWORD = "usernametokenpassword";
    private static final String CHILD_NAME_STS_ENDPOINT =
        "securitytokenendpoint";
    private static final String CHILD_NAME_STS_METADATA_ENDPOINT =
        "securitytokenmetadataendpoint";
    private static Map attrToChildNames = new HashMap();
        
    static final String DEFAULT_DISPLAY_URL =
        "/console/agentconfig/WebServiceSTSEdit.jsp";
    
    private Set clientUIProperties = parseExternalizeUIProperties(
        "webServiceSTSUI");
    
    
    static {
        attrToChildNames.put("userpassword", "userpassword");
        attrToChildNames.put("SecurityMech", "SecurityMech");
        attrToChildNames.put("STS", "sts");
        attrToChildNames.put("STSEndpoint", "securitytokenendpoint");
        attrToChildNames.put("STSMexEndpoint", "securitytokenmetadataendpoint");
        attrToChildNames.put("isRequestSign", "isrequestsigned");
        attrToChildNames.put("isRequestHeaderEncrypt",
            "isRequestHeaderEncrypt");
        attrToChildNames.put("isRequestEncrypt", "isrequestencrypted");
        attrToChildNames.put("isResponseSign", "isresponsesigned");
        attrToChildNames.put("isResponseEncrypt", "isresponsedecrypted");
        attrToChildNames.put("publicKeyAlias", "publicKeyAlias");
        attrToChildNames.put("privateKeyAlias", "certalias");
        attrToChildNames.put("useDefaultStore", "keystoreusage");
        attrToChildNames.put("keepSecurityHeaders", "preservesecurityheader");
        attrToChildNames.put("UserCredential", "usernametokenname");
    }
    
    /**
     * Creates an instance of this view bean.
     */
    public WebServiceSTSEditViewBean() {
        super(PAGE_NAME, DEFAULT_DISPLAY_URL, true,
            "com/sun/identity/console/propertyWebServiceSTSEdit.xml");
    }
    
    /**
     * Populates the STS option list.
     *
     * @param event Display Event.
     * @throws ModelControlException if cannot access to framework model.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        
        if (!inheritedPropertyNames.contains("STS")) {
            CCDropDownMenu ccSTS = (CCDropDownMenu)getChild("sts");
            AgentsModel model = (AgentsModel)getModel();
            ccSTS.setOptions(createOptionList(model.getSTSConfigurations()));
        }
    }

    protected void setExtendedDefaultValues(Map values)
        throws AMConsoleException {
        setExternalizeUIValues(clientUIProperties, values);
        setUserCredential(values);
    }
    
    private void setUserCredential(Map values) {
        String userCredential = getValueFromMap(values, 
            WSSAttributeNames.USERCREDENTIAL);
        if ((userCredential != null) && (userCredential.trim().length() > 0)) {
            String[] result = splitUserCredToken(userCredential);
            if (result != null) {
                propertySheetModel.setValue(CHILD_NAME_USERTOKEN_NAME, 
                    result[0]);
                propertySheetModel.setValue(CHILD_NAME_USERTOKEN_PASSWORD, 
                    result[1]);
            }
        }
    }

    protected void getExtendedFormsValues(Map values)
        throws AMConsoleException {
        String userCredName = (String)propertySheetModel.getValue(
            CHILD_NAME_USERTOKEN_NAME);
        String userCredPwd = (String)propertySheetModel.getValue(
            CHILD_NAME_USERTOKEN_PASSWORD);

        if ((userCredName != null) && (userCredName.trim().length() > 0) &&
            (userCredPwd  != null) && (userCredPwd.trim().length() > 0)
        ) {
            Map map = new HashMap(2);
            map.put(userCredName, userCredPwd);
            values.put(WSSAttributeNames.USERCREDENTIAL, 
                formatUserCredential(map));
        }

        getExternalizeUIValues(clientUIProperties, values);
    }

    protected Map getAttrToChildNamesMapping() {
        return attrToChildNames;
    }
    
    protected String handleReadonlyAttributes(String xml) {
        xml = super.handleReadonlyAttributes(xml);
        if (inheritedPropertyNames.contains(WSSAttributeNames.USERCREDENTIAL)) {
            xml = makeReadOnly(xml, "usernametokenpassword");
        }
        return xml;
    }
} 
