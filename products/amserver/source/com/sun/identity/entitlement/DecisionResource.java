/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms of the Common
 * Development and Distribution License (the License). You may not use
 * this file except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each
 * file and include the License file at opensso/legal/CDDLv1.0.txt. If
 * applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: DecisionResource.java,v 1.11 2009-10-21 00:07:22 dillidorai Exp $
 */

package com.sun.identity.entitlement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Exposes the entitlement decision REST resource.
 * 
 * @author Paul C. Bryan <pbryan@sun.com>
 * @author Ravi Hingarajiya <ravi.hingarajiya@sun.com>
 */
@Path("/1/entitlement")
public class DecisionResource extends ResourceBase {
    public static final String JSON_DECISION_ARRAY_KEY = "results";

    private enum Permission {
        deny, allow
    }

    private Evaluator getEvaluator(Subject caller, String application)
        throws EntitlementException {
        return ((application == null) || (application.length() == 0))
            ? new Evaluator(caller) : new Evaluator(caller, application);
    }

    /**
     * Returns entitlement decision of a given user.
     *
     * @param realm Realm name.
     * @param subject Subject of interest.
     * @param action Action to be evaluated.
     * @param resource Resource to be evaluated.
     * @param application Application name.
     * @param environment environment parameters.
     * @return entitlement decision of a given user. Either "deny" or "allow".
     */
    @GET
    @Produces("text/plain")
    @Path("/decision")
    public String getDecision(
        @Context HttpHeaders headers,
        @QueryParam("admin") String admin,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @QueryParam("subject") String subject,
        @QueryParam("action") String action,
        @QueryParam("resource") String resource,
        @QueryParam("application") String application,
        @QueryParam("env") List<String> environment
    ) {
         return decision(
            headers,
            admin,
            realm,
            subject,
            action,
            resource,
            application,
            environment);
    }

    /**
     * Returns entitlement decision of a given user.
     *
     * @param realm Realm name.
     * @param subject Subject of interest.
     * @param action Action to be evaluated.
     * @param resource Resource to be evaluated.
     * @param application Application name.
     * @param environment environment parameters.
     * @return entitlement decision of a given user. Either "deny" or "allow".
     */
    @POST
    @Produces("text/plain")
    @Path("/decision")
    public String postDecision(
        @Context HttpHeaders headers,
        @FormParam("admin") String admin,
        @FormParam("realm") @DefaultValue("/") String realm,
        @FormParam("subject") String subject,
        @FormParam("action") String action,
        @FormParam("resource") String resource,
        @FormParam("application") String application,
        @FormParam("env") List<String> environment
    ) {
         return decision(
            headers,
            admin,
            realm,
            subject,
            action,
            resource,
            application,
            environment);
    }

    /**
     * Returns entitlement decision of a given user.
     *
     * @param realm Realm name.
     * @param subject Subject of interest.
     * @param action Action to be evaluated.
     * @param resource Resource to be evaluated.
     * @param application Application name.
     * @param environment environment parameters.
     * @return entitlement decision of a given user. Either "deny" or "allow".
     */
    private String decision(
        HttpHeaders headers,
        String admin,
        String realm,
        String subject,
        String action,
        String resource,
        String application,
        List<String> environment
    ) {

        if (!realm.startsWith("/")) {
            realm = "/" + realm;
        }
        Subject caller = delegationCheck(admin);
        Map env = getMap(environment);

        try {
            validateSubjectAndResource(subject, resource);

            if ((action == null) || (action.trim().length() == 0)) {
                throw new EntitlementException(422);
            }
            Evaluator evaluator = getEvaluator(caller, application);
            return permission(evaluator.hasEntitlement(realm,
                toSubject(subject), toEntitlement(resource, action),
                env));
        } catch (EntitlementException e) {
            PrivilegeManager.debug.warning("DecisionResource.decision", e);
            throw getWebApplicationException(headers, e);
        }
    }

    /**
     * Returns the entitlements of a given subject.
     *
     * @param realm Realm Name.
     * @param subject Subject of interest.
     * @param action action to be evaluated.
     * @param resource resources to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlement of a given subject (in JSON string).
     */
    @GET
    @Produces("application/json")
    @Path("/decisions")
    public String getDecisions(
        @Context HttpHeaders headers,
        @QueryParam("admin") String admin,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @QueryParam("subject") String subject,
        @QueryParam("resources") List<String> resources,
        @QueryParam("application") String application,
        @QueryParam("env") List<String> environment
    ) {
        return decisions(
            headers,
            admin,
            realm,
            subject,
            resources,
            application,
            environment);
    }

