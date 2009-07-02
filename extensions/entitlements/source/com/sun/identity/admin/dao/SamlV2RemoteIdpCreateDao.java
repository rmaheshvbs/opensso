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
 * $Id: SamlV2RemoteIdpCreateDao.java,v 1.3 2009-07-02 22:19:39 asyhuang Exp $
 */
package com.sun.identity.admin.dao;

import com.sun.identity.cot.COTException;
import com.sun.identity.workflow.AddProviderToCOT;
import com.sun.identity.workflow.ImportSAML2MetaData;
import com.sun.identity.workflow.WorkflowException;
import java.io.Serializable;

public class SamlV2RemoteIdpCreateDao
        implements Serializable {

    public SamlV2RemoteIdpCreateDao() {
    }

    public void importSamlv2RemoteIdp(
            String realm,
            String cot,
            String standardMetadata) throws RuntimeException {

        String entityId = null;
        String extendedMetadata = null;
        try {
            String[] results = ImportSAML2MetaData.importData(
                    realm, standardMetadata, extendedMetadata);
            realm = results[0];
            entityId = results[1];
        } catch (WorkflowException e) {
            throw new RuntimeException(e);
        }

        if ((cot != null) && (cot.length() > 0)) {
            try {
                AddProviderToCOT.addToCOT(realm, cot, entityId);
            } catch (COTException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void importSamlv2RemoteIdpFromURL(
            String realm,
            String cot,
            String metadataUrl) {

        String standardMetadata = SamlV2CreateSharedDao.getContent(metadataUrl);
        String extendedMetadata = null;

        String[] results;
        try {
            results = ImportSAML2MetaData.importData(
                    realm,
                    standardMetadata,
                    extendedMetadata);
        } catch (WorkflowException ex) {
            throw new RuntimeException(ex);
        }
        String entityId = results[1];

        if ((cot != null) && (cot.length() > 0)) {
            try {
                AddProviderToCOT.addToCOT(realm, cot, entityId);
            } catch (COTException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

