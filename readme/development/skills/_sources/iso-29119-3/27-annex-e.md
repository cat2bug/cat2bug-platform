# Annex E (informative) Test plan

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `annex-e`
- **PDF pages**: 56–63

---
<!-- PDF page 56 -->

Annex  E 
(inf
ormative) 
 
Test plan
E.1 Ex ample  1 – Agile Corporation
NOTE  I n this example, this test plan would be available on the agile team wiki, with the latest version posted 
in the top right corner of the story board in the development room.
Test Plan for: New Subscription System (NSS)                                                                                         Iteration:  3
C
ontext, test items & scope: NSS Iteration 3, including all test types and levels of testing for all stories 
in play for the given sprint. All stories are captured in the team’s requirement and task management 
tool.
Team & Communication: Each iteration is carried out by the agile team consisting of a Delivery Lead, 
Developers, Business Analyst, User Representatives, Testers and a Product Owner. The agile team reports to the Delivery Lead, while all Developers also report independently to the Head of Product Development (Stephanie McFarlan) and Testers to the Head of Testing (Shane Dolan)
Stakeholders:  The Product Owner and Delivery Lead are stakeholders for approval purposes. 
Escalation to the Head of Product Development & Head of Testing is only required when consensus 
cannot be achieved within the agile team.
Risks:  The risks for this iteration are listed on relevant story cards. A current general risk is that the 
agile team does not have access to live data in the supporting databases.Test strategy:—
 A
t release and iteration planning, estimate testing and development effort based on average velocity, 
returning to the backlog any user stories that cannot be achieved (done) by the end of the iteration, 
including any accumulated technical debt (defects) that cannot be resolved in the current iteration.
—
 C
reate automated unit & integration tests based on stories before coding starts (i.e. TDD). Test new 
code with system tests, followed by system integration tests with external systems and finally with user acceptance testing (using ATTD) before marking stories as Done. Ensure customers are involved in and ideally conduct user acceptance testing. Automate as many tests as possible.
—
 R
egression test any feature that has changed since previous iterations and the entire system prior 
to the showcase meeting, via the automated test suite.
—
 U
se test design techniques that best suit acceptance criteria, keeping in mind that higher risk stories 
require more thorough testing that lower risk stories.
—
 F
or low risk stories, use minimized combination coverage, for high risk stories use maximized 
combination coverage.
—
 E
nsure that no defects of severity 1 or 2 remain outstanding when a story is marked as Done. 
All remaining defects must be converted to new user stories and placed in the backlog for future iterations.
—
 O
utline test progress at daily stand up meetings including any blockers (e.g. environment issues) 
that are preventing testing from progressing as planned.
—
 S
tore all session sheets and tests in the central test management tool and all automated test scripts 
in the central code repository, so they are available for retesting and regression testing.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved46

<!-- PDF page 57 -->

— I ssue a short email-based summary reporting on the outcomes of testing at the end of each iteration.
—
 E
ntry, exit and completion criteria for testing are built into the Definition of Done.
All other strategy details are as per the Organizational Test Practices & Agile Team Charter.
E.2 Ex ample  2 – Traditional Ltd
NOTE  T his annex provides an example of a project test plan.
Project Test Plan
Traditional Ltd
Project # UV/TRT-14 33a
Document approved, with approval provided by project board
Revision log
Date Version Changes Init.
10.11.2020 0.1 Initial draft released for review AMH
16.11.2020 0.2 Updated following internal test team feedback TPR
17.11.2020 0.3 Updated following stakeholder feedback TPR
20.11.2020 1.0 Approved by stakeholders AMH
30.11.2020 1.1 Updating following environment design change TPR
1   Overview
1-1   
Introduction
This document describes the plan and strategy for testing on the UV/TRT-14 33a project, specifically 
the PC-based aspect of the system.
1-2   
Scope
This document is written at the project level, covering all levels and types of testing in scope for the 
project.
1-3   
References
—
 P
roject Plan (PP) for the UV/TRT-14 33a project
—
 B
usiness Requirements Specification (BRS) for the UV/TRT-14 33a project
—
 S
oftware Requirements Specification for the UV/TRT-14 33a project
—
 E
nterprise Architecture Specification (EAS) for the UV/TRT-14 33a project
—
 O
rganizational Test Practices (OTP) for Traditional Ltd
1-4   
Glossary
The glossary of terms for all documentation related to this project is provided in the team wiki.2
   
Overview / Context of Testing
2-1
   
Projects, Test Types and Test Levels
The UV/TRT product consists of the following hardware modules:—
 U
V SpectrometerISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved47

<!-- PDF page 58 -->

— I R Spectrometer
—
 A
