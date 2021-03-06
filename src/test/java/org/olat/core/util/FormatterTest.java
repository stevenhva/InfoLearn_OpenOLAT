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
package org.olat.core.util;

import org.apache.commons.lang.StringEscapeUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * This was the test code mebbed in the main method of the Formatter class
 * 
 */
public class FormatterTest {
	
	@Test
	public void testEscapeHtml() {
		Assert.assertEquals("abcdef&amp;&lt;&gt;", StringEscapeUtils.escapeHtml("abcdef&<>"));
		Assert.assertEquals("&amp;#256;&lt;ba&gt;abcdef&amp;&lt;&gt;", StringEscapeUtils.escapeHtml("&#256;<ba>abcdef&<>"));
		Assert.assertEquals("&amp;#256;\n&lt;ba&gt;\nabcdef&amp;&lt;&gt;", StringEscapeUtils.escapeHtml("&#256;\n<ba>\nabcdef&<>"));
	}
	
	@Test
	public void testTruncate() {
		Assert.assertEquals("abcdef", Formatter.truncate("abcdef", 0));
		Assert.assertEquals("abcdef", Formatter.truncate("abcdef", 2));
		Assert.assertEquals("a...", Formatter.truncate("abcdef", 4));
		Assert.assertEquals("abcdef", Formatter.truncate("abcdef", 6));
		Assert.assertEquals("abcdef", Formatter.truncate("abcdef", 7));
		Assert.assertEquals("abcdef", Formatter.truncate("abcdef", 8));
		Assert.assertEquals("abcdef", Formatter.truncate("abcdef", -2));
		Assert.assertEquals("abcdef", Formatter.truncate("abcdef", -4));
		Assert.assertEquals("abcdef", Formatter.truncate("abcdef", -6));
		Assert.assertEquals("abcdef", Formatter.truncate("abcdef", -7));
		Assert.assertEquals("abcdef", Formatter.truncate("abcdef", -8));
	}

	@Test
	public void testMakeStringFilesystemSave() {
		String ugly = "guido/\\:? .|*\"\"<><guidoöäü";
		Assert.assertEquals("guido%2F%5C%3A%3F+.%7C*%22%22%3C%3E%3Cguido%C3%B6%C3%A4%C3%BC", Formatter.makeStringFilesystemSave(ugly));
	}
}
