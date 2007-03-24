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
 * $Id: ActionMapper.java,v 1.1 2007-03-24 01:26:04 dillidorai Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.xacml2.spi;
import com.sun.identity.xacml2.context.Action;
import com.sun.identity.xacml2.common.XACML2Exception;
import java.util.Map;

/**
 * This interface defines the SPI for pluggable implementations 
 * to map XACML context Action and native Action
 */
public interface ActionMapper {

    /**
     * Initializes the mapper implementation. This would be called immediately 
     * after constructing an instance of the implementation.
     *
     * @param pdpEntityId EntityID of PDP
     * @param pepEntityId EntityID of PEP
     * @param properties configuration properties
     * @exception XACML2Exception if can not initialize
     */
    public void initialize(String pdpEntityId, String pepEntityId, 
            Map properties) throws XACML2Exception;

    /**
     * Returns native action name
     * @param xacmlContextAction XACML  context Action
     * @param serviceName native service name the requested resource belongs to
     * @return native action name
     * @exception XACML2Exception if can not map to native action name
     */
    public String mapToNativeAction(Action xacmlContextAction, 
            String serviceName) throws XACML2Exception;

    /**
     * Returns XACML  context Action
     * @param nativeActionName native action name
     * @param serviceName native service name the requested resource belongs to
     * @return XACML  context Action
     * @exception XACML2Exception if can not map to XACML  context Action
     */
    public Action mapToXACMLAction(String nativeActionName, 
            String serviceName) throws XACML2Exception;

    /**
     * Returns XACML  context decision effect
     * @param nativeActionEffect native action effect
     * @param serviceName native service name the requested resource belongs to
     * @exception XACML2Exception if can not map to XACML  context Action
     */
    public String mapToXACMLActionEffect(String nativeActionEffect,
            String serviceName) throws XACML2Exception;

}

