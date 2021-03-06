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

package org.olat.resource.accesscontrol.ui;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.util.StringHelper;
import org.olat.resource.OLATResource;
import org.olat.resource.accesscontrol.ACService;
import org.olat.resource.accesscontrol.ACUIFactory;
import org.olat.resource.accesscontrol.AccessControlModule;
import org.olat.resource.accesscontrol.method.AccessMethodHandler;
import org.olat.resource.accesscontrol.model.AccessMethod;
import org.olat.resource.accesscontrol.model.Offer;
import org.olat.resource.accesscontrol.model.OfferAccess;
import org.olat.resource.accesscontrol.model.OfferImpl;

/**
 * 
 * Description:<br>
 * 
 * 
 * <P>
 * Initial Date:  14 avr. 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class AccessConfigurationController extends FormBasicController {

	private List<Link> addMethods = new ArrayList<Link>();
	private final String displayName;
	private final OLATResource resource;
	private final AccessControlModule acModule;
	private final ACService acService;
	
	private FormLink createLink;
	private FormLayoutContainer confControllerContainer;
	private CloseableCalloutWindowController createCalloutCtrl;
	private CloseableModalController cmc;
	private AbstractConfigurationMethodController newMethodCtrl;
	
	private final List<AccessInfo> confControllers = new ArrayList<AccessInfo>();
	
	private final boolean embbed;
	private final boolean emptyConfigGrantsFullAccess;
	private boolean allowPaymentMethod;
	private final boolean editable;
	
	public AccessConfigurationController(UserRequest ureq, WindowControl wControl, OLATResource resource,
			String displayName, boolean allowPaymentMethod, boolean editable) {
		super(ureq, wControl, "access_configuration");
		
		this.resource = resource;
		this.displayName = displayName;
		this.allowPaymentMethod = allowPaymentMethod;
		acModule = (AccessControlModule)CoreSpringFactory.getBean("acModule");
		acService = CoreSpringFactory.getImpl(ACService.class);
		embbed = false;
		this.editable = editable;
		emptyConfigGrantsFullAccess = true; 
		
		initForm(ureq);
	}
		
	public AccessConfigurationController(UserRequest ureq, WindowControl wControl, OLATResource resource,
			String displayName, boolean allowPaymentMethod, boolean editable, Form form) {
		super(ureq, wControl, FormBasicController.LAYOUT_CUSTOM, "access_configuration", form);
		
		this.editable = editable;
		this.resource = resource;
		this.displayName = displayName;
		this.allowPaymentMethod = allowPaymentMethod;
		acModule = CoreSpringFactory.getImpl(AccessControlModule.class);
		acService = CoreSpringFactory.getImpl(ACService.class);
		embbed = true;
		emptyConfigGrantsFullAccess = false;
		
		initForm(ureq);
	}
	
	public int getNumOfBookingConfigurations() {
		return confControllers.size();
	}
	
	public FormItem getInitialFormItem() {
		return flc;
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		if(editable) {
			createLink = uifactory.addFormLink("add.accesscontrol", formLayout, Link.BUTTON);
			createLink.setElementCssClass("o_sel_accesscontrol_create");
		}
		
		String confPage = velocity_root + "/configuration_list.html";
		confControllerContainer = FormLayoutContainer.createCustomFormLayout("conf-controllers", getTranslator(), confPage);
		confControllerContainer.setRootForm(mainForm);
		formLayout.add(confControllerContainer);
		
		loadConfigurations();
		
		confControllerContainer.contextPut("confControllers", confControllers);
		
		if(!embbed) {
			setFormTitle("accesscontrol.title");
			setFormDescription("accesscontrol.desc");
			setFormContextHelp(AccessConfigurationController.class.getPackage().getName(), "accesscontrol.html", "chelp.accesscontrol.hover");
			
			if(editable) {
				final FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttonLayout", getTranslator());
				buttonGroupLayout.setRootForm(mainForm);
				formLayout.add(buttonGroupLayout);
				formLayout.add("buttonLayout", buttonGroupLayout);

				uifactory.addFormSubmitButton("save", buttonGroupLayout);
			}
		}
		
		confControllerContainer.contextPut("emptyConfigGrantsFullAccess", Boolean.valueOf(emptyConfigGrantsFullAccess));		
	}
	
	public void setAllowPaymentMethod(boolean allowPayment) {
		this.allowPaymentMethod = allowPayment;
	}
	
	public boolean isPaymentMethodInUse() {
		boolean paymentMethodInUse = false;
		for(AccessInfo info:confControllers) {
			paymentMethodInUse |= info.isPaymentMethod();
		}
		return paymentMethodInUse;
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if(addMethods.contains(source)) {
			AccessMethod method = (AccessMethod)((Link)source).getUserObject();
			addMethod(ureq, method);
		} else {
			super.event(ureq, source, event);
		}
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(newMethodCtrl == source) {
			if(event.equals(Event.DONE_EVENT)) {
				OfferAccess newLink = newMethodCtrl.commitChanges();
				newLink = acService.saveOfferAccess(newLink);
				addConfiguration(newLink);
				fireEvent(ureq, Event.CHANGED_EVENT);
			}
			cmc.deactivate();
			removeAsListenerAndDispose(newMethodCtrl);
			removeAsListenerAndDispose(cmc);
			newMethodCtrl = null;
			cmc = null;
		} else if (cmc == source) {
			removeAsListenerAndDispose(newMethodCtrl);
			removeAsListenerAndDispose(cmc);
			newMethodCtrl = null;
			cmc = null;
		} else {
			super.event(ureq, source, event);
		}
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(addMethods.contains(source)) {
			AccessMethod method = (AccessMethod)source.getUserObject();
			addMethod(ureq, method);
		} else if (source == createLink) {
			popupCallout(ureq);
		} else if (source.getName().startsWith("del_")) {
			AccessInfo infos = (AccessInfo)source.getUserObject();
			acService.deleteOffer(infos.getLink().getOffer());
			confControllers.remove(infos);
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	public void formOK(UserRequest ureq) {
		Map<String,FormItem> formItemMap = confControllerContainer.getFormComponents();
		
		List<OfferAccess> links = new ArrayList<OfferAccess>();
		for(AccessInfo info:confControllers) {
			FormItem dateFrom = formItemMap.get("from_" + info.getLink().getKey());
			if(dateFrom instanceof DateChooser) {
				Date from = ((DateChooser)dateFrom).getDate();
				info.getLink().setValidFrom(from);
				info.getLink().getOffer().setValidFrom(from);
			}
			
			FormItem dateTo = formItemMap.get("to_" + info.getLink().getKey());
			if(dateTo instanceof DateChooser) {
				Date to = ((DateChooser)dateTo).getDate();
				info.getLink().setValidTo(to);
				info.getLink().getOffer().setValidTo(to);
			}
			
			links.add(info.getLink());
		}
		acService.saveOfferAccess(links);
	}
	
	protected void popupCallout(UserRequest ureq) {
		addMethods.clear();
		
		VelocityContainer mapCreateVC = createVelocityContainer("createAccessCallout");
		List<AccessMethod> methods = acService.getAvailableMethods(getIdentity(), ureq.getUserSession().getRoles());
		for(AccessMethod method:methods) {
			AccessMethodHandler handler = acModule.getAccessMethodHandler(method.getType());
			if(handler.isPaymentMethod() && !allowPaymentMethod) {
				continue;
			}
			
			Link add = LinkFactory.createLink("create." + handler.getType(), mapCreateVC, this);
			add.setCustomDisplayText(handler.getMethodName(getLocale()));
			add.setUserObject(method);
			add.setCustomEnabledLinkCSS(("b_with_small_icon_left " + method.getMethodCssClass() + "_icon").intern());
			addMethods.add(add);
			mapCreateVC.put(add.getComponentName(), add);
		}
		mapCreateVC.contextPut("methods", addMethods);
		
		String title = translate("add.accesscontrol");
		removeAsListenerAndDispose(createCalloutCtrl);
		createCalloutCtrl = new CloseableCalloutWindowController(ureq, getWindowControl(), mapCreateVC, createLink, title, true, null);
		listenTo(createCalloutCtrl);
		createCalloutCtrl.activate();
		mainForm.setDirtyMarking(false);
	}
	
	protected void loadConfigurations() {
		List<Offer> offers = acService.findOfferByResource(resource, true, null);
		for(Offer offer:offers) {
			List<OfferAccess> offerAccess = acService.getOfferAccess(offer, true);
			for(OfferAccess access:offerAccess) {
				addConfiguration(access);
			}
		}
	}
	
	protected void addConfiguration(OfferAccess link) {
		AccessMethodHandler handler = acModule.getAccessMethodHandler(link.getMethod().getType());
		AccessInfo infos = new AccessInfo(handler.getMethodName(getLocale()), handler.isPaymentMethod(), null, link);
		confControllers.add(infos);

		DateChooser dateFrom = uifactory.addDateChooser("from_" + link.getKey(), "from", null, confControllerContainer);
		dateFrom.setUserObject(infos);
		dateFrom.setEnabled(editable);
		dateFrom.setDate(link.getValidFrom());
		confControllerContainer.add(dateFrom.getName(), dateFrom);
		
		DateChooser dateTo = uifactory.addDateChooser("to_" + link.getKey(), "to", null, confControllerContainer);
		dateTo.setEnabled(editable);
		dateTo.setUserObject(infos);
		dateTo.setDate(link.getValidTo());
		confControllerContainer.add(dateTo.getName(), dateTo);
		
		if(editable) {
			FormLink delLink = uifactory.addFormLink("del_" + link.getKey(), "delete", null, confControllerContainer, Link.LINK);
			delLink.setUserObject(infos);
			delLink.setCustomEnabledLinkCSS("b_with_small_icon_left b_delete_icon");
			confControllerContainer.add(delLink.getName(), delLink);
		}
	}
	
	protected void addMethod(UserRequest ureq, AccessMethod method) {
		createCalloutCtrl.deactivate();
		
		Offer offer = acService.createOffer(resource, displayName);
		OfferAccess link = acService.createOfferAccess(offer, method);
		
		removeAsListenerAndDispose(newMethodCtrl);
		newMethodCtrl = ACUIFactory.createAccessConfigurationController(ureq, getWindowControl(), link);
		if(newMethodCtrl != null) {
			listenTo(newMethodCtrl);

			AccessMethodHandler handler = acModule.getAccessMethodHandler(method.getType());
			String title = handler.getMethodName(getLocale());
		
			cmc = new CloseableModalController(getWindowControl(), translate("close"), newMethodCtrl.getInitialComponent(), true, title);
			cmc.activate();
			listenTo(cmc);
		} else {
			OfferAccess newLink = acService.saveOfferAccess(link);
			addConfiguration(newLink);
		}
	}
	
	public class AccessInfo {
		private String name;
		private String infos;
		private OfferAccess link;
		private final boolean paymentMethod;
		
		public AccessInfo(String name, boolean paymentMethod, String infos, OfferAccess link) {
			this.name = name;
			this.paymentMethod = paymentMethod;
			this.infos = infos;
			this.link = link;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public boolean isPaymentMethod() {
			return paymentMethod;
		}

		public String getInfos() {
			if(infos == null && link.getOffer() != null) {
				OfferImpl casted = (OfferImpl)link.getOffer();
				if(StringHelper.containsNonWhitespace(casted.getToken())) {
					return casted.getToken();
				}
				if(!link.getOffer().getPrice().isEmpty()) {
					String price = PriceFormat.fullFormat(link.getOffer().getPrice());
					if(acModule.isVatEnabled()) {
						BigDecimal vat = acModule.getVat();
						String vatStr = vat == null ? "" : vat.setScale(3, BigDecimal.ROUND_HALF_EVEN).toPlainString();
						return translate("access.info.price.vat", new String[]{price, vatStr});
						
					} else {
						return translate("access.info.price.noVat", new String[]{price});
					}
				}
			}
			if(StringHelper.containsNonWhitespace(infos)) {
				return infos;
			}
			return "";
		}
		
		public void setInfos(String infos) {
			this.infos = infos;
		}

		public OfferAccess getLink() {
			return link;
		}

		public void setLink(OfferAccess link) {
			this.link = link;
		}
	}
}