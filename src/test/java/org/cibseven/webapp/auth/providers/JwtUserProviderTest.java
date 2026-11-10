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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.ServiceLoader;

import javax.crypto.SecretKey;

import org.cibseven.webapp.auth.User;
import org.cibseven.webapp.auth.exception.AuthenticationException;
import org.cibseven.webapp.auth.exception.TokenExpiredException;
import org.cibseven.webapp.auth.rest.StandardLogin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.io.Serializer;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Covers the default JWT handling of {@link JwtUserProvider}: the claims {@code createToken} writes and the claims
 * {@code parse} reads back ({@code sub}, {@code exp}, {@code iat}, the {@code user} string and the {@code prolongable}
 * and {@code verify} booleans), through whatever JSON implementation jjwt finds on the classpath.
 */
class JwtUserProviderTest {

	/** Base64 of the 32 bytes "common-auth-test-secret-32-bytes". */
	static final String SECRET = "Y29tbW9uLWF1dGgtdGVzdC1zZWNyZXQtMzItYnl0ZXM=";
	/** Base64 of the 32 bytes "another-secret-of-exactly-32-byt". */
	static final String OTHER_SECRET = "YW5vdGhlci1zZWNyZXQtb2YtZXhhY3RseS0zMi1ieXQ=";

	/**
	 * A display name that JSON writers have to escape (quote, backslash, control character, line/paragraph
	 * separator) or may escape (HTML characters, non-ASCII, a supplementary character).
	 */
	static final String DISPLAY_NAME = "J\u00f6rg \"JJ\" O'Brien <admin> & co \\ / \t \u2028 \u2029 \u20ac \ud83d\ude00 = ?";

	static final TestUser USER = new TestUser("jdoe", DISPLAY_NAME);

	TestProvider provider;

	@BeforeEach
	void setUp() {
		provider = new TestProvider(new Settings(SECRET, Duration.ofMinutes(10), Duration.ofHours(1)));
	}

	@Nested
	class CreateToken {

		@Test
		void returnsABearerTokenSignedWithTheSecret() {
			String token = provider.createToken(provider.getSettings(), true, false, USER);

			assertTrue(token.startsWith("Bearer "), token);
			assertNotNull(claims(SECRET, bare(token)));
			assertThrows(io.jsonwebtoken.security.SignatureException.class, () -> claims(OTHER_SECRET, bare(token)));
		}

		@Test
		void writesTheClaimsThatParseReads() {
			Claims claims = claims(SECRET, bare(provider.createToken(provider.getSettings(), true, false, USER)));

			assertEquals("jdoe", claims.getSubject());
			assertEquals(provider.serialize(USER), claims.get("user"));
			assertEquals(Boolean.TRUE, claims.get("prolongable"));
			assertEquals(Boolean.FALSE, claims.get("verify"));
			// jjwt hands out the NumericDates as Long seconds whatever number type the JSON implementation reads
			assertEquals(claims.getExpiration().getTime() / 1000, claims.get("exp"));
			assertEquals(claims.getIssuedAt().getTime() / 1000, claims.get("iat"));
			assertEquals(claims.getExpiration(), claims.get("exp", Date.class));
			assertEquals(claims.getIssuedAt(), claims.get("iat", Date.class));
		}

		@Test
		void writesTheFlagsAsBooleans() {
			Claims claims = claims(SECRET, bare(provider.createToken(provider.getSettings(), false, true, USER)));

			assertInstanceOf(Boolean.class, claims.get("prolongable"));
			assertInstanceOf(Boolean.class, claims.get("verify"));
			assertEquals(Boolean.FALSE, claims.get("prolongable"));
			assertEquals(Boolean.TRUE, claims.get("verify"));
		}

		@Test
		void expiresAfterTheValidityOfTheSettings() {
			long before = System.currentTimeMillis();
			Claims claims = claims(SECRET, bare(provider.createToken(provider.getSettings(), true, false, USER)));
			long after = System.currentTimeMillis();

			// NumericDate claims have a resolution of one second
			long validMillis = Duration.ofMinutes(10).toMillis();
			assertBetween(before - 1000, claims.getIssuedAt().getTime(), after);
			assertBetween(before + validMillis - 1000, claims.getExpiration().getTime(), after + validMillis);
		}
	}