    /**
     * Returns the entitlements of a given subject.
     *
     * @param realm Realm Name.
     * @param subject Subject of interest.
     * @param action action to be evaluated.
     * @param resource resources to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlement of a given subject (in JSON string).
     */
    @POST
    @Produces("application/json")
    @Path("/decisions")
    public String postDecisions(
        @Context HttpHeaders headers,
        @FormParam("admin") String admin,
        @FormParam("realm") @DefaultValue("/") String realm,
        @FormParam("subject") String subject,
        @FormParam("resources") List<String> resources,
        @FormParam("application") String application,
        @FormParam("env") List<String> environment
    ) {
        return decisions(
            headers,
            admin,
            realm,
            subject,
            resources,
            application,
            environment);
    }

    /**
     * Returns the entitlements of a given subject.
     *
     * @param realm Realm Name.
     * @param subject Subject of interest.
     * @param action action to be evaluated.
     * @param resource resources to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlement of a given subject (in JSON string).
     */
    private String decisions(
        HttpHeaders headers,
        String admin,
        String realm,
        String subject,
        List<String> resources,
        String application,
        List<String> environment
    ) {
        try {
            if (!realm.startsWith("/")) {
                realm = "/" + realm;
            }
            if ((resources == null) || resources.isEmpty()) {
                throw new EntitlementException(424);
            }

            Subject caller = delegationCheck(admin);

            Map env = getMap(environment);
            Set<String> setResources = new HashSet<String>();
            setResources.addAll(resources);
            validateSubject(subject);
            Evaluator evaluator = getEvaluator(caller, application);
            List<Entitlement> entitlements = evaluator.evaluate(
                realm, toSubject(subject), setResources, env);

            List<JSONObject> results = new ArrayList<JSONObject>();
            if (entitlements != null) {
                for (Entitlement e : entitlements) {
                    Map<String, Boolean> actionValues = e.getActionValues();
                    if ((actionValues != null) && !actionValues.isEmpty()) {
                        JSONEntitlement je = new JSONEntitlement(
                            e.getResourceName(), actionValues, e.getAdvices(),
                            e.getAttributes());
                        results.add(je.toJSONObject());
                    }
                }
            }

            JSONObject jo = new JSONObject();
            jo.put(JSON_DECISION_ARRAY_KEY, results);
            return jo.toString();
        } catch (JSONException e) {
            PrivilegeManager.debug.warning("DecisionResource.decisions", e);
            throw getWebApplicationException(e);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.warning("DecisionResource.decisions", e);
            throw getWebApplicationException(headers, e);
        }
    }

    /**
     * Returns the entitlement of a given subject.
     *
     * @param realm Realm Name.
     * @param subject Subject of interest.
     * @param action action to be evaluated.
     * @param resource resource to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlement of a given subject (in JSON string).
     */
    @GET
    @Produces("application/json")
    @Path("/entitlement")
    public String getEntitlement(
        @Context HttpHeaders headers,
        @QueryParam("admin") String admin,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @QueryParam("subject") String subject,
        @QueryParam("resource") String resource,
        @QueryParam("application") String application,
        @QueryParam("env") List<String> environment
    ) {
        return entitlement(
            headers,
            admin,
            realm,
            subject,
            resource,
            application,
            environment);
    }

    /**
     * Returns the entitlement of a given subject.
     *
     * @param realm Realm Name.
     * @param subject Subject of interest.
     * @param action action to be evaluated.
     * @param resource resource to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlement of a given subject (in JSON string).
     */
    @POST
    @Produces("application/json")
    @Path("/entitlement")
    public String postEntitlement(
        @Context HttpHeaders headers,
        @FormParam("admin") String admin,
        @FormParam("realm") @DefaultValue("/") String realm,
        @FormParam("subject") String subject,
        @FormParam("resource") String resource,
        @FormParam("application") String application,
        @FormParam("env") List<String> environment
    ) {
        return entitlement(
            headers,
            admin,
            realm,
            subject,
            resource,
            application,
            environment);
    }

