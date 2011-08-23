/*******************************************************************************
 * Copyright (c) 2011 Ericsson Research Canada and others
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Ericsson - Initial API and implementation
 ******************************************************************************/

package org.eclipse.mylyn.internal.subclipse.ui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.internal.subclipse.ui.connector.SVNConnectorUi;
import org.eclipse.mylyn.versions.core.Change;
import org.eclipse.mylyn.versions.core.ChangeSet;
import org.eclipse.mylyn.versions.core.ChangeType;
import org.eclipse.mylyn.versions.core.ScmCore;
import org.eclipse.mylyn.versions.core.ScmRepository;
import org.eclipse.mylyn.versions.core.spi.ScmConnector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.history.provider.FileRevision;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

/**
 * This class implements the dialog used to fill-in the Find review items details This is a modeless-like dialog.
 * 
 * @author Alvaro Sanhez-Leon
 */
public class GetChangeSetDialog extends FormDialog {

	/**
	 * Field FIND_REVIEW_ITEMS_DIALOG_TITLE. (value is ""Find Review Items"")
	 */
	private static final String FIND_REVIEW_ITEMS_DIALOG_TITLE = "Find Review Items";

	/**
	 * Field FIND_REVIEW_ITEMS_DIALOG_VALUE. (value is ""Review Item Info"")
	 */
	private static final String FIND_REVIEW_ITEMS_DIALOG_VALUE = "Review Item Info";

	/**
	 * Field FIND_REVIEW_ITEMS_DESCRIPTION_DIALOG_VALUE. (value is ""Review Item Components"")
	 */
	private static final String FIND_REVIEW_ITEMS_DESCRIPTION_DIALOG_VALUE = "Review Item Components";

	/**
	 * Field COMMIT_INFO_HEADER_MSG. (value is ""Commit Information"")
	 */
	private static final String COMMIT_INFO_HEADER_MSG = "Commit Information";

	/**
	 * Field COMMIT_COMPONENTS_HEADER_MSG. (value is ""Committed Components"")
	 */
	private static final String COMMIT_COMPONENTS_HEADER_MSG = "Committed Components";

	/**
	 * Field DIALOG_COMBO_MAX_CHARACTERS. (value is 80)
	 */
	private static final int DIALOG_COMBO_MAX_CHARACTERS = 80;

	/**
	 * Field NEWLINE. (value is "\n")
	 */
	private static final String NEWLINE = "\n";

	/**
	 * Field COMMIT_CHUNK_SIZE.
	 */
	private static final int COMMIT_CHUNK_SIZE = 9;

	/**
	 * Field NUM_COMMIT_SHOWN. (value is 10)
	 */
	private static final int NUM_COMMIT_SHOWN = 10;

	/**
	 * Field MORE_ITEMS_LABEL. (value is ""<more items>"")
	 */
	private static final String MORE_ITEMS_LABEL = "<more items>";

	/**
	 * Field inputProject - Input project
	 */
	protected final IProject inputProject;

	/**
	 * Field commitList
	 */
	private CCombo commitList = null;

	/**
	 * Field selectedChangeSet.
	 */
	protected ChangeSet selectedChangeSet = null;

	/**
	 * Field messageText.
	 */
	Label messageText = null;

	/**
	 * Field idText.
	 */
	Label idText = null;

	/**
	 * Field authorNameText.
	 */
	Label authorNameText = null;

	/**
	 * Field authorEmailText.
	 */
	Label authorEmailText = null;

	/**
	 * Field dateText.
	 */
	Label dateText = null;

	/**
	 * Field repositoryNameText.
	 */
	Label repositoryNameText = null;

	/**
	 * Field changeList.
	 */
	org.eclipse.swt.widgets.List changeList = null;

	/**
	 * Field dateFormat.
	 */
	final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-dd hh:mm:ss");

	/**
	 * Field connector
	 */
	private ScmConnector connector = null;

	/**
	 * Field repository
	 */
	private ScmRepository repository = null;

	private Iterator<ChangeSet> changeSetsIter = null;

	/**
	 * Field filteredChangeSets - The ChangeSets that will be displayed
	 */
	private final List<ChangeSet> filteredChangeSets = new ArrayList<ChangeSet>();

	private final IProgressMonitor changeSetIterMonitor;

