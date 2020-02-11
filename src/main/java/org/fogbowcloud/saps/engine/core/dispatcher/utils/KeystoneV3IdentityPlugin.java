package org.fogbowcloud.saps.engine.core.dispatcher.utils;

import java.util.Map;
import java.util.Properties;
import org.apache.commons.codec.Charsets;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class KeystoneV3IdentityPlugin {

    public static final String PROJECT_ID = "projectId";
    public static final String PASSWORD = "password";
    public static final String USER_ID = "userId";
    public static final String AUTH_URL = "authUrl";
    private final static Logger LOGGER = Logger.getLogger(KeystoneV3IdentityPlugin.class);
    private static final String IDENTITY_URL = "identity_url";
    private static final String X_SUBJECT_TOKEN = "X-Subject-Token";
    private static final String PASSWORD_PROP = "password";
    private static final String IDENTITY_PROP = "identity";
    private static final String PROJECT_PROP = "project";
    private static final String METHODS_PROP = "methods";
    private static final String SCOPE_PROP = "scope";
    private static final String AUTH_PROP = "auth";
    private static final String USER_PROP = "user";
    private static final String ID_PROP = "id";
    public static String V3_TOKENS_ENDPOINT_PATH = "/v3/auth/tokens";
    private String keystoneUrl;
    private String v3TokensEndpoint;

    public KeystoneV3IdentityPlugin(Properties properties) {
        this.keystoneUrl =
            properties.getProperty(IDENTITY_URL) == null ? properties.getProperty(AUTH_URL)
                : properties.getProperty(IDENTITY_URL);
        this.v3TokensEndpoint = keystoneUrl + V3_TOKENS_ENDPOINT_PATH;
    }

    public String createAccessId(Map<String, String> credentials) {

        LOGGER.debug("Creating new Token");

        JSONObject json;
        try {
            json = mountJson(credentials);
        } catch (JSONException e) {
            LOGGER.error("Could not mount JSON while creating token.", e);
            throw new RuntimeException(e);
        }

        String authUrl = credentials.get(AUTH_URL);
        String currentTokenEndpoint = v3TokensEndpoint;
        if (authUrl != null && !authUrl.isEmpty()) {
            currentTokenEndpoint = authUrl + V3_TOKENS_ENDPOINT_PATH;
        }

        String accessId = null;
        try {
            StringEntity body = new StringEntity(json.toString(), Charsets.UTF_8);
            HttpPost request = new HttpPost(currentTokenEndpoint);
            request.setEntity(body);
            HttpResponse response = HttpClients.createMinimal().execute(request);
            accessId = response.getFirstHeader(X_SUBJECT_TOKEN).getValue();
        } catch (Exception e) {

        }
        return accessId;
    }

    protected JSONObject mountJson(Map<String, String> credentials) throws JSONException {
        JSONObject projectId = new JSONObject();
        projectId.put(ID_PROP, credentials.get(PROJECT_ID));
        JSONObject project = new JSONObject();
        project.put(PROJECT_PROP, projectId);

        JSONObject userProperties = new JSONObject();
        userProperties.put(PASSWORD_PROP, credentials.get(PASSWORD));
        userProperties.put(ID_PROP, credentials.get(USER_ID));
        JSONObject password = new JSONObject();
        password.put(USER_PROP, userProperties);

        JSONObject identity = new JSONObject();
        identity.put(METHODS_PROP, new JSONArray(new String[]{PASSWORD_PROP}));
        identity.put(PASSWORD_PROP, password);

        JSONObject auth = new JSONObject();
        auth.put(SCOPE_PROP, project);
        auth.put(IDENTITY_PROP, identity);

        JSONObject root = new JSONObject();
        root.put(AUTH_PROP, auth);
        return root;
    }
}