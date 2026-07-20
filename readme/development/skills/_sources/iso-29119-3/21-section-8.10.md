# 8.10 Test execution log

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `section-8.10`
- **PDF pages**: 38–38

---
<!-- PDF page 38 -->

8.9 A ctual results and test result
8.9.1  Gener al
The actual results and the test result can be stored within a copy of the test case specification or 
test procedure specification (when test cases are embedded within a test procedure specification). The actual results and test result are not usually considered as independent documents. By storing actual results and test result in a copy of the test case specification or test procedure specification, the documents become a combined specification and results document.
The requirements for the actual results and test results are defined in 8.9.2  and 8.9.3 .
8.9.2
 A
ctual results
The actual results are a record of the result of the execution of a test case. The actual results are 
compared to the expected results to determine the test result.
Actual results are not always formally recorded. For some types of systems (e.g. regulated safety 
critical), there may be a requirement to fully document the actual results, and for some systems (e.g. those with high data integrity or reliability requirements) the testers may choose to fully record the actual results. The recording of actual results may be done by an automated tool during test execution.
Some test cases may include actions that provide results, which are not part of the actual results of 
executing the complete test case, but intermediate results. These may be recorded separately in the test log or with the actual results (with a clear distinction between the actual results and the intermediate results).
8.9.3
 T
est result
The test result is a record of whether or not a specific test case has passed or failed; i.e. if the actual 
results correspond to the expected results or if deviations were observed, or if planned execution of the test case was not possible.
EXAMPLE  I f the actual results and expected results match, then the test result can be a tick mark or the word 
“passed”. Otherwise, if the test case execution fails, then the test result can be marked with a cross or with the word “failed” along with the unique identifier of the incident report that was raised as a result of the observation of the test failure. If the test case cannot be executed, then the test result can be marked as “blocked”, typically along with information on what caused the test case to be blocked, if that information is available.
Sometimes this process is fully automated, with the tool comparing the actual results to expected results, and providing a report of which test cases passed, failed, or cannot be executed.
NOTE  T est result is sometimes referred to as the “pass/fail” status of a test case.
8.10  T est execution log
8.10.1  Ov erview
Records details of the execution of one or more test procedures as a series of events. Each event may be 
described in a list, a table, in a document or stored in a tool, such as a database or a dedicated test case management tool.
EXAMPLE  E vents that can be recorded in the test execution log include:
—
 t
he date/time that the test execution session started and ended;
—
 t
he starting date/time at which each individual test was executed;
—
 t
he details of when a sudden drop in performance occurred of the computer on which a test was being 
executed;ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved28
