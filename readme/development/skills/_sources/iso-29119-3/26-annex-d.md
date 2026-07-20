# Annex D (informative) Organizational test practices

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `annex-d`
- **PDF pages**: 51–55

---
<!-- PDF page 51 -->

Annex  D 
(inf
ormative) 
 
Organizational test practices
D.1 Ex ample  1 – Agile Corporation
Organizational Test Practices – Agile Corporation
Scope:  The Organizational Test Practices are applicable to all testing conducted at Agile Corporation.
Risk management:  All agile teams must conduct a product & project risk assessment based on the 
Testing-Related Risk Management Process (TRM56) defined on the team wiki. Risk assessments are 
conducted during iteration zero and revisited during subsequent iteration planning meetings.
Test selection & prioritization: All test selection and prioritization will be based on the iteration risk 
assessment, which will be carried out during each iteration planning session.
Test automation, tools, defect/incident management & test documentation: Tests are automated 
whenever possible via Agile Corporation's central automation framework, with scripts stored in the central source control system. Exploratory test sessions are captured via the approved recording tool, with execution evidence stored in the central test management tool. Manual tests (when required) are also stored in the central test management tool. All manual and automated tests must be traced to user stories. Defects are stored in the task/requirement management system, either as defects linked to user stories, or as comments on user stories, using the standard defect and story management practices and standard templates that have been configured into the tools. All agile teams must prepare a one-page Release Test Plan, Iteration Test Plans (whiteboard-based), Test Cases for high risk features, and an end of release one-page Test Summary Report.
Reporting:  Test progress is reported verbally at morning stand-up. Due to client requirements, a point-
in-time capture of test status is captured weekly on the team wiki, indicating numbers of tests passing, 
failing and blocked per sprint. In teams that are recording defects on separate defect cards, defect counts per severity level are also reported weekly on the wiki. Test and defect status are available real-time on the team’s dashboards. Due to client requirements, all teams will be required to produce one-page Test Summary Reports.
Configuration
	m
anagement
	o
f
	t
est
	a
ssets: The organization's requirement management, test 
management and test automation tools have in-built configuration management for all assets stored 
within each tool. All document-based assets will be stored on the team wiki, with manual version control applied to ensure each unique document version is captured and backed up.
Test levels, types & techniques: Types of testing that prove user stories meet their acceptance criteria 
and that reduce risks will be chosen during iteration zero and subsequent iteration planning sessions. Levels of testing typically applied include unit, integration, story/system, system integration, acceptance and production verification testing. Types of testing typically applied are functional, performance, accessibility, penetration and disaster/recovery testing. Entry criteria, exit criteria and test completion criteria are specified in the Definition of Done. Test techniques from ISO/IEC/IEEE
 2
9119-4 are tailored 
to ensure testing remains lean.
Guidelines for deviation from Organizational Test Practices: Each team is required to use the same 
templates for requirement/story management, defect management and test management within the organization's suite of tools. Test documentation templates (i.e. for written documents) can be tailored as required by each delivery team.ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved41

<!-- PDF page 52 -->

Degree of independence:  Each Agile team is assigned a tester that reports to the Delivery Lead. 
However, testers also have the ability to report any risks or issues to the Head of Testing (HoT), 
providing independence to all testing within the organization, as the HoT works in parallel to the Head of Development and reports directly to the Chief Technology Officer. The test organization is technically, managerially and financially independent from the development organization of Agile Corp, while within each agile team, assigned testers participate directly in self-organizing teams that report to their Delivery Lead.
Test environment & data: Developers conduct all unit and integration testing in the development 
environment. All testers conduct story/system and system integration testing in the system test environment. Users conduct acceptance testing in the pre-production environment. Performance, accessibility, penetration and disaster/recovery testing is also conducted in the pre-production environment. Production verification and penetration testing are conducted in the production environment. Obfuscated production data will be utilized during testing, ensuring all uniquely identifiable private data is removed or masked prior to testing.
Metrics:—
 T
est progress is measured according to team velocity, which is assessed at the end of each iteration
—
 T
est automation coverage is measured by the percentage of acceptance criteria (from user stories) 
covered by automation tests, and is reported at the end of each iteration
From a product quality improvement perspective, the number of defects per severity level that slip 
through to production is assessed monthly, to identify opportunities to improve iteration and release testing processes.
Retesting and regression testing:  Tests are automated wherever possible, reducing regression testing 
overheads. For all detected defects, an automated or manual regression test must be created.
D.2 Ex ample  2 – Traditional Ltd
Organizational Test Practices – Traditional Ltd
Item Practices
Risk management ap -
proachAll projects must conduct a product & project risk assessment based on the organ -
izational Testing-Related Risk Management Process (TRM2019_04), which meets 
the requirements of ISO
 
31000 and utilizes quality criteria from ISO/IEC
 
25010 
and ISO/IEC
 
25012. Risk assessments must be conducted at the start of each pro -
ject, ideally while requirements are still being written, and revisited regularly (e.g. weekly) throughout each project. Any deviations from the specified risk manage -
ment process must be approved by the Head of Testing.
Test selection and prioriti -
zationTest cases and test procedures will be prioritized according to the risk associated with the requirements the cases are covering. If a test procedure includes test cases with different risk levels, the test case with the highest level determines the risk level for the entire procedure.
Execution of test procedures must always be scheduled according to the risk, so 
that the higher the risk level the sooner the procedure is scheduled to be executed. Care must however be taken so that all feature sets are covered by some testing, that is no feature set must be left out of the execution schedule.
Test documentation and reportingThe test projects must be documented in such a way that an audit can establish what has been planned and what has been performed. Tracing between artefacts is essential. Test documentation requirements specified in ISO/IEC/IEEE
 
