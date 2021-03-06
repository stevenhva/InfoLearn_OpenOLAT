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
package org.olat.portfolio.ui.artefacts.view;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.id.Identity;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.filter.FilterFactory;
import org.olat.portfolio.EPSecurityCallback;
import org.olat.portfolio.EPUIFactory;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.user.UserManager;

/**
 * Description:<br>
 * simple artefact read-only controller
 * <P>
 * Initial Date:  17.11.2010 <br>
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPArtefactViewReadOnlyController extends BasicController {

	private VelocityContainer vC;
	private EPFrontendManager ePFMgr;
	private Link detailsLink;
	private AbstractArtefact artefact;
	private EPSecurityCallback secCallback;

	protected EPArtefactViewReadOnlyController(UserRequest ureq, WindowControl wControl, AbstractArtefact artefact, EPSecurityCallback secCallback) {
		super(ureq, wControl);
		this.artefact = artefact;
		this.secCallback = secCallback;
		vC = createVelocityContainer("smallSingleArtefact");
		vC.contextPut("artefact", artefact);
		Identity artIdent = artefact.getAuthor();
		String fullName = CoreSpringFactory.getImpl(UserManager.class).getUserDisplayName(artIdent);
		String description = FilterFactory.getHtmlTagAndDescapingFilter().filter(artefact.getDescription());
		description = StringHelper.xssScan(description);
		description = Formatter.truncate(description, 50);
		vC.contextPut("description", description);
		vC.contextPut("authorName", StringHelper.escapeHtml(fullName));
		if (secCallback.canView()){
			detailsLink = LinkFactory.createCustomLink("small.details.link", "open", "small.details.link", Link.LINK, vC, this);
			detailsLink.setElementCssClass("o_sel_artefact_details");
		}
		
		ePFMgr = CoreSpringFactory.getImpl(EPFrontendManager.class);
		List<String> tags = ePFMgr.getArtefactTags(artefact);
		List<String> escapedTags = new ArrayList<String>(tags.size());
		for(String tag:tags) {
			escapedTags.add(StringHelper.escapeHtml(tag));
		}
		vC.contextPut("tags", StringHelper.formatAsCSVString(escapedTags));
		
		putInitialPanel(vC);	
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if (source == detailsLink && secCallback.canView()){
			String title = translate("view.artefact.header");
			CloseableModalController artDetails = EPUIFactory.getAndActivatePopupArtefactController(artefact, ureq, getWindowControl(), title);
			listenTo(artDetails);
		} 
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		//
	}
}
