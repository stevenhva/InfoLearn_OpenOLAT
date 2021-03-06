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
package org.olat.course.config.ui.courselayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.image.ImageComponent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.logging.AssertException;
import org.olat.core.util.ArrayHelper;
import org.olat.core.util.FileUtils;
import org.olat.core.util.ImageHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSMediaResource;
import org.olat.core.util.vfs.filters.VFSItemSuffixFilter;
import org.olat.course.config.CourseConfig;
import org.olat.course.config.ui.courselayout.attribs.AbstractLayoutAttribute;
import org.olat.course.config.ui.courselayout.attribs.PreviewLA;
import org.olat.course.config.ui.courselayout.attribs.SpecialAttributeFormItemHandler;
import org.olat.course.config.ui.courselayout.elements.AbstractLayoutElement;
import org.olat.course.run.environment.CourseEnvironment;

/**
 * Description:<br>
 * Present different templates for course-layouts and let user generate his own.
 * 
 * <P>
 * Initial Date:  01.02.2011 <br>
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class CourseLayoutGeneratorController extends FormBasicController {

	private static final String ELEMENT_ATTRIBUTE_DELIM = "::";
	private static final String PREVIEW_IMAGE_NAME = "preview.png";
	private CourseConfig courseConfig;
	private SingleSelection styleSel;
	private FileElement logoUpl;
	private FormLayoutContainer previewImgFlc;
	private CourseEnvironment courseEnvironment;
	private FormLayoutContainer styleFlc;
	private CustomConfigManager customCMgr;
	private LinkedHashMap<String, Map<String, FormItem>> guiWrapper;
	private Map<String, Map<String, Object>> persistedCustomConfig;
	private FormLayoutContainer logoImgFlc;
	private FormLink logoDel;
	private boolean elWithErrorExists = false;
	private final boolean editable;

	public CourseLayoutGeneratorController(UserRequest ureq, WindowControl wControl, CourseConfig courseConfig,
			CourseEnvironment courseEnvironment, boolean editable) {
		super(ureq, wControl);
		
		this.editable = editable;
		this.courseConfig = courseConfig;
		this.courseEnvironment = courseEnvironment;
		customCMgr = (CustomConfigManager) CoreSpringFactory.getBean("courseConfigManager");
		// stack the translator to get attribs/elements
		PackageTranslator pt = new PackageTranslator(AbstractLayoutAttribute.class.getPackage().getName(), ureq.getLocale(), getTranslator());
		pt = new PackageTranslator(AbstractLayoutElement.class.getPackage().getName(), ureq.getLocale(), pt);
		setTranslator(pt);
		persistedCustomConfig = customCMgr.getCustomConfig(courseEnvironment);
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer, org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormTitle("tab.layout.title");
		
		ArrayList<String> keys = new ArrayList<String>();
		ArrayList<String> vals = new ArrayList<String>();
		ArrayList<String> csss = new ArrayList<String>();

		String actualCSSSettings = courseConfig.getCssLayoutRef();
		
		// add a default option
		keys.add(CourseLayoutHelper.CONFIG_KEY_DEFAULT);
		vals.add(translate("course.layout.default"));
		csss.add("");
		
		// check for old legacy template, only available if yet one set
		if(actualCSSSettings.startsWith("/") && actualCSSSettings.lastIndexOf("/") == 0) {
			keys.add(actualCSSSettings);
			vals.add(translate("course.layout.legacy", actualCSSSettings));
			csss.add("");
		} 
		
		// add css from hidden coursecss-folder
		VFSContainer coursecssCont = (VFSContainer) courseEnvironment.getCourseFolderContainer().resolve(CourseLayoutHelper.COURSEFOLDER_CSS_BASE);
		if (coursecssCont != null) {
			coursecssCont.setDefaultItemFilter(new VFSItemSuffixFilter(new String[]{"css"}));
			List<VFSItem> coursecssStyles = coursecssCont.getItems();
			if (coursecssStyles != null) {
				for (VFSItem vfsItem : coursecssStyles) {
					keys.add(CourseLayoutHelper.COURSEFOLDER_CSS_BASE + "/" + vfsItem.getName());
					vals.add(translate("course.layout.legacy", vfsItem.getName()));
					csss.add("");
				}
			}
		}

		// get the olat-wide templates
		List<VFSItem> templates = CourseLayoutHelper.getCourseThemeTemplates();
		if (templates != null) {
			for (VFSItem vfsItem : templates) {
				if (CourseLayoutHelper.isCourseThemeFolderValid((VFSContainer) vfsItem)){
					keys.add(CourseLayoutHelper.CONFIG_KEY_TEMPLATE + vfsItem.getName());
					String name = translate("course.layout.template", vfsItem.getName());
					vals.add(name);
					csss.add("");
				}
			}
		}
		
		// get the predefined template for this course if any
		VFSItem predefCont = courseEnvironment.getCourseBaseContainer().resolve(CourseLayoutHelper.LAYOUT_COURSE_SUBFOLDER + "/" + CourseLayoutHelper.CONFIG_KEY_PREDEFINED);
		if (predefCont != null && CourseLayoutHelper.isCourseThemeFolderValid((VFSContainer) predefCont)) {
			keys.add(CourseLayoutHelper.CONFIG_KEY_PREDEFINED);
			vals.add(translate("course.layout.predefined"));
			csss.add("");
		}

		// add option for customizing
		keys.add(CourseLayoutHelper.CONFIG_KEY_CUSTOM);
		vals.add(translate("course.layout.custom"));
		csss.add("");
		
		String[] theKeys = ArrayHelper.toArray(keys);
		String[] theValues = ArrayHelper.toArray(vals);
		String[] theCssClasses = ArrayHelper.toArray(csss);
		
		styleSel = uifactory.addDropdownSingleselect("course.layout.selector", formLayout, theKeys, theValues, theCssClasses);
		styleSel.addActionListener(this, FormEvent.ONCHANGE);
		styleSel.setEnabled(editable);
		if (keys.contains(actualCSSSettings)){
			styleSel.select(actualCSSSettings, true);
		} else {
			styleSel.select(CourseLayoutHelper.CONFIG_KEY_DEFAULT, true);
		}

		previewImgFlc = FormLayoutContainer.createCustomFormLayout("preview.image", getTranslator(), velocity_root + "/image.html");
		formLayout.add(previewImgFlc);
		previewImgFlc.setLabel("preview.image.label", null);		
		refreshPreviewImage(actualCSSSettings);		
		
		logoImgFlc = FormLayoutContainer.createCustomFormLayout("logo.image", getTranslator(), velocity_root + "/image.html");
		formLayout.add(logoImgFlc);
		logoImgFlc.setLabel("logo.image.label", null);		
		refreshLogoImage();	
		
		// offer upload for 2nd logo
		if(editable) {
			logoUpl = uifactory.addFileElement("upload.second.logo", formLayout);
			logoUpl.addActionListener(this, FormEvent.ONCHANGE);
			Set<String> mimeTypes = new HashSet<String>();
			mimeTypes.add("image/*");
			logoUpl.limitToMimeType(mimeTypes, "logo.file.type.error", null);
			logoUpl.setMaxUploadSizeKB(2048, "logo.size.error", null);
		}
		
		// prepare the custom layouter
		styleFlc = FormLayoutContainer.createCustomFormLayout("style", getTranslator(), velocity_root + "/style.html");
		formLayout.add(styleFlc);
		styleFlc.setLabel(null, null);
		enableDisableCustom(CourseLayoutHelper.CONFIG_KEY_CUSTOM.equals(actualCSSSettings));
		
		if(editable) {
			uifactory.addFormSubmitButton("course.layout.save", formLayout);
		}
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formInnerEvent(org.olat.core.gui.UserRequest, org.olat.core.gui.components.form.flexible.FormItem, org.olat.core.gui.components.form.flexible.impl.FormEvent)
	 */
	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if (source == styleSel) {
			String selection = styleSel.getSelectedKey();
			if (CourseLayoutHelper.CONFIG_KEY_CUSTOM.equals(selection)) {
				enableDisableCustom(true);
			} else {
				enableDisableCustom(false);
			}
			refreshPreviewImage(selection); // in any case!
		} else if (source == logoUpl && event.wasTriggerdBy(FormEvent.ONCHANGE)) {
			if (logoUpl.isUploadSuccess()) {
				File newFile = logoUpl.getUploadFile();
				String newFilename = logoUpl.getUploadFileName();
				boolean isValidFileType = newFilename.toLowerCase().matches(".*[.](png|jpg|jpeg|gif)");
				if (!isValidFileType) {
					logoUpl.setErrorKey("logo.file.type.error", null);
				} else {
					logoUpl.clearError();
				}
				
				if (processUploadedImage(newFile)){
					logoUpl.reset();
					showInfo("logo.upload.success");
					refreshLogoImage();
				} else {
					showError("logo.upload.error");
				}
			}
		} else if (source.getName().contains(ELEMENT_ATTRIBUTE_DELIM)){
			// some selections changed, refresh to get new preview
			prepareStyleEditor(compileCustomConfigFromGuiWrapper());
		} else if (source == logoDel){
			VFSItem logo = (VFSItem) logoDel.getUserObject();
			logo.delete();
			refreshLogoImage();
		}
	}
	
	private void enableDisableCustom(boolean onOff){
		if (onOff) prepareStyleEditor(persistedCustomConfig);
		styleFlc.setVisible(onOff);
		styleFlc.setEnabled(editable);
		if(logoUpl != null) logoUpl.setVisible(onOff);
		logoImgFlc.setVisible(onOff);
	}
	
	// process uploaded file according to image size and persist in <course>/layout/logo.xy
	private boolean processUploadedImage(File image){
		int height = 0;
		int width = 0;
		String size[] = customCMgr.getImageSize(image);
		if (size != null) {
			width = Integer.parseInt(size[0]);
			height = Integer.parseInt(size[1]);
		} else return false;
		// target file:
		String fileType = logoUpl.getUploadFileName().substring(logoUpl.getUploadFileName().lastIndexOf("."));
		VFSContainer base = (VFSContainer) courseEnvironment.getCourseBaseContainer().resolve(CourseLayoutHelper.LAYOUT_COURSE_SUBFOLDER);
		if (base == null) {
			base = courseEnvironment.getCourseBaseContainer().createChildContainer(CourseLayoutHelper.LAYOUT_COURSE_SUBFOLDER);
		}
		VFSContainer customBase = (VFSContainer) base.resolve("/" + CourseLayoutHelper.CONFIG_KEY_CUSTOM);
		if (customBase==null) {
			customBase = base.createChildContainer(CourseLayoutHelper.CONFIG_KEY_CUSTOM);
		}
		if (customBase.resolve("logo" + fileType) != null) customBase.resolve("logo" + fileType).delete();
		VFSLeaf targetFile = customBase.createChildLeaf("logo" + fileType);
		int maxHeight = CourseLayoutHelper.getLogoMaxHeight();
		int maxWidth = CourseLayoutHelper.getLogoMaxWidth();
		if (height > maxHeight || width > maxWidth){
			// scale image
			try {
				ImageHelper helper = CourseLayoutHelper.getImageHelperToUse();
				String extension = FileUtils.getFileSuffix(logoUpl.getUploadFileName());
				helper.scaleImage(image, extension, targetFile, maxWidth, maxHeight);
			} catch (Exception e) {
				logError("could not find to be scaled image", e);
				return false;
			}
		} else {
			// only persist without scaling
			InputStream in = null;
			OutputStream out = null;
			try {
				in = new FileInputStream(image);
				out = targetFile.getOutputStream(false);
				FileUtils.copy(in, out);
			} catch (FileNotFoundException e) {
				logError("Problem reading uploaded image to copy", e);
				return false;
			} finally {
				FileUtils.closeSafely(in);
				FileUtils.closeSafely(out);
			}
		}
		return true;
	}
	

	private void refreshPreviewImage(String template) {
		VFSContainer baseFolder = CourseLayoutHelper.getThemeBaseFolder(courseEnvironment, template);
		if (baseFolder != null) {
			VFSItem preview = baseFolder.resolve("/" + PREVIEW_IMAGE_NAME);
			if (preview != null) {
				ImageComponent image = new ImageComponent("preview");
				previewImgFlc.setVisible(true);
				previewImgFlc.put("preview", image);
				VFSMediaResource prevImage = new VFSMediaResource((VFSLeaf) preview);
				image.setMediaResource(prevImage);
				image.setMaxWithAndHeightToFitWithin(300, 300);
				return;
			}
		}
		previewImgFlc.setVisible(false);
		previewImgFlc.remove(previewImgFlc.getComponent("preview"));
	}
	
	private void refreshLogoImage(){
		VFSContainer baseFolder = CourseLayoutHelper.getThemeBaseFolder(courseEnvironment, CourseLayoutHelper.CONFIG_KEY_CUSTOM);
		VFSItem logo = customCMgr.getLogoItem(baseFolder);
		if (logo != null) {
			ImageComponent image = new ImageComponent("preview");
			logoImgFlc.setVisible(true);
			logoImgFlc.put("preview", image);
			VFSMediaResource prevImage = new VFSMediaResource((VFSLeaf) logo);
			image.setMediaResource(prevImage);
			image.setMaxWithAndHeightToFitWithin(300, 300);
			logoDel = uifactory.addFormLink("logo.delete", logoImgFlc, Link.BUTTON_XSMALL);
			logoDel.setUserObject(logo);
			logoDel.setVisible(editable);
			return;
		}	
		logoImgFlc.setVisible(false);
		logoImgFlc.remove(logoImgFlc.getComponent("preview"));
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(UserRequest ureq) {
		String selection = styleSel.getSelectedKey();
		courseConfig.setCssLayoutRef(selection);
		
		if(CourseLayoutHelper.CONFIG_KEY_CUSTOM.equals(selection)){
			Map<String, Map<String, Object>> customConfig = compileCustomConfigFromGuiWrapper();		
			customCMgr.saveCustomConfigAndCompileCSS(customConfig, courseEnvironment);
			persistedCustomConfig = customConfig;
			if (!elWithErrorExists) prepareStyleEditor(customConfig);
		}
		
		// inform course-settings-dialog about changes:
		fireEvent(ureq, Event.CHANGED_EVENT);
	}
	
	private Map<String, Map<String, Object>> compileCustomConfigFromGuiWrapper(){
		// get config from wrapper-object
		elWithErrorExists = false;
		Map<String, Map<String, Object>> customConfig = new HashMap<String, Map<String, Object>>();
		for (Iterator<Entry<String, Map<String, FormItem>>> iterator = guiWrapper.entrySet().iterator(); iterator.hasNext();) {
			Entry<String, Map<String, FormItem>> type =  iterator.next();
			String cIdent = type.getKey();
			Map<String, Object> elementConfig = new HashMap<String, Object>();
			Map<String, FormItem> element = type.getValue();
			for (Entry<String, FormItem> entry : element.entrySet()) {
				String attribName = entry.getKey();
				if (!attribName.equals(PreviewLA.IDENTIFIER)){ // exclude preview
					FormItem foItem = entry.getValue();
					String value = "";
					if (foItem instanceof SingleSelection) {
						value = ((SingleSelection)foItem).isOneSelected() ? ((SingleSelection)foItem).getSelectedKey() : "";
					} else if (foItem.getUserObject() != null && foItem.getUserObject() instanceof SpecialAttributeFormItemHandler) {
						// enclosed item
						SpecialAttributeFormItemHandler specHandler = (SpecialAttributeFormItemHandler) foItem.getUserObject();
						value = specHandler.getValue();						
						if (specHandler.hasError()) {
							elWithErrorExists = true;
						}
					} else {
						throw new AssertException("implement a getValue for this FormItem to get back a processable value.");
					}
					elementConfig.put(attribName, value);
				}
			}			
			customConfig.put(cIdent, elementConfig);
		}
		return customConfig;		
	}
	
	
	private void prepareStyleEditor(Map<String, Map<String, Object>> customConfig){
		guiWrapper = new LinkedHashMap<String, Map<String, FormItem>>(); //keep config order

		List<AbstractLayoutElement> allElements = customCMgr.getAllAvailableElements();
		List<AbstractLayoutAttribute> allAttribs = customCMgr.getAllAvailableAttributes();
		styleFlc.contextPut("allAttribs", allAttribs);
		styleFlc.setUserObject(this); // needed reference to get listener back.
		
		for (AbstractLayoutElement abstractLayoutElement : allElements) {			
			String elementType = abstractLayoutElement.getLayoutElementTypeName();
			Map<String, Object> elConf = customConfig.get(elementType);
			AbstractLayoutElement concreteElmt = abstractLayoutElement.createInstance(elConf);
			
			HashMap<String, FormItem> elAttribGui = new HashMap<String, FormItem>();

			List<AbstractLayoutAttribute> attributes = concreteElmt.getAvailableAttributes();
			for (AbstractLayoutAttribute attrib : attributes) {
				String compName = elementType + ELEMENT_ATTRIBUTE_DELIM + attrib.getLayoutAttributeTypeName();
				FormItem fi = attrib.getFormItem(compName, styleFlc);
				fi.addActionListener(this, FormEvent.ONCHANGE);
				elAttribGui.put(attrib.getLayoutAttributeTypeName(), fi);
			}			
			guiWrapper.put(elementType, elAttribGui);			
		}		
		styleFlc.contextPut("guiWrapper", guiWrapper);
	}
	
	
	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// nothing to dispose
	}


}