	@Nested
	class Parse {

		@Test
		void restoresTheUserFromTheUserClaim() {
			String token = bare(provider.createToken(provider.getSettings(), true, false, USER));

			TestUser user = (TestUser) provider.parse(token, provider.getSettings());

			assertEquals(provider.serialize(USER), user.json);
			assertEquals("Bearer " + token, user.getAuthToken());
			assertEquals(0, provider.verifyCalls);
		}

		@Test
		void keepsEveryCharacterOfTheUserClaim() {
			TestUser user = new TestUser("\u00fcser@example.org", DISPLAY_NAME + "\u0000\u001f\u007f\"\\");
			String token = bare(provider.createToken(provider.getSettings(), true, false, user));

			assertEquals(provider.serialize(user), ((TestUser) provider.parse(token, provider.getSettings())).json);
		}

		@Test
		void verifiesTheUser_whenTheTokenAsksForIt() {
			String token = bare(provider.createToken(provider.getSettings(), false, true, USER));

			assertNotNull(provider.parse(token, provider.getSettings()));

			assertEquals(1, provider.verifyCalls);
			Claims claims = provider.verifiedClaims;
			assertEquals("jdoe", claims.getSubject());
			assertEquals(provider.serialize(USER), claims.get("user"));
			assertEquals(Boolean.FALSE, claims.get("prolongable"));
			assertEquals(Boolean.TRUE, claims.get("verify"));
			assertNotNull(claims.getIssuedAt());
			assertNotNull(claims.getExpiration());
		}

		@Test
		void rejectsTheToken_whenTheUserCannotBeVerified() {
			String token = bare(provider.createToken(provider.getSettings(), false, true, USER));
			provider.verifyResult = null;

			AuthenticationException x = assertThrows(AuthenticationException.class,
					() -> provider.parse(token, provider.getSettings()));

			assertEquals(AuthenticationException.class, x.getClass());
			assertArrayEquals(new Object[] { token }, x.getData());
		}

		@Test
		void rejectsATokenSignedWithAnotherSecret() {
			String token = bare(provider.createToken(new Settings(OTHER_SECRET, Duration.ofMinutes(10),
					Duration.ofHours(1)), true, false, USER));

			AuthenticationException x = assertThrows(AuthenticationException.class,
					() -> provider.parse(token, provider.getSettings()));

			assertEquals(AuthenticationException.class, x.getClass());
			assertArrayEquals(new Object[] { token }, x.getData());
		}

		@Test
		void rejectsATokenThatIsNoJwt() {
			AuthenticationException x = assertThrows(AuthenticationException.class,
					() -> provider.parse("no.jwt.at-all", provider.getSettings()));

			assertEquals(AuthenticationException.class, x.getClass());
		}

		@Test
		void rejectsAnUnsignedToken() {
			String token = Jwts.builder().subject("jdoe").claim("user", provider.serialize(USER))
					.claim("prolongable", true).claim("verify", false).compact();

			AuthenticationException x = assertThrows(AuthenticationException.class,
					() -> provider.parse(token, provider.getSettings()));

			assertEquals(AuthenticationException.class, x.getClass());
		}

		/** The header is read before the signature is checked, so whatever is in it must end as a 401. */
		@ParameterizedTest
		@ValueSource(strings = { "", "null", "\"HS256\"", "[\"HS256\"]", "{\"alg\":\"HS256\"", "{\"alg\":\"HS256\"} {}",
				"{alg:'HS256'}", "{\"alg\":\"none\"}", "{\"alg\":42}", "{\"alg\":\"HS256\",\"crit\":[\"exp\"]}",
				// duplicate keys: jjwt's Gson backend takes the last value, its Jackson backend rejects them
				"{\"alg\":\"HS256\",\"alg\":\"none\"}", "{\"alg\":\"none\",\"alg\":\"HS256\"}",
				"{\"alg\":\"HS256\",\"alg\":\"HS256\"}" })
		void rejectsATokenWithAMalformedHeader(String header) {
			String token = base64Url(header) + "." + base64Url("{\"sub\":\"jdoe\"}") + "." + base64Url("signature");

			AuthenticationException x = assertThrows(AuthenticationException.class,
					() -> provider.parse(token, provider.getSettings()));

			assertEquals(AuthenticationException.class, x.getClass());
		}

