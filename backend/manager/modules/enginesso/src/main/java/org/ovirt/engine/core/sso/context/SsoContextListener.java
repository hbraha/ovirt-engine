package org.ovirt.engine.core.sso.context;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.CertificateFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.ovirt.engine.core.sso.api.SsoConstants;
import org.ovirt.engine.core.sso.api.SsoContext;
import org.ovirt.engine.core.sso.db.DBUtils;
import org.ovirt.engine.core.sso.service.AuthenticationUtils;
import org.ovirt.engine.core.sso.service.LocalizationUtils;
import org.ovirt.engine.core.sso.service.NegotiateAuthUtils;
import org.ovirt.engine.core.sso.service.SsoExtensionsManager;
import org.ovirt.engine.core.sso.utils.SsoLocalConfig;

public class SsoContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext ctx = event.getServletContext();
        SsoLocalConfig localConfig = new SsoLocalConfig();

        SsoContext ssoContext = new SsoContext();
        ssoContext.setSsoExtensionsManager(new SsoExtensionsManager(localConfig));
        ssoContext.init(localConfig);
        ssoContext.setSsoClientRegistry(DBUtils.getAllSsoClientsInfo());
        ssoContext.setScopeDependencies(DBUtils.getAllSsoScopeDependencies());
        ssoContext.setSsoDefaultProfile(AuthenticationUtils.getDefaultProfile(ssoContext.getSsoExtensionsManager()));
        ssoContext.setSsoProfiles(AuthenticationUtils.getAvailableProfiles(ssoContext.getSsoExtensionsManager()));
        // required in login.jsp
        ssoContext.setSsoProfilesSupportingPasswd(
                AuthenticationUtils.getAvailableProfilesSupportingPasswd(ssoContext.getSsoExtensionsManager()));
        ssoContext.setSsoProfilesSupportingPasswdChange(
                AuthenticationUtils.getAvailableProfilesSupportingPasswdChange(ssoContext.getSsoExtensionsManager()));
        ssoContext.setNegotiateAuthUtils(new NegotiateAuthUtils(ssoContext.getProfiles()));
        ssoContext.setLocalizationUtils(new LocalizationUtils(SsoConstants.APP_MESSAGE_FILENAME));

        try (InputStream in = new FileInputStream(localConfig.getPKIEngineCert().getAbsoluteFile())) {
            ssoContext.setEngineCertificate(CertificateFactory.getInstance("X.509").generateCertificate(in));
        } catch (Exception ex) {
            throw new RuntimeException("Unable to load engine certificate.");
        }

        ctx.setAttribute(SsoConstants.OVIRT_SSO_CONTEXT, ssoContext);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // empty
    }
}
