/*******************************************************************************
 * Copyright (c) 2008 xored software, Inc.  
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html  
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Andrei Sobolev)
 *******************************************************************************/
package org.eclipse.dltk.tcl.validators.tests;


import org.eclipse.dltk.tcl.validators.internal.tests.ArgumentsDefinitionCheckTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for org.eclipse.dltk.tcl.validators.internal.tests");
		// $JUnit-BEGIN$
		// suite.addTestSuite(ChecksProjectTest.class);
		suite.addTestSuite(ArgumentsDefinitionCheckTest.class);
		// suite.addTestSuite(CheckMethodExistanceTest.class);
		// $JUnit-END$
		return suite;
	}

}