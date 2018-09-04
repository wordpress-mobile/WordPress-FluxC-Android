package org.wordpress.android.fluxc.post

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.post.PostType

class PostTypeTest {
    @Test
    fun fromApiValuePage() {
        assertThat(PostType.fromApiValue("page")).isEqualTo(PostType.PAGE)
    }

    @Test
    fun fromApiValuePost() {
        assertThat(PostType.fromApiValue("post")).isEqualTo(PostType.POST)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromApiValueRaiseExceptionWhenUnknown() {
        PostType.fromApiValue("unknown value")
    }

    @Test
    fun fromModelValuePage() {
        assertThat(PostType.fromModelValue(1)).isEqualTo(PostType.PAGE)
    }

    @Test
    fun fromModelValuePost() {
        assertThat(PostType.fromModelValue(0)).isEqualTo(PostType.POST)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromModelValueRaiseExceptionWhenUnknown() {
        PostType.fromModelValue(-1)
    }
}
