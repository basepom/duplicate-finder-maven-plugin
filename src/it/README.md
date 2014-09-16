### Integration tests

test types:

* equal: conflict with equal content
* diff:  conflict with diffing content
* both: both conflicts

|| Test                             || description              || type || pEF || fBICOF || fBICODCC || fBICOECC || report || success ||
| conflict-default                  | defaults                  | both  |      |         |           |           |  diff   |  yes     |
| conflict-default-print-equal      | defaults, reporting       | both  |  yes |         |           |           |  both   |  yes     |
| conflict-fail                     | fail flag                 | both  |  yes |  yes    |           |           |  both   |  no      |
| conflict-equal-fail               | fail flag                 | equal |  yes |  yes    |           |           |  equal  |  no      |
| conflict-diff-fail                | fail flag                 | diff  |  yes |  yes    |           |           |  diff   |  no      |
| conflict-equal-fail-equal-content | fail equal, equal content | equal |  yes |         |           |  yes      |  equal  |  no      |
| conflict-equal-fail-diff-content  | fail equal, diff content  | diff  |  yes |         |           |  yes      |  diff   |  no      |
| conflict-diff-fail-equal-content  | fail diff, equal content  | equal |  yes |         |  yes      |           |  equal  |  yes     |
| conflict-diff-fail-diff-content   | fail diff, diff content   | diff  |  yes |         |  yes      |           |  diff   |  no      |
