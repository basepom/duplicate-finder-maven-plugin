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

import com.google.common.io.CharStreams

def buildFileReader = new FileReader(new File(basedir, "build.log").getCanonicalFile())
def buildLogLines = CharStreams.readLines(buildFileReader)

def linefilter = {line -> line.startsWith("[INFO]") || line.startsWith("[WARNING]") || line.startsWith("[ERROR]")}
def relevantLogLines = buildLogLines.findAll(linefilter).reverse()

def includeMessages = [
  "[WARNING] Found duplicate and different classes in [com.ning.maven.plugins.duplicate-finder-maven-plugin:first-class-jar:1.0,com.ning.maven.plugins.duplicate-finder-maven-plugin:first-diff-jar:1.0]",
  "[WARNING] Found duplicate (but equal) classes in [com.ning.maven.plugins.duplicate-finder-maven-plugin:second-class-jar:1.0,com.ning.maven.plugins.duplicate-finder-maven-plugin:second-equal-jar:1.0]"
]

def excludeMessages = [
]

includeMessages.each() { message ->
    def found = relevantLogLines.find() { line -> return line.indexOf(message) >= 0 }
    assert found != null, "Did not find '" + message + "' in the build output!"
}

excludeMessages.each() { message ->
    def found = relevantLogLines.find() { line -> return line.indexOf(message) >= 0 }
    assert found == null, "Found '" + message + "' in the build output!"
}

return true
