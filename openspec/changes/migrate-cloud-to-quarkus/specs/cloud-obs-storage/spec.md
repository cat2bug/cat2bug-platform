## ADDED Requirements

### Requirement: Cloud profile stores files only in OBS

When the cloud profile is enabled, every user upload, download, and delete MUST use Huawei OBS. The system MUST NOT write those files to the local upload directory. Object keys MUST be the relative path with the profile prefix removed. The URL returned to clients MUST be a URL that fetches the object from the configured bucket.

#### Scenario: Upload returns a bucket URL

- **WHEN** an authenticated user uploads a file under the cloud profile
- **THEN** the object exists in the configured OBS bucket and the response URL opens that object

#### Scenario: Local disk is not the store

- **WHEN** an upload succeeds under the cloud profile
- **THEN** the system does not require the file to exist under the local profile directory

### Requirement: Missing object delete succeeds

Deleting a stored file whose OBS object is already absent MUST succeed so the product record can be removed.

#### Scenario: Delete missing object

- **WHEN** a client deletes a file whose object key is not in the bucket
- **THEN** the delete operation completes successfully

### Requirement: Non-cloud builds keep local storage

When the cloud profile is not enabled, uploads MUST keep using the local upload directory and MUST NOT require OBS credentials.

#### Scenario: Default build without OBS

- **WHEN** the application runs without the cloud profile and a user uploads a file
- **THEN** the file is stored on the local upload path and no OBS client is required
