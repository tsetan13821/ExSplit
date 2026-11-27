package com.tenzin.splitter.group;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param inviteTtl how long an invite link stays valid
 */
@ConfigurationProperties("app.groups")
public record GroupProperties(@DefaultValue("48h") Duration inviteTtl) {
}