utomatic Burette
— C
onveyor
— C
omputer (server)
—
 P
C / Laptop
The architecture is summarised in the following figure:
The system consists of the following software modules on the computer (the server):—
 U
V module
— I
R module
— B
urette module
— C
onveyor module
— N
etwork module
The system consists of the following software modules on the PC:
— C
alibration module
—
 C
ompound identification and concentration module
— S
etup module
—
 C
ontrol and report module
— N
etwork module
This test plan is applicable to the types and levels of testing listed in section
 
5-1.
2-2
   
Test Items
The test for this project includes testing of:—
 E
ach module of the PC software, as listed in 2-1 above
—
 E
ach component of the PC software modules listed in 2-1 above
—
 T
he functionality of the complete software system
The version of each test item will be obtained and recorded from the configuration management system 
at the time of executing each test.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved48

<!-- PDF page 59 -->

2-3   Test Scope
T
he PC system consists of software modules listed in section
 2
-1. The network module is a commercial-
off-the-shelf product, proven in many other organizations, and does not require standalone testing. All 
the other modules shall be tested under the assumption that the PC's operating system and network are working correctly. Functionality related to the network connection will not be tested, except indirectly when these features are utilised in connection with other tests. Non-functional quality factors such as performance, security and disaster/recovery will be outsourced to another company that will perform this type of testing independently. A separate Test Plan will be created for each of these types of testing by the outsourcing company.
2-4
   
Test Basis
The requirements for the system are captured in the following documents:
—
 B
usiness Requirements Specification (BRS) for the UV/TRT-14 33a project
—
 S
oftware Requirements Specification for the UV/TRT-14 33a project
—
 E
nterprise Architecture Specification (EAS) for the UV/TRT-14 33a project
2-5
   
Assumptions and Constraints
AS-1: it is assumed that all team members will be co-located for efficiency and effectiveness.
2-6   
Stakeholders
Please refer to the stakeholder list in the PP.3
   
Testing Communication
Please refer to PP for a list of teams and individuals involved in test and project communication.4
   
Risk Register
The following abbreviations are used in the risk tables:
—
 P = p
robability or likelihood of the risk
—
 C = C
onsequence, impact or effect if risk materializes
—
 E = E
xposure = Probability x Consequence
The scales for both probability and consequence will be 1 to 6, where 6 is the highest.
4-1
   
Product Risks
Risk ID & Name P C E Mitigation Activities
1 Risk that calibration is incorrect 2 5 10 Design and code inspection
Thorough unit, system & acceptance testingAutomated testing
2 Risk that compound identification is 
incorrect2 6 12 Design and code inspection
Thorough unit, system & acceptance testing
3 Risk that concentration calculation 
is incorrect1 6 6 Design and code inspection
Thorough unit, system & acceptance testing
4 Risk that calculations ‘drift’ if the 
machine is not turned off regularly3 5 15 Code inspection
Dynamic analysis to identify memory leaksEndurance / soak testISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved49

<!-- PDF page 60 -->

Risk ID & Name P C E Mitigation Activities
5 Risk that compound identification is 
too slow3 1 3 Performance test under various conditions
6 Risk that reports are not accurate 2 5 10 Design and code inspection
Thorough unit, system & acceptance test
7 Risk that system crashes in middle 
of analysis, giving unreliable results2 2 4 Disaster/recovery testing that forces system to crash during analysis
8 Risk that user manuals are not un-derstandable by lab technicians4 3 12 Usability assessment & inspection of user manuals
9 Risk that setup is difficult to under -
stand4 2 8 Usability assessment of prototype's user interface
Review of requirements & design specifications
10 Risk of errors in text of localised 
reports, due to translation errors5 3 15 Review all reports in languages known to ensure 
correctness
4-2   Project Risks
Risk ID & Name P C E Mitigation Activities
1 Risk that staff num-bers in specific roles are insufficient2 4 8 Ensure estimates consider staff capability requirements.
Track test progress closely and report any predicted 
budget or schedule issues.
2 Risk that available staff lack sufficient knowledge and experi -
ence2 3 6 Perform gap analysis of needs vs. staff availability.
Prepare a training plan for individual participants.
Include training time in the schedule.Find mentors, if possible.Ensure time is available for reviews by senior engineers.
3 Risk that some of the 
people who are avail-able will not / cannot work together5 2 10 Try to identify what the problem is.
Arrange for arbitration if necessary / practicable.Where necessary, arrange for a Belbin analysis to in -
crease understanding between the various types.Distribute activities so that there is less contact between 
the individuals in question.
4 Risk that there are too few licenses available for test execution tools3 2 6 Prepare business case for additional licences.
Distribute, plan and track test activities in detail, to 
reduce licence waiting times as far as possible.
Report related problems as early as possible.
5 Risk that testing staff 
are unfamiliar with the test management tool4 1 4 Prepare business case for funding tools training.
Find someone from within the company who has experi -
ence of using the tool and share knowledge.
Allow for additional time in initial test design & execu -
tion.Report related problems as early as possible.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved50

