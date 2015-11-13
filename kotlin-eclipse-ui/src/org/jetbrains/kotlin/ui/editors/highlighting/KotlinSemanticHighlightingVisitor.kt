/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui.editors.highlighting

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.core.builder.KotlinPsiManager
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache
import org.eclipse.jface.text.Position
import org.jetbrains.kotlin.psi.KtVisitorVoid
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.eclipse.ui.utils.LineEndUtil
import org.jetbrains.kotlin.psi.KtExpression

public class KotlinSemanticHighlightingVisitor(val editor: KotlinFileEditor) : KtVisitorVoid() {
    private val bindingContext: BindingContext
        get() = KotlinAnalysisFileCache.getAnalysisResult(editor.parsedFile!!, editor.javaProject!!).analysisResult.bindingContext
    
    private val positions = arrayListOf<HighlightPosition>()
    
    fun computeHighlightingRanges(): List<HighlightPosition> {
        positions.clear()
        editor.parsedFile!!.acceptChildren(this)
        return positions.toList() // make copy
    }
    
    private fun highlight(styleAttributes: KotlinHighlightingAttributes, range: TextRange) {
        val shiftedStart = LineEndUtil.convertLfToDocumentOffset(
                editor.parsedFile!!.getText(), 
                range.getStartOffset(), 
                editor.document)
        positions.add(HighlightPosition(styleAttributes, shiftedStart, range.getLength()))
    }
    
    override fun visitElement(element: PsiElement) {
        element.acceptChildren(this)
    }
    
    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        if (expression.getParent() is KtThisExpression) return
        
        val target = bindingContext[BindingContext.REFERENCE_TARGET, expression]
        if (target == null) return
        
        val withSmartCast = bindingContext.get(BindingContext.SMARTCAST, expression) != null
        
        when (target) {
            is PropertyDescriptor -> highlightProperty(expression, target, withSmartCast)
            is VariableDescriptor -> highlightVariable(expression, target, withSmartCast)
        }
        super.visitSimpleNameExpression(expression)
    }
    
    override fun visitProperty(property: KtProperty) {
        val nameIdentifier = property.getNameIdentifier()
        if (nameIdentifier == null) return
        val propertyDescriptor = bindingContext[BindingContext.VARIABLE, property]
        if (propertyDescriptor is PropertyDescriptor) {
            highlightProperty(nameIdentifier, propertyDescriptor, false)
        } else {
            visitVariableDeclaration(property)
        }
        
        super.visitProperty(property)
    }
    
    override fun visitParameter(parameter: KtParameter) {
        val nameIdentifier = parameter.getNameIdentifier()
        if (nameIdentifier == null) return
        val propertyDescriptor = bindingContext[BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter]
        if (propertyDescriptor is PropertyDescriptor) {
            highlightProperty(nameIdentifier, propertyDescriptor, false)
        } else {
            visitVariableDeclaration(parameter)
        }
        
        super.visitParameter(parameter)
    }
    
    private fun visitVariableDeclaration(declaration: KtNamedDeclaration) {
        val declarationDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
        val nameIdentifier = declaration.getNameIdentifier()
        if (nameIdentifier != null && declarationDescriptor != null) {
            highlightVariable(nameIdentifier, declarationDescriptor, false)
        }
    }
    
    private fun highlightProperty(element: PsiElement, descriptor: PropertyDescriptor, withSmartCast: Boolean) {
        val range = element.getTextRange()
        val mutable = descriptor.isVar()
        val attributes = if (DescriptorUtils.isStaticDeclaration(descriptor)) {
            if (mutable) KotlinHighlightingAttributes.STATIC_FIELD else KotlinHighlightingAttributes.STATIC_FINAL_FIELD
        } else {
            if (mutable) KotlinHighlightingAttributes.FIELD else KotlinHighlightingAttributes.FINAL_FIELD
        }
        
        highlight(attributes, range)
    }
    
    private fun highlightVariable(element: PsiElement, descriptor: DeclarationDescriptor, withSmartCast: Boolean) {
        if (descriptor !is VariableDescriptor) return
        
        val attributes = when (descriptor) {
            is LocalVariableDescriptor -> {
                if (descriptor.isVar()) {
                    KotlinHighlightingAttributes.LOCAL_VARIABLE
                } else {
                    KotlinHighlightingAttributes.LOCAL_FINAL_VARIABLE
                }
            }
            
            is ValueParameterDescriptor -> KotlinHighlightingAttributes.PARAMETER_VARIABLE
            
            else -> throw IllegalStateException("Cannot find highlight attributes for $descriptor")
        }
        
        highlight(attributes, element.getTextRange())
    }
}

class HighlightPosition(val styleAttributes: KotlinHighlightingAttributes, offset: Int, length: Int) : Position(offset, length)