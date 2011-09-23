/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <p>
*/ 

package org.olat.course.nodes.projectbroker;

import java.io.File;

import org.olat.admin.quota.QuotaConstants;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.core.util.vfs.callbacks.ReadOnlyCallback;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.course.nodes.ta.DropboxController;
import org.olat.course.nodes.ta.DropboxScoringViewController;
import org.olat.course.nodes.ta.ReturnboxController;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * @author Christian Guretzki
 */

public class ProjectBrokerDropboxScoringViewController extends DropboxScoringViewController {

	private Project project;

	
	/**
	 * Scoring view of the dropbox.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param node
	 * @param userCourseEnv
	 */
	public ProjectBrokerDropboxScoringViewController(Project project, UserRequest ureq, WindowControl wControl, CourseNode node, UserCourseEnvironment userCourseEnv) { 
		super(ureq, wControl, node, userCourseEnv, false);	
		this.project = project;
		this.setVelocityRoot(Util.getPackageVelocityRoot(DropboxScoringViewController.class));
		Translator fallbackTranslator = new PackageTranslator( Util.getPackageName(this.getClass()), ureq.getLocale());
		Translator myTranslator = new PackageTranslator( Util.getPackageName(DropboxScoringViewController.class), ureq.getLocale(), fallbackTranslator);
		this.setTranslator(myTranslator);
		init(ureq);
	}
	
	protected String getDropboxFilePath(String assesseeName) {
		return DropboxController.getDropboxPathRelToFolderRoot(userCourseEnv.getCourseEnvironment(), node)
		+ File.separator + project.getKey();
	}

	protected String getReturnboxFilePath(String assesseeName) {
		return ReturnboxController.getReturnboxPathRelToFolderRoot(userCourseEnv.getCourseEnvironment(), node) 
		+ File.separator + project.getKey();
	}

	protected String getDropboxRootFolderName(String assesseeName	) {
		return translate("scoring.dropbox.rootfolder.name");
	}

	protected String getReturnboxRootFolderName(String assesseeName) {
		return translate("scoring.returnbox.rootfolder.name");
	}

	protected VFSSecurityCallback getDropboxVfsSecurityCallback() {
		return new ReadOnlyCallback();
	}

	protected VFSSecurityCallback getReturnboxVfsSecurityCallback(String returnboxRelPath, UserCourseEnvironment userCourseEnv2, CourseNode node2) {
		return new ReturnboxFullAccessCallback(returnboxRelPath, userCourseEnv2, node2);
	}

}

class ReturnboxFullAccessCallback implements VFSSecurityCallback {

	private Quota quota;
	private UserCourseEnvironment userCourseEnv;
	private CourseNode courseNode;

	public ReturnboxFullAccessCallback(String relPath, UserCourseEnvironment userCourseEnv, CourseNode courseNode) {
		this.userCourseEnv = userCourseEnv;
		this.courseNode = courseNode;
		QuotaManager qm = QuotaManager.getInstance();
		quota = qm.getCustomQuota(relPath);
		if (quota == null) { // if no custom quota set, use the default quotas...
			Quota defQuota = qm.getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_POWER);
			quota = QuotaManager.getInstance().createQuota(relPath, defQuota.getQuotaKB(), defQuota.getUlLimitKB());
		}
	}
	
	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#canList(org.olat.modules.bc.Path)
	 */
	public boolean canList() { return true; }
	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#canRead(org.olat.modules.bc.Path)
	 */
	public boolean canRead() { return true; }
	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#canWrite(org.olat.modules.bc.Path)
	 */
	public boolean canWrite() { return true; }
	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#canDelete(org.olat.modules.bc.Path)
	 */
	public boolean canDelete() { return false; }
	/**
	 * @see org.olat.core.util.vfs.callbacks.VFSSecurityCallback#canCopy()
	 */
	public boolean canCopy() { return false; }
	/**
	 * @see org.olat.core.util.vfs.callbacks.VFSSecurityCallback#canDeleteRevisionsPermanently()
	 */
	public boolean canDeleteRevisionsPermanently() { return false; }
	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#getQuotaKB(org.olat.modules.bc.Path)
	 */
	public Quota getQuota() {
		return quota;
	}
	/**
	 * @see org.olat.core.util.vfs.callbacks.VFSSecurityCallback#setQuota(org.olat.admin.quota.Quota)
	 */
	public void setQuota(Quota quota) {
		this.quota = quota;
	}
	/**
	 * @see org.olat.modules.bc.callbacks.SecurityCallback#getSubscriptionContext()
	 */
	public SubscriptionContext getSubscriptionContext() {
		return null;
	} 
}