		@Test
		void rejectsATokenWithADeeplyNestedHeader() {
			int depth = 50_000;
			String header = "{\"alg\":\"HS256\",\"x\":" + "[".repeat(depth) + "]".repeat(depth) + "}";
			String token = base64Url(header) + "." + base64Url("{\"sub\":\"jdoe\"}") + "." + base64Url("signature");

			AuthenticationException x = assertThrows(AuthenticationException.class,
					() -> provider.parse(token, provider.getSettings()));

			assertEquals(AuthenticationException.class, x.getClass());
		}
	}

	@Nested
	class ParseExpiredToken {

		/** Issues tokens that expired a minute ago. */
		final Settings expired = new Settings(SECRET, Duration.ofMinutes(-1), Duration.ofHours(1));

		@Test
		void returnsAProlongedToken_whenTheTokenIsProlongableAndTheUserIsStillValid() {
			String token = bare(provider.createToken(expired, true, false, USER));

			TokenExpiredException x = assertThrows(TokenExpiredException.class, () -> provider.parse(token, expired));

			assertEquals(1, x.getData().length);
			String prolonged = (String) x.getData()[0];
			assertTrue(prolonged.startsWith("Bearer "), prolonged);
			assertEquals(1, provider.verifyCalls);
			assertEquals("jdoe", provider.verifiedClaims.getSubject());
			assertEquals(provider.serialize(USER), provider.verifiedClaims.get("user"));
			assertNotNull(provider.verifiedClaims.getIssuedAt());

			// The prolonged token is issued with the same settings (so it has expired, too) as prolongable, not verify
			Claims claims = Jwts.parser().verifyWith(key(SECRET)).clock(() -> new Date(0)).build()
					.parseSignedClaims(bare(prolonged)).getPayload();
			assertEquals("jdoe", claims.getSubject());
			assertEquals(provider.serialize(USER), claims.get("user"));
			assertEquals(Boolean.TRUE, claims.get("prolongable"));
			assertEquals(Boolean.FALSE, claims.get("verify"));
		}

		@Test
		void returnsNoToken_whenTheUserIsNotValidAnymore() {
			String token = bare(provider.createToken(expired, true, false, USER));
			provider.verifyResult = null;

			TokenExpiredException x = assertThrows(TokenExpiredException.class, () -> provider.parse(token, expired));

			assertEquals(0, x.getData().length);
			assertEquals(1, provider.verifyCalls);
		}

		@Test
		void returnsNoToken_whenTheTokenIsNotProlongable() {
			String token = bare(provider.createToken(expired, false, false, USER));

			TokenExpiredException x = assertThrows(TokenExpiredException.class, () -> provider.parse(token, expired));

			assertEquals(0, x.getData().length);
			assertEquals(0, provider.verifyCalls);
		}

		@Test
		void returnsNoToken_whenTheProlongPeriodIsOver() {
			Settings noProlong = new Settings(SECRET, Duration.ofMinutes(-1), Duration.ofSeconds(30));
			String token = bare(provider.createToken(noProlong, true, false, USER));

			TokenExpiredException x = assertThrows(TokenExpiredException.class, () -> provider.parse(token, noProlong));

			assertEquals(0, x.getData().length);
			assertEquals(0, provider.verifyCalls);
		}
	}

	/**
	 * Tokens issued by common-auth 1.4.0, whose jjwt wrote the JSON with Jackson (jjwt-jackson 0.12.6). Users keep
	 * such tokens across an update of the application, so they must still be accepted and prolonged.
	 */
	@Nested
	class TokensIssuedByCommonAuth14 {

