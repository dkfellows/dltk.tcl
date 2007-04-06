package org.eclipse.dltk.tcl.internal.tclchecker;

import org.eclipse.dltk.tcl.internal.tclchecker.ui.preferences.TclCheckerPreferences;
import org.eclipse.dltk.validators.ValidatorConfigurationPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

public class TclCheckerConfigurationPage extends ValidatorConfigurationPage {
	TclCheckerPreferences prefs = new TclCheckerPreferences();
	public void applyChanges() {
//		prefs.applyData(null);
		prefs.performOk();
	}

	public void createControl(Composite parent, int columns) {
		Composite c = new Composite(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = columns;
		c.setLayoutData(gd);
		c.setLayout(new FillLayout());
		prefs.createContents(c);
	}

}
