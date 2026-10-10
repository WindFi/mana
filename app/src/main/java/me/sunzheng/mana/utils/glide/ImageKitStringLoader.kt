package me.sunzheng.mana.utils.glide

import android.net.Uri
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.model.StringLoader
import me.sunzheng.mana.utils.HostUtil
import java.io.InputStream

/**
 * Rewrites ImageKit hosted urls with a `tr=w-..,h-..` transformation that matches the size Glide
 * resolved for the target, right before the request is dispatched.
 *
 * Non ImageKit urls are returned untouched by [HostUtil.imagekitSupport], so this loader stays a
 * transparent pass-through for every other string model (`content://`, file paths, `data:image`,
 * plain http(s) urls hosted elsewhere, ...).
 */
class ImageKitStringLoader(
    private val delegate: ModelLoader<String, InputStream>
) : ModelLoader<String, InputStream> {

    override fun handles(model: String): Boolean = delegate.handles(model)

    override fun buildLoadData(
        model: String,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream>? {
        // Glide hands us the final target size here. `Target.SIZE_ORIGINAL` (Int.MIN_VALUE) and
        // not-yet-measured targets fall through as unusable negative values.
        val ratio = if (width > 0 && height > 0) height.toFloat() / width.toFloat() else -1f
        val transformed = HostUtil.imagekitSupport(model, width.toFloat(), height.toFloat(), ratio)
        return delegate.buildLoadData(transformed, width, height, options)
    }
}

/**
 * Equivalent of Glide's default `StringLoader.StreamFactory`, wrapped in [ImageKitStringLoader].
 *
 * The delegate has to be built from the `Uri` mappers explicitly: asking `multiFactory` for
 * `String -> InputStream` again would recurse into this very factory.
 */
class ImageKitStringLoaderFactory : ModelLoaderFactory<String, InputStream> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String, InputStream> =
        ImageKitStringLoader(
            StringLoader(multiFactory.build(Uri::class.java, InputStream::class.java))
        )

    override fun teardown() = Unit
}
