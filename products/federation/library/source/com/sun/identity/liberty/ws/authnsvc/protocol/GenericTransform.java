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
 * $Id: GenericTransform.java,v 1.1 2006-10-30 23:14:43 qcheng Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.liberty.ws.authnsvc.protocol;

/**
 * The <code>GenericTransform</code> class represents a generic
 * <code>Transform</code> that doesn't transform.
 * 
 * @supported.all.api
 */
public class GenericTransform extends Transform {


    /**
     * Constructs <code>GenericTransform</code> with the value of 'name'
     * attribute.
     * @param name value of 'name' attribute
     */
    public GenericTransform(String name) {
        this.name = name;
    }

    /**
     * Transforms password.
     * @param password original password
     * @return transformed password
     */
    public String transform(String password)
    {
        return password;
    }
}
