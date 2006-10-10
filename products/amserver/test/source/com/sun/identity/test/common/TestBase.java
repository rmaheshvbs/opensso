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
 * $Id: TestBase.java,v 1.3 2006-10-10 07:40:08 veiming Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.test.common;

import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.test.UnitTestBase;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * This class is the base for all <code>OpenSSO</code> unit testcases.
 * It has commonly used methods; and hopefully we can grow this class
 * to support more methods in future.
 */
public abstract class TestBase extends UnitTestBase {
    protected TestBase(String componentName) {
        super(componentName);
    }
    
    /**
     * Returns super administrator single sign on token.
     *
     * @return super administrator single sign on token.
     */
    protected SSOToken getAdminSSOToken() {
        AdminTokenAction action = AdminTokenAction.getInstance();
        return (SSOToken)AccessController.doPrivileged(
            (PrivilegedAction<AdminTokenAction>)action);
    }
}
