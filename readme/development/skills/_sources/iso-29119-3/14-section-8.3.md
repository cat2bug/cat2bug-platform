# 8.3 Test case specification

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `section-8.3`
- **PDF pages**: 31–32

---
<!-- PDF page 31 -->

8.2.3  Objecti ve
Identifies and briefly describes the particular focus of the test model.
8.2.4
 Priority
D
efines the priority of the testing associated with this particular test model, if needed.
8.2.5
 T
est strategy extract
Includes the part of the test strategy that describes the relevant test design technique and test 
completion criterion. This may also include whether the test will be automated or manually executed.
8.2.6
 T
est model
Presents the test model. The notation used for the test model will depend on the required test coverage. 
Thus, it can be written in natural language, formal language, expressed in a tabular format, graphical format or other format as appropriate. It may simply reference the artefact (e.g. source code) that is the test model.
8.2.7
 T
raceability
Describes traceability from the test model to associated requirements or artefacts (e.g. source code). 
This can be recorded in a test traceability matrix.
8.3	 Test	case	specification
8.3.1  Ov erview
The test case specification identifies the test coverage items and the corresponding test cases derived 
by applying one or more test techniques to the test model.
A.3 provides an outline of the test case specification, while Annex  
I provides examples that demonstrate 
how test case specification can be described for two different example projects.The contents for a test case specification are specified in 8.3.2  and 8.3.3 .
8.3.2
 T
est coverage items
8.3.2.1
 Ov
erview
Summarizes the test coverage items identified from the test model. Test coverage items are derived by 
applying a test design technique to the test model.
EXAMPLE  T est coverage items in the form of transitions between states can be derived from a state 
transition diagram by applying state transition testing.
NOTE
 T
he test coverage items can be described in lists or in tables in a document or using a tool, e.g. a 
database or a dedicated test tool. Test coverage items are not always formally documented, as they can be seen as 
a draft of test cases.
The contents for a test coverage item are defined in 8.3.2.2  to 8.3.2.5.
8.3.2.2	 Unique
	
identifier
Describes the unique identifier for the test coverage item so that it can be distinguished from all other test coverage items. An automated tool may control the generation of the identifiers, or it may be done manually according to an appropriate notation scheme. The unique identifier is not usually changed during the lifetime of the test coverage item because it is used for traceability purposes.ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved21

<!-- PDF page 32 -->

When the number or volatility of test coverage items is so high that the requirement for unique 
identifiers becomes impractical, they may be replaced by the use of other means, generally based on automated tools, to establish traceability between test cases and the test model.
8.3.2.3
 Description
D
escribes the test coverage item; that is, the attribute that is expected to be covered by a test case 
according to the test design technique that was used during its derivation. It may also include additional 
information about the coverage item.
EXAMPLE  W hether or not it is a valid or invalid equivalence partition.
8.3.2.4  Priority
D
efines the priority for the testing of this particular test coverage item, if needed. The higher priority 
test coverage items will be executed before lower priority test coverage items.
8.3.2.5
 T
raceability
Describes traceability from the test coverage items to the test model. This may be recorded in a test 
traceability matrix.
8.3.3
 T
est cases
8.3.3.1
 Ov
erview
Defines the test cases derived from the test coverage items. A test case specifies how one or more test 
coverage items are exercised to help determine whether or not the test item has been implemented correctly.
The number of test cases derived from test coverage items will depend on the test coverage criterion 
defined in the test plan (e.g. if only 80
 % 
test coverage is required not all test coverage items need to 
have an associated test case) and the risk level associated with the test coverage item (e.g. more test cases are normally generated for higher risk test coverage items).
NOTE  T he test cases can be described in lists or in tables in a document or using a tool, e.g. a database or a 
dedicated test tool.
The information for a test case is specified in 8.3.3.2  to 8.3.3.8 .
8.3.3.2	 Unique
	
identifier
Describes the unique identifier for the test case so that it can be distinguished from all other test cases. An automated tool may control the generation of the identifiers or it may be done manually according to an appropriate notation scheme. The unique identifier is not usually changed during the lifetime of the test case, because it is used for traceability purposes.
8.3.3.3
 Objecti
ve
Identifies and briefly describes the special focus or objective of the test case. This is typically in the 
form of a title.
8.3.3.4
 Priority
D
efines the priority for the testing of this particular test case, if needed. The higher priority test cases 
will be run before the lower priority test cases, where possible.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved22
