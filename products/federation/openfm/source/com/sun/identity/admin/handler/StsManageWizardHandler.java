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
 * $Id: StsManageWizardHandler.java,v 1.4 2009-10-08 16:16:22 ggennaro Exp $
 */

package com.sun.identity.admin.handler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import com.sun.identity.admin.Resources;
import com.sun.identity.admin.model.LinkBean;
import com.sun.identity.admin.model.MessagesBean;
import com.sun.identity.admin.model.NextPopupBean;
import com.sun.identity.admin.model.SecurityMechanismPanelBean;
import com.sun.identity.admin.model.StsManageWizardBean;
import com.sun.identity.admin.model.StsManageWizardStep;
import com.sun.identity.admin.model.StsManageWizardStep1Validator;
import com.sun.identity.admin.model.StsManageWizardStep2Validator;
import com.sun.identity.admin.model.StsManageWizardStep4Validator;

public class StsManageWizardHandler 
        extends WizardHandler 
        implements Serializable
{
    private MessagesBean messagesBean;
    
    @Override
    public void initWizardStepValidators() {
    	getWizardStepValidators()[StsManageWizardStep.TOKEN_ISSUANCE.toInt()] = new StsManageWizardStep1Validator(getWizardBean());
        getWizardStepValidators()[StsManageWizardStep.SECURITY.toInt()] = new StsManageWizardStep2Validator(getWizardBean());
        getWizardStepValidators()[StsManageWizardStep.SAML_CONFIG.toInt()] = new StsManageWizardStep4Validator(getWizardBean());
    }

    @Override
    public void cancelListener(ActionEvent event) {
        getWizardBean().reset();
        doCancelNext();
    }

    public void doCancelNext() {
        NextPopupBean npb = NextPopupBean.getInstance();
        npb.setVisible(true);
        Resources r = new Resources();
        npb.setTitle(r.getString(this, "cancelTitle"));
        npb.setMessage(r.getString(this, "cancelMessage"));
        npb.setLinkBeans(getCancelLinkBeans());
    }

    private List<LinkBean> getCancelLinkBeans() {
        List<LinkBean> lbs = new ArrayList<LinkBean>();
        lbs.add(LinkBean.HOME);
        lbs.add(LinkBean.WSS);
        return lbs;
    }


    @Override
    public void finishListener(ActionEvent event) {
        resetWidgets(event);
        
        if (!validateFinish(event)) {
            return;
        }

        if( save() ) {
            doFinishNext();
            getWizardBean().reset();
        }
    }

    public void doFinishNext() {
        NextPopupBean npb = NextPopupBean.getInstance();
        npb.setVisible(true);
        Resources r = new Resources();
        npb.setTitle(r.getString(this, "finishTitle"));
        npb.setMessage(r.getString(this, "finishMessage"));
        npb.setLinkBeans(getFinishLinkBeans());
    }

    private List<LinkBean> getFinishLinkBeans() {
        List<LinkBean> lbs = new ArrayList<LinkBean>();
        lbs.add(LinkBean.HOME);
        lbs.add(LinkBean.WSS);
        return lbs;
    }

    
    @Override
    public void expandStepListener(ActionEvent event) {
        resetWidgets(event);
        super.expandStepListener(event);
    }
    
    @Override
    public void nextListener(ActionEvent event) {
        resetWidgets(event);
        super.nextListener(event);
    }
    
    @Override
    public void previousListener(ActionEvent event) {
        resetWidgets(event);
        super.previousListener(event);
    }
    
    private void resetWidgets(ActionEvent event) {
        int step = getStep(event);
        StsManageWizardStep wizardStep = StsManageWizardStep.valueOf(step);
        StsManageWizardBean wizardBean = (StsManageWizardBean) getWizardBean();
        boolean resetSecurityWidgets = false;
        boolean resetSamlWidgets = false;
        boolean resetValidationWidgets = false;

        switch(wizardStep) {
            case SECURITY:
                resetSecurityWidgets = true;
                break;
            case SAML_CONFIG:
                resetSamlWidgets = true;
                break;
            case TOKEN_VALIDATION:
                resetValidationWidgets = true;
                break;
            case SUMMARY:
                resetSecurityWidgets = true;
                resetSamlWidgets = true;
                resetValidationWidgets = true;
                break;
            default:
                break;
        }
        
        if( resetSecurityWidgets ) {
            wizardBean.getUserCredentialsTable().resetInterface();
        }
        
        if( resetSamlWidgets ) {
            wizardBean.getSamlAttributesTable().resetInterface();
        }
        
        if( resetValidationWidgets ) {
            wizardBean.getTrustedAddresses().resetInterface();
            wizardBean.getTrustedIssuers().resetInterface();
        }        
    }

    
    private boolean save() {
        return true;
    }

    // listeners for the security mechanism panels -----------------------------
    
    public void securityMechanismPanelChangeListener(ValueChangeEvent event) {
        StsManageWizardBean wizardBean = (StsManageWizardBean) getWizardBean();
        ArrayList<SecurityMechanismPanelBean> panelBeans
            = wizardBean.getSecurityMechanismPanels();
        Object attributeValue 
            = event.getComponent().getAttributes().get("panelBean");

        if( attributeValue instanceof SecurityMechanismPanelBean ) {
            SecurityMechanismPanelBean activePanelBean
                = (SecurityMechanismPanelBean) attributeValue;
            
            if( activePanelBean.isChecked() ) {
                activePanelBean.setExpanded(true);
            } else {
                activePanelBean.setExpanded(false);
            }
            
            for(SecurityMechanismPanelBean panelBean : panelBeans) {
                if( panelBean != activePanelBean 
                        && activePanelBean.isCollapsible() ) {

                        panelBean.setExpanded(false);
                }
            }
        }   
    }
    
    public void securityMechanismPanelActionListener(ActionEvent event) {
        StsManageWizardBean wizardBean = (StsManageWizardBean) getWizardBean();
        ArrayList<SecurityMechanismPanelBean> panelBeans
            = wizardBean.getSecurityMechanismPanels();
        Object attributeValue 
            = event.getComponent().getAttributes().get("panelBean");
        
        if( attributeValue instanceof SecurityMechanismPanelBean ) {
            SecurityMechanismPanelBean activePanelBean 
                = (SecurityMechanismPanelBean) attributeValue;
            
            // only one is actively shown
            for(SecurityMechanismPanelBean panelBean : panelBeans) {
                if( activePanelBean == panelBean 
                        && panelBean.isChecked() 
                        && !panelBean.isExpanded() ) {
                    panelBean.setExpanded(true);
                } else {
                    panelBean.setExpanded(false);
                }
            }
        }   
    }

    // Getters / Setters -------------------------------------------------------

    public void setMessagesBean(MessagesBean messagesBean) {
        this.messagesBean = messagesBean;
    }

    public MessagesBean getMessagesBean() {
        return messagesBean;
    }

}
