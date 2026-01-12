@file:Suppress("UNUSED_CHANGED_VALUE", "AssignedValueIsNeverRead")

package com.learnopengles.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.SimpleAdapter
import com.learnopengles.android.lesson1.LessonOneActivity
import com.learnopengles.android.lesson2.LessonTwoActivity
import com.learnopengles.android.lesson3.LessonThreeActivity
import com.learnopengles.android.lesson4.LessonFourActivity
import com.learnopengles.android.lesson5.LessonFiveActivity
import com.learnopengles.android.lesson6.LessonSixActivity
import com.learnopengles.android.lesson7.LessonSevenActivity
import com.learnopengles.android.lesson8.LessonEightActivity
import java.util.*

class TableOfContents : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.toc)
        setContentView(R.layout.table_of_contents)

        val listView = findViewById<ListView>(R.id.list_view)

        // Initialize data
        val arrayList = ArrayList<Map<String, Any>>()
        val activityMapping = SparseArray<Class<out Activity>>()

        var i = 0
        var item = HashMap<String, Any>()

        item[ITEM_IMAGE] = R.drawable.ic_lesson_one
        item[ITEM_TITLE] = getText(R.string.lesson_one)
        item[ITEM_SUBTITLE] = getText(R.string.lesson_one_subtitle)
        arrayList.add(item)
        activityMapping.put(i++, LessonOneActivity::class.java)

        item = HashMap()
        item[ITEM_IMAGE] = R.drawable.ic_lesson_two
        item[ITEM_TITLE] = getText(R.string.lesson_two)
        item[ITEM_SUBTITLE] = getText(R.string.lesson_two_subtitle)
        arrayList.add(item)
        activityMapping.put(i++, LessonTwoActivity::class.java)

        item = HashMap()
        item[ITEM_IMAGE] = R.drawable.ic_lesson_three
        item[ITEM_TITLE] = getText(R.string.lesson_three)
        item[ITEM_SUBTITLE] = getText(R.string.lesson_three_subtitle)
        arrayList.add(item)
        activityMapping.put(i++, LessonThreeActivity::class.java)

        item = HashMap()
        item[ITEM_IMAGE] = R.drawable.ic_lesson_four
        item[ITEM_TITLE] = getText(R.string.lesson_four)
        item[ITEM_SUBTITLE] = getText(R.string.lesson_four_subtitle)
        arrayList.add(item)
        activityMapping.put(i++, LessonFourActivity::class.java)

        item = HashMap()
        item[ITEM_IMAGE] = R.drawable.ic_lesson_five
        item[ITEM_TITLE] = getText(R.string.lesson_five)
        item[ITEM_SUBTITLE] = getText(R.string.lesson_five_subtitle)
        arrayList.add(item)
        activityMapping.put(i++, LessonFiveActivity::class.java)

        item = HashMap()
        item[ITEM_IMAGE] = R.drawable.ic_lesson_six
        item[ITEM_TITLE] = getText(R.string.lesson_six)
        item[ITEM_SUBTITLE] = getText(R.string.lesson_six_subtitle)
        arrayList.add(item)
        activityMapping.put(i++, LessonSixActivity::class.java)

        item = HashMap()
        item[ITEM_IMAGE] = R.drawable.ic_lesson_seven
        item[ITEM_TITLE] = getText(R.string.lesson_seven)
        item[ITEM_SUBTITLE] = getText(R.string.lesson_seven_subtitle)
        arrayList.add(item)
        activityMapping.put(i++, LessonSevenActivity::class.java)

        item = HashMap()
        item[ITEM_IMAGE] = R.drawable.ic_lesson_eight
        item[ITEM_TITLE] = getText(R.string.lesson_eight)
        item[ITEM_SUBTITLE] = getText(R.string.lesson_eight_subtitle)
        arrayList.add(item)
        activityMapping.put(i++, LessonEightActivity::class.java)

        val dataAdapter = SimpleAdapter(
                this,
                arrayList,
                R.layout.toc_item,
                arrayOf(ITEM_IMAGE, ITEM_TITLE, ITEM_SUBTITLE),
                intArrayOf(R.id.Image, R.id.Title, R.id.SubTitle))
        listView.adapter = dataAdapter

        listView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            val activityToLaunch = activityMapping.get(position)

            if (activityToLaunch != null) {
                val launchIntent = Intent(this@TableOfContents, activityToLaunch)
                startActivity(launchIntent)
            }
        }
    }

    companion object {
        private const val ITEM_IMAGE = "item_image"
        private const val ITEM_TITLE = "item_title"
        private const val ITEM_SUBTITLE = "item_subtitle"
    }
}
