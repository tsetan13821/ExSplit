package com.tenzin.splitter.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class TokenGeneratorTest {

	private final TokenGenerator generator = new TokenGenerator();

	@Test
	void tokensAre256BitUrlSafeStrings() {
		String token = generator.newToken();
		assertThat(token).hasSize(43).matches("[A-Za-z0-9_-]+");
	}

	@Test
	void tokensDoNotRepeat() {
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 10_000; i++) {
			assertThat(seen.add(generator.newToken())).isTrue();
		}
	}

	@Test
	void sha256MatchesKnownVector() {
		assertThat(TokenGenerator.sha256("abc"))
			.isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
	}

	@Test
	void hashIsDeterministicAndDiffersFromToken() {
		String token = generator.newToken();
		assertThat(TokenGenerator.sha256(token)).isEqualTo(TokenGenerator.sha256(token)).isNotEqualTo(token);
	}

}
