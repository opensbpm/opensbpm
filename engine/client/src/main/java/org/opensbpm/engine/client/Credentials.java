package org.opensbpm.engine.client;

import java.util.Objects;

public interface Credentials {
    static Credentials of(String userName, char[] password) {
        Objects.requireNonNull(userName,"Username must be non null");
        Objects.requireNonNull(password,"Password must be non null");
        return new Credentials() {
            @Override
            public String getUserName() {
                return userName;
            }

            @Override
            public char[] getPassword() {
                return password;
            }
        };
    }

    String getUserName();

    char[] getPassword();
}
