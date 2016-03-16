/*******************************************************************************
 * Copyright (c) 2014, 2016 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.internal.reviews.ui.editors.parts;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.mylyn.internal.reviews.ui.editors.parts.messages"; //$NON-NLS-1$

	public static String AbstractCommentPart_Draft;

	public static String AbstractCommentPart_No_author;

	public static String AbstractCommentPart_Section_header;

	public static String TaskEditorReviewsPart_ReviewsString;

	public static String TaskEditorReviewsPart_VerifiedString;

	public static String TaskEditorReviewsPart_CodeReviewString;

	public static String TaskEditorReviewsPart_DescriptionString;

	public static String TaskEditorReviewsPart_VerifiedAttribute;

	public static String TaskEditorReviewsPart_CodeReviewAttribute;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
