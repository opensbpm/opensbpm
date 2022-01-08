/** *****************************************************************************
 * Copyright (C) 2020 Stefan Sedelmaier
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
 * ****************************************************************************
 */
package org.opensbpm.webui.backend.authentication;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.opensbpm.engine.api.UserTokenService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class UserTokenRegisterer {

    private static final Logger LOGGER = Logger.getLogger(UserTokenRegisterer.class.getName());

    private final UserTokenService userTokenService;

    public UserTokenRegisterer(UserTokenService userTokenService) {
        this.userTokenService = Objects.requireNonNull(userTokenService, "userTokenService must be non null");
    }

    @EventListener
    public void successfulAuthentication(AuthenticationSuccessEvent successEvent) {
        LOGGER.log(Level.INFO, "registering User ''{0}''", successEvent.getAuthentication().getName());
        userTokenService.registerUser(SpringAuthentication.of(successEvent.getAuthentication()));
    }

}
