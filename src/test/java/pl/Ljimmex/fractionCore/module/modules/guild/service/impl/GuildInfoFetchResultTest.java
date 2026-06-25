package pl.Ljimmex.fractionCore.module.modules.guild.service.impl;

import org.junit.jupiter.api.Test;
import pl.Ljimmex.fractionCore.lang.PlaceholderContext;
import pl.Ljimmex.fractionCore.module.modules.guild.service.GuildInfo;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GuildInfoFetchResultTest {

    @Test
    void successFactoryReturnsInfoWithNoError() {
        GuildInfo info = new GuildInfo(null, Collections.emptyList(), false);

        var result = GuildInfoFetchResult.success(info);

        assertSame(info, result.info());
        assertNull(result.errorKey());
        assertNull(result.errorContext());
    }

    @Test
    void errorFactoryReturnsErrorWithNoInfo() {
        PlaceholderContext context = PlaceholderContext.empty().with("reason", "blocked");

        var result = GuildInfoFetchResult.error("guild.error.blocked", context);

        assertNull(result.info());
        assertEquals("guild.error.blocked", result.errorKey());
        assertSame(context, result.errorContext());
    }
}
