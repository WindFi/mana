package me.sunzheng.mana.utils.glide

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import java.io.InputStream

/**
 * Registers [ImageKitStringLoader] so every `Glide.with(..).load("<url>")` call gets the ImageKit
 * `tr=` transformation for the actual target size.
 *
 * `replace` (and not `prepend`) is required: Glide's default `StringLoader` accepts every string,
 * so prepending would leave two loaders emitting a `LoadData` for the same request. The disk cache
 * key of a `MultiModelLoader` comes from the last loader, which would then be the untransformed
 * url and different view sizes could serve each other's bitmaps.
 */
@GlideModule
class ManaGlideModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(
            String::class.java,
            InputStream::class.java,
            ImageKitStringLoaderFactory()
        )
    }

    /** The project has no legacy manifest declared `GlideModule` entries to parse. */
    override fun isManifestParsingEnabled(): Boolean = false
}
