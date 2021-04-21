/*
 The MIT License

 Copyright (c) 2020, Andrew Grimberg

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package org.jenkinsci.plugins.configfiles.custom.security;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.configfiles.custom.security.TokenValueMacro;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomConfigCredentialsHelper {

    private static final Logger LOGGER = Logger.getLogger(CustomConfigCredentialsHelper.class.getName());

    /**
     * hide constructor
     */
    private CustomConfigCredentialsHelper() {
    }

    public static Map<String, IdCredentials> resolveCredentials(Run<?, ?> build, final List<CustomizedCredentialMapping> customizedCredentialMappings, TaskListener listener) {

        Map<String, IdCredentials> customizedCredentialsMap = new HashMap<>();
        for (CustomizedCredentialMapping customizedCredentialMapping : customizedCredentialMappings) {
            final String credentialsId = customizedCredentialMapping.getCredentialsId();
            final String tokenKey = customizedCredentialMapping.getTokenKey();

            List<DomainRequirement> domainRequirements = Collections.emptyList();
            if (StringUtils.isNotBlank(tokenKey)) {
                domainRequirements = Collections.singletonList(new TokenKeyRequirement(tokenKey));
            }

            final IdCredentials c = CredentialsProvider.findCredentialById(credentialsId, IdCredentials.class, build, domainRequirements);

            if (c != null) {
                customizedCredentialsMap.put(tokenKey, c);
            } else {
                listener.getLogger().println("Could not find credentials [" + credentialsId + "] for " + build);
            }
        }
        return customizedCredentialsMap;
    }

    public static String fillAuthentication(Run<?, ?> build, FilePath workDir, TaskListener listener,
            String customizedContent, Map<String, IdCredentials> customizedCredentialsMap)
            throws MacroEvaluationException, IOException, InterruptedException {
        List<TokenValueMacro> customizedMacroArray = new ArrayList<TokenValueMacro>();

        customizedCredentialsMap.forEach((key, value) -> customizedMacroArray.addAll(createCredentialBasedToken(key, value)));

	@SuppressWarnings("unchecked")
        List<TokenMacro> tokenMacroArray = (List<TokenMacro>) (List<? extends TokenMacro>) customizedMacroArray;

        return TokenValueMacro.expand(build, workDir, listener, customizedContent, false, tokenMacroArray);
    }

    private static List<TokenValueMacro> createCredentialBasedToken(final String tokenKey,
            final IdCredentials credential)
    {
        List<TokenValueMacro> credentialTokens = new ArrayList<TokenValueMacro>();
        String tokenValue = "";

        if (credential instanceof StandardUsernamePasswordCredentials) {
            StandardUsernamePasswordCredentials usernamePasswordCredentials = (StandardUsernamePasswordCredentials) credential;
            tokenValue = usernamePasswordCredentials.getPassword().getPlainText();
            String tokenUser = usernamePasswordCredentials.getUsername();

            credentialTokens.add(new TokenValueMacro(tokenKey + "_USR", tokenUser));
            credentialTokens.add(new TokenValueMacro(tokenKey + "_PSW", tokenValue));
            tokenValue = tokenUser + ":" + tokenValue;
        } else if (credential instanceof SSHUserPrivateKey) {
            SSHUserPrivateKey sshUserPrivateKey = (SSHUserPrivateKey) credential;
	    String tokenUser = sshUserPrivateKey.getUsername();

	    credentialTokens.add(new TokenValueMacro(tokenKey + "_USR", tokenUser));

            List<String> privateKeys = sshUserPrivateKey.getPrivateKeys();
            if (privateKeys.isEmpty()) {
                LOGGER.log(Level.WARNING, "Property {0}: not private key defined in {1}, skip", new Object[]{tokenKey, sshUserPrivateKey.getId()});
            } else if (privateKeys.size() == 1) {
                LOGGER.log(Level.FINE, "Property {0}: use {1}", new Object[]{tokenKey, sshUserPrivateKey.getId()});
                tokenValue = privateKeys.get(0);
            } else {
                LOGGER.log(Level.WARNING, "Property {0}: more than ({1}) private key defined in {1}, use first private key", new Object[]{tokenKey, privateKeys.size(), sshUserPrivateKey.getId()});
                tokenValue = privateKeys.get(0);
            }
        } else if (credential instanceof StringCredentials) {
            StringCredentials stringCredentials = (StringCredentials) credential;
            tokenValue = stringCredentials.getSecret().getPlainText();
        }

        credentialTokens.add(new TokenValueMacro(tokenKey, tokenValue));

        return credentialTokens;
    }
}
