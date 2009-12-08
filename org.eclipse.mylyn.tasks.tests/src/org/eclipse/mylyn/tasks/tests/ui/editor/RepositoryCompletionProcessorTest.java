/*******************************************************************************
 * Copyright (c) 2009 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylyn.tasks.tests.ui.editor;

import junit.framework.TestCase;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.mylyn.internal.tasks.core.TaskTask;
import org.eclipse.mylyn.internal.tasks.ui.TasksUiPlugin;
import org.eclipse.mylyn.internal.tasks.ui.editors.RepositoryCompletionProcessor;
import org.eclipse.mylyn.internal.tasks.ui.editors.RepositoryCompletionProcessor.TaskCompletionProposal;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.tests.TaskTestUtil;
import org.eclipse.mylyn.tasks.tests.ui.MockTextViewer;
import org.eclipse.swt.graphics.Point;

/**
 * @author Steffen Pingel
 */
public class RepositoryCompletionProcessorTest extends TestCase {

	private TaskRepository repository;

	@Override
	protected void setUp() throws Exception {
		TaskTestUtil.resetTaskListAndRepositories();
		repository = TaskTestUtil.createMockRepository();
	}

	@Override
	protected void tearDown() throws Exception {
		TaskTestUtil.resetTaskListAndRepositories();
	}

	public void testComputeCompletionProposals() {
		TaskTask taskFoo = addTask("1", "foo");
		TaskTask taskBar = addTask("2", "bar");

		RepositoryCompletionProcessor processor = new RepositoryCompletionProcessor(repository);
		ICompletionProposal[] proposals = processor.computeCompletionProposals(new MockTextViewer(""), 0);
		assertEquals(2, proposals.length);
		assertEquals(taskFoo, ((TaskCompletionProposal) proposals[0]).getTask());
		assertEquals(taskBar, ((TaskCompletionProposal) proposals[1]).getTask());

		proposals = processor.computeCompletionProposals(new MockTextViewer("abc"), 1);
		assertEquals(0, proposals.length);

	}

	public void testComputeCompletionProposalsPrefix() {
		TaskTask taskFoo = addTask("1", "foo");
		TaskTask taskBar = addTask("2", "bar");

		RepositoryCompletionProcessor processor = new RepositoryCompletionProcessor(repository);
		MockTextViewer viewer = new MockTextViewer("task");
		ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 4);
		assertEquals(2, proposals.length);
		TaskCompletionProposal proposal = (TaskCompletionProposal) proposals[0];
		assertEquals(taskFoo, proposal.getTask());
		assertEquals(taskBar, ((TaskCompletionProposal) proposals[1]).getTask());
		assertEquals("task 1", proposal.getReplacement());

		IDocument doc = viewer.getDocument();
		proposal.apply(doc);
		assertEquals("task 1", doc.get());
		assertEquals(new Point(6, 0), proposal.getSelection(doc));
	}

	public void testComputeCompletionProposalsPrefixSpace() {
		TaskTask taskFoo = addTask("1", "foo");
		TaskTask taskBar = addTask("2", "bar");

		RepositoryCompletionProcessor processor = new RepositoryCompletionProcessor(repository);
		MockTextViewer viewer = new MockTextViewer("task ");
		ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 5);
		assertEquals(2, proposals.length);
		TaskCompletionProposal proposal = (TaskCompletionProposal) proposals[0];
		assertEquals(taskFoo, proposal.getTask());
		assertEquals(taskBar, ((TaskCompletionProposal) proposals[1]).getTask());
		assertEquals("task 1", proposal.getReplacement());

		IDocument doc = viewer.getDocument();
		proposal.apply(doc);
		assertEquals("task 1", doc.get());
		assertEquals(new Point(6, 0), proposal.getSelection(doc));
	}

	public void testComputeCompletionProposalsParenthesis() {
		TaskTask taskFoo = addTask("1", "foo");
		TaskTask taskBar = addTask("2", "bar");

		RepositoryCompletionProcessor processor = new RepositoryCompletionProcessor(repository);
		MockTextViewer viewer = new MockTextViewer("(task");
		ICompletionProposal[] proposals = processor.computeCompletionProposals(viewer, 5);
		assertEquals(2, proposals.length);
		TaskCompletionProposal proposal = (TaskCompletionProposal) proposals[0];
		assertEquals(taskFoo, proposal.getTask());
		assertEquals(taskBar, ((TaskCompletionProposal) proposals[1]).getTask());
		assertEquals("task 1", proposal.getReplacement());

		IDocument doc = viewer.getDocument();
		proposal.apply(doc);
		assertEquals("(task 1", doc.get());
		assertEquals(new Point(7, 0), proposal.getSelection(doc));
	}

	private TaskTask addTask(String taskId, String summary) {
		TaskTask task = TaskTestUtil.createMockTask(taskId);
		task.setSummary(summary);
		TasksUiPlugin.getTaskList().addTask(task);
		return task;
	}

}
