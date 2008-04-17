<%--
   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.

   You can obtain a copy of the License at
   https://opensso.dev.java.net/public/CDDLv1.0.html or
   opensso/legal/CDDLv1.0.txt
   See the License for the specific language governing
   permission and limitations under the License.

   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at opensso/legal/CDDLv1.0.txt.
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"

   $Id: index.jsp,v 1.4 2008-04-17 00:34:45 qcheng Exp $

   Copyright 2008 Sun Microsystems Inc. All Rights Reserved
--%>




<%@ page import="com.sun.identity.saml2.common.SAML2Exception" %>
<%@ page import="com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement" %>
<%@ page import="com.sun.identity.saml2.jaxb.metadata.SingleSignOnServiceElement" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaException" %>
<%@ page import="com.sun.identity.saml2.meta.SAML2MetaManager" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="java.io.FileOutputStream" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>

<%--
    index.jsp contains links to test SP or IDP initiated Single Sign-on
--%>
<%
    String deployuri = request.getRequestURI();
    int slashLoc = deployuri.indexOf("/", 1);
    if (slashLoc != -1) {
        deployuri = deployuri.substring(0, slashLoc);
    }
    String fedletHomeDir = System.getProperty("com.sun.identity.fedlet.home");
    if ((fedletHomeDir == null) || (fedletHomeDir.trim().length() == 0)) {
        if (System.getProperty("user.home").equals(File.separator)) {
            fedletHomeDir = File.separator + "fedlet";
        } else {
            fedletHomeDir = System.getProperty("user.home") +
                File.separator + "fedlet";
        }
    }
%>
<html>

<head>
    <title>Validate Fedlet Setup</title>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
    <link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
</head>

<body>
<div class="MstDiv"><table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblTop" title="">
<tbody><tr>
<td nowrap="nowrap">&nbsp;</td>
<td nowrap="nowrap">&nbsp;</td>
</tr></tbody></table>

<table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblBot" title="">
<tbody><tr>
<td class="MstTdTtl" width="99%">
<div class="MstDivTtl"><img name="ProdName" src="<%= deployuri %>/console/images/PrimaryProductName.png" alt="" /></div></td><td class="MstTdLogo" width="1%"><img name="RMRealm.mhCommon.BrandLogo" src="<%= deployuri %>/com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td></tr></tbody></table>
<table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tbody><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="<%= deployuri %>/com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" /></td></tr></tbody></table></div><div class="SkpMedGry1"><a name="SkipAnchor2089" id="SkipAnchor2089"></a></div>
<div class="SkpMedGry1"><a href="#SkipAnchor4928"><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="Jump Over Tab Navigation Area. Current Selection is: Access Control" border="0" height="1" width="1" /></a></div>

