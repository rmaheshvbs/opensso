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
 * $Id: Step7.java,v 1.2 2008-01-24 20:26:40 jonnelson Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.config.wizard;

import com.sun.identity.config.pojos.LDAPStore;
import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.setup.SetupConstants;
import net.sf.click.Page;


public class Step7 extends AjaxPage {

    public Step7(){}

    public void onInit() {
        String hostName = (String)getContext().getSessionAttribute(
            SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST);
        if (hostName != null) {                    
            add("hostName", hostName);            
            add("hostPort", (String)getContext().getSessionAttribute(
                SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT));
            add("userDN", (String)getContext().getSessionAttribute(
                SetupConstants.CONFIG_VAR_DS_MGR_DN));
            add("baseDN", (String)getContext().getSessionAttribute(
                SetupConstants.CONFIG_VAR_ROOT_SUFFIX));
        }
        
        String baseDir = (String)getContext().getSessionAttribute("ConfigBaseDir");
        add("ConfigBaseDir", baseDir);
        
        LDAPStore configStore = (LDAPStore)getContext().getSessionAttribute(
            Step3.LDAP_STORE_SESSION_KEY);
        add("configStore", configStore );

        LDAPStore userStore = (LDAPStore)getContext().getSessionAttribute(
            Step4.LDAP_STORE_SESSION_KEY );
        add( "userStore", userStore );

        String loadBalancerHost = (String)getContext().getSessionAttribute( 
            Step5.LOAD_BALANCER_HOST_SESSION_KEY );
        add( "loadBalancerHost", loadBalancerHost );
        Integer loadBalancerPort = (Integer)getContext().getSessionAttribute( 
            Step5.LOAD_BALANCER_PORT_SESSION_KEY );
        add( "loadBalancerPort", loadBalancerPort );

        super.onInit();
    }

    protected void add( String key, Object value ) {
        if ( value != null ) {
            addModel( key, value );
        }
    }
}
