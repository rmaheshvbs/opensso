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
 * $Id: ConfiguredAuthServices.java,v 1.2 2006-08-25 21:20:30 veiming Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.authentication.service;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.Constants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.ChoiceValues;

/**
 * The class determines the configured Identity Types for Identity Repository.
 * This list is computed per realm.
 */
public class ConfiguredAuthServices extends ChoiceValues {
    /**
     * Creates <code>ConfiguredAuthServices</code> object.
     * Default constructor that will be used by the SMS
     * to create an instance of this class
     */
    public ConfiguredAuthServices() {
        // do nothing
    }
    
    /**
     * Returns the choice values and their corresponding localization keys.
     *
     * @return the choice values and their corresponding localization keys.
     */
    public Map getChoiceValues() {
        return getChoiceValues(Collections.EMPTY_MAP);
    }

    /**
     * Returns the choice values from configured environment params.
     * @param envParams map for configured parameters
     * @return the choice values from configured environment params.
     */
    public Map getChoiceValues(Map envParams) {
        String orgDN = null;
        
        if (envParams != null) {
            orgDN = (String)envParams.get(Constants.ORGANIZATION_NAME);
        }
        if (orgDN == null || orgDN.length() == 0) {
            orgDN = SystemProperties.get(Constants.AM_ROOT_SUFFIX);
        }

        SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        Set namedConfigs = Collections.EMPTY_SET;
        Map answer = new HashMap();
        try {
            namedConfigs = AMAuthConfigUtils.getAllNamedConfig(orgDN, 
                    adminToken);
        } catch (Exception e) {
            // do nothing as namedConfigs will be empty.
        }

        if (namedConfigs != null && !namedConfigs.isEmpty()) {
            for (Iterator it = namedConfigs.iterator(); it.hasNext(); ) {
                String config = (String) it.next();
                answer.put(config, config);
            }
        }

        answer.put(ISAuthConstants.BLANK, ISAuthConstants.BLANK);

        //return the choice values map
        return (answer);
    }
}
