/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: EntitlementServiceTest.java,v 1.1 2009-05-05 06:43:24 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.sun.identity.entitlement.PolicyConfigFactory;
import com.sun.identity.entitlement.interfaces.IPolicyConfig;
import com.sun.identity.unittest.UnittestLog;
import org.testng.annotations.Test;

public class EntitlementServiceTest {
    @Test
    public void isEntitlementMode() {
        IPolicyConfig pc = PolicyConfigFactory.getPolicyConfig();
        boolean result = pc.isEntitlementMode();
        UnittestLog.logMessage(
            "EntitlementServiceTest.isEntitlementMode: returns " + result);
    }
    
    @Test
    public void showEntitlementConsole() {
        IPolicyConfig pc = PolicyConfigFactory.getPolicyConfig();
        boolean result = pc.showEntitlementConsole();
        UnittestLog.logMessage(
            "EntitlementServiceTest.showEntitlementConsole: returns " + result);
    }
}
