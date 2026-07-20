# Annex I (informative) Test case specification

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `annex-i`
- **PDF pages**: 72–73

---
<!-- PDF page 72 -->

Annex  I 
(inf
ormative) 
 
Test
	
case
	
specification
I.1 Ex ample  1 – Agile Corporation
NOTE  T he test coverage items and test cases are derived from the test model specification in Annex  H. 
T
he coverage method used depends on the risk of the test model items and has been outlined in the test plan. 
The combination table below the classification tree shows the test cases that are formed by covering (using a 
minimized approach) each test coverage item (marked by a black dot).
Test	Case	Specification
I.2 Ex ample  2 – Traditional Ltd
Based on the test model (shown as a state transition model in Annex  H – Test Model #23) the following 
6 transitions can be defined as test coverage items.
 T1 T2 T 3 T4 T5 T6
Starting State (TC) S1 S2 S2 S3 S2 S4
Input P 
‘Lock’P 
‘Open’P 
‘Forward’P 
‘Stop’P 
‘Backward’P 
‘Stop’
Exp. output Lid locked Lid opens Conveyor 
moves for -
wardConveyor 
stopsConveyor 
moves back -
wardConveyor 
stops
End State (TC) S2 S1 S3 S2 S4 S2
The following tables identify the null-transitions, shown in bold italic.
 P ress ‘Lock’ Press ‘Forward’ Press ‘Stop’ Press ‘Backward’ Press ‘Open’
S1 S2 / Lid locked S1 / Null S1 / Null S1 / Null S1 / NullISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved62

<!-- PDF page 73 -->

P ress ‘Lock’ Press ‘Forward’ Press ‘Stop’ Press ‘Backward’ Press ‘Open’
S2 S2 / Null S3 / Conveyor 
moves forwardS2 / Null S4 / Conveyor 
moves backwardS1 / Lid opens
S3 S3 / Null S3 / Null S2 / Conveyor stops S3 / Null S3 / Null
S4 S4 / Null S4 / Null S2 / Conveyor stops S4 / Null S4 / Null
There are 6 test cases required to cover the valid transitions, and 14 to cover the null transitions, 
leading to a total of 20 test cases to be performed, as follows.
Test Case ID: Lid–1
Priority: 1
Test Coverage Item: T1Purpose:  to test the locking of the lid when the conveyor is not moving
Preconditions: The apparatus must be in a stopped state with the lid open.
Inputs: Press the lock button.
Expected results: The lid is locked and the conveyor is still stopped.
 
Test Case ID: Lid–2Priority: 1
Test Coverage Item: T2Purpose:  to test the opening of the lid when the conveyor is not moving
Preconditions: The apparatus must be in a stopped state with the lid locked.
Inputs: Press the open button.
Expected results: The lid is opened and the conveyor is still stopped.
…ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved63
