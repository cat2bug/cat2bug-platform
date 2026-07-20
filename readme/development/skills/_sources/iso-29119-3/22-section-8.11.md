# 8.11 Test incident report

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `section-8.11`
- **PDF pages**: 39–41

---
<!-- PDF page 39 -->

— a f ailure, which makes further execution of a test impossible;
—
 a d
isruption to the test environment that causes the actual results to be unreliable.
A.3 provides an outline of the test execution log, while Annex  Q provides examples that demonstrate 
how the test execution log can be described for two different example projects.
The contents of the test execution log are specified in 8.10.2  to 8.10.5 , which apply to each individual 
event recorded in the log.
8.10.2	 Unique
	
identifier
Defines a unique identifier for each event in the test execution log.8.10.3
 Dat
e/time
Defines the precise time and date at which each event was encountered.8.10.4
 Description
D
escribes what happened during each event. This may include a reference to the test procedure and 
test case being executed when the event was encountered, if relevant.8.10.5
 Impact
D
escribes the impact on test execution and the actual result, if relevant.
8.11  T est incident report
8.11.1  Ov erview
A test incident is any issue that is noticed during testing that requires investigation, such as when an 
actual result deviates from the expected result of a test or when the system behaves differently to how a tester expected. Test incidents are recorded in test incident reports. There will be one incident report for each unique incident (incident reports may also be known as defect reports, bug reports, fault reports, etc.).
The incident reports may be documented in lists or in tables in a document or using a tool, e.g. a 
database or a dedicated bug-tracking tool.
The format of an incident report may be defined elsewhere in an organization, for example as part of 
the Incident Management Processes, in which case that definition should be used.
An incident report in this context documents an incident recognized during testing.
NOTE  I ncidents can occur, and be reported, in other contexts, e.g., ambiguities in a business requirements 
specification discovered during software design, or a software failure occurring during production.
The test incident reporting template that is described in this clause only incudes those fields that are 
needed when the incident report is first raised. More information can be added to the incident report as it passes through the wider incident management process.
EXAMPLE  A dditional information that can be added to the test incident report as it moves through the 
broader incident management process includes the following: the cause of the incident, information on what was changed in the test item to fix the defect, the results of retesting the defect after fix, the root cause of the defect and the person that the defect is allocated to at any given time.
A.3 provides an outline of the test incident report, while Annex  R provides examples that demonstrate 
how a test incident report can be described for two different example projects.
The contents of the test incident report are specified in 8.11.2  to 8.11.9 .ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved29

<!-- PDF page 40 -->

8.11.2  Timing information
R
ecords the date and possibly also the time when the incident was observed.
8.11.3
 Originat
or
Specifies the name and title of the individual who reported the incident.
8.11.4
 C
ontext
Describes the context in which the incident was observed.
EXAMPLE  T his can include:
—
 t
est item the incident occurred in (i.e. name of the system or product being tested), including its version 
number;
—
 t
he test procedure and test case, including their unique identifiers, being executed when the incident was 
observed;
—
 a
ny relevant information about the test environment or test data not included in other documents and 
considered significant by the tester;
—
 t
he test level/type in which the incident was observed.
8.11.5  Description of the incident
P
rovides a detailed description of the incident. Indicates if the incident is reproducible, and, if so, 
provides enough information to reproduce it.
Related information and observations that can help to isolate (and potentially help with correction) of 
the cause of the incident may be included.
The description may also reference where additional evidence or supporting information can be found 
to aid in diagnosis of the incident.
EXAMPLE  S uch evidence can include screenshots, system logs and output files.
It may also identify the existence of any known workarounds.
8.11.6
 Originat
or’s assessment of severity
Indicates, from the originator’s point of view, the impact the incident can have from both technical and 
business perspectives. This may include an estimate of the time and effort to fix the associated defect.
EXAMPLE  1 T echnical impact can include the user’s ability to perform tasks and operate the system, or the 
tester's ability to continue testing the system after the incident occurred.
EXAMPLE
 
2
 B
usiness impact can include consideration of how the organization's reputation can be negatively 
impacted by the incident, if it were to occur in the live production system.
8.11.7  Originat or’s assessment of priority
Provides an evaluation of the urgency for the repair. Most organizations have from three to five 
categories.
EXAMPLE  A categorization scheme can be that the most serious category, e.g. “Fix now”, means that the 
product is unusable, and the least serious, e.g. “Fix in support”, is a cosmetic incident.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved30

<!-- PDF page 41 -->

8.11.8  Risk
P
rovides information on the introduction of new risks or changes to the status of existing risks, where 
applicable.
8.11.9
 Status of the incident
I
dentifies the current status of the incident.
NOTE  A common sequence for incidents as they progress through their life cycles can be: “Open”, “Approved 
for resolution”, “Assigned for resolution”, “Fixed”, “Retested with fix confirmed” and “Closed”. Other possible 
status values can be “Rejected” or “Withdrawn”.ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved31
