package org.eclipse.dltk.tcl.internal.parser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.Modifiers;
import org.eclipse.dltk.ast.declarations.Argument;
import org.eclipse.dltk.ast.declarations.Declaration;
import org.eclipse.dltk.ast.declarations.FieldDeclaration;
import org.eclipse.dltk.ast.declarations.MethodDeclaration;
import org.eclipse.dltk.ast.declarations.ModuleDeclaration;
import org.eclipse.dltk.ast.declarations.TypeDeclaration;
import org.eclipse.dltk.ast.expressions.Expression;
import org.eclipse.dltk.ast.expressions.StringLiteral;
import org.eclipse.dltk.ast.references.Reference;
import org.eclipse.dltk.ast.references.SimpleReference;
import org.eclipse.dltk.ast.statements.Statement;
import org.eclipse.dltk.compiler.IElementRequestor;
import org.eclipse.dltk.compiler.ISourceElementRequestor;
import org.eclipse.dltk.compiler.SourceElementRequestVisitor;
import org.eclipse.dltk.compiler.IElementRequestor.ImportInfo;
import org.eclipse.dltk.compiler.problem.IProblemReporter;
import org.eclipse.dltk.compiler.util.Util;
import org.eclipse.dltk.tcl.ast.TclConstants;
import org.eclipse.dltk.tcl.ast.TclStatement;
import org.eclipse.dltk.tcl.ast.expressions.TclBlockExpression;
import org.eclipse.dltk.tcl.ast.expressions.TclExecuteExpression;
import org.eclipse.dltk.tcl.core.TclKeywordsManager;
import org.eclipse.dltk.tcl.core.TclParseUtil;
import org.eclipse.dltk.tcl.core.ast.TclAdvancedExecuteExpression;
import org.eclipse.dltk.tcl.core.ast.TclPackageDeclaration;
import org.eclipse.dltk.tcl.core.extensions.ISourceElementRequestVisitorExtension;
import org.eclipse.dltk.tcl.internal.core.TclExtensionManager;

public class TclSourceElementRequestVisitor extends SourceElementRequestVisitor {

	protected Stack<ExitFromType> exitStack = new Stack<ExitFromType>();
	protected IProblemReporter fReporter;

	protected ISourceElementRequestVisitorExtension[] extensions = TclExtensionManager
			.getDefault().getSourceElementRequestoVisitorExtensions();

	public TclSourceElementRequestVisitor(ISourceElementRequestor requestor,
			IProblemReporter reporter) {
		super(requestor);
		this.fReporter = reporter;
	}

	protected String removeLastSegment(String s, String delimeter) {
		if (s.indexOf("::") == -1) {
			return Util.EMPTY_STRING;
		}
		int pos = s.length() - 1;
		while (s.charAt(pos) != ':') {
			pos--;
		}
		if (pos > 1) {
			return s.substring(0, pos - 1);
		} else {
			return "::";
		}
	}

	public static class ExitFromType {
		private int level;
		private int end;
		private boolean exitFromModule;
		private String namespace;
		public boolean created = false;

		/**
		 * @since 2.0
		 */
		public ExitFromType(int level, int declEnd, boolean mod, String pop) {
			this(level, declEnd, mod, pop, false);
		}

		/**
		 * @since 2.0
		 */
		public ExitFromType(int level, int declEnd, boolean mod, String pop,
				boolean created) {
			this.level = level;
			this.end = declEnd;
			this.exitFromModule = mod;
			this.namespace = pop;
			this.created = created;
		}

		/**
		 * @since 2.0
		 */
		public void go(IElementRequestor requestor) {
			for (int i = 0; i < this.level; i++) {
				requestor.exitType(this.end);
			}
			if (this.exitFromModule) {
				requestor.exitModuleRoot();
			}
		}
	}

	protected String getEnclosingNamespace() {
		for (int head = exitStack.size() - 1;; --head) {
			final ExitFromType exit = exitStack.get(head);
			if (exit.namespace != null) {
				return exit.namespace;
			}
		}
	}