<%
    // Retreive the metadata information 
    String spEntityID = null;
    String spMetaAlias = null;
    String idpEntityID = null;
    String idpMetaAlias= null;
    boolean createConfig = false; 
    // check need to create configuration
    String param = request.getParameter("CreateConfig");
    if ((param != null) && param.equalsIgnoreCase("true")) {
        createConfig = true;
    } 
    try {
        if (createConfig) {
            // copy all files under conf to fedletHomeDir
            String[] files = new String[] {
                "FederationConfig.properties",
                "idp.xml",
                "idp-extended.xml",
                "sp.xml",
                "sp-extended.xml",
                "fedlet.cot"};
            File dir = new File(fedletHomeDir);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new SAML2Exception("Failed to create Fedlet " +
                        "configuration home directory " + fedletHomeDir);
                }
            } else if (dir.isFile()) {
                throw new SAML2Exception("Fedlet configuration home " + 
                    fedletHomeDir + " is a pre-existing file. <br>Please " +
                    "remove the file and try again."); 
            } 
            ServletContext servletCtx = getServletContext();
            for (int i = 0; i < files.length; i++) {
                String source = "/conf/" + files[i];
                String dest =  dir.getPath() + File.separator + files[i];
                FileOutputStream fos = null;
                InputStream src = null;
                try {
                    src = servletCtx.getResourceAsStream(source);
                    if (src != null) {
                        fos = new FileOutputStream(dest);
                        int length = 0;
                        byte[] bytes = new byte[1024];
                        while ((length = src.read(bytes)) != -1) {
                            fos.write(bytes, 0, length);
                        }
                    } else {
                        throw new SAML2Exception("File " + source + 
                            " could not be found in fedlet.war");
                    }
                } catch (IOException e) {
                    throw new SAML2Exception(e.getMessage());
                } finally {
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                        if (src != null) {
                            src.close();
                        }
                    } catch (IOException ex) {
                        //ignore
                    }
                }
            }
            out.println("<p><br><b>Fedlet configuration created under \"" +
                fedletHomeDir + "\" directory.</b>");
            out.println("<br><br>Click <a href=\"index.jsp\">here</a> to continue.");
        } else {
            // check if this WAR contain Fedlet configuration
            boolean confExist = false;
            InputStream src = getServletContext().getResourceAsStream(
                "/conf/FederationConfig.properties");
            if (src != null) {
                confExist = true;
            }

            File dir = new File(fedletHomeDir);
            File file = new File(fedletHomeDir + File.separator + 
                "FederationConfig.properties");
            if (!dir.exists() || !dir.isDirectory()) {
                out.println("<p><br><b>Fedlet configuration home directory does not exists.</b>");
                if (confExist) {
                    out.println("<br><br>Click <a href=\"index.jsp?CreateConfig=true\">here</a> to create Fedlet configuration automatically.");
                    out.println("<br>Or manually extract your fedlet.war and copy all files under \"conf\" directory to \"" + fedletHomeDir + "\" directory, then restart your web container.");
                } else {
                    out.println("<br>Please follow the README bundled inside your Fedlet-unconfigured.zip file to setup Fedlet configuration, then restart your web container.");
                }
            } else if (!file.exists()) {
                out.println("<p><br><b>FederationConfig.properties could not be found.</b>");
                if (confExist) {
                    out.println("<br><br>Click <a href=\"index.jsp\">here</a> to create Fedlet configuration automatically.");
                    out.println("<br>Or manually extract your fedlet.war and copy all files under \"conf\" directory to \"" + fedletHomeDir + "\" directory, then restart your web container.");
                } else {
                    out.println("<br>Please follow the README bundled inside your Fedlet-unconfigured.zip file to setup Fedlet configuration, then restart your web container.");
                }
            } else {
                SAML2MetaManager manager = new SAML2MetaManager();
                List spEntities = 
                    manager.getAllHostedServiceProviderEntities("/");
                if ((spEntities != null) && !spEntities.isEmpty()) {
                    // get first one
                    spEntityID = (String) spEntities.get(0);
                }

                List spMetaAliases =
                    manager.getAllHostedServiceProviderMetaAliases("/");
                if ((spMetaAliases != null) && !spMetaAliases.isEmpty()) {
                    // get first one
                    spMetaAlias = (String) spMetaAliases.get(0);
                }

                List idpEntities = 
                    manager.getAllRemoteIdentityProviderEntities("/");
                if ((idpEntities != null) && !idpEntities.isEmpty()) {
                    // get first one
                    idpEntityID = (String) idpEntities.get(0);
                }
                if ((spEntityID == null) || (idpEntityID == null)) {
                    out.println("<p><br><b>Fedlet or remote Identity Provider metadata is not configured.</b>");
                    if (confExist) {
                        out.println("<p><br>Click <a href=\"index.jsp\">here</a> to create Fedlet configuration automatically.");
                        out.println("<br>Or manually extract your fedlet.war and copy all files under \"conf\" directory to \"" + fedletHomeDir + "\" directory, then restart your web container.");
                    } else {
                        out.println("<br>Please follow the README bundled inside your Fedlet-unconfigured.zip file to setup Fedlet configuration, then restart your web container.");
                    }
                } else {
                    // IDP base URL
                    String idpBaseUrl = null;
                    // find out IDP meta alias
                    IDPSSODescriptorElement idp = 
                        manager.getIDPSSODescriptor("/", idpEntityID);
                    List ssoServiceList = idp.getSingleSignOnService();
                    if ((ssoServiceList != null) 
                        && (!ssoServiceList.isEmpty())) {
                        Iterator i = ssoServiceList.iterator();
                        while (i.hasNext()) {
                            SingleSignOnServiceElement sso =
                                (SingleSignOnServiceElement) i.next();
                            if ((sso != null) && (sso.getBinding() != null)) {
                                String ssoURL = sso.getLocation();
                                int loc = ssoURL.indexOf("/metaAlias/");
                                if (loc == -1) {
                                    continue;
                                } else {
                                    idpMetaAlias = ssoURL.substring(loc + 10);
                                    String tmp = ssoURL.substring(0, loc);
                                    loc = tmp.lastIndexOf("/");
                                    idpBaseUrl = tmp.substring(0, loc);
                                    break;
                                }
                            }
                        }
                    }
                    String fedletBaseUrl = request.getScheme() + "://" +
                        request.getServerName() + ":" + 
                        request.getServerPort() + deployuri;
%>
    <h2>Validate Fedlet Setup</h2>
    <p><br>
    <table border="0" width="700">
    <tr>
       <td colspan="2">
          Click following links to start Fedlet(SP) and/or IDP initiated 
          Single Sign-On. Upon successful completion, you will be presented 
          with a page to display the Single Sign-On Response, Assertion and 
          AttributeStatement received from IDP side.</td>
    </tr> 
    <tr>
      <td colspan="2"> </td>
    </tr>
    <tr>
      <td colspan="2"> </td>
    </tr>
    <tr>
      <td colspan="2"> </td>
    </tr>
    <tr>
      <td><b>Fedlet (SP) Configuration Directory:&nbsp;&nbsp;</b></td> <td><%= fedletHomeDir %></td>
    </tr>
    <tr>
      <td><b>Fedlet (SP) Entity ID:</b>&nbsp;&nbsp;&nbsp;&nbsp;</td> <td><%= spEntityID %></td>
    </tr>
    <tr>
      <td><b>IDP Entity ID:</b></td> <td><%= idpEntityID %></td>
    </tr>
    <tr>
      <td colspan="2"> </td>
    </tr>
    <tr>
      <td colspan="2"> </td>
    </tr>
    <tr>
      <td colspan="2"><a href="<%= fedletBaseUrl %>/saml2/jsp/fedletSSOInit.jsp?metaAlias=<%= spMetaAlias %>&idpEntityID=<%= idpEntityID%>">Run Fedlet (SP) initiated Single Sign-On</a></td>
    </tr>
<%
                    if ((idpMetaAlias != null) && (idpMetaAlias.length() != 0)){
                        //remote IDP is also FAM, show IDP initiated SSO 
%>
    <tr>
      <td colspan="2"> </td>
    </tr>
     <tr>
       <td colspan="2"><a href="<%= idpBaseUrl %>/idpssoinit?NameIDFormat=urn:oasis:names:tc:SAML:2.0:nameid-format:transient&metaAlias=<%= idpMetaAlias %>&spEntityID=<%=spEntityID %>&binding=urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST">Run Identity Provider initiated Single Sign-On</a></td>
     </tr>
<%
                    }       
%>
    <tr>
      <td colspan="2"> </td>
    </tr>
    </table>
<%
                }
            }
        }
    } catch (SAML2MetaException se) {
        se.printStackTrace();
        response.sendError(response.SC_INTERNAL_SERVER_ERROR, se.getMessage());
    } catch (SAML2Exception sse) {
        sse.printStackTrace();
        response.sendError(response.SC_INTERNAL_SERVER_ERROR, sse.getMessage());
    }
%>
</body>
</html>
