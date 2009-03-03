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
 * $Id: SwekeyWebServiceClient.java,v 1.2 2009-03-03 22:48:07 superpat7 Exp $
 *
 */
package com.sun.identity.authentication.modules.swekey;

import com.sun.identity.shared.debug.Debug;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SwekeyWebServiceClient {
    private static final Debug debug = SwekeyLoginModule.debug;
    private static String swekeyRndTokenServer = "http://auth-rnd-gen.musbe.net";
    private static String swekeyCheckServer = "http://auth-check.musbe.net";
    private static String swekeyLastError;

    /**
     * Private constructor - this is a singleton
     */
    private SwekeyWebServiceClient() {
    }

    /**
     *  Return the last error.
     *
     *  @return                     The Last Error
     *  @access public
     */
    public static String getLastError() {
        return swekeyLastError;
    }

    private static String getResponse(String urlString) {
        StringBuffer response = new StringBuffer();
        BufferedReader in = null;
        HttpURLConnection urlConn = null;

        try {
            System.out.println("urlString: " + urlString);
            URL url = new URL(urlString);

            if (debug.messageEnabled()) {
                debug.message("SwekeyWebServiceClient: url = " + url);
            }

            urlConn = (HttpURLConnection) url.openConnection();
            in = new BufferedReader(new InputStreamReader(
                    urlConn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            System.out.println("Response: " + response);
            if (urlConn.getResponseCode() != 200) {
                swekeyLastError = response.toString();
                return "";
            }

            if (debug.messageEnabled()) {
                debug.message("SwekeyWebServiceClient: response = " + response);
            }

        } catch (IOException e) {
            try {
                swekeyLastError = Integer.toString(urlConn.getResponseCode());
            } catch (IOException e2) {
                swekeyLastError = e2.getMessage();
            }
            Logger.getLogger(
                    SwekeyWebServiceClient.class.getName()).log(
                    Level.SEVERE, null, e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(
                            SwekeyWebServiceClient.class.getName()).log(
                            Level.SEVERE, null, ex);
                }
            }
        }

        return response.toString();
    }

    /**
     *  Get a Random Token from a Token Server
     *  The RT is a 64 vhars hexadecimal value
     *  You should better use Swekey_GetFastRndToken() for performance
     *  @access public
     */
    public static String getRndToken() {
        return getResponse(swekeyRndTokenServer + "/FULL-RND-TOKEN");
    }

    /**
     *  Checks that an OTP generated by a Swekey is valid
     *
     *  @param  id                  The id of the swekey
     *  @param rt                   The random token used to generate the otp
     *  @param otp                  The otp generated by the swekey
     *  @return                     true or false
     *  @access public
     */
    public static boolean checkOtp(String id, String rt, String otp) {
        String res = getResponse(swekeyCheckServer + "/CHECK-OTP/" + id + "/" + rt + "/" + otp);
        return res.equals("OK");
    }
}
