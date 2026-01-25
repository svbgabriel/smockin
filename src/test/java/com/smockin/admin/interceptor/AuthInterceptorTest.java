package com.smockin.admin.interceptor;

import com.smockin.admin.service.AuthService;
import com.smockin.admin.service.SmockinUserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock
    private SmockinUserService smockinUserService;

    @Mock
    private AuthService authService;

    @Spy
    @InjectMocks
    private AuthInterceptor authInterceptor = new AuthInterceptor(smockinUserService, authService);


    @ParameterizedTest(name = "[{index}] exclusionKey={0} inboundUrl={1} expected={2}")
    @MethodSource("matchExclusionUrlCases")
    void matchExclusionUrl(final String exclusionKey, final String inboundUrl, final boolean expected) {
        Assertions.assertEquals(expected, authInterceptor.matchExclusionUrl(exclusionKey, inboundUrl));
    }

    private static Stream<Arguments> matchExclusionUrlCases() {
        return Stream.of(
                // exact match
                Arguments.of("/smockin/test/mock", "/smockin/test/mock", true),
                // non-matching exact path
                Arguments.of("/smockin/test/mock", "/smockin/test/mock2", false),
                // wildcard path match (single segment)
                Arguments.of("/smockin/test/mock/*", "/smockin/test/mock/123", true),
                // wildcard path mismatch (different segment)
                Arguments.of("/smockin/test/mock/*", "/smockin/test/mock2/123", false),
                // wildcard path mismatch (different base)
                Arguments.of("/smockin/test/mock/*", "/test/mock/123", false),
                // wildcard path mismatch (key missing base prefix)
                Arguments.of("/test/mock/*", "/smockin/test/mock/123", false),
                // wildcard file extension match
                Arguments.of("*.html", "/smockin/test/mock/file.html", true),
                // wildcard file extension mismatch
                Arguments.of("*.html", "/smockin/test/mock/file.htmlx", false)
        );
    }

}
