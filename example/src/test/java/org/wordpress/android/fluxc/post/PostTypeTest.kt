package org.wordpress.android.fluxc.post

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.post.PostType

class PostTypeTest {
    @Test
    fun fromApiValuePage() {
        assertThat(PostType.fromApiValue("page")).isEqualTo(PostType.TypePage)
    }

    @Test
    fun fromApiValuePost() {
        assertThat(PostType.fromApiValue("post")).isEqualTo(PostType.TypePost)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromApiValueRaiseExceptionWhenUnknown() {
        PostType.fromApiValue("unknown value")
    }

    @Test
    fun fromModelValuePage() {
        assertThat(PostType.fromModelValue(1)).isEqualTo(PostType.TypePage)
    }

    @Test
    fun fromModelValuePost() {
        assertThat(PostType.fromModelValue(0)).isEqualTo(PostType.TypePost)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fromModelValueRaiseExceptionWhenUnknown() {
        PostType.fromModelValue(-1)
    }
}
