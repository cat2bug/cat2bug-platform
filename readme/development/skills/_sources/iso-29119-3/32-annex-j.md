# Annex J (informative) Test procedure specification

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `annex-j`
- **PDF pages**: 74–75

---
<!-- PDF page 74 -->

Annex  J 
(inf
ormative) 
 
Test
	
procedure
	
specification
J.1 Ex ample  1 – Agile Corporation
The following was agreed with Agile Corp’s customer and team for creating new subscriptions. The 
team also has the option of producing documentation online with the same information. It is designed to be lightweight, covering the test case, test procedure, actual results and test result and is independent of all other session sheets.
Session Sheet ID: 9 Priority: 1
Tester Name Michael Tester Backlog Item # BI_201
Sprint # 16 Date 11/12/2020
Session Duration 1
 
hour, 30
 
minutes Build # 15.5.8
Charter Test create magazine subscription process from perspective (persona) of new buyer / subscriber. All fields & objects on screen are in scope and should be fully operational.
Start-up steps: Log in as a new buyer/subscriber
Test Ideas & Exe -
cution Results1)
 C
reate new subscriptions with combinations of:
i)
 A
ll subscription types – PASSED
ii)
 A
ll subscription lengths – PASSED
iii)
 N
o discount, full discount – FAILED (see Defects row)
iv)
 M
ultiple subscriptions with combination of each type, length and discount 
included – PASSED
2)
 S
tart creating subscription and then cancel out of process – PASSED
3)
 E
nter variety of positive & negative boundary values & equivalence class 
values in all input fields – PASSED
4)
 C
lick buttons repeatedly to check performance/reliability – NOT YET 
EXECUTED – deferred to next exploratory test session
5)
 I
nspect look & feel against company design guidelines – NOT YET EXECUTED – 
deferred to next exploratory test session
Defects Defect details recorded as comments on Backlog Item BI_279
J.2 Ex ample  2 – Traditional Ltd
Test	procedure	specification
Test Proc. ID Objective and Priority Est. Duration
TP-3 The purpose of this test procedure is to test the way the system handles the defined measuring ranges for NCS.
                                                  
Priority: 220 minutesISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved64

<!-- PDF page 75 -->

Test Proc. ID Objective and Priority Est. Duration
Start up:  Set apparatus ready for sampling analysis. Place NCS samples with following values in carousel:
1) Value of 1 2) Value of 2 3) Value of 56 4) Value of 315 5) Value of 316
Relationships to other procedures: NoneTest LogDate: Initials: Test item: OK / Not OK
    
C
omments:
ProcedureTest ID Activities Examination of Result Actual Results Test Result
1 Start the sampling analy -
sis. Wait for first sample to 
be analysed.Check that the display shows “Invalid sample”
  
2 W
ait for second sample to 
be analysed.Check that the sample is analysed   
3 W
ait for the sample to be 
analysed.Check that the sample is analysed   
4 W
ait for fourth sample to 
be analysed.Check that the sample is analysed   
5 W
ait for fifth sample to be 
analysed.Check that the display shows “Invalid sample”   
S
top and wrap up: Turn off the apparatus, remove the samples, and clean up any spillage.
…ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved65