		/** {@code createToken(settings, true, true, USER)}, valid until 2100-01-01T00:00:00Z. */
		static final String VALID_VERIFY = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqZG9lIiwiZXhwIjo0MTAyNDQ0ODAwLCJpYXQiOjE3OTQwNTUxMjYsInByb2xvbmdhYmxlIjp0cnVlLCJ2ZXJpZnkiOnRydWUsInVzZXIiOiJ7XCJpZFwiOlwiamRvZVwiLFwiZGlzcGxheU5hbWVcIjpcIkrDtnJnIFxcXCJKSlxcXCIgTydCcmllbiA8YWRtaW4-ICYgY28gXFxcXCAvIFxcdTAwMDkg4oCoIOKAqSDigqwgXHVEODNEXHVERTAwID0gP1wifSJ9.aAOHjOMDCTNlri64PzD1wWels6vazSCh_AV0A6kEjtY";
		/** {@code createToken(settings, true, false, USER)}, expired on 2026-11-07T12:37:46Z. */
		static final String EXPIRED_PROLONGABLE = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqZG9lIiwiZXhwIjoxNzk0MDU1MDY2LCJpYXQiOjE3OTQwNTUxMjYsInByb2xvbmdhYmxlIjp0cnVlLCJ2ZXJpZnkiOmZhbHNlLCJ1c2VyIjoie1wiaWRcIjpcImpkb2VcIixcImRpc3BsYXlOYW1lXCI6XCJKw7ZyZyBcXFwiSkpcXFwiIE8nQnJpZW4gPGFkbWluPiAmIGNvIFxcXFwgLyBcXHUwMDA5IOKAqCDigKkg4oKsIFx1RDgzRFx1REUwMCA9ID9cIn0ifQ.32UBat0tDHTg1r0L34VXeBm-UslogYxehVvHJpWtyRY";

		final Date issuedAt = Date.from(Instant.parse("2026-11-07T12:38:46Z"));

		@Test
		void areAcceptedAndVerified() {
			TestUser user = (TestUser) provider.parse(VALID_VERIFY, provider.getSettings());

			assertEquals(provider.serialize(USER), user.json);
			assertEquals("Bearer " + VALID_VERIFY, user.getAuthToken());
			assertEquals(1, provider.verifyCalls);
			Claims claims = provider.verifiedClaims;
			assertEquals("jdoe", claims.getSubject());
			assertEquals(issuedAt, claims.getIssuedAt());
			assertEquals(Date.from(Instant.parse("2100-01-01T00:00:00Z")), claims.getExpiration());
			assertEquals(Boolean.TRUE, claims.get("prolongable"));
			assertEquals(Boolean.TRUE, claims.get("verify"));
		}

		@Test
		void areProlonged() {
			Settings settings = new Settings(SECRET, Duration.ofMinutes(10), Duration.ofDays(365L * 100));

			TokenExpiredException x = assertThrows(TokenExpiredException.class,
					() -> provider.parse(EXPIRED_PROLONGABLE, settings));

			assertEquals(1, x.getData().length);
			assertEquals(1, provider.verifyCalls);
			Claims expired = provider.verifiedClaims;
			assertEquals("jdoe", expired.getSubject());
			assertEquals(issuedAt, expired.getIssuedAt());
			assertEquals(Date.from(Instant.parse("2026-11-07T12:37:46Z")), expired.getExpiration());
			assertEquals(provider.serialize(USER), expired.get("user"));

			String prolonged = (String) x.getData()[0];
			TestUser user = (TestUser) provider.parse(bare(prolonged), settings);
			assertEquals(provider.serialize(USER), user.json);
			assertEquals(prolonged, user.getAuthToken());
		}
	}

	@Nested
	class Authenticate {

		@Test
		void parsesTheBearerTokenOfTheAuthorizationHeader() {
			String token = provider.createToken(provider.getSettings(), true, false, USER);

			TestUser user = (TestUser) provider.authenticate(request(token));

			assertEquals(provider.serialize(USER), user.json);
			assertEquals(token, user.getAuthToken());
		}

		@Test
		void rejectsARequestWithoutAuthorizationHeader() {
			AuthenticationException x = assertThrows(AuthenticationException.class,
					() -> provider.authenticate(request(null)));

			assertTrue(x.isNoAuth());
		}

		@Test
		void rejectsARequestWithAnotherAuthorizationScheme() {
			AuthenticationException x = assertThrows(AuthenticationException.class,
					() -> provider.authenticate(request("Basic amRvZTpzZWNyZXQ=")));

			assertTrue(x.isNoAuth());
		}
	}

