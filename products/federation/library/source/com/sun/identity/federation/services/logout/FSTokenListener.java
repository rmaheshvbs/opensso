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
 * $Id: FSTokenListener.java,v 1.1 2006-10-30 23:14:33 qcheng Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */



package com.sun.identity.federation.services.logout;

import com.sun.identity.plugin.session.SessionListener;

/**
 * Listens to <code>Session</code> change event.
 */
public class FSTokenListener implements SessionListener {
    
    private String hostedProviderId = null;
    
    /**
     * Default constructor.
     */
    private FSTokenListener() {
    }

    /**
     * Construct a <code>FSTokenListener</code> object.
     * @param providerId hosted provider identifier
     */
    public FSTokenListener(String providerId) {
        hostedProviderId = providerId;
    }
    
    /**
     * Sets the hosted Provider where cleanup of session must happen.
     * @param hostedProviderId the Hosted provider Identifier
     */
    public void setHostedProvider(String hostedProviderId) {
        this.hostedProviderId = hostedProviderId;
    }
    
    /**
     * Gets notification when session changes state.
     * @param session The session being invalidated
     */
    public void sessionInvalidated(Object session) {
        FSLogoutUtil.removeTokenFromSession(session, hostedProviderId);
    }
}
