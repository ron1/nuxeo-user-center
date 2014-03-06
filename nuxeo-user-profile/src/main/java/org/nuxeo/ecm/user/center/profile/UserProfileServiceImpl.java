/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Quentin Lamerand <qlamerand@nuxeo.com>
 */

package org.nuxeo.ecm.user.center.profile;

import static org.nuxeo.ecm.core.api.security.SecurityConstants.ADD_CHILDREN;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYONE;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.READ;
import static org.nuxeo.ecm.user.center.profile.UserProfileConstants.USER_PROFILE_DOCTYPE;

<<<<<<< HEAD
import java.util.concurrent.TimeUnit;

=======
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
>>>>>>> f7fa761... NXP-12200 Implement UserProfileService CSV data file importer
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Implementation of {@code UserProfileService}.
 *
 * @see UserProfileService
 * @author <a href="mailto:qlamerand@nuxeo.com">Quentin Lamerand</a>
 * @since 5.5
 */
public class UserProfileServiceImpl extends DefaultComponent implements
        UserProfileService {

    private static final Log log = LogFactory.getLog(UserProfileServiceImpl.class);

    protected static final Integer CACHE_CONCURRENCY_LEVEL = 10;

    protected static final Integer CACHE_TIMEOUT = 10;

    protected static final Integer CACHE_MAXIMUM_SIZE = 1000;

    public static final String CONFIG_EP = "config";

    private ImporterConfig config;

    private UserWorkspaceService userWorkspaceService;

    protected final Cache<String, String> profileUidCache = CacheBuilder.newBuilder().concurrencyLevel(
            CACHE_CONCURRENCY_LEVEL).maximumSize(CACHE_MAXIMUM_SIZE).expireAfterWrite(
            CACHE_TIMEOUT, TimeUnit.MINUTES).build();

    public DocumentModel getUserProfileDocument(CoreSession session)
            throws ClientException {
        DocumentModel userWorkspace = getUserWorkspaceService().getCurrentUserPersonalWorkspace(
                session, null);
        String uid = profileUidCache.getIfPresent(session.getPrincipal().getName());
        if (uid != null) {
            return session.getDocument(new IdRef(uid));
        } else {
            DocumentModel profile = new UserProfileDocumentGetter(session,
                    userWorkspace).getOrCreate();
            profileUidCache.put(session.getPrincipal().getName(),
                    profile.getId());
            return profile;
        }
    }

    @Override
    public DocumentModel getUserProfileDocument(String userName,
            CoreSession session) throws ClientException {
        DocumentModel userWorkspace = getUserWorkspaceService().getUserPersonalWorkspace(
                userName, session.getRootDocument());

        String uid = profileUidCache.getIfPresent(userName);
        if (uid != null) {
            return session.getDocument(new IdRef(uid));
        } else {
            DocumentModel profile = new UserProfileDocumentGetter(session,
                    userWorkspace).getOrCreate();
            profileUidCache.put(userName, profile.getId());
            return profile;
        }
    }

    @Override
    public DocumentModel getUserProfile(DocumentModel userModel,
            CoreSession session) throws ClientException {
        DocumentModel userProfileDoc = getUserProfileDocument(
                userModel.getId(), session);
        userProfileDoc.detach(true);
        userProfileDoc.getDataModels().putAll(userModel.getDataModels());
        return userProfileDoc;
    }

    private UserWorkspaceService getUserWorkspaceService() {
        if (userWorkspaceService == null) {
            userWorkspaceService = Framework.getLocalService(UserWorkspaceService.class);
        }
        return userWorkspaceService;
    }

    private class UserProfileDocumentGetter extends UnrestrictedSessionRunner {

        private DocumentModel userWorkspace;

        private DocumentRef userProfileDocRef;

        public UserProfileDocumentGetter(CoreSession session,
                DocumentModel userWorkspace) {
            super(session);
            this.userWorkspace = userWorkspace;
        }

        @Override
        public void run() throws ClientException {

            String query = "select * from "
                    + USER_PROFILE_DOCTYPE
                    + " where ecm:parentId='"
                    + userWorkspace.getId()
                    + "' "
                    + " AND ecm:isProxy = 0 "
                    + " AND ecm:isCheckedInVersion = 0 AND ecm:currentLifeCycleState != 'deleted'";
            DocumentModelList children = session.query(query);
            if (!children.isEmpty()) {
                userProfileDocRef = children.get(0).getRef();
            } else {
                DocumentModel userProfileDoc = session.createDocumentModel(
                        userWorkspace.getPathAsString(),
                        String.valueOf(System.currentTimeMillis()),
                        USER_PROFILE_DOCTYPE);
                userProfileDoc = session.createDocument(userProfileDoc);
                userProfileDocRef = userProfileDoc.getRef();
                ACP acp = session.getACP(userProfileDocRef);
                ACL acl = acp.getOrCreateACL();
                acl.add(new ACE(EVERYONE, READ, true));
                acp.addACL(acl);
                session.setACP(userProfileDocRef, acp, true);
                session.save();
            }
        }

        public DocumentModel getOrCreate() throws ClientException {
            if (session.hasPermission(userWorkspace.getRef(), ADD_CHILDREN)) {
                run();
            } else {
                runUnrestricted();
            }
            return session.getDocument(userProfileDocRef);
        }
    }

    @Override
    public void clearCache() {
        profileUidCache.invalidateAll();
    }

    @Override
    public ImporterConfig getImporterConfig() {
        return config;
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        if (config == null || config.getDataFileName() == null) {
            return;
        }
        boolean started = false;
        boolean ok = false;
        try {
            started = !TransactionHelper.isTransactionActive() && TransactionHelper.startTransaction();
            RepositoryManager repositoryManager = Framework.getLocalService(RepositoryManager.class);
            Repository defaultRepository = repositoryManager.getDefaultRepository();
            new UnrestrictedSessionRunner(defaultRepository.getName()) {
                @Override
                public void run() throws ClientException {
                    new UserProfileImporter().doImport(session);
                }
            }.runUnrestricted();
            ok = true;
        } finally {
            if (started) {
                try {
                    if (!ok) {
                        TransactionHelper.setTransactionRollbackOnly();
                    }
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
        }
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONFIG_EP.equals(extensionPoint)) {
            if (config != null) {
                log.warn("Overriding existing user profile importer config");
            }
            config = (ImporterConfig) contribution;
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (CONFIG_EP.equals(extensionPoint)) {
            if (config != null && config.equals(contribution)) {
                config = null;
            }
        }
    }
}