	/**
	 * Enters into required type (if type doesn't exists, creates it). If name
	 * is fully-qualified (starting with a "::") then it is always resolved
	 * globally. Else search are done first in current namespace, than in
	 * global. Flags <code>onlyCurrent</code> allows to search
	 * <em>not qualified</em> names only in current namespace. If type doesn't
	 * exists, it will be created. If name is qualified, it will be created
	 * globally, else in current namespace.s
	 * 
	 * @param decl
	 *            expression containing typedeclaration correct source ranges
	 *            setup
	 * @param name
	 *            name containing a type
	 * @param onlyCurrent
	 * @return ExitFromType object, that should be called to exit
	 */
	public ExitFromType resolveType(Declaration decl, String name,
			boolean onlyCurrent) {
		String type = removeLastSegment(name, "::");
		while (type.length() > 2 && type.endsWith("::")) {
			type = type.substring(0, type.length() - 2);
		}

		if (type.length() == 0) {
			return new ExitFromType(0, 0, false, null);
		}

		if (type.equals("::")) {
			this.fRequestor.enterModuleRoot();
			return new ExitFromType(0, decl.sourceEnd(), true, "::");
		}

		boolean fqn = type.startsWith("::");

		String fullyQualified = type;
		if (!fqn) { // make name fully-qualified
			String e = this.getEnclosingNamespace();
			if (e == null) {
				throw new AssertionError("there are no enclosing namespace!");
			}
			if (!e.endsWith("::")) {
				e += "::";
			}
			fullyQualified = e + type;
		}

		// first, try existent
		if (((ISourceElementRequestor) this.fRequestor).enterTypeAppend(type,
				"::")) {
			return new ExitFromType(1/* split.length */, decl.sourceEnd(),
					false, fullyQualified);
		}

		// create it
		// Lets add warning in any case.
		int needEnterLeave = 0;
		String[] split = null;
		String e = this.getEnclosingNamespace();
		if (e == null) {
			throw new AssertionError("there are no enclosing namespace!");
		}
		boolean entered = false;
		boolean exitFromModule = false;
		if (e.length() > 0 && !fqn) {
			// We need to report warning here.
			entered = ((ISourceElementRequestor) this.fRequestor)
					.enterTypeAppend(e, "::");
		}
		if (fqn || !entered) {
			split = TclParseUtil.tclSplit(fullyQualified.substring(2));
			this.fRequestor.enterModuleRoot();
			exitFromModule = true;
		} else {
			if (!entered) {
				throw new AssertionError("can't enter to enclosing namespace!");
			}
			needEnterLeave++;
			split = TclParseUtil.tclSplit(type);
		}

		for (int i = 0; i < split.length; ++i) {
			if (split[i].length() > 0) {
				needEnterLeave++;
				if (!((ISourceElementRequestor) this.fRequestor)
						.enterTypeAppend(split[i], "::")) {
					ISourceElementRequestor.TypeInfo ti = new ISourceElementRequestor.TypeInfo();
					if (decl instanceof TypeDeclaration) {
						ti.modifiers = getModifiers(decl);
					} else {
						ti.modifiers = Modifiers.AccNameSpace;
					}

					ti.name = split[i];
					ti.nameSourceStart = decl.getNameStart();
					ti.nameSourceEnd = decl.getNameEnd() - 1;
					ti.declarationStart = decl.sourceStart();
					if (decl instanceof TypeDeclaration) {
						ti.superclasses = processSuperClasses((TypeDeclaration) decl);
					}
					this.fRequestor.enterType(ti);
				}
			}
		}
		return new ExitFromType(needEnterLeave, decl.sourceEnd(),
				exitFromModule, fullyQualified, true);
	}

	public boolean visit(TypeDeclaration s) throws Exception {
		this.fNodes.push(s);

		ISourceElementRequestor.TypeInfo info = new ISourceElementRequestor.TypeInfo();
		info.modifiers = this.getModifiers(s);

		String fullName = s.getName();

		String[] split = TclParseUtil.tclSplit(fullName);
		if (split.length != 0) {
			info.name = split[split.length - 1];
		} else {
			info.name = "";
		}

		info.nameSourceStart = s.getNameStart();
		info.nameSourceEnd = s.getNameEnd();
		info.declarationStart = s.sourceStart();
		info.superclasses = this.processSuperClasses(s);
		info.modifiers = this.getModifiers(s);

		ExitFromType exit = this.resolveType(s, fullName + "::dummy", true);

		this.exitStack.push(exit);
		this.fInClass = true;

		return true;
	}

	protected int getModifiers(Declaration s) {
		int flags = 0;

		flags = s.getModifiers();
		for (int i = 0; i < this.extensions.length; i++) {
			flags |= this.extensions[i].getModifiers(s);
		}

		return flags;
	}

