# 8.6 Test environment requirements

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `section-8.6`
- **PDF pages**: 35–36

---
<!-- PDF page 35 -->

8.5 T est data requirements
8.5.1  Ov erview
The test data requirements describe the test data needed to execute the test procedures defined in the 
test procedure specification.
NOTE  T he data quality model defined in ISO/IEC  2 5012 can support the identification of test data 
requirements. Data quality characteristics include accuracy, completeness, consistency, credibility, currentness, accessibility, compliance, confidentiality, efficiency, precision, traceability, understandability, availability, portability and recoverability. Data quality can be measured via data quality measures defined in ISO/IEC
 2
5024.
A.3 provides an outline of the test data requirements documentation, while Annex  K provides examples 
that demonstrate how test data requirements can be described for two different example projects.
The contents specified in 8.5.2  to 8.5.7  apply to each individual Test Data Requirement.
8.5.2	 Unique
	
identifier
Provides a unique identifier for each test data item so that it can be distinguished from all other test 
data items.
8.5.3
 Description
D
efines the specific name and required values or ranges of values for each test data element. It may also 
describe when data needs to be made anonymous or manipulated in other ways.
EXAMPLE  “ At least 10 customers must exist in the database with complete and correct CustomerID and all 
other mandatory customer information.”
8.5.4  R esponsibility
Specifies who is responsible for making the test data available.
8.5.5
 P
eriod needed
Identifies when and for how long the test data is needed. Test data may be needed for a single undivided 
period or for several separate periods.
8.5.6
 R
esetting needs
Specifies if the test data needs to be reset during testing.8.5.7
 Ar
chiving or disposal
Identifies when and how test data may be archived or disposed of after completion of the testing.
8.6 T est environment requirements
8.6.1  Ov erview
The test environment requirements describe the properties of the test environment needed to execute 
the test procedures defined in the test procedure specification. If test environment requirements have been fully specified elsewhere, then separate test environment requirements documentation is not needed.
EXAMPLE  T he test environments requirements can be found in the organizational test practices, test plan 
or test specification.ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved25

<!-- PDF page 36 -->

In practice, the test environment might not be a perfect representation of the operational (production) 
environment. As such, the test environment requirements should mention the degree to which each test environment item needs to represent the operational environment.
A.3 provides an outline of the test environment requirements documentation, while Annex
 L 
provides 
examples that demonstrate how test environment requirements can be described for two different 
example projects.
The contents specified in 8.6.2  to 8.6.6  apply to each individual test environment item that is specified 
in the test environment requirements.
8.6.2	 Unique
	
identifier
Provides a unique identifier for each test environment item so that it can be distinguished from all 
other test environment items. Unique identifiers are used for traceability issues, so they should not usually be changed during the lifetime of the test environment.
8.6.3
 T
est environment item
Identifies each element of the test environment that is required for execution of the test procedures 
defined in the test procedure specification. Test environment items may be required to support set-up before the execution of a test procedure, for execution of test procedures and for any activities after test execution. If each test environment item is not a perfect reflection of the operational (production) environment, then the degree to which each test environment item represents or deviates from the operational environment should be stated.
EXAMPLE  T he test environment items can be divided into the following types, although other types can be 
needed depending on the specific environment requirements:
— h
ardware;
— m
iddleware;
—
 s
oftware, e.g. specific systems or browsers;
—
 i
nterfaces to other systems and interfacing systems;
—
 p
eripherals, e.g. printers;
—
 m
obile devices, e.g. mobile phones, tablets;
—
 c
ommunication means, e.g. web access;
— t
ools;
—
 s
ecurity;
—
 v
enue, e.g. size of rooms, desks/chairs and background noise level;
—
 ac
cessories, e.g. special pre-printed paper forms.
8.6.4  Description
I
dentifies the environment item in sufficient detail for it to be implemented as required.
EXAMPLE  T his can include precisely named hardware or software in specific versions and specific 
configurations. It can also list required batch jobs that need to be run at certain points during testing to support 
the testing process, such as the requirement to run a standard set of batch jobs nightly, so as to reflect the production environment.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved26
