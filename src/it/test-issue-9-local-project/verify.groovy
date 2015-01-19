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

def p1Dir = new File(basedir, "p1")
def p2Dir = new File(basedir, "p2")

def p1Result = loadTestXml(p1Dir)
overallState(NO_CONFLICT, 0, NOT_FAILED, p1Result)

def p2Result = loadTestXml(p2Dir)
overallState(NO_CONFLICT, 0, NOT_FAILED, p2Result)

return true
