# 8.2 Test model specification

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `section-8.2`
- **PDF pages**: 30–30

---
<!-- PDF page 30 -->

NOTE  1 T hese can be separate documents, can appear as chapters in a test specification, can be in the form of 
charters, or can be electronic records in a test tool, depending on the size and nature of the test project.
— t est data requirements;
—
 t
est environment requirements;
—
 t
est data readiness report;
—
 t
est environment readiness report;
—
 t
est execution documentation, divided into:
—
 a
ctual results;
—
 t
est results;
—
 t
est execution log;
— i
ncident report.
The full templates with explanatory text for each of these items of test documentation are found in 
8.2 to 8.11 . Annex  A p
rovides an abbreviated overview of each item of test documentation along with 
the requirements, recommendations and permissions (shall, should and may) for each section of the test documentation. Annexes
 H t
o R provide examples of the dynamic test process documentation for 
example projects.
NOTE  2 T here are many test documentation styles and names, e.g. in agile, session sheets and charters with 
test ideas are often used. It is expected that when tailoring is required (see 4.1.3 ), other test documentation 
names or headings can be substituted for the names used in this document. A mapping can be produced as a part of that tailoring process. In Annexes
 B t
o R, examples for two different project types are presented with 
options for tailoring of test documentation names. The annexes are not inclusive of all test documentation names, formats, or test methodologies, but are intended to demonstrate some possible options.
8.2	 Test	model	specification
8.2.1  Ov erview
A test model is a representation of an aspect of the test item, which is focused on the attributes that 
define required test coverage. The test model and the required test coverage are used to derive the test coverage items.
A test model will either already exist (e.g. source code or state transition diagram used for development) 
or can be documented in a test model specification.
A.3 provides an outline of the test model specification, while Annex
 H 
provides examples that 
demonstrate how test models can be described for two different example projects.
EXAMPLE  E xamples of test models are equivalence classes, a set of identified boundaries on the edges of 
each equivalence class, a decision table, a state transition diagram, source code, and a control flow graph.
The contents for a test model specification are specified in 8.2.2  to 8.2.7 .
8.2.2	 Unique
	
identifier
Describes the unique identifier for the test model so that it can be distinguished from all other test 
models. An automated tool may control the generation of the identifiers, or it may be done manually according to an appropriate notation scheme. The unique identifier is not usually changed during the lifetime of the test model, because it is used for traceability purposes.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved20
