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
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/ 

package org.olat.core.commons.modules.bc.commands;

import org.olat.core.commons.modules.bc.components.FolderComponent;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSManager;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;

public class CmdEditQuota extends DefaultController implements FolderCommand, ControllerEventListener {

	private int status = FolderCommandStatus.STATUS_SUCCESS;
	private Controller quotaEditController;
	private VFSSecurityCallback currentSecCallback = null;
	
	protected CmdEditQuota(WindowControl wControl) {
		super(wControl);
	}

	public Controller execute(FolderComponent folderComponent, UserRequest ureq, WindowControl wControl, Translator translator) {

		VFSContainer inheritingContainer = VFSManager.findInheritingSecurityCallbackContainer(folderComponent.getCurrentContainer());
		if (inheritingContainer == null || inheritingContainer.getLocalSecurityCallback().getQuota() == null) {
			getWindowControl().setWarning(translator.translate("editQuota.nop"));
			return null;
		}
		
		currentSecCallback = inheritingContainer.getLocalSecurityCallback();
		// cleanup old controller first
		if (quotaEditController != null) quotaEditController.dispose();
		// create a edit controller
		quotaEditController = QuotaManager.getInstance().getQuotaEditorInstance(ureq, wControl, currentSecCallback.getQuota().getPath(), true);			quotaEditController.addControllerListener(this);
		if (quotaEditController != null) {
			setInitialComponent(quotaEditController.getInitialComponent());
			return this;
		} else {
			// do nothing, quota can't be edited
			wControl.setWarning("No quota editor available in briefcase, can't use this function!");
			return null;
		}
	}

	public int getStatus() { return status; }
	
	/**
	 * @see org.olat.core.gui.control.ControllerEventListener#dispatchEvent(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == quotaEditController) {
			if (event == Event.CHANGED_EVENT) {
				// update quota
				Quota newQuota = QuotaManager.getInstance().getCustomQuota(currentSecCallback.getQuota().getPath());
				if (newQuota != null) currentSecCallback.setQuota(newQuota);
			} else if (event == Event.CANCELLED_EVENT) {
				// do nothing
			}
			fireEvent(ureq, FOLDERCOMMAND_FINISHED);
		}
	}

	public void event(UserRequest ureq, Component source, Event event) {
		// TODO Auto-generated method stub
		
	}

	protected void doDispose() {
		if (quotaEditController != null) {
			quotaEditController.dispose();
			quotaEditController = null;
		}
	}

	public boolean runsModal() {
		return false;
	}

}
