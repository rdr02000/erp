package com.ai.rag.security;

import java.security.Principal;

public record AuthenticatedUser(long id, String username, String email) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
