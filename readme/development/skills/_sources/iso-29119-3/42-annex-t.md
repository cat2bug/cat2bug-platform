# Annex T (informative) Test models

- **Standard**: ISO/IEC/IEEE 29119-3:2021
- **Slice id**: `annex-t`
- **PDF pages**: 92–92

---
<!-- PDF page 92 -->

Annex  T 
(inf
ormative) 
 
Test models
In the previous edition of ISO/IEC/IEEE  2 9119-2, the Test Design and Implementation Process included 
the use of test conditions. Feedback on use of the previous edition highlighted a problem with users’ 
understanding of test conditions and their use for deriving test cases – many users described them as ‘confusing’. An alternative approach based on test models was chosen instead.
Test models have been successfully used to define each of the test case design techniques in ISO/IEC/
IEEE
 2
9119-4 and new simpler examples have been generated for each of the techniques using test 
models. An example of using test models for test design and implementation is provided in Annexes  
H, 
I and J .
The use of test models allows the introduction of a simplified test design process, where test coverage items are more easily identified. This simplified test design process is comprised of four activities rather than the previous six, where each activity can be seen to contribute to the overall process. In the previous edition of ISO/IEC/IEEE
 2
9119-2, for some test case design techniques, the two activities 
of deriving test conditions and deriving test coverage items resulted in the same output, suggesting that one of the two activities was superfluous. By using a test model (instead of test conditions), there is no one-to-one mapping between the test model and test coverage items, which means each activity is performing a valid transformation.
The difference between a test condition and a test model can be challenging to grasp. A test condition is 
a somewhat more abstract concept that is used at various levels of detail to describe anything that may be used as the basis for testing. At a high level, a test condition can be used to agree what user features are to be covered by tests with a customer or, at a low level, a test condition can describe a line of code that will be executed by a test by a developer. In contrast, the test model is more focused, describing the behaviour of that part of the test item that is being tested in terms of required test coverage. The test model can cover the same contents as one or more test conditions (and will typically replace several) when test conditions relate closely to test coverage items. The test model is created to provide a means of representing the behaviour of a test item that explicitly shows the test coverage items to be covered by tests, whereas test conditions are often less directly linked to test coverage items. Another obvious difference is that when tests are created to satisfy a test objective, a single cohesive test model can normally be used to create many tests (often all the tests for a given test design technique), while to create those same tests, many, less cohesive, test conditions would typically have to be identified.ISO/IEC/IEEE 29119-3:2021(E)
   © I SO/IEC 2021 – All rights reserved
 © IEEE 2021 – All rights reserved82
