package com.ruuvi.station.util.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BottomFadingEdgeListViewTest {
    @Test
    fun `list uses the shared bottom-only fade while scrolling behind navigation padding`() {
        val (root, list) = createList()
        repeat(3) {
            assertTrue(list.canScrollVertically(1))
            val bitmap = render(root)
            assertEquals(Color.BLACK, bitmap.getPixel(50, 20))
            assertEquals(Color.BLACK, bitmap.getPixel(50, 135))
            assertTrue(Color.red(bitmap.getPixel(50, 151)) in 1..254)
            val middleFade = Color.red(bitmap.getPixel(50, 176))
            val bottomFade = Color.red(bitmap.getPixel(50, 199))
            assertTrue(middleFade in 1..254)
            assertTrue(bottomFade > middleFade)
            bitmap.recycle()
            list.scrollListBy(60)
        }
    }

    @Test
    fun `last row and short lists remain fully readable`() {
        for (count in listOf(1, 12)) {
            val (root, list) = createList(count)
            repeat(12) { list.scrollListBy(100) }
            assertFalse(list.canScrollVertically(1))
            val bitmap = render(root)
            val lastRow = list.getChildAt(list.childCount - 1)
            assertEquals(Color.BLACK, bitmap.getPixel(50, lastRow.bottom - 5))
            assertEquals(Color.WHITE, bitmap.getPixel(50, 190))
            bitmap.recycle()
        }
    }

    private fun createList(count: Int = 12): Pair<FrameLayout, BottomFadingEdgeListView> {
        val context = RuntimeEnvironment.getApplication()
        val list = BottomFadingEdgeListView(context).apply {
            setBackgroundColor(Color.WHITE)
            setPadding(0, 0, 0, 24)
            clipToPadding = false
            dividerHeight = 0
            adapter = object : BaseAdapter() {
                override fun getCount(): Int = count
                override fun getItem(position: Int): Any = position
                override fun getItemId(position: Int): Long = position.toLong()
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                    (convertView ?: View(context)).apply {
                        layoutParams = AbsListView.LayoutParams(100, 50)
                        setBackgroundColor(Color.BLACK)
                    }
            }
        }
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.WHITE)
            addView(list, FrameLayout.LayoutParams(100, 200))
            measure(
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
            )
            layout(0, 0, 100, 200)
        }
        return root to list
    }

    private fun render(root: View): Bitmap =
        Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888).also {
            root.draw(Canvas(it))
        }
}
