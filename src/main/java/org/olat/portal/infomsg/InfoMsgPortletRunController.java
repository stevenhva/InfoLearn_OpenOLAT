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

package org.olat.portal.infomsg;

import org.olat.admin.sysinfo.InfoMessageManager;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;


/**
 * Description:<br>
 * Displays the infomessage in a portlet.
 * 
 * <P>
 * @author Lars Eberle (<a href="http://www.bps-system.de/">BPS Bildungsportal Sachsen GmbH</a>)
 */
public class InfoMsgPortletRunController extends DefaultController {

	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(InfoMsgPortletRunController.class);
	private Translator trans;
	private VelocityContainer infoVC;

	/**
	 * Constructor
	 * @param ureq
	 * @param wControl
	 */
	protected InfoMsgPortletRunController(UserRequest ureq, WindowControl wControl) {
		super(wControl);
		this.trans = new PackageTranslator(Util.getPackageName(InfoMsgPortletRunController.class), ureq.getLocale());
		this.infoVC = new VelocityContainer("infoVC", VELOCITY_ROOT + "/portlet.html", trans, this);
		InfoMessageManager mrg = (InfoMessageManager)CoreSpringFactory.getBean(InfoMessageManager.class);
		String infoMsg = mrg.getInfoMessage();
		if (StringHelper.containsNonWhitespace(infoMsg)) {
			infoVC.contextPut("content", infoMsg);
		} else {
			infoVC.contextPut("content", trans.translate("nothing"));			
		}
		setInitialComponent(this.infoVC);
	}

	public void event(UserRequest ureq, Component source, Event event) {
		// doesn't do anything
	}

	protected void doDispose() {
		// nothing to dispose
	}

}