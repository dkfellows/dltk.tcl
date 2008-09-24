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
package org.eclipse.dltk.tcl.ast.impl;

import java.util.Collection;

import org.eclipse.dltk.tcl.ast.AstPackage;
import org.eclipse.dltk.tcl.ast.TclArgument;
import org.eclipse.dltk.tcl.ast.TclArgumentList;
import org.eclipse.dltk.tcl.definitions.ComplexArgument;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Tcl Argument List</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link org.eclipse.dltk.tcl.ast.impl.TclArgumentListImpl#getArguments <em>Arguments</em>}</li>
 *   <li>{@link org.eclipse.dltk.tcl.ast.impl.TclArgumentListImpl#getDefinitionArgument <em>Definition Argument</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class TclArgumentListImpl extends TclArgumentImpl implements TclArgumentList {
	/**
	 * The cached value of the '{@link #getArguments() <em>Arguments</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getArguments()
	 * @generated
	 * @ordered
	 */
	protected EList<TclArgument> arguments;

	/**
	 * The cached value of the '{@link #getDefinitionArgument() <em>Definition Argument</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDefinitionArgument()
	 * @generated
	 * @ordered
	 */
	protected ComplexArgument definitionArgument;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected TclArgumentListImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return AstPackage.Literals.TCL_ARGUMENT_LIST;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<TclArgument> getArguments() {
		if (arguments == null) {
			arguments = new EObjectResolvingEList<TclArgument>(TclArgument.class, this, AstPackage.TCL_ARGUMENT_LIST__ARGUMENTS);
		}
		return arguments;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ComplexArgument getDefinitionArgument() {
		if (definitionArgument != null && definitionArgument.eIsProxy()) {
			InternalEObject oldDefinitionArgument = (InternalEObject)definitionArgument;
			definitionArgument = (ComplexArgument)eResolveProxy(oldDefinitionArgument);
			if (definitionArgument != oldDefinitionArgument) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE, AstPackage.TCL_ARGUMENT_LIST__DEFINITION_ARGUMENT, oldDefinitionArgument, definitionArgument));
			}
		}
		return definitionArgument;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public ComplexArgument basicGetDefinitionArgument() {
		return definitionArgument;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setDefinitionArgument(ComplexArgument newDefinitionArgument) {
		ComplexArgument oldDefinitionArgument = definitionArgument;
		definitionArgument = newDefinitionArgument;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, AstPackage.TCL_ARGUMENT_LIST__DEFINITION_ARGUMENT, oldDefinitionArgument, definitionArgument));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case AstPackage.TCL_ARGUMENT_LIST__ARGUMENTS:
				return getArguments();
			case AstPackage.TCL_ARGUMENT_LIST__DEFINITION_ARGUMENT:
				if (resolve) return getDefinitionArgument();
				return basicGetDefinitionArgument();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case AstPackage.TCL_ARGUMENT_LIST__ARGUMENTS:
				getArguments().clear();
				getArguments().addAll((Collection<? extends TclArgument>)newValue);
				return;
			case AstPackage.TCL_ARGUMENT_LIST__DEFINITION_ARGUMENT:
				setDefinitionArgument((ComplexArgument)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case AstPackage.TCL_ARGUMENT_LIST__ARGUMENTS:
				getArguments().clear();
				return;
			case AstPackage.TCL_ARGUMENT_LIST__DEFINITION_ARGUMENT:
				setDefinitionArgument((ComplexArgument)null);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case AstPackage.TCL_ARGUMENT_LIST__ARGUMENTS:
				return arguments != null && !arguments.isEmpty();
			case AstPackage.TCL_ARGUMENT_LIST__DEFINITION_ARGUMENT:
				return definitionArgument != null;
		}
		return super.eIsSet(featureID);
	}

} //TclArgumentListImpl