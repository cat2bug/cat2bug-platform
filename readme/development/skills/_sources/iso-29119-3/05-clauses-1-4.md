# 1–4 Scope, Normative references, Terms and definitions, Conformance

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `clauses-1-4`
- **PDF pages**: 11–15

---
<!-- PDF page 11 -->

Software and systems engineering  — Software testing  —
P
art
 
3: 
Test documentation
1 Scope
This document specifies software test documentation templates that can be used for any organization, 
project or testing activity. It describes the test documentation that is an output of the processes specified in ISO/IEC/IEEE
 
29119-2. 
This document is applicable to testing in all software development lifecycle models. This document is intended for, but not limited to, testers, test managers, developers, and project managers, particularly those responsible for governing, managing, and implementing software testing.
2 Normat ive references
There are no normative references in this document.
3	 Terms	and	definitions
For the purposes of this document, the following terms and definitions apply.
ISO, IEC and IEEE maintain terminology databases for use in standardization at the following addresses:—
 I
SO Online browsing platform: available at h t t p s ://  w
ww .
iso .
org/  o
bp
—
 I
EC Electropedia: available at h t t p s ://  w
ww .e
lectropedia  .
org/  
—
 I
EEE Standards Dictionary Online: available at h t t p s ://  ie
eexplore  .i
eee .
org/  x
pls/  d
ictionary  .
jsp
NOTE  F or additional terms and definitions in the field of systems and software engineering, see ISO/IEC/
IEEE
 2
4765, which is published periodically as a “snapshot” of the SEVOCAB (Systems and software Engineering 
Vocabulary) database and is publicly accessible at h t t p s ://  www  .
computer  .
org/  se
vocab .
3.1
actual resultsset of behaviours or conditions of a test item, or set of conditions of associated data or the test environment, observed as a result of test execution
EXAMPLE  O utput to screen, outputs to hardware, changes to data, reports, and communication messages 
sent.
3.2expected resultsobservable predicted behaviour of the test item under specified conditions based on its specification or another source
3.3
incidentanomalous or unexpected event, set of events, condition, or situation at any time during the life cycle of a project, product, service, or systemINTERNATIONAL ST ANDARD ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved1

<!-- PDF page 12 -->

3.4
incident reportdocumentation of the occurrence, nature, and status of an incident  (3.3 )
Note  1  t o  e ntry:  I ncident reports are also known as anomaly reports, bug reports, defect reports, error reports, 
issues, problem reports and trouble reports, amongst other terms.
3.5organizational test practicesdocumentation that expresses the recommended approaches or methods for the testing to be performed within an organization, providing detail on how the testing is to be performed
Note  1 to entry:  The organizational test practices is aligned with the test policy  (3 .20).
Note
 2
 t
o entry:
 A
n organization can have more than one organizational test practices document to cover 
markedly different contexts, such one for mobile apps and one for safety-critical systems.
Note
 3
 t
o entry:
 T
he organizational test practices can incorporate the context of the test policy where no separate 
test policy is available.
3.6
organizational
	
test
	
specification
documentation that provides information about testing for an organization, i.e. information that is not project specific
EXAMPLE  T he most common examples of organizational test specifications are the test policy  (3.20 ) and 
organizational test practices  (3.5).
3.7test basisinformation used as the basis for designing and implementing test cases
Note  1  t o  e ntry:  T he test basis can take the form of documentation, such as a requirements specification, design 
specification, or module specification, but can also be an undocumented understanding of the required behaviour.
3.8test
	
case
	
specification
documentation of a set of one or more test cases
3.9
test completion reporttest summary reportreport that provides a summary of the testing that was performed
3.10
test data readiness reportdocumentation describing the status of each test data requirement
3.11
test environment itemelement of a test environment that can be considered separately from other parts of the test environment
EXAMPLE  H ardware, software, interfaces, peripherals, tools.
3.12test environment readiness reportdocumentation that describes the status of each test environment requirement
Note  1 to entry:  This can list the status of each of the test environment requirements  (3 .13).ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved2

<!-- PDF page 13 -->

3.13
test environment requirementsdocumentation of the necessary properties of the test environment
Note  1  t o  e ntry:  A ll or parts of the test environment requirements can reference where the information can be 
found, e.g. in the appropriate organizational test practices  (3.5) document, test plan  (3.19 ) and/or test specification  
(3.23 ).
3.14test execution logrecord of the execution of one or more test procedures
3.15
test incidentevent occurring during the execution of a test that requires investigation
3.16
test modelrepresentation of the test item, which allows the testing to be focused on particular characteristics or qualities
EXAMPLE  R equirements statements, equivalence partitions, state transition diagram, use case description, 
decision table, input syntax, source code, control flow graph, parameters and values, classification tree, natural language.
Note
 
1
 
to entry:
 
The test model and the required test coverage are used to identify test coverage items.
Note
 2
 t
o entry:
 A 
separate test model can be required for each different type of required test coverage included 
in the test completion criteria.Note
 
3
 
to entry:
 
A test model can include one or more test conditions.
Note
 4
 t
o entry:
 T
est models are commonly used to support test design (e.g. they are used to support the test 
design in ISO/IEC/IEEE
 2
9119-4, and they are used in model-based testing). Other types of models exist to 
support other aspects of testing, such as test environment models, test maturity models and test architecture 
models.
3.17test
	
model
	
