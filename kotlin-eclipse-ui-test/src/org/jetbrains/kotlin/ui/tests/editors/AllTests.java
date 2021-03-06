/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.tests.editors;

import org.jetbrains.kotlin.ui.tests.editors.formatter.KotlinFormatActionTest;
import org.jetbrains.kotlin.ui.tests.editors.highlighting.KotlinHighlightingTest;
import org.jetbrains.kotlin.ui.tests.editors.selection.KotlinSelectEnclosingTest;
import org.jetbrains.kotlin.ui.tests.editors.selection.KotlinSelectNextTest;
import org.jetbrains.kotlin.ui.tests.editors.selection.KotlinSelectPreviousTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { 
    KotlinEditorBaseTest.class, 
    KotlinBasicAutoIndentTest.class,
    KotlinAnalyzerInIDETest.class,
    KotlinHighlightingTest.class,
    KotlinBracketInserterTest.class,
    KotlinFormatActionTest.class,
    KotlinCustomLocationBugTest.class,
    PsiVisualizationCommandTest.class,
    KotlinEditorClosedProjectInfluenceTest.class,
    KotlinSelectEnclosingTest.class,
    KotlinSelectNextTest.class,
    KotlinSelectPreviousTest.class
} )
public class AllTests {

}