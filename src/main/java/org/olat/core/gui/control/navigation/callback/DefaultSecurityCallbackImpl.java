/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.core.gui.control.navigation.callback;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.navigation.SiteSecurityCallback;

/**
 * <h3>Description:</h3>
 * default SecurityCallback giving access to anyone
 * 
 * Initial Date:  24.11.2009 <br>
 * @author Roman Haag, roman.haag@frentix.com, www.frentix.com
 */
public class DefaultSecurityCallbackImpl implements SiteSecurityCallback {

	/**
	 * @see com.frentix.olat.coursesite.SiteSecurityCallback#isAllowedToLaunchSite(org.olat.core.gui.UserRequest)
	 */
	@Override
	public boolean isAllowedToLaunchSite(UserRequest ureq) {
		return true;
	}

	/**
	 * @see com.frentix.olat.coursesite.SiteSecurityCallback#isAllowedToViewSite(org.olat.core.gui.UserRequest)
	 */
	@Override
	public boolean isAllowedToViewSite(UserRequest ureq) {
		return true;
	}

}
