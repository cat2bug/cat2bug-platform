## REMOVED Requirements

### Requirement: Users manage their own personal access tokens

**Reason**: AI clients connect only through OAuth login (account and password); users no longer create, copy, or store tokens themselves.
**Migration**: The profile "access tokens" page and `/system/user/access-token` API are removed, and all existing `c2b_pat_` tokens are deleted by migration. Configure the MCP URL in the client and log in through the browser when prompted.

### Requirement: Tokens are stored as hashes

**Reason**: Personal access tokens are removed; hash-only storage of OAuth tokens is kept under `oauth-authorization-server`.
**Migration**: None.

### Requirement: Tokens authenticate only on MCP endpoints

**Reason**: Personal access tokens are removed; the rule that OAuth access tokens work only on MCP endpoints moves to `oauth-authorization-server`.
**Migration**: None.

### Requirement: Invalid tokens are rejected uniformly

**Reason**: Personal access tokens are removed; uniform 401 handling of OAuth access tokens moves to `oauth-authorization-server`.
**Migration**: None.

### Requirement: Token usage is recorded

**Reason**: Personal access tokens are removed; usage recording of OAuth access tokens moves to `oauth-authorization-server`.
**Migration**: None.
