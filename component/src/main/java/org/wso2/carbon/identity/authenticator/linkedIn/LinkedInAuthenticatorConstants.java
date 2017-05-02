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

public class LinkedInAuthenticatorConstants {

    /*
	 * Private Constructor will prevent the instantiation of this class directly
	 */
    private LinkedInAuthenticatorConstants() {
    }

    //LinkedIn authorize endpoint URL.
    public static final String LINKEDIN_OAUTH_ENDPOINT = "https://www.linkedin.com/uas/oauth2/authorization";
    //LinkedIn token  endpoint URL.
    public static final String LINKEDIN_TOKEN_ENDPOINT = "https://www.linkedin.com/uas/oauth2/accessToken";
    //LinkedIn user info endpoint URL.
    public static final String LINKEDIN_USERINFO_ENDPOINT =
            "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,industry,headline,email-address)?format=json";
    //LinkedIn connector friendly name.
    public static final String LINKEDIN_CONNECTOR_FRIENDLY_NAME = "LinkedIn Authenticator";
    //LinkedIn connector name.
    public static final String LINKEDIN_CONNECTOR_NAME = "LinkedIn";
    //The query string.
    public static final String QUERY_STRING = "scope=r_basicprofile%20r_emailaddress";
    //The oauth2 access token
    public static final String LINKEDIN_OAUTH2_ACCESS_TOKEN_PARAMETER = "oauth2_access_token";
    //The log in type.
    public static final String LINKEDIN_LOGIN_TYPE = "linkedin";
    //The authorization code that the application requested.
    public static final String OAUTH2_GRANT_TYPE_CODE = "code";
    //A randomly generated non-reused value that is sent in the request and returned in the response.
    public static final String OAUTH2_PARAM_STATE = "state";
    //The access token.
    public static final String ACCESS_TOKEN = "access_token";
    //The client ID of the client application.
    public static final String CLIENT_ID = "Client Id";
    //The value of the key that contains the client password.
    public static final String CLIENT_SECRET = "Client Secret";
    //The oauth2 authorization URL.
    public static final String OAUTH2_AUTHZ_URL = "OAuth2AuthzUrl";
    //The oauth2 token URL.
    public static final String OAUTH2_TOKEN_URL = "OAUTH2TokenUrl";
    //The reply URL of the application.
    public static final String CALLBACK_URL = "callbackUrl";
    //The ID of the user.
    public static final String USER_ID = "id";
    //The claim dialect URI.
    public static final String CLAIM_DIALECT_URI = "http://wso2.org/linkedin/claims";
    //The Http get method.
    public static final String HTTP_GET_METHOD = "GET";
    //Constant for connection time out.
    public static final int CONNECTION_TIMEOUT_VALUE = 15000;
    //Constant for read time out.
    public static final int READ_TIMEOUT_VALUE = 15000;
}