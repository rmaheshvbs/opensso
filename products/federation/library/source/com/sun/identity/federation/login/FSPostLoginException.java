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
 * $Id: FSPostLoginException.java,v 1.1 2006-10-30 23:14:06 qcheng Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.federation.login;

import com.sun.identity.federation.common.FSException;   

/**
 * The {@link FSPostLoginException} is used to specify an exception
 * related to an error on post login operations.
 *
 * @see FSPostLogin
 *
 */

public class FSPostLoginException extends FSException {
    
    /**
     * Creates <code>FMPostLoginException</code> object.
     *
     * @param errorCode Key of the error message in resource bundle.
     * @param args Arguments to the message.
     */
    public FSPostLoginException(String errorCode, Object[] args) {
        super(errorCode, args);
    }
    
    /**
     * Creates <code>FSPostLoginException</code> object.
     *
     * @param msg the message for this exception.
     */
    public FSPostLoginException(String msg) {
        super(msg);
    }
    
    /**
     * Creates <code>AMAuthCallBackException</code> object.
     * @param t Root cause of this exception.
     * @param message English message for this exception.
     */
    public FSPostLoginException(Throwable t, String msg) {
        super(t,msg);
    }
}
