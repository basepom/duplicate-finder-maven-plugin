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
package org.basepom.mojo.duplicatefinder;

import static com.google.common.base.Preconditions.checkState;

public enum ConflictState
{
    // Conflict states in order from low to high.
    NO_CONFLICT(""),
    CONFLICT_CONTENT_EQUAL("(but equal)"),
    CONFLICT_CONTENT_DIFFERENT("and different");

    private final String hint;

    private ConflictState(String hint)
    {
        this.hint = hint;
    }

    public String getHint()
    {
        return hint;
    }

    public static ConflictState max(final ConflictState ... states)
    {
        checkState(states.length > 0, "states is empty");

        ConflictState result = states[0];
        for (int i = 1; i < states.length; i++) {
            if (states[i].ordinal() > result.ordinal()) {
                result = states[i];
            }
        }

        return result;
    }
}
