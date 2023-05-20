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

overallState(NO_CONFLICT, 1, NOT_FAILED, result) // This is "1" because both conflictResults are in the same conflict.
checkConflictResult("conflict-same-content", TYPE_RESOURCE, CONFLICT_EQUAL, EXCEPTED, NOT_PRINTED, NOT_FAILED, findConflictResult(result, 2, FIRST_JAR, SECOND_JAR))
checkConflictResult("conflict-different-content", TYPE_RESOURCE, CONFLICT_DIFF, EXCEPTED, NOT_PRINTED, NOT_FAILED, findConflictResult(result, 2, FIRST_JAR, SECOND_JAR))

def conflictingDependencies = xml.configuration.conflictingDependencies
assert 1 == conflictingDependencies.size()

// True because it is a wildcard match
assert "true" == conflictingDependencies[0].conflictingDependency.@wildcard.text()

return true
