package org.eclipse.dltk.itcl.internal.core.parser.processors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.Modifiers;
import org.eclipse.dltk.ast.declarations.TypeDeclaration;
import org.eclipse.dltk.ast.expressions.Expression;
import org.eclipse.dltk.ast.references.SimpleReference;
import org.eclipse.dltk.compiler.problem.ProblemSeverities;
import org.eclipse.dltk.itcl.internal.core.IIncrTclModifiers;
import org.eclipse.dltk.itcl.internal.core.classes.IncrTclClassesManager;
import org.eclipse.dltk.itcl.internal.core.parser.ast.IncrTclFieldDeclaration;
import org.eclipse.dltk.itcl.internal.core.parser.ast.IncrTclMethodDeclaration;
import org.eclipse.dltk.tcl.ast.TclStatement;
import org.eclipse.dltk.tcl.ast.expressions.TclBlockExpression;
import org.eclipse.dltk.tcl.core.AbstractTclCommandProcessor;
import org.eclipse.dltk.tcl.core.ITclParser;
import org.eclipse.dltk.tcl.core.TclParseUtil;
import org.eclipse.dltk.tcl.core.ast.ExtendedTclMethodDeclaration;

public class IncrTclClassCommandProcessor extends AbstractTclCommandProcessor {
	public ASTNode process(TclStatement statement, ITclParser parser,
			ASTNode parent) {
		if (statement == null
				|| (statement != null && statement.getCount() == 0)) {
			return null;
		}
		if (statement.getCount() != 3) {
			// error not correct arguments
			return null;
		}
		Expression classNameExpr = statement.getAt(1);
		Expression blockExpr = statement.getAt(2);
		if (classNameExpr instanceof SimpleReference
				&& blockExpr instanceof TclBlockExpression) {
			TclBlockExpression block = (TclBlockExpression) blockExpr;
			TypeDeclaration type = new TypeDeclaration(
					((SimpleReference) classNameExpr).getName(), classNameExpr
							.sourceStart(), classNameExpr.sourceEnd(),
					statement.sourceStart(), statement.sourceEnd());
			type.setModifiers(IIncrTclModifiers.AccIncrTcl);
			this.addToParent(parent, type);
			// Report type to list of types.
			IncrTclClassesManager.getDefault().add(type.getName());

			List commands = block.parseBlockSimple(false);
			for (int i = 0; i < commands.size(); i++) {
				ASTNode nde = (ASTNode) commands.get(i);
				if (nde instanceof TclStatement) {
					TclStatement st = (TclStatement) nde;
					Expression commandName = st.getAt(0);
					if (commandName instanceof SimpleReference) {
						String commandNameStr = ((SimpleReference) commandName)
								.getName();
						if ("inherit".equals(commandNameStr)) {
							handleInherit(st, type, parser);
						} else if ("public".equals(commandNameStr)) {
							handleWithModifierSub(st, type,
									Modifiers.AccPublic, parser);
						} else if ("protected".equals(commandNameStr)) {
							handleWithModifierSub(st, type,
									Modifiers.AccProtected, parser);
						} else if ("private".equals(commandNameStr)) {
							handleWithModifierSub(st, type,
									Modifiers.AccPrivate, parser);
						} else {
							handleWithModifier(commandNameStr, st, type,
									Modifiers.AccPrivate, parser);
						}
					}
				}
			}
			return type;
		}
		return null;
	}

	private void handleDestructor(TclStatement statement, TypeDeclaration type,
			ITclParser parser) {
		if (statement.getCount() != 2) {
			this.report(parser, "Wrong number of arguments", statement
					.sourceStart(), statement.sourceEnd(),
					ProblemSeverities.Error);
			addToParent(type, statement);
			return;
		}
		Expression procCode = statement.getAt(1);

		ExtendedTclMethodDeclaration method = new ExtendedTclMethodDeclaration(
				statement.sourceStart(), statement.sourceEnd());
		method.setName("destructor");
		Expression o = statement.getAt(0);
		method.setNameStart(o.sourceStart());
		method.setNameEnd(o.sourceEnd());
		method.setModifier(IIncrTclModifiers.AccIncrTcl
				| IIncrTclModifiers.AccDestructor);
		method.setDeclaringType(type);
		IncrTclUtils.parseBlockAdd(parser, procCode, method);
		type.getMethodList().add(method);
		this.addToParent(type, method);
	}

