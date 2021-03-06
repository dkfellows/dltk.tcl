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

package org.eclipse.dltk.tcl.parser.tests;

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.dltk.tcl.ast.Script;
import org.eclipse.dltk.tcl.ast.TclCommand;
import org.eclipse.dltk.tcl.parser.TclErrorCollector;
import org.eclipse.dltk.tcl.parser.TclParser;
import org.eclipse.dltk.tcl.parser.TclParserUtils;
import org.eclipse.dltk.tcl.parser.TclVisitor;
import org.eclipse.dltk.tcl.parser.definitions.DefinitionManager;
import org.eclipse.dltk.tcl.parser.definitions.NamespaceScopeProcessor;

public class SwitchCommandTests extends TestCase {
	NamespaceScopeProcessor processor;

	public void test001() throws Exception {
		String source = "switch $some {a {puts $some}}";
		typedCheck(source, 0, 1);
	}

	public void test002() throws Exception {
		String source = "switch -glob -- -some {a concat2}";
		typedCheck(source, 0, 1);
	}

	public void test003() throws Exception {
		String source = "switch -glob -- $some {a \"puts $some\"}";
		typedCheck(source, 0, 1);
	}

	public void test004() throws Exception {
		String source = "switch -glob -- $some {a {puts $some} b \"puts $some\" c history}";
		typedCheck(source, 0, 3);
	}

	public void test006() throws Exception {
		String source = "switch -exact -- $caller {cancel {puts cancel}}";
		typedCheck(source, 0, 1);
	}

	public void test007() throws Exception {
		String source = "switch $caller {cancel return cancel2 return}";
		typedCheck(source, 0, 2);
	}

	public void test008() throws Exception {
		String source = "switch -- $caller {cancel return cancel2 return}";
		typedCheck(source, 0, 2);
	}

	public void test009() throws Exception {
		String source = "switch -- $caller cancel return cancel2 return2";
		typedCheck(source, 0, 2);
	}

	public void test010() throws Exception {
		// cancel - unknown command - ats_rmserver
		String source = "switch $caller {{cancel} {puts cancel}}";
		typedCheck(source, 0, 1);
	}

	public void test011() throws Exception {
		// -jobs unknown command - autoeasy_abort
		String source = "switch -exact -- $i { -jobs { set flag 1} }";
		typedCheck(source, 0, 1);
	}

	public void test012() throws Exception {
		// -jobs unknown command - autoeasy_abort
		String source = "switch -exact -- $i { \"jobs\" { set flag 1} }";
		typedCheck(source, 0, 1);
	}

	public void test013() throws Exception {
		String script = "switch -exact -regexp -glob \"\" {"
				+ "	[func] {puts py!}" + "	default {puts boo}" + " }";
		typedCheck(script, 0, 2);
	}

	public void test014() throws Exception {
		String script = "switch string {" + "set {" + "	pid" + "} "
				+ "default {puts boo}}";
		typedCheck(script, 0, 2);
	}

	private void typedCheck(final String source, int errs, int code)
			throws Exception {
		System.out.println("=============================================");
		processor = DefinitionManager.getInstance().createProcessor();
		TclParser parser = TestUtils.createParser("8.4");
		TclErrorCollector errors = new TclErrorCollector();
		List<TclCommand> module = parser.parse(source, errors, processor);
		TestCase.assertEquals(1, module.size());
		final int[] scripts = { 0 };
		TclParserUtils.traverse(module, new TclVisitor() {
			@Override
			public boolean visit(Script script) {
				scripts[0]++;
				TestUtils.outCode(source, script.getStart(), script.getEnd());
				return true;
			}
		});
		TestCase.assertEquals(code, scripts[0]);

		/*
		 * int scripts0 = 0; for (int i = 0; i < args.size(); i++) { if
		 * (args.get(i) instanceof Script) { scripts0++;
		 * TestUtils.outCode(source, args.get(i).getStart(),
		 * args.get(i).getEnd()); } if (args.get(i) instanceof
		 * TclArgumentListImpl) { EList<TclArgument> innerArgs =
		 * ((TclArgumentList)args.get(i)).getArguments(); for (int k = 0; k <
		 * innerArgs.size(); k++) { if (innerArgs.get(k) instanceof Script) {
		 * scripts0++; TestUtils.outCode(source, innerArgs.get(i).getStart(),
		 * innerArgs.get(i).getEnd()); } } }
		 * 
		 * } TestCase.assertEquals(code, scripts0);
		 */

		if (errors.getCount() > 0) {
			TestUtils.outErrors(source, errors);
		}
		TestCase.assertEquals(errs, errors.getCount());
	}
}
