/*******************************************************************************
 * Copyright (c) 2004, 2009 Tasktop Technologies and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     See git history
 *******************************************************************************/

package org.eclipse.mylyn.internal.tasks.ui;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITaskAttachment;
import org.eclipse.mylyn.tasks.core.ITaskComment;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorInput;

/**
 * Factory for adapting objects to task elements.
 *
 * @author Steffen Pingel
 */
public class TasksAdapterFactory implements IAdapterFactory {

	private static final Class<?>[] ADAPTER_LIST = new Class[] { ITask.class, TaskRepository.class };

	@Override
	public Class<?>[] getAdapterList() {
		return ADAPTER_LIST;
	}

	@Override
	public <T> T getAdapter(final Object adaptable, Class<T> adapterType) {
		if (adapterType == ITask.class) {
			if (adaptable instanceof TaskEditor) {
				return adapterType.cast(((TaskEditor) adaptable).getTaskEditorInput().getTask());
			} else if (adaptable instanceof TaskEditorInput) {
				return adapterType.cast(((TaskEditorInput) adaptable).getTask());
			} else if (adaptable instanceof ITaskAttachment) {
				return adapterType.cast(((ITaskAttachment) adaptable).getTask());
			} else if (adaptable instanceof ITaskComment) {
				return adapterType.cast(((ITaskComment) adaptable).getTask());
			}
		}
		if (adapterType == TaskRepository.class) {
			if (adaptable instanceof TaskEditor) {
				return adapterType.cast(((TaskEditor) adaptable).getTaskEditorInput().getTaskRepository());
			} else if (adaptable instanceof TaskEditorInput) {
				return adapterType.cast(((TaskEditorInput) adaptable).getTaskRepository());
			} else if (adaptable instanceof ITaskAttachment) {
				return adapterType.cast(((ITaskAttachment) adaptable).getTaskRepository());
			} else if (adaptable instanceof ITaskComment) {
				return adapterType.cast(((ITaskComment) adaptable).getTask());
			}
		}
		return null;
	}

}
