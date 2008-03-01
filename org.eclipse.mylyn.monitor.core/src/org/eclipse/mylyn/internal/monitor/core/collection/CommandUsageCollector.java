/*******************************************************************************
 * Copyright (c) 2004, 2007 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.mylyn.internal.monitor.core.collection;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.mylyn.monitor.core.InteractionEvent;

/**
 * @author Mik Kersten and Leah Findlater
 */
public class CommandUsageCollector implements IUsageCollector {

	private final InteractionByTypeSummary commands = new InteractionByTypeSummary();

	private final Set<Integer> userIdSet = new HashSet<Integer>();

	public void consumeEvent(InteractionEvent event, int userId) {
		userIdSet.add(userId);
		if (event.getKind().equals(InteractionEvent.Kind.COMMAND)) {
			commands.setUserCount(userId, InteractionEventUtil.getCleanOriginId(event), commands.getUserCount(userId,
					InteractionEventUtil.getCleanOriginId(event)) + 1);
		}
	}

	public List<String> getReport() {
		return Collections.emptyList();
	}

	public String getReportTitle() {
		return "Command Usage";
	}

	public void exportAsCSVFile(String directoryName) {
		// TODO Auto-generated method stub

	}

	public InteractionByTypeSummary getCommands() {
		return commands;
	}

	public List<String> getPlainTextReport() {
		return Collections.emptyList();
	}
}
