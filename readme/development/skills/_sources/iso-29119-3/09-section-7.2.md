# 7 Test management processes documentation / 7.2 Test plan

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `section-7.2`
- **PDF pages**: 22–26

---
<!-- PDF page 22 -->

6.3.3.10  R egression testing
Identifies the practices, conditions and activities for regression testing in each test level or type.
7 T est management processes documentation
7.1 Overview
The test documentation developed in the test management processes comprise the following types:
—
 t
est plan;
—
 t
est status report;
— t
est completion report.
The full templates with explanatory text for each of these items of test documentation are specified 
in 7.2 to 7.4 . Annex  A p
rovides an abbreviated overview of each item of test documentation along with 
the requirements, recommendations and permissions (shall, should and may) for each section of the test documentation. Annexes
 E, F
 and G provide examples of test plans, test status reports, and test 
completion reports for example projects.
7.2 T est plan
7.2.1  Ov erview
The test plan is an item of test documentation that is used for test planning and test management. It 
describes the decisions made during initial test planning and evolves as re-planning is performed as part of the control activity. Some projects may have a single test plan, while for larger projects multiple test plans may be produced. Test plans may apply across multiple projects (at the programme level), or to a single project (i.e. project test plan/master test plan), or to a specific test level or type (e.g. system test plan, integration software test plan, subsystem test plan, subcontractor software test plan, unit software test plan, or performance test plan), or to a specific iteration of testing. If multiple software test plans are created, a mapping tree may be produced to aid the documentation of relationships and the information contained in each.
If the organization does not yet have organizational test practices defined, then there may be sections 
from that test documentation that need to be included in the test plan. This may include the test selection and prioritization ( 6.3.2.3 ), test automation and tools ( 6.3.2.5 ), configuration management 
of test work products ( 6.3.2.6 ) and incident management ( 6.3.2.7 ), as these are not repeated in the test 
plan template defined in this subclause.
A.3 provides an outline of the test plan, while Annex
 E 
provides examples that demonstrate how test 
plans can be developed for two different example projects.The contents of the test plan are specified in 7.2.2  to 7.2.10 .
7.2.2
 C
ontext of testing
7.2.2.1
 Pr
ojects / test levels / test types
Identifies the projects, test levels and/or test types for which the test plan is being written and other 
relevant contextual information.
7.2.2.2
 T
est items
Identifies the test items for the testing covered by this test plan including version/revision information 
or a reference for where this information can be found.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved12

<!-- PDF page 23 -->

This section may describe the purpose of the test item or reference where this information can be found.
NOTE  T his information can be defined in a system definition document, such as a concept of operations.
EXAMPLE
 T
he test item can be a software unit, a group of units or modules, a service, a subsystem, or a 
complete system, app or product.
7.2.2.3  T est scope
Summarizes the features of each test item to be tested. Also identifies any features of a test item that 
are to be specifically excluded from testing and the rationale for their exclusion.
EXAMPLE  F eatures to be tested can be specific functions, interfaces, business processes or attributes of the 
software, such as usability or performance.
7.2.2.4  T est basis
Identifies the basis for designing and implementing tests.
7.2.3
 Assumptions and c
onstraints
Describes any assumptions and constraints for the testing covered by this plan. These may include 
regulatory standards, requirements in the test policy and the organizational test practices, contractual requirements, project time and cost constraints, and availability of appropriately skilled staff, tools and/or environments.
7.2.4
 Stak
eholders
Lists the stakeholders and their relevance to the testing.
EXAMPLE  1 T his can include the stakeholder with the authority for resolving issues raised as a result of the 
testing activities and the stakeholder with the authority for approving test products and processes.
EXAMPLE
 
2
 T
his information can be captured in a RACI (responsible, accountable, consulted, informed) 
matrix.
7.2.5  T esting communication
Describes the lines of communication between testing, other lifecycle activities, and within the 
organization.
This information may be represented visually.
NOTE  A visual representation can include an organization chart or a figure that illustrates the flow of 
information.
7.2.6  Risk r egister
7.2.6.1
 Gener
al
Lists the risks identified as part of the test planning activity. This should include any relevant risks that 
may be specified in the organizational test practices. Provides an exposure level for each risk based on its impact and probability. Provides recommendations to manage the risks. This section may reference a separate risk register.
EXAMPLE  R ecommendations to manage risk can include to eliminate, reduce, monitor or accept a risk.
NOTE
 A r
isk register can be located in a project plan or a risk management plan.ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved13

<!-- PDF page 24 -->

7.2.6.2  Pr oduct risks
Identifies test-related product risks.
EXAMPLE  T est-related product risks can include defects in functionality or in non-functional aspects such 
as performance.
7.2.6.3  Pr oject risks
Identifies test-related project risks.
EXAMPLE  T est-related project risks can include risks related to schedule or resources.
7.2.7  T est strategy
7.2.7.1
 Gener
al
Describes the approach to testing for the identified scope of testing of the test item, as outlined in 7.2.7.2  
to 7.2.7.15 . It may refer to the organizational test practices stating only its differences from it.
7.2.7.2
 T
