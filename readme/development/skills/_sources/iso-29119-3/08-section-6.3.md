# 6.3 Organizational test practices

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `section-6.3`
- **PDF pages**: 18–21

---
<!-- PDF page 18 -->

6.2.3  T est process
Identifies the test process that the organization will follow. This may include reference to a specific 
document providing details of the test process.
EXAMPLE  S uch a document can be a test process in ISO/IEC/IEEE  2 9119-2. The details of activities in the 
test process can be described in more detailed test process documentation.
6.2.4  T est organization structure
Identifies the roles and structure of the test organization. A diagram to show test organization hierarchy 
may be used, or the information may be presented in a table.
6.2.5
 T
ester training
States required training and certifications for individuals performing testing in the organization.6.2.6
 T
ester ethics
Identifies the organizational ethics code to be upheld by the testers.6.2.7
 Standar
ds
States which standards are applicable to the testing performed within the organization.6.2.8
 Other r
elevant policies
Identifies policies that impact the testing performed within the organization.
EXAMPLE  A p olicy statement can be that testing will conform to the quality policy.
6.2.9  Measuring the v alue of testing
States how the organization determines the return on investment of testing. Identifies the objectives 
for measuring the value of testing.
6.2.10
 T
est asset archiving and reuse
States the organization’s position on the archiving and reuse of test assets.6.2.11
 T
est process improvement
States the method for ensuring continuous improvement of the test process.
6.3 Or ganizational test practices
6.3.1  Ov erview
The organizational test practices is technical test documentation that provides guidelines on how 
testing should be carried out within the organization, i.e. how to achieve the objectives stated in the test policy, where one exists.
The organizational test practices provides guidelines to projects within its scope; it is not project 
specific.
For small or highly homogenous organizations, a single set of organizational test practices 
documentation may cover all testing activities. An organization may have more than one set of organizational test practices documentation if the organization performs testing in a number of significantly different ways, such as for both safety-critical products and non-safety-critical products; ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved8

<!-- PDF page 19 -->

or if it is using both agile and V-model development models; or if its programmes are large enough to 
merit their own documented test practices.
The organizational test practices may incorporate the content of the test policy where no separate test 
policy is available.
The practices are defined for the specified scope – such as at the level of a programme, project or for a 
specific level or type of testing.
The organizational test practices includes identification of test levels expected to be used and types 
and practice statements for each of these. It may be partitioned with a subsection for each identified test level and test type if the test level or test type practice statements differ significantly between each test level or type; this is illustrated in Figure
 
2.
The contents of the organizational test practices are defined in 6.3.2  and 6.3.3 . A.3 provides an outline 
of the organizational test practices documentation, while Annex  
D provides examples that demonstrate 
how organizational test practices can be developed for two example organizations.
Figure  2 — An example structure of the organizational test practices document
6.3.2
 Or
ganization-level test practice statements
6.3.2.1
 Gener
al
This section includes statements that are apply to all of the test levels and test types to be performed in projects run within the organization (or that part of the organization that is within the scope of the organizational test practices documentation). This section may include subsections from the test policy, if needed.
6.3.2.2
 Risk management appr
oach
Identifies the generic approach to risk management expected to be used to direct the testing activities.
EXAMPLE  T his can be to identify project and product risks that can be mitigated or reduced by testing.ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved9

<!-- PDF page 20 -->

6.3.2.3  T est selection and prioritization
Describes the approach to selecting and prioritizing test execution, in the form of prioritized test 
procedures. Test procedures consist of prioritized test cases, derived from prioritized test coverage items.
6.3.2.4
 T
est documentation and reporting
Identifies the test documentation expected to be produced during testing by each project being run 
within the organization. Describes when each documentation type is prepared and the associated approval process. This is tightly connected to the test process that is specified in the test policy.
6.3.2.5
 T
est automation and tools
Describes the approach to test automation. Identifies the test tools to be used throughout testing.
EXAMPLE  T his can include test management tools, test execution tools, performance test tools, security test 
tools and usability test tools.
6.3.2.6	 Configuration 	management 	of	test	work	products
D
escribes the configuration management to be performed for the work products from testing; describes 
how these work products are to be identified, traced, stored, and made available to stakeholders.6.3.2.7
 Incident management
D
escribes how incidents should be managed during testing or refers to a description elsewhere.
6.3.2.8
 T
est levels
Identifies specific test levels to be performed as part of the testing within the scope of the practices.6.3.2.9
 T
est types
Identifies specific types of testing to be performed as part of testing within the scope of the practices.6.3.2.10
 R
ules/guidelines for deviation from organizational test practices
Defines rules or guidelines for determining the conditions under which deviation from organizational 
test practices is possible/allowed within the organization.
EXAMPLE  A n organization can decide that individual projects or teams cannot change the defect severity 
and priority levels that are set out in the organizational test practices.
6.3.3	 Test 	level/type-specific 	organizational 	test	practice	statements
6.3.3.1
 Entry and e
xit criteria
Criteria that specify when test processes for a given test level or type can start and stop.
Each test level and type consists of the following test processes:—
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
est execution;ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved10

<!-- PDF page 21 -->

— t est incident reporting.
Different entry and exit criteria may be defined for each of these test processes, or for selected ones, or 
for the entire test level or type as a whole.
EXAMPLE  E ntry criteria for test execution during system testing can include: the system test plan has been 
created and signed off, test cases have been created and approved by business stakeholders and test data and the test environment (including deployment of the test item) are prepared and ready for execution. Exit criteria for system testing can include: all high priority test cases have been executed and passed testing or that any severity 1 or 2 defects that remain open have been reviewed and approved by the business as not requiring addressing during system testing.
6.3.3.2  T est completion criteria
Criteria that specify when test execution for a given test level, test type or test technique can be 
considered complete. For test types and test levels, the test completion criteria may be the same as the exit criteria for that test level.
EXAMPLE  T he test completion criterion for state transition testing can be that test execution will continue 
until all states in the state model have been "visited" during testing.
6.3.3.3  T est documentation and reporting
Identifies the test documentation, including reporting, used for testing activities in the test level or 
type. Describes when each is prepared and the associated approval process. This is tightly connected to the test process specified in the test policy.
6.3.3.4
 Degr
ee of independence
Establishes the level of independence of those performing the testing. States how testers or groups of 
testers are technically, managerially, and financially independent.
6.3.3.5
 T
est design techniques
Identifies specific test design techniques to be used during test design and implementation within the 
test level or type.
6.3.3.6
 T
est data
Identifies the test data required for the test level or type. This may include a statement of the origin of 
test data, where particular types of test data are located and groups or organizations responsible for creating and maintaining test data.
NOTE  T he data quality model defined in ISO/IEC  2 5012 can support the identification of test data 
requirements (and potentially data measurement needs).
6.3.3.7  T est environment
Identifies the test environment for the test level or type. This may include where specific types of test 
should be performed and may identify groups or organizations responsible for the test environment.
6.3.3.8
 Metrics t
o be collected
Describes the metrics for which values are to be collected during the test activities in the test level or 
type.
6.3.3.9
 R
etesting
Identifies the practices, conditions and activities for retesting in each test level or type.ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved11
