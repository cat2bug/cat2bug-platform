package com.cat2bug.quarkus.admin.util;

import jakarta.ws.rs.core.UriInfo;

public final class InviteLinkUrlSupport {

    private InviteLinkUrlSupport() {
    }

    public static String resolveInviteBaseUrl(UriInfo uriInfo) {
        if (uriInfo == null || uriInfo.getRequestUri() == null) {
            return null;
        }
        return uriInfo.getRequestUri().getScheme() + "://" + uriInfo.getRequestUri().getAuthority();
    }
}