	private void handleWithModifierSub(TclStatement statement,
			TypeDeclaration type, int modifier, ITclParser parser) {
		List expressions = statement.getExpressions();
		List newExpressions = new ArrayList();
		newExpressions.addAll(expressions);
		if (newExpressions.size() > 0) {
			newExpressions.remove(0);
		}
		TclStatement sub = new TclStatement(newExpressions);
		Expression commandName = sub.getAt(0);
		if (commandName instanceof SimpleReference) {
			handleWithModifier(((SimpleReference) commandName).getName(), sub,
					type, modifier, parser);
		}
	}

	private void handleConstructor(TclStatement statement,
			TypeDeclaration type, ITclParser parser) {
		if (statement.getCount() < 3) {
			this.report(parser, "Wrong number of arguments", statement
					.sourceStart(), statement.sourceEnd(),
					ProblemSeverities.Error);
			addToParent(type, statement);
			return;
		}
		Expression procArguments = statement.getAt(1);
		Expression procCode = null;
		// Expression procInit = null;
		if (statement.getCount() == 3) {
			procCode = statement.getAt(2);
		} else if (statement.getCount() == 4) {
			procCode = statement.getAt(3);
			// procInit = statement.getAt(2);
		}

		List arguments = IncrTclUtils.extractMethodArguments(procArguments);

		ExtendedTclMethodDeclaration method = new ExtendedTclMethodDeclaration(
				statement.sourceStart(), statement.sourceEnd());
		method.setDeclaringType(type);
		method.setName("constructor");
		Expression o = statement.getAt(0);
		method.setNameStart(o.sourceStart());
		method.setNameEnd(o.sourceEnd());
		method.acceptArguments(arguments);
		method.setModifier(IIncrTclModifiers.AccIncrTcl
				| IIncrTclModifiers.AccConstructor);
		IncrTclUtils.parseBlockAdd(parser, procCode, method);
		type.getMethodList().add(method);
		this.addToParent(type, method);
	}

	private void handleWithModifier(String commandNameStr,
			TclStatement statement, TypeDeclaration type, int modifier,
			ITclParser parser) {

		if ("method".equals(commandNameStr)) {
			handleMethod(statement, type, modifier, parser);
		} else if ("proc".equals(commandNameStr)) {
			handleMethod(statement, type, modifier
					| IIncrTclModifiers.AccIncrTclProc, parser);
		} else if ("variable".equals(commandNameStr)) {
			handleVariable(statement, type, modifier, parser);
		} else if ("common".equals(commandNameStr)) {
			handleCommon(statement, type, modifier, parser);
		} else if ("set".equals(commandNameStr)) {
			handleSet(statement, type, modifier, parser);
		} else if ("array".equals(commandNameStr)) {
			handleArray(statement, type, modifier);
		} else if ("constructor".equals(commandNameStr)) {
			handleConstructor(statement, type, parser);
		} else if ("destructor".equals(commandNameStr)) {
			handleDestructor(statement, type, parser);
		}
	}

	private void handleSet(TclStatement statement, TypeDeclaration type,
			int modifier, ITclParser parser) {
		processVariable(statement, type, modifier, parser);
	}

