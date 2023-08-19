package net.insprill.robotinsprill.hoplite.decoder

import com.sksamuel.hoplite.ConfigFailure
import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.DecoderContext
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.StringNode
import com.sksamuel.hoplite.decoder.NullHandlingDecoder
import com.sksamuel.hoplite.fp.Validated
import com.sksamuel.hoplite.fp.invalid
import dev.kord.common.Color
import net.insprill.robotinsprill.extension.color
import kotlin.reflect.KType

class ColorDecoder : NullHandlingDecoder<Color> {

    override fun supports(type: KType): Boolean = type.classifier == Color::class

    override fun safeDecode(node: Node, type: KType, context: DecoderContext): ConfigResult<Color> {
        return when (node) {
            is StringNode -> {
                return Validated.Valid(node.value.color())
            }

            else -> ConfigFailure.DecodeError(node, type).invalid()
        }
    }

}
