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

overallState(CONFLICT_DIFF, 4, NOT_FAILED, result) // Lots of conflicts expected. check a few.

isBootClasspathMatch(checkConflictResult("javax.activation.ActivationDataFlavor", TYPE_CLASS,    CONFLICT_DIFF, NOT_EXCEPTED, PRINTED, NOT_FAILED, findAllConflictResults(result, "javax.activation:activation:1.1")))

isBootClasspathMatch(checkConflictResult("javax.xml.bind.Binder",                 TYPE_CLASS,    CONFLICT_DIFF, NOT_EXCEPTED, PRINTED, NOT_FAILED, findAllConflictResults(result, "javax.xml.bind:jaxb-api:2.2.2")))
isBootClasspathMatch(checkConflictResult("javax/xml/bind/Messages.properties",    TYPE_RESOURCE, CONFLICT_DIFF, NOT_EXCEPTED, PRINTED, NOT_FAILED, findAllConflictResults(result, "javax.xml.bind:jaxb-api:2.2.2")))

return true
