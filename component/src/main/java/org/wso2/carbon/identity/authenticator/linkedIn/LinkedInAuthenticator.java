/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.identity.authenticator.linkedIn;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.json.JSONObject;
import org.wso2.carbon.identity.application.authentication.framework.FederatedApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.ApplicationAuthenticatorException;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.application.authenticator.oidc.OIDCAuthenticatorConstants;
import org.wso2.carbon.identity.application.authenticator.oidc.OpenIDConnectAuthenticator;
import org.wso2.carbon.identity.application.common.model.Claim;
import org.wso2.carbon.identity.application.common.model.ClaimMapping;
import org.wso2.carbon.identity.application.common.model.Property;
import org.wso2.carbon.identity.core.util.IdentityIOStreamUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Authenticator of linkedIn.
 */
public class LinkedInAuthenticator extends OpenIDConnectAuthenticator implements FederatedApplicationAuthenticator {

    private static Log log = LogFactory.getLog(LinkedInAuthenticator.class);

    /**
     * check weather user can process or not.
     *
     * @param request the request
     * @return true or false
     */
    @Override
    public boolean canHandle(HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("Inside LinkedInOAuth2Authenticator canHandle method and checking whether the code and state " +
                      "exist");
        }
        return request.getParameter(LinkedInAuthenticatorConstants.OAUTH2_GRANT_TYPE_CODE) != null
                && request.getParameter(LinkedInAuthenticatorConstants.OAUTH2_PARAM_STATE) != null
                && (getLoginType(request));
    }

    /**
     * check whether the state contain login type or not.
     *
     * @param request the request
     * @return login type
     */
    private Boolean getLoginType(HttpServletRequest request) {
        String state = request.getParameter(LinkedInAuthenticatorConstants.OAUTH2_PARAM_STATE);
        if (StringUtils.isNotEmpty(state)) {
            return state.contains(LinkedInAuthenticatorConstants.LINKEDIN_LOGIN_TYPE);
        } else {
            return false;
        }
    }

    /**
     * Get linkedIn authorization endpoint.
     */
    @Override
    protected String getAuthorizationServerEndpoint(Map<String, String> authenticatorProperties) {
        return LinkedInAuthenticatorConstants.LINKEDIN_OAUTH_ENDPOINT;
    }

    /**
     * Get linkedIn token endpoint.
     */
    @Override
    protected String getTokenEndpoint(Map<String, String> authenticatorProperties) {
        return LinkedInAuthenticatorConstants.LINKEDIN_TOKEN_ENDPOINT;
    }

    /**
     * Get linkedIn user info endpoint.
     */
    @Override
    protected String getUserInfoEndpoint(OAuthClientResponse token, Map<String, String> authenticatorProperties) {
        return LinkedInAuthenticatorConstants.LINKEDIN_USERINFO_ENDPOINT;
    }

    /**
     * Check ID token in linkedIn OAuth.
     */
    @Override
    protected boolean requiredIDToken(Map<String, String> authenticatorProperties) {
        return false;
    }

    /**
     * Get the friendly name of the Authenticator.
     */
    @Override
    public String getFriendlyName() {
        return LinkedInAuthenticatorConstants.LINKEDIN_CONNECTOR_FRIENDLY_NAME;
    }

    /**
     * Get the name of the Authenticator.
     */
    @Override
    public String getName() {
        return LinkedInAuthenticatorConstants.LINKEDIN_CONNECTOR_NAME;
    }

    /**
     * Get Configuration Properties.
     */
    @Override
    public List<Property> getConfigurationProperties() {
        List<Property> configProperties = new ArrayList<>();

        Property clientId = new Property();
        clientId.setName(OIDCAuthenticatorConstants.CLIENT_ID);
        clientId.setDisplayName(LinkedInAuthenticatorConstants.CLIENT_ID);
        clientId.setRequired(true);
        clientId.setDescription("Enter Linkedin IDP client identifier value");
        clientId.setDisplayOrder(0);
        configProperties.add(clientId);

        Property clientSecret = new Property();
        clientSecret.setName(OIDCAuthenticatorConstants.CLIENT_SECRET);
        clientSecret.setDisplayName(LinkedInAuthenticatorConstants.CLIENT_SECRET);
        clientSecret.setRequired(true);
        clientSecret.setConfidential(true);
        clientSecret.setDescription("Enter Linkedin IDP client secret value");
        clientSecret.setDisplayOrder(1);
        configProperties.add(clientSecret);

        Property callbackUrl = new Property();
        callbackUrl.setDisplayName("Callback URL");
        callbackUrl.setName(LinkedInAuthenticatorConstants.CALLBACK_URL);
        callbackUrl.setDescription("Enter value corresponding to callback URL");
        callbackUrl.setRequired(true);
        callbackUrl.setDisplayOrder(2);
        configProperties.add(callbackUrl);

        return configProperties;
    }

    /**
     * This is override because of query string values hard coded and input
     * values validations are not required.
     *
     * @param request  the http request
     * @param response the http response
     * @param context  the authentication context
     * @throws AuthenticationFailedException
     */
    @Override
    protected void initiateAuthenticationRequest(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context)
            throws AuthenticationFailedException {
        try {
            Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();
            if (authenticatorProperties != null) {
                String clientId = authenticatorProperties.get(OIDCAuthenticatorConstants.CLIENT_ID);
                String authorizationEP = getAuthorizationServerEndpoint(authenticatorProperties);
                if (authorizationEP == null) {
                    authorizationEP = authenticatorProperties.get(LinkedInAuthenticatorConstants.OAUTH2_AUTHZ_URL);
                }
                String callbackurl = getCallbackUrl(authenticatorProperties);
                String state = context.getContextIdentifier() + "," + LinkedInAuthenticatorConstants.LINKEDIN_LOGIN_TYPE;
                state = getState(state, authenticatorProperties);
                OAuthClientRequest authzRequest;
                String queryString = LinkedInAuthenticatorConstants.QUERY_STRING;
                authzRequest = OAuthClientRequest
                        .authorizationLocation(authorizationEP)
                        .setClientId(clientId)
                        .setRedirectURI(callbackurl)
                        .setResponseType(LinkedInAuthenticatorConstants.OAUTH2_GRANT_TYPE_CODE)
                        .setState(state).buildQueryMessage();
                String loginPage = authzRequest.getLocationUri();
                if (!queryString.startsWith("&")) {
                    loginPage = loginPage + "&" + queryString;
                } else {
                    loginPage = loginPage + queryString;
                }
                response.sendRedirect(loginPage);
            } else {
                throw new AuthenticationFailedException("Authenticator Properties obtained from the " +
                                                        "AuthenticationContext is null");
            }
        } catch (IOException e) {
            throw new AuthenticationFailedException("Exception while sending the redirect response to the client", e);
        } catch (OAuthSystemException e) {
            throw new AuthenticationFailedException("Exception while building the request", e);
        }
    }

    /**
     * Get the CallBackURL.
     */
    @Override
    protected String getCallbackUrl(Map<String, String> authenticatorProperties) {
        return authenticatorProperties.get(LinkedInAuthenticatorConstants.CALLBACK_URL);
    }

    /**
     * This method are overridden for extra claim request to LinkedIn end-point.
     *
     * @param request  the http request
     * @param response the http response
     * @param context  the authentication context
     * @throws AuthenticationFailedException
     */
    @Override
    protected void processAuthenticationResponse(HttpServletRequest request, HttpServletResponse response,
                                                 AuthenticationContext context)
            throws AuthenticationFailedException {
        try {
            Map<String, String> authenticatorProperties = context.getAuthenticatorProperties();
            String clientId = authenticatorProperties.get(OIDCAuthenticatorConstants.CLIENT_ID);
            String clientSecret = authenticatorProperties.get(OIDCAuthenticatorConstants.CLIENT_SECRET);
            String tokenEndPoint = getTokenEndpoint(authenticatorProperties);
            if (tokenEndPoint == null) {
                tokenEndPoint = authenticatorProperties.get(LinkedInAuthenticatorConstants.OAUTH2_TOKEN_URL);
            }
            String callbackurl = getCallbackUrl(authenticatorProperties);
            OAuthAuthzResponse authzResponse = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
            String code = authzResponse.getCode();
            OAuthClientRequest accessRequest;
            try {
                accessRequest = OAuthClientRequest.tokenLocation(tokenEndPoint)
                        .setGrantType(GrantType.AUTHORIZATION_CODE)
                        .setClientId(clientId).setClientSecret(clientSecret)
                        .setRedirectURI(callbackurl).setCode(code)
                        .buildBodyMessage();
                // create OAuth client that uses custom http client under the hood
                OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
                OAuthClientResponse oAuthResponse;
                oAuthResponse = oAuthClient.accessToken(accessRequest);
                String accessToken = oAuthResponse.getParam(LinkedInAuthenticatorConstants.ACCESS_TOKEN);
                if (StringUtils.isNotEmpty(accessToken)) {
                        Map<ClaimMapping, String> claims = buildClaims(oAuthResponse, authenticatorProperties);
                        if (claims != null && !claims.isEmpty()) {
                            //Find the subject from the IDP claim mapping, subject Claim URI.
                            String subjectFromClaims = FrameworkUtils
                                    .getFederatedSubjectFromClaims(context.getExternalIdP().getIdentityProvider(),
                                                                   claims);
                            associateSubjectFromClaims(context, subjectFromClaims, claims);
                        } else {
                            throw new AuthenticationFailedException("Claims for the user not found for access Token : "
                                                                    + accessToken);
                        }
                    } else {
                        throw new AuthenticationFailedException("Could not receive a valid access token from LinkedIn");
                    }
            } catch (OAuthSystemException e) {
                throw new AuthenticationFailedException("Exception while building access token request", e);
            } catch (ApplicationAuthenticatorException e) {
                throw new AuthenticationFailedException("Exception while building the claim mapping", e);
            }
        } catch (OAuthProblemException e) {
            throw new AuthenticationFailedException("Exception while getting the access token form the response", e);
        }
    }

    /**
     * Get the Linkedin specific claim dialect URI.
     * @return Claim dialect URI.
     */
    @Override
    public String getClaimDialectURI() {
        return LinkedInAuthenticatorConstants.CLAIM_DIALECT_URI;
    }

    /**
     * This method is to get the LinkedIn user details.
     *
     * @param url         user info endpoint.
     * @param accessToken access token.
     * @return user info
     * @throws ApplicationAuthenticatorException
     */
    private JSONObject fetchUserInfo(String url, String accessToken) throws ApplicationAuthenticatorException {
        if (log.isDebugEnabled()) {
            log.debug("Sending the request for getting the user info");
        }
        StringBuilder jsonResponseCollector = new StringBuilder();
        BufferedReader bufferedReader = null;
        HttpURLConnection httpConnection = null;
        JSONObject jsonObj = null;
        try {
            URL obj = new URL(url + "&" + LinkedInAuthenticatorConstants.LINKEDIN_OAUTH2_ACCESS_TOKEN_PARAMETER + "="
                              + accessToken);
            URLConnection connection = obj.openConnection();
            // Cast to a HttpURLConnection
            if (connection instanceof HttpURLConnection) {
                httpConnection = (HttpURLConnection) connection;
                httpConnection.setConnectTimeout(LinkedInAuthenticatorConstants.CONNECTION_TIMEOUT_VALUE);
                httpConnection.setReadTimeout(LinkedInAuthenticatorConstants.READ_TIMEOUT_VALUE);
                httpConnection.setRequestMethod(LinkedInAuthenticatorConstants.HTTP_GET_METHOD);
                bufferedReader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            } else {
                throw new ApplicationAuthenticatorException("Exception while casting the HttpURLConnection");
            }
            String inputLine = bufferedReader.readLine();
            while (inputLine != null) {
                jsonResponseCollector.append(inputLine).append("\n");
                inputLine = bufferedReader.readLine();
            }
            jsonObj = new JSONObject(jsonResponseCollector.toString());
        } catch (MalformedURLException e) {
            throw new ApplicationAuthenticatorException("MalformedURLException while generating the user info URL: "
                                                        + url, e);
        } catch (ProtocolException e) {
            throw new ApplicationAuthenticatorException("ProtocolException while setting the request method: " +
                                                        LinkedInAuthenticatorConstants.HTTP_GET_METHOD +
                                                        " for the URL: " + url, e);
        } catch (IOException e) {
            throw new ApplicationAuthenticatorException("Error when reading the response from " + url +
                                                        "to update user claims", e);
        } finally {
            IdentityIOStreamUtils.closeReader(bufferedReader);
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Receiving the response for the User info: " + jsonResponseCollector.toString());
        }
        return jsonObj;
    }

    /**
     * This method is to build the claims for the user info.
     *
     * @param token                   token
     * @param authenticatorProperties authenticatorProperties
     * @return claims
     */
    private Map<ClaimMapping, String> buildClaims(OAuthClientResponse token,
                                                  Map<String, String> authenticatorProperties)
            throws ApplicationAuthenticatorException {
        Map<ClaimMapping, String> claims = new HashMap<>();
        String accessToken = token.getParam("access_token");
        String url = getUserInfoEndpoint(token, authenticatorProperties);
        JSONObject userData;
        try {
            userData = fetchUserInfo(url, accessToken);
            if (userData.length() == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to fetch user claims. Proceeding without user claims");
                }
                return claims;
            }
            Iterator<?> keys = userData.keys();
            while( keys.hasNext() ){
                String key = (String)keys.next();
                String value = userData.getString(key);
                String claimUri = LinkedInAuthenticatorConstants.CLAIM_DIALECT_URI + "/" + key;
                ClaimMapping claimMapping = new ClaimMapping();
                Claim claim = new Claim();
                claim.setClaimUri(claimUri);
                claimMapping.setRemoteClaim(claim);
                claimMapping.setLocalClaim(claim);
                claims.put(claimMapping, value);
            }
        } catch (ApplicationAuthenticatorException e) {
            throw new ApplicationAuthenticatorException("Exception while fetching the user info from " + url, e);
        }
        return claims;
    }

    /**
     * This method is to configure the subject identifier from the claims.
     *
     * @param context           AuthenticationContext
     * @param subjectFromClaims subject identifier claim
     * @param claims            claims
     */
    private void associateSubjectFromClaims(AuthenticationContext context, String subjectFromClaims,
                                            Map<ClaimMapping, String> claims) {
        //Use default claim URI on the Authenticator if claim mapping is not defined by the admin
        if (StringUtils.isBlank(subjectFromClaims)) {
            String userId =
                    LinkedInAuthenticatorConstants.CLAIM_DIALECT_URI + "/" + LinkedInAuthenticatorConstants.USER_ID;
            ClaimMapping claimMapping = new ClaimMapping();
            Claim claim = new Claim();
            claim.setClaimUri(userId);
            claimMapping.setRemoteClaim(claim);
            claimMapping.setLocalClaim(claim);
            subjectFromClaims = claims.get(claimMapping);
        }
        AuthenticatedUser authenticatedUserObj =
                AuthenticatedUser.createFederateAuthenticatedUserFromSubjectIdentifier(subjectFromClaims);
        context.setSubject(authenticatedUserObj);
        authenticatedUserObj.setUserAttributes(claims);
    }
}