	private void processVariable(TclStatement statement, TypeDeclaration type,
			int modifier, ITclParser parser) {
		if (statement.getCount() < 2) {
			this.report(parser,
					"Syntax error: at least one argument expected.", statement,
					ProblemSeverities.Error);
			return;
		}
		Expression variableName = statement.getAt(1);
		Expression variableValue = null;
		if (statement.getCount() == 3)
			variableValue = statement.getAt(2);
		if (variableName == null) {
			throw new RuntimeException("empty variable name");
		}
		SimpleReference variable = TclParseUtil.makeVariable(variableName);
		if (variable != null) {
			IncrTclFieldDeclaration var = new IncrTclFieldDeclaration(variable
					.getName(), variable.sourceStart(), variable.sourceEnd(),
					statement.sourceStart(), statement.sourceEnd());
			var.setModifier(IIncrTclModifiers.AccIncrTcl | modifier);
			var.setDeclaringType(type);
			this.addToParent(type, var);
			type.getFieldList().add(var);
		}
	}

	private void handleArray(TclStatement statement, TypeDeclaration type,
			int modifier) {
		// processVariable(statement, type, modifier, parser);
	}

	private void handleCommon(TclStatement statement, TypeDeclaration type,
			int modifier, ITclParser parser) {
		if (statement.getCount() < 2) {
			this.report(parser, "Syntax error: one argument expected.",
					statement, ProblemSeverities.Error);
			return;
		}
		Expression variableName = statement.getAt(1);
		if (variableName == null) {
			throw new RuntimeException("empty variable name");
		}
		SimpleReference variable = TclParseUtil.makeVariable(variableName);
		if (variable != null) {
			IncrTclFieldDeclaration var = new IncrTclFieldDeclaration(variable
					.getName(), variable.sourceStart(), variable.sourceEnd(),
					statement.sourceStart(), statement.sourceEnd());
			var.setModifier(IIncrTclModifiers.AccIncrTcl | modifier);
			var.setDeclaringType(type);
			this.addToParent(type, var);
		}
	}

	private void handleVariable(TclStatement statement, TypeDeclaration type,
			int modifier, ITclParser parser) {
		processVariable(statement, type, modifier, parser);
	}

	private void handleMethod(TclStatement statement, TypeDeclaration type,
			int modifier, ITclParser parser) {
		if (statement.getCount() < 2) {
			this.report(parser, "Wrong number of arguments", statement
					.sourceStart(), statement.sourceEnd(),
					ProblemSeverities.Error);
			addToParent(type, statement);
			return;
		}
		Expression procName = statement.getAt(1);

		String sName = IncrTclUtils.extractMethodName(procName);
		if (sName == null || sName.length() == 0) {
			this.report(parser, "Wrong number of arguments", statement
					.sourceStart(), statement.sourceEnd(),
					ProblemSeverities.Error);
			addToParent(type, statement);
			return;
		}
		Expression procArguments = null;// statement.getAt(2);
		Expression procCode = null;// statement.getAt(3);
		if (statement.getCount() >= 3) {
			procArguments = statement.getAt(2);
		}
		if (statement.getCount() == 4) {
			procCode = statement.getAt(3);
		}

		List arguments = IncrTclUtils.extractMethodArguments(procArguments);

		IncrTclMethodDeclaration method = new IncrTclMethodDeclaration(
				statement.sourceStart(), statement.sourceEnd());
		method.setName(sName);
		method.setNameStart(procName.sourceStart());
		method.setNameEnd(procName.sourceEnd());
		method.acceptArguments(arguments);
		method.setModifier(IIncrTclModifiers.AccIncrTcl | modifier);
		method.setDeclaringType(type);
		if ((modifier & IIncrTclModifiers.AccIncrTclProc) != 0) {
			method.setKind(ExtendedTclMethodDeclaration.KIND_PROC);
		} else {
			method.setKind(ExtendedTclMethodDeclaration.KIND_INSTPROC);
		}
		IncrTclUtils.parseBlockAdd(parser, procCode, method);
		type.getMethodList().add(method);

		this.addToParent(type, method);
	}

	private void handleInherit(TclStatement statement, TypeDeclaration type,
			ITclParser parser) {
		for (int i = 1; i < statement.getCount(); i++) {
			Expression expr = statement.getAt(i);
			if (expr instanceof SimpleReference) {
				type.addSuperClass(expr);
			}
		}
	}
}
