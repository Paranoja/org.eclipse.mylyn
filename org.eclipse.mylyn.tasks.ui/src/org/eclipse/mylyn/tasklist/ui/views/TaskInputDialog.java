/*******************************************************************************
 * Copyright (c) 2004 - 2005 University Of British Columbia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     University Of British Columbia - initial API and implementation
 *******************************************************************************/

package org.eclipse.mylar.tasklist.ui.views;

import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.mylar.core.MylarPlugin;
import org.eclipse.mylar.core.util.ErrorLogger;
import org.eclipse.mylar.tasklist.MylarTaskListPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.TitleEvent;
import org.eclipse.swt.browser.TitleListener;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author Ken Sueda
 * @author Wesley Coelho (Extended to allow URL input)
 */
public class TaskInputDialog extends Dialog {

	private static final String LABEL_SHELL = "New Task";
	private static final String LABEL_DESCRIPTION = "Description:";
	private String taskName = "";
	private String priority = "P3";
	private String taskURL = "http://";
	private Date reminderDate = null;
	private Text taskNameTextWidget = null;
	private Text issueURLTextWidget = null;
	private Button getDescButton = null;

	public TaskInputDialog(Shell parentShell) {
		super(parentShell);
	}

	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		GridLayout gl = new GridLayout(4, false);
		composite.setLayout(gl);
		GridData data = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_CENTER);
		data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH + 100);
		composite.setLayoutData(data);

		Label taskNameLabel = new Label(composite, SWT.WRAP);
		taskNameLabel.setText(LABEL_DESCRIPTION);
		taskNameLabel.setFont(parent.getFont());

		taskNameTextWidget = new Text(composite, SWT.SINGLE | SWT.BORDER);
		taskNameTextWidget.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));

		final Combo c = new Combo(composite, SWT.NO_BACKGROUND | SWT.MULTI
				| SWT.V_SCROLL | SWT.READ_ONLY | SWT.DROP_DOWN);
		c.setItems(TaskListView.PRIORITY_LEVELS);
		c.setText(priority);
		c.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				priority = c.getText();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
			        
        final DatePicker datePicker = new DatePicker(composite, SWT.NULL);	
        datePicker.setDateText("<reminder>");
		datePicker.addPickerSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent arg0) {
				if (datePicker.getDate() != null){
					reminderDate = datePicker.getDate().getTime();
				}
			}

			public void widgetDefaultSelected(SelectionEvent arg0) {
				// ignore
			}
		});
		
		Label urlLabel = new Label(composite, SWT.WRAP);
		urlLabel.setText("Web Link:");
		urlLabel.setFont(parent.getFont());

		issueURLTextWidget = new Text(composite, SWT.SINGLE | SWT.BORDER);
		issueURLTextWidget.setText(getDefaultIssueURL());
		GridData urlData = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
		urlData.horizontalSpan = 2;
		issueURLTextWidget.setLayoutData(urlData);
		
		getDescButton = new Button(composite, SWT.PUSH);
		getDescButton.setText("Get Description");
		getDescButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		setButtonStatus();
		
		
		issueURLTextWidget.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				setButtonStatus();
			}
			public void keyReleased(KeyEvent e) {
				setButtonStatus();
			}
		});
		
		getDescButton.addSelectionListener(new SelectionListener(){
			public void widgetSelected(SelectionEvent e) {
				retrieveTaskDescription(issueURLTextWidget.getText());
			}
			public void widgetDefaultSelected(SelectionEvent e) {	
			}
		});
		
		return composite;
	}

	/**Sets the Get Description button enabled or not depending on whether there is a URL specified*/
	protected void setButtonStatus(){
		String url = issueURLTextWidget.getText();
		
		if (url.length() > 10 && (url.startsWith("http://") || url.startsWith("https://"))){
			String defaultPrefix = MylarPlugin.getDefault().getPreferenceStore().getString(MylarTaskListPlugin.DEFAULT_URL_PREFIX);
			if (url.equals(defaultPrefix)){
				getDescButton.setEnabled(false);
			}
			else{
				getDescButton.setEnabled(true);
			}
		}
		else{
			getDescButton.setEnabled(false);
		}
	}
	
	/**
	 * Returns the default URL text for the task by first
	 * checking the contents of the clipboard and then using
	 * the default prefix preference if that fails
	 */
	protected String getDefaultIssueURL(){
		
		String clipboardText = getClipboardText();
		if ((clipboardText.startsWith("http://") || clipboardText.startsWith("https://") && clipboardText.length() > 10)){
			return clipboardText;
		}
		
		String defaultPrefix = MylarPlugin.getDefault().getPreferenceStore().getString(MylarTaskListPlugin.DEFAULT_URL_PREFIX);
		if (!defaultPrefix.equals("")){
			return defaultPrefix;
		}
		
		return taskURL;
	}
	
	/**
	 * Attempts to set the task pageTitle to the title from
	 * the specified url
	 */
	protected void retrieveTaskDescription(final String url){
		
		try {
			final Shell shell = new Shell(Display.getDefault());
			shell.setVisible(false);
			Browser browser = new Browser(shell, SWT.NONE);
			
			RetrieveTaskDescriptionJob job = new RetrieveTaskDescriptionJob("Retrieving task description", issueURLTextWidget.getText());
			browser.addTitleListener(job);
			browser.setUrl(url);
			job.schedule();
			
		} catch (RuntimeException e) {
			ErrorLogger.fail(e, "could not open task web page", false);
		}		
	}
	
	/**
	 * Returns the contents of the clipboard or "" if
	 * no text content was available
	 */
	protected String getClipboardText(){
		Clipboard clipboard = new Clipboard(Display.getDefault());
		TextTransfer transfer = TextTransfer.getInstance();
		String contents = (String) clipboard.getContents(transfer);
		if (contents != null){
			return contents;
		} else {
			return "";
		}
	}

	public String getSelectedPriority() {
		return priority;
	}

	public String getTaskname() {
		return taskName;
	}
	
	public Date getReminderDate() {
		return reminderDate;
	}

	public String getIssueURL(){
		return taskURL;
	}
	
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			taskName = taskNameTextWidget.getText();
			taskURL = issueURLTextWidget.getText();
		} else {
			taskName = null;
		}
		super.buttonPressed(buttonId);
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(LABEL_SHELL);
	}

	/**
	 * Waits for the title from the browser
	 * @author Wesley Coelho
	 */
	private class RetrieveTaskDescriptionJob extends Job implements TitleListener{

		private final static long MAX_WAIT_TIME_MILLIS = 1000 * 30; //(30 Seconds)
		private final static long SLEEP_INTERVAL_MILLIS = 500;
		
		
		private String taskURL = null;
		private String pageTitle = null;
		private boolean retrievalFailed = false;
		private long timeWaitedMillis = 0;
		boolean ignoreChangeCall = false; //Determines when to ignore the second call to changed()
		
		public RetrieveTaskDescriptionJob(String name, String url) {
			super(name);
			taskURL = url;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			while(pageTitle == null && !retrievalFailed && (timeWaitedMillis <= MAX_WAIT_TIME_MILLIS)){
				
				try {
					Thread.sleep(SLEEP_INTERVAL_MILLIS);
				} catch (InterruptedException e) {
					ErrorLogger.fail(e, "Thread interrupted during sleep", false);
				}
				timeWaitedMillis += SLEEP_INTERVAL_MILLIS;
			}
			
			if (pageTitle != null){
				Display.getDefault().asyncExec(new Runnable(){
					public void run(){
						taskNameTextWidget.setText(pageTitle);
					}
				});
				return Status.OK_STATUS;
			}
			else{
				Display.getDefault().asyncExec(new Runnable(){
					public void run(){
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Task Description Error", "Could not retrieve a description from the specified web page.");
					}
				});				
				return Status.CANCEL_STATUS;
			}
			
		}
		

		public void changed(TitleEvent event) {
			if (!ignoreChangeCall){
				if (event.title.equals(taskURL)){
					return;
				}
				else{
					ignoreChangeCall = true;
					if (event.title.equals(taskURL + "/") ||
							event.title.equals("Object not found!") || 
							event.title.equals("No page to display") || 
							event.title.equals("Cannot find server") || 
							event.title.equals("Invalid Bug ID")){ //Last one is bugzilla-specific
						retrievalFailed = true;
					}
					else{
						pageTitle = event.title;
					}							
				}
			}
		}
		
	}
}
