import com.soywiz.korev.Key
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.ui.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.BitmapFont
import com.soywiz.korim.font.readBitmapFont
import com.soywiz.korim.format.readBitmap
import com.soywiz.korim.text.TextAlignment
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.vector.roundRect
import kotlin.properties.Delegates
import kotlin.random.Random

var cellSize: Double = 0.0
var fieldSize: Double = 0.0
var leftIndent: Double = 0.0
var topIndent: Double = 0.0
var font: BitmapFont by Delegates.notNull()
var freeId = 0
var map = PositionMap()
var isAnimationRunning = false
var isGameOver = false

val blocks = mutableMapOf<Int, Block>()

suspend fun main() = Korge(width = 480, height = 640, bgcolor = RGBA(253, 247, 240), title = "2048 Test") {
    cellSize = views.virtualWidth / 5.0
    fieldSize = 50 + 4 * cellSize
    leftIndent = (views.virtualWidth - fieldSize) / 2
    topIndent = 150.0
    font = resourcesVfs["clear_sans.fnt"].readBitmapFont()

    val restartImg = resourcesVfs["restart.png"].readBitmap()
    val undoImg = resourcesVfs["undo.png"].readBitmap()
    val btnSize = cellSize * 0.3

    val bgLogo = roundRect(cellSize, cellSize, 5.0, fill = Colors["#edc403"]) {
        position(leftIndent, 30.0)
    }
    val bgField = roundRect(fieldSize, fieldSize, 5.0, fill = Colors["#b9aea0"]) {
        position(leftIndent, topIndent)
    }
    val bgBest = roundRect(cellSize * 1.5, cellSize * 0.8, 5.0, fill = Colors["#bbae9e"]) {
        alignRightToRightOf(bgField)
        alignTopToTopOf(bgLogo)
    }
    val bgScore = roundRect(cellSize * 1.5, cellSize * 0.8, 5.0, fill = Colors["#bbae9e"]) {
        alignRightToLeftOf(bgBest, 24)
        alignTopToTopOf(bgBest)
    }

    graphics {
        position(leftIndent, topIndent)
        fill(Colors["#cec0b2"]) {
            for (i in 0..3) {
                for (j in 0..3) {
                    roundRect(10.0 + (10.0 + cellSize) * i, 10.0 + (10.0 + cellSize) * j, cellSize, cellSize, 5.0)
                }
            }
        }
    }

    text("2048", cellSize * 0.5, Colors.WHITE, font).centerOn(bgLogo)
    text("BEST", cellSize * 0.25, RGBA(239, 226, 210), font) {
        centerXOn(bgBest)
        alignTopToTopOf(bgBest, 5.0)
    }
    text("0", cellSize * 0.5, Colors.WHITE, font) {
        setTextBounds(Rectangle(0.0, 0.0, bgBest.width, cellSize - 24.0))
        alignment = TextAlignment.MIDDLE_CENTER
        alignTopToTopOf(bgBest, 12.0)
        centerXOn(bgBest)
    }
    text("SCORE", cellSize * 0.25, RGBA(239, 226, 210), font) {
        centerXOn(bgScore)
        alignTopToTopOf(bgScore, 5.0)
    }
    text("0", cellSize * 0.5, Colors.WHITE, font) {
        setTextBounds(Rectangle(0.0, 0.0, bgScore.width, cellSize - 24.0))
        alignment = TextAlignment.MIDDLE_CENTER
        alignTopToTopOf(bgScore, 12.0)
        centerXOn(bgScore)
    }
    val restartBlock = container {
        val background = roundRect(btnSize, btnSize, 5.0, fill = RGBA(185, 174, 160))
        image(restartImg) {
            size(btnSize * 0.8, btnSize * 0.8)
            centerOn(background)
        }
        alignTopToBottomOf(bgBest, 5.0)
        alignRightToRightOf(bgField)
    }
    val undoBlock = container {
        val background = roundRect(btnSize, btnSize, 5.0, fill = RGBA(185, 174, 160))
        image(undoImg) {
            size(btnSize * 0.8, btnSize * 0.8)
            centerOn(background)
        }
        alignTopToTopOf(restartBlock)
        alignRightToLeftOf(restartBlock, 5.0)
    }

    generateBlock()

    keys {
        down {
            when (it.key) {
                Key.LEFT -> moveBlocksTo(Direction.LEFT)
                Key.RIGHT -> moveBlocksTo(Direction.RIGHT)
                Key.UP -> moveBlocksTo(Direction.UP)
                Key.DOWN -> moveBlocksTo(Direction.DOWN)
                else -> Unit
            }
        }
    }

    onSwipe(20.0) {
        when (it.direction) {
            SwipeDirection.LEFT -> moveBlocksTo(Direction.LEFT)
            SwipeDirection.RIGHT -> moveBlocksTo(Direction.RIGHT)
            SwipeDirection.TOP -> moveBlocksTo(Direction.UP)
            SwipeDirection.BOTTOM -> moveBlocksTo(Direction.DOWN)
        }
    }
}

fun columnX(number: Int) = leftIndent + 10 + (cellSize + 10) * number
fun rowY(number: Int) = topIndent + 10 + (cellSize + 10) * number

fun Container.createNewBlockWithId(id: Int, number: Number, position: Position) {
    blocks[id] = block(number).position(columnX(position.x), rowY(position.y))
}

fun Container.createNewBlock(number: Number, position: Position): Int {
    val id = freeId++
    createNewBlockWithId(id, number, position)
    return id
}

fun Container.generateBlock() {
    val position = map.getRandomFreePosition() ?: return
    val number = if (Random.nextDouble() < 0.9) Number.ZERO else Number.ONE
    val newId = createNewBlock(number, position)
    map[position.x, position.y] = newId
}

fun Container.showGameOver(onRestart: () -> Unit) = container {
    fun restart() {
        this@container.removeFromParent()
        onRestart()
    }

    position(leftIndent, topIndent)

    roundRect(fieldSize, fieldSize, 5.0, fill = Colors["#FFFFFF33"])
    text("Game Over", 60.0, Colors.BLACK, font) {
        centerBetween(0.0, 0.0, fieldSize, fieldSize)
        y -= 60
    }
    uiText("Try again", 120.0, 35.0) {
        centerBetween(0.0, 0.0, fieldSize, fieldSize)
        y += 20
        textSize = 40.0
        textFont = font
        textColor = RGBA(0, 0, 0)
        onOver { textColor = RGBA(90, 90, 90) }
        onOut { textColor = RGBA(0, 0, 0) }
        onDown { textColor = RGBA(120, 120, 120) }
        onUp { textColor = RGBA(120, 120, 120) }
        onClick { restart() }
    }

    keys {
        down {
            when (it.key) {
                Key.ENTER, Key.SPACE -> restart()
                else -> Unit
            }
        }
    }
}

fun Stage.moveBlocksTo(direction: Direction) {
    if (isAnimationRunning) return
    //if (!map.hasAvailableMoves()) {
    if (!isGameOver) {
        isGameOver = true
        showGameOver {
            isGameOver = false
            //restart()
        }
    }
    //}
    return
}