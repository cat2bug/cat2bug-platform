# Annex G (informative) Test completion report

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `annex-g`
- **PDF pages**: 68–69

---
<!-- PDF page 68 -->

Annex  G 
(inf
ormative) 
 
Test completion report
G.1 Ex ample  1 – Agile Corporation
NOTE  T his report is available on the team wiki at the end of each release.
Release Test Report for: New Subscription System (NSS) Version:  Iteration 12
Date:  March 12, 2021
Covers:  Release 1, which is planned for release to production in release train cycle 4.
Risks:  All open risks have been accepted by the product owner. Residual risks related to performance 
are not expected to impact on go-live as the product will be released to a reduced user base, until 
proven stable in production. Should additional performance testing be needed, it will be conducted in Release 2.
Test Results:  the product owner has accepted this release of the product based on the following:
—
 A
ll 16 in-scope user stories passed system testing, system integration testing and user acceptance 
testing
—
 1
00
 % 
statement coverage was achieved for 15 high-risk user stories, and 80
 % 
for the remaining 
story
—
 A
ll remaining defects were severity 3 and 4, with the Product Owner confirming that they have very 
little impact on end-users for product features that will be in use during the trial period. All defects 
have been closed and raised as new user stories, to be addressed in the first iteration of the next release
—
 A
ll showcases during the release were accepted by the Product Owner and all users present, with 
no outstanding issues
—
 A
ccessibility requirements (WCAG 2.1 AA) were met for all features implemented during the release
Notes for future work from retrospective:
—
 T
he delivery team feels an additional tester new be required to speed up accessibility testing during 
the next release by ensuring that accessibility testing is carried out in every iteration.
—
 S
everity 3 and 4 defects that have moved into the backlog as new user stories must be addressed in 
next release
—
 O
bfuscated production data that was used during the second half of release 1 accelerated 
development and testing. This approach should be continued in all remaining releases.
—
 I
n-sprint test automation and exploratory testing accelerating well, but additional test design 
techniques should be considered, e.g. combinatorial testing, to ensure robust data coverage.
G.2 Ex ample  2 – Traditional Ltd
Project PC-part of the UV/TRT-14 33a productISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved58

<!-- PDF page 69 -->

System Test Completion Report, V 1.0, 29/11/2019
Written by:  Carlo Titlefsen (Test Manager)
Approved by:  Benedicte Rytter (Project Manager)
Summary of testing performed:
—
 T
he test specification was produced; it included 600 test procedures.
—
 T
he test environment was established according to the plan.
—
 T
est execution and recording were performed according to the plan.
—
 B
ased on the outcomes of testing, it is recommended that UAT commence.
Deviations from planned testing:  The Requirement Specification was updated during the test to v5.6. 
This entailed rewriting of a few test cases, but this did not have an impact on the schedule.
Test completion evaluation:  All test procedures were executed and passed testing, except 3. There 
are no remaining severity 1 or 2 defects, and all open severity 3 and 4 defects have been signed off by 
the business. The remaining 3 failed test cases will be rerun during UAT, once remaining severity 3 and 4 defects have been fixed. The requirements for these 3 failed tests are low risk.
Factors that blocked progress:  Deployment was delayed on 5 occasions, preventing testing from 
starting on time. This equated to an increase in testing duration by 25 person days.Test measures:Three test procedures out of 605 were not passed due to defects. All test procedures that were run had 
passed at the end of the planned 3 weeks of testing.
During test execution, 83 defects were detected and 80 were closed.Duration of test design and execution:
—
 1
2,080 working hours were spent on the production of test cases and test procedures
—
 1
0 working hours were spent on the establishment of the test environment
—
 1
5,040 working hours were spent on test execution and defect reporting
—
 O
ne hour was spent on the production of this report.
Residual risks:  All the risks listed in the test plan have been eliminated, except the one with the lowest 
exposure, Risk # 19. This risk has been deemed acceptable by the Business Representative.
Test deliverables:  All deliverables specified in the plan have been delivered to the common CM-system 
according to the procedure.Reusable test assets:  The test specification and the related test data and test environment 
requirements can be reused for maintenance testing, if and when this is needed.Lessons learned: The test executors should have been given a general introduction to the system; it 
sometimes delayed them finding out how to perform a test case, but fortunately the test analyst was available throughout the test period.ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved59
