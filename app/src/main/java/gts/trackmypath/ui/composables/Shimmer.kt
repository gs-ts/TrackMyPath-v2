package gts.trackmypath.ui.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.launch

internal fun Modifier.shimmer(): Modifier = this then ShimmerNodeElement()

private class ShimmerNodeElement : ModifierNodeElement<ShimmerNode>() {

    override fun create(): ShimmerNode = ShimmerNode()

    override fun update(node: ShimmerNode) {
        // no parameters to update
    }

    override fun equals(other: Any?): Boolean = other === this || other is ShimmerNodeElement

    override fun hashCode(): Int = "ShimmerNodeElement".hashCode()

    override fun InspectorInfo.inspectableProperties() {
        name = "shimmer"
    }
}

// Modifier.composed should be avoided for custom modifiers because it creates un-cacheable instances
// and adds unnecessary recomposition overhead to the node tree whenever the state changes.
// see: https://github.com/twitter/compose-rules/blob/main/docs/rules.md#avoid-modifier-extension-factory-functions
@Suppress("MagicNumber")
private class ShimmerNode : Modifier.Node(), DrawModifierNode {

    private val animatable = Animatable(initialValue = -500f)

    override fun onAttach() {
        super.onAttach()
        coroutineScope.launch {
            // animate continuously
            animatable.animateTo(
                targetValue = 2000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            ) {
                // invalidate the drawing phase every time the value updates
                invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        val translateAnim = animatable.value

        val brush = Brush.linearGradient(
            colors = listOf(
                Color.LightGray.copy(alpha = 0.6f),
                Color.LightGray.copy(alpha = 0.2f),
                Color.LightGray.copy(alpha = 0.6f)
            ),
            start = Offset(x = translateAnim, y = 0f),
            end = Offset(x = translateAnim + 400f, y = 0f)
        )

        // draw the background color first
        drawRect(brush = brush)
        // then draw the actual contents on top (if any)
        drawContent()
    }
}
