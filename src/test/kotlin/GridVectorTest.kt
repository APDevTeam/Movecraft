import dev.cccm5.movecraft.util.GridVector
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GridVectorTest {
    private val zero = GridVector(0, 0, 0)
    private val one = GridVector(1, 1, 1)
    private val a = GridVector(1, 6, 5)
    private val b = GridVector(10, 3, 7)
    private val c = GridVector(8, 2, 1)

    @Test
    fun testNeg(){
        assertEquals(-zero, zero)
        assertEquals(-one , GridVector(-1, -1, -1))
        val (x,y,z) = a
        assertEquals(-a , GridVector(-x, -y, -z))
    }

    @Test
    fun testAdd(){
        assertEquals(zero+zero , zero)
        assertEquals(zero+a , a)
        assertEquals(a+zero , a)
        assertEquals(zero+b , b)
        assertEquals(b+zero , b)
        assertEquals(a+b , GridVector(11, 9, 12))
        assertEquals(b+a , GridVector(11, 9, 12))
        assertEquals(b+a , a+b)
    }

    @Test
    fun testSubtract(){
        assertEquals(zero-zero , zero)
        assertEquals(zero-a , -a)
        assertEquals(a-b , GridVector(a.x - b.x, a.y - b.y, a.z - b.z))
    }

    @Test
    fun testRange(){
        assertEquals((zero..a).minX , zero.x)
        assertEquals((zero..a).minY , zero.y)
        assertEquals((zero..a).minZ , zero.z)
        assertEquals((zero..a).maxX , a.x)
        assertEquals((zero..a).maxY , a.y)
        assertEquals((zero..a).maxZ , a.z)
        assertEquals((zero..a).xLength, a.x+1)
        assertEquals((zero..a).yLength, a.y+1)
        assertEquals((zero..a).zLength, a.z+1)
        assertEquals((zero..a).size , (a.x+1) * (a.y+1) * (a.z+1))
    }

    @Test
    fun testMagnitude(){
        assertEquals(a.magnitudeSquared(), 62)
        assertEquals(a.magnitudeSquared(b), 94)
    }

    @Test
    fun testDot(){
        assertEquals(zero dot zero , zero)
        assertEquals(one dot zero , zero)
        assertEquals(zero dot one , zero)
        assertEquals(a dot zero , zero)
        assertEquals(a dot one , a)
        assertEquals(one dot a , a)
        assertEquals(a dot b , GridVector(a.x * b.x, a.y * b.y, a.z * b.z))


    }
}