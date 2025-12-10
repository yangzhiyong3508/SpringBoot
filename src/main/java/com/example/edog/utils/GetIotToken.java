package com.example.edog.utils;

import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.auth.GlobalCredentials;
import com.huaweicloud.sdk.iam.v3.IamClient;
import com.huaweicloud.sdk.iam.v3.model.*;
import com.huaweicloud.sdk.iam.v3.region.IamRegion;

import java.util.ArrayList;
import java.util.List;

public class GetIotToken {

    private static String cachedToken = null;

    public static synchronized String getToken() {
        if (cachedToken == null) {
            refreshToken();
        }
        return cachedToken;
    }

    public static synchronized void refreshToken() {
        try {
            System.out.println("[GetIotToken] Refreshing token...");
            // String ak = System.getenv("CLOUD_SDK_AK");
            // String sk = System.getenv("CLOUD_SDK_SK");
            String ak = "HPUAS4B6TEJQRMEQ6XMO";
            String sk = "dM8oZoMmfaTAP3M6oLBFK6Z5VMsV7axh5noe1SJv";

            ICredential auth = new GlobalCredentials()
                    .withAk(ak)
                    .withSk(sk);

            IamClient client = IamClient.newBuilder()
                    .withCredential(auth)
                    .withRegion(IamRegion.valueOf("cn-east-3"))
                    .build();

            KeystoneCreateUserTokenByPasswordRequest request =
                    new KeystoneCreateUserTokenByPasswordRequest();

            KeystoneCreateUserTokenByPasswordRequestBody body =
                    new KeystoneCreateUserTokenByPasswordRequestBody();

            // ⚠️ 这里仍然是你自己的账号
            PwdPasswordUserDomain domainUser = new PwdPasswordUserDomain();
            domainUser.withName("GT-Yang_zhiyong");

            PwdPasswordUser userPassword = new PwdPasswordUser();
            userPassword.withDomain(domainUser)
                    .withName("GT-Yang_zhiyong")
                    .withPassword("Mimawangle2");

            PwdPassword passwordIdentity = new PwdPassword();
            passwordIdentity.withUser(userPassword);

            List<PwdIdentity.MethodsEnum> listIdentityMethods = new ArrayList<>();
            listIdentityMethods.add(PwdIdentity.MethodsEnum.fromValue("password"));

            PwdIdentity identityAuth = new PwdIdentity();
            identityAuth.withMethods(listIdentityMethods)
                    .withPassword(passwordIdentity);

            PwdAuth authbody = new PwdAuth();
            authbody.withIdentity(identityAuth);

            body.withAuth(authbody);
            request.withBody(body);

            KeystoneCreateUserTokenByPasswordResponse response =
                    client.keystoneCreateUserTokenByPassword(request);

            // ✅ 真正的 token 在 Header 里
            cachedToken = response.getXSubjectToken();
            System.out.println("[GetIotToken] Token refreshed successfully.");
        } catch (Exception e) {
            System.err.println("[GetIotToken] Failed to refresh token: " + e.getMessage());
            e.printStackTrace();
            cachedToken = null; // Ensure it's null if failed
        }
    }
}