	/**
	 * Constructor for R4EReviewGroupInputDialog.
	 * 
	 * @param aParentShell
	 *            Shell
	 * @param aInputProject
	 *            IProject
	 * @param monitor
	 */
	public GetChangeSetDialog(Shell aParentShell, IProject aInputProject, IProgressMonitor monitor) {
		super(aParentShell);
		setBlockOnOpen(true);
		inputProject = aInputProject;
		if (monitor == null) {
			changeSetIterMonitor = new NullProgressMonitor();
		} else {
			changeSetIterMonitor = monitor;
		}
	}

	/**
	 * @param shell
	 *            Shell
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell aShell) {
		super.configureShell(aShell);
		aShell.setText(FIND_REVIEW_ITEMS_DIALOG_TITLE);
	}

	/**
	 * Configures the dialog form and creates form content. Clients should override this method.
	 * 
	 * @param mform
	 *            - the dialog form
	 */
	@Override
	protected void createFormContent(final IManagedForm mform) {

		try {
			connector = ScmCore.getConnector(inputProject);
			repository = connector.getRepository(inputProject, new NullProgressMonitor());
			changeSetsIter = connector.getChangeSetsIterator(repository, changeSetIterMonitor);

			final FormToolkit toolkit = mform.getToolkit();
			final ScrolledForm sform = mform.getForm();
			sform.setExpandVertical(true);

			// Main dialog composite
			final Composite composite = sform.getBody();
			composite.setLayout(new GridLayout(4, false));

			// Add Commit List in drop-down menu
			final Label label = toolkit.createLabel(composite, "Available Commits: ");
			label.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
			commitList = new CCombo(composite, SWT.WRAP | SWT.READ_ONLY);
			populateNextChangeSets();

			commitList.setTextLimit(DIALOG_COMBO_MAX_CHARACTERS);
			commitList.setVisibleItemCount(NUM_COMMIT_SHOWN);
			commitList.select(0);
			selectedChangeSet = filteredChangeSets.get(0);
			final GridData textGridData = new GridData(GridData.FILL, GridData.FILL, true, false);
			textGridData.horizontalSpan = 3;
			commitList.setLayoutData(textGridData);
			commitList.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int selectedIndex = commitList.getSelectionIndex();
					if (commitList.getItem(selectedIndex).equals(MORE_ITEMS_LABEL)) {
						populateNextChangeSets();
						commitList.select(selectedIndex);
						selectedChangeSet = filteredChangeSets.get(selectedIndex);
					} else {
						selectedChangeSet = filteredChangeSets.get(selectedIndex);
					}
					refresh();
				}
			});

			createReviewItemDetails(toolkit, sform);
			createReviewItemComponents(toolkit, sform);

		} catch (CoreException e) {
			StatusHandler.log(new Status(IStatus.ERROR, SVNConnectorUi.ID_PLUGIN, IStatus.OK, e.toString(), e));
		}
		return;
	}

	/**
	 * Gets the information and populates the local data structure with the next round of commit informations
	 */
	protected void populateNextChangeSets() {
		ChangeSet updatedChangeSet = null;
		String repoProject = null;
		int addedCount = 0;

		// Remove more items entry
		int selIndex = commitList.getSelectionIndex();
		if (selIndex > 0) {
			commitList.remove(selIndex);
		}

		while (changeSetsIter.hasNext()) {
			updatedChangeSet = changeSetsIter.next();

			// Only display commits that contains at least one file from the
			// selected project
			for (Change change : updatedChangeSet.getChanges()) {
				if (change.getTarget() != null) {
					repoProject = change.getTarget().getProjectName();
				} else if (change.getBase() != null) {
					repoProject = change.getBase().getProjectName();
				}
				if (repoProject != null && repoProject.equals(inputProject.getName())) {
					String[] tokens = updatedChangeSet.getMessage().split(NEWLINE, 2);
					commitList.add((tokens[0].length() > DIALOG_COMBO_MAX_CHARACTERS) ? tokens[0].substring(0,
							DIALOG_COMBO_MAX_CHARACTERS - 3) + "..." : tokens[0]);
					filteredChangeSets.add(updatedChangeSet);

					addedCount++;

					// If we already have all the commits from the current batch
					// exit
					if (addedCount % COMMIT_CHUNK_SIZE == 0) {
						commitList.add(MORE_ITEMS_LABEL);
						return;
					}
					break;
				}
			}
		}
		return;
	}

	/**
	 * Creates the Currently Selected Review Items Details Expandable Composite
	 * 
	 * @param aToolkit
	 *            FormToolkit
	 * @param aParent
	 *            Composite
	 */
	private void createReviewItemDetails(FormToolkit aToolkit, final ScrolledForm aParent) {

		GridData textGridData = null;

		// Basic parameters section
		final Section basicSection = aToolkit.createSection(aParent.getBody(), Section.DESCRIPTION
				| ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
		final GridData basicSectionGridData = new GridData(GridData.FILL, GridData.FILL, true, false);
		basicSectionGridData.horizontalSpan = 4;
		basicSection.setLayoutData(basicSectionGridData);
		basicSection.setText(COMMIT_INFO_HEADER_MSG);
		basicSection.setDescription(FIND_REVIEW_ITEMS_DIALOG_VALUE);
		basicSection.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				aParent.reflow(true);
				getShell().setSize(getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});
		final Composite basicSectionClient = aToolkit.createComposite(basicSection);
		basicSectionClient.setLayout(new GridLayout(4, false));
		basicSection.setClient(basicSectionClient);

		// Message
		final Label titleLabel = aToolkit.createLabel(basicSectionClient, "Title: ", SWT.WRAP);
		titleLabel.setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false));
		messageText = aToolkit.createLabel(basicSectionClient, selectedChangeSet.getMessage(), SWT.NONE);
		textGridData = new GridData(GridData.FILL, GridData.FILL, true, false);
		textGridData.horizontalSpan = 3;
		messageText.setLayoutData(textGridData);

		// Id
		final Label idLabel = aToolkit.createLabel(basicSectionClient, "ID: ", SWT.WRAP);
		idLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
		idText = aToolkit.createLabel(basicSectionClient, selectedChangeSet.getId(), SWT.NONE);
		textGridData = new GridData(GridData.FILL, GridData.FILL, true, false);
		textGridData.horizontalSpan = 3;
		idText.setLayoutData(textGridData);

		// Author Name
		final Label authorNameLabel = aToolkit.createLabel(basicSectionClient, "Author Name: ", SWT.WRAP);
		authorNameLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
		authorNameText = aToolkit.createLabel(basicSectionClient, selectedChangeSet.getAuthor().getName(), SWT.NONE);
		textGridData = new GridData(GridData.FILL, GridData.FILL, true, false);
		textGridData.horizontalSpan = 3;
		authorNameText.setLayoutData(textGridData);

		// Author Email
		final Label authorEmailLabel = aToolkit.createLabel(basicSectionClient, "Author Email: ", SWT.WRAP);
		authorEmailLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
		authorEmailText = aToolkit.createLabel(basicSectionClient, selectedChangeSet.getAuthor().getEmail(), SWT.NONE);
		textGridData = new GridData(GridData.FILL, GridData.FILL, true, false);
		textGridData.horizontalSpan = 3;
		authorEmailText.setLayoutData(textGridData);

		// Date
		final Label dateLabel = aToolkit.createLabel(basicSectionClient, "Date: ", SWT.WRAP);
		dateLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
		final String dateStr = dateFormat.format(selectedChangeSet.getDate());
		dateText = aToolkit.createLabel(basicSectionClient, dateStr, SWT.NONE);
		textGridData = new GridData(GridData.FILL, GridData.FILL, true, false);
		textGridData.horizontalSpan = 3;
		dateText.setLayoutData(textGridData);

		// Repository Name
		final Label messageLabel = aToolkit.createLabel(basicSectionClient, "Repository: ", SWT.WRAP);
		messageLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
		repositoryNameText = aToolkit.createLabel(basicSectionClient, selectedChangeSet.getRepository().getName(),
				SWT.NONE);
		textGridData = new GridData(GridData.FILL, GridData.FILL, true, false);
		textGridData.horizontalSpan = 3;
		repositoryNameText.setLayoutData(textGridData);

		basicSectionClient.layout();
	}

	/**
	 * Creates the Review Items Components Expandable Composite
	 * 
	 * @param aToolkit
	 *            FormToolkit
	 * @param aParent
	 *            Composite
	 */
	private void createReviewItemComponents(FormToolkit aToolkit, final ScrolledForm aParent) {

		// Extra parameters section
		final Section extraSection = aToolkit.createSection(aParent.getBody(), Section.DESCRIPTION
				| ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED);
		final GridData extraSectionGridData = new GridData(GridData.FILL, GridData.FILL, true, true);
		extraSectionGridData.horizontalSpan = 4;
		extraSection.setLayoutData(extraSectionGridData);
		extraSection.setText(COMMIT_COMPONENTS_HEADER_MSG);
		extraSection.setDescription(FIND_REVIEW_ITEMS_DESCRIPTION_DIALOG_VALUE);
		extraSection.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				aParent.reflow(true);
				getShell().setSize(getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		final Composite extraSectionClient = aToolkit.createComposite(extraSection);
		extraSectionClient.setLayout(new GridLayout(4, false));
		extraSectionClient.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		extraSection.setClient(extraSectionClient);

		// Components List
		changeList = new org.eclipse.swt.widgets.List(extraSectionClient, SWT.V_SCROLL | SWT.H_SCROLL);
		for (Change change : selectedChangeSet.getChanges()) {
			changeList.add(getAdjustedPath(change));
		}
		final GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);
		changeList.setLayoutData(data);
	}

	/**
	 * Fetches and updates the local ChangeSet information with resolution of base changes
	 * 
	 * @param aSelectedChangeSet
	 *            ChangeSet
	 * @throws CoreException
	 */
	private ChangeSet updateChangeSet(ChangeSet aSelectedChangeSet) {
		String changeSetId = aSelectedChangeSet.getId();
		ChangeSet updatedChangeSet = null;

		// IFileRevision
		IFileRevision fileRevision = createFileRevision(changeSetId);
		try {
			updatedChangeSet = connector.getChangeSet(repository, fileRevision, new NullProgressMonitor());
		} catch (CoreException e) {
			StatusHandler.log(new Status(IStatus.ERROR, SVNConnectorUi.ID_PLUGIN, "Unable to resolve changeSet " //$NON-NLS-1$
					+ changeSetId, e));
			return aSelectedChangeSet; // return non-updated ChangeSet
		}
		return updatedChangeSet;
	}

	/**
	 * @param changeSetId
	 *            String
	 * @return IFileRevision
	 */
	private IFileRevision createFileRevision(final String changeSetId) {
		IFileRevision fileRevision = new FileRevision() {

			public IFileRevision withAllProperties(IProgressMonitor monitor) throws CoreException {
				return null;
			}

			public boolean isPropertyMissing() {
				return false;
			}

			public IStorage getStorage(IProgressMonitor monitor) throws CoreException {
				return null;
			}

			@Override
			public String getContentIdentifier() {
				return changeSetId;
			}

			public String getName() {
				return null;
			}
		};
		return fileRevision;
	}

	/**
	 * Refreshes the Form.
	 */
	void refresh() {
		messageText.setText(selectedChangeSet.getMessage());
		idText.setText(selectedChangeSet.getId());
		dateText.setText(dateFormat.format(selectedChangeSet.getDate())); // BUG!
		authorNameText.setText(selectedChangeSet.getAuthor().getName());
		authorEmailText.setText(selectedChangeSet.getAuthor().getEmail());
		repositoryNameText.setText(selectedChangeSet.getRepository().getUrl());
		changeList.removeAll();
		for (Change change : selectedChangeSet.getChanges()) {
			changeList.add(getAdjustedPath(change));
		}
	}

	/**
	 * @return boolean
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	@Override
	protected boolean isResizable() {
		return true;
	}


	public ChangeSet getChangeSet() {
		//return a deep parsed version of the changeset
		return updateChangeSet(selectedChangeSet);
	}

	/**
	 * Add a little Text Decoration if the File was Added or Removed
	 * 
	 * @param aChange
	 *            Change
	 * @return String
	 */
	private String getAdjustedPath(Change aChange) {
		if (aChange.getChangeType().equals(ChangeType.DELETED)) {
			return "[-] " + aChange.getBase().getPath();
		} else if (aChange.getChangeType().equals(ChangeType.ADDED)) {
			return "[+] " + aChange.getTarget().getPath();
		} else {
			return "     " + aChange.getTarget().getPath();
		}
	}
}
