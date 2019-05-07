import dev.cccm5.movecraft.hitbox.SolidHitBox
import dev.cccm5.movecraft.util.GridVector
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.isIn
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SolidHitBoxTest {
    private fun hasSize(size: Int) = has(Collection<Any>::size, equalTo(size))
    private val minimum = GridVector(0, 0, 0)
    private val maximum = GridVector(3, 3, 3)
    private val hitBox = SolidHitBox(minimum, maximum)

    @Test
    fun testBounds(){
        assertEquals(minimum.x, hitBox.minX)
        assertEquals(minimum.y, hitBox.minY)
        assertEquals(minimum.z, hitBox.minZ)
        assertEquals(maximum.x, hitBox.maxX)
        assertEquals(maximum.y, hitBox.maxY)
        assertEquals(maximum.z, hitBox.maxZ)
    }

    @Test
    fun testSize() {
        assertEquals(64, hitBox.size)
        assertThat(hitBox, hasSize(64))
    }

    @Test
    fun testContents(){
        for (x in minimum.x..maximum.x) {
            for (y in minimum.y..maximum.y) {
                for (z in minimum.x..maximum.y) {
                    assertThat(GridVector(x, y, z), isIn(hitBox))
                }
            }
        }
    }

    @Test
    fun testBoundingHitBox(){
        assertEquals(hitBox, hitBox.boundingHitBox())
    }

    @Test
    fun testContainsAll() {
        val locations = mutableListOf<GridVector>()
        for (x in minimum.x..maximum.x) {
            for (y in minimum.y..maximum.y) {
                for (z in minimum.x..maximum.x) {
                    locations.add(GridVector(x, y, z))
                }
            }
        }
        assert(hitBox.containsAll(locations))
    }
}