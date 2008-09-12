package org.eclipse.dltk.tcl.internal.core.packages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.dltk.ast.declarations.ModuleDeclaration;
import org.eclipse.dltk.compiler.problem.DefaultProblem;
import org.eclipse.dltk.compiler.problem.IProblemReporter;
import org.eclipse.dltk.compiler.problem.ProblemSeverities;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IBuildpathEntry;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.builder.IScriptBuilder.DependencyResponse;
import org.eclipse.dltk.core.environment.EnvironmentPathUtils;
import org.eclipse.dltk.launching.IInterpreterInstall;
import org.eclipse.dltk.launching.ScriptRuntime;
import org.eclipse.dltk.tcl.core.TclPlugin;
import org.eclipse.dltk.tcl.core.TclProblems;
import org.eclipse.dltk.tcl.core.TclParseUtil.CodeModel;
import org.eclipse.dltk.tcl.core.ast.TclPackageDeclaration;
import org.eclipse.dltk.validators.core.IBuildParticipant;
import org.eclipse.dltk.validators.core.IBuildParticipantExtension;
import org.eclipse.dltk.validators.core.IBuildParticipantExtension2;
import org.eclipse.osgi.util.NLS;

public class TclCheckBuilder implements IBuildParticipant,
		IBuildParticipantExtension, IBuildParticipantExtension2 {

	private final IScriptProject project;
	private final IInterpreterInstall install;

	private final PackagesManager manager = PackagesManager.getInstance();
	private final TclBuildPathPackageCollector packageCollector = new TclBuildPathPackageCollector();
	private final Map codeModels = new HashMap();
	private final Map resourceToModuleInfos = new HashMap();
	private final Set knownPackageNames;
	private final Set buildpath;

	private static class ModuleInfo {
		final List requireDirectives;
		final IProblemReporter reporter;

		public ModuleInfo(IProblemReporter reporter, List requireDirectives) {
			this.reporter = reporter;
			this.requireDirectives = requireDirectives;
		}

	}

	public TclCheckBuilder(IScriptProject project) throws CoreException {
		this.project = project;
		install = ScriptRuntime.getInterpreterInstall(project);
		if (install != null) {
			knownPackageNames = manager.getPackageNames(install);
			buildpath = getBuildpath(project);
		} else {
			knownPackageNames = new HashSet();
			buildpath = new HashSet();
		}
	}

	private int buildType;

	public void beginBuild(int buildType) {
		if (install == null) {
			return;
		}
		this.buildType = buildType;
		if (buildType != FULL_BUILD) {
			packageCollector.getPackagesProvided().addAll(
					manager.getInternalPackageNames(install, project));
		}
		// This method will populate all required paths.
		manager.getPathsForPackages(install, packageCollector
				.getPackagesRequired());
		manager.getPathsForPackagesWithDeps(install, packageCollector
				.getPackagesRequired());
	}

	public void build(ISourceModule module, ModuleDeclaration ast,
			IProblemReporter reporter) throws CoreException {
		if (install == null) {
			return;
		}
		packageCollector.getRequireDirectives().clear();
		packageCollector.process(ast);
		if (!packageCollector.getRequireDirectives().isEmpty()) {
			resourceToModuleInfos.put(module, new ModuleInfo(reporter,
					new ArrayList(packageCollector.getRequireDirectives())));
		}
	}

	public void endBuild() {
		if (install == null) {
			return;
		}
		// TODO re-process files with our errors
		if (buildType != STRUCTURE_BUILD) {
			manager.setInternalPackageNames(install, project, packageCollector
					.getPackagesProvided());
		}
		for (Iterator i = resourceToModuleInfos.entrySet().iterator(); i
				.hasNext();) {
			final Map.Entry entry = (Map.Entry) i.next();
			final ISourceModule module = (ISourceModule) entry.getKey();
			final ModuleInfo info = (ModuleInfo) entry.getValue();

			final CodeModel model = getCodeModel(module);
			if (model == null) {
				continue;
			}

			for (Iterator j = info.requireDirectives.iterator(); j.hasNext();) {
				TclPackageDeclaration pkg = (TclPackageDeclaration) j.next();
				if (pkg.getStyle() == TclPackageDeclaration.STYLE_REQUIRE) {
					checkPackage(pkg, info.reporter, model);
				}
			}
		}
	}

	private CodeModel getCodeModel(ISourceModule module) {
		CodeModel model = (CodeModel) codeModels.get(module);
		if (model == null) {
			try {
				model = new CodeModel(module.getSource());
			} catch (ModelException e) {
				if (DLTKCore.DEBUG) {
					e.printStackTrace();
				}
				return null;
			}
		}
		return model;
	}

	private static Set getBuildpath(IScriptProject project) {
		final IBuildpathEntry[] resolvedBuildpath;
		try {
			resolvedBuildpath = project.getResolvedBuildpath(true);
		} catch (ModelException e1) {
			TclPlugin.error(e1);
			return Collections.EMPTY_SET;
		}
		final Set buildpath = new HashSet();
		for (int i = 0; i < resolvedBuildpath.length; i++) {
			final IBuildpathEntry entry = resolvedBuildpath[i];
			if (entry.getEntryKind() == IBuildpathEntry.BPE_LIBRARY
					&& entry.isExternal()) {
				buildpath.add(EnvironmentPathUtils
						.getLocalPath(entry.getPath()));
			}
		}
		return buildpath;
	}

	private static void reportPackageProblem(TclPackageDeclaration pkg,
			IProblemReporter reporter, CodeModel model, String message,
			String pkgName) {
		reporter.reportProblem(new DefaultProblem(message,
				TclProblems.UNKNOWN_REQUIRED_PACKAGE, new String[] { pkgName },
				ProblemSeverities.Error, pkg.sourceStart(), pkg.sourceEnd(),
				model.getLineNumber(pkg.sourceStart(), pkg.sourceEnd())));
	}

	private void checkPackage(TclPackageDeclaration pkg,
			IProblemReporter reporter, CodeModel model) {
		final String packageName = pkg.getName();

		if (packageCollector.getPackagesProvided().contains(packageName)) {
			return;
		}
		if (!isValidPackageName(packageName)) {
			return;
		}

		// Report unknown packages
		if (!knownPackageNames.contains(packageName)) {
			reportPackageProblem(pkg, reporter, model, NLS.bind(
					Messages.TclCheckBuilder_unknownPackage, packageName),
					packageName);
			return;
		}

		// Receive main package and it paths.
		if (checkPackage(packageName)) {
			reportPackageProblem(pkg, reporter, model, NLS.bind(
					Messages.TclCheckBuilder_unresolvedDependencies,
					packageName), packageName);
			return;
		}

		final Set dependencies = manager.getDependencies(packageName, install)
				.keySet();
		for (Iterator i = dependencies.iterator(); i.hasNext();) {
			String pkgName = (String) i.next();
			if (checkPackage(pkgName)) {
				reportPackageProblem(pkg, reporter, model, NLS.bind(
						Messages.TclCheckBuilder_unresolvedDependencies,
						packageName), packageName);
				return;
			}
		}
	}

	static boolean isValidPackageName(String packageName) {
		return packageName != null && packageName.length() != 0
				&& packageName.indexOf('$') == -1
				&& packageName.indexOf('[') == -1
				&& packageName.indexOf(']') == -1;
	}

	/**
	 * returns <code>true</code> on error
	 * 
	 * @param packageName
	 * @return
	 */
	private boolean checkPackage(String packageName) {
		if (packageCollector.getPackagesProvided().contains(packageName)) {
			return false;
		}
		return isOnBuildpath(buildpath, manager.getPathsForPackage(install,
				packageName));
	}

	private static boolean isOnBuildpath(Set buildpath, IPath path) {
		if (!buildpath.contains(path)) {
			for (Iterator i = buildpath.iterator(); i.hasNext();) {
				IPath pp = (IPath) i.next();
				if (pp.isPrefixOf(path)) {
					return true;
				}
			}
			return false;
		}
		return true;
	}

	private static boolean isOnBuildpath(Set buildpath, IPath[] paths) {
		if (paths != null) {
			for (int i = 0; i < paths.length; i++) {
				final IPath path = paths[i];
				if (!isOnBuildpath(buildpath, path)) {
					return true;
				}
			}
		}
		return false;
	}

	public DependencyResponse getDependencies(int buildType, Set localElements,
			Set externalElements, Set oldExternalFolders, Set externalFolders) {
		if (!oldExternalFolders.equals(externalFolders)) {
			return DependencyResponse.FULL_BUILD;
		} else {
			return null;
		}
	}

}