    /**
     * Returns the entitlement of a given subject.
     *
     * @param realm Realm Name.
     * @param subject Subject of interest.
     * @param action action to be evaluated.
     * @param resource resource to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlement of a given subject (in JSON string).
     */
    private String entitlement(
        HttpHeaders headers,
        String admin,
        String realm,
        String subject,
        String resource,
        String application,
        List<String> environment
    ) {
        if (!realm.startsWith("/")) {
            realm = "/" + realm;
        }

        Map env = getMap(environment);
        Subject caller = delegationCheck(admin);

        try {
            validateSubjectAndResource(subject, resource);
            Evaluator evaluator = getEvaluator(caller, application);
            List<Entitlement> entitlements = evaluator.evaluate(
                realm, toSubject(subject), resource, env, false);

            Entitlement e = entitlements.get(0);
            JSONEntitlement jsonE = new JSONEntitlement(e.getResourceName(),
                e.getActionValues(), e.getAdvices(), e.getAttributes());
            return jsonE.toJSONObject().toString();
        } catch (JSONException e) {
            PrivilegeManager.debug.warning("DecisionResource.evaluate", e);
            throw getWebApplicationException(e);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.warning("DecisionResource.evaluate", e);
            throw getWebApplicationException(headers, e);
        }
    }

    /**
     * Returns the entitlements of a given subject.
     *
     * @param realm Realm Name.
     * @param subject Subject of interest.
     * @param action action to be evaluated.
     * @param resource resource to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlements of a given subject (in JSON string).
     */
    @GET
    @Produces("application/json")
    @Path("/entitlements")
    public String getEntitlements(
        @Context HttpHeaders headers,
        @QueryParam("admin") String admin,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @QueryParam("subject") String subject,
        @QueryParam("resource") String resource,
        @QueryParam("application") String application,
        @QueryParam("env") List<String> environment
    ) {
        return entitlements(
            headers,
            admin,
            realm,
            subject,
            resource,
            application,
            environment);
    }

    /**
     * Returns the entitlements of a given subject.
     *
     * @param realm Realm Name.
     * @param subject Subject of interest.
     * @param action action to be evaluated.
     * @param resource resource to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlements of a given subject (in JSON string).
     */
    @POST
    @Produces("application/json")
    @Path("/entitlements")
    public String postEntitlements(
        @Context HttpHeaders headers,
        @FormParam("admin") String admin,
        @FormParam("realm") @DefaultValue("/") String realm,
        @FormParam("subject") String subject,
        @FormParam("resource") String resource,
        @FormParam("application") String application,
        @FormParam("env") List<String> environment
    ) {
        return entitlements(
            headers,
            admin,
            realm,
            subject,
            resource,
            application,
            environment);
    }

    /**
     * Returns the entitlements of a given subject.
     *
     * @param realm Realm Name.
     * @param subject Subject of interest.
     * @param action action to be evaluated.
     * @param resource resource to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlements of a given subject (in JSON string).
     */
    private String entitlements(
        HttpHeaders headers,
        String admin,
        String realm,
        String subject,
        String resource,
        String application,
        List<String> environment
    ) {
        if (!realm.startsWith("/")) {
            realm = "/" + realm;
        }

        Map env = getMap(environment);
        Subject caller = delegationCheck(admin);

        try {
            validateSubjectAndResource(subject, resource);
            Evaluator evaluator = getEvaluator(caller, application);
            List<Entitlement> entitlements = evaluator.evaluate(
                realm, toSubject(subject), resource, env, true);
            List<JSONObject> result = new ArrayList<JSONObject>();

            for (Entitlement e : entitlements) {
                JSONEntitlement json = new JSONEntitlement(e.getResourceName(),
                    e.getActionValues(), e.getAdvices(), e.getAttributes());
                result.add(json.toJSONObject());
            }

            JSONObject jo = new JSONObject();
            jo.put(JSON_DECISION_ARRAY_KEY, result);
            return jo.toString();
        } catch (JSONException e) {
            PrivilegeManager.debug.warning("DecisionResource.evaluate", e);
            throw getWebApplicationException(e);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.warning("DecisionResource.evaluate", e);
            throw getWebApplicationException(headers, e);
        }
    }

    private String permission(boolean b) {
        return (b ? Permission.allow.toString() : Permission.deny.toString());
    }

    private void validateSubjectAndResource(String subject, String resource)
        throws EntitlementException {
        validateSubject(subject);
        validateResource(resource);
    }

    private void validateSubject(String subject)
        throws EntitlementException {
        if ((subject == null) || (subject.trim().length() == 0)) {
            throw new EntitlementException(421);
        }
    }

    private void validateResource(String resource)
        throws EntitlementException {
        if ((resource == null) || (resource.trim().length() == 0)) {
            throw new EntitlementException(420);
        }
    }
}

