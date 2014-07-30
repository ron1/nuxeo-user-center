package org.nuxeo.ecm.user.center.profile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.RepositorySettings;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.userworkspace.api.UserWorkspaceService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ UserProfileFeature.class })
public class TestUserProfileCreation {

    @Inject
    RepositorySettings repositorySettings;

    @Inject
    UserWorkspaceService userWorkspaceService;

    @Inject
    DirectoryService directoryService;

    @Inject
    UserProfileService ups;

    @Test
    public void testAdminCreate() throws Exception {

        Session userDir = directoryService.getDirectory("userDirectory").getSession();
        DocumentModel user;
        try {
            Map<String, Object> user1 = new HashMap<String, Object>();
            user1.put("username", "user1");
            user1.put("groups", Arrays.asList(new String[] { "members" }));
            user = userDir.createEntry(user1);
        } finally {
            userDir.close();
        }

        try (CoreSession session = repositorySettings.openSessionAs(user.getId())) {
            DocumentModel userWorkspace = userWorkspaceService.getCurrentUserPersonalWorkspace(
                    session, null);
            Assert.assertEquals(user.getId(), userWorkspace.getName());

            DocumentModel up = ups.getUserProfileDocument(session);
            Assert.assertNotNull(up);
        }
    }

}