29119-3 
must be produced throughout all projects.
Test automation and tools The organization's central test management tool is to be used on all test projects and for all test levels and types. In the cases where more than 4 cycles of regres-sion testing are planned, the project might consider automating testing.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved42

<!-- PDF page 53 -->

Item Practices
Configuration manage -
ment of test work productsAll test documentation must be stored in the central knowledge repository for 
each project (which is backed up and version controlled). All manual and automat -
ed test cases and execution results must be stored in the central test management tool. All automated test scripts, including for functional regression testing and performance testing, must be stored in the central source code control system.
Incident management The company's Incident Management Process (IM2019_01) must be followed on all projects. This includes the mandatory requirement for documenting all defects or issues detected in each system.
Test levels All projects must include the following test levels: 
—
 S
tatic Testing: Requirement, Design & Code Inspections
—
 U
nit Testing
—
 I
ntegration Testing
—
 S
ystem Testing
—
 S
ystem Integration Testing
—
 U
ser Acceptance Testing
—
 P
roduction Verification Testing
Test types All projects must consider applying the following types of testing, depending on 
the requirements and risks involved in each project: 
— P
erformance Testing
—
 D
isaster/Recovery Testing
— P
enetration Testing
 
 All other types of non-functional testing must be considered based on the unique requirements and risks involved in each project.
Guidelines for deviation from Organizational Test PracticesDeviations from the Organizational Test Practices will only be permitted with the express written approval of the Head of Testing and the Test Manager of the given project or program of work.ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved43

<!-- PDF page 54 -->

Item Practices
Entry criteria Entry Criteria for test execution: 
—
 S
tatic Testing: Requirement, Design & Code Inspections
—
 A
rtefact being inspected has been drafted
—
 U
nit & Integration Testing
—
 D
esign & code have been implemented for a given feature
—
 S
ystem Testing
—
 U
nit & Integration Testing are complete for the given feature
—
 T
est cases are designed, traced to requirements & signed off by 
stakeholders
—
 S
ystem Integration Testing
—
 I
nterface has been specified including functions & parameters
—
 I
nternal or external system interface is available (either as real interface 
or stub)
—
 T
est cases are designed, traced to requirements & signed off by 
stakeholders
—
 U
ser Acceptance Testing
—
 A
ll previous stages of testing are complete and passed testing
—
 T
est cases are designed, traced to requirements & signed off by 
stakeholders
—
 P
roduction Verification Testing
—
 A
ll previous stages of testing are complete and passed testing
—
 T
est cases are designed, traced to requirements & signed off by 
stakeholders
—
 P
erformance, Disaster/Recovery & Penetration Testing
—
 R
equirements for each type of testing have been specified and signed off 
by stakeholders
—
 T
est cases are designed, traced to requirements & signed off by 
stakeholders
—
 P
roduction-like environment is available and ready for testing
—
 R
equired test data has been generated and is available
 
 For all test levels, it is recommended that a Project Test Plan (at a minimum) be signed off by the Project Manager, Test Manager and Head of Testing, prior to com -
mencing each type of testing.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved44

<!-- PDF page 55 -->

Item Practices
Exit criteria Exit Criteria for test execution: 
—
 S
tatic Testing: Requirement, Design & Code Inspections
—
 A
ll
—
 A
ll Test Levels
—
 A
ll in scope test cases have either been executed and passed testing, or 
any remaining tests that have not yet passed testing have been signed off 
by stakeholders
—
 A
ll severity 1 and 2 defects have been resolved and have passed 
retesting, and/or any remaining defects have been signed off by stakeholders
Test completion criteria All projects must aim to achieve 100
 
% requirements coverage, with all remaining 
test cases being signed off by stakeholders.
All severity 1 and 2 defects are resolved and have passed retesting and/or all 
outstanding defects have been signed off by stakeholders, with a plan & schedule in place and agreed to address all outstanding defects.
Test documentation & reportingISO/IEC/IEEE
 
29119-3 requirements must be met for all types of dynamic testing. 
ISO/IEC  
20246 requirements must be met for static testing.
Degree of independence All testers will report to their assigned Project Manager, and also to the Head of Testing to ensure independence throughout testing.
Test design techniques Specification-Based and Structure-Based testing techniques defined in ISO/IEC/
IEEE
 
29119-4 must be implemented as required to cover requirements and to mit -
igate risks. Any tailoring of the techniques defined in the standard must be signed off by the Head of Testing.
Test dat a Test data will be extracted from production but must be obfuscated (i.e. an -
onymized) to ensure customer privacy is maintained throughout development and testing.
Test environment The test environment must be as representative as possible to the production environment in terms of hardware and software. In the case of embedded systems, the system test can be executed on a simulator.
Metrics to be collected The following metrics shall be collected and reported throughout testing: 
—
 P
lanned vs. actual number of test cases & test procedures designed and 
executed, per day
—
 D
efect leakage – percentage of defects found in a test level that escaped from 
a previous test level
—
 D
efects density in production – the number of defects escaped from 
production normalized by project size (e.g. 0.4 defects per function point)
—
 D
efect age
—
 R
eturn on investment for automated testing (e.g. cost of manual regression 
testing vs. cost of automated regression testing)
Retesting All test cases and procedures that detect defects must be rerun after defect cor-
rection to confirm that defects are fixed and have passed testing.
Regression testing Regression testing during the system test level is at the Test Manager’s discretion, based on the risks present at that time.
In the final execution cycle of System Testing and User Acceptance Testing, all test 
cases and procedures must be executed.ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved45