<!-- PDF page 61 -->

Risk ID & Name P C E Mitigation Activities
6 Risk that vendors do 
not supply the expected material on time and to the expected quality3 4 12 Ensure contractual requirements for quality and delivery are stated precisely, and if not, follow up with procurement.
Provide templates to vendors wherever possible.Establish specific quality criteria for deliverables.Specify the consequences of breaches of quality and 
scheduled delivery.
Follow progress and quality closely.
7 Risk that teams use 
more than 30
 
% of test 
execution time on inci -
dent reporting, causing schedule slippage4 5 20 Review unit test coverage and adequacy.
Request additional until testing if gaps are identified.
5   Test Strategy
5
-1
   
Test Levels & Types
The test for the PC-part of the UV/TRT-14 33a product shall include the following test levels:
—
 U
nit Testing
— I
ntegration Testing (component level)
—
 S
ystem Testing
—
 S
ystem Integration Testing
—
 U
ser Acceptance Testing
— P
roduction Verification Testing
As stated in section
 2
-3, non-functional testing will be outsourced to a third-party supplier, with details 
to be provided in their Non-Functional Test Plan.
5-2   
Test Deliverables
For each test level and type of testing, the following documentation will be produced:—
 T
est Plan
—
 T
est Model Specification
— T
est Cases, Test Procedures, Actual Results, Test Result, Test Log, Incident Reports and Test Status 
Reports in the organization's test management tool
—
 T
est Completion Report (also referred to as a Test Summary Report)
An overall Project Test Completion Report will also be produced at the end of the project, summarising 
the results of testing across all levels of testing.
5-3
   
Test Design Techniques
This is specified for each test level in the OTP.5-4
   
Entry and Exit Criteria
This is specified for each test level in the OTP.5-5
   
Test Completion CriteriaISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved51

<!-- PDF page 62 -->

As per the OTP:
—
 A
ll areas of high to medium risk will undergo testing that covers 100
 % 
of the requirements and 
risks with the most comprehensive number of test cases available. This includes 3-value Boundary 
Value Analysis and coverage/execution of all potential positive, negative and alternative tests.
—
 A
ny areas of low risk will undergo positive testing, with minimal coverage of negative tests.
5-6   
Degree of Independence
Unit testing will be conducted by Developers. All other levels of testing will be carried out by independent testers.
5-7
   
Metrics to be Collected
This is specified for each test level in the OTP.
5-8   
Test Data Requirements
Sanitized data from the production environment will be provided in the system test environment.5-9
   
Test Environment Requirements
The test environment will be setup to mimic production. Specific test tool requirements are:—
 A
nt
— J
ira
— J
Boss
— X
Ray
—
 S
elenium Web Driver
—
 S
oapUI or Postman (proof-of-concept implementation to determine most suitable tool)
Additional environment details will be specified in a separate Test Environment Requirements 
document.
5-10    
Retesting
This is specified for each test level in the OTP.5-11
   
Regression Testing
This is specified for each test level in the OTP.5-12
   
Suspension and Resumption Criteria
Suspension criteria are listed in the project risk register.
5-13
   
Deviations from Organizational Test Practices
There are no deviations planned from the organizational test practices. Any deviations deemed 
necessary will be communicated to the Delivery Lead and Head of Testing and will be captured in the quality risk register.
6
   
Testing Activities and Estimates
This information is available on the team dashboard: h t t p s ://  mp
ower  .
traditional  .
com/  i
rj/ p
ortal
For information on cost related measures and monthly tracking, refer the following link:
h t t p s ://  p
rocessnet  .m
asked  .
com / p
rojectdashboard/  d
ashboardhome  _
new  .
aspISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved52

<!-- PDF page 63 -->

7			Staffing
7
-1
   
Roles, Activities and Responsibilities
Detailed roles, activities and responsibilities will be documented in Test Plans of each level of testing.
7-2   
Hiring Needs
An experienced system integration tester is required – interviews will commence shortly.7-3
   
Training Needs
Training in the company's test case management tool will be provided for any tester that do not have 
existing experience in the tool, via existing in-house training materials.
8   
Schedule
The test schedule is provided in the Gantt chart for the project, on the team dashboard:h t t p s ://
 mp
ower  .
Traditional  .
com/  i
rj/ p
ortalISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved53
