/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 */
package org.nuxeo.ecm.user.center.profile;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * @since 5.9.6
 */
@RunWith(FeaturesRunner.class)
@Features({ UserProfileFeature.class })
@LocalDeploy({ "org.nuxeo.ecm.user.center.profile:OSGI-INF/test-sql-directories-contrib.xml",
        "org.nuxeo.ecm.user.center.profile:OSGI-INF/test-core-types-contrib.xml",
        "org.nuxeo.ecm.user.center.profile:OSGI-INF/user-profile-test-ok-contrib.xml" })
public class TestUserProfileImporterOk extends AbstractUserProfileImporterTest {

    @Test
    public void userProfileImportsShouldSucceed() throws Exception {
        checkDocs();

        harness.deployContrib("org.nuxeo.ecm.user.center.profile.test",
                "OSGI-INF/user-profile-test-ok-no-update-contrib.xml");

        UserProfileImporter importer = new UserProfileImporter();
        importer.doImport(session);

        checkDocs();
    }

    private void checkDocs() throws Exception {

        File blobsFolder = getBlobsFolder();

        DocumentModel doc = userProfileService.getUserProfileDocument("user1", session);
        Calendar birthDate = (Calendar) doc.getPropertyValue("userprofile:birthdate");
        assertEquals("01/01/2001",
                new SimpleDateFormat(ImporterConfig.DEFAULT_DATE_FORMAT).format(birthDate.getTime()));
        assertEquals("111-222-3333", doc.getPropertyValue("userprofile:phonenumber"));
        byte[] signatureBytes = FileUtils.readFileToByteArray(
                new File(blobsFolder.getAbsolutePath() + "/" + "signature1.jpg"));
        byte[] docSignatureBytes = ((Blob) doc.getPropertyValue("myuserprofile:signature")).getByteArray();
        assertArrayEquals(signatureBytes, docSignatureBytes);
        assertEquals(false, doc.getPropertyValue("myuserprofile:publicprofile"));
        List<String> skills = Arrays.asList((String[]) doc.getPropertyValue("myuserprofile:skills"));
        assertEquals(2, skills.size());
        assertTrue(skills.contains("reading"));
        assertTrue(skills.contains("writing"));

        doc = userProfileService.getUserProfileDocument("user2", session);
        birthDate = (Calendar) doc.getPropertyValue("userprofile:birthdate");
        assertEquals("02/02/2002",
                new SimpleDateFormat(ImporterConfig.DEFAULT_DATE_FORMAT).format(birthDate.getTime()));
        assertEquals("222-333-4444", doc.getPropertyValue("userprofile:phonenumber"));
        signatureBytes = FileUtils.readFileToByteArray(
                new File(blobsFolder.getAbsolutePath() + "/" + "signature2.jpg"));
        docSignatureBytes = ((Blob) doc.getPropertyValue("myuserprofile:signature")).getByteArray();
        assertArrayEquals(signatureBytes, docSignatureBytes);
        assertEquals(true, doc.getPropertyValue("myuserprofile:publicprofile"));
        skills = Arrays.asList((String[]) doc.getPropertyValue("myuserprofile:skills"));
        assertEquals(2, skills.size());
        assertTrue(skills.contains("reading"));
        assertTrue(skills.contains("arithmetic"));

        doc = userProfileService.getUserProfileDocument("user3", session);
        birthDate = (Calendar) doc.getPropertyValue("userprofile:birthdate");
        assertEquals("03/03/2003",
                new SimpleDateFormat(ImporterConfig.DEFAULT_DATE_FORMAT).format(birthDate.getTime()));
    }
}