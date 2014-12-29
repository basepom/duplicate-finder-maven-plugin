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
package org.basepom.mojo.duplicatefinder.groovy

/**
 * Helper tool for the integration tests. Do not use outside the integration tests. Placed in the test sources
 * so that the integration tests can pick it up through the addTestClassPath setting of the invoker plugin.
 */
public final class ITools
{
  def static final TYPE_CLASS = "classes"
  def static final TYPE_RESOURCE = "resources"

  def static final NO_CONFLICT = "no-conflict"
  def static final CONFLICT_EQUAL = "content-equal"
  def static final CONFLICT_DIFF = "content-different"

  def static final NOT_EXCEPTED = false
  def static final EXCEPTED = true

  def static final NOT_PRINTED = false
  def static final PRINTED = true

  def static final NOT_FAILED = false
  def static final FAILED = true

  def static jarName(String a) {
    return "testjar:" + a + ":1.0.under-test"
  }

  def static final FIRST_JAR        = jarName("first-jar")
  def static final SECOND_JAR       = jarName("second-jar")
  def static final FIRST_CLASS_JAR  = jarName("first-class-jar")
  def static final FIRST_DIFF_JAR   = jarName("first-diff-jar")
  def static final SECOND_CLASS_JAR = jarName("second-class-jar")
  def static final SECOND_EQUAL_JAR = jarName("second-equal-jar")

  /**
   * Finds a conflict element in an XML result where the name field matches all of the elements given as matches.
   * This method should be used to locate one conflict that matches all of the fields. Fails if the number of elements
   * is not exactly one.
   *
   * Returns a collection of 'conflictResult' nodes for the conflict.
   */
  def static findSingleConflictResults(result, String ... matches) {
    def elements = result.conflicts.conflict.findAll( { def found = true; matches.each() { match -> found &= it.@name.text().contains(match) }; return found } )
    assert 1 == elements.size()
    assert null != elements[0].conflictResults
    return elements[0].conflictResults.conflictResult
  }

  /**
   * Returns a list of all 'conflictResult' nodes from any matching conflict. This allows "underdefining" a conflict and returning the results for multiple
   * matches at once. Mostly useful if one or more names are not known (e.g. because they are installation specific (classpath matches)).
   */
  def static findAllConflictResults(result, String ... matches) {
    def elements = result.conflicts.conflict.findAll( { def found = true; matches.each() { match -> found &= it.@name.text().contains(match) }; return found } )
    return elements.conflictResults.conflictResult.collect()
  }

  /**
   * Ensures that only a single conflictResult elements from a single conflict exists.
   */
  def static findConflictResult(result, String ... matches) {
    return findConflictResult(result, 1, matches);
  }

  /**
   * Ensures that exactly 'count' conflictResult elements from a single conflict exists.
   */
  def static findConflictResult(result, int count, String ... matches) {
    def conflictResult = findSingleConflictResults(result, matches)
    assert conflictResult.size() == count
    return conflictResult
  }

  /**
   * Checks whether a given conflict result matches in name, type and conflict state.
   */
  def static checkConflictResult(String conflictName, String conflictType, String conflictState, boolean excepted, boolean printed, boolean failed, conflictResult) {
    println("*** TEST: checkConflictResult(name:${conflictName}, type:${conflictType}, state:${conflictState}, excepted:${excepted}, printed:${printed}, failed:${failed}, result)...");

    assert null != conflictResult

    def results = conflictResult.findAll( { it.@name.text().equals(conflictName) } )
    assert 1 == results.size()

    def result = results[0]

    assert conflictName == result.@name.text()
    assert conflictType == result.@type.text()
    assert conflictState == result.@conflictState.text()
    assert Boolean.toString(excepted) == result.@excepted.text()
    assert Boolean.toString(printed) == result.@printed.text()
    assert Boolean.toString(failed) == result.@failed.text()

    println("*** TEST: checkConflictResult(name:${conflictName}, type:${conflictType}, state:${conflictState}, excepted:${excepted}, printed:${printed}, failed:${failed}, result) --> OK");

    return result
  }

  /**
   * Checks whether a given conflictResult is also a bootClasspath element match.
   */
  def static isBootClasspathMatch(conflictResult) {
    assert 1 == conflictResult.conflictNames.conflictName.grep{it.@bootClasspathElement.text() == "true" }.size()
  }

  /**
   * Loads the full XML result file and returns the root result node for the named result.
   */
  def static loadXml(dir, String name) {
    return loadXmlAndResult(dir, name)[0]
  }

  /**
   * Loads the full XML result and return the root result node and the full XML as an array.
   */
  def static loadXmlAndResult(dir, String name) {
    def xml = new XmlSlurper().parse(new File(dir, "target/duplicate-finder-result.xml").getCanonicalFile())
    assert null != xml

    // Version sanity check
    assert "1" == xml.@version.text()

    def elements = xml.results.result.findAll( { it.@name.text().equals(name) } )
    assert 1 == elements.size()
    return [ elements[0], xml ]
  }

  /**
   * Finds the 'test' result in the XML node.
   */
  def static loadTestXml(dir) {
    return loadXml(dir, "test")
  }

  /**
   * Returns the location of the project target/classes folder.
   */
  def static projectTargetFolder(dir) {
    return new File(dir, "target/classes").getAbsolutePath()
  }

  /**
   * Returns the location of the project target/test-classes folder.
   */
  def static projectTargetTestFolder(dir) {
    return new File(dir, "target/test-classes").getAbsolutePath()
  }

  /**
   * checks the basic state of a test result.
   */
  def static overallState(String state, int conflictCount, boolean failed, result) {
    println("*** TEST: overallState(state:${state}, count:${conflictCount}, failed:${failed}, result)...");
    assert state == result.@conflictState.text()
    assert Boolean.toString(failed) == result.@failed.text()
    assert conflictCount == result.conflicts.conflict.size()
    println("*** TEST: overallState(state:${state}, count:${conflictCount}, failed:${failed}, result) --> OK");
  }
}
