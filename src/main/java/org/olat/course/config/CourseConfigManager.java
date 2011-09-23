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

package org.olat.course.config;

import org.olat.course.ICourse;

/**
 * Description: <br>
 * TODO: patrick Class Description for CourseConfigManager
 * <P>
 * 
 * Initial Date: Jun 3, 2005 <br>
 * @author patrick
 */
public interface CourseConfigManager {
	/**
	 * the filename used for saving the configuration information in the course
	 * folder
	 */
	public static final String COURSECONFIG_XML = "CourseConfig.xml";

	/**
	 * Copy the configuration of a given course.
	 * 
	 * @param course
	 * @return copied course configuration
	 */
	CourseConfig copyConfigOf(ICourse course);

	/**
	 * Delete the configuration for a given course, this means removing it from
	 * the persistent area. It is used as part of the course removing operation.
	 * Furthermore in connection with a <code>loadConfigFor(...)</code> it can
	 * be used to recreate a default <code>CourseConfig</code>.
	 * 
	 * @param course
	 */
	boolean deleteConfigOf(ICourse course);

	/**
	 * Load the configuration of course. If the configuration is not existing, a
	 * new default course configuration is created, initialized with default
	 * values and persisted. Otherwise the configuration is loaded from the file
	 * system. If the <code>CourseConfig</code>'s version does not match the
	 * loaded configuration version a procedure for converting/migrating to the
	 * new version is initiated.
	 * 
	 * @param course
	 * @return configuration for a course
	 */
	CourseConfig loadConfigFor(ICourse course);

	/**
	 * saves the configuration for the given course in the persistent area of the
	 * course.
	 * 
	 * @param course
	 * @param courseConfig
	 */
	void saveConfigTo(ICourse course, CourseConfig courseConfig);

}