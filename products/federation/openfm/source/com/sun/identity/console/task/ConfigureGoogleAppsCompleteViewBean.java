/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ConfigureGoogleAppsCompleteViewBean.java,v 1.1 2009-02-27 11:55:56 asyhuang Exp $
 *
 */
package com.sun.identity.console.task;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.task.model.TaskModel;
import com.sun.identity.console.task.model.TaskModelImpl;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

/**
 * Create register product UI.
 */
public class ConfigureGoogleAppsCompleteViewBean
        extends AMPrimaryMastHeadViewBean {

    public static final String DEFAULT_DISPLAY_URL =
            "/console/task/ConfigureGoogleAppsComplete.jsp";
    protected static final String PROPERTIES = "propertyAttributes";
    private AMPropertySheetModel psModel;
    private CCPageTitleModel ptModel;
    private static final String PGTITLE_TWO_BTNS =
            "pgtitleTwoBtns";
    private boolean initialized;

    /**
     * Creates a authentication domains view bean.
     */
    public ConfigureGoogleAppsCompleteViewBean() {
        super("ConfigureGoogleAppsCompleteViewBean");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
        if (!initialized) {
            initialized = true;
            createPropertyModel();
            createPageTitleModel();
            registerChildren();
            super.initialize();
        }
        super.registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PROPERTIES, AMPropertySheet.class);
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        psModel.registerChildren(this);
        ptModel.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;
        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTIES)) {
            view = new AMPropertySheet(this, psModel, name);
        } else if ((psModel != null) && psModel.isChildSupported(name)) {
            view = psModel.createChild(this, name, getModel());
        } else if ((ptModel != null) && ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }
        return view;
    }

    public void beginDisplay(DisplayEvent event)
            throws ModelControlException {
        try {
            super.beginDisplay(event);
            
            //ps.init();
            // TaskModel model = (TaskModel) getModel();
            String realm = "/";
            String entityId = "http://katmai.red.iplanet.com:8080/opensso";
            
            // ps.setDisplayFieldValue("SigninPageURL", "111");
            //  ps.setDisplayFieldValue("SignoutPageURL", "222");
            // ps.setDisplayFieldValue("ChangePasswordURL", "333");
            // ps.setDisplayFieldValue("key", "4444");
            TaskModel model = (TaskModel) getModelInternal();
            Map values = model.getConfigureGoogleAppURLs(realm, entityId);
            AMPropertySheet ps = (AMPropertySheet) getChild(PROPERTIES);
            ps.setAttributeValues(values, model);
        } catch (AMConsoleException ex) {
            Logger.getLogger(ConfigureGoogleAppsCompleteViewBean.class.getName()).log(Level.SEVERE, null, ex);
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    ex.getMessage());
        }
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new TaskModelImpl(req, getPageSessionAttributes());
    }

    private void createPropertyModel() {
        psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/ConfigureGoogleAppsComplete.xml"));
        psModel.clear();
    }

    private Map populateValue(String realm, String entityName) {
        Map map = new HashMap();
       

            //gets standard metadata values
            TaskModel model = (TaskModel) getModel();
            map.put("SigninPageURL", "111");
            map.put("SignoutPageURL", "222");
            map.put("ChangePasswordURL", "333");
            map.put("key", "4444");

            Map map1 = new HashMap();
          
        return map;
    }

    /**
     * Handles save button request.
     * save
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
            throws ModelControlException {

        forwardTo();

    }

    /**
     * Handles page cancel request.
     * 
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }
}