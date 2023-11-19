/*******************************************************************************
 * Copyright (c) 2013, 2015 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.wikitext.html.internal;

import org.eclipse.mylyn.wikitext.parser.Attributes;
import org.eclipse.mylyn.wikitext.parser.DocumentBuilder;
import org.eclipse.mylyn.wikitext.parser.DocumentBuilder.BlockType;

class UnsupportedBlockStrategy implements BlockStrategy {

	private static class DoubleLineBreakSeparator implements BlockSeparator {
		@Override
		public void emit(DocumentBuilder builder) {
			builder.lineBreak();
			builder.lineBreak();
		}
	}

	static final UnsupportedBlockStrategy instance = new UnsupportedBlockStrategy();

	@Override
	public void beginBlock(DocumentBuilder builder, BlockType type, Attributes attributes) {
	}

	@Override
	public void endBlock(DocumentBuilder builder) {
	}

	@Override
	public BlockSeparator trailingSeparator() {
		return new DoubleLineBreakSeparator();
	}

}