	private boolean isConstructor(MethodDeclaration s) {
		for (int i = 0; i < this.extensions.length; i++) {
			if (this.extensions[i].isConstructor(s)) {
				return true;
			}
		}
		return false;
	}

	public boolean endvisit(TypeDeclaration typeDeclaration) throws Exception {
		ExitFromType exit = this.exitStack.pop();
		exit.go(fRequestor);
		this.fInClass = false;
		this.onEndVisitClass(typeDeclaration);
		this.fNodes.pop();
		return true;
	}

	private static Map<String, Boolean> kwMap = new HashMap<String, Boolean>();
	static {
		String[] kw = TclKeywordsManager.getKeywords();
		for (int q = 0; q < kw.length; ++q) {
			kwMap.put(kw[q], Boolean.TRUE);
		}
	}

	public boolean visit(Statement statement) throws Exception {
		this.fNodes.push(statement);

		if (statement instanceof TclPackageDeclaration) {
			this.processPackage(statement);
			this.fNodes.pop();
			return false;
		} else if (statement instanceof TclStatement) {
			processReferences((TclStatement) statement);
			// it can contain nested scripts/substitutions
			return true;
		} else if (statement instanceof FieldDeclaration) {
			this.processField(statement);
			return false;
		}

		for (int i = 0; i < extensions.length; i++) {
			if (extensions[i].visit(statement, this)) {
				return true;
			}
		}
		return true;
	}

	private void processReferences(TclStatement statement) {
		Expression commandId = statement.getAt(0);
		if (commandId != null && commandId instanceof SimpleReference) {
			String name = ((SimpleReference) commandId).getName();
			if (name.startsWith("::")) {
				name = name.substring(2);
			}
			if ("source".equals(name)) {
				if (statement.getCount() > 1) {
					Expression sourceFile = statement.getAt(1);
					if (sourceFile instanceof Reference) {
						ImportInfo importInfo = new ImportInfo();
						importInfo.sourceStart = statement.sourceStart();
						importInfo.sourceEnd = statement.sourceEnd();
						importInfo.containerName = org.eclipse.dltk.tcl.core.TclConstants.SOURCE_CONTAINER;
						importInfo.name = ((Reference) sourceFile)
								.getStringRepresentation();
						this.fRequestor.acceptImport(importInfo);
					}
				}
			}
			if (!kwMap.containsKey(name)) {
				int argCount = statement.getCount() - 1;
				if (name.length() > 0) {
					if (name.charAt(0) != '$') {
						this.fRequestor.acceptMethodReference(name, argCount,
								commandId.sourceStart(), commandId.sourceEnd());
					}
				}
			}
		}
		for (int j = 1; j < statement.getCount(); ++j) {
			Expression st = statement.getAt(j);
			if (st instanceof TclExecuteExpression) {
				TclExecuteExpression expr = (TclExecuteExpression) st;
				List exprs = expr.parseExpression();
				for (int i = 0; i < exprs.size(); ++i) {
					if (exprs.get(i) instanceof TclStatement) {
						this.processReferences((TclStatement) exprs.get(i));
					} else if (exprs.get(i) instanceof TclPackageDeclaration) {
						processPackage((Statement) exprs.get(i));
					}
				}
			} else if (st instanceof TclAdvancedExecuteExpression) {
				TclAdvancedExecuteExpression expr = (TclAdvancedExecuteExpression) st;
				List exprs = expr.getStatements();
				for (int i = 0; i < exprs.size(); ++i) {
					if (exprs.get(i) instanceof TclStatement) {
						this.processReferences((TclStatement) exprs.get(i));
					} else if (exprs.get(i) instanceof TclPackageDeclaration) {
						processPackage((Statement) exprs.get(i));
					}
				}
			} else if (st instanceof StringLiteral) {
				int pos = 0;
				StringLiteral literal = (StringLiteral) st;
				String value = literal.getValue();
				pos = value.indexOf("$");
				while (pos != -1) {
					SimpleReference ref = OldTclParserUtils
							.findVariableFromString(literal, pos);
					if (ref != null) {
						this.fRequestor.acceptFieldReference(ref.getName()
								.substring(1), ref.sourceStart());
						pos = pos + ref.getName().length();
					}
					pos = value.indexOf("$", pos + 1);
				}
			} else if (st instanceof SimpleReference) {
				SimpleReference ref = (SimpleReference) st;
				String name = ref.getName();
				if (name.startsWith("$")) { // This is variable usage.
					this.fRequestor.acceptFieldReference(ref.getName()
							.substring(1), ref.sourceStart());
				}
			}
		}
	}

