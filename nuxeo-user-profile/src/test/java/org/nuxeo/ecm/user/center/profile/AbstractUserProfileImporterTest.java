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

import java.io.File;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

import com.google.inject.Inject;

/**
 * @since 5.9.5
 */
public abstract class AbstractUserProfileImporterTest {

    @Inject
    protected FeaturesRunner featuresRunner;

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected CoreSession session;

    @Inject
    protected UserProfileService userProfileService;

    protected File tmpDir;

    protected File getDataDir() {
        return new File(Framework.getProperty("nuxeo.csv.blobs.folder"));
    }

}
