## ADDED Requirements

### Requirement: Successful receive-pack triggers push and tag webhooks

After a successful Smart HTTP receive-pack that updates one or more refs, the system SHALL enqueue outbound webhook deliveries for active repository webhooks subscribed to push and/or tag events, according to whether the updated refs are branch refs or tag refs (`refs/tags/*`). Delivery failures MUST NOT fail the already-accepted push.

#### Scenario: Branch push notifies push subscribers

- **WHEN** a user with push permission successfully pushes commits to a branch ref
- **THEN** active webhooks subscribed to `push` for that repository are enqueued for delivery with a push-style payload

#### Scenario: Tag update notifies tag subscribers

- **WHEN** a successful receive-pack creates, updates, or deletes a `refs/tags/*` ref
- **THEN** active webhooks subscribed to `tag` for that repository are enqueued for delivery