	private void processPackage(Statement statement) {
		TclPackageDeclaration pack = (TclPackageDeclaration) statement;
		ASTNode version = pack.getVersion();
		if (pack.getStyle() == TclPackageDeclaration.STYLE_PROVIDE) {
			if (version != null && version instanceof SimpleReference) {
				this.fRequestor.acceptPackage(pack.getNameStart(), pack
						.getNameEnd() - 1, pack.getName() + " ("
						+ ((SimpleReference) version).getName() + ")");
			} else {
				this.fRequestor.acceptPackage(pack.getNameStart(), pack
						.getNameEnd() - 1, pack.getName());
			}
		} else if (pack.getStyle() == TclPackageDeclaration.STYLE_IFNEEDED) {
			if (version != null && version instanceof SimpleReference) {
				this.fRequestor.acceptPackage(pack.getNameStart(), pack
						.getNameEnd() - 1, pack.getName() + " ("
						+ ((SimpleReference) version).getName() + ")*");
			} else {
				this.fRequestor.acceptPackage(pack.getNameStart(), pack
						.getNameEnd() - 1, pack.getName() + "*");
			}
		} else if (pack.getStyle() == TclPackageDeclaration.STYLE_REQUIRE) {
			ImportInfo importInfo = new ImportInfo();
			importInfo.sourceStart = pack.getNameStart();
			importInfo.sourceEnd = pack.getNameEnd() - 1;
			importInfo.containerName = org.eclipse.dltk.tcl.core.TclConstants.REQUIRE_CONTAINER;
			importInfo.name = pack.getName();
			importInfo.version = version instanceof SimpleReference ? ((SimpleReference) version)
					.getName()
					: null;
			this.fRequestor.acceptImport(importInfo);
		}
	}

	protected boolean processField(Statement statement) {
		FieldDeclaration decl = (FieldDeclaration) statement;
		ISourceElementRequestor.FieldInfo fi = new ISourceElementRequestor.FieldInfo();
		fi.nameSourceStart = decl.getNameStart();
		fi.nameSourceEnd = decl.getNameEnd() - 1;
		fi.declarationStart = decl.sourceStart();
		fi.modifiers = this.getModifiers(decl);

		boolean needExit = false;

		String arrayName = null;
		String arrayIndex = null;
		String name = decl.getName();
		if (TclParseUtil.isArrayVariable(name)) {
			arrayName = TclParseUtil.extractArrayName(name);
			arrayIndex = TclParseUtil.extractArrayIndex(name);
		}
		if (arrayName != null) {
			name = arrayName;
		}
		fi.name = name;
		String fullName = TclParseUtil.escapeName(name);
		ExitFromType exit = null;// this.resolveType(decl, fullName, false);

		for (int i = 0; i < extensions.length; i++) {
			if ((exit = extensions[i].processField(decl, this)) != null) {
				continue;
			}
		}
		if (exit == null) {
			exit = this.resolveType(decl, fullName, false);
		}

		needExit = ((ISourceElementRequestor) this.fRequestor)
				.enterFieldCheckDuplicates(fi);

		int end = decl.sourceEnd();
		if (needExit) {
			if (arrayName != null) {
				ISourceElementRequestor.FieldInfo fiIndex = new ISourceElementRequestor.FieldInfo();
				fiIndex.name = arrayName + "(" + arrayIndex + ")";
				fiIndex.nameSourceStart = decl.getNameStart();
				fiIndex.nameSourceEnd = decl.getNameEnd() - 1;
				fiIndex.declarationStart = decl.sourceStart();
				fiIndex.modifiers = TclConstants.TCL_FIELD_TYPE_INDEX
						| this.getModifiers(decl);
				if (((ISourceElementRequestor) this.fRequestor)
						.enterFieldCheckDuplicates(fiIndex)) {
					this.fRequestor.exitField(end);
				}
			}
			this.fRequestor.exitField(end);
		}
		exit.go(fRequestor);
		return false;
	}

