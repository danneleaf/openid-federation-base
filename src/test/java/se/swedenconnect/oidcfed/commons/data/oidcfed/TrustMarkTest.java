package se.swedenconnect.oidcfed.commons.data.oidcfed;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.oidcfed.commons.testdata.TestCredentials;
import se.swedenconnect.oidcfed.commons.utils.OidcUtils;

/**
 * Trust Mark tests
 */
@Slf4j
class TrustMarkTest {

  @Test
  void builderTest() throws Exception {

    TrustMark trustMark = TrustMark.builder()
      .id("http://example.com/trust_mark_id")
      .issuer("http://example.com/trust_mark_issuer")
      .subject("http://example.com/trust_mark_subject")
      .issueTime(new Date())
      .expriationTime(Date.from(Instant.now().plus(Duration.ofDays(30))))
      .logoUri("http://example.com/logo")
      .ref("http://example.com/information")
      .claim("organization_name", "Trust Mark issuer organization")
      .claim("organization_nme#sv", "Utfärdare av tillitsmärke AB")
      .delegation(TrustMarkDelegation.builder()
        .issuer("https://example.com/trust_mark_owner")
        .subject("http://example.com/trust_mark_issuer")
        .id("http://example.com/trust_mark_id")
        .issueTime(Date.from(Instant.now().minus(Duration.ofDays(10))))
        .expriationTime(Date.from(Instant.now().plus(Duration.ofDays(30))))
        .build(TestCredentials.p256JwtCredential, null).getSignedJWT())
      .build(TestCredentials.p256JwtCredential, null);

    SignedJWT trustMarkSignedJWT = trustMark.getSignedJWT();
    Map<String, Object> headerJsonObject = trustMarkSignedJWT.getHeader().toJSONObject();
    String headerJson = OidcUtils.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(headerJsonObject);
    log.info("Trust Mark header:\n{}", headerJson);

    JWTClaimsSet claimsSet = trustMarkSignedJWT.getJWTClaimsSet();
    String payloadJson = OidcUtils.OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
      .writeValueAsString(claimsSet.toJSONObject());

    log.info("Trust Mark payload:\n{}", payloadJson);

    JSONAssert.assertEquals("{\n"
      + "  \"sub\" : \"http://example.com/trust_mark_subject\",\n"
      + "  \"ref\" : \"http://example.com/information\",\n"
      + "  \"logo_uri\" : \"http://example.com/logo\",\n"
      + "  \"iss\" : \"http://example.com/trust_mark_issuer\",\n"
      + "  \"id\" : \"http://example.com/trust_mark_id\",\n"
      + "  \"organization_name\" : \"Trust Mark issuer organization\",\n"
      + "  \"organization_nme#sv\" : \"Utfärdare av tillitsmärke AB\"\n"
      + "}", payloadJson, false);

    assertTrue(claimsSet.getIssueTime().toInstant().minusMillis(1).isBefore(Instant.now()));
    assertTrue(claimsSet.getIssueTime().toInstant().plusSeconds(5).isAfter(Instant.now()));
    assertTrue(Instant.now().plusSeconds(29).isBefore(claimsSet.getExpirationTime().toInstant()));
    assertTrue(Instant.now().plus(Duration.ofDays(31)).isAfter(claimsSet.getExpirationTime().toInstant()));
    assertNotNull(claimsSet.getJWTID());
    assertTrue(claimsSet.getJWTID().length() > 30);
    assertEquals("trust-mark+jwt", trustMarkSignedJWT.getHeader().getType().getType());

    SignedJWT parsedDelegation = SignedJWT.parse((String) claimsSet.getClaim("delegation"));
    Map<String, Object> delegationHeaderJsonObject = parsedDelegation.getHeader().toJSONObject();
    String delegationHeaderJson = OidcUtils.OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(delegationHeaderJsonObject);
    log.info("Trust Mark Delegation header:\n{}", delegationHeaderJson);

    JWTClaimsSet delegationClaimsSet = parsedDelegation.getJWTClaimsSet();
    String delegationPayloadJson = OidcUtils.OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
      .writeValueAsString(delegationClaimsSet.toJSONObject());

    log.info("Trust Mark Delegation payload:\n{}", delegationPayloadJson);

    JSONAssert.assertEquals("{\n"
      + "  \"sub\" : \"http://example.com/trust_mark_issuer\",\n"
      + "  \"iss\" : \"https://example.com/trust_mark_owner\",\n"
      + "  \"id\" : \"http://example.com/trust_mark_id\"\n"
      + "}\n", delegationPayloadJson, false);

    assertTrue(delegationClaimsSet.getIssueTime().toInstant().minusMillis(1).isBefore(Instant.now()));
    assertTrue(Instant.now().minus(Duration.ofDays(11)).isBefore(delegationClaimsSet.getIssueTime().toInstant()));
    assertTrue(Instant.now().plusSeconds(29).isBefore(delegationClaimsSet.getExpirationTime().toInstant()));
    assertTrue(Instant.now().plus(Duration.ofDays(31)).isAfter(delegationClaimsSet.getExpirationTime().toInstant()));
    assertNotNull(delegationClaimsSet.getJWTID());
    assertTrue(delegationClaimsSet.getJWTID().length() > 30);
    assertEquals("trust-mark-delegation+jwt", parsedDelegation.getHeader().getType().getType());

  }

}