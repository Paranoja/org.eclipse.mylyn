/*******************************************************************************
 * Copyright (c) 2015 Frank Becker and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Frank Becker - initial API and implementation
 *     See git history
 *******************************************************************************/

package org.eclipse.mylyn.internal.bugzilla.rest.core;

import java.util.AbstractMap;
import java.util.Map;

import org.eclipse.mylyn.tasks.core.data.AbstractTaskSchema;
import org.eclipse.mylyn.tasks.core.data.DefaultTaskSchema;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

public class BugzillaRestCreateTaskSchema extends AbstractTaskSchema {

	private static final BugzillaRestCreateTaskSchema instance = new BugzillaRestCreateTaskSchema();

	public static BugzillaRestCreateTaskSchema getDefault() {
		return instance;
	}

	private static Map<String, String> attribute2FieldMapper = Map.ofEntries(
			new AbstractMap.SimpleEntry<>(getDefault().SUMMARY.getKey(), "summary"), //$NON-NLS-1$
			new AbstractMap.SimpleEntry<>(getDefault().DESCRIPTION.getKey(), "description"), //$NON-NLS-1$
			new AbstractMap.SimpleEntry<>(getDefault().OPERATION.getKey(), "status"), //$NON-NLS-1$
			new AbstractMap.SimpleEntry<>(getDefault().PRODUCT.getKey(), "product"), //$NON-NLS-1$
			new AbstractMap.SimpleEntry<>(getDefault().COMPONENT.getKey(), "component"), //$NON-NLS-1$
			new AbstractMap.SimpleEntry<>(getDefault().CC.getKey(), "cc"), //$NON-NLS-1$
			new AbstractMap.SimpleEntry<>(getDefault().SEVERITY.getKey(), "severity"), //$NON-NLS-1$
			new AbstractMap.SimpleEntry<>(getDefault().PRIORITY.getKey(), "priority"), //$NON-NLS-1$
			new AbstractMap.SimpleEntry<>(getDefault().ASSIGNED_TO.getKey(), "assigned_to"), //$NON-NLS-1$
			new AbstractMap.SimpleEntry<>(getDefault().OS.getKey(), "op_sys"), //$NON-NLS-1$
			new AbstractMap.SimpleEntry<>(getDefault().VERSION.getKey(), "version") //$NON-NLS-1$
			);

	public static String getFieldNameFromAttributeName(String attributeName) {
		String result = attribute2FieldMapper.get(attributeName);
		if (result == null) {
			result = attributeName;
		}
		return result;
	}

	private final DefaultTaskSchema parent = DefaultTaskSchema.getInstance();

	public final Field PRODUCT = inheritFrom(parent.PRODUCT).addFlags(Flag.REQUIRED).create();

	public final Field COMPONENT = inheritFrom(parent.COMPONENT).addFlags(Flag.REQUIRED)
			.dependsOn(PRODUCT.getKey())
			.create();

	public final Field SUMMARY = inheritFrom(parent.SUMMARY).addFlags(Flag.REQUIRED).create();

	public final Field VERSION = createField(TaskAttribute.VERSION, Messages.BugzillaRestCreateTaskSchema_Version, TaskAttribute.TYPE_SINGLE_SELECT, null,
			PRODUCT.getKey(), Flag.ATTRIBUTE, Flag.REQUIRED);

	public final Field DESCRIPTION = inheritFrom(parent.DESCRIPTION).addFlags(Flag.REQUIRED).create();

	public final Field OS = createField("os", "OS", TaskAttribute.TYPE_SINGLE_SELECT, Flag.ATTRIBUTE); //$NON-NLS-1$ //$NON-NLS-2$

	public final Field PLATFORM = createField("platform", Messages.BugzillaRestCreateTaskSchema_Platform, TaskAttribute.TYPE_SINGLE_SELECT, Flag.ATTRIBUTE); //$NON-NLS-1$

	public final Field PRIORITY = inheritFrom(parent.PRIORITY).create();

	public final Field SEVERITY = inheritFrom(parent.SEVERITY).create();

	public final Field STATUS = inheritFrom(parent.STATUS).create();

	public final Field ALIAS = createField("alias", Messages.BugzillaRestCreateTaskSchema_Alias, TaskAttribute.TYPE_SHORT_TEXT, Flag.ATTRIBUTE); //$NON-NLS-1$

	public final Field ASSIGNED_TO = inheritFrom(parent.USER_ASSIGNED).label(Messages.BugzillaRestCreateTaskSchema_Assigned_To)
			.dependsOn(COMPONENT.getKey())
			.create();

	public final Field CC = createField(TaskAttribute.USER_CC, Messages.BugzillaRestCreateTaskSchema_CC, TaskAttribute.TYPE_PERSON, Flag.PEOPLE);

	public final Field DESCRIPTION_IS_PRIVATE = createField("description_is_private", Messages.BugzillaRestCreateTaskSchema_Description_Private, //$NON-NLS-1$
			TaskAttribute.TYPE_BOOLEAN, Flag.ATTRIBUTE);

	public final Field QA_CONTACT = createField("qa_contact", Messages.BugzillaRestCreateTaskSchema_QA_Contact, TaskAttribute.TYPE_PERSON, null, //$NON-NLS-1$
			COMPONENT.getKey(), Flag.PEOPLE);

	public final Field TARGET_MILESTONE = createField("target_milestone", Messages.BugzillaRestCreateTaskSchema_Target_Milestone, //$NON-NLS-1$
			TaskAttribute.TYPE_SINGLE_SELECT, null, PRODUCT.getKey(), Flag.ATTRIBUTE, Flag.REQUIRED);

	public final Field RESOLUTION = inheritFrom(parent.RESOLUTION).create();

	public final Field OPERATION = createField(TaskAttribute.OPERATION, Messages.BugzillaRestCreateTaskSchema_Operation, TaskAttribute.TYPE_OPERATION);

	public final Field DEPENDS_ON = createField("depends_on", Messages.BugzillaRestCreateTaskSchema_Depends_On, TaskAttribute.TYPE_TASK_DEPENDENCY, //$NON-NLS-1$
			Flag.ATTRIBUTE);

	public final Field BLOCKS = createField("blocks", Messages.BugzillaRestCreateTaskSchema_Blocks, TaskAttribute.TYPE_TASK_DEPENDENCY, Flag.ATTRIBUTE); //$NON-NLS-1$

	public final Field KEYWORDS = createField("keywords", Messages.BugzillaRestCreateTaskSchema_Keywords, IBugzillaRestConstants.EDITOR_TYPE_KEYWORD, //$NON-NLS-1$
			Flag.ATTRIBUTE);
}
