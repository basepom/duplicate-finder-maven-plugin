/*
 * Copyright 2010 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.maven.plugins.duplicatefinder.groovy

/**
 * Helper tool for the integration tests. Do not use outside the integration tests. Placed in the test sources
 * so that the integration tests can pick it up through the addTestClassPath setting of the invoker plugin.
 */
public final class ITools
{
  def static final CONFLICT_EQUAL = "CONFLICT_CONTENT_EQUAL"
  def static final CONFLICT_DIFF = "CONFLICT_CONTENT_DIFFERENT"

  def static final TYPE_CLASS = "CLASS";
  def static final TYPE_RESOURCE = "RESOURCE";

  def static jarName(String g, String a, String v) {
    return g + ":" + a + ":" + v;
  }

  def static final FIRST_CLASS_JAR = jarName("testjar", "first-class-jar", "1.0.under-test")
  def static final FIRST_DIFF_JAR = jarName("testjar", "first-diff-jar", "1.0.under-test")
  def static final SECOND_CLASS_JAR = jarName("testjar", "second-class-jar", "1.0.under-test")
  def static final SECOND_EQUAL_JAR = jarName("testjar", "second-equal-jar", "1.0.under-test")

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
   * Ensures that only a single conflictResult elements from a single conflict exists. 
   */
  def static findConflictResult(result, String ... matches) {
    def conflictResult = findSingleConflictResults(result, matches)
    assert 1 == conflictResult.size()
    return conflictResult[0]
  }

  /**
   * Checks whether a given conflict result matches in name, type and conflict state.
   */
  def static checkConflictResult(String conflictName, String conflictType, String conflictState, result) {
    assert null != result

    assert conflictName == result.@name.text()
    assert conflictType == result.@type.text()
    assert conflictState == result.@conflictState.text()
  }

  /**
   * Loads the full XML result file and returns the root node.
   */
  def static loadXml(dir) {
    def xml = new XmlSlurper().parse(new File(dir, "target/duplicate-finder-result.xml").getCanonicalFile())
    assert null != xml
    return xml
  }

  /**
   * Finds the 'test' result in the XML node.
   */
  def static loadTestXml(dir) {
    def xml = new XmlSlurper().parse(new File(dir, "target/duplicate-finder-result.xml").getCanonicalFile())
    def result = xml.results.result[0]
    assert "test" == result.@name.text()
    return result
  }

  /**
   * checks the basic state of a test result.
   */
  def static overallState(String state, int conflictCount, result) {
    assert state == result.@conflictState.text()
    assert conflictCount == result.conflicts.conflict.size()
  }
}
