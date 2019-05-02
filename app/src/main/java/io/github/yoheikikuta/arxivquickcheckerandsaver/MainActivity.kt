package io.github.yoheikikuta.arxivquickcheckerandsaver

import android.annotation.SuppressLint
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.*

class MainActivity : FragmentActivity() {

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private lateinit var mPager: ViewPager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_slide)

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager)

        // Get arXiv RSS information and set the pager adapter which provides the pages to the view paper widget.
        "http://export.arxiv.org/rss/cs.CV".httpGet().responseString { _, _, result ->
            when (result) {
                is Result.Failure -> {
                    val ex = result.getException()
                }
                is Result.Success -> {
                    val data = result.get()
                    val items: List<Item> = ArxivRSSXmlParser().parse(data) as List<Item>?
                        ?: listOf(Item(title = "NO ENTRY", creator = "", description = ""))
                    val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager, items)
                    mPager.adapter = pagerAdapter
                }
            }
        }
    }

    override fun onBackPressed() {
        if (mPager.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed()
        } else {
            // Otherwise, select the previous step.
            mPager.currentItem = mPager.currentItem - 1
        }
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager, val items: List<Item>) : FragmentStatePagerAdapter(fm) {
        override fun getCount(): Int = items.size

        override fun getItem(position: Int): Fragment = ScreenSlidePageFragment.newInstance(position, items)
    }

}

@Parcelize
data class Item(val title: String, val creator: String, val description: String): Parcelable

class ScreenSlidePageFragment: Fragment() {

    companion object {
        fun newInstance(position: Int, items: List<Item>): ScreenSlidePageFragment {
            val fragment = ScreenSlidePageFragment()
            val args = Bundle()
            args.putInt("position", position)
            items as ArrayList<Parcelable>
            args.putParcelableArrayList("items", items)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_screen_slide_page, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val position: Int = arguments!!.get("position") as Int
        val items: List<Item> = arguments!!.get("items") as List<Item>

        val titleTextView = view.findViewById<TextView>(R.id.title)
        val creatorTextView = view.findViewById<TextView>(R.id.creator)
        val descriptionTextView = view.findViewById<TextView>(R.id.description)

        titleTextView.text = items[position].title
        creatorTextView.text = items[position].creator
        descriptionTextView.text = items[position].description
    }
}
