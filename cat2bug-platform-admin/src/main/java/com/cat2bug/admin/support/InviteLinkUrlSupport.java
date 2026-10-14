package com.cat2bug.admin.support;

import jakarta.ws.rs.core.UriInfo;

public final class InviteLinkUrlSupport {

    private InviteLinkUrlSupport() {
    }

    public static String resolveInviteBaseUrl(UriInfo uriInfo) {
        if (uriInfo == null || uriInfo.getBaseUri() == null) {
            return "";
        }
        String base = uriInfo.getBaseUri().toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }
}
