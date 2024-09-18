/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensbpm;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
public class ResourceServerController {

	@CrossOrigin(origins = "http://localhost:3000")
	@GetMapping("/")
	public String index(@AuthenticationPrincipal Jwt jwt) {
		String authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.joining(","));

		SecurityContextHolder.getContext().getAuthentication().getAuthorities();
		return jwt.getClaimAsString("preferred_username")+" "+ authorities;
	}

	@CrossOrigin(origins = "http://localhost:3000")
	@GetMapping("/protected/premium")
	public String premium(@AuthenticationPrincipal Jwt jwt) {
		return String.format("Hello, %s!", jwt.getClaimAsString("preferred_username"));
	}
}
