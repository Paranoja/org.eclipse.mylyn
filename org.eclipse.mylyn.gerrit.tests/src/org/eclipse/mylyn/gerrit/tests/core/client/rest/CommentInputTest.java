/*******************************************************************************
 * Copyright (c) 2013, 2015 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.mylyn.gerrit.tests.core.client.rest;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.eclipse.mylyn.commons.sdk.util.CommonTestUtil;
import org.eclipse.mylyn.internal.gerrit.core.client.JSonSupport;
import org.eclipse.mylyn.internal.gerrit.core.client.rest.CommentInput;
import org.junit.Test;

public class CommentInputTest extends TestCase {

	@Test
	public void testFromEmptyJson() throws Exception {
		CommentInput CommentInput = parseFile("testdata/EmptyWithMagic.json");

		assertNotNull(CommentInput);
		assertNull(CommentInput.getId());
		assertEquals("gerritcodereview#comment", CommentInput.getKind());
		assertNull(CommentInput.getMessage());
		assertNull(CommentInput.getPath());
		assertEquals(0, CommentInput.getLine());
	}

	@Test
	public void testFromInvalid() throws Exception {
		CommentInput CommentInput = parseFile("testdata/InvalidWithMagic.json");

		assertNotNull(CommentInput);
		assertNull(CommentInput.getId());
		assertEquals("gerritcodereview#comment", CommentInput.getKind());
		assertNull(CommentInput.getMessage());
		assertNull(CommentInput.getPath());
		assertEquals(0, CommentInput.getLine());
	}

	@Test
	public void testFromCodeReviewMinusOne() throws Exception {
		CommentInput CommentInput = parseFile("testdata/CommentInput_draft.json");

		assertNotNull(CommentInput);
		assertEquals("gerritcodereview#comment", CommentInput.getKind());
		assertEquals("daeb3561_e122c600", CommentInput.getId());
		assertEquals("Line 2 Comment", CommentInput.getMessage());
		assertEquals(2, CommentInput.getLine());
	}

	private CommentInput parseFile(String path) throws IOException {
		File file = CommonTestUtil.getFile(this, path);
		String content = CommonTestUtil.read(file);
		return new JSonSupport().parseResponse(content, CommentInput.class);
	}

}
