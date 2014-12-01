/*
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
 */
import static org.basepom.mojo.duplicatefinder.groovy.ITools.*

def (result, xml) = loadXmlAndResult(basedir, "test")
assert "3" == xml.preferences.@resultFileMinClasspathCount.text()

overallState(CONFLICT_DIFF, 2, NOT_FAILED, result)
checkConflictResult("diff.Demo",                  TYPE_CLASS,    CONFLICT_DIFF,  NOT_EXCEPTED, PRINTED,     NOT_FAILED, findConflictResult(result, 1, FIRST_CLASS_JAR, FIRST_DIFF_JAR))
checkConflictResult("conflict-same-content",      TYPE_RESOURCE, CONFLICT_EQUAL, NOT_EXCEPTED, NOT_PRINTED, NOT_FAILED, findConflictResult(result, 2, FIRST_JAR, SECOND_JAR))
checkConflictResult("conflict-different-content", TYPE_RESOURCE, CONFLICT_DIFF,  NOT_EXCEPTED, PRINTED,     NOT_FAILED, findConflictResult(result, 2, FIRST_JAR, SECOND_JAR))

assert 2 == result.classpathElements.size()

def resources = result.classpathElements.findAll({ it.@type.text().equals(TYPE_RESOURCE) })
assert null != resources
assert 0 == resources.classpathElement.size()

def classes = result.classpathElements.findAll({ it.@type.text().equals(TYPE_CLASS) })
assert null != classes
assert 0 == classes.classpathElement.size()

return true