	/**
	 * jjwt takes the first JSON implementation the ServiceLoader finds. There must be exactly one, Gson: jjwt-jackson
	 * would bring Jackson 2 back to the applications.
	 */
	@Nested
	class JsonImplementation {

		@Test
		void isGson() {
			assertEquals(List.of("io.jsonwebtoken.gson.io.GsonSerializer"), implementations(Serializer.class));
			assertEquals(List.of("io.jsonwebtoken.gson.io.GsonDeserializer"), implementations(Deserializer.class));
		}

		List<String> implementations(Class<?> service) {
			List<String> names = new ArrayList<>();
			ServiceLoader.load(service).stream().forEach(provider -> names.add(provider.type().getName()));
			return names;
		}
	}

	// ---- helpers

	static String bare(String token) {
		return token.substring(JwtUserProvider.BEARER_PREFIX.length());
	}

	static String base64Url(String value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	static SecretKey key(String secret) {
		return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
	}

	static Claims claims(String secret, String token) {
		return Jwts.parser().verifyWith(key(secret)).build().parseSignedClaims(token).getPayload();
	}

	static void assertBetween(long min, long actual, long max) {
		assertTrue(min <= actual && actual <= max, actual + " is not in [" + min + ", " + max + "] ("
				+ Instant.ofEpochMilli(actual) + ")");
	}

	static HttpServletRequest request(String authorization) {
		return (HttpServletRequest) Proxy.newProxyInstance(JwtUserProviderTest.class.getClassLoader(),
				new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> {
					if (method.getName().equals("getHeader"))
						return "Authorization".equals(args[0]) ? authorization : null;
					throw new UnsupportedOperationException(method.getName());
				});
	}

	static final class Settings implements JwtUserProvider.TokenSettings {

		final String secret;
		final Duration valid;
		final Duration prolong;

		Settings(String secret, Duration valid, Duration prolong) {
			this.secret = secret;
			this.valid = valid;
			this.prolong = prolong;
		}

		@Override
		public String getSecret() {
			return secret;
		}

		@Override
		public Duration getValid() {
			return valid;
		}

		@Override
		public Duration getProlong() {
			return prolong;
		}
	}

	static final class TestUser implements User {

		final String id;
		final String displayName;
		final String authToken;
		/** The {@code user} claim this user was deserialized from. */
		final String json;

		TestUser(String id, String displayName) {
			this(id, displayName, null, null);
		}

		TestUser(String id, String displayName, String authToken, String json) {
			this.id = id;
			this.displayName = displayName;
			this.authToken = authToken;
			this.json = json;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public String getDisplayName() {
			return displayName;
		}

		@Override
		public String getAuthToken() {
			return authToken;
		}
	}

	static final class TestProvider implements JwtUserProvider<StandardLogin> {

		final TokenSettings settings;
		/** What {@link #verify(Claims)} returns. */
		User verifyResult = USER;
		int verifyCalls;
		Claims verifiedClaims;

		TestProvider(TokenSettings settings) {
			this.settings = settings;
		}

		@Override
		public TokenSettings getSettings() {
			return settings;
		}

		/** Writes the user as JSON, the way the CIB seven webclient does (a JSON document in a string claim). */
		@Override
		public String serialize(User user) {
			return "{\"id\":\"" + escape(user.getId()) + "\",\"displayName\":\"" + escape(user.getDisplayName()) + "\"}";
		}

		@Override
		public User deserialize(String json, String token) {
			return new TestUser(null, null, token, json);
		}

		@Override
		public User verify(Claims claims) {
			verifyCalls++;
			verifiedClaims = claims;
			return verifyResult;
		}

		@Override
		public User login(StandardLogin params, HttpServletRequest rq) {
			throw new UnsupportedOperationException();
		}

		@Override
		public User getUserInfo(User user, String userId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void logout(User user) {
			throw new UnsupportedOperationException();
		}

		static String escape(String value) {
			StringBuilder sb = new StringBuilder();
			for (char c : value.toCharArray()) {
				if (c == '"' || c == '\\')
					sb.append('\\').append(c);
				else if (c < 0x20)
					sb.append(String.format("\\u%04x", (int) c));
				else
					sb.append(c);
			}
			return sb.toString();
		}
	}
}
