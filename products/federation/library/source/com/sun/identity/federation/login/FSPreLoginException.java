/**
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
 * $Id: FSPreLoginException.java,v 1.1 2006-10-30 23:14:07 qcheng Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.federation.login;

import com.sun.identity.federation.common.FSException;   

/**
 * The {@link FSPreLoginException} is used to specify an exception
 * related to an error during pre login operations.
 *
 * @see FSPreLogin
 */
public class FSPreLoginException extends FSException {

    /**
     * Creates <code>FSPreLoginException</code> object.
     * @param errorCode Key of the error message in resource bundle.
     * @param args Arguments to the message.
     */
    public FSPreLoginException(String errorCode, Object[] args) {
        super(errorCode, args);
    }

    /**
     * Creates <code>FSPreLoginException</code> object.
     *
     * @param msg the message for this exception.
     */
    public FSPreLoginException(String msg) {
        super(msg);
    }

    /**
     * Creates <code>FSPreLoginException</code> object.
     *
     * @param t Root cause of this exception.
     * @param message English message for this exception.
     */
    public FSPreLoginException(Throwable t, String msg) {
        super(t, msg);
    }
}