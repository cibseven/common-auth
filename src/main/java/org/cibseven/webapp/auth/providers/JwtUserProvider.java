/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.cibseven.webapp.auth.providers;

import java.nio.charset.Charset;
import java.security.Key;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.cibseven.webapp.auth.Login;
import org.cibseven.webapp.auth.User;
import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.TokenExpiredException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

public interface JwtUserProvider<T extends Login> extends UserProvider<T> {
	
	static final String BEARER_PREFIX = "Bearer ";
	static final Charset UTF8 = Charset.forName("UTF-8");	
	
	public static interface TokenSettings {
		String getSecret();	
		Duration getValid();	
		Duration getProlong();
	}
	
	TokenSettings getSettings();
	
	@Override
	default User authenticate(HttpServletRequest rq) {
		return Optional.ofNullable(rq.getHeader("Authorization"))
				.filter(val -> val.startsWith(BEARER_PREFIX)).map(val -> val.substring(BEARER_PREFIX.length()))
				.map(token -> this.parse(token, getSettings()))
				.orElseThrow(() -> new AuthenticationException());
	}
	
	default User parse(String token, TokenSettings settings) {
		try {			
			SecretKey key = createKey(settings.getSecret());
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
			User user = deserialize((String) claims.get("user"), JwtUserProvider.BEARER_PREFIX + token);
			if ((boolean) claims.get("verify") && verify(claims) == null)
				throw new AuthenticationException(token);
			return user;			
		} catch (ExpiredJwtException x) {
			long ageMillis = System.currentTimeMillis() - x.getClaims().getExpiration().getTime();
			if ((boolean) x.getClaims().get("prolongable") && (ageMillis < settings.getProlong().toMillis())) {
				User user = verify(x.getClaims());
				if (user != null)
					throw new TokenExpiredException(createToken(settings, true, false, user));				
			}
			throw new TokenExpiredException();			
		} catch (JwtException x) {
			throw new AuthenticationException(token);
		}
	}	
	
	User deserialize(String json, String token);
	
	/** 
	 * @return null if the user was changed (especially password) after the token was issued or a user if it hasn't changed in the meantime
	 */
	User verify(Claims claims);
	
	default String createToken(TokenSettings settings, boolean prolongable, boolean verify, User user) {
		return BEARER_PREFIX + Jwts.builder()
				.claims()
		    	.subject(user.getId())
	    		.expiration(new Date(System.currentTimeMillis() + settings.getValid().toMillis()))
	    		.issuedAt(new Date())
	    		.add("prolongable", prolongable)
	    		.add("verify", verify)
	    		.add("user", serialize(user))
	    		.and()
	    		.signWith(createKey(settings.getSecret()))
	    		.compact();
	};
	
	default SecretKey createKey(String secret) {
		return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
	}
	
	String serialize(User user);
	
}
