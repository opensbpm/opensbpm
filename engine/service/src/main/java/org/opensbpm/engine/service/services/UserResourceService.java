/** *****************************************************************************
 * Copyright (C) 2024 Stefan Sedelmaier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************
 */
package org.opensbpm.engine.service.services;

import org.opensbpm.engine.api.UserTokenService;
import org.opensbpm.engine.api.UserTokenService.TokenRequest;
import org.opensbpm.engine.api.instance.UserToken;
import org.opensbpm.engine.api.instance.UserTokenRequest;
import java.util.logging.Logger;

import org.opensbpm.engine.server.api.UserResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.Set;
import org.opensbpm.engine.service.authentication.SpringAuthentication;

@Component
public class UserResourceService implements UserResource {

    private static final Logger LOGGER = Logger.getLogger(UserResourceService.class.getName());

    @Autowired
    private UserTokenService userTokenService;

    @Override
    public synchronized UserToken info() {
        TokenRequest tokenRequest = SpringAuthentication.of(SecurityContextHolder.getContext().getAuthentication());
        return userTokenService.registerUser(tokenRequest);
    }

    @Override
    public UserToken registerUser(UserTokenRequest tokenRequest) {
        return userTokenService.registerUser(new TokenRequest() {
            @Override
            public String getUsername() {
                return tokenRequest.getUsername();
            }

            @Override
            public Set<String> getRoles() {
                return tokenRequest.getRoles();
            }
        });
    }

}
