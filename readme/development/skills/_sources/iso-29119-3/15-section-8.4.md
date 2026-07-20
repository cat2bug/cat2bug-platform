# 8.4 Test procedure specification

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `section-8.4`
- **PDF pages**: 33–34

---
<!-- PDF page 33 -->

8.3.3.5  T raceability
Describes traceability to the test coverage items that the test case exercises. This may be recorded in a 
test traceability matrix.
8.3.3.6
 Pr
econditions
Describes the required state of the test environment and test data and any special constraints 
pertaining to the execution of the test case.
EXAMPLE  T he required state of the test item before execution can start, including existence of specific test 
data and the currently active form or screen.
This can be described explicitly, or it can include references to other test cases, whose execution will set the preconditions.
The environment needed may be described collectively for one or more test cases, or it may not be 
described in each individual test case specification if the description in the test plan is sufficient.
8.3.3.7
 Inputs
S
pecifies each action required to bring the test item into a state where the expected result can be 
compared to the actual results. The level of detail provided by the descriptions should be tailored to fit 
the knowledge of the test executors.
NOTE  T his can require provision of input data and/or events, e.g. button clicks, to the test item. Some of the 
input data can be specified by value, while others may be specified by name. Constant tables, transaction files, databases, files, terminal messages, memory resident areas, and values passed by the operating system can be considered.
Specifies all required relationships between input events.
EXAMPLE  A r elationship can be timing.
The actions may be numbered within the test case, if needed.
8.3.3.8
 Expect
ed results
Specifies the expected outputs and behaviour required of the test item in response to the inputs that are 
given to the test item when it is in its precondition state. Provides the expected values (with tolerances where appropriate) for each required output.
EXAMPLE  B ehaviour required of the test item can be response time.
The actions required to compare the expected results to the actual results may also be specified. For instance, examining the output in a field on a form that is not active when the input is provided, waiting for a batch job to run and a report to be printed out and examined, or closing down the test item and restarting it to examine stored data.
8.4	 Test	procedure	specification
8.4.1  Ov erview
The test procedure specification describes the test cases in execution order, along with any associated 
actions that may be required to set up necessary preconditions and any post-execution wrap up activities.ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved23

<!-- PDF page 34 -->

The test procedures may be described in lists, in tables, in a document or stored by a tool, such as a 
database or a dedicated test case management tool.
NOTE  T he test procedures can be described in lists or in tables in a document or using a tool, e.g. a database 
or a dedicated test tool.
A.3 provides an outline of the test procedure specification, while Annex  J provides examples that 
demonstrate how test procedure specifications can be described for two different example projects.
The contents of the test procedure specification are specified in 8.4.2  to 8.4.8 .
8.4.2	 Unique
	
identifier
Describes the unique identifier for the test procedure so that it can be distinguished from all other test 
procedures. An automated tool may control the generation of the identifiers or it may be done manually according to an appropriate notation scheme. The unique identifier is not usually changed during the lifetime of the test procedure, because it is used for traceability purposes.
8.4.3
 Objecti
ve
Identifies and briefly describes the focus or objective of the test procedure.8.4.4
 Priority
D
efines the priority for this particular test procedure, if needed.
8.4.5
 Start up
D
escribes the necessary actions to prepare for execution of the test cases specified in the test procedure. 
This will typically be the actions to set up the preconditions for the first test case to be executed.8.4.6
 Or
dered test cases
Lists the test cases in the order in which they are to be executed. The test cases may be numbered 
sequentially within the test procedure. The degree to which the procedure can be varied may be defined.
This list may be a reference to the test cases, or it may be a copied list of the test cases.
If the execution of one or more test cases in the procedure does not set the preconditions for the 
following test case, actions to set the preconditions may be added between test cases.
The test procedure may include placeholders to record actual results and test results. Alternatively, 
actual results and test results may be recorded in the actual results and test result (see 8.9 ).
8.4.7
 R
elationship to other procedures
Describes dependencies this test procedure may have on any other test procedures.
Examples of dependencies on other test procedures include that they are executed before this one, 
concurrently with this one, or subsequent to this one.
8.4.8
 St
op and wrap up
Describes the actions necessary to bring execution to an orderly halt and the actions necessary after 
the execution of the procedure has been completed.
EXAMPLE  A ctions can be termination of logging or resetting of test database.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved24
