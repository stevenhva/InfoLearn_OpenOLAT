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
* Copyright (c) 2008 frentix GmbH, Switzerland<br>
* <p>
*/

package org.olat.resource.accesscontrol.ui;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.resource.accesscontrol.AccessResult;
import org.olat.resource.accesscontrol.manager.ACFrontendManager;
import org.olat.resource.accesscontrol.model.OfferAccess;


/**
 * 
 * Description:<br>
 * Free access
 * 
 * <P>
 * Initial Date:  15 avr. 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class FreeAccessController extends FormBasicController implements FormController {
	
	private final OfferAccess link;
	private final ACFrontendManager acFrontendManager;

	public FreeAccessController(UserRequest ureq, WindowControl wControl, OfferAccess link) {
		super(ureq, wControl);
		
		this.link = link;
		acFrontendManager = (ACFrontendManager)CoreSpringFactory.getBean("acFrontendManager");
			
		initForm(ureq);
	}
	
	public FreeAccessController(UserRequest ureq, WindowControl wControl, OfferAccess link, Form form) {
		super(ureq, wControl, LAYOUT_DEFAULT, null, form);
		
		this.link = link;
		acFrontendManager = (ACFrontendManager)CoreSpringFactory.getBean("acFrontendManager");
			
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormTitle("access.free.title");
		setFormDescription("access.free.desc");
		
		String description = link.getOffer().getDescription();
		if(StringHelper.containsNonWhitespace(description)) {
			uifactory.addStaticTextElement("offer.description", description, formLayout);
		}
		
		final FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttonLayout", getTranslator());
		buttonGroupLayout.setRootForm(mainForm);
		formLayout.add(buttonGroupLayout);
			
		uifactory.addFormSubmitButton("access.button", buttonGroupLayout);
	}
		
	@Override
	protected void doDispose() {
			//
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		return true;
	}

	@Override
	protected void formOK(UserRequest ureq) {
		AccessResult result = acFrontendManager.accessResource(getIdentity(), link, null);
		
		if(result.isAccessible()) {
			fireEvent(ureq, AccessEvent.ACCESS_OK_EVENT);
		} else {
			fireEvent(ureq, new AccessEvent(AccessEvent.ACCESS_FAILED));
		}
	}

	@Override
	public FormItem getInitialFormItem() {
		return flc;
	}
}