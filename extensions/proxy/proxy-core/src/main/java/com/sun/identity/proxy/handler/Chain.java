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
 * $Id: Chain.java,v 1.1 2009-10-06 01:05:17 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.handler;

import com.sun.identity.proxy.http.Exchange;
import java.io.IOException;

/**
 * TODO: Description.
 *
 * @author Paul C. Bryan
 * @credit Paul Sandoz (influenced by the com.sun.jersey.client.filter.Filterable class)
 */
public class Chain implements Handler
{
    /** TODO: Description. */
    private final Handler root;

    /** TODO: Description. */
    private Handler head;

    /**
     * TODO: Description.
     *
     * @param root TODO.
     */
    public Chain(Handler root) {
        this.head = this.root = root;
    }

    /**
     * Adds a filter before the existing chain of filter(s) and/or root
     * handler in the chain.
     *
     * @param filter the filter to be added.
     */
    public void addFilter(Filter filter) {
        filter.next = head;
        head = filter;
    }

    /**
     * TODO: Description.
     *
     * @param request TODO.
     * @return TODO.
     * @throws IOException TODO.
     * @throws ProxyException TODO.
     */
    public void handle(Exchange exchange) throws IOException, HandlerException {
        head.handle(exchange);
    }
}

