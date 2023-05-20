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

def result = loadTestXml(basedir)

overallState(NO_CONFLICT, 2, NOT_FAILED, result)
checkConflictResult("diff.Demo", TYPE_CLASS, CONFLICT_DIFF, EXCEPTED, NOT_PRINTED, NOT_FAILED, findConflictResult(result, FIRST_CLASS_JAR, projectTargetFolder(basedir)))
checkConflictResult("conflict-same-content", TYPE_RESOURCE, CONFLICT_EQUAL, EXCEPTED, NOT_PRINTED, NOT_FAILED, findConflictResult(result, 2, FIRST_JAR, projectTargetFolder(basedir)))
checkConflictResult("conflict-different-content", TYPE_RESOURCE, CONFLICT_DIFF, EXCEPTED, NOT_PRINTED, NOT_FAILED, findConflictResult(result, 2, FIRST_JAR, projectTargetFolder(basedir)))

return true
