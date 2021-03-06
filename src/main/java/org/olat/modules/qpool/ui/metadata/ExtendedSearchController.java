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
package org.olat.modules.qpool.ui.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.ExtendedFlexiTableSearchController;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.nodes.INode;
import org.olat.modules.qpool.QPoolService;
import org.olat.modules.qpool.TaxonomyLevel;
import org.olat.modules.qpool.model.QItemDocument;
import org.olat.modules.qpool.model.QLicense;
import org.olat.modules.qpool.ui.QuestionsController;
import org.olat.modules.qpool.ui.admin.TaxonomyTreeModel;
import org.olat.modules.qpool.ui.metadata.MetaUIFactory.KeyValues;
import org.olat.search.model.AbstractOlatDocument;

/**
 * 
 * Initial date: 03.05.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class ExtendedSearchController extends FormBasicController implements ExtendedFlexiTableSearchController {
	
	private FormLink searchButton;
	
	private final SearchAttributes searchAttributes = new SearchAttributes();
	private final List<ConditionalQuery> uiQueries = new ArrayList<ConditionalQuery>();
	private final List<String> condQueries = new ArrayList<String>();
	
	private final String prefsKey;
	private ExtendedSearchPrefs prefs;
	
	private final QPoolService qpoolService;

	public ExtendedSearchController(UserRequest ureq, WindowControl wControl, String prefsKey) {
		super(ureq, wControl, "extended_search");
		setTranslator(Util.createPackageTranslator(QuestionsController.class, getLocale(), getTranslator()));
		
		qpoolService = CoreSpringFactory.getImpl(QPoolService.class);
		
		this.prefsKey = prefsKey;
		prefs = (ExtendedSearchPrefs) ureq.getUserSession().getGuiPreferences()
				.get(ExtendedFlexiTableSearchController.class, prefsKey);
		
		if(prefs != null && prefs.getCondQueries().size() > 0) {
			for(ExtendedSearchPref pref:prefs.getCondQueries()) {
				uiQueries.add(new ConditionalQuery(pref));
			}

		} else {
			uiQueries.add(new ConditionalQuery());
		}
		
		
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		if(formLayout instanceof FormLayoutContainer) {
			FormLayoutContainer layoutCont = (FormLayoutContainer)formLayout;
			layoutCont.contextPut("uiQueries", uiQueries);
		}
		
		FormLayoutContainer buttonsCont = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
		buttonsCont.setRootForm(mainForm);
		formLayout.add(buttonsCont);
		searchButton = uifactory.addFormLink("search", buttonsCont, Link.BUTTON);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		doSearch(ureq);
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source == searchButton) {
			doSearch(ureq);
		} else if (source instanceof SingleSelection) {
			SingleSelection attrEl = (SingleSelection)source;
			if(attrEl.isOneSelected()) {
				ConditionalQuery query = (ConditionalQuery)attrEl.getUserObject();
				query.selectAttributeType(attrEl.getSelectedKey(), null);
			}
		} else if(source instanceof FormLink) {
			FormLink button = (FormLink)source;
			ConditionalQuery query = (ConditionalQuery)button.getUserObject();
			if(button.getCmd().startsWith("add")) {
				addParameter(query);
			} else if(button.getCmd().startsWith("remove")) {
				removeParameter(query);
			}
		}
		super.formInnerEvent(ureq, source, event);
	}
	
	private void addParameter(ConditionalQuery query) {
		int index = uiQueries.indexOf(query);
		ConditionalQuery newQuery = new ConditionalQuery();
		if(index < 0 || (index + 1) > uiQueries.size()) {
			uiQueries.add(newQuery);
		} else {
			uiQueries.add(index+1, newQuery);
		}
	}
	
	private void removeParameter(ConditionalQuery query) {
		if(uiQueries.size() > 1 && uiQueries.remove(query)) {
			flc.setDirty(true);
		}
	}
	
	private void doSearch(UserRequest ureq) {
		condQueries.clear();
		
		List<ExtendedSearchPref> params = new ArrayList<ExtendedSearchPref>();
		for(ConditionalQuery uiQuery:uiQueries) {
			String query = uiQuery.getQuery();
			if(StringHelper.containsNonWhitespace(query)) {
				condQueries.add(query);
				
				params.add(new ExtendedSearchPref(uiQuery.getAttribute(), uiQuery.getValue()));
			}
		}
		
		if (prefs == null){
			prefs = new ExtendedSearchPrefs();
		}
		prefs.setCondQueries(params);
		ureq.getUserSession().getGuiPreferences().putAndSave(ExtendedFlexiTableSearchController.class, prefsKey, prefs);
		fireEvent(ureq, Event.DONE_EVENT);
	}

	/**
	 * Append 'AND' operation if buf is not empty.
	 * @param buf
	 */
	private String append(String... strings) {
		StringBuilder query = new StringBuilder();
		for(String string:strings) {
			query.append(string);
		}
		return query.toString();
	}
	
	@Override
	public List<String> getConditionalQueries() {
		return condQueries;
	}

	@Override
	protected void doDispose() {
		//
	}
	
	public class ConditionalQuery {
		
		private SingleSelection attributeChoice;
		private FormItem parameter;
		private QueryParameterFactory parameterFactory;
		private FormLink addButton;
		private FormLink removeButton;

		public ConditionalQuery() {
			this(null);
		}
		
		public ConditionalQuery(ExtendedSearchPref pref) {
			long id = CodeHelper.getRAMUniqueID();

			String[] attrKeys = searchAttributes.getKeys();
			String[] attrValues = new String[attrKeys.length];
			for(int i=attrValues.length; i-->0; ) {
				attrValues[i] = translate(attrKeys[i]);
			}

			attributeChoice = uifactory.addDropdownSingleselect("attr-" + id, flc, attrKeys, attrValues, null);
			attributeChoice.select(attrKeys[0], true);
			if(pref == null) {
				selectAttributeType(attrKeys[0], null);
			} else {
				selectAttributeType(pref.getAttribute(), pref.getValue());
			}
			attributeChoice.addActionListener(ExtendedSearchController.this, FormEvent.ONCHANGE);
			attributeChoice.setUserObject(this);
			flc.add(attributeChoice.getName(), attributeChoice);
			addButton = uifactory.addFormLink("add-" + id, "add", null, flc, Link.BUTTON);
			addButton.setUserObject(this);
			flc.add(addButton.getComponent().getComponentName(), addButton);
			removeButton = uifactory.addFormLink("remove-"+ id, "remove", null, flc, Link.BUTTON);
			removeButton.setUserObject(this);
			flc.add(removeButton.getComponent().getComponentName(), removeButton);
		}
		
		public String getAttribute() {
			return attributeChoice.isOneSelected() ? attributeChoice.getSelectedKey() : null;
		}
		
		public String getValue() {
			return "test";
		}
		
		public SingleSelection getAttributChoice() {
			return attributeChoice;
		}
		
		public FormItem getParameterItem() {
			return parameter;
		}
		
		public FormLink getAddButton() {
			return addButton;
		}

		public FormLink getRemoveButton() {
			return removeButton;
		}

		public void selectAttributeType(String type, String value) {
			parameterFactory = searchAttributes.getQueryParameterFactory(type);
			if(parameterFactory != null) {
				parameter = parameterFactory.createItem(value);
			}
		}
		
		public String getQuery() {
			if(parameterFactory != null && parameter != null) {
				return parameterFactory.getQuery(parameter);
			}
			return null;
		}
	}

	public static interface QueryParameterFactory {
		public String getValue(FormItem item);
		
		public FormItem createItem(String startValue);
		
		public String getQuery(FormItem item);
	}
	
	private class SearchAttributes {
		private List<SearchAttribute> attributes = new ArrayList<SearchAttribute>();
		
		public SearchAttributes() {
			//general
			attributes.add(new SearchAttribute("general.title", new StringQueryParameter(AbstractOlatDocument.TITLE_FIELD_NAME)));
			attributes.add(new SearchAttribute("general.keywords", new StringQueryParameter(QItemDocument.KEYWORDS_FIELD)));
			attributes.add(new SearchAttribute("general.coverage", new StringQueryParameter(QItemDocument.COVERAGE_FIELD)));
			attributes.add(new SearchAttribute("general.additional.informations", new StringQueryParameter(QItemDocument.ADD_INFOS_FIELD)));
			attributes.add(new SearchAttribute("general.language", new StringQueryParameter(QItemDocument.LANGUAGE_FIELD)));
			attributes.add(new SearchAttribute("classification.taxonomic.path", new TaxonomicPathQueryParameter()));
			//educational
			attributes.add(new SearchAttribute("educational.context", new ContextQueryParameter()));
			//question
			attributes.add(new SearchAttribute("question.type", new TypeQueryParameter()));
			attributes.add(new SearchAttribute("question.assessmentType", new AssessmentQueryParameter()));
			//lifecycle
			attributes.add(new SearchAttribute("lifecycle.status", new StatusQueryParameter()));
			//technical
			attributes.add(new SearchAttribute("technical.editor", new StringQueryParameter(QItemDocument.EDITOR_FIELD)));
			attributes.add(new SearchAttribute("technical.format", new FormatQueryParameter()));
			//rights
			attributes.add(new SearchAttribute("rights.copyright", new LicenseQueryParameter()));	
		}
		
		public QueryParameterFactory getQueryParameterFactory(String type) {
			for(SearchAttribute attribute:attributes) {
				if(type.equals(attribute.getI18nKey())) {
					return attribute.getFactory();
				}
			}
			return null;
		}
		
		public String[] getKeys() {
			String[] keys = new String[attributes.size()];
			for(int i=keys.length; i-->0; ) {
				keys[i] = attributes.get(i).getI18nKey();
			}
			return keys;
		}
	}
	
	public class StringQueryParameter implements QueryParameterFactory {
		private final String docAttribute;
		
		public StringQueryParameter(String docAttribute) {
			this.docAttribute = docAttribute;
		}

		@Override
		public String getValue(FormItem item) {
			if(item instanceof TextElement) {
				return ((TextElement)item).getValue();
			}
			return null;
		}

		@Override
		public FormItem createItem(String startValue) {
			return uifactory.addTextElement("type-" + CodeHelper.getRAMUniqueID(), null, 50, startValue, flc);
		}

		@Override
		public String getQuery(FormItem item) {
			String val = getValue(item);
			if(StringHelper.containsNonWhitespace(val)) {
				return append(docAttribute, ":(", val, ") ");
			}
			return null;
		}
	}
	
	public class TaxonomicPathQueryParameter extends SingleChoiceQueryParameter {
		
		public TaxonomicPathQueryParameter() {
			super(QItemDocument.TAXONOMIC_PATH_FIELD);
		}
		
		@Override
		public FormItem createItem(String startValue) {
			TaxonomyTreeModel treeModel = new TaxonomyTreeModel("");
			List<String> keys = new ArrayList<String>();
			List<String> values = new ArrayList<String>();
			flatTree(treeModel.getRootNode(), "", keys, values);

			String[] keysArr = keys.toArray(new String[keys.size()]);
			String[] valuesArr = values.toArray(new String[values.size()]);
			return createItem(keysArr, valuesArr, startValue);
		}
		
		private void flatTree(TreeNode node, String path, List<String> keys, List<String> values) {
			for(int i=0; i<node.getChildCount(); i++) {
				INode child = node.getChildAt(i);
				if(child instanceof GenericTreeNode) {
					GenericTreeNode gChild = (GenericTreeNode)child;
					TaxonomyLevel level = (TaxonomyLevel)gChild.getUserObject();
					String field = level.getField();
					keys.add(field);
					values.add(path + "" + field);
					flatTree(gChild, path + "\u00A0\u00A0\u00A0\u00A0", keys, values);
				}
			}
		}
	}
	
	public class LicenseQueryParameter extends SingleChoiceQueryParameter {
		
		public LicenseQueryParameter() {
			super(QItemDocument.COPYRIGHT_FIELD);
		}
		
		@Override
		public FormItem createItem(String startValue) {
			List<QLicense> allLicenses = qpoolService.getAllLicenses();
			List<QLicense> licenses = new ArrayList<QLicense>(allLicenses);
			for(Iterator<QLicense> it=licenses.iterator(); it.hasNext(); ) {
				String key = it.next().getLicenseKey();
				if(key != null && key.startsWith("perso-")) {
					it.remove();
				}
			}

			String[] keys = new String[licenses.size()];
			int count = 0;
			for(QLicense license:licenses) {
				keys[count++] = license.getLicenseKey();
			}
			return createItem(keys, keys, startValue);
		}
	}
	
	public class TypeQueryParameter extends SingleChoiceQueryParameter {
		
		public TypeQueryParameter() {
			super(QItemDocument.ITEM_TYPE_FIELD);
		}
		
		@Override
		public FormItem createItem(String startValue) {
			KeyValues types = MetaUIFactory.getQItemTypeKeyValues(getTranslator(), qpoolService);
			return createItem(types.getKeys(), types.getValues(), startValue);
		}
	}
	
	public class FormatQueryParameter extends SingleChoiceQueryParameter {
		
		public FormatQueryParameter() {
			super(QItemDocument.FORMAT_FIELD);
		}
		
		@Override
		public FormItem createItem(String startValue) {
			KeyValues formats = MetaUIFactory.getFormats();
			return createItem(formats.getKeys(), formats.getValues(), startValue);
		}
	}
	
	public class ContextQueryParameter extends SingleChoiceQueryParameter {
		
		public ContextQueryParameter() {
			super(QItemDocument.EDU_CONTEXT_FIELD);
		}
		
		@Override
		public FormItem createItem(String startValue) {
			KeyValues contexts = MetaUIFactory.getContextKeyValues(getTranslator(), qpoolService);
			return createItem(contexts.getKeys(), contexts.getValues(), startValue);
		}
	}
	
	public class AssessmentQueryParameter extends SingleChoiceQueryParameter {
		
		public AssessmentQueryParameter() {
			super(QItemDocument.ASSESSMENT_TYPE_FIELD);
		}
		
		@Override
		public FormItem createItem(String startValue) {
			KeyValues types = MetaUIFactory.getAssessmentTypes(getTranslator());
			return createItem(types.getKeys(), types.getValues(), startValue);
		}
	}
	
	public class StatusQueryParameter extends SingleChoiceQueryParameter {
		public StatusQueryParameter() {
			super(QItemDocument.ITEM_STATUS_FIELD);
		}
		
		@Override
		public FormItem createItem(String startValue) {
			KeyValues types = MetaUIFactory.getStatus(getTranslator());
			return createItem(types.getKeys(), types.getValues(), startValue);
		}
	}
	
	public abstract class SingleChoiceQueryParameter implements QueryParameterFactory {
		private final String docAttribute;
		
		public SingleChoiceQueryParameter(String docAttribute) {
			this.docAttribute = docAttribute;
		}

		@Override
		public String getValue(FormItem item) {
			if(item instanceof SingleSelection && ((SingleSelection)item).isOneSelected()) {
				return ((SingleSelection)item).getSelectedKey();
			}
			return null;
		}

		protected FormItem createItem(String[] keys, String[] values, String startValue) {
			SingleSelection choice = uifactory.addDropdownSingleselect(docAttribute + "-" + CodeHelper.getRAMUniqueID(),  flc,
					keys, values, null);
			
			if(startValue != null) {
				for(String key:keys) {
					if(key.equals(startValue)) {
						choice.select(key, true);
						
					}
				}	
			}
			return choice;
		}
		
		@Override
		public String getQuery(FormItem item) {
			String val = getValue(item);
			if(StringHelper.containsNonWhitespace(val)) {
				return append(docAttribute, ":(", val, ") ");	
			}
			return null;
		}
	}
	
	private static class SearchAttribute {
		private final String i18nKey;
		private final QueryParameterFactory factory;
		
		public SearchAttribute(String i18nKey, QueryParameterFactory factory) {
			this.i18nKey = i18nKey;
			this.factory = factory;
		}

		public String getI18nKey() {
			return i18nKey;
		}

		public QueryParameterFactory getFactory() {
			return factory;
		}
	}
}