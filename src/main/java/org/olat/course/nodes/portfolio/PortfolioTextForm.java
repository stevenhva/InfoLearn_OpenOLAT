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

package org.olat.course.nodes.portfolio;

import java.util.Arrays;
import java.util.Date;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.nodes.PortfolioCourseNode;
import org.olat.course.nodes.portfolio.PortfolioCourseNodeConfiguration.DeadlineType;
import org.olat.modules.ModuleConfiguration;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.structel.PortfolioStructureMap;
import org.olat.repository.RepositoryEntry;

/**
 * 
 * Description:<br>
 * This form edit the explanation text of the course building block
 * 
 * <P>
 * Initial Date:  6 oct. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class PortfolioTextForm extends FormBasicController {
	private final ModuleConfiguration config;
	
	private SingleSelection deadlineMonth;
	private SingleSelection deadlineWeek;
	private SingleSelection deadlineDay;
	private DateChooser deadlineChooser;
	private FormLayoutContainer deadlineLayout;
	private SingleSelection deadlineType;
	
	private RichTextElement textEl;

	private boolean inUse;

	private boolean warningShown;
	
	public PortfolioTextForm(UserRequest ureq, WindowControl wControl, ICourse course, PortfolioCourseNode courseNode) {
		super(ureq, wControl);
		this.config = courseNode.getModuleConfiguration();		
		
		RepositoryEntry  mapEntry = courseNode.getReferencedRepositoryEntry();
		if(mapEntry != null) {
			EPFrontendManager ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
			PortfolioStructureMap template = (PortfolioStructureMap) ePFMgr.loadPortfolioStructure(mapEntry.getOlatResource());
			Long courseResId = course.getResourceableId();
			OLATResourceable courseOres = OresHelper.createOLATResourceableInstance(CourseModule.class, courseResId);
			if (template != null) {
				inUse = ePFMgr.isTemplateInUse(template, courseOres, courseNode.getIdent(), null);
			}
		}
		
		initForm(ureq);
	}
	
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormTitle("pane.tab.portfolio_config.explanation");
		setFormContextHelp(this.getClass().getPackage().getName(), "ced_portfolio_task.html", "chelp.ced_portfolio_task.hoover");
		
		String[] absolutKeys = new String[]{DeadlineType.none.name(), DeadlineType.absolut.name(), DeadlineType.relative.name()};
		String[] absolutValues = new String[] {
				translate("map.deadline." + absolutKeys[0]), translate("map.deadline." + absolutKeys[1]), translate("map.deadline." + absolutKeys[2])
		};
		deadlineType = uifactory.addRadiosVertical("deadline-type", "map.deadline", formLayout, absolutKeys, absolutValues);
		deadlineType.addActionListener(this, FormEvent.ONCHANGE);
		String type = (String)config.get(PortfolioCourseNodeConfiguration.DEADLINE_TYPE);
		if(StringHelper.containsNonWhitespace(type)) {
			try {
				deadlineType.select(type, true);
			} catch (Exception e) {
				logWarn("Wrong type for deadline: " + type, e);
				deadlineType.select(absolutKeys[0], true);
			}
		} else {
			deadlineType.select(absolutKeys[0], true);
		}
		
		//absolut deadline
		Date deadline = (Date)config.get(PortfolioCourseNodeConfiguration.DEADLINE_DATE);
		deadlineChooser = uifactory.addDateChooser("deadline-date", "map.deadline." + DeadlineType.absolut.name() + ".label", null, formLayout);
		if(deadline != null) {
			deadlineChooser.setDate(deadline);
		}
		deadlineChooser.setValidDateCheck("map.deadline.invalid");
		deadlineChooser.setNotEmptyCheck("map.deadline.invalid");
		deadlineChooser.setMandatory(true);
		deadlineChooser.addActionListener(this, FormEvent.ONCHANGE);
		
		//relativ deadline
		String template = Util.getPackageVelocityRoot(PortfolioConfigForm.class) + "/deadline.html";
		deadlineLayout = FormLayoutContainer.createCustomFormLayout("deadline", getTranslator(), template);
		deadlineLayout.setRootForm(mainForm);
		deadlineLayout.setLabel("map.deadline." + DeadlineType.relative.name() + ".label", null);

		String[] monthKeys = getTimeKeys(24);
		deadlineMonth = uifactory.addDropdownSingleselect("deadline-month", deadlineLayout, monthKeys, monthKeys, null);
		deadlineMonth.addActionListener(this, FormEvent.ONCHANGE);
		select(deadlineMonth, PortfolioCourseNodeConfiguration.DEADLINE_MONTH, monthKeys);
		
		String[] weekKeys = getTimeKeys(4);
		deadlineWeek = uifactory.addDropdownSingleselect("deadline-week", deadlineLayout, weekKeys, weekKeys, null);
		deadlineWeek.addActionListener(this, FormEvent.ONCHANGE);
		select(deadlineWeek, PortfolioCourseNodeConfiguration.DEADLINE_WEEK, weekKeys);
		
		String[] dayKeys = getTimeKeys(7);
		deadlineDay = uifactory.addDropdownSingleselect("deadline-day", deadlineLayout, dayKeys, dayKeys, null);
		deadlineDay.addActionListener(this, FormEvent.ONCHANGE);
		select(deadlineDay, PortfolioCourseNodeConfiguration.DEADLINE_DAY, dayKeys);
		formLayout.add(deadlineLayout);

		updateUI();
		
		uifactory.addSpacerElement("spacer-1", formLayout, false);
	
		Object nodeText = config.get(PortfolioCourseNodeConfiguration.NODE_TEXT);
		String text = nodeText instanceof String ? (String)nodeText : "";
		textEl = uifactory.addRichTextElementForStringDataMinimalistic("text", "explanation.text", text, 10, -1, formLayout, ureq.getUserSession(), getWindowControl());

		if(formLayout instanceof FormLayoutContainer) {
			FormLayoutContainer layoutContainer = (FormLayoutContainer)formLayout;
		
			FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
			buttonGroupLayout.setRootForm(mainForm);
			layoutContainer.add(buttonGroupLayout);
			uifactory.addFormSubmitButton("save", buttonGroupLayout);
		}
	}
	
	private String[] getTimeKeys(int numOfTimeSlots) {
		String[] timeKeys = new String[numOfTimeSlots + 1];
		timeKeys[0] = "";
		for(int i=1; i<timeKeys.length; i++) {
			timeKeys[i] = Integer.toString(i);
		}
		return timeKeys;
	}
	
	private void select(SingleSelection selection, String property, String[] allowedKeys) {
		Object time = config.get(property);
		if(time instanceof String && StringHelper.containsNonWhitespace((String)time) && Arrays.asList(allowedKeys).contains(time)) {
			try {
				selection.select((String)time, true);
			} catch (Exception e) {
				// continue and dont preset anything
			}
		} else {
			selection.select("", true);
		}
	}

	protected ModuleConfiguration getUpdatedConfig() {
		String text = textEl.getValue();
		config.set(PortfolioCourseNodeConfiguration.NODE_TEXT, text);
		
		if(deadlineType.isOneSelected()) {
			config.set(PortfolioCourseNodeConfiguration.DEADLINE_TYPE, deadlineType.getSelectedKey());
		}

		if(deadlineChooser != null && deadlineChooser.isVisible() && deadlineChooser.getDate() != null) {
			config.set(PortfolioCourseNodeConfiguration.DEADLINE_DATE, deadlineChooser.getDate());
		} else {
			config.remove(PortfolioCourseNodeConfiguration.DEADLINE_DATE);
		}
		if(deadlineMonth != null && deadlineMonth.isOneSelected() && deadlineMonth.getSelected() > 0) {
			config.set(PortfolioCourseNodeConfiguration.DEADLINE_MONTH, deadlineMonth.getSelectedKey());
		} else {
			config.remove(PortfolioCourseNodeConfiguration.DEADLINE_MONTH);
		}
		if(deadlineWeek != null && deadlineWeek.isOneSelected() && deadlineWeek.getSelected() > 0) {
			config.set(PortfolioCourseNodeConfiguration.DEADLINE_WEEK, deadlineWeek.getSelectedKey());
		} else {
			config.remove(PortfolioCourseNodeConfiguration.DEADLINE_WEEK);
		}
		if(deadlineDay != null && deadlineDay.isOneSelected() && deadlineDay.getSelected() > 0) {
			config.set(PortfolioCourseNodeConfiguration.DEADLINE_DAY, deadlineDay.getSelectedKey());
		} else {
			config.remove(PortfolioCourseNodeConfiguration.DEADLINE_DAY);
		}
		return config;
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formOK(UserRequest ureq) {
		fireEvent (ureq, Event.DONE_EVENT);
		warningShown = false;
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source == deadlineType) {
			showWarningWhenInUse();
			updateUI();
		} else if (event.wasTriggerdBy(FormEvent.ONCHANGE)){
			showWarningWhenInUse();
		}
		fireEvent(ureq, Event.CHANGED_EVENT);
	}
	
	private void showWarningWhenInUse(){
		if (inUse && !warningShown) {
			showWarning("map.deadline.change.template.in.use");
			warningShown = true;
		}			
	}
	
	protected void updateUI() {
		if(deadlineType.isOneSelected()) {
			if(deadlineType.getSelected() == 0) {
				deadlineLayout.setVisible(false);
				deadlineChooser.setVisible(false);
			} else if(deadlineType.getSelected() == 1) {
				deadlineLayout.setVisible(false);
				deadlineChooser.setVisible(true);
			} else {
				deadlineLayout.setVisible(true);
				deadlineChooser.setVisible(false);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#validateFormLogic(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		if (deadlineType.getSelected() == 1) {
			Date newDeadLine = deadlineChooser.getDate();
			if (newDeadLine != null && newDeadLine.before(new Date())) {
				deadlineChooser.setErrorKey("map.deadline.invalid.before", null);
				return false;
			} else return true;
		}
		return super.validateFormLogic(ureq);
	}
	
	
}