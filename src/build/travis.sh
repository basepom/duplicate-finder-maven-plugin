#! /bin/bash

set -ev

export OLD_PATH=${PATH}

switch_travis_jdk() {
    for jdk_prefix in /usr/local/lib/jvm $TRAVIS_HOME; do
        if [ -d "${jdk_prefix}/openjdk${1}" ]; then
            export JAVA_HOME=${jdk_prefix}/openjdk${1}
            export PATH=${JAVA_HOME}/bin:${OLD_PATH}
            return
        fi
    done

    $TRAVIS_HOME/bin/install-jdk.sh --target "${TRAVIS_HOME}/openjdk${1}" --workspace "${TRAVIS_HOME}/.cache/install-jdk" --feature "${1}" --license "GPL" --cacerts
    export JAVA_HOME=${TRAVIS_HOME}/openjdk${1}
    export PATH=${JAVA_HOME}/bin:${OLD_PATH}
}
