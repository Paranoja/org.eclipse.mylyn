/*******************************************************************************
 * Copyright (c) 2010 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *     Itema AS - bug 33148 server tooltip show a summary of build plans
 *******************************************************************************/

package org.eclipse.mylyn.internal.builds.ui;

import java.util.Date;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.mylyn.builds.core.IBuild;
import org.eclipse.mylyn.builds.core.IBuildElement;
import org.eclipse.mylyn.builds.core.IBuildPlan;
import org.eclipse.mylyn.builds.core.IBuildServer;
import org.eclipse.mylyn.builds.core.IHealthReport;
import org.eclipse.mylyn.builds.internal.core.BuildServer;
import org.eclipse.mylyn.builds.ui.BuildsUi;
import org.eclipse.mylyn.builds.ui.spi.BuildConnectorUi;
import org.eclipse.mylyn.commons.core.DateUtil;
import org.eclipse.mylyn.internal.builds.ui.view.BuildSummaryLabelProvider;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonFonts;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonUiUtil;
import org.eclipse.mylyn.internal.provisional.commons.ui.RichToolTip;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

/**
 * @author Steffen Pingel
 * @author Torkild U. Resheim
 */
public class BuildToolTip extends RichToolTip {

	private final static int MAX_TEXT_WIDTH = 300;

	private final static int MAX_WIDTH = 600;

	public BuildToolTip(Control control) {
		super(control);
	}

//	@Override
//	protected Object computeData(Widget hoverWidget) {
//		Object data = super.computeData(hoverWidget);
//		if (data instanceof IBuildPlan || data instanceof IBuildServer) {
//			return data;
//		}
//		return null;
//	}

	@Override
	public ViewerCell getData() {
		Object data = super.getData();
		return (ViewerCell) data;
	}

	@Override
	protected Composite createToolTipArea(Event event, Composite parent) {
		ViewerCell cell = getData();
		IBuildElement data = (IBuildElement) cell.getItem().getData();

		parent.setLayout(new GridLayout(2, false));

		BuildConnectorUi connectorUi = BuildsUi.getConnectorUi(data.getServer());
		addIconAndLabel(parent, CommonImages.getImage(connectorUi.getImageDescriptor()), data.getLabel(), true);

		Date refreshDate = data.getRefreshDate();
		if (refreshDate != null) {
			String refreshString = DateUtil.getRelative(refreshDate.getTime());
			addIconAndLabel(parent, null, NLS.bind("Refreshed {0}", refreshString), false);
		}

		if (data instanceof IBuildPlan) {
			IBuildPlan plan = (IBuildPlan) data;
			if (plan.getLastBuild() != null) {
				addBuild(parent, plan.getLastBuild());
			}
			addPlan(parent, plan);
		}

		if (data instanceof IBuildElement) {
			if (data.getElementStatus() != null) {
				addIconAndLabel(parent, CommonImages.getImage(CommonImages.WARNING), data.getElementStatus()
						.getMessage());
			}
		}
		if (data instanceof IBuildServer) {
			int passed = 0;
			int failed = 0;
			int unstable = 0;
			int aborted = 0;
			int disabled = 0;
			List<IBuildPlan> plans = BuildsUiInternal.getModel().getPlans((BuildServer) data);
			for (IBuildPlan iBuildPlan : plans) {
				if (iBuildPlan.isSelected()) {
					switch (iBuildPlan.getStatus()) {
					case SUCCESS:
						passed++;
						break;
					case FAILED:
						failed++;
						break;
					case UNSTABLE:
						unstable++;
						break;
					case ABORTED:
						aborted++;
						break;
					case DISABLED:
						disabled++;
						break;
					default:
						break;
					}
				}
			}
			addIconAndLabel(parent, CommonImages.getImage(BuildImages.STATUS_PASSED), NLS.bind(
					"{0} passed builds of a total of {1}", new Object[] { passed, plans.size() }));
			addIconAndLabel(parent, CommonImages.getImage(BuildImages.STATUS_FAILED), NLS.bind("{0} failed builds",
					new Object[] { failed }));
			addIconAndLabel(parent, CommonImages.getImage(BuildImages.STATUS_UNSTABLE), NLS.bind("{0} unstable builds",
					new Object[] { unstable }));
			addIconAndLabel(parent, CommonImages.getImage(BuildImages.STATUS_DISABLED), NLS.bind("{0} disabled builds",
					new Object[] { disabled }));
			addIconAndLabel(parent, CommonImages.getImage(BuildImages.STATUS_DISABLED), NLS.bind("{0} aborted builds",
					new Object[] { unstable }));

		}

		return parent;
	}

	private void addBuild(Composite parent, IBuild build) {
		String text = "";
		String time = DateUtil.getRelative(build.getTimestamp());
		if (time.length() > 0) {
			text = NLS.bind("Last built {0}, ", time);
		}
		addLabel(parent, text + NLS.bind("took {0}", DateUtil.getFormattedDurationShort(build.getDuration())));
		addLabel(parent, NLS.bind("Build {0} [{1}]", build.getLabel(), build.getServer().getLabel()));
	}

	private void addPlan(Composite parent, IBuildPlan plan) {
		for (IHealthReport healthReport : plan.getHealthReports()) {
			addIconAndLabel(parent, BuildSummaryLabelProvider.getHealthImageDescriptor(healthReport.getHealth()),
					healthReport.getDescription());
		}
	}

	protected void addLabel(Composite parent, String text) {
		addIconAndLabel(parent, (Image) null, text, false);
	}

	protected void addIconAndLabel(Composite parent, ImageDescriptor descriptor, String text) {
		addIconAndLabel(parent, CommonImages.getImage(descriptor), text, false);
	}

	protected void addIconAndLabel(Composite parent, Image image, String text) {
		addIconAndLabel(parent, image, text, false);
	}

	protected void addIconAndLabel(Composite parent, Image image, String text, boolean bold) {
		Label imageLabel = new Label(parent, SWT.NONE);
		imageLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
		imageLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		imageLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING));
		imageLabel.setImage(image);

		Label textLabel = new Label(parent, SWT.WRAP);
		if (bold) {
			textLabel.setFont(CommonFonts.BOLD);
		}
		textLabel.setForeground(getTitleColor());
		textLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		textLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER));

		textLabel.setText(CommonUiUtil.toLabel(text));
		int width = Math.min(textLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, MAX_WIDTH);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).hint(width, SWT.DEFAULT).applyTo(textLabel);
	}

}