est levels
For a project test plan or a type test plan, this identifies the levels of testing that will be conducted.
7.2.7.3
 T
est types
For a project test plan or a level test plan, this identifies the types of testing that will be conducted.7.2.7.4
 T
est deliverables
Identifies all test documentation that is to be delivered from the testing activity or equivalent 
information to be recorded electronically, for example in spreadsheets, databases or dedicated test tools.
EXAMPLE  T he following test documentation can be included:
—
 t
est plan;
—
 t
est model specification;
—
 t
est case specification;
—
 t
est procedure specification;
—
 t
est data readiness report;
—
 t
est environment readiness report;
—
 i
ncident reports;
—
 t
est status reports;
— t
est completion report.
Test input data and test output data may be identified as deliverables. Test tools created as part of the testing activity may also be included. If test documentation has been combined or eliminated, then this list will be modified accordingly.
This subsection may include information on when each item of test documentation should be delivered, 
and name the sender and recipient (preferably by position, not name).ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved14

<!-- PDF page 25 -->

7.2.7.5  T est design techniques
Specifies which test design techniques are to be applied.
7.2.7.6
 Entry and e
xit criteria
Specifies when test processes for a given test level or type can start and stop.
Each test level and type consists of the following processes:
—
 t
est planning;
—
 t
est design and implementation;
—
 t
est environment and data management;
—
 t
est execution;
— t
est incident reporting.
Different entry and exit criteria may be defined for each of these test processes, or for selected ones, or 
for the entire test level or type as a whole.
EXAMPLE  E ntry criteria for test execution during system testing can include: the system test plan has been 
created and signed off, test cases have been created and approved by business stakeholders and test data and the test environment (including deployment of the test item) are prepared and ready for execution. Exit criteria for system testing can include: all high priority test cases have been executed and passed testing or that any severity 1 or 2 defects that remain open have been reviewed and approved by the business as not requiring addressing during system testing.
7.2.7.7  T est completion criteria
Specifies when test execution for a given test level, test type or test technique can be considered 
complete. For test types and test levels, the test completion criteria may be the same as the exit criteria for that test level.
EXAMPLE  T he test completion criterion for state transition testing can be that test execution will continue 
until all states in the state model have been "visited" during testing.
7.2.7.8  Degr ee of independence
Establishes the level of independence of those performing the testing. States how the testers are technically, managerially, and financially independent.
7.2.7.9
 Metrics t
o be collected
Describes the metrics for which values are to be collected during the test activities.7.2.7.10
 T
est data requirements
Specifies all relevant test data requirements for the project or test level or test type (as appropriate).
EXAMPLE  T his can identify the origin of the test data and state where specific test data is located, whether 
data has to be sanitised for confidentiality reasons, and the role responsible for the test data.
These test data requirements may be deferred to the test data requirements documentation (see 8.5), 
as applicable.
NOTE  T he data quality model defined in ISO/IEC  2 5012 can support the identification of test data 
requirements (and potentially data measurement needs).ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved15

<!-- PDF page 26 -->

7.2.7.11  T est environment requirements
Specifies the necessary and desired properties of the test environment.
EXAMPLE  1 T his can include hardware, software, test tools, databases, and personnel (identifying their 
organizations, as appropriate).
Information regarding selection, evaluation, acquisition and support for each component part of the 
test environment may be included.
It may include test environment requirements for test preparation, test execution (including data 
capture), and any post-execution activities.
EXAMPLE  2 A p ost-execution activity can be data analysis.
These test environment requirements may be defined in the test environment requirements documentation (see 8.6 ) as applicable, but this should be stated in the test plan.
7.2.7.12
 R
etesting
Specifies the conditions under which retesting will be performed.
7.2.7.13
 R
egression testing
Specifies the conditions under which regression testing will be performed.7.2.7.14
 Suspension and r
esumption criteria
Specifies the criteria used to suspend and resume all or a subset of the testing activities in the test plan. 
Identifies who has the authority to suspend and resume testing activities. Specifies the activities that need to be completed when testing resumes, which may include repeating specific testing activities.
7.2.7.15
 De
viations from the organizational test practices
Records any test plan content that deviates from the organizational test practices and the reasons for 
deviation. Identifies those responsible for approving deviations, where applicable.
7.2.8
 T
esting activities and estimates
Identifies all the necessary testing activities required to implement the test strategy. The approach and 
expected number of iterations for retesting and regression testing as well as any dependencies between activities should be considered.
NOTE  1  T he testing activities can be described in terms of a work breakdown structure or activities on an 
agile activity board.
Describes estimates for each identified testing activity. Additionally, where appropriate, describes the allocated testing budget and cost estimates or references where that information can be found.
NOTE  2 A llocated budget and cost estimates can be located in the project plan.
7.2.9	 Staffing
7.2.9.1
 Gener
al
Describes the staffing requirements for the testing covered by this plan.
NOTE  S taffing can be accomplished by internal transfer, external hiring, consultants, subcontractors, 
business partners, and/or outsourced resources.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved16