	public boolean visit(ModuleDeclaration declaration) throws Exception {
		this.exitStack.push(new ExitFromType(0, 0, false, "::"));
		return super.visit(declaration);
	}

	protected ExitFromType getExitExtended(MethodDeclaration method) {
		for (int i = 0; i < extensions.length; i++) {
			if (extensions[i].extendedExitRequired(method, this)) {
				return extensions[i].getExitExtended(method, this);
			}
		}
		return null;
	}

	protected boolean extendedExitRequired(MethodDeclaration method) {
		for (int i = 0; i < extensions.length; i++) {
			if (extensions[i].extendedExitRequired(method, this)) {
				return true;
			}
		}
		return false;
	}

	public boolean visit(MethodDeclaration method) throws Exception {
		this.fNodes.push(method);
		this.fInMethod = true;
		this.fCurrentMethod = method;
		for (int i = 0; i < extensions.length; i++) {
			if (extensions[i].skipMethod(method, this)) {
				return true;
			}
		}

		String[] parameter = null;
		String[] parameterInitializers = null;
		List arguments = method.getArguments();
		if (arguments != null) {
			parameter = new String[arguments.size()];
			parameterInitializers = new String[arguments.size()];
			for (int a = 0; a < arguments.size(); a++) {
				Object node = arguments.get(a);
				parameterInitializers[a] = null;
				if (node instanceof Argument) {
					Argument ref = (Argument) node;
					parameter[a] = ref.getName();
					Statement e = (Statement) ref.getInitialization();
					if (e != null) {
						if (e instanceof SimpleReference) {
							parameterInitializers[a] = ((SimpleReference) e)
									.getName();
						} else if (e instanceof TclBlockExpression) {
							String name = ((TclBlockExpression) e).getBlock();
							parameterInitializers[a] = name;
						} else if (e instanceof StringLiteral) {
							String name = ((StringLiteral) e).getValue();
							parameterInitializers[a] = name;
						} else if (e instanceof TclExecuteExpression) {
							String name = ((TclExecuteExpression) e)
									.getExpression();
							parameterInitializers[a] = name;
						}
					}
					// if( parameterInitializers[a] == null ) {
					// parameterInitializers[a] = "";
					// }
				} else if (node instanceof String) {
					parameter[a] = (String) node;
				}
			}
		}
		ISourceElementRequestor.MethodInfo mi = new ISourceElementRequestor.MethodInfo();
		String sName = method.getName();
		sName = TclParseUtil.escapeName(sName);
		String fullName = sName;

		if (fullName.indexOf("::") != -1) {
			String[] split = TclParseUtil.tclSplit(fullName);
			sName = split[split.length - 1];
		}

		mi.parameterNames = parameter;
		mi.parameterInitializers = parameterInitializers;
		mi.name = sName;
		mi.modifiers = this.getModifiers(method);
		mi.isConstructor = this.isConstructor(method);
		mi.nameSourceStart = method.getNameStart();
		mi.nameSourceEnd = method.getNameEnd() - 1;
		mi.declarationStart = method.sourceStart();

		ExitFromType exit = null;
		// boolean requireFieldExit = false;
		if (extendedExitRequired(method)) {
			exit = getExitExtended(method);
		} else {
			exit = this.resolveType(method, fullName, false);
		}
		// if (exit.created) {
		// if (this.fReporter != null) {
		// try {
		// this.fReporter.reportProblem(new DefaultProblem("",
		// "Namespace not found.", 0, null,
		// ProblemSeverities.Warning, method.getNameStart(),
		// method.getNameEnd(), -1));
		// } catch (CoreException e1) {
		// if (DLTKCore.DEBUG) {
		// e1.printStackTrace();
		// }
		// }
		// }
		// }
		((ISourceElementRequestor) this.fRequestor).enterMethodRemoveSame(mi);
		this.exitStack.push(exit);
		return true;
	}

	public boolean endvisit(MethodDeclaration method) throws Exception {
		for (int i = 0; i < extensions.length; i++) {
			if (extensions[i].skipMethod(method, this)) {
				this.fNodes.pop();
				return true;
			}
		}
		super.endvisit(method);
		ExitFromType exit = this.exitStack.pop();
		exit.go(fRequestor);
		return true;
	}

	public ISourceElementRequestor getRequestor() {
		return ((ISourceElementRequestor) this.fRequestor);
	}

}
