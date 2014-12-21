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

overallState(NO_CONFLICT, 0, NOT_FAILED, result)

def conflictingDependencies = xml.preferences.conflictingDependencies
assert 1 == conflictingDependencies.size()

// True because we want to include the current project
assert "true" == conflictingDependencies[0].conflictingDependency.@currentProject.text()
// Must be false because the current project was already included.
assert "false" == conflictingDependencies[0].conflictingDependency.@currentProjectIncluded.text()

return true
