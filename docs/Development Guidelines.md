Any pull request that contains integration tests to demonstrate the change and unit tests that test the code changes in isolation **will receive preferred treatment**. If no test cases or integration tests are included, pull request may sit dormant for a longer time.

### Developing code and sending pull requests

* Use spaces to indent, not hard tab characters. The code base uses four space indents, please respect this.
* Use the US-ASCII charset. If unicode characters are necessary (e.g. for a test case), please use the `\uxxxx` escape syntax.
* Do not reformat an existing file or imports. If an existing file is changed, please format the code similar to the rest of the file.

### Integration test suite

The duplicate finder plugin contains a comprehensive [integration test suite](Writing Integration Tests) to ensure its behavior and catch possible regressions. Any significant code change, feature addition or fix **must** come with a test that fails before the change is applied and passes afterwards.




