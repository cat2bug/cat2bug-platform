# Annex H (informative) Test model specification

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `annex-h`
- **PDF pages**: 70–71

---
<!-- PDF page 70 -->

Annex  H 
(inf
ormative) 
 
Test
	
model
	
specification
H.1 Ex ample  1 – Agile Corporation
NOTE  F or Agile Corporation's new subscription system, they have created a high-level test model to show 
the different subscription options and actions for the users. Using this model (and keeping it up to date with each 
iteration) will allow the team to derive test cases for each new piece of functionality to be implemented as well as for regression testing. This test model specification is available on the product portal and the newest version is also posted in the top right corner of the story board under the test plan in the development room.
Test	Model	Specification
H.2 Ex ample  2 – Traditional Ltd
Test	Model	Specification
T
est Model:  #23 – Lid Operation Model (maps to Requirement ID 12)
                                            Pr
iority:  1
Objective:  One of the variants of the product shall be equipped with a lid to protect the technicians 
performing the analyses.
Type of Testing Required (from Test Strategy): State Transition Testing (manual & automated)Test Model:  The lid covers the carousel when it is moving. The lid has to be locked before the carousel 
is started and it is not possible to open it before the carousel has stopped completely. Two sensors are in 
place to detect if the lid is locked and if the carousel is moving.
As long as the lid is locked it is possible to start the carousel moving either forwards or backwards. To 
change the direction, it is necessary to stop the carousel first, but it is not necessary to open the lid.
The manoeuvring panel has the following buttons that can be pressed:—
 L
ock
— O
pen
— F
orward
— B
ackward
— S
topISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved60

<!-- PDF page 71 -->

The lid operation test model can be illustrated in the following state-machine diagram, where the states 
and transitions are numbered, and the event “P” means “Press”:ISO/IEC/IEEE 29119-3:2021(E)
© ISO/IEC 2021 – All rights reserved   
© IEEE 2021 – All rights reserved61
