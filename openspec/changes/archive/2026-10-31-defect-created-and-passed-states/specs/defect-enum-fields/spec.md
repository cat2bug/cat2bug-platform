## MODIFIED Requirements

### Requirement: Defect JSON exposes enum names

Every defect object returned by the web API SHALL include `defectTypeName` (one of `BUG`, `TASK`, `DEMAND`) and `defectStateName` (one of `PROCESSING`, `AUDIT`, `RESOLVED`, `REJECTED`, `CLOSED`, `CREATED`). `defectType` SHALL be serialized as the type name and `defectState` SHALL be serialized as its numeric code（`PROCESSING`=0 … `CLOSED`=4，`CREATED`=5）, matching the platform enum table. `RESOLVED` SHALL remain a first-class state name（产品文案「通过」）。

#### Scenario: Defect list returns names

- **WHEN** a client requests `GET /system/defect/list` for a project containing a defect of type TASK in state AUDIT
- **THEN** the row contains `defectType: "TASK"`, `defectTypeName: "TASK"`, `defectState: 1`, `defectStateName: "AUDIT"`

#### Scenario: Defect detail returns names

- **WHEN** a client requests `GET /system/defect/{defectId}`
- **THEN** the response data contains `defectTypeName` and `defectStateName` consistent with the stored codes

#### Scenario: Created state code is five

- **WHEN** a client requests a defect in state CREATED
- **THEN** the row contains `defectState: 5` and `defectStateName: "CREATED"`

### Requirement: Defect type and state accept names or codes on input

Request bodies that create or update a defect SHALL accept `defectType` and `defectState` either as an enum name (case-insensitive) or as the numeric code（含 `CREATED` / `5`）. An unknown name or an out-of-range code SHALL be rejected with a client error, and the defect SHALL NOT be written. Name fields (`defectTypeName`, `defectStateName`) sent back by the client SHALL be ignored.

#### Scenario: Create with type name

- **WHEN** the web client posts `/system/defect` with `defectType: "TASK"`
- **THEN** the defect is created and stored with type code 1

#### Scenario: Create with numeric type

- **WHEN** a client posts `/system/defect` with `defectType: 2`
- **THEN** the defect is created with type DEMAND

#### Scenario: Unknown type rejected

- **WHEN** a client posts `/system/defect` with `defectType: "FEATURE"`
- **THEN** the request fails with a client error and no defect is created

#### Scenario: Round-trip update

- **WHEN** a client sends back a defect object it received (including `defectTypeName`, `defectStateName`) to `PUT /system/defect`
- **THEN** the update succeeds and the type and state are unchanged

#### Scenario: Accept created by name

- **WHEN** a client posts or updates with `defectState: "CREATED"` or `defectState: 5`
- **THEN** the value is accepted as state CREATED when otherwise valid
