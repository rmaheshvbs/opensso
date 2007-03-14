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
 * $Id: SMDiscoveryDescriptionAddViewBean.java,v 1.1 2007-03-14 19:33:30 jonnelson Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
 
package com.sun.identity.console.service;

import com.sun.identity.console.service.model.SMDescriptionData;
import com.sun.identity.console.service.model.SMDiscoEntryData;

public class SMDiscoveryDescriptionAddViewBean
    extends SMDiscoveryDescriptionViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/service/SMDiscoveryDescriptionAdd.jsp";

    public SMDiscoveryDescriptionAddViewBean(
	String name,
	String defaultDisplayURL
    ) {
	super(name, defaultDisplayURL);
    }

    public SMDiscoveryDescriptionAddViewBean() {
	super("SMDiscoveryDescriptionAdd", DEFAULT_DISPLAY_URL);
    }

    protected String getPageTitleText() {
        return "discovery.service.description.create.page.title";
    }

    protected void handleButton1Request(SMDescriptionData smData) {
	SMDiscoveryBootstrapRefOffViewBeanBase vb =
	    (SMDiscoveryBootstrapRefOffViewBeanBase)getReturnToViewBean();
	SMDiscoEntryData data = (SMDiscoEntryData)removePageSessionAttribute(
	    PG_SESSION_DISCO_ENTRY_DATA);
	data.descData.add(smData);
	setPageSessionAttribute(SMDiscoveryBootstrapRefOffViewBeanBase.
	    PROPERTY_ATTRIBUTE, data);
	backTrail();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    protected SMDescriptionData getCurrentData() {
	return null;
    }

    protected String getBreadCrumbDisplayName() {
	return "breadcrumbs.webservices.discovery.description.add";
    }

    protected boolean startPageTrail() {
	return false;
    }

}
