import java.util.*
import kotlin.math.absoluteValue

fun addPath(mapString: String): String {
    val structuredGraph = StructuredGraph(mapString)
    val startNode = structuredGraph[NodeType.Start] ?: error("No start node")
    val finishNode = structuredGraph[NodeType.Finish] ?: error("No finish node")

    val finalPath = performBestFirstSearch(
            startNode = startNode,
            finishNode = finishNode,
            graph = structuredGraph) ?: error("no path found")

    val outputGrid = mapString
            .split("\n")
            .map { it.toCharArray() }
    finalPath.forEach { pathPoint ->
        outputGrid[pathPoint.node.row][pathPoint.node.column] = '*'
    }
    return outputGrid.joinToString("\n") { String(it) }

}

/*
    Strategy is to consider a path as any number of travelled nodes. These nodes track how far they've travelled thus
    far and their current distance to the finish. We expand this path by looking at all the next available path options
    from all the currently viewed paths, and selecting the one that has the shortest combined current path distance +
    remaining distance to the finish. As we search the most promising next step first always, the first path that
    reaches the finish will have equivalent distance to the most optimal one, and we return. If we exhaust the priority
    queue, no path is possible.
 */
fun performBestFirstSearch(startNode: StructuredNode, finishNode: StructuredNode, graph: StructuredGraph): PathNode? {
    val priorityQueue = PriorityQueue<PathNode>()
    priorityQueue.add(PathNode(startNode, startNode.distanceTo(finishNode), 0.0))
    while (priorityQueue.isNotEmpty()) {
        val currentNode = priorityQueue.remove()
        if (currentNode.node == finishNode) {
            return currentNode
        }
        val neighbors = graph
                .getNeighbors(currentNode.node)
                .asSequence()
                .filter { it.type != NodeType.Blocked }
                .map { PathNode(it, it.distanceTo(finishNode), currentNode.pathDistance + currentNode.node.distanceTo(it), currentNode) }
        neighbors.forEach { node ->
            priorityQueue.find { pqNode -> pqNode.node == node.node && pqNode.pathDistance >= node.pathDistance }?.let { priorityQueue.remove(it) }
        }
        priorityQueue.addAll(neighbors)
    }
    return null
}

data class PathNode(val node: StructuredNode, val distance: Double, val pathDistance: Double, val priorNode: PathNode? = null) : Comparable<PathNode>, Iterable<PathNode> {
    override fun iterator() =
            iterator {
                yield(this@PathNode)
                priorNode?.let { priorNode: PathNode ->
                    yieldAll(priorNode)
                }
            }

    override operator fun compareTo(other: PathNode) = (pathDistance + distance).compareTo(other.pathDistance + other.distance)
}

inline operator fun <reified T> List<List<T>>.get(rowIndex: Int, columnIndex: Int): T? =
        getOrNull(rowIndex)?.getOrNull(columnIndex)

data class StructuredGraph(private val nodes: List<List<StructuredNode>>) {
    constructor(baseString: String) : this(
            baseString
                    .split(delimiters = *arrayOf("\n"))
                    .mapIndexed { rowIndex, line ->
                        line.mapIndexed { columnIndex, specificCharacter ->
                            StructuredNode(rowIndex, columnIndex, NodeType.fromCharacter(specificCharacter))
                        }
                    }
    )

    fun getNeighbors(startNode: StructuredNode): List<StructuredNode> {
        (maxOf(0, startNode.row - 1)..minOf(nodes.size, startNode.row + 1)).zip(
                maxOf(0, startNode.column - 1)..minOf(nodes[0].size, startNode.column + 1)
        ).map { neighborCandidatePoint ->
            nodes[neighborCandidatePoint.first, neighborCandidatePoint.second]
        }
        return nodes
                .filterIndexed { index, _ ->
                    (index - startNode.row).absoluteValue <= 1
                }
                .flatMap { rowList ->
                    rowList.filterIndexed { index, _ ->
                        (index - startNode.column).absoluteValue <= 1
                    }
                }
                .filter { it.row != startNode.row || it.column != startNode.column }
    }

    operator fun get(nodeType: NodeType) =
            nodes
                    .asSequence()
                    .flatten()
                    .filter { candidateNode ->
                        candidateNode.type == nodeType
                    }
                    .firstOrNull()
}

data class StructuredNode(val row: Int, val column: Int, val type: NodeType) {

    // The actual shortest distance between two nodes in the scope of the problem
    fun distanceTo(otherNode: StructuredNode): Double {
        val rowDistance = (row - otherNode.row).absoluteValue
        val columnDistance = (column - otherNode.column).absoluteValue

        return (minOf(rowDistance, columnDistance) * 1.5) + (rowDistance - columnDistance).absoluteValue
    }
}

sealed class NodeType {
    open class Visitable : NodeType()
    object Start : Visitable()
    object Finish : Visitable()
    object Blocked : NodeType()

    companion object {
        fun fromCharacter(character: Char): NodeType {
            return when (character) {
                '.' -> Visitable()
                'S' -> Start
                'X' -> Finish
                'B' -> Blocked
                else -> error("Unrecognized NodeType for character: $character")
            }
        }
    }
}