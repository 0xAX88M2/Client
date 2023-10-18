package net.ccbluex.liquidbounce.utils.block.targetFinding

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.utils.block.canBeReplacedWith
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.client.getFace
import net.ccbluex.liquidbounce.utils.math.geometry.Face
import net.minecraft.block.*
import net.minecraft.item.ItemStack
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.*


enum class AimMode(override val choiceName: String) : NamedChoice {
    CENTER("Center"),
    RANDOM("Random"),
    STABILIZED("Stabilized"),
    NEAREST_ROTATION("NearestRotation"),
}

class BlockPlacementTargetFindingOptions(
    val offsetsToInvestigate: List<Vec3i>,
    val stackToPlaceWith: ItemStack,
    val facePositionFactory: FaceTargetPositionFactory,
    /**
     * Compares two offsets by their priority. The offset with the higher priority will be prioritized.
     */
    val offsetPriorityGetter: (Vec3i) -> Double
) {
    companion object {
        val PRIORITIZE_LEAST_BLOCK_DISTANCE: (Vec3i) -> Double = { vec ->
            -Vec3d.of(vec).add(0.5, 0.5, 0.5).squaredDistanceTo(mc.player!!.pos)
        }
    }
}

data class BlockTargetPlan(
    val blockPosToInteractWith: BlockPos,
    val interactionDirection: Direction,
    val angleToPlayerEyeCosine: Double
) {
    constructor(pos: BlockPos, direction: Direction) : this(pos, direction, calculateAngleCosine(pos, direction))

    companion object {
        private fun calculateAngleCosine(pos: BlockPos, direction: Direction): Double {
            val targetPositionOnBlock = pos.toCenterPos().add(Vec3d.of(direction.vector).multiply(0.5))
            val deltaToPlayerPos = mc.player!!.eyes.subtract(targetPositionOnBlock)

            return deltaToPlayerPos.dotProduct(Vec3d.of(direction.vector)) / deltaToPlayerPos.length()
        }
    }

}

enum class BlockTargetingMode {
    PLACE_AT_NEIGHBOR,
    REPLACE_EXISTING_BLOCK
}

private fun findBestTargetPlanForTargetPosition(posToInvestigate: BlockPos, mode: BlockTargetingMode): BlockTargetPlan? {
    val directions = Direction.values()

    val options = directions.mapNotNull { direction ->
        val targetPlan =
            getTargetPlanForPositionAndDirection(posToInvestigate, direction, mode)
                ?: return@mapNotNull null

        // Check if the target face is pointing away from the player
        if (targetPlan.angleToPlayerEyeCosine < 0)
            return@mapNotNull null

        return@mapNotNull targetPlan
    }

    return options.maxByOrNull { it.angleToPlayerEyeCosine }
}

/**
 * @return null if it is impossible to target the block with the given parameters
 */
fun getTargetPlanForPositionAndDirection(pos: BlockPos, direction: Direction, mode: BlockTargetingMode): BlockTargetPlan? {
     when (mode) {
        BlockTargetingMode.PLACE_AT_NEIGHBOR -> {
            val currPos = pos.add(direction.opposite.vector)
            val currState = currPos.getState() ?: return null

            if (currState.isAir || currState.isReplaceable) {
                return null
            }

            return BlockTargetPlan(currPos, direction)
        }
        BlockTargetingMode.REPLACE_EXISTING_BLOCK -> {
            return BlockTargetPlan(pos, direction)
        }
    }
}

class PointOnFace(val face: Face, val point: Vec3d)


private fun findTargetPointOnFace(
    currState: BlockState,
    currPos: BlockPos,
    targetPlan: BlockTargetPlan,
    options: BlockPlacementTargetFindingOptions
): PointOnFace? {
    val currBlock = currPos.getState()!!.block
    val truncate = currBlock is StairsBlock || currBlock is SlabBlock // TODO Find this out

    val shapeBBs = currState.getOutlineShape(mc.world!!, currPos, ShapeContext.of(mc.player!!)).boundingBoxes

    val face = shapeBBs.mapNotNull {
        var face = it.getFace(targetPlan.interactionDirection)

        if (truncate) {
            face = face.truncateY(0.5).requireNonEmpty() ?: return@mapNotNull null
        }

        val targetPos = options.facePositionFactory.producePositionOnFace(face, currPos)

        PointOnFace(
            face,
            targetPos
        )
    }.maxWithOrNull(
        Comparator.comparingDouble<PointOnFace> {
            it.point.subtract(
                Vec3d(
                    0.5,
                    0.5,
                    0.5
                )
            ).multiply(Vec3d.of(targetPlan.interactionDirection.vector)).lengthSquared()
        }.thenComparingDouble { it.point.y }
    )
    return face
}


private fun isBlockSolid(state: BlockState, pos: BlockPos) =
    state.isSideSolid(mc.world!!, pos, Direction.UP, SideShapeType.CENTER)
