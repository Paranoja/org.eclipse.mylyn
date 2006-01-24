/*******************************************************************************
 * Copyright (c) 2004 - 2006 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.internal.tasklist.planner.ui;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.mylar.core.util.MylarStatusHandler;
import org.eclipse.mylar.internal.tasklist.MylarTaskListPlugin;
import org.eclipse.mylar.internal.tasklist.MylarTaskListPrefConstants;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

/**
 * @author Ken Sueda
 * @author Mik Kersten
 */
public class TaskPlannerWizard extends Wizard implements INewWizard {

	private TaskPlannerWizardPage planningGamePage;

	public TaskPlannerWizard() {
		super();
		init();
	}

	@Override
	public boolean performFinish() {
		try {
			IWorkbenchPage page = MylarTaskListPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow()
					.getActivePage();
			if (page == null)
				return false;
			IEditorInput input = new TaskPlannerEditorInput(planningGamePage.getReportStartDate(), planningGamePage
					.getSelectedFilters(), MylarTaskListPlugin.getTaskListManager().getTaskList());
			page.openEditor(input, MylarTaskListPrefConstants.PLANNER_EDITOR_ID);
		} catch (PartInitException ex) {
			MylarStatusHandler.log(ex, "couldn't open summary editor");
		}
		return true;
	}

	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

	private void init() {
		planningGamePage = new TaskPlannerWizardPage();
		super.setForcePreviousAndNextButtons(true);
	}

	@Override
	public void addPages() {
		addPage(planningGamePage);
	}
}