specification
documentation specifying the test model  (3.16 )
3.18test organizationmanagement structure responsible for testing within an organization
Note  1 to entry:  The test organization is typically technically, managerially and financially independent from the 
development organization.
3.19test plandetailed description of test objectives to be achieved and the means and schedule for achieving them, organized to coordinate testing activities for some test item or set of test items
Note  1  t o entry:  A project can have more than one test plan, for example there can be a project test plan (also 
known as a master test plan) that encompasses all testing activities on the project; further detail of particular test activities can be defined in one or more test level / test type plans (e.g. a system test plan or a performance test plan).
Note
 2
 t
o entry:
 A 
test plan is typically a written document, although other formats can be possible as defined 
locally within an organization or project.Note
 
3
 
to entry:
 
Test plans can also be written for non-project activities, for example a maintenance test plan.ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved3

<!-- PDF page 14 -->

3.20
test policyorganizational test policyexecutive-level documentation that describes the purpose, goals, principles and scope of testing within an organization
Note  1 to entry:  The test policy defines what testing is performed and what it is expected to achieve but does not 
detail how testing is to be performed.
Note
 2
 t
o entry:
 T
he test policy can provide a framework for establishing, reviewing and continually improving 
the organization's testing.
3.21
test
	
procedure
	
specification
test scriptdocumentation specifying one or more test procedures
3.22
test resultindication of whether or not a specific test case has passed or failed, i.e. if the actual results  (3.1 ) 
corresponds to the expected results  (3.2 ) or if deviations were observed
3.23test
	s
pecification
complete documentation of the test design, test cases and test procedures for a specific test item
Note  1  t o  e ntry:  A test specification can be detailed in one document, in a set of documents, or in other ways, for 
example in a mixture of documents and database entries.
3.24test status reportreport that provides information about the status of the testing that is being performed in a specified reporting period
3.25
test strategypart of the test plan  (3.19 ) that describes the approach to testing for a specific project, test level or test 
type
Note  1  t o  e ntry:  T he test strategy usually describes some or all of the following; the test levels and test types to be 
implemented; the retesting and regression testing to be employed; the test design techniques and corresponding test completion criteria to be used; test data; test environment and testing tool requirements; and expectations for test deliverables.
3.26test traceability matrixverification cross reference matrixrequirements test matrixrequirements verification tabledocument, spreadsheet, or other tool used to identify related items in documentation and software, such as requirements with associated tests
Note  1 to entry:  Different test traceability matrices can have different information, formats, and levels of detail.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved4

<!-- PDF page 15 -->

4 Conformance
4.1 Int ended usage
4.1.1  Gener al
The requirements in this document are contained in Clauses  
4, 5, 6, 7, 8 and Annex
 
A.
This document provides requirements for test documentation across the complete software testing 
lifecycle. Test documentation shall be considered as conforming if it is available in an electronic format (e.g. records in a test management tool or spreadsheet), divided into separate documents, or combined with other documents into one document.
In this document, for simplicity, each item of test documentation is described as if it were a separate 
hardcopy document. Test documentation titles, headings and layout described in this document may be modified (e.g. added to, combined or re-titled). The use of the nomenclature used in Clauses
 5, 6, 7
 and 
8 is not mandatory.
The information items identified in Clauses  6, 7
 and 8 correspond to the outputs of the test processes 
in ISO/IEC/IEEE
 2
9119-2. Annex  A 
provides an overview of the requirements for clauses in ISO/IEC/
IEEE
 
29119-2 where the creation of the information items defined in Clauses
 
6, 7 and 8 are described.
It is recognized that some organizations, projects or teams may not need to use all of the test 
documentation defined by this document. Therefore, implementation of this document typically involves selecting a set of test documentation suitable for the organization, project or team. There are two ways that an implementation can be claimed to conform to the provisions of this document: full conformance and tailored conformance.
The organization shall assert whether it is claiming full or tailored conformance to this document.
NOTE  1 O ptions for conformance are provided for needed flexibility in the application of this document.
NOTE
 
2
 W
hen this document is used to help develop an agreement between an acquirer and a supplier, clauses 
of this document can be selected for incorporation in the agreement with or without modification. In this case, 
it is more appropriate for the acquirer and supplier to claim compliance with the agreement than conformance with this document.
NOTE
 
3
 A
n organization (for example, national, industrial association, company) imposing this document, 
as a condition of trade, can specify and make public the minimum set of test documentation, which constitute 
suppliers’ compliance with the conditions of trade.
NOTE
 
4
 R
equirements of this document are marked by the use of the verb “shall”. Recommendations are 
marked by the use of the verb “should". Permissions are marked by the use of the verb “may”. However, despite 
the verb that is used, the requirements for conformance are selected as described previously.
4.1.2  F ull conformance
Full conformance shall be achieved by providing evidence that all requirements (i.e. shall statements) 
of the test documentation defined in Clauses
 
5 to 8 and Annex
 
A have been met.
4.1.3
 T
ailored conformance
When this document is used as a basis for establishing a set of test documentation that does not qualify for full conformance, the subset of test documentation for which tailored conformance is claimed, shall be recorded. Tailored conformance is achieved by demonstrating that all requirements (i.e. shall statements) for the recorded subset of test documentation defined in Clauses
 5 t
o 8 and Annex  A h
ave 
been satisfied.
Where tailoring occurs, justification shall be provided (either directly or by reference), whenever the 
requirements (i.e. shall statements) of test documentation defined in Clauses  5 t
o 8 and Annex  A a
re ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